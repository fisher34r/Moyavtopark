package com.example.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.fleet.DocumentType
import com.example.data.fleet.Driver
import com.example.data.fleet.DriverStatus
import com.example.data.fleet.FleetDocument
import com.example.data.fleet.FuelRecord
import com.example.data.fleet.ServiceRecord
import com.example.data.fleet.ServiceType
import com.example.data.fleet.Vehicle
import com.example.data.fleet.VehicleStatus
import com.example.data.fleet.VehicleType
import com.example.data.fleet.Waybill
import com.example.data.fleet.WaybillStatus
import com.example.data.local.SettingsPreferences
import com.example.data.model.RateType
import com.example.data.model.Trip
import com.example.data.model.TripStatus
import com.example.data.repository.FleetRepository
import com.example.data.repository.TripRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RestoreResult(
    val success: Boolean,
    val restoredTripsCount: Int = 0,
    val restoredVehiclesCount: Int = 0,
    val restoredDriversCount: Int = 0,
    val message: String = ""
)

object BackupService {

    private const val TAG = "BackupService"

    /**
     * Создает JSON-строку полной резервной копии базы данных (рейсы + автопарк) и настроек приложения
     */
    fun createBackupJson(
        trips: List<Trip>,
        settings: SettingsPreferences,
        vehicles: List<Vehicle> = emptyList(),
        drivers: List<Driver> = emptyList(),
        serviceRecords: List<ServiceRecord> = emptyList(),
        waybills: List<Waybill> = emptyList(),
        documents: List<FleetDocument> = emptyList(),
        fuelRecords: List<FuelRecord> = emptyList()
    ): String {
        val root = JSONObject()
        root.put("app", "Зерновоз")
        root.put("backupVersion", 2)
        root.put("createdAt", System.currentTimeMillis())
        root.put("createdDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("tripsCount", trips.size)
        root.put("vehiclesCount", vehicles.size)
        root.put("driversCount", drivers.size)

        // Сериализация настроек
        val settingsObj = JSONObject().apply {
            put("defaultRateType", settings.defaultRateType.name)
            put("defaultRateValue", settings.defaultRateValue)
            put("defaultDriver", settings.defaultDriver)
            put("defaultTruck", settings.defaultTruck)
            put("defaultCargo", settings.defaultCargo)
            put("defaultFuelConsumptionRate", settings.defaultFuelConsumptionRate)
            put("defaultFuelPricePerLiter", settings.defaultFuelPricePerLiter)
            put("defaultDriverSalaryPercent", settings.defaultDriverSalaryPercent)
            put("autoCalculateFuel", settings.autoCalculateFuel)
        }
        root.put("settings", settingsObj)

        // Сериализация всех рейсов
        val tripsArray = JSONArray()
        trips.forEach { trip ->
            val tripObj = JSONObject().apply {
                put("id", trip.id)
                put("tripNumber", trip.tripNumber)
                put("cargoType", trip.cargoType)
                put("loadingDate", trip.loadingDate)
                if (trip.unloadingDate != null) put("unloadingDate", trip.unloadingDate)
                put("loadingLocation", trip.loadingLocation)
                put("unloadingLocation", trip.unloadingLocation)
                put("weightTons", trip.weightTons)
                if (trip.volumeM3 != null) put("volumeM3", trip.volumeM3)
                put("distanceKm", trip.distanceKm)
                put("rateType", trip.rateType.name)
                put("rateValue", trip.rateValue)
                put("totalPrice", trip.totalPrice)
                if (trip.fuelConsumptionRate != null) put("fuelConsumptionRate", trip.fuelConsumptionRate)
                if (trip.fuelPricePerLiter != null) put("fuelPricePerLiter", trip.fuelPricePerLiter)
                if (trip.fuelLiters != null) put("fuelLiters", trip.fuelLiters)
                put("fuelExpenses", trip.fuelExpenses)
                put("otherExpenses", trip.otherExpenses)
                put("driverSalaryPercent", trip.driverSalaryPercent)
                put("driverSalaryAmount", trip.driverSalaryAmount)
                put("status", trip.status.name)
                put("ttnNumber", trip.ttnNumber)
                put("customerName", trip.customerName)
                put("driverName", trip.driverName)
                put("truckPlate", trip.truckPlate)
                put("notes", trip.notes)
                put("createdAt", trip.createdAt)
            }
            tripsArray.put(tripObj)
        }
        root.put("trips", tripsArray)

        // Сериализация Автопарка (ТС)
        val vehiclesArray = JSONArray()
        vehicles.forEach { v ->
            val vObj = JSONObject().apply {
                put("id", v.id)
                put("plateNumber", v.plateNumber)
                put("model", v.model)
                put("type", v.type.name)
                put("vin", v.vin)
                put("year", v.year)
                put("status", v.status.name)
                put("currentOdometerKm", v.currentOdometerKm)
                put("fuelTankCapacityLiters", v.fuelTankCapacityLiters)
                put("currentFuelLiters", v.currentFuelLiters)
                put("assignedDriverName", v.assignedDriverName)
                put("notes", v.notes)
                put("createdAt", v.createdAt)
            }
            vehiclesArray.put(vObj)
        }
        root.put("vehicles", vehiclesArray)

        // Сериализация Водителей
        val driversArray = JSONArray()
        drivers.forEach { d ->
            val dObj = JSONObject().apply {
                put("id", d.id)
                put("fullName", d.fullName)
                put("phone", d.phone)
                put("licenseNumber", d.licenseNumber)
                put("categories", d.categories)
                put("hasDopog", d.hasDopog)
                put("hasSkziCard", d.hasSkziCard)
                put("status", d.status.name)
                put("shiftSchedule", d.shiftSchedule)
                put("assignedVehiclePlate", d.assignedVehiclePlate)
                put("experienceYears", d.experienceYears)
                put("salaryPercent", d.salaryPercent)
                put("notes", d.notes)
                put("createdAt", d.createdAt)
            }
            driversArray.put(dObj)
        }
        root.put("drivers", driversArray)

        // Сериализация Сервиса и ТО
        val serviceArray = JSONArray()
        serviceRecords.forEach { s ->
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("vehiclePlate", s.vehiclePlate)
                put("serviceType", s.serviceType.name)
                put("date", s.date)
                put("odometerKm", s.odometerKm)
                put("description", s.description)
                put("cost", s.cost)
                put("orderNumber", s.orderNumber)
                put("serviceStation", s.serviceStation)
                put("isCompleted", s.isCompleted)
                if (s.nextPlannedKm != null) put("nextPlannedKm", s.nextPlannedKm)
                if (s.nextPlannedDate != null) put("nextPlannedDate", s.nextPlannedDate)
                put("createdAt", s.createdAt)
            }
            serviceArray.put(sObj)
        }
        root.put("serviceRecords", serviceArray)

        // Сериализация Путевых листов
        val waybillsArray = JSONArray()
        waybills.forEach { w ->
            val wObj = JSONObject().apply {
                put("id", w.id)
                put("number", w.number)
                put("date", w.date)
                put("vehiclePlate", w.vehiclePlate)
                put("trailerPlate", w.trailerPlate)
                put("driverName", w.driverName)
                put("startOdometerKm", w.startOdometerKm)
                if (w.endOdometerKm != null) put("endOdometerKm", w.endOdometerKm)
                put("startFuelLiters", w.startFuelLiters)
                if (w.endFuelLiters != null) put("endFuelLiters", w.endFuelLiters)
                put("fuelAddedLiters", w.fuelAddedLiters)
                put("routeDescription", w.routeDescription)
                put("medicalCheckPassed", w.medicalCheckPassed)
                put("technicalCheckPassed", w.technicalCheckPassed)
                put("status", w.status.name)
                put("notes", w.notes)
                put("createdAt", w.createdAt)
            }
            waybillsArray.put(wObj)
        }
        root.put("waybills", waybillsArray)

        // Сериализация Документов автопарка
        val docsArray = JSONArray()
        documents.forEach { doc ->
            val docObj = JSONObject().apply {
                put("id", doc.id)
                put("title", doc.title)
                put("docType", doc.docType.name)
                put("vehiclePlateOrDriver", doc.vehiclePlateOrDriver)
                put("seriesAndNumber", doc.seriesAndNumber)
                put("issueDate", doc.issueDate)
                put("expiryDate", doc.expiryDate)
                put("issuingAuthority", doc.issuingAuthority)
                put("cost", doc.cost)
                put("notes", doc.notes)
                put("createdAt", doc.createdAt)
            }
            docsArray.put(docObj)
        }
        root.put("fleetDocuments", docsArray)

        // Сериализация Заправок автопарка
        val fuelArray = JSONArray()
        fuelRecords.forEach { f ->
            val fObj = JSONObject().apply {
                put("id", f.id)
                put("vehiclePlate", f.vehiclePlate)
                put("date", f.date)
                put("liters", f.liters)
                put("pricePerLiter", f.pricePerLiter)
                put("totalCost", f.totalCost)
                put("stationName", f.stationName)
                if (f.odometerKm != null) put("odometerKm", f.odometerKm)
                put("notes", f.notes)
                put("createdAt", f.createdAt)
            }
            fuelArray.put(fObj)
        }
        root.put("fuelRecords", fuelArray)

        return root.toString(2)
    }

