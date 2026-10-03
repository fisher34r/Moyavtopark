package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_cache",
    indices = [Index(value = ["originNormalized", "destinationNormalized"], unique = true)]
)
data class RouteCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originNormalized: String,
    val destinationNormalized: String,
    val distanceKm: Double,
    val provider: String,
    val cachedAt: Long = System.currentTimeMillis()
)
