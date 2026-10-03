package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupService
import com.example.data.backup.RestoreResult
import com.example.data.fleet.Driver
import com.example.data.fleet.Vehicle
import com.example.data.local.SettingsPreferences
import com.example.data.model.RateType
import com.example.data.model.Trip
import com.example.data.model.TripStatus
import com.example.data.network.LocationService
import com.example.data.repository.FleetRepository
import com.example.data.repository.TripRepository
import com.example.util.RouteResult
import com.example.util.RoutingService
import com.example.util.TripNumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

data class TripsSummary(
    val count: Int = 0,
    val totalTons: Double = 0.0,
    val totalKm: Double = 0.0,
    val totalTonKm: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalFuelExpenses: Double = 0.0,
    val totalFuelLiters: Double = 0.0,
    val totalOtherExpenses: Double = 0.0,
    val totalDriverSalary: Double = 0.0,
    val activeTripsCount: Int = 0
)

data class RememberedTripData(
    val nextTripNumber: String,
    val cargoType: String,
    val loadingLocation: String,
    val unloadingLocation: String,
    val weightTons: Double,
    val volumeM3: Double?,
    val distanceKm: Double,
    val rateType: RateType,
    val rateValue: Double,
    val fuelRate: Double,
    val fuelPrice: Double,
    val customerName: String,
    val driverName: String,
    val truckPlate: String,
    val driverSalaryPercent: Double = 20.0,
    val isAutoFilledFromPrevious: Boolean
)

