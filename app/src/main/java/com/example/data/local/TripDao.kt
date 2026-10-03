package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY loadingDate DESC, id DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun getTripById(id: Long): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripByIdOnce(id: Long): Trip?

    @Query("SELECT * FROM trips ORDER BY id DESC LIMIT 1")
    fun getLatestTrip(): Flow<Trip?>

    @Query("SELECT * FROM trips ORDER BY id DESC LIMIT 1")
    suspend fun getLatestTripOnce(): Trip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTripById(id: Long)

    @Query("DELETE FROM trips")
    suspend fun deleteAllTrips()

    @Query("SELECT COUNT(*) FROM trips")
    fun getTripCount(): Flow<Int>

    @Query("SELECT * FROM trips")
    suspend fun getAllTripsList(): List<Trip>

    @Update
    suspend fun updateTrips(trips: List<Trip>)

    @Query("SELECT DISTINCT loadingLocation FROM trips WHERE loadingLocation != '' UNION SELECT DISTINCT unloadingLocation FROM trips WHERE unloadingLocation != ''")
    fun getAllKnownLocations(): Flow<List<String>>

    @Query("SELECT DISTINCT customerName FROM trips WHERE customerName != ''")
    fun getAllKnownCustomers(): Flow<List<String>>
}
