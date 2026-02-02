package com.transitolibre.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.transitolibre.R
import com.transitolibre.data.database.GtfsDatabase
import com.transitolibre.data.repository.GtfsRepository
import com.transitolibre.parser.GtfsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GtfsImportWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_FILE_URI = "file_uri"
        const val KEY_PROGRESS = "progress"
        const val KEY_MESSAGE = "message"
        const val WORK_NAME = "gtfs_import_work"

        private const val NOTIFICATION_CHANNEL_ID = "gtfs_import_channel"
        private const val NOTIFICATION_ID = 1

        fun createWorkRequest(fileUri: Uri): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<GtfsImportWorker>()
                .setInputData(workDataOf(KEY_FILE_URI to fileUri.toString()))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        }
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val fileUriString = inputData.getString(KEY_FILE_URI)
            ?: return@withContext Result.failure()

        val fileUri = Uri.parse(fileUriString)

        createNotificationChannel()
        setForeground(createForegroundInfo(0, "Démarrage de l'import..."))

        try {
            val database = GtfsDatabase.getInstance(context)
            val repository = GtfsRepository(
                database.agencyDao(),
                database.stopDao(),
                database.routeDao(),
                database.tripDao(),
                database.stopTimeDao(),
                database.calendarDao()
            )

            // Clear existing data
            setProgressAsync(workDataOf(KEY_PROGRESS to 5, KEY_MESSAGE to "Suppression des anciennes données..."))
            updateNotification(5, "Suppression des anciennes données...")
            repository.clearAllData()

            // Parse GTFS file
            val parser = GtfsParser()
            val inputStream = context.contentResolver.openInputStream(fileUri)
                ?: return@withContext Result.failure()

            setProgressAsync(workDataOf(KEY_PROGRESS to 10, KEY_MESSAGE to "Analyse du fichier GTFS..."))
            updateNotification(10, "Analyse du fichier GTFS...")

            val parseResult = inputStream.use {
                parser.parseZip(it, object : GtfsParser.ProgressListener {
                    override fun onProgress(current: Int, total: Int, message: String) {
                        val progress = 10 + (current * 40 / total)
                        setProgressAsync(workDataOf(KEY_PROGRESS to progress, KEY_MESSAGE to message))
                        updateNotification(progress, message)
                    }
                })
            }

            // Insert data into database
            setProgressAsync(workDataOf(KEY_PROGRESS to 50, KEY_MESSAGE to "Insertion des agences..."))
            updateNotification(50, "Insertion des agences...")
            repository.insertAgencies(parseResult.agencies)

            setProgressAsync(workDataOf(KEY_PROGRESS to 55, KEY_MESSAGE to "Insertion des arrêts..."))
            updateNotification(55, "Insertion des arrêts...")
            repository.insertStops(parseResult.stops)

            setProgressAsync(workDataOf(KEY_PROGRESS to 60, KEY_MESSAGE to "Insertion des lignes..."))
            updateNotification(60, "Insertion des lignes...")
            repository.insertRoutes(parseResult.routes)

            setProgressAsync(workDataOf(KEY_PROGRESS to 65, KEY_MESSAGE to "Insertion des calendriers..."))
            updateNotification(65, "Insertion des calendriers...")
            repository.insertCalendars(parseResult.calendars)

            setProgressAsync(workDataOf(KEY_PROGRESS to 70, KEY_MESSAGE to "Insertion des trajets..."))
            updateNotification(70, "Insertion des trajets...")
            // Insert trips in batches
            parseResult.trips.chunked(1000).forEachIndexed { index, batch ->
                repository.insertTrips(batch)
                val progress = 70 + (index * 10 / (parseResult.trips.size / 1000 + 1))
                setProgressAsync(workDataOf(KEY_PROGRESS to progress, KEY_MESSAGE to "Insertion des trajets..."))
            }

            setProgressAsync(workDataOf(KEY_PROGRESS to 80, KEY_MESSAGE to "Insertion des horaires..."))
            updateNotification(80, "Insertion des horaires...")
            // Insert stop times in larger batches for performance
            parseResult.stopTimes.chunked(5000).forEachIndexed { index, batch ->
                repository.insertStopTimes(batch)
                val progress = 80 + (index * 19 / (parseResult.stopTimes.size / 5000 + 1))
                setProgressAsync(workDataOf(KEY_PROGRESS to progress, KEY_MESSAGE to "Insertion des horaires..."))
                updateNotification(progress, "Insertion des horaires (${index * 5000}/${parseResult.stopTimes.size})...")
            }

            setProgressAsync(workDataOf(KEY_PROGRESS to 100, KEY_MESSAGE to "Import terminé!"))
            updateNotification(100, "Import terminé!")

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(workDataOf(KEY_MESSAGE to "Erreur: ${e.message}"))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(progress: Int, message: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.parsing_gtfs))
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(progress: Int, message: String) {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.parsing_gtfs))
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setProgress(100, progress, false)
            .setOngoing(progress < 100)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
