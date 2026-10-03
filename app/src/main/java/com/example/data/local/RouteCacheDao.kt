package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RouteCacheEntity

@Dao
interface RouteCacheDao {
    @Query("SELECT * FROM route_cache WHERE originNormalized = :origin AND destinationNormalized = :dest LIMIT 1")
    suspend fun getCachedRoute(origin: String, dest: String): RouteCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteCacheEntity)

    @Query("DELETE FROM route_cache")
    suspend fun clearCache()

    @Query("SELECT COUNT(*) FROM route_cache")
    suspend fun getCacheCount(): Int
}
