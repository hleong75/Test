package com.transitolibre.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes",
    indices = [Index(value = ["agency_id"])],
    foreignKeys = [
        ForeignKey(
            entity = Agency::class,
            parentColumns = ["agency_id"],
            childColumns = ["agency_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Route(
    @PrimaryKey
    @ColumnInfo(name = "route_id")
    val routeId: String,

    @ColumnInfo(name = "agency_id")
    val agencyId: String? = null,

    @ColumnInfo(name = "route_short_name")
    val shortName: String? = null,

    @ColumnInfo(name = "route_long_name")
    val longName: String? = null,

    @ColumnInfo(name = "route_desc")
    val description: String? = null,

    @ColumnInfo(name = "route_type")
    val type: Int = 3, // Default: Bus

    @ColumnInfo(name = "route_color")
    val color: String? = null,

    @ColumnInfo(name = "route_text_color")
    val textColor: String? = null,

    @ColumnInfo(name = "route_url")
    val url: String? = null,

    @ColumnInfo(name = "route_sort_order")
    val sortOrder: Int? = null
)
