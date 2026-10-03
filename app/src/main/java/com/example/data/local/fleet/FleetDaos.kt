package com.example.data.local.fleet

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.fleet.Driver
import com.example.data.fleet.FleetDocument
import com.example.data.fleet.ServiceRecord
import com.example.data.fleet.Vehicle
import com.example.data.fleet.Waybill
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY plateNumber ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun getVehicleById(id: Long): Flow<Vehicle?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)
}

@Dao
interface DriverDao {
    @Query("SELECT * FROM drivers ORDER BY fullName ASC")
    fun getAllDrivers(): Flow<List<Driver>>

    @Query("SELECT * FROM drivers WHERE id = :id")
    fun getDriverById(id: Long): Flow<Driver?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: Driver): Long

    @Update
    suspend fun updateDriver(driver: Driver)

    @Delete
    suspend fun deleteDriver(driver: Driver)
}

@Dao
interface ServiceRecordDao {
    @Query("SELECT * FROM service_records ORDER BY date DESC")
    fun getAllServiceRecords(): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_records WHERE vehiclePlate = :plate ORDER BY date DESC")
    fun getRecordsForVehicle(plate: String): Flow<List<ServiceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceRecord(record: ServiceRecord): Long

    @Update
    suspend fun updateServiceRecord(record: ServiceRecord)

    @Delete
    suspend fun deleteServiceRecord(record: ServiceRecord)
}

@Dao
interface WaybillDao {
    @Query("SELECT * FROM waybills ORDER BY date DESC")
    fun getAllWaybills(): Flow<List<Waybill>>

    @Query("SELECT * FROM waybills WHERE status = 'ISSUED' ORDER BY date DESC")
    fun getActiveWaybills(): Flow<List<Waybill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaybill(waybill: Waybill): Long

    @Update
    suspend fun updateWaybill(waybill: Waybill)

    @Delete
    suspend fun deleteWaybill(waybill: Waybill)
}

@Dao
interface FleetDocumentDao {
    @Query("SELECT * FROM fleet_documents ORDER BY expiryDate ASC")
    fun getAllDocuments(): Flow<List<FleetDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: FleetDocument): Long

    @Update
    suspend fun updateDocument(document: FleetDocument)

    @Delete
    suspend fun deleteDocument(document: FleetDocument)
}
