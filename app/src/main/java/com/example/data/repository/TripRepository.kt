package com.example.data.repository

import com.example.data.local.SettingsPreferences
import com.example.data.local.TripDao
import com.example.data.model.Trip
import kotlinx.coroutines.flow.Flow

class TripRepository(
    private val tripDao: TripDao,
    val settingsPreferences: SettingsPreferences
) {
    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips()

    fun getTripById(id: Long): Flow<Trip?> = tripDao.getTripById(id)

    suspend fun getTripByIdOnce(id: Long): Trip? = tripDao.getTripByIdOnce(id)

    fun getLatestTrip(): Flow<Trip?> = tripDao.getLatestTrip()

    suspend fun getLatestTripOnce(): Trip? = tripDao.getLatestTripOnce()

    suspend fun insertTrip(trip: Trip): Long {
        val id = tripDao.insertTrip(trip)
        settingsPreferences.saveLastTripData(trip.copy(id = id))
        return id
    }

    suspend fun insertTrips(trips: List<Trip>): List<Long> {
        val ids = tripDao.insertTrips(trips)
        trips.lastOrNull()?.let { last ->
            settingsPreferences.saveLastTripData(last)
        }
        return ids
    }

    suspend fun updateTrip(trip: Trip) {
        tripDao.updateTrip(trip)
        settingsPreferences.saveLastTripData(trip)
    }

    suspend fun getAllTripsList(): List<Trip> = tripDao.getAllTripsList()

    suspend fun updateTrips(trips: List<Trip>) = tripDao.updateTrips(trips)

    suspend fun deleteTrip(trip: Trip) = tripDao.deleteTrip(trip)

    suspend fun deleteTripById(id: Long) = tripDao.deleteTripById(id)

    suspend fun deleteAllTrips() = tripDao.deleteAllTrips()

    val knownLocations: Flow<List<String>> = tripDao.getAllKnownLocations()

    val knownCustomers: Flow<List<String>> = tripDao.getAllKnownCustomers()
}