    /**
     * Восстанавливает базу данных (рейсы и автопарк) и настройки из JSON-строки
     */
    suspend fun restoreFromJson(
        jsonString: String,
        repository: TripRepository,
        fleetRepository: FleetRepository? = null,
        replaceExisting: Boolean
    ): RestoreResult {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("app") || !root.has("trips")) {
                return RestoreResult(false, 0, 0, 0, "Неверный формат файла резервной копии")
            }

            // 1. Восстановление настроек (если присутствуют)
            if (root.has("settings")) {
                val s = root.getJSONObject("settings")
                val prefs = repository.settingsPreferences
                if (s.has("defaultRateType")) {
                    try {
                        prefs.defaultRateType = RateType.valueOf(s.getString("defaultRateType"))
                    } catch (e: Exception) { /* keep current */ }
                }
                if (s.has("defaultRateValue")) prefs.defaultRateValue = s.getDouble("defaultRateValue")
                if (s.has("defaultDriver")) prefs.defaultDriver = s.getString("defaultDriver")
                if (s.has("defaultTruck")) prefs.defaultTruck = s.getString("defaultTruck")
                if (s.has("defaultCargo")) prefs.defaultCargo = s.getString("defaultCargo")
                if (s.has("defaultFuelConsumptionRate")) prefs.defaultFuelConsumptionRate = s.getDouble("defaultFuelConsumptionRate")
                if (s.has("defaultFuelPricePerLiter")) prefs.defaultFuelPricePerLiter = s.getDouble("defaultFuelPricePerLiter")
                if (s.has("autoCalculateFuel")) prefs.autoCalculateFuel = s.getBoolean("autoCalculateFuel")
            }

