package com.example.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.SettingsPreferences
import com.example.data.model.RateType
import com.example.data.model.Trip
import com.example.data.model.TripStatus
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
    val message: String = ""
)

object BackupService {

    private const val TAG = "BackupService"

    /**
     * Создает JSON-строку полной резервной копии базы данных и настроек приложения
     */
    fun createBackupJson(trips: List<Trip>, settings: SettingsPreferences): String {
        val root = JSONObject()
        root.put("app", "Зерновоз")
        root.put("backupVersion", 1)
        root.put("createdAt", System.currentTimeMillis())
        root.put("createdDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("tripsCount", trips.size)

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

        return root.toString(2)
    }

    /**
     * Восстанавливает базу данных и настройки из JSON-строки
     */
    suspend fun restoreFromJson(
        jsonString: String,
        repository: TripRepository,
        replaceExisting: Boolean
    ): RestoreResult {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("app") || !root.has("trips")) {
                return RestoreResult(false, 0, "Неверный формат файла резервной копии")
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

            RestoreResult(
                success = true,
                restoredTripsCount = restoredTrips.size,
                message = "Успешно восстановлено ${restoredTrips.size} рейсов и настройки"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Restore error", e)
            RestoreResult(false, 0, "Ошибка при восстановлении: ${e.localizedMessage ?: "Сбой чтения данных"}")
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
    fun createShareableBackupUri(context: Context, trips: List<Trip>, settings: SettingsPreferences): Uri {
        val json = createBackupJson(trips, settings)
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
            putExtra(Intent.EXTRA_TEXT, "Резервная копия базы данных и настроек приложения «Зерновоз»")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Сохранить на Google Диск или отправить")
    }
}
