package com.example.data.fleet

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 1. Автопарк (ТС): Реестр тягачей, прицепов и спецтехники
 */
enum class VehicleType(val label: String) {
    TRACTOR("Тягач"),
    TRAILER("Полуприцеп-зерновоз"),
    TRUCK("Самосвал-одиночка"),
    SPECIAL("Спецтехника/Погрузчик")
}

enum class VehicleStatus(val label: String) {
    ACTIVE("В рейсе"),
    AVAILABLE("Готов к рейсу"),
    SERVICE("На ТО / Ремонте"),
    DECOMMISSIONED("Списан / В резерве")
}

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plateNumber: String, // Госномер (например: А 742 КХ 123)
    val model: String, // Марка и модель (например: KAMAZ-54901, Scania R450, Тонар 9523)
    val type: VehicleType = VehicleType.TRACTOR,
    val vin: String = "",
    val year: Int = 2021,
    val status: VehicleStatus = VehicleStatus.AVAILABLE,
    val currentOdometerKm: Double = 145000.0,
    val fuelTankCapacityLiters: Double = 600.0,
    val currentFuelLiters: Double = 420.0,
    val assignedDriverName: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 2. Водители: База водительского состава
 */
enum class DriverStatus(val label: String) {
    AVAILABLE("Свободен"),
    ON_TRIP("В рейсе"),
    REST("Выходной"),
    VACATION("В отпуске"),
    SICK("Больничный")
}

@Entity(tableName = "drivers")
data class Driver(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String, // ФИО (например: Иванов Сергей Михайлович)
    val phone: String = "",
    val licenseNumber: String = "", // Номер ВУ
    val categories: String = "B, C, CE", // Открытые категории
    val hasDopog: Boolean = false, // ДОПОГ допуск
    val hasSkziCard: Boolean = true, // Карта тахографа СКЗИ
    val status: DriverStatus = DriverStatus.AVAILABLE,
    val shiftSchedule: String = "15/15 вахта", // График: 15/15, 20/10, 5/2
    val assignedVehiclePlate: String = "", // Закрепленный тягач
    val experienceYears: Int = 10,
    val salaryPercent: Double = 20.0, // Процент от фрахта (например 20%)
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 3. Сервис и ТО: Журнал регламентных и внеплановых работ
 */
enum class ServiceType(val label: String) {
    TO_1("ТО-1 (Регламент)"),
    TO_2("ТО-2 (Расширенное)"),
    SEASONAL("Сезонное обслуживание"),
    REPAIR("Внеплановый ремонт"),
    TIRE("Шиномонтаж / Колеса")
}

@Entity(tableName = "service_records")
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehiclePlate: String,
    val serviceType: ServiceType = ServiceType.TO_1,
    val date: Long = System.currentTimeMillis(),
    val odometerKm: Double = 0.0,
    val description: String, // Описание работ (замена масла, фильтров, колодок)
    val cost: Double = 0.0, // Стоимость
    val orderNumber: String = "", // Номер заказ-наряда
    val serviceStation: String = "", // СТО / Сервис
    val isCompleted: Boolean = true,
    val nextPlannedKm: Double? = null, // Следующее ТО на пробеге
    val nextPlannedDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 4. Рейсы и путевые: Оперативная работа парка, путевые листы
 */
enum class WaybillStatus(val label: String) {
    ISSUED("На линии"),
    CLOSED("Закрыт")
}

@Entity(tableName = "waybills")
data class Waybill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String, // Номер ПЛ (например: ПЛ-0042)
    val date: Long = System.currentTimeMillis(),
    val vehiclePlate: String,
    val trailerPlate: String = "",
    val driverName: String,
    val startOdometerKm: Double = 0.0, // Одометр при выезде
    val endOdometerKm: Double? = null, // Одометр при возвращении
    val startFuelLiters: Double = 0.0, // Топливо при выезде
    val endFuelLiters: Double? = null, // Топливо при возвращении
    val fuelAddedLiters: Double = 0.0, // Заправлено топлива
    val routeDescription: String = "", // Маршрут (например: Каневская -> Новороссийск НЗТ)
    val medicalCheckPassed: Boolean = true, // Предрейсовый медосмотр
    val technicalCheckPassed: Boolean = true, // Предрейсовый техосмотр
    val status: WaybillStatus = WaybillStatus.ISSUED,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalDistanceKm: Double
        get() = if (endOdometerKm != null && endOdometerKm >= startOdometerKm) {
            endOdometerKm - startOdometerKm
        } else 0.0

    val totalFuelConsumedLiters: Double
        get() = if (endFuelLiters != null) {
            val consumed = startFuelLiters + fuelAddedLiters - endFuelLiters
            if (consumed > 0) consumed else 0.0
        } else 0.0
}

/**
 * 5. Документы и сроки: Контроль легитимности эксплуатации ТС
 */
enum class DocumentType(val label: String) {
    OSAGO("Полис ОСАГО"),
    KASKO("Полис КАСКО"),
    DIAGNOSTIC_CARD("Техосмотр (ДК)"),
    TACHOGRAPH_CALIBRATION("Поверка тахографа"),
    PASS("Пропуск (СК/МКАД/Порт)"),
    MEDICAL("Медсправка водителя")
}

@Entity(tableName = "fleet_documents")
data class FleetDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, // Название (например: ОСАГО на тягач)
    val docType: DocumentType = DocumentType.OSAGO,
    val vehiclePlateOrDriver: String, // Привязка к ТС или водителю
    val seriesAndNumber: String = "",
    val issueDate: Long = System.currentTimeMillis(),
    val expiryDate: Long, // Дата окончания срока действия
    val issuingAuthority: String = "", // Кем выдан (Ингосстрах, РСА, Росстандарт)
    val cost: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Статус срока действия:
     * - Просрочен (осталось < 0 дней)
     * - Истекает скоро (осталось <= 30 дней)
     * - Действует (> 30 дней)
     */
    val daysRemaining: Long
        get() {
            val now = System.currentTimeMillis()
            val diff = expiryDate - now
            return diff / (24L * 60 * 60 * 1000)
        }

    val isExpired: Boolean
        get() = daysRemaining < 0

    val isExpiringSoon: Boolean
        get() = daysRemaining in 0..30
}

/**
 * 6. Fuel Record: Tracking gas station receipts
 */
@Entity(tableName = "fuel_records")
data class FuelRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehiclePlate: String,
    val date: Long = System.currentTimeMillis(),
    val liters: Double,
    val pricePerLiter: Double,
    val totalCost: Double,
    val stationName: String = "",
    val odometerKm: Double? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
