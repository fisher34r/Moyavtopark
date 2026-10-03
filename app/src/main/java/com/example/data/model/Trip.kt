package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripNumber: String,
    val cargoType: String,
    val loadingDate: Long,
    val unloadingDate: Long? = null,
    val loadingLocation: String,
    val unloadingLocation: String,
    val weightTons: Double,
    val volumeM3: Double? = null,
    val distanceKm: Double,
    val rateType: RateType = RateType.PER_TON_KM,
    val rateValue: Double = 4.5,
    val totalPrice: Double = 0.0,
    val status: TripStatus = TripStatus.IN_TRANSIT,
    val ttnNumber: String = "",
    val customerName: String = "",
    val driverName: String = "",
    val truckPlate: String = "",
    val fuelExpenses: Double = 0.0,
    val otherExpenses: Double = 0.0,
    val driverSalaryPercent: Double = 20.0, // Процент зарплаты водителя от суммы фрахта
    val driverSalaryAmount: Double = 0.0,   // Рассчитанная или явная сумма зарплаты водителя (₽)
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val fuelConsumptionRate: Double? = 38.0, // Норма расхода топлива (л / 100 км)
    val fuelPricePerLiter: Double? = 66.0,  // Цена топлива за литр (₽ / л)
    val fuelLiters: Double? = null           // Израсходовано литров топлива
) {
    /**
     * Тонно-километры (т·км) - ключевая транспортная работа зерновоза
     */
    val tonKilometers: Double
        get() = weightTons * distanceKm

    /**
     * Эффективная начисленная зарплата водителя за рейс
     */
    val effectiveDriverSalary: Double
        get() = when {
            driverSalaryAmount > 0.0 -> driverSalaryAmount
            driverSalaryPercent > 0.0 && totalPrice > 0.0 -> totalPrice * (driverSalaryPercent / 100.0)
            else -> 0.0
        }

    /**
     * Затраченное топливо в литрах
     */
    val effectiveFuelLiters: Double
        get() = when {
            fuelLiters != null && fuelLiters > 0 -> fuelLiters
            fuelConsumptionRate != null && fuelConsumptionRate > 0 && distanceKm > 0 ->
                (distanceKm / 100.0) * fuelConsumptionRate
            fuelExpenses > 0 && (fuelPricePerLiter != null && fuelPricePerLiter > 0) ->
                fuelExpenses / fuelPricePerLiter
            else -> 0.0
        }

    /**
     * Эффективные затраты на топливо (явные расходы или расчет по литрам и цене)
     */
    val effectiveFuelExpenses: Double
        get() = when {
            fuelExpenses > 0.0 -> fuelExpenses
            effectiveFuelLiters > 0.0 && (fuelPricePerLiter != null && fuelPricePerLiter > 0.0) ->
                effectiveFuelLiters * fuelPricePerLiter
            effectiveFuelLiters > 0.0 -> effectiveFuelLiters * 66.0
            else -> 0.0
        }

    /**
     * Суммарные расходы в рейсе (топливо + зарплата водителя + сопутствующие расходы)
     */
    val totalExpenses: Double
        get() = effectiveFuelExpenses + otherExpenses + effectiveDriverSalary

    /**
     * Чистая прибыль владельца автопарка / перевозчика
     */
    val netProfit: Double
        get() = totalPrice - totalExpenses

    /**
     * Фактическая ставка за тонну
     */
    val effectiveRatePerTon: Double
        get() = if (weightTons > 0) totalPrice / weightTons else 0.0

    /**
     * Фактическая ставка за километр
     */
    val effectiveRatePerKm: Double
        get() = if (distanceKm > 0) totalPrice / distanceKm else 0.0

    companion object {
        fun calculatePrice(
            weight: Double,
            distance: Double,
            rateType: RateType,
            rateValue: Double
        ): Double {
            return when (rateType) {
                RateType.PER_TON_KM -> weight * distance * rateValue
                RateType.PER_TON -> weight * rateValue
                RateType.PER_KM -> distance * rateValue
                RateType.FIXED -> rateValue
            }
        }

        fun calculateFuelLiters(distanceKm: Double, rateLitersPer100Km: Double): Double {
            return if (distanceKm > 0 && rateLitersPer100Km > 0) {
                (distanceKm / 100.0) * rateLitersPer100Km
            } else 0.0
        }

        fun calculateFuelCost(
            distanceKm: Double,
            rateLitersPer100Km: Double,
            pricePerLiter: Double
        ): Double {
            return calculateFuelLiters(distanceKm, rateLitersPer100Km) * pricePerLiter
        }

        fun calculateDriverSalary(totalPrice: Double, percent: Double): Double {
            return if (totalPrice > 0 && percent > 0) totalPrice * (percent / 100.0) else 0.0
        }
    }
}