            // 2. Восстановление рейсов
            val tripsArray = root.getJSONArray("trips")
            val restoredTrips = mutableListOf<Trip>()

            for (i in 0 until tripsArray.length()) {
                val obj = tripsArray.getJSONObject(i)
                val rateType = try {
                    RateType.valueOf(obj.getString("rateType"))
                } catch (e: Exception) {
                    RateType.PER_TON_KM
                }
                val status = try {
                    TripStatus.valueOf(obj.getString("status"))
                } catch (e: Exception) {
                    TripStatus.IN_TRANSIT
                }

                val weight = obj.optDouble("weightTons", 28.5)
                val distance = obj.optDouble("distanceKm", 280.0)
                val rateVal = obj.optDouble("rateValue", 4.5)
                val totalPrice = if (obj.has("totalPrice")) {
                    obj.getDouble("totalPrice")
                } else {
                    Trip.calculatePrice(weight, distance, rateType, rateVal)
                }

                val trip = Trip(
                    id = if (replaceExisting) obj.optLong("id", 0L) else 0L,
                    tripNumber = obj.optString("tripNumber", "Рейс №${i + 1}"),
                    cargoType = obj.optString("cargoType", "Пшеница"),
                    loadingDate = obj.optLong("loadingDate", System.currentTimeMillis()),
                    unloadingDate = if (obj.has("unloadingDate")) obj.getLong("unloadingDate") else null,
                    loadingLocation = obj.optString("loadingLocation", ""),
                    unloadingLocation = obj.optString("unloadingLocation", ""),
                    weightTons = weight,
                    volumeM3 = if (obj.has("volumeM3")) obj.getDouble("volumeM3") else null,
                    distanceKm = distance,
                    rateType = rateType,
                    rateValue = rateVal,
                    totalPrice = totalPrice,
                    status = status,
                    ttnNumber = obj.optString("ttnNumber", ""),
                    customerName = obj.optString("customerName", ""),
                    driverName = obj.optString("driverName", ""),
                    truckPlate = obj.optString("truckPlate", ""),
                    fuelExpenses = obj.optDouble("fuelExpenses", 0.0),
                    otherExpenses = obj.optDouble("otherExpenses", 0.0),
                    driverSalaryPercent = obj.optDouble("driverSalaryPercent", 20.0),
                    driverSalaryAmount = obj.optDouble("driverSalaryAmount", 0.0),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    fuelConsumptionRate = if (obj.has("fuelConsumptionRate")) obj.getDouble("fuelConsumptionRate") else 38.0,
                    fuelPricePerLiter = if (obj.has("fuelPricePerLiter")) obj.getDouble("fuelPricePerLiter") else 66.0,
                    fuelLiters = if (obj.has("fuelLiters")) obj.getDouble("fuelLiters") else null
                )
                restoredTrips.add(trip)
            }

