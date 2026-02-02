package com.transitolibre.parser

import com.opencsv.CSVReaderBuilder
import com.transitolibre.data.entity.*
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

class GtfsParser {

    data class ParseResult(
        val agencies: List<Agency>,
        val stops: List<Stop>,
        val routes: List<Route>,
        val trips: List<Trip>,
        val stopTimes: List<StopTime>,
        val calendars: List<Calendar>
    )

    interface ProgressListener {
        fun onProgress(current: Int, total: Int, message: String)
    }

    fun parseZip(inputStream: InputStream, listener: ProgressListener? = null): ParseResult {
        val agencies = mutableListOf<Agency>()
        val stops = mutableListOf<Stop>()
        val routes = mutableListOf<Route>()
        val trips = mutableListOf<Trip>()
        val stopTimes = mutableListOf<StopTime>()
        val calendars = mutableListOf<Calendar>()

        val totalFiles = 6
        var processedFiles = 0

        ZipInputStream(inputStream).use { zipStream ->
            var entry = zipStream.nextEntry

            while (entry != null) {
                when (entry.name) {
                    "agency.txt" -> {
                        listener?.onProgress(++processedFiles, totalFiles, "Parsing agencies...")
                        agencies.addAll(parseAgencies(zipStream))
                    }
                    "stops.txt" -> {
                        listener?.onProgress(++processedFiles, totalFiles, "Parsing stops...")
                        stops.addAll(parseStops(zipStream))
                    }
                    "routes.txt" -> {
                        listener?.onProgress(++processedFiles, totalFiles, "Parsing routes...")
                        routes.addAll(parseRoutes(zipStream))
                    }
                    "trips.txt" -> {
                        listener?.onProgress(++processedFiles, totalFiles, "Parsing trips...")
                        trips.addAll(parseTrips(zipStream))
                    }
                    "stop_times.txt" -> {
                        listener?.onProgress(++processedFiles, totalFiles, "Parsing stop times...")
                        stopTimes.addAll(parseStopTimes(zipStream))
                    }
                    "calendar.txt" -> {
                        listener?.onProgress(++processedFiles, totalFiles, "Parsing calendar...")
                        calendars.addAll(parseCalendars(zipStream))
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }

        return ParseResult(agencies, stops, routes, trips, stopTimes, calendars)
    }

    private fun parseAgencies(inputStream: InputStream): List<Agency> {
        val agencies = mutableListOf<Agency>()
        val reader = CSVReaderBuilder(InputStreamReader(inputStream))
            .withSkipLines(0)
            .build()

        val header = reader.readNext() ?: return agencies
        val headerMap = header.mapIndexed { index, name -> name.trim() to index }.toMap()

        var line = reader.readNext()
        while (line != null) {
            try {
                agencies.add(
                    Agency(
                        agencyId = getFieldValue(line, headerMap, "agency_id") ?: "default",
                        name = getFieldValue(line, headerMap, "agency_name") ?: "",
                        url = getFieldValue(line, headerMap, "agency_url"),
                        timezone = getFieldValue(line, headerMap, "agency_timezone"),
                        lang = getFieldValue(line, headerMap, "agency_lang"),
                        phone = getFieldValue(line, headerMap, "agency_phone")
                    )
                )
            } catch (_: Exception) { }
            line = reader.readNext()
        }
        return agencies
    }

    private fun parseStops(inputStream: InputStream): List<Stop> {
        val stops = mutableListOf<Stop>()
        val reader = CSVReaderBuilder(InputStreamReader(inputStream))
            .withSkipLines(0)
            .build()

        val header = reader.readNext() ?: return stops
        val headerMap = header.mapIndexed { index, name -> name.trim() to index }.toMap()

        var line = reader.readNext()
        while (line != null) {
            try {
                val lat = getFieldValue(line, headerMap, "stop_lat")?.toDoubleOrNull()
                val lon = getFieldValue(line, headerMap, "stop_lon")?.toDoubleOrNull()

                if (lat != null && lon != null) {
                    stops.add(
                        Stop(
                            stopId = getFieldValue(line, headerMap, "stop_id") ?: "",
                            name = getFieldValue(line, headerMap, "stop_name") ?: "",
                            lat = lat,
                            lon = lon,
                            code = getFieldValue(line, headerMap, "stop_code"),
                            description = getFieldValue(line, headerMap, "stop_desc"),
                            zoneId = getFieldValue(line, headerMap, "zone_id"),
                            url = getFieldValue(line, headerMap, "stop_url"),
                            locationType = getFieldValue(line, headerMap, "location_type")?.toIntOrNull(),
                            parentStation = getFieldValue(line, headerMap, "parent_station"),
                            wheelchairBoarding = getFieldValue(line, headerMap, "wheelchair_boarding")?.toIntOrNull()
                        )
                    )
                }
            } catch (_: Exception) { }
            line = reader.readNext()
        }
        return stops
    }

    private fun parseRoutes(inputStream: InputStream): List<Route> {
        val routes = mutableListOf<Route>()
        val reader = CSVReaderBuilder(InputStreamReader(inputStream))
            .withSkipLines(0)
            .build()

        val header = reader.readNext() ?: return routes
        val headerMap = header.mapIndexed { index, name -> name.trim() to index }.toMap()

        var line = reader.readNext()
        while (line != null) {
            try {
                routes.add(
                    Route(
                        routeId = getFieldValue(line, headerMap, "route_id") ?: "",
                        agencyId = getFieldValue(line, headerMap, "agency_id"),
                        shortName = getFieldValue(line, headerMap, "route_short_name"),
                        longName = getFieldValue(line, headerMap, "route_long_name"),
                        description = getFieldValue(line, headerMap, "route_desc"),
                        type = getFieldValue(line, headerMap, "route_type")?.toIntOrNull() ?: 3,
                        color = getFieldValue(line, headerMap, "route_color"),
                        textColor = getFieldValue(line, headerMap, "route_text_color"),
                        url = getFieldValue(line, headerMap, "route_url"),
                        sortOrder = getFieldValue(line, headerMap, "route_sort_order")?.toIntOrNull()
                    )
                )
            } catch (_: Exception) { }
            line = reader.readNext()
        }
        return routes
    }

    private fun parseTrips(inputStream: InputStream): List<Trip> {
        val trips = mutableListOf<Trip>()
        val reader = CSVReaderBuilder(InputStreamReader(inputStream))
            .withSkipLines(0)
            .build()

        val header = reader.readNext() ?: return trips
        val headerMap = header.mapIndexed { index, name -> name.trim() to index }.toMap()

        var line = reader.readNext()
        while (line != null) {
            try {
                trips.add(
                    Trip(
                        tripId = getFieldValue(line, headerMap, "trip_id") ?: "",
                        routeId = getFieldValue(line, headerMap, "route_id") ?: "",
                        serviceId = getFieldValue(line, headerMap, "service_id") ?: "",
                        headsign = getFieldValue(line, headerMap, "trip_headsign"),
                        shortName = getFieldValue(line, headerMap, "trip_short_name"),
                        directionId = getFieldValue(line, headerMap, "direction_id")?.toIntOrNull(),
                        blockId = getFieldValue(line, headerMap, "block_id"),
                        shapeId = getFieldValue(line, headerMap, "shape_id"),
                        wheelchairAccessible = getFieldValue(line, headerMap, "wheelchair_accessible")?.toIntOrNull(),
                        bikesAllowed = getFieldValue(line, headerMap, "bikes_allowed")?.toIntOrNull()
                    )
                )
            } catch (_: Exception) { }
            line = reader.readNext()
        }
        return trips
    }

    private fun parseStopTimes(inputStream: InputStream): List<StopTime> {
        val stopTimes = mutableListOf<StopTime>()
        val reader = CSVReaderBuilder(InputStreamReader(inputStream))
            .withSkipLines(0)
            .build()

        val header = reader.readNext() ?: return stopTimes
        val headerMap = header.mapIndexed { index, name -> name.trim() to index }.toMap()

        var line = reader.readNext()
        while (line != null) {
            try {
                stopTimes.add(
                    StopTime(
                        tripId = getFieldValue(line, headerMap, "trip_id") ?: "",
                        arrivalTime = getFieldValue(line, headerMap, "arrival_time") ?: "",
                        departureTime = getFieldValue(line, headerMap, "departure_time"),
                        stopId = getFieldValue(line, headerMap, "stop_id") ?: "",
                        stopSequence = getFieldValue(line, headerMap, "stop_sequence")?.toIntOrNull() ?: 0,
                        stopHeadsign = getFieldValue(line, headerMap, "stop_headsign"),
                        pickupType = getFieldValue(line, headerMap, "pickup_type")?.toIntOrNull(),
                        dropOffType = getFieldValue(line, headerMap, "drop_off_type")?.toIntOrNull(),
                        shapeDistTraveled = getFieldValue(line, headerMap, "shape_dist_traveled")?.toDoubleOrNull(),
                        timepoint = getFieldValue(line, headerMap, "timepoint")?.toIntOrNull()
                    )
                )
            } catch (_: Exception) { }
            line = reader.readNext()
        }
        return stopTimes
    }

    private fun parseCalendars(inputStream: InputStream): List<Calendar> {
        val calendars = mutableListOf<Calendar>()
        val reader = CSVReaderBuilder(InputStreamReader(inputStream))
            .withSkipLines(0)
            .build()

        val header = reader.readNext() ?: return calendars
        val headerMap = header.mapIndexed { index, name -> name.trim() to index }.toMap()

        var line = reader.readNext()
        while (line != null) {
            try {
                calendars.add(
                    Calendar(
                        serviceId = getFieldValue(line, headerMap, "service_id") ?: "",
                        monday = getFieldValue(line, headerMap, "monday")?.toIntOrNull() ?: 0,
                        tuesday = getFieldValue(line, headerMap, "tuesday")?.toIntOrNull() ?: 0,
                        wednesday = getFieldValue(line, headerMap, "wednesday")?.toIntOrNull() ?: 0,
                        thursday = getFieldValue(line, headerMap, "thursday")?.toIntOrNull() ?: 0,
                        friday = getFieldValue(line, headerMap, "friday")?.toIntOrNull() ?: 0,
                        saturday = getFieldValue(line, headerMap, "saturday")?.toIntOrNull() ?: 0,
                        sunday = getFieldValue(line, headerMap, "sunday")?.toIntOrNull() ?: 0,
                        startDate = getFieldValue(line, headerMap, "start_date") ?: "",
                        endDate = getFieldValue(line, headerMap, "end_date") ?: ""
                    )
                )
            } catch (_: Exception) { }
            line = reader.readNext()
        }
        return calendars
    }

    private fun getFieldValue(
        line: Array<String>,
        headerMap: Map<String, Int>,
        fieldName: String
    ): String? {
        val index = headerMap[fieldName] ?: return null
        return if (index < line.size) {
            val value = line[index].trim()
            if (value.isEmpty()) null else value
        } else null
    }
}
