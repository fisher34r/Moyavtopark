package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.fleet.DocumentType
import com.example.data.fleet.Driver
import com.example.data.fleet.DriverStatus
import com.example.data.fleet.FleetDocument
import com.example.data.fleet.ServiceRecord
import com.example.data.fleet.ServiceType
import com.example.data.fleet.Vehicle
import com.example.data.fleet.VehicleStatus
import com.example.data.fleet.VehicleType
import com.example.data.fleet.Waybill
import com.example.data.fleet.WaybillStatus
import com.example.data.model.Trip
import com.example.data.repository.FleetRepository
import com.example.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FleetAnalyticsSummary(
    val totalVehicles: Int = 0,
    val activeVehiclesOnTrip: Int = 0,
    val availableVehicles: Int = 0,
    val vehiclesInService: Int = 0,
    val totalDrivers: Int = 0,
    val totalServiceCost: Double = 0.0,
    val totalFuelCost: Double = 0.0,
    val totalFuelLiters: Double = 0.0,
    val totalDocumentsCost: Double = 0.0,
    val totalDriverSalary: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val netProfit: Double = 0.0,
    val driverSalaryShareOfRevenue: Double = 0.0,
    val totalTco: Double = 0.0, // TCO = топливо + ФОТ водителей + сервис + документы + прочее
    val totalFleetDistanceKm: Double = 0.0,
    val costPerKm: Double = 0.0, // Удельный расход на 1 км
    val expiredDocsCount: Int = 0,
    val expiringDocsCount: Int = 0
)

