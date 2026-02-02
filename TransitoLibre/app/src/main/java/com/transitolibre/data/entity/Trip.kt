package com.transitolibre.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trips",
    indices = [
        Index(value = ["route_id"]),
        Index(value = ["service_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["route_id"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Trip(
    @PrimaryKey
    @ColumnInfo(name = "trip_id")
    val tripId: String,

    @ColumnInfo(name = "route_id")
    val routeId: String,

    @ColumnInfo(name = "service_id")
    val serviceId: String,

    @ColumnInfo(name = "trip_headsign")
    val headsign: String? = null,

    @ColumnInfo(name = "trip_short_name")
    val shortName: String? = null,

    @ColumnInfo(name = "direction_id")
    val directionId: Int? = null,

    @ColumnInfo(name = "block_id")
    val blockId: String? = null,

    @ColumnInfo(name = "shape_id")
    val shapeId: String? = null,

    @ColumnInfo(name = "wheelchair_accessible")
    val wheelchairAccessible: Int? = null,

    @ColumnInfo(name = "bikes_allowed")
    val bikesAllowed: Int? = null
)
