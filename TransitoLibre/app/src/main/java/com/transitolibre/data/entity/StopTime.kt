package com.transitolibre.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "stop_times",
    primaryKeys = ["trip_id", "stop_sequence"],
    indices = [
        Index(value = ["stop_id"]),
        Index(value = ["trip_id"]),
        Index(value = ["arrival_time"]),
        Index(value = ["stop_id", "arrival_time"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["trip_id"],
            childColumns = ["trip_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Stop::class,
            parentColumns = ["stop_id"],
            childColumns = ["stop_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StopTime(
    @ColumnInfo(name = "trip_id")
    val tripId: String,

    @ColumnInfo(name = "arrival_time")
    val arrivalTime: String,

    @ColumnInfo(name = "departure_time")
    val departureTime: String? = null,

    @ColumnInfo(name = "stop_id")
    val stopId: String,

    @ColumnInfo(name = "stop_sequence")
    val stopSequence: Int,

    @ColumnInfo(name = "stop_headsign")
    val stopHeadsign: String? = null,

    @ColumnInfo(name = "pickup_type")
    val pickupType: Int? = null,

    @ColumnInfo(name = "drop_off_type")
    val dropOffType: Int? = null,

    @ColumnInfo(name = "shape_dist_traveled")
    val shapeDistTraveled: Double? = null,

    @ColumnInfo(name = "timepoint")
    val timepoint: Int? = null
)
