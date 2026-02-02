package com.transitolibre.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.transitolibre.data.entity.Agency
import kotlinx.coroutines.flow.Flow

@Dao
interface AgencyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(agencies: List<Agency>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(agency: Agency)

    @Query("SELECT * FROM agencies")
    fun getAllAgencies(): Flow<List<Agency>>

    @Query("SELECT * FROM agencies WHERE agency_id = :agencyId")
    suspend fun getAgencyById(agencyId: String): Agency?

    @Query("DELETE FROM agencies")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM agencies")
    suspend fun getCount(): Int
}
