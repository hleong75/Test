package com.transitolibre.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.transitolibre.data.entity.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trips: List<Trip>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: Trip)

    @Query("SELECT * FROM trips")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE trip_id = :tripId")
    suspend fun getTripById(tripId: String): Trip?

    @Query("SELECT * FROM trips WHERE route_id = :routeId")
    suspend fun getTripsByRouteId(routeId: String): List<Trip>

    @Query("SELECT * FROM trips WHERE service_id = :serviceId")
    suspend fun getTripsByServiceId(serviceId: String): List<Trip>

    @Query("DELETE FROM trips")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun getCount(): Int
}
