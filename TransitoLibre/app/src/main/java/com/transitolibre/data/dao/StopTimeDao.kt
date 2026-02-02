package com.transitolibre.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.transitolibre.data.entity.StopTime
import kotlinx.coroutines.flow.Flow

@Dao
interface StopTimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stopTimes: List<StopTime>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stopTime: StopTime)

    @Query("SELECT * FROM stop_times WHERE stop_id = :stopId ORDER BY arrival_time")
    fun getStopTimesByStopId(stopId: String): Flow<List<StopTime>>

    @Query("SELECT * FROM stop_times WHERE trip_id = :tripId ORDER BY stop_sequence")
    suspend fun getStopTimesByTripId(tripId: String): List<StopTime>

    @Query("""
        SELECT st.* FROM stop_times st
        INNER JOIN trips t ON st.trip_id = t.trip_id
        WHERE st.stop_id = :stopId 
        AND st.arrival_time >= :currentTime
        ORDER BY st.arrival_time
        LIMIT :limit
    """)
    suspend fun getNextDepartures(
        stopId: String,
        currentTime: String,
        limit: Int = 10
    ): List<StopTime>

    @Query("""
        SELECT st.*, r.route_short_name, r.route_long_name, r.route_color, t.trip_headsign
        FROM stop_times st
        INNER JOIN trips t ON st.trip_id = t.trip_id
        INNER JOIN routes r ON t.route_id = r.route_id
        WHERE st.stop_id = :stopId 
        AND st.arrival_time >= :currentTime
        ORDER BY st.arrival_time
        LIMIT :limit
    """)
    suspend fun getNextDeparturesWithRouteInfo(
        stopId: String,
        currentTime: String,
        limit: Int = 10
    ): List<DepartureInfo>

    @Query("DELETE FROM stop_times")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM stop_times")
    suspend fun getCount(): Int
}

data class DepartureInfo(
    @androidx.room.ColumnInfo(name = "trip_id")
    val tripId: String,
    @androidx.room.ColumnInfo(name = "arrival_time")
    val arrivalTime: String,
    @androidx.room.ColumnInfo(name = "departure_time")
    val departureTime: String?,
    @androidx.room.ColumnInfo(name = "stop_id")
    val stopId: String,
    @androidx.room.ColumnInfo(name = "stop_sequence")
    val stopSequence: Int,
    @androidx.room.ColumnInfo(name = "route_short_name")
    val routeShortName: String?,
    @androidx.room.ColumnInfo(name = "route_long_name")
    val routeLongName: String?,
    @androidx.room.ColumnInfo(name = "route_color")
    val routeColor: String?,
    @androidx.room.ColumnInfo(name = "trip_headsign")
    val tripHeadsign: String?
)
