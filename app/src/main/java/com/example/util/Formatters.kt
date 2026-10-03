package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val symbols = DecimalFormatSymbols(Locale.forLanguageTag("ru-RU")).apply {
        groupingSeparator = ' '
        decimalSeparator = ','
    }

    private val moneyFormat = DecimalFormat("#,##0.##", symbols)
    private val moneyExactFormat = DecimalFormat("#,##0.00", symbols)
    private val weightFormat = DecimalFormat("#,##0.00", symbols)
    private val integerFormat = DecimalFormat("#,##0", symbols)
    private val distanceFormat = DecimalFormat("#,##0.#", symbols)
    private val fuelFormat = DecimalFormat("#,##0.#", symbols)

    fun formatWeightKgExact(kg: Long): String {
        return integerFormat.format(kg)
    }

    fun formatWeightKgExact(kg: Double): String {
        return integerFormat.format(kg.toLong())
    }

    fun formatMoney(amount: Double): String {
        return "${moneyFormat.format(amount)} ₽"
    }

    fun formatMoneyExact(amount: Double): String {
        return "${moneyExactFormat.format(amount)} ₽"
    }

    fun formatMoneyRub(amount: Double): String {
        return "${moneyExactFormat.format(amount)} руб."
    }

    fun formatWeight(tons: Double): String {
        return "${weightFormat.format(tons)} т"
    }

    fun formatDistance(km: Double): String {
        return "${distanceFormat.format(km)} км"
    }

    fun formatTonKm(tkm: Double): String {
        return "${moneyFormat.format(tkm)} т·км"
    }

    fun formatFuelLiters(liters: Double): String {
        return "${fuelFormat.format(liters)} л"
    }

    fun formatFuelRate(rate: Double): String {
        return "${fuelFormat.format(rate)} л/100 км"
    }

    fun formatFuelPrice(price: Double): String {
        return "${moneyExactFormat.format(price)}/л"
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "—"
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("ru"))
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        if (timestamp <= 0) return "—"
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("ru"))
        return sdf.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        if (timestamp <= 0) return "—"
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("ru"))
        return sdf.format(Date(timestamp))
    }
}
