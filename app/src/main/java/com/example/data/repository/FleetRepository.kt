package com.example.data.repository

import com.example.data.fleet.Driver
import com.example.data.fleet.FleetDocument
import com.example.data.fleet.ServiceRecord
import com.example.data.fleet.Vehicle
import com.example.data.fleet.Waybill
import com.example.data.local.fleet.DriverDao
import com.example.data.local.fleet.FleetDocumentDao
import com.example.data.local.fleet.ServiceRecordDao
import com.example.data.local.fleet.VehicleDao
import com.example.data.local.fleet.WaybillDao
import kotlinx.coroutines.flow.Flow

class FleetRepository(
    private val vehicleDao: VehicleDao,
    private val driverDao: DriverDao,
    private val serviceDao: ServiceRecordDao,
    private val waybillDao: WaybillDao,
    private val docDao: FleetDocumentDao
) {
    // 1. Автопарк (ТС)
    val allVehicles: Flow<List<Vehicle>> = vehicleDao.getAllVehicles()
    suspend fun insertVehicle(vehicle: Vehicle) = vehicleDao.insertVehicle(vehicle)
    suspend fun updateVehicle(vehicle: Vehicle) = vehicleDao.updateVehicle(vehicle)
    suspend fun deleteVehicle(vehicle: Vehicle) = vehicleDao.deleteVehicle(vehicle)

    // 2. Водители
    val allDrivers: Flow<List<Driver>> = driverDao.getAllDrivers()
    suspend fun insertDriver(driver: Driver) = driverDao.insertDriver(driver)
    suspend fun updateDriver(driver: Driver) = driverDao.updateDriver(driver)
    suspend fun deleteDriver(driver: Driver) = driverDao.deleteDriver(driver)

    // 3. Сервис и ТО
    val allServiceRecords: Flow<List<ServiceRecord>> = serviceDao.getAllServiceRecords()
    suspend fun insertServiceRecord(record: ServiceRecord) = serviceDao.insertServiceRecord(record)
    suspend fun updateServiceRecord(record: ServiceRecord) = serviceDao.updateServiceRecord(record)
    suspend fun deleteServiceRecord(record: ServiceRecord) = serviceDao.deleteServiceRecord(record)

    // 4. Рейсы и путевые
    val allWaybills: Flow<List<Waybill>> = waybillDao.getAllWaybills()
    suspend fun insertWaybill(waybill: Waybill) = waybillDao.insertWaybill(waybill)
    suspend fun updateWaybill(waybill: Waybill) = waybillDao.updateWaybill(waybill)
    suspend fun deleteWaybill(waybill: Waybill) = waybillDao.deleteWaybill(waybill)

    // 5. Документы и сроки
    val allDocuments: Flow<List<FleetDocument>> = docDao.getAllDocuments()
    suspend fun insertDocument(doc: FleetDocument) = docDao.insertDocument(doc)
    suspend fun updateDocument(doc: FleetDocument) = docDao.updateDocument(doc)
    suspend fun deleteDocument(doc: FleetDocument) = docDao.deleteDocument(doc)
}
