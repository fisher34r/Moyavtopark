package com.example.data.repository

import com.example.data.fleet.Driver
import com.example.data.fleet.FleetDocument
import com.example.data.fleet.ServiceRecord
import com.example.data.fleet.Vehicle
import com.example.data.fleet.Waybill
import com.example.data.fleet.FuelRecord
import com.example.data.local.fleet.DriverDao
import com.example.data.local.fleet.FleetDocumentDao
import com.example.data.local.fleet.ServiceRecordDao
import com.example.data.local.fleet.VehicleDao
import com.example.data.local.fleet.WaybillDao
import com.example.data.local.fleet.FuelDao
import kotlinx.coroutines.flow.Flow

class FleetRepository(
    private val vehicleDao: VehicleDao,
    private val driverDao: DriverDao,
    private val serviceDao: ServiceRecordDao,
    private val waybillDao: WaybillDao,
    private val docDao: FleetDocumentDao,
    private val fuelDao: FuelDao
) {
    // 6. Fuel Records
    val allFuelRecords: Flow<List<FuelRecord>> = fuelDao.getAllFuelRecords()
    suspend fun insertFuelRecord(record: FuelRecord) = fuelDao.insertFuelRecord(record)
    suspend fun insertFuelRecords(records: List<FuelRecord>) = fuelDao.insertFuelRecords(records)
    suspend fun updateFuelRecord(record: FuelRecord) = fuelDao.updateFuelRecord(record)
    suspend fun deleteFuelRecord(record: FuelRecord) = fuelDao.deleteFuelRecord(record)
    suspend fun deleteAllFuelRecords() = fuelDao.deleteAllFuelRecords()


    // 1. Автопарк (ТС)
    val allVehicles: Flow<List<Vehicle>> = vehicleDao.getAllVehicles()
    suspend fun insertVehicle(vehicle: Vehicle) = vehicleDao.insertVehicle(vehicle)
    suspend fun insertVehicles(vehicles: List<Vehicle>) = vehicleDao.insertVehicles(vehicles)
    suspend fun updateVehicle(vehicle: Vehicle) = vehicleDao.updateVehicle(vehicle)
    suspend fun updateVehicles(vehicles: List<Vehicle>) = vehicleDao.updateVehicles(vehicles)
    suspend fun deleteVehicle(vehicle: Vehicle) = vehicleDao.deleteVehicle(vehicle)
    suspend fun deleteVehiclesByIds(ids: List<Long>) = vehicleDao.deleteVehiclesByIds(ids)
    suspend fun deleteAllVehicles() = vehicleDao.deleteAllVehicles()

    // 2. Водители
    val allDrivers: Flow<List<Driver>> = driverDao.getAllDrivers()
    suspend fun insertDriver(driver: Driver) = driverDao.insertDriver(driver)
    suspend fun insertDrivers(drivers: List<Driver>) = driverDao.insertDrivers(drivers)
    suspend fun updateDriver(driver: Driver) = driverDao.updateDriver(driver)
    suspend fun updateDrivers(drivers: List<Driver>) = driverDao.updateDrivers(drivers)
    suspend fun deleteDriver(driver: Driver) = driverDao.deleteDriver(driver)
    suspend fun deleteDriversByIds(ids: List<Long>) = driverDao.deleteDriversByIds(ids)
    suspend fun deleteAllDrivers() = driverDao.deleteAllDrivers()

    // 3. Сервис и ТО
    val allServiceRecords: Flow<List<ServiceRecord>> = serviceDao.getAllServiceRecords()
    suspend fun insertServiceRecord(record: ServiceRecord) = serviceDao.insertServiceRecord(record)
    suspend fun insertServiceRecords(records: List<ServiceRecord>) = serviceDao.insertServiceRecords(records)
    suspend fun updateServiceRecord(record: ServiceRecord) = serviceDao.updateServiceRecord(record)
    suspend fun deleteServiceRecord(record: ServiceRecord) = serviceDao.deleteServiceRecord(record)
    suspend fun deleteAllServiceRecords() = serviceDao.deleteAllServiceRecords()

    // 4. Рейсы и путевые
    val allWaybills: Flow<List<Waybill>> = waybillDao.getAllWaybills()
    suspend fun insertWaybill(waybill: Waybill) = waybillDao.insertWaybill(waybill)
    suspend fun insertWaybills(waybills: List<Waybill>) = waybillDao.insertWaybills(waybills)
    suspend fun updateWaybill(waybill: Waybill) = waybillDao.updateWaybill(waybill)
    suspend fun deleteWaybill(waybill: Waybill) = waybillDao.deleteWaybill(waybill)
    suspend fun deleteAllWaybills() = waybillDao.deleteAllWaybills()

    // 5. Документы и сроки
    val allDocuments: Flow<List<FleetDocument>> = docDao.getAllDocuments()
    suspend fun insertDocument(doc: FleetDocument) = docDao.insertDocument(doc)
    suspend fun insertDocuments(docs: List<FleetDocument>) = docDao.insertDocuments(docs)
    suspend fun updateDocument(doc: FleetDocument) = docDao.updateDocument(doc)
    suspend fun deleteDocument(doc: FleetDocument) = docDao.deleteDocument(doc)
    suspend fun deleteAllDocuments() = docDao.deleteAllDocuments()
}
