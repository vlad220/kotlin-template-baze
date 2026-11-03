package com.template.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "taxi_addresses")
data class SavedAddressEntity(
    @PrimaryKey val text: String,
    val lat: Double,
    val lon: Double,
    val usedAt: Long
)
