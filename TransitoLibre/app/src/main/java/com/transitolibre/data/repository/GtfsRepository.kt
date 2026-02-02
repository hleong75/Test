package com.transitolibre.data.repository

import com.transitolibre.data.dao.*
import com.transitolibre.data.entity.*
import kotlinx.coroutines.flow.Flow

class GtfsRepository(
    private val agencyDao: AgencyDao,
    private val stopDao: StopDao,
    private val routeDao: RouteDao,
    private val tripDao: TripDao,
    private val stopTimeDao: StopTimeDao,
    private val calendarDao: CalendarDao
) {
    // Agency operations
    fun getAllAgencies(): Flow<List<Agency>> = agencyDao.getAllAgencies()
    suspend fun insertAgencies(agencies: List<Agency>) = agencyDao.insertAll(agencies)
    suspend fun deleteAllAgencies() = agencyDao.deleteAll()

    // Stop operations
    fun getAllStops(): Flow<List<Stop>> = stopDao.getAllStops()
    suspend fun getStopById(stopId: String): Stop? = stopDao.getStopById(stopId)
    suspend fun searchStops(query: String): List<Stop> = stopDao.searchStops(query)
    suspend fun getNearestStops(lat: Double, lon: Double, limit: Int = 10): List<Stop> =
        stopDao.getNearestStops(lat, lon, limit)
    suspend fun getStopsInBounds(
        minLat: Double, maxLat: Double, minLon: Double, maxLon: Double
    ): List<Stop> = stopDao.getStopsInBounds(minLat, maxLat, minLon, maxLon)
    suspend fun insertStops(stops: List<Stop>) = stopDao.insertAll(stops)
    suspend fun deleteAllStops() = stopDao.deleteAll()

    // Route operations
    fun getAllRoutes(): Flow<List<Route>> = routeDao.getAllRoutes()
    suspend fun getRouteById(routeId: String): Route? = routeDao.getRouteById(routeId)
    suspend fun searchRoutes(query: String): List<Route> = routeDao.searchRoutes(query)
    suspend fun insertRoutes(routes: List<Route>) = routeDao.insertAll(routes)
    suspend fun deleteAllRoutes() = routeDao.deleteAll()

    // Trip operations
    fun getAllTrips(): Flow<List<Trip>> = tripDao.getAllTrips()
    suspend fun getTripById(tripId: String): Trip? = tripDao.getTripById(tripId)
    suspend fun getTripsByRouteId(routeId: String): List<Trip> = tripDao.getTripsByRouteId(routeId)
    suspend fun insertTrips(trips: List<Trip>) = tripDao.insertAll(trips)
    suspend fun deleteAllTrips() = tripDao.deleteAll()

    // StopTime operations
    fun getStopTimesByStopId(stopId: String): Flow<List<StopTime>> =
        stopTimeDao.getStopTimesByStopId(stopId)
    suspend fun getStopTimesByTripId(tripId: String): List<StopTime> =
        stopTimeDao.getStopTimesByTripId(tripId)
    suspend fun getNextDepartures(stopId: String, currentTime: String, limit: Int = 10): List<StopTime> =
        stopTimeDao.getNextDepartures(stopId, currentTime, limit)
    suspend fun getNextDeparturesWithRouteInfo(
        stopId: String, currentTime: String, limit: Int = 10
    ): List<DepartureInfo> = stopTimeDao.getNextDeparturesWithRouteInfo(stopId, currentTime, limit)
    suspend fun insertStopTimes(stopTimes: List<StopTime>) = stopTimeDao.insertAll(stopTimes)
    suspend fun deleteAllStopTimes() = stopTimeDao.deleteAll()

    // Calendar operations
    fun getAllCalendars(): Flow<List<Calendar>> = calendarDao.getAllCalendars()
    suspend fun getActiveCalendars(date: String): List<Calendar> = calendarDao.getActiveCalendars(date)
    suspend fun insertCalendars(calendars: List<Calendar>) = calendarDao.insertAll(calendars)
    suspend fun deleteAllCalendars() = calendarDao.deleteAll()

    // Clear all data
    suspend fun clearAllData() {
        stopTimeDao.deleteAll()
        tripDao.deleteAll()
        routeDao.deleteAll()
        stopDao.deleteAll()
        calendarDao.deleteAll()
        agencyDao.deleteAll()
    }

    // Get counts for statistics
    suspend fun getStatistics(): DatabaseStats {
        return DatabaseStats(
            agencyCount = agencyDao.getCount(),
            stopCount = stopDao.getCount(),
            routeCount = routeDao.getCount(),
            tripCount = tripDao.getCount(),
            stopTimeCount = stopTimeDao.getCount(),
            calendarCount = calendarDao.getCount()
        )
    }
}

data class DatabaseStats(
    val agencyCount: Int,
    val stopCount: Int,
    val routeCount: Int,
    val tripCount: Int,
    val stopTimeCount: Int,
    val calendarCount: Int
)