class TripViewModel(
    private val repository: TripRepository,
    private val fleetRepository: FleetRepository? = null,
    val routingService: RoutingService? = null
) : ViewModel() {

    val settings: SettingsPreferences = repository.settingsPreferences
    val locationService = LocationService()

    suspend fun calculateDistance(origin: String, destination: String, forceRefresh: Boolean = false): RouteResult {
        return routingService?.calculateDistance(origin, destination, forceRefresh)
            ?: RouteResult.Error("Сервис маршрутизации не инициализирован")
    }

    suspend fun clearRouteCache() {
        routingService?.clearCache()
    }

    suspend fun getRouteCacheCount(): Int {
        return routingService?.getCachedRoutesCount() ?: 0
    }

    val registeredDrivers: StateFlow<List<Driver>> = (fleetRepository?.allDrivers ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registeredVehicles: StateFlow<List<Vehicle>> = (fleetRepository?.allVehicles ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            locationService.loadSettlementsFromInternet()
        }
    }

    fun refreshOnlineLocations() {
        viewModelScope.launch {
            locationService.loadSettlementsFromInternet()
        }
    }

    suspend fun getBackupJson(): String {
        val tripsList = allTrips.value
        val vehiclesList = fleetRepository?.allVehicles?.firstOrNull() ?: emptyList()
        val driversList = fleetRepository?.allDrivers?.firstOrNull() ?: emptyList()
        val serviceList = fleetRepository?.allServiceRecords?.firstOrNull() ?: emptyList()
        val waybillsList = fleetRepository?.allWaybills?.firstOrNull() ?: emptyList()
        val docsList = fleetRepository?.allDocuments?.firstOrNull() ?: emptyList()

        return BackupService.createBackupJson(
            trips = tripsList,
            settings = settings,
            vehicles = vehiclesList,
            drivers = driversList,
            serviceRecords = serviceList,
            waybills = waybillsList,
            documents = docsList
        )
    }

    fun exportBackupToStream(outputStream: OutputStream, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = getBackupJson()
            BackupService.writeBackupToStream(outputStream, json)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    suspend fun getShareableBackupUri(context: Context): android.net.Uri {
        val tripsList = allTrips.value
        val vehiclesList = fleetRepository?.allVehicles?.firstOrNull() ?: emptyList()
        val driversList = fleetRepository?.allDrivers?.firstOrNull() ?: emptyList()
        val serviceList = fleetRepository?.allServiceRecords?.firstOrNull() ?: emptyList()
        val waybillsList = fleetRepository?.allWaybills?.firstOrNull() ?: emptyList()
        val docsList = fleetRepository?.allDocuments?.firstOrNull() ?: emptyList()

        return BackupService.createShareableBackupUri(
            context = context,
            trips = tripsList,
            settings = settings,
            vehicles = vehiclesList,
            drivers = driversList,
            serviceRecords = serviceList,
            waybills = waybillsList,
            documents = docsList
        )
    }

    fun restoreBackupFromStream(
        inputStream: InputStream,
        replaceExisting: Boolean,
        onResult: (RestoreResult) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = try {
                BackupService.readBackupFromStream(inputStream)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(RestoreResult(false, 0, 0, 0, "Не удалось прочитать файл: ${e.message}"))
                }
                return@launch
            }
            val result = BackupService.restoreFromJson(json, repository, fleetRepository, replaceExisting)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    val searchQuery = MutableStateFlow("")
    val filterStatus = MutableStateFlow<TripStatus?>(null)
    val filterCargo = MutableStateFlow<String?>(null)
    val filterPeriod = MutableStateFlow(TripListPeriod.ALL)
    val customDateStart = MutableStateFlow<Long?>(null)
    val customDateEnd = MutableStateFlow<Long?>(null)
    val filterDriver = MutableStateFlow<String?>(null)
    val filterTruck = MutableStateFlow<String?>(null)
    val sortOrder = MutableStateFlow(TripSortOrder.DATE_DESC)

    // App Theme and Styling State
    val currentThemeMode = MutableStateFlow(settings.appThemeMode)
    val currentColorStyle = MutableStateFlow(settings.appColorStyle)

    fun setThemeMode(mode: String) {
        settings.appThemeMode = mode
        currentThemeMode.value = mode
    }

    fun setColorStyle(style: String) {
        settings.appColorStyle = style
        currentColorStyle.value = style
    }

    fun applyFilters(driver: String? = null, truck: String? = null, status: TripStatus? = null) {
        driver?.let { filterDriver.value = it }
        truck?.let { filterTruck.value = it }
        status?.let { filterStatus.value = it }
    }

    val allTrips: StateFlow<List<Trip>> = repository.allTrips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val latestTrip: StateFlow<Trip?> = repository.getLatestTrip()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val knownLocations: StateFlow<List<String>> = repository.knownLocations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val knownCustomers: StateFlow<List<String>> = repository.knownCustomers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private data class FilterState1(
        val trips: List<Trip>,
        val query: String,
        val status: TripStatus?,
        val cargo: String?
    )

    private data class FilterState2(
        val period: TripListPeriod,
        val customStart: Long?,
        val customEnd: Long?,
        val driver: String?,
        val truck: String?,
        val sort: TripSortOrder
    )

    val filteredTrips: StateFlow<List<Trip>> = combine(
        combine(allTrips, searchQuery, filterStatus, filterCargo) { trips, query, status, cargo ->
            FilterState1(trips, query, status, cargo)
        },
        combine(
            combine(filterPeriod, customDateStart, customDateEnd) { period, start, end -> Triple(period, start, end) },
            filterDriver,
            filterTruck,
            sortOrder
        ) { periodInfo, driver, truck, sort ->
            FilterState2(periodInfo.first, periodInfo.second, periodInfo.third, driver, truck, sort)
        }
    ) { f1, f2 ->
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        val filtered = f1.trips.filter { trip ->
            val matchesQuery = f1.query.isBlank() ||
                trip.tripNumber.contains(f1.query, ignoreCase = true) ||
                trip.cargoType.contains(f1.query, ignoreCase = true) ||
                trip.loadingLocation.contains(f1.query, ignoreCase = true) ||
                trip.unloadingLocation.contains(f1.query, ignoreCase = true) ||
                trip.ttnNumber.contains(f1.query, ignoreCase = true) ||
                trip.customerName.contains(f1.query, ignoreCase = true) ||
                trip.driverName.contains(f1.query, ignoreCase = true) ||
                trip.truckPlate.contains(f1.query, ignoreCase = true)

            val matchesStatus = f1.status == null || trip.status == f1.status
            val matchesCargo = f1.cargo == null || trip.cargoType.equals(f1.cargo, ignoreCase = true)
            val matchesDriver = f2.driver == null || trip.driverName.equals(f2.driver, ignoreCase = true)
            val matchesTruck = f2.truck == null || trip.truckPlate.equals(f2.truck, ignoreCase = true)

            val matchesPeriod = when (f2.period) {
                TripListPeriod.ALL -> true
                TripListPeriod.TODAY -> {
                    val cal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    trip.loadingDate >= cal.timeInMillis
                }
                TripListPeriod.WEEK -> trip.loadingDate >= (now - 7L * oneDay)
                TripListPeriod.MONTH -> trip.loadingDate >= (now - 30L * oneDay)
                TripListPeriod.CUSTOM -> {
                    val startMatches = f2.customStart == null || trip.loadingDate >= f2.customStart
                    val endMatches = f2.customEnd == null || trip.loadingDate <= f2.customEnd
                    startMatches && endMatches
                }
            }

            matchesQuery && matchesStatus && matchesCargo && matchesDriver && matchesTruck && matchesPeriod
        }

        when (f2.sort) {
            TripSortOrder.DATE_DESC -> filtered.sortedByDescending { it.loadingDate }
            TripSortOrder.DATE_ASC -> filtered.sortedBy { it.loadingDate }
            TripSortOrder.PRICE_DESC -> filtered.sortedByDescending { it.totalPrice }
            TripSortOrder.WEIGHT_DESC -> filtered.sortedByDescending { it.weightTons }
            TripSortOrder.DISTANCE_DESC -> filtered.sortedByDescending { it.distanceKm }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val summary: StateFlow<TripsSummary> = filteredTrips.combine(allTrips) { filtered, all ->
        var tons = 0.0
        var km = 0.0
        var tkm = 0.0
        var rev = 0.0
        var fuelExp = 0.0
        var fuelLit = 0.0
        var otherExp = 0.0
        var driverSal = 0.0

        for (t in filtered) {
            tons += t.weightTons
            km += t.distanceKm
            tkm += t.tonKilometers
            rev += t.totalPrice
            fuelExp += t.fuelExpenses
            fuelLit += t.effectiveFuelLiters
            otherExp += t.otherExpenses
            driverSal += t.effectiveDriverSalary
        }
        val totalExp = fuelExp + otherExp + driverSal
        val active = all.count { it.status == TripStatus.IN_TRANSIT || it.status == TripStatus.LOADING }

        TripsSummary(
            count = filtered.size,
            totalTons = tons,
            totalKm = km,
            totalTonKm = tkm,
            totalRevenue = rev,
            totalExpenses = totalExp,
            totalProfit = rev - totalExp,
            totalFuelExpenses = fuelExp,
            totalFuelLiters = fuelLit,
            totalOtherExpenses = otherExp,
            totalDriverSalary = driverSal,
            activeTripsCount = active
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TripsSummary()
    )

    fun getNextTripNumber(): String {
        val tripsList = allTrips.value
        val lastTripFromDb = tripsList.maxByOrNull { it.id } ?: latestTrip.value
        val lastNumberStr = lastTripFromDb?.tripNumber ?: if (settings.hasRememberedData) settings.lastTripNumber else null
        return TripNumberUtils.generateNextTripNumber(lastNumberStr)
    }

    fun getInitialNewTripData(): RememberedTripData {
        val tripsList = allTrips.value
        val lastTrip = tripsList.maxByOrNull { it.id } ?: latestTrip.value
        val nextNumber = getNextTripNumber()

        return if (lastTrip != null) {
            RememberedTripData(
                nextTripNumber = nextNumber,
                cargoType = lastTrip.cargoType,
                loadingLocation = lastTrip.loadingLocation,
                unloadingLocation = lastTrip.unloadingLocation,
                weightTons = lastTrip.weightTons,
                volumeM3 = lastTrip.volumeM3,
                distanceKm = lastTrip.distanceKm,
                rateType = lastTrip.rateType,
                rateValue = lastTrip.rateValue,
                fuelRate = lastTrip.fuelConsumptionRate ?: settings.defaultFuelConsumptionRate,
                fuelPrice = lastTrip.fuelPricePerLiter ?: settings.defaultFuelPricePerLiter,
                customerName = lastTrip.customerName,
                driverName = if (lastTrip.driverName.isNotBlank()) lastTrip.driverName else settings.defaultDriver,
                truckPlate = if (lastTrip.truckPlate.isNotBlank()) lastTrip.truckPlate else settings.defaultTruck,
                driverSalaryPercent = if (lastTrip.driverSalaryPercent > 0) lastTrip.driverSalaryPercent else settings.defaultDriverSalaryPercent,
                isAutoFilledFromPrevious = true
            )
        } else if (settings.hasRememberedData) {
            RememberedTripData(
                nextTripNumber = nextNumber,
                cargoType = settings.lastCargo,
                loadingLocation = settings.lastLoadingLocation,
                unloadingLocation = settings.lastUnloadingLocation,
                weightTons = settings.lastWeightTons,
                volumeM3 = if (settings.lastVolumeM3 > 0) settings.lastVolumeM3 else null,
                distanceKm = settings.lastDistanceKm,
                rateType = settings.lastRateType,
                rateValue = settings.lastRateValue,
                fuelRate = settings.lastFuelRate,
                fuelPrice = settings.lastFuelPrice,
                customerName = settings.lastCustomer,
                driverName = settings.defaultDriver,
                truckPlate = settings.defaultTruck,
                driverSalaryPercent = settings.lastDriverSalaryPercent,
                isAutoFilledFromPrevious = true
            )
        } else {
            RememberedTripData(
                nextTripNumber = nextNumber,
                cargoType = settings.defaultCargo,
                loadingLocation = "",
                unloadingLocation = "",
                weightTons = 28.50,
                volumeM3 = null,
                distanceKm = 280.0,
                rateType = settings.defaultRateType,
                rateValue = settings.defaultRateValue,
                fuelRate = settings.defaultFuelConsumptionRate,
                fuelPrice = settings.defaultFuelPricePerLiter,
                customerName = "",
                driverName = settings.defaultDriver,
                truckPlate = settings.defaultTruck,
                driverSalaryPercent = settings.defaultDriverSalaryPercent,
                isAutoFilledFromPrevious = false
            )
        }
    }

    fun setFilterStatus(status: TripStatus?) {
        filterStatus.value = status
    }

    fun setFilterCargo(cargo: String?) {
        filterCargo.value = cargo
    }

    fun setFilterPeriod(period: TripListPeriod) {
        filterPeriod.value = period
    }

    fun setCustomPeriod(start: Long?, end: Long?) {
        customDateStart.value = start
        customDateEnd.value = end
        filterPeriod.value = TripListPeriod.CUSTOM
    }

    fun setFilterDriver(driver: String?) {
        filterDriver.value = driver
    }

    fun setFilterTruck(truck: String?) {
        filterTruck.value = truck
    }

    fun setSortOrder(order: TripSortOrder) {
        sortOrder.value = order
    }

    fun resetAllFilters() {
        searchQuery.value = ""
        filterStatus.value = null
        filterCargo.value = null
        filterPeriod.value = TripListPeriod.ALL
        customDateStart.value = null
        customDateEnd.value = null
        filterDriver.value = null
        filterTruck.value = null
        sortOrder.value = TripSortOrder.DATE_DESC
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateTripStatus(trip: Trip, newStatus: TripStatus) {
        viewModelScope.launch {
            val updated = trip.copy(
                status = newStatus,
                unloadingDate = if (newStatus == TripStatus.UNLOADED || newStatus == TripStatus.PAID) {
                    trip.unloadingDate ?: System.currentTimeMillis()
                } else trip.unloadingDate
            )
            repository.updateTrip(updated)
        }
    }

    fun saveTrip(trip: Trip, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = if (trip.id == 0L) {
                repository.insertTrip(trip)
            } else {
                repository.updateTrip(trip)
                trip.id
            }
            onComplete(id)
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
        }
    }

    fun deleteTripById(id: Long) {
        viewModelScope.launch {
            repository.deleteTripById(id)
        }
    }

    fun saveDefaultSettings(
        driver: String,
        truck: String,
        rateType: RateType,
        rateValue: Double,
        cargo: String,
        fuelConsumptionRate: Double,
        fuelPricePerLiter: Double,
        autoCalculateFuel: Boolean,
        defaultDriverSalaryPercent: Double = 20.0,
        applyToAllPreviousTrips: Boolean = true
    ) {
        settings.defaultDriver = driver
        settings.defaultTruck = truck
        settings.defaultRateType = rateType
        settings.defaultRateValue = rateValue
        settings.defaultCargo = cargo
        settings.defaultFuelConsumptionRate = fuelConsumptionRate
        settings.defaultFuelPricePerLiter = fuelPricePerLiter
        settings.autoCalculateFuel = autoCalculateFuel
        settings.defaultDriverSalaryPercent = defaultDriverSalaryPercent

        if (applyToAllPreviousTrips) {
            viewModelScope.launch {
                val currentTrips = repository.getAllTripsList()
                val updatedTrips = currentTrips.map { trip ->
                    val newRateType = if (rateValue > 0) rateType else trip.rateType
                    val newRateValue = if (rateValue > 0) rateValue else trip.rateValue
                    val newPrice = Trip.calculatePrice(trip.weightTons, trip.distanceKm, newRateType, newRateValue)
                    val finalPrice = if (newPrice > 0) newPrice else trip.totalPrice

                    val effectiveFuelRate = if (fuelConsumptionRate > 0) fuelConsumptionRate else (trip.fuelConsumptionRate ?: 38.0)
                    val effectiveFuelPrice = if (fuelPricePerLiter > 0) fuelPricePerLiter else (trip.fuelPricePerLiter ?: 66.0)
                    val newFuelLiters = if (trip.distanceKm > 0) (trip.distanceKm / 100.0) * effectiveFuelRate else trip.effectiveFuelLiters
                    val newFuelExpenses = if (autoCalculateFuel && trip.distanceKm > 0) newFuelLiters * effectiveFuelPrice else trip.fuelExpenses

                    val newSalaryPercent = if (defaultDriverSalaryPercent > 0) defaultDriverSalaryPercent else trip.driverSalaryPercent
                    val newSalaryAmount = if (finalPrice > 0 && newSalaryPercent > 0) finalPrice * (newSalaryPercent / 100.0) else trip.effectiveDriverSalary

                    trip.copy(
                        driverName = if (driver.isNotBlank() && (trip.driverName.isBlank() || trip.driverName == settings.defaultDriver)) driver else trip.driverName,
                        truckPlate = if (truck.isNotBlank() && (trip.truckPlate.isBlank() || trip.truckPlate == settings.defaultTruck)) truck else trip.truckPlate,
                        cargoType = if (cargo.isNotBlank() && (trip.cargoType.isBlank() || trip.cargoType == settings.defaultCargo)) cargo else trip.cargoType,
                        rateType = newRateType,
                        rateValue = newRateValue,
                        totalPrice = finalPrice,
                        fuelConsumptionRate = effectiveFuelRate,
                        fuelPricePerLiter = effectiveFuelPrice,
                        fuelLiters = newFuelLiters,
                        fuelExpenses = newFuelExpenses,
                        driverSalaryPercent = newSalaryPercent,
                        driverSalaryAmount = newSalaryAmount
                    )
                }
                repository.updateTrips(updatedTrips)
            }
        }
    }

    fun saveFuelAndGeneralSettings(
        fuelConsumptionRate: Double,
        fuelPricePerLiter: Double,
        autoCalculateFuel: Boolean,
        applyToAllTrips: Boolean = false
    ) {
        settings.defaultFuelConsumptionRate = fuelConsumptionRate
        settings.defaultFuelPricePerLiter = fuelPricePerLiter
        settings.autoCalculateFuel = autoCalculateFuel

        if (applyToAllTrips) {
            viewModelScope.launch {
                val currentTrips = repository.getAllTripsList()
                val updatedTrips = currentTrips.map { trip ->
                    val effectiveFuelRate = if (fuelConsumptionRate > 0) fuelConsumptionRate else (trip.fuelConsumptionRate ?: 38.0)
                    val effectiveFuelPrice = if (fuelPricePerLiter > 0) fuelPricePerLiter else (trip.fuelPricePerLiter ?: 66.0)
                    val newFuelLiters = if (trip.distanceKm > 0) (trip.distanceKm / 100.0) * effectiveFuelRate else trip.effectiveFuelLiters
                    val newFuelExpenses = if (autoCalculateFuel && trip.distanceKm > 0) newFuelLiters * effectiveFuelPrice else trip.fuelExpenses

                    trip.copy(
                        fuelConsumptionRate = effectiveFuelRate,
                        fuelPricePerLiter = effectiveFuelPrice,
                        fuelLiters = newFuelLiters,
                        fuelExpenses = newFuelExpenses
                    )
                }
                repository.updateTrips(updatedTrips)
            }
        }
    }

    fun batchUpdateTrips(
        tripIds: Set<Long>,
        newStatus: TripStatus? = null,
        newDriverName: String? = null,
        newTruckPlate: String? = null,
        newCargoType: String? = null,
        newRateType: RateType? = null,
        newRateValue: Double? = null,
        newSalaryPercent: Double? = null
    ) {
        viewModelScope.launch {
            val allTrips = repository.getAllTripsList()
            val updated = allTrips.map { trip ->
                if (trip.id in tripIds) {
                    val status = newStatus ?: trip.status
                    val driver = newDriverName ?: trip.driverName
                    val truck = newTruckPlate ?: trip.truckPlate
                    val cargo = newCargoType ?: trip.cargoType
                    val rType = newRateType ?: trip.rateType
                    val rVal = newRateValue ?: trip.rateValue
                    val calcPrice = if (newRateValue != null || newRateType != null) {
                        Trip.calculatePrice(trip.weightTons, trip.distanceKm, rType, rVal)
                    } else trip.totalPrice
                    val finalPrice = if (calcPrice > 0) calcPrice else trip.totalPrice
                    val salPercent = newSalaryPercent ?: trip.driverSalaryPercent
                    val salAmount = if (newSalaryPercent != null && finalPrice > 0) {
                        finalPrice * (salPercent / 100.0)
                    } else trip.effectiveDriverSalary

                    trip.copy(
                        status = status,
                        driverName = driver,
                        truckPlate = truck,
                        cargoType = cargo,
                        rateType = rType,
                        rateValue = rVal,
                        totalPrice = finalPrice,
                        driverSalaryPercent = salPercent,
                        driverSalaryAmount = salAmount
                    )
                } else {
                    trip
                }
            }
            repository.updateTrips(updated)
        }
    }

    fun batchDeleteTrips(tripIds: Set<Long>) {
        viewModelScope.launch {
            val allTrips = repository.getAllTripsList()
            val toDelete = allTrips.filter { it.id in tripIds }
            toDelete.forEach { repository.deleteTrip(it) }
        }
    }
}

class TripViewModelFactory(
    private val repository: TripRepository,
    private val fleetRepository: FleetRepository? = null,
    private val routingService: RoutingService? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripViewModel::class.java)) {
            return TripViewModel(repository, fleetRepository, routingService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

enum class TripListPeriod(val label: String) {
    ALL("Все время"),
    TODAY("Сегодня"),
    WEEK("7 дней"),
    MONTH("Этот месяц"),
    CUSTOM("Свой период")
}

enum class TripSortOrder(val label: String) {
    DATE_DESC("Сначала новые"),
    DATE_ASC("Сначала старые"),
    PRICE_DESC("Сумма (убыв.)"),
    WEIGHT_DESC("Вес (убыв.)"),
    DISTANCE_DESC("Расстояние (убыв.)")
}
