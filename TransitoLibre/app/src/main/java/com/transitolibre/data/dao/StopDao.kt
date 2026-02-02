package com.transitolibre.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.transitolibre.data.entity.Stop
import kotlinx.coroutines.flow.Flow

@Dao
interface StopDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<Stop>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stop: Stop)

    @Query("SELECT * FROM stops")
    fun getAllStops(): Flow<List<Stop>>

    @Query("SELECT * FROM stops WHERE stop_id = :stopId")
    suspend fun getStopById(stopId: String): Stop?

    @Query("SELECT * FROM stops WHERE stop_name LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchStops(query: String, limit: Int = 20): List<Stop>

    @Query("""
        SELECT * FROM stops 
        WHERE stop_lat BETWEEN :minLat AND :maxLat 
        AND stop_lon BETWEEN :minLon AND :maxLon
    """)
    suspend fun getStopsInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<Stop>

    @Query("""
        SELECT *, 
        ((:lat - stop_lat) * (:lat - stop_lat) + (:lon - stop_lon) * (:lon - stop_lon)) as distance
        FROM stops
        ORDER BY distance
        LIMIT :limit
    """)
    suspend fun getNearestStops(lat: Double, lon: Double, limit: Int = 10): List<Stop>

    @Query("DELETE FROM stops")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM stops")
    suspend fun getCount(): Int
}