class FleetViewModel(
    private val fleetRepository: FleetRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    // Tab Selection (0 to 5)
    val selectedTab = MutableStateFlow(0)

    // Data streams
    val vehicles: StateFlow<List<Vehicle>> = fleetRepository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drivers: StateFlow<List<Driver>> = fleetRepository.allDrivers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serviceRecords: StateFlow<List<ServiceRecord>> = fleetRepository.allServiceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waybills: StateFlow<List<Waybill>> = fleetRepository.allWaybills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<FleetDocument>> = fleetRepository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fuelRecords: StateFlow<List<com.example.data.fleet.FuelRecord>> = fleetRepository.allFuelRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trips: StateFlow<List<Trip>> = tripRepository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 1. Фильтры ТС
    val vehicleSearchQuery = MutableStateFlow("")
    val vehicleTypeFilter = MutableStateFlow<VehicleType?>(null)
    val vehicleStatusFilter = MutableStateFlow<VehicleStatus?>(null)

    val filteredVehicles: StateFlow<List<Vehicle>> = combine(
        vehicles,
        vehicleSearchQuery,
        vehicleTypeFilter,
        vehicleStatusFilter
    ) { list, query, type, status ->
        list.filter { v ->
            val matchQuery = query.isBlank() ||
                    v.plateNumber.contains(query, ignoreCase = true) ||
                    v.model.contains(query, ignoreCase = true) ||
                    v.vin.contains(query, ignoreCase = true) ||
                    v.assignedDriverName.contains(query, ignoreCase = true)
            val matchType = type == null || v.type == type
            val matchStatus = status == null || v.status == status
            matchQuery && matchType && matchStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Фильтры Водителей
    val driverSearchQuery = MutableStateFlow("")
    val driverStatusFilter = MutableStateFlow<DriverStatus?>(null)

    val filteredDrivers: StateFlow<List<Driver>> = combine(
        drivers,
        driverSearchQuery,
        driverStatusFilter
    ) { list, query, status ->
        list.filter { d ->
            val matchQuery = query.isBlank() ||
                    d.fullName.contains(query, ignoreCase = true) ||
                    d.phone.contains(query, ignoreCase = true) ||
                    d.licenseNumber.contains(query, ignoreCase = true) ||
                    d.assignedVehiclePlate.contains(query, ignoreCase = true)
            val matchStatus = status == null || d.status == status
            matchQuery && matchStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Фильтры Сервис и ТО
    val serviceTypeFilter = MutableStateFlow<ServiceType?>(null)
    val filteredServiceRecords: StateFlow<List<ServiceRecord>> = combine(
        serviceRecords,
        serviceTypeFilter
    ) { list, type ->
        list.filter { s ->
            type == null || s.serviceType == type
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Фильтры Документов
    val docFilterExpiringOnly = MutableStateFlow(false)
    val filteredDocuments: StateFlow<List<FleetDocument>> = combine(
        documents,
        docFilterExpiringOnly
    ) { list, expiringOnly ->
        if (expiringOnly) {
            list.filter { it.isExpired || it.isExpiringSoon }
        } else {
            list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Аналитика и затраты (TCO, удельный расход на 1 км, расход ГСМ)
    val analyticsDateStart = MutableStateFlow<Long?>(null)
    val analyticsDateEnd = MutableStateFlow<Long?>(null)
    val analyticsVehiclePlate = MutableStateFlow<String?>(null)
    val analyticsDriverName = MutableStateFlow<String?>(null)

    val analyticsSummary: StateFlow<FleetAnalyticsSummary> = combine(
        combine(vehicles, drivers, analyticsVehiclePlate, analyticsDriverName) { v, d, p, dn -> 
            val filteredV = if (p != null) v.filter { it.plateNumber == p } else v
            val filteredD = if (dn != null) d.filter { it.fullName == dn } else d
            Pair(filteredV, filteredD)
        },
        combine(serviceRecords, documents, fuelRecords) { s, doc, f -> 
            Triple(s, doc, f)
        },
        combine(trips, waybills, analyticsDateStart, analyticsDateEnd) { t, w, ds, de -> 
            val filteredT = t.filter {
                (ds == null || it.loadingDate >= ds) && (de == null || it.loadingDate <= de)
            }
            val filteredW = w.filter {
                (ds == null || it.date >= ds) && (de == null || it.date <= de)
            }
            Pair(filteredT, filteredW)
        }
    ) { (vList, dList), (sListRaw, docListRaw, fListRaw), (tList, wList) ->
        val plateFilter = analyticsVehiclePlate.value
        val ds = analyticsDateStart.value
        val de = analyticsDateEnd.value

        val sList = sListRaw.filter {
            (plateFilter == null || it.vehiclePlate == plateFilter) &&
            (ds == null || it.date >= ds) && (de == null || it.date <= de)
        }
        val docList = docListRaw.filter {
            (plateFilter == null || it.vehiclePlateOrDriver == plateFilter) &&
            (ds == null || it.issueDate >= ds) && (de == null || it.issueDate <= de)
        }
        val fList = fListRaw.filter {
            (plateFilter == null || it.vehiclePlate == plateFilter) &&
            (ds == null || it.date >= ds) && (de == null || it.date <= de)
        }

        val totalVehicles = vList.size
        val onTrip = vList.count { it.status == VehicleStatus.ACTIVE }
        val available = vList.count { it.status == VehicleStatus.AVAILABLE }
        val inService = vList.count { it.status == VehicleStatus.SERVICE }

        val serviceTotal = sList.sumOf { it.cost }
        val docsTotal = docList.sumOf { it.cost }
        val fuelTotal = fList.sumOf { it.totalCost }
        val fuelLitersTotal = fList.sumOf { it.liters }

        // Filter trips by driver if selected
        val driverName = analyticsDriverName.value
        val finalTList = if (driverName != null) tList.filter { it.driverName == driverName } else tList

        val tripsDistance = finalTList.sumOf { it.distanceKm }
        val waybillsDistance = wList.sumOf { it.totalDistanceKm }

        val rawDist = maxOf(tripsDistance, waybillsDistance)
        val totalDist = if (rawDist > 0.0) rawDist else 1.0
        
        val totalFuelCost = fuelTotal // Use ONLY fuel receipts as requested
        val totalFuelLiters = fuelLitersTotal

        val tripsDriverSalary = finalTList.sumOf { it.effectiveDriverSalary }
        val totalRevenue = finalTList.sumOf { it.totalPrice }
        val otherExpenses = finalTList.sumOf { it.otherExpenses }
        val totalTco = totalFuelCost + serviceTotal + docsTotal + otherExpenses + tripsDriverSalary
        val costPerKm = if (totalDist > 0.0) totalTco / totalDist else 0.0
        val netProfit = totalRevenue - totalTco
        val driverSalaryShare = if (totalRevenue > 0.0) (tripsDriverSalary / totalRevenue) * 100.0 else 0.0

        val expiredDocs = docList.count { it.isExpired }
        val expiringDocs = docList.count { it.isExpiringSoon }

        FleetAnalyticsSummary(
            totalVehicles = totalVehicles,
            activeVehiclesOnTrip = onTrip,
            availableVehicles = available,
            vehiclesInService = inService,
            totalDrivers = dList.size,
            totalServiceCost = serviceTotal,
            totalFuelCost = totalFuelCost,
            totalFuelLiters = totalFuelLiters,
            totalDocumentsCost = docsTotal,
            totalDriverSalary = tripsDriverSalary,
            totalRevenue = totalRevenue,
            netProfit = netProfit,
            driverSalaryShareOfRevenue = driverSalaryShare,
            totalTco = totalTco,
            totalFleetDistanceKm = totalDist,
            costPerKm = costPerKm,
            expiredDocsCount = expiredDocs,
            expiringDocsCount = expiringDocs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FleetAnalyticsSummary())

    // Actions for Vehicle
    fun saveVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            if (vehicle.id == 0L) {
                fleetRepository.insertVehicle(vehicle)
            } else {
                fleetRepository.updateVehicle(vehicle)
            }
            // Synchronize dependency between transport and drivers
            if (vehicle.assignedDriverName.isNotBlank()) {
                val driver = drivers.value.firstOrNull { it.fullName.equals(vehicle.assignedDriverName, ignoreCase = true) }
                if (driver != null && driver.assignedVehiclePlate != vehicle.plateNumber) {
                    fleetRepository.updateDriver(driver.copy(assignedVehiclePlate = vehicle.plateNumber))
                }
            }
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            fleetRepository.deleteVehicle(vehicle)
        }
    }

    fun batchUpdateVehicles(
        vehicleIds: Set<Long>,
        newStatus: VehicleStatus? = null,
        newAssignedDriverName: String? = null
    ) {
        viewModelScope.launch {
            val allV = vehicles.value
            val toUpdate = allV.filter { it.id in vehicleIds }.map { v ->
                v.copy(
                    status = newStatus ?: v.status,
                    assignedDriverName = newAssignedDriverName ?: v.assignedDriverName
                )
            }
            fleetRepository.updateVehicles(toUpdate)

            // Если назначен водитель, синхронизируем водителя
            if (newAssignedDriverName != null) {
                val driver = drivers.value.firstOrNull { it.fullName.equals(newAssignedDriverName, ignoreCase = true) }
                if (driver != null) {
                    val firstPlate = toUpdate.firstOrNull()?.plateNumber ?: ""
                    fleetRepository.updateDriver(driver.copy(assignedVehiclePlate = firstPlate))
                }
            }
        }
    }

    fun batchDeleteVehicles(vehicleIds: Set<Long>) {
        viewModelScope.launch {
            fleetRepository.deleteVehiclesByIds(vehicleIds.toList())
        }
    }

    // Actions for Driver
    fun saveDriver(driver: Driver) {
        viewModelScope.launch {
            if (driver.id == 0L) {
                fleetRepository.insertDriver(driver)
            } else {
                fleetRepository.updateDriver(driver)
            }
            // Synchronize dependency between transport and drivers
            if (driver.assignedVehiclePlate.isNotBlank()) {
                val vehicle = vehicles.value.firstOrNull { it.plateNumber.equals(driver.assignedVehiclePlate, ignoreCase = true) }
                if (vehicle != null && vehicle.assignedDriverName != driver.fullName) {
                    fleetRepository.updateVehicle(vehicle.copy(assignedDriverName = driver.fullName))
                }
            }
        }
    }

    fun updateDriverStatus(driver: Driver, newStatus: DriverStatus) {
        viewModelScope.launch {
            fleetRepository.updateDriver(driver.copy(status = newStatus))
        }
    }

    fun deleteDriver(driver: Driver) {
        viewModelScope.launch {
            fleetRepository.deleteDriver(driver)
        }
    }

    fun batchUpdateDrivers(
        driverIds: Set<Long>,
        newStatus: DriverStatus? = null,
        newSalaryPercent: Double? = null,
        newShiftSchedule: String? = null
    ) {
        viewModelScope.launch {
            val allD = drivers.value
            val toUpdate = allD.filter { it.id in driverIds }.map { d ->
                d.copy(
                    status = newStatus ?: d.status,
                    salaryPercent = newSalaryPercent ?: d.salaryPercent,
                    shiftSchedule = newShiftSchedule ?: d.shiftSchedule
                )
            }
            fleetRepository.updateDrivers(toUpdate)
        }
    }

    fun batchDeleteDrivers(driverIds: Set<Long>) {
        viewModelScope.launch {
            fleetRepository.deleteDriversByIds(driverIds.toList())
        }
    }

    // Actions for Service
    fun saveServiceRecord(record: ServiceRecord) {
        viewModelScope.launch {
            if (record.id == 0L) {
                fleetRepository.insertServiceRecord(record)
            } else {
                fleetRepository.updateServiceRecord(record)
            }
        }
    }

    fun deleteServiceRecord(record: ServiceRecord) {
        viewModelScope.launch {
            fleetRepository.deleteServiceRecord(record)
        }
    }

    // Actions for Waybill
    fun saveWaybill(waybill: Waybill) {
        viewModelScope.launch {
            if (waybill.id == 0L) {
                fleetRepository.insertWaybill(waybill)
            } else {
                fleetRepository.updateWaybill(waybill)
            }
        }
    }

    fun closeWaybill(waybill: Waybill, endOdometer: Double, endFuel: Double) {
        viewModelScope.launch {
            fleetRepository.updateWaybill(
                waybill.copy(
                    endOdometerKm = endOdometer,
                    endFuelLiters = endFuel,
                    status = WaybillStatus.CLOSED
                )
            )
        }
    }

    fun deleteWaybill(waybill: Waybill) {
        viewModelScope.launch {
            fleetRepository.deleteWaybill(waybill)
        }
    }

    // Actions for FleetDocument
    fun saveDocument(doc: FleetDocument) {
        viewModelScope.launch {
            if (doc.id == 0L) {
                fleetRepository.insertDocument(doc)
            } else {
                fleetRepository.updateDocument(doc)
            }
        }
    }

    fun deleteDocument(doc: FleetDocument) {
        viewModelScope.launch {
            fleetRepository.deleteDocument(doc)
        }
    }

    fun batchDeleteDocuments(ids: Set<Long>) {
        viewModelScope.launch {
            fleetRepository.deleteDocumentsByIds(ids.toList())
        }
    }

    fun batchUpdateDocuments(
        ids: Set<Long>,
        newVehiclePlateOrDriver: String? = null,
        newIssuingAuthority: String? = null
    ) {
        viewModelScope.launch {
            val allD = documents.value
            val toUpdate = allD.filter { it.id in ids }.map { doc ->
                doc.copy(
                    vehiclePlateOrDriver = newVehiclePlateOrDriver ?: doc.vehiclePlateOrDriver,
                    issuingAuthority = newIssuingAuthority ?: doc.issuingAuthority
                )
            }
            fleetRepository.updateDocuments(toUpdate)
        }
    }

    // Actions for FuelRecord
    fun saveFuelRecord(record: com.example.data.fleet.FuelRecord) {
        viewModelScope.launch {
            if (record.id == 0L) {
                fleetRepository.insertFuelRecord(record)
                fleetRepository.adjustVehicleFuel(record.vehiclePlate, record.liters)
            } else {
                val oldRecord = fuelRecords.value.find { it.id == record.id }
                if (oldRecord != null) {
                    if (oldRecord.vehiclePlate == record.vehiclePlate) {
                        val delta = record.liters - oldRecord.liters
                        fleetRepository.adjustVehicleFuel(record.vehiclePlate, delta)
                    } else {
                        fleetRepository.adjustVehicleFuel(oldRecord.vehiclePlate, -oldRecord.liters)
                        fleetRepository.adjustVehicleFuel(record.vehiclePlate, record.liters)
                    }
                }
                fleetRepository.updateFuelRecord(record)
            }
        }
    }

    fun deleteFuelRecord(record: com.example.data.fleet.FuelRecord) {
        viewModelScope.launch {
            fleetRepository.adjustVehicleFuel(record.vehiclePlate, -record.liters)
            fleetRepository.deleteFuelRecord(record)
        }
    }

    fun batchDeleteFuelRecords(ids: Set<Long>) {
        viewModelScope.launch {
            val recordsToDelete = fuelRecords.value.filter { it.id in ids }
            recordsToDelete.forEach { r ->
                fleetRepository.adjustVehicleFuel(r.vehiclePlate, -r.liters)
            }
            fleetRepository.deleteFuelRecordsByIds(ids.toList())
        }
    }

    fun batchUpdateFuelRecords(
        ids: Set<Long>,
        newVehiclePlate: String? = null,
        newStationName: String? = null
    ) {
        viewModelScope.launch {
            val allF = fuelRecords.value
            val toUpdate = allF.filter { it.id in ids }.map { r ->
                r.copy(
                    vehiclePlate = newVehiclePlate ?: r.vehiclePlate,
                    stationName = newStationName ?: r.stationName
                )
            }
            fleetRepository.updateFuelRecords(toUpdate)
        }
    }
}

class FleetViewModelFactory(
    private val fleetRepository: FleetRepository,
    private val tripRepository: TripRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FleetViewModel::class.java)) {
            return FleetViewModel(fleetRepository, tripRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
