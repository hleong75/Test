package com.transitolibre.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.transitolibre.data.dao.*
import com.transitolibre.data.entity.*

@Database(
    entities = [
        Agency::class,
        Stop::class,
        Route::class,
        Trip::class,
        StopTime::class,
        Calendar::class
    ],
    version = 1,
    exportSchema = true
)
abstract class GtfsDatabase : RoomDatabase() {

    abstract fun agencyDao(): AgencyDao
    abstract fun stopDao(): StopDao
    abstract fun routeDao(): RouteDao
    abstract fun tripDao(): TripDao
    abstract fun stopTimeDao(): StopTimeDao
    abstract fun calendarDao(): CalendarDao

    companion object {
        private const val DATABASE_NAME = "gtfs_database"

        @Volatile
        private var INSTANCE: GtfsDatabase? = null

        fun getInstance(context: Context): GtfsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GtfsDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
