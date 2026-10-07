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
import com.example.data.fleet.FuelRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY plateNumber ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun getVehicleById(id: Long): Flow<Vehicle?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<Vehicle>)

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Update
    suspend fun updateVehicles(vehicles: List<Vehicle>)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("DELETE FROM vehicles WHERE id IN (:ids)")
    suspend fun deleteVehiclesByIds(ids: List<Long>)

    @Query("DELETE FROM vehicles")
    suspend fun deleteAllVehicles()
}

@Dao
interface DriverDao {
    @Query("SELECT * FROM drivers ORDER BY fullName ASC")
    fun getAllDrivers(): Flow<List<Driver>>

    @Query("SELECT * FROM drivers WHERE id = :id")
    fun getDriverById(id: Long): Flow<Driver?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: Driver): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrivers(drivers: List<Driver>)

    @Update
    suspend fun updateDriver(driver: Driver)

    @Update
    suspend fun updateDrivers(drivers: List<Driver>)

    @Delete
    suspend fun deleteDriver(driver: Driver)

    @Query("DELETE FROM drivers WHERE id IN (:ids)")
    suspend fun deleteDriversByIds(ids: List<Long>)

    @Query("DELETE FROM drivers")
    suspend fun deleteAllDrivers()
}

@Dao
interface ServiceRecordDao {
    @Query("SELECT * FROM service_records ORDER BY date DESC")
    fun getAllServiceRecords(): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_records WHERE vehiclePlate = :plate ORDER BY date DESC")
    fun getRecordsForVehicle(plate: String): Flow<List<ServiceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceRecord(record: ServiceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceRecords(records: List<ServiceRecord>)

    @Update
    suspend fun updateServiceRecord(record: ServiceRecord)

    @Delete
    suspend fun deleteServiceRecord(record: ServiceRecord)

    @Query("DELETE FROM service_records")
    suspend fun deleteAllServiceRecords()
}

@Dao
interface WaybillDao {
    @Query("SELECT * FROM waybills ORDER BY date DESC")
    fun getAllWaybills(): Flow<List<Waybill>>

    @Query("SELECT * FROM waybills WHERE status = 'ISSUED' ORDER BY date DESC")
    fun getActiveWaybills(): Flow<List<Waybill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaybill(waybill: Waybill): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaybills(waybills: List<Waybill>)

    @Update
    suspend fun updateWaybill(waybill: Waybill)

    @Delete
    suspend fun deleteWaybill(waybill: Waybill)

    @Query("DELETE FROM waybills")
    suspend fun deleteAllWaybills()
}

@Dao
interface FleetDocumentDao {
    @Query("SELECT * FROM fleet_documents ORDER BY expiryDate ASC")
    fun getAllDocuments(): Flow<List<FleetDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: FleetDocument): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<FleetDocument>)

    @Update
    suspend fun updateDocument(document: FleetDocument)

    @Delete
    suspend fun deleteDocument(document: FleetDocument)

    @Query("DELETE FROM fleet_documents")
    suspend fun deleteAllDocuments()
}

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_records ORDER BY date DESC")
    fun getAllFuelRecords(): Flow<List<FuelRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelRecord(record: FuelRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelRecords(records: List<FuelRecord>)

    @Update
    suspend fun updateFuelRecord(record: FuelRecord)

    @Delete
    suspend fun deleteFuelRecord(record: FuelRecord)

    @Query("DELETE FROM fuel_records")
    suspend fun deleteAllFuelRecords()
}