            if (replaceExisting) {
                repository.deleteAllTrips()
            }

            restoredTrips.forEach { trip ->
                repository.insertTrip(trip)
            }

            // 3. Восстановление данных Автопарка (если присутствуют в бэкапе и передан репозиторий)
            var restoredVehiclesCount = 0
            var restoredDriversCount = 0

            if (fleetRepository != null) {
                // ТС
                if (root.has("vehicles")) {
                    val vehiclesArray = root.getJSONArray("vehicles")
                    val restoredVehicles = mutableListOf<Vehicle>()
                    for (i in 0 until vehiclesArray.length()) {
                        val obj = vehiclesArray.getJSONObject(i)
                        val v = Vehicle(
                            id = if (replaceExisting) obj.optLong("id", 0L) else 0L,
                            plateNumber = obj.getString("plateNumber"),
                            model = obj.optString("model", ""),
                            type = try { VehicleType.valueOf(obj.getString("type")) } catch (_: Exception) { VehicleType.TRACTOR },
                            vin = obj.optString("vin", ""),
                            year = obj.optInt("year", 2021),
                            status = try { VehicleStatus.valueOf(obj.getString("status")) } catch (_: Exception) { VehicleStatus.AVAILABLE },
                            currentOdometerKm = obj.optDouble("currentOdometerKm", 0.0),
                            fuelTankCapacityLiters = obj.optDouble("fuelTankCapacityLiters", 600.0),
                            currentFuelLiters = obj.optDouble("currentFuelLiters", 0.0),
                            assignedDriverName = obj.optString("assignedDriverName", ""),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        restoredVehicles.add(v)
                    }
                    if (replaceExisting) {
                        fleetRepository.deleteAllVehicles()
                    }
                    fleetRepository.insertVehicles(restoredVehicles)
                    restoredVehiclesCount = restoredVehicles.size
                }

                // Водители
                if (root.has("drivers")) {
                    val driversArray = root.getJSONArray("drivers")
                    val restoredDrivers = mutableListOf<Driver>()
                    for (i in 0 until driversArray.length()) {
                        val obj = driversArray.getJSONObject(i)
                        val d = Driver(
                            id = if (replaceExisting) obj.optLong("id", 0L) else 0L,
                            fullName = obj.getString("fullName"),
                            phone = obj.optString("phone", ""),
                            licenseNumber = obj.optString("licenseNumber", ""),
                            categories = obj.optString("categories", "B, C, CE"),
                            hasDopog = obj.optBoolean("hasDopog", false),
                            hasSkziCard = obj.optBoolean("hasSkziCard", true),
                            status = try { DriverStatus.valueOf(obj.getString("status")) } catch (_: Exception) { DriverStatus.AVAILABLE },
                            shiftSchedule = obj.optString("shiftSchedule", "15/15 вахта"),
                            assignedVehiclePlate = obj.optString("assignedVehiclePlate", ""),
                            experienceYears = obj.optInt("experienceYears", 5),
                            salaryPercent = obj.optDouble("salaryPercent", 20.0),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        restoredDrivers.add(d)
                    }
                    if (replaceExisting) {
                        fleetRepository.deleteAllDrivers()
                    }
                    fleetRepository.insertDrivers(restoredDrivers)
                    restoredDriversCount = restoredDrivers.size
                }

                // ТО и сервис
                if (root.has("serviceRecords")) {
                    val sArray = root.getJSONArray("serviceRecords")
                    val restoredRecords = mutableListOf<ServiceRecord>()
                    for (i in 0 until sArray.length()) {
                        val obj = sArray.getJSONObject(i)
                        val s = ServiceRecord(
                            id = if (replaceExisting) obj.optLong("id", 0L) else 0L,
                            vehiclePlate = obj.getString("vehiclePlate"),
                            serviceType = try { ServiceType.valueOf(obj.getString("serviceType")) } catch (_: Exception) { ServiceType.TO_1 },
                            date = obj.optLong("date", System.currentTimeMillis()),
                            odometerKm = obj.optDouble("odometerKm", 0.0),
                            description = obj.optString("description", ""),
                            cost = obj.optDouble("cost", 0.0),
                            orderNumber = obj.optString("orderNumber", ""),
                            serviceStation = obj.optString("serviceStation", ""),
                            isCompleted = obj.optBoolean("isCompleted", true),
                            nextPlannedKm = if (obj.has("nextPlannedKm")) obj.getDouble("nextPlannedKm") else null,
                            nextPlannedDate = if (obj.has("nextPlannedDate")) obj.getLong("nextPlannedDate") else null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        restoredRecords.add(s)
                    }
                    if (replaceExisting) {
                        fleetRepository.deleteAllServiceRecords()
                    }
                    fleetRepository.insertServiceRecords(restoredRecords)
                }

                // Путевые листы
                if (root.has("waybills")) {
                    val wArray = root.getJSONArray("waybills")
                    val restoredWaybills = mutableListOf<Waybill>()
                    for (i in 0 until wArray.length()) {
                        val obj = wArray.getJSONObject(i)
                        val w = Waybill(
                            id = if (replaceExisting) obj.optLong("id", 0L) else 0L,
                            number = obj.getString("number"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            vehiclePlate = obj.getString("vehiclePlate"),
                            trailerPlate = obj.optString("trailerPlate", ""),
                            driverName = obj.optString("driverName", ""),
                            startOdometerKm = obj.optDouble("startOdometerKm", 0.0),
                            endOdometerKm = if (obj.has("endOdometerKm")) obj.getDouble("endOdometerKm") else null,
                            startFuelLiters = obj.optDouble("startFuelLiters", 0.0),
                            endFuelLiters = if (obj.has("endFuelLiters")) obj.getDouble("endFuelLiters") else null,
                            fuelAddedLiters = obj.optDouble("fuelAddedLiters", 0.0),
                            routeDescription = obj.optString("routeDescription", ""),
                            medicalCheckPassed = obj.optBoolean("medicalCheckPassed", true),
                            technicalCheckPassed = obj.optBoolean("technicalCheckPassed", true),
                            status = try { WaybillStatus.valueOf(obj.getString("status")) } catch (_: Exception) { WaybillStatus.ISSUED },
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        restoredWaybills.add(w)
                    }
                    if (replaceExisting) {
                        fleetRepository.deleteAllWaybills()
                    }
                    fleetRepository.insertWaybills(restoredWaybills)
                }

                // Документы
                if (root.has("fleetDocuments")) {
                    val dArray = root.getJSONArray("fleetDocuments")
                    val restoredDocs = mutableListOf<FleetDocument>()
                    for (i in 0 until dArray.length()) {
                        val obj = dArray.getJSONObject(i)
                        val doc = FleetDocument(
                            id = if (replaceExisting) obj.optLong("id", 0L) else 0L,
                            title = obj.getString("title"),
                            docType = try { DocumentType.valueOf(obj.getString("docType")) } catch (_: Exception) { DocumentType.OSAGO },
                            vehiclePlateOrDriver = obj.optString("vehiclePlateOrDriver", ""),
                            seriesAndNumber = obj.optString("seriesAndNumber", ""),
                            issueDate = obj.optLong("issueDate", System.currentTimeMillis()),
                            expiryDate = obj.optLong("expiryDate", System.currentTimeMillis() + 365L * 24 * 3600 * 1000),
                            issuingAuthority = obj.optString("issuingAuthority", ""),
                            cost = obj.optDouble("cost", 0.0),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        restoredDocs.add(doc)
                    }
                    if (replaceExisting) {
                        fleetRepository.deleteAllDocuments()
                    }
                    fleetRepository.insertDocuments(restoredDocs)
                }

                // Заправки
                if (root.has("fuelRecords")) {
                    val fArray = root.getJSONArray("fuelRecords")
                    val restoredFuel = mutableListOf<FuelRecord>()
                    for (i in 0 until fArray.length()) {
                        val obj = fArray.getJSONObject(i)
                        val fuel = FuelRecord(
                            id = if (replaceExisting) obj.optLong("id", 0L) else 0L,
                            vehiclePlate = obj.getString("vehiclePlate"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            liters = obj.optDouble("liters", 0.0),
                            pricePerLiter = obj.optDouble("pricePerLiter", 0.0),
                            totalCost = obj.optDouble("totalCost", 0.0),
                            stationName = obj.optString("stationName", ""),
                            odometerKm = if (obj.has("odometerKm")) obj.getDouble("odometerKm") else null,
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        restoredFuel.add(fuel)
                    }
                    if (replaceExisting) {
                        fleetRepository.deleteAllFuelRecords()
                    }
                    fleetRepository.insertFuelRecords(restoredFuel)
                }
            }

            val fleetMsg = if (restoredVehiclesCount > 0 || restoredDriversCount > 0) {
                ", ТС: $restoredVehiclesCount, водителей: $restoredDriversCount"
            } else ""

            RestoreResult(
                success = true,
                restoredTripsCount = restoredTrips.size,
                restoredVehiclesCount = restoredVehiclesCount,
                restoredDriversCount = restoredDriversCount,
                message = "Успешно восстановлено: ${restoredTrips.size} рейсов$fleetMsg и настройки"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Restore error", e)
            RestoreResult(false, 0, 0, 0, "Ошибка при восстановлении: ${e.localizedMessage ?: "Сбой чтения данных"}")
        }
    }

    /**
     * Записывает JSON бэкапа в исходящий поток (Storage Access Framework)
     */
    fun writeBackupToStream(outputStream: OutputStream, jsonContent: String) {
        outputStream.use { stream ->
            stream.write(jsonContent.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    /**
     * Считывает JSON бэкапа из входящего потока (Storage Access Framework)
     */
    fun readBackupFromStream(inputStream: InputStream): String {
        return inputStream.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }

    /**
     * Создает временный файл резервной копии в кэше приложения и возвращает Content URI для Google Диска
     */
    fun createShareableBackupUri(
        context: Context,
        trips: List<Trip>,
        settings: SettingsPreferences,
        vehicles: List<Vehicle> = emptyList(),
        drivers: List<Driver> = emptyList(),
        serviceRecords: List<ServiceRecord> = emptyList(),
        waybills: List<Waybill> = emptyList(),
        documents: List<FleetDocument> = emptyList(),
        fuelRecords: List<FuelRecord> = emptyList()
    ): Uri {
        val json = createBackupJson(
            trips = trips,
            settings = settings,
            vehicles = vehicles,
            drivers = drivers,
            serviceRecords = serviceRecords,
            waybills = waybills,
            documents = documents,
            fuelRecords = fuelRecords
        )
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "zernovoz_backup_$dateStr.json"
        val file = File(context.cacheDir, fileName)
        file.writeText(json, Charsets.UTF_8)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Создает Intent для отправки резервной копии на Google Диск или другие облачные хранилища
     */
    fun createGoogleDriveShareIntent(context: Context, fileUri: Uri): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Резервная копия Зерновоз (${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())})")
            putExtra(Intent.EXTRA_TEXT, "Резервная копия базы данных (рейсы и автопарк) и настроек приложения «Зерновоз»")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Сохранить на Google Диск или отправить")
    }
}
