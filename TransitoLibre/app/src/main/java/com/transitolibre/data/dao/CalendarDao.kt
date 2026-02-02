package com.transitolibre.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.transitolibre.data.entity.Calendar
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(calendars: List<Calendar>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(calendar: Calendar)

    @Query("SELECT * FROM calendar")
    fun getAllCalendars(): Flow<List<Calendar>>

    @Query("SELECT * FROM calendar WHERE service_id = :serviceId")
    suspend fun getCalendarByServiceId(serviceId: String): Calendar?

    @Query("""
        SELECT * FROM calendar 
        WHERE :date BETWEEN start_date AND end_date
    """)
    suspend fun getActiveCalendars(date: String): List<Calendar>

    @Query("DELETE FROM calendar")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM calendar")
    suspend fun getCount(): Int
}
