package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RateType
import com.example.data.model.Trip
import com.example.util.Formatters
import com.example.util.TripNumberUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Зерновоз", appName)
    }

    @Test
    fun `test per ton-km price calculation`() {
        val price = Trip.calculatePrice(
            weight = 25.0,
            distance = 200.0,
            rateType = RateType.PER_TON_KM,
            rateValue = 4.50
        )
        assertEquals(22500.0, price, 0.001)
    }

    @Test
    fun `test fuel calculation`() {
        // 300 km at 38 l/100 km -> 114 liters
        val liters = Trip.calculateFuelLiters(300.0, 38.0)
        assertEquals(114.0, liters, 0.001)

        // 114 liters * 66.00 rub/liter -> 7524 rubles
        val cost = Trip.calculateFuelCost(300.0, 38.0, 66.0)
        assertEquals(7524.0, cost, 0.001)
    }

    @Test
    fun `test formatting helpers`() {
        val weightStr = Formatters.formatWeight(28.50)
        assertEquals("28,50 т", weightStr)

        val distStr = Formatters.formatDistance(320.0)
        assertEquals("320 км", distStr)

        val fuelStr = Formatters.formatFuelLiters(114.0)
        assertEquals("114 л", fuelStr)

        val fuelRateStr = Formatters.formatFuelRate(38.0)
        assertEquals("38 л/100 км", fuelRateStr)
    }

    @Test
    fun `test next trip number auto increment`() {
        assertEquals("Рейс №2", TripNumberUtils.generateNextTripNumber("Рейс №1"))
        assertEquals("Рейс №13", TripNumberUtils.generateNextTripNumber("Рейс №12"))
        assertEquals("Рейс #6", TripNumberUtils.generateNextTripNumber("Рейс #5"))
        assertEquals("Рейс №15", TripNumberUtils.generateNextTripNumber("14"))
        assertEquals("Рейс №1", TripNumberUtils.generateNextTripNumber(null))
        assertEquals("Рейс №1", TripNumberUtils.generateNextTripNumber(""))
    }

    @Test
    fun `test settlement insert value cleans unwanted prefixes`() {
        val s1 = com.example.data.model.Settlement(name = "Каневская", type = "ст-ца")
        assertEquals("Каневская", s1.insertValue)

        val s2 = com.example.data.model.Settlement(name = "Из истории Новороссийск", isFromHistory = true)
        assertEquals("Новороссийск", s2.insertValue)

        val s3 = com.example.data.model.Settlement(name = "Сеть хутор Ленина", isOnlineResult = true)
        assertEquals("хутор Ленина", s3.insertValue)
    }

    @Test
    fun `test backup json creation and validation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = com.example.data.local.SettingsPreferences(context)
        val dummyTrips = listOf(
            Trip(
                id = 1L,
                tripNumber = "Рейс №1",
                cargoType = "Пшеница",
                loadingDate = System.currentTimeMillis(),
                loadingLocation = "Каневская",
                unloadingLocation = "Новороссийск",
                weightTons = 28.5,
                distanceKm = 245.0
            )
        )

        val json = com.example.data.backup.BackupService.createBackupJson(dummyTrips, settings)
        org.junit.Assert.assertTrue(json.contains("Зерновоз"))
        org.junit.Assert.assertTrue(json.contains("Рейс №1"))
        org.junit.Assert.assertTrue(json.contains("Каневская"))
        org.junit.Assert.assertTrue(json.contains("Новороссийск"))
    }

    @Test
    fun `test waybill distance and fuel calculation`() {
        val wb = com.example.data.fleet.Waybill(
            number = "ПЛ-001",
            vehiclePlate = "А 742 КХ 123",
            driverName = "Иванов С. М.",
            startOdometerKm = 1000.0,
            endOdometerKm = 1350.0,
            startFuelLiters = 400.0,
            fuelAddedLiters = 100.0,
            endFuelLiters = 370.0
        )

        org.junit.Assert.assertEquals(350.0, wb.totalDistanceKm, 0.1)
        // Consumed: 400 + 100 - 370 = 130 liters
        org.junit.Assert.assertEquals(130.0, wb.totalFuelConsumedLiters, 0.1)
    }

    @Test
    fun `test fleet document expiration logic`() {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        val expiredDoc = com.example.data.fleet.FleetDocument(
            title = "ОСАГО",
            vehiclePlateOrDriver = "А 742 КХ 123",
            expiryDate = now - oneDay * 5
        )
        org.junit.Assert.assertTrue(expiredDoc.isExpired)
        org.junit.Assert.assertFalse(expiredDoc.isExpiringSoon)

        val expiringSoonDoc = com.example.data.fleet.FleetDocument(
            title = "Техосмотр",
            vehiclePlateOrDriver = "А 742 КХ 123",
            expiryDate = now + oneDay * 12
        )
        org.junit.Assert.assertFalse(expiringSoonDoc.isExpired)
        org.junit.Assert.assertTrue(expiringSoonDoc.isExpiringSoon)

        val validDoc = com.example.data.fleet.FleetDocument(
            title = "КАСКО",
            vehiclePlateOrDriver = "А 742 КХ 123",
            expiryDate = now + oneDay * 180
        )
        org.junit.Assert.assertFalse(validDoc.isExpired)
        org.junit.Assert.assertFalse(validDoc.isExpiringSoon)
    }

    @Test
    fun `test driver salary percent and net profit calculation`() {
        // Freight = 100 000 rub, 20% salary -> 20 000 rub salary
        // Fuel = 15 000 rub, other expenses = 5 000 rub
        // Net profit = 100 000 - (15 000 + 5 000 + 20 000) = 60 000 rub
        val trip = Trip(
            tripNumber = "Рейс №10",
            cargoType = "Пшеница",
            loadingDate = System.currentTimeMillis(),
            loadingLocation = "Каневская",
            unloadingLocation = "Новороссийск",
            weightTons = 25.0,
            distanceKm = 200.0,
            rateType = RateType.FIXED,
            rateValue = 100000.0,
            totalPrice = 100000.0,
            fuelExpenses = 15000.0,
            otherExpenses = 5000.0,
            driverSalaryPercent = 20.0,
            driverSalaryAmount = 0.0
        )

        assertEquals(20000.0, trip.effectiveDriverSalary, 0.001)
        assertEquals(40000.0, trip.totalExpenses, 0.001)
        assertEquals(60000.0, trip.netProfit, 0.001)
    }

    @Test
    fun `test driver explicit salary override`() {
        val trip = Trip(
            tripNumber = "Рейс №11",
            cargoType = "Ячмень",
            loadingDate = System.currentTimeMillis(),
            loadingLocation = "Кущевская",
            unloadingLocation = "Тамань",
            weightTons = 25.0,
            distanceKm = 300.0,
            totalPrice = 120000.0,
            driverSalaryPercent = 20.0,
            driverSalaryAmount = 25000.0 // explicit bonus/fixed amount
        )

        assertEquals(25000.0, trip.effectiveDriverSalary, 0.001)
    }

    @Test
    fun `test report variants and headers`() {
        val reportTypes = com.example.ui.components.ReportType.values()
        assertEquals(5, reportTypes.size)
        org.junit.Assert.assertTrue(reportTypes.contains(com.example.ui.components.ReportType.FINANCIAL))
        org.junit.Assert.assertTrue(reportTypes.contains(com.example.ui.components.ReportType.PAYROLL))
        org.junit.Assert.assertTrue(reportTypes.contains(com.example.ui.components.ReportType.CARGO))
        org.junit.Assert.assertTrue(reportTypes.contains(com.example.ui.components.ReportType.FUEL_LOGISTICS))
        org.junit.Assert.assertTrue(reportTypes.contains(com.example.ui.components.ReportType.REGISTRY))
    }

    @Test
    fun `test trip default unloading date equals loading date when set`() {
        val loadTime = System.currentTimeMillis()
        val trip = Trip(
            tripNumber = "Рейс №12",
            cargoType = "Кукуруза",
            loadingDate = loadTime,
            unloadingDate = loadTime,
            loadingLocation = "Тимашевск",
            unloadingLocation = "Новороссийск",
            weightTons = 30.0,
            distanceKm = 180.0
        )
        assertEquals(trip.loadingDate, trip.unloadingDate)
    }
}
