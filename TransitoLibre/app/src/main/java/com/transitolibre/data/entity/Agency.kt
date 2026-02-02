package com.transitolibre.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agencies")
data class Agency(
    @PrimaryKey
    @ColumnInfo(name = "agency_id")
    val agencyId: String,

    @ColumnInfo(name = "agency_name")
    val name: String,

    @ColumnInfo(name = "agency_url")
    val url: String? = null,

    @ColumnInfo(name = "agency_timezone")
    val timezone: String? = null,

    @ColumnInfo(name = "agency_lang")
    val lang: String? = null,

    @ColumnInfo(name = "agency_phone")
    val phone: String? = null
)
