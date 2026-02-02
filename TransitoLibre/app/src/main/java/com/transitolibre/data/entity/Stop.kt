package com.transitolibre.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stops",
    indices = [
        Index(value = ["stop_lat", "stop_lon"]),
        Index(value = ["stop_name"])
    ]
)
data class Stop(
    @PrimaryKey
    @ColumnInfo(name = "stop_id")
    val stopId: String,

    @ColumnInfo(name = "stop_name")
    val name: String,

    @ColumnInfo(name = "stop_lat")
    val lat: Double,

    @ColumnInfo(name = "stop_lon")
    val lon: Double,

    @ColumnInfo(name = "stop_code")
    val code: String? = null,

    @ColumnInfo(name = "stop_desc")
    val description: String? = null,

    @ColumnInfo(name = "zone_id")
    val zoneId: String? = null,

    @ColumnInfo(name = "stop_url")
    val url: String? = null,

    @ColumnInfo(name = "location_type")
    val locationType: Int? = null,

    @ColumnInfo(name = "parent_station")
    val parentStation: String? = null,

    @ColumnInfo(name = "wheelchair_boarding")
    val wheelchairBoarding: Int? = null
)
