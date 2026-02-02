package com.transitolibre.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.transitolibre.data.entity.Route
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routes: List<Route>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: Route)

    @Query("SELECT * FROM routes")
    fun getAllRoutes(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE route_id = :routeId")
    suspend fun getRouteById(routeId: String): Route?

    @Query("SELECT * FROM routes WHERE route_short_name LIKE '%' || :query || '%' OR route_long_name LIKE '%' || :query || '%'")
    suspend fun searchRoutes(query: String): List<Route>

    @Query("DELETE FROM routes")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM routes")
    suspend fun getCount(): Int
}
