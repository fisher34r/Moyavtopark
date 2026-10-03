package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.RateType
import com.example.data.model.Trip

class SettingsPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("grain_truck_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DEFAULT_RATE_TYPE = "default_rate_type"
        private const val KEY_DEFAULT_RATE_VALUE = "default_rate_value"
        private const val KEY_DEFAULT_DRIVER = "default_driver"
        private const val KEY_DEFAULT_TRUCK = "default_truck"
        private const val KEY_DEFAULT_CARGO = "default_cargo"
        private const val KEY_DEFAULT_FUEL_RATE = "default_fuel_rate"
        private const val KEY_DEFAULT_FUEL_PRICE = "default_fuel_price"
        private const val KEY_DEFAULT_DRIVER_SALARY_PERCENT = "default_driver_salary_percent"
        private const val KEY_AUTO_CALCULATE_FUEL = "auto_calculate_fuel"

        // Remembered last entered trip data
        private const val KEY_LAST_TRIP_NUMBER = "last_trip_number"
        private const val KEY_LAST_CARGO = "last_cargo"
        private const val KEY_LAST_LOADING_LOCATION = "last_loading_location"
        private const val KEY_LAST_UNLOADING_LOCATION = "last_unloading_location"
        private const val KEY_LAST_WEIGHT = "last_weight"
        private const val KEY_LAST_VOLUME = "last_volume"
        private const val KEY_LAST_DISTANCE = "last_distance"
        private const val KEY_LAST_RATE_TYPE = "last_rate_type"
        private const val KEY_LAST_RATE_VALUE = "last_rate_value"
        private const val KEY_LAST_FUEL_RATE = "last_fuel_rate"
        private const val KEY_LAST_FUEL_PRICE = "last_fuel_price"
        private const val KEY_LAST_DRIVER_SALARY_PERCENT = "last_driver_salary_percent"
        private const val KEY_LAST_CUSTOMER = "last_customer"
        private const val KEY_HAS_REMEMBERED_DATA = "has_remembered_data"

        // App Theme & Styling
        private const val KEY_APP_THEME_MODE = "app_theme_mode"
        private const val KEY_APP_COLOR_STYLE = "app_color_style"
    }

    var appThemeMode: String
        get() = prefs.getString(KEY_APP_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_APP_THEME_MODE, value).apply()

    var appColorStyle: String
        get() = prefs.getString(KEY_APP_COLOR_STYLE, "AGRO_GREEN") ?: "AGRO_GREEN"
        set(value) = prefs.edit().putString(KEY_APP_COLOR_STYLE, value).apply()

    var defaultRateType: RateType
        get() {
            val name = prefs.getString(KEY_DEFAULT_RATE_TYPE, RateType.PER_TON_KM.name)
            return try {
                RateType.valueOf(name ?: RateType.PER_TON_KM.name)
            } catch (_: Exception) {
                RateType.PER_TON_KM
            }
        }
        set(value) = prefs.edit().putString(KEY_DEFAULT_RATE_TYPE, value.name).apply()

    var defaultRateValue: Double
        get() = prefs.getFloat(KEY_DEFAULT_RATE_VALUE, 4.60f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_DEFAULT_RATE_VALUE, value.toFloat()).apply()

    var defaultDriver: String
        get() = prefs.getString(KEY_DEFAULT_DRIVER, "Иванов С. М.") ?: "Иванов С. М."
        set(value) = prefs.edit().putString(KEY_DEFAULT_DRIVER, value).apply()

    var defaultTruck: String
        get() = prefs.getString(KEY_DEFAULT_TRUCK, "А 742 КХ 123") ?: "А 742 КХ 123"
        set(value) = prefs.edit().putString(KEY_DEFAULT_TRUCK, value).apply()

    var defaultCargo: String
        get() = prefs.getString(KEY_DEFAULT_CARGO, "Пшеница") ?: "Пшеница"
        set(value) = prefs.edit().putString(KEY_DEFAULT_CARGO, value).apply()

    var defaultFuelConsumptionRate: Double
        get() = prefs.getFloat(KEY_DEFAULT_FUEL_RATE, 38.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_DEFAULT_FUEL_RATE, value.toFloat()).apply()

    var defaultFuelPricePerLiter: Double
        get() = prefs.getFloat(KEY_DEFAULT_FUEL_PRICE, 66.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_DEFAULT_FUEL_PRICE, value.toFloat()).apply()

    var defaultDriverSalaryPercent: Double
        get() = prefs.getFloat(KEY_DEFAULT_DRIVER_SALARY_PERCENT, 20.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_DEFAULT_DRIVER_SALARY_PERCENT, value.toFloat()).apply()

    var autoCalculateFuel: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CALCULATE_FUEL, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CALCULATE_FUEL, value).apply()

    // Last trip remembered fields
    val hasRememberedData: Boolean
        get() = prefs.getBoolean(KEY_HAS_REMEMBERED_DATA, false)

    var lastTripNumber: String
        get() = prefs.getString(KEY_LAST_TRIP_NUMBER, "Рейс №1") ?: "Рейс №1"
        set(value) = prefs.edit().putString(KEY_LAST_TRIP_NUMBER, value).apply()

    var lastCargo: String
        get() = prefs.getString(KEY_LAST_CARGO, defaultCargo) ?: defaultCargo
        set(value) = prefs.edit().putString(KEY_LAST_CARGO, value).apply()

    var lastLoadingLocation: String
        get() = prefs.getString(KEY_LAST_LOADING_LOCATION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_LOADING_LOCATION, value).apply()

    var lastUnloadingLocation: String
        get() = prefs.getString(KEY_LAST_UNLOADING_LOCATION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_UNLOADING_LOCATION, value).apply()

    var lastWeightTons: Double
        get() = prefs.getFloat(KEY_LAST_WEIGHT, 28.50f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_LAST_WEIGHT, value.toFloat()).apply()

    var lastVolumeM3: Double
        get() = prefs.getFloat(KEY_LAST_VOLUME, 0.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_LAST_VOLUME, value.toFloat()).apply()

    var lastDistanceKm: Double
        get() = prefs.getFloat(KEY_LAST_DISTANCE, 280.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_LAST_DISTANCE, value.toFloat()).apply()

    var lastRateType: RateType
        get() {
            val name = prefs.getString(KEY_LAST_RATE_TYPE, defaultRateType.name)
            return try {
                RateType.valueOf(name ?: defaultRateType.name)
            } catch (_: Exception) {
                defaultRateType
            }
        }
        set(value) = prefs.edit().putString(KEY_LAST_RATE_TYPE, value.name).apply()

    var lastRateValue: Double
        get() = prefs.getFloat(KEY_LAST_RATE_VALUE, defaultRateValue.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(KEY_LAST_RATE_VALUE, value.toFloat()).apply()

    var lastCustomer: String
        get() = prefs.getString(KEY_LAST_CUSTOMER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_CUSTOMER, value).apply()

    var lastFuelRate: Double
        get() = prefs.getFloat(KEY_LAST_FUEL_RATE, defaultFuelConsumptionRate.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(KEY_LAST_FUEL_RATE, value.toFloat()).apply()

    var lastFuelPrice: Double
        get() = prefs.getFloat(KEY_LAST_FUEL_PRICE, defaultFuelPricePerLiter.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(KEY_LAST_FUEL_PRICE, value.toFloat()).apply()

    var lastDriverSalaryPercent: Double
        get() = prefs.getFloat(KEY_LAST_DRIVER_SALARY_PERCENT, defaultDriverSalaryPercent.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(KEY_LAST_DRIVER_SALARY_PERCENT, value.toFloat()).apply()

    // Cost Calculator Preferences
    var calcLoadedDistanceKm: Double
        get() = prefs.getFloat("calc_loaded_dist", 280.0f).toDouble()
        set(value) = prefs.edit().putFloat("calc_loaded_dist", value.toFloat()).apply()

    var calcEmptyDistanceKm: Double
        get() = prefs.getFloat("calc_empty_dist", 100.0f).toDouble()
        set(value) = prefs.edit().putFloat("calc_empty_dist", value.toFloat()).apply()

    var calcWeightTons: Double
        get() = prefs.getFloat("calc_weight_tons", 28.5f).toDouble()
        set(value) = prefs.edit().putFloat("calc_weight_tons", value.toFloat()).apply()

    var calcFuelConsumptionRate: Double
        get() = prefs.getFloat("calc_fuel_rate", defaultFuelConsumptionRate.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat("calc_fuel_rate", value.toFloat()).apply()

    var calcFuelPricePerLiter: Double
        get() = prefs.getFloat("calc_fuel_price", defaultFuelPricePerLiter.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat("calc_fuel_price", value.toFloat()).apply()

    var calcDriverPayMode: String // "PER_KM" or "PERCENT"
        get() = prefs.getString("calc_driver_mode", "PER_KM") ?: "PER_KM"
        set(value) = prefs.edit().putString("calc_driver_mode", value).apply()

    var calcDriverRatePerKm: Double
        get() = prefs.getFloat("calc_driver_per_km", 12.0f).toDouble()
        set(value) = prefs.edit().putFloat("calc_driver_per_km", value.toFloat()).apply()

    var calcDriverSalaryPercent: Double
        get() = prefs.getFloat("calc_driver_percent", defaultDriverSalaryPercent.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat("calc_driver_percent", value.toFloat()).apply()

    var calcDriverPerDiemDaily: Double
        get() = prefs.getFloat("calc_driver_daily", 1500.0f).toDouble()
        set(value) = prefs.edit().putFloat("calc_driver_daily", value.toFloat()).apply()

    var calcPlatonRatePerKm: Double
        get() = prefs.getFloat("calc_platon_per_km", 3.05f).toDouble()
        set(value) = prefs.edit().putFloat("calc_platon_per_km", value.toFloat()).apply()

    var calcTollRoadsCost: Double
        get() = prefs.getFloat("calc_toll_roads", 0.0f).toDouble()
        set(value) = prefs.edit().putFloat("calc_toll_roads", value.toFloat()).apply()

    var calcDepreciationRatePerKm: Double
        get() = prefs.getFloat("calc_deprec_per_km", 5.0f).toDouble()
        set(value) = prefs.edit().putFloat("calc_deprec_per_km", value.toFloat()).apply()

    var calcTripDays: Int
        get() = prefs.getInt("calc_trip_days", 1)
        set(value) = prefs.edit().putInt("calc_trip_days", value).apply()

    var calcDesiredMarginPercent: Double
        get() = prefs.getFloat("calc_margin_percent", 20.0f).toDouble()
        set(value) = prefs.edit().putFloat("calc_margin_percent", value.toFloat()).apply()

    var yandexApiKey: String
        get() = prefs.getString("yandex_api_key", "") ?: ""
        set(value) = prefs.edit().putString("yandex_api_key", value).apply()

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key", value).apply()

    var geminiProxyUrl: String
        get() = prefs.getString("gemini_proxy_url", "") ?: ""
        set(value) = prefs.edit().putString("gemini_proxy_url", value).apply()

    var routeDetourPercent: Double
        get() = prefs.getFloat("route_detour_percent", 5.0f).toDouble()
        set(value) = prefs.edit().putFloat("route_detour_percent", value.toFloat()).apply()

    var autoCalculateDistance: Boolean
        get() = prefs.getBoolean("auto_calculate_distance", true)
        set(value) = prefs.edit().putBoolean("auto_calculate_distance", value).apply()

    fun saveLastTripData(trip: Trip) {
        prefs.edit()
            .putBoolean(KEY_HAS_REMEMBERED_DATA, true)
            .putString(KEY_LAST_TRIP_NUMBER, trip.tripNumber)
            .putString(KEY_LAST_CARGO, trip.cargoType)
            .putString(KEY_LAST_LOADING_LOCATION, trip.loadingLocation)
            .putString(KEY_LAST_UNLOADING_LOCATION, trip.unloadingLocation)
            .putFloat(KEY_LAST_WEIGHT, trip.weightTons.toFloat())
            .putFloat(KEY_LAST_VOLUME, (trip.volumeM3 ?: 0.0).toFloat())
            .putFloat(KEY_LAST_DISTANCE, trip.distanceKm.toFloat())
            .putString(KEY_LAST_RATE_TYPE, trip.rateType.name)
            .putFloat(KEY_LAST_RATE_VALUE, trip.rateValue.toFloat())
            .putString(KEY_LAST_CUSTOMER, trip.customerName)
            .putFloat(KEY_LAST_FUEL_RATE, (trip.fuelConsumptionRate ?: defaultFuelConsumptionRate).toFloat())
            .putFloat(KEY_LAST_FUEL_PRICE, (trip.fuelPricePerLiter ?: defaultFuelPricePerLiter).toFloat())
            .putFloat(KEY_LAST_DRIVER_SALARY_PERCENT, trip.driverSalaryPercent.toFloat())
            .apply()
    }
}
