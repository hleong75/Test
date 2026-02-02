package com.transitolibre.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar",
    indices = [Index(value = ["start_date", "end_date"])]
)
data class Calendar(
    @PrimaryKey
    @ColumnInfo(name = "service_id")
    val serviceId: String,

    @ColumnInfo(name = "monday")
    val monday: Int,

    @ColumnInfo(name = "tuesday")
    val tuesday: Int,

    @ColumnInfo(name = "wednesday")
    val wednesday: Int,

    @ColumnInfo(name = "thursday")
    val thursday: Int,

    @ColumnInfo(name = "friday")
    val friday: Int,

    @ColumnInfo(name = "saturday")
    val saturday: Int,

    @ColumnInfo(name = "sunday")
    val sunday: Int,

    @ColumnInfo(name = "start_date")
    val startDate: String,

    @ColumnInfo(name = "end_date")
    val endDate: String
)
