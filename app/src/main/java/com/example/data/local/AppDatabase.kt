package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.fleet.Driver
import com.example.data.fleet.DriverStatus
import com.example.data.fleet.FleetDocument
import com.example.data.fleet.DocumentType
import com.example.data.fleet.ServiceRecord
import com.example.data.fleet.ServiceType
import com.example.data.fleet.Vehicle
import com.example.data.fleet.VehicleStatus
import com.example.data.fleet.VehicleType
import com.example.data.fleet.Waybill
import com.example.data.fleet.WaybillStatus
import com.example.data.fleet.FuelRecord
import com.example.data.local.fleet.DriverDao
import com.example.data.local.fleet.FleetDocumentDao
import com.example.data.local.fleet.ServiceRecordDao
import com.example.data.local.fleet.VehicleDao
import com.example.data.local.fleet.WaybillDao
import com.example.data.local.fleet.FuelDao
import androidx.room.migration.Migration
import com.example.data.model.RateType
import com.example.data.model.RouteCacheEntity
import com.example.data.model.Trip
import com.example.data.model.TripStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Trip::class,
        Vehicle::class,
        Driver::class,
        ServiceRecord::class,
        Waybill::class,
        FleetDocument::class,
        RouteCacheEntity::class,
        FuelRecord::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun driverDao(): DriverDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun waybillDao(): WaybillDao
    abstract fun fleetDocumentDao(): FleetDocumentDao
    abstract fun routeCacheDao(): RouteCacheDao
    abstract fun fuelDao(): FuelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `route_cache` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `originNormalized` TEXT NOT NULL,
                        `destinationNormalized` TEXT NOT NULL,
                        `distanceKm` REAL NOT NULL,
                        `provider` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_route_cache_originNormalized_destinationNormalized` ON `route_cache` (`originNormalized`, `destinationNormalized`)"
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                var createdInstance: AppDatabase? = null
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "grain_truck_database"
                )
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(DatabaseCallback(scope) { createdInstance ?: INSTANCE })
                .build()
                createdInstance = instance
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope,
            private val databaseProvider: () -> AppDatabase?
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope.launch(Dispatchers.IO) {
                    try {
                        val database = databaseProvider() ?: INSTANCE
                        database?.let { dbInstance ->
                            populateInitialTrips(dbInstance.tripDao())
                            populateInitialFleet(
                                dbInstance.vehicleDao(),
                                dbInstance.driverDao(),
                                dbInstance.serviceRecordDao(),
                                dbInstance.waybillDao(),
                                dbInstance.fleetDocumentDao()
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AppDatabase", "Failed to populate initial data", e)
                    }
                }
            }

            suspend fun populateInitialTrips(dao: TripDao) {
                val now = System.currentTimeMillis()
                val oneDay = 24L * 60 * 60 * 1000

                val sampleTrip1 = Trip(
                    tripNumber = "Рейс №1",
                    cargoType = "Пшеница (3 класс)",
                    loadingDate = now - oneDay * 2,
                    unloadingDate = now - oneDay * 2 + 10 * 3600 * 1000,
                    loadingLocation = "ст. Каневская, Элеватор ЗАО 'Нива'",
                    unloadingLocation = "Новороссийск, Зерновой Терминал (НЗТ)",
                    weightTons = 28.50,
                    volumeM3 = 36.0,
                    distanceKm = 295.0,
                    rateType = RateType.PER_TON_KM,
                    rateValue = 4.60,
                    totalPrice = Trip.calculatePrice(28.50, 295.0, RateType.PER_TON_KM, 4.60),
                    status = TripStatus.PAID,
                    ttnNumber = "ТТН-04821",
                    customerName = "ООО 'АгроЭкспортЮг'",
                    driverName = "Иванов С. М.",
                    truckPlate = "А 742 КХ 123",
                    fuelExpenses = 7400.0,
                    otherExpenses = 1200.0,
                    driverSalaryPercent = 20.0,
                    driverSalaryAmount = 7733.0,
                    fuelConsumptionRate = 38.0,
                    fuelPricePerLiter = 66.0,
                    fuelLiters = 112.1,
                    notes = "Влажность 12.8%, клейковина 24%. Погрузка через верхний люк."
                )

                val sampleTrip2 = Trip(
                    tripNumber = "Рейс №2",
                    cargoType = "Подсолнечник",
                    loadingDate = now - oneDay,
                    unloadingDate = now - oneDay + 8 * 3600 * 1000,
                    loadingLocation = "х. Бейсужек, Ток АФ 'Победа'",
                    unloadingLocation = "г. Усть-Лабинск, Маслозавод",
                    weightTons = 21.40,
                    volumeM3 = 45.0,
                    distanceKm = 145.0,
                    rateType = RateType.PER_TON_KM,
                    rateValue = 5.20,
                    totalPrice = Trip.calculatePrice(21.40, 145.0, RateType.PER_TON_KM, 5.20),
                    status = TripStatus.UNLOADED,
                    ttnNumber = "ТТН-04855",
                    customerName = "ГК 'ЭФКО'",
                    driverName = "Иванов С. М.",
                    truckPlate = "А 742 КХ 123",
                    fuelExpenses = 3500.0,
                    otherExpenses = 600.0,
                    driverSalaryPercent = 20.0,
                    driverSalaryAmount = 3227.0,
                    fuelConsumptionRate = 37.0,
                    fuelPricePerLiter = 65.5,
                    fuelLiters = 53.65,
                    notes = "Масличность 49%, сорность 1.5%. Оплата по безналу с НДС."
                )

                val sampleTrip3 = Trip(
                    tripNumber = "Рейс №3",
                    cargoType = "Кукуруза",
                    loadingDate = now,
                    unloadingDate = null,
                    loadingLocation = "ст. Павловская, Поле №14",
                    unloadingLocation = "Тамань, Группа 'ОТЭКО'",
                    weightTons = 27.80,
                    volumeM3 = 38.0,
                    distanceKm = 360.0,
                    rateType = RateType.PER_TON_KM,
                    rateValue = 4.80,
                    totalPrice = Trip.calculatePrice(27.80, 360.0, RateType.PER_TON_KM, 4.80),
                    status = TripStatus.IN_TRANSIT,
                    ttnNumber = "ТТН-04902",
                    customerName = "ТД 'РИФ'",
                    driverName = "Иванов С. М.",
                    truckPlate = "А 742 КХ 123",
                    fuelExpenses = 9050.0,
                    otherExpenses = 1500.0,
                    driverSalaryPercent = 20.0,
                    driverSalaryAmount = 9608.0,
                    fuelConsumptionRate = 38.5,
                    fuelPricePerLiter = 66.0,
                    fuelLiters = 138.6,
                    notes = "Прямая погрузка из-под комбайна. В пути, планируемое прибытие к 21:00."
                )

                dao.insertTrip(sampleTrip1)
                dao.insertTrip(sampleTrip2)
                dao.insertTrip(sampleTrip3)
            }

            suspend fun populateInitialFleet(
                vehicleDao: VehicleDao,
                driverDao: DriverDao,
                serviceDao: ServiceRecordDao,
                waybillDao: WaybillDao,
                docDao: FleetDocumentDao
            ) {
                val now = System.currentTimeMillis()
                val oneDay = 24L * 60 * 60 * 1000

                // 1. Автопарк (ТС)
                val v1 = Vehicle(
                    plateNumber = "А 742 КХ 123",
                    model = "KAMAZ-54901 (K5)",
                    type = VehicleType.TRACTOR,
                    vin = "XTC549010N1234567",
                    year = 2022,
                    status = VehicleStatus.ACTIVE,
                    currentOdometerKm = 148500.0,
                    fuelTankCapacityLiters = 600.0,
                    currentFuelLiters = 390.0,
                    assignedDriverName = "Иванов Сергей Михайлович",
                    notes = "Установлен тахограф СКЗИ, датчики уровня топлива Omnicomm"
                )

                val v2 = Vehicle(
                    plateNumber = "В 518 МР 123",
                    model = "Scania R450",
                    type = VehicleType.TRACTOR,
                    vin = "YS2R4X20005432190",
                    year = 2021,
                    status = VehicleStatus.AVAILABLE,
                    currentOdometerKm = 210400.0,
                    fuelTankCapacityLiters = 700.0,
                    currentFuelLiters = 540.0,
                    assignedDriverName = "Петров Алексей Владимирович",
                    notes = "Ретардер, холодильник, высокая кабина Topline"
                )

                val v3 = Vehicle(
                    plateNumber = "ЕК 4120 23",
                    model = "Тонар 9523-0000020",
                    type = VehicleType.TRAILER,
                    vin = "X9K952300N0009876",
                    year = 2022,
                    status = VehicleStatus.ACTIVE,
                    currentOdometerKm = 148500.0,
                    fuelTankCapacityLiters = 0.0,
                    currentFuelLiters = 0.0,
                    assignedDriverName = "Иванов Сергей Михайлович",
                    notes = "Алюминиевый кузов 45 м³, тент-штора, 4 оси SAF"
                )

                val v4 = Vehicle(
                    plateNumber = "АВ 8891 23",
                    model = "Grunwald зерновоз",
                    type = VehicleType.TRAILER,
                    vin = "XW8334400P0005612",
                    year = 2023,
                    status = VehicleStatus.AVAILABLE,
                    currentOdometerKm = 85000.0,
                    fuelTankCapacityLiters = 0.0,
                    currentFuelLiters = 0.0,
                    assignedDriverName = "Петров Алексей Владимирович",
                    notes = "Объем 50 м³, стальной кузов Hardox, распашные ворота"
                )

                val v5 = Vehicle(
                    plateNumber = "У 903 СТ 123",
                    model = "KAMAZ-65207 зерновоз",
                    type = VehicleType.TRUCK,
                    vin = "XTC652070M0003412",
                    year = 2020,
                    status = VehicleStatus.SERVICE,
                    currentOdometerKm = 280000.0,
                    fuelTankCapacityLiters = 450.0,
                    currentFuelLiters = 110.0,
                    assignedDriverName = "Сидоров Дмитрий Николаевич",
                    notes = "Самосвал с боковой разгрузкой, на плановой замене сцепления"
                )

                vehicleDao.insertVehicle(v1)
                vehicleDao.insertVehicle(v2)
                vehicleDao.insertVehicle(v3)
                vehicleDao.insertVehicle(v4)
                vehicleDao.insertVehicle(v5)

                // 2. Водители
                val d1 = Driver(
                    fullName = "Иванов Сергей Михайлович",
                    phone = "+7 (918) 456-78-90",
                    licenseNumber = "23 45 678912",
                    categories = "B, C, CE",
                    hasDopog = false,
                    hasSkziCard = true,
                    status = DriverStatus.ON_TRIP,
                    shiftSchedule = "15/15 вахта",
                    assignedVehiclePlate = "А 742 КХ 123",
                    experienceYears = 12,
                    salaryPercent = 20.0,
                    notes = "Ответственный, опыт перевозок зерновых по Краснодарскому краю и в порт Новороссийск"
                )

                val d2 = Driver(
                    fullName = "Петров Алексей Владимирович",
                    phone = "+7 (928) 112-33-44",
                    licenseNumber = "23 12 345678",
                    categories = "B, C, CE",
                    hasDopog = true,
                    hasSkziCard = true,
                    status = DriverStatus.AVAILABLE,
                    shiftSchedule = "15/15 вахта",
                    assignedVehiclePlate = "В 518 МР 123",
                    experienceYears = 15,
                    salaryPercent = 22.0,
                    notes = "Есть допуск ДОПОГ, опыт дальних рейсов Ростов - Воронеж"
                )

                val d3 = Driver(
                    fullName = "Сидоров Дмитрий Николаевич",
                    phone = "+7 (905) 777-88-99",
                    licenseNumber = "61 99 887766",
                    categories = "B, C, CE",
                    hasDopog = false,
                    hasSkziCard = true,
                    status = DriverStatus.REST,
                    shiftSchedule = "5/2 постоянный",
                    assignedVehiclePlate = "У 903 СТ 123",
                    experienceYears = 8,
                    salaryPercent = 18.0,
                    notes = "Работает по плечу элеватор - завод до 200 км"
                )

                driverDao.insertDriver(d1)
                driverDao.insertDriver(d2)
                driverDao.insertDriver(d3)

                // 3. Сервис и ТО
                val s1 = ServiceRecord(
                    vehiclePlate = "А 742 КХ 123",
                    serviceType = ServiceType.TO_1,
                    date = now - oneDay * 12,
                    odometerKm = 145000.0,
                    description = "Регламентное ТО-1: замена моторного масла Rimula 10W-40 (36 л), фильтра масляного, топливного тонкой очистки, воздушного. Шприцевание карданного вала и шкворней.",
                    cost = 42500.0,
                    orderNumber = "ЗН-1402",
                    serviceStation = "Камаз-Центр Кубань",
                    isCompleted = true,
                    nextPlannedKm = 175000.0,
                    nextPlannedDate = now + oneDay * 75
                )

                val s2 = ServiceRecord(
                    vehiclePlate = "В 518 МР 123",
                    serviceType = ServiceType.TO_2,
                    date = now - oneDay * 25,
                    odometerKm = 205000.0,
                    description = "Комплексное ТО-2: замена масла ДВС, КПП Opticruise и ведущего моста. Замена осушителя воздуха, регулировка клапанов, компьютерная диагностика Scania.",
                    cost = 78000.0,
                    orderNumber = "ЗН-8941",
                    serviceStation = "Скания Сервис Юг (Краснодар)",
                    isCompleted = true,
                    nextPlannedKm = 245000.0,
                    nextPlannedDate = now + oneDay * 90
                )

                val s3 = ServiceRecord(
                    vehiclePlate = "У 903 СТ 123",
                    serviceType = ServiceType.REPAIR,
                    date = now - oneDay * 2,
                    odometerKm = 280000.0,
                    description = "Внеплановый ремонт: замена комплекта сцепления Sachs, выжимного подшипника, прокачка ПГУ.",
                    cost = 63000.0,
                    orderNumber = "ЗН-0391",
                    serviceStation = "Грузовой бокс АгроТех",
                    isCompleted = false,
                    nextPlannedKm = null,
                    nextPlannedDate = null
                )

                serviceDao.insertServiceRecord(s1)
                serviceDao.insertServiceRecord(s2)
                serviceDao.insertServiceRecord(s3)

                // 4. Рейсы и путевые
                val w1 = Waybill(
                    number = "ПЛ-0089",
                    date = now,
                    vehiclePlate = "А 742 КХ 123",
                    trailerPlate = "ЕК 4120 23",
                    driverName = "Иванов С. М.",
                    startOdometerKm = 148205.0,
                    endOdometerKm = null,
                    startFuelLiters = 480.0,
                    endFuelLiters = null,
                    fuelAddedLiters = 200.0,
                    routeDescription = "ст. Каневская -> Новороссийск (НЗТ) -> ст. Каневская",
                    medicalCheckPassed = true,
                    technicalCheckPassed = true,
                    status = WaybillStatus.ISSUED,
                    notes = "Выпуск на линию 06:30. Механик: Ковалев А. В., Врач: Семенова Е. И."
                )

                val w2 = Waybill(
                    number = "ПЛ-0088",
                    date = now - oneDay,
                    vehiclePlate = "В 518 МР 123",
                    trailerPlate = "АВ 8891 23",
                    driverName = "Петров А. В.",
                    startOdometerKm = 209950.0,
                    endOdometerKm = 210400.0,
                    startFuelLiters = 620.0,
                    endFuelLiters = 460.0,
                    fuelAddedLiters = 0.0,
                    routeDescription = "ст. Павловская -> порт Тамань (ОТЭКО)",
                    medicalCheckPassed = true,
                    technicalCheckPassed = true,
                    status = WaybillStatus.CLOSED,
                    notes = "Рейс завершен без происшествий. Расход по норме."
                )

                waybillDao.insertWaybill(w1)
                waybillDao.insertWaybill(w2)

                // 5. Документы и сроки
                val dDoc1 = FleetDocument(
                    title = "Полис ОСАГО (Тягач)",
                    docType = DocumentType.OSAGO,
                    vehiclePlateOrDriver = "А 742 КХ 123",
                    seriesAndNumber = "ХХХ 0345892110",
                    issueDate = now - oneDay * 300,
                    expiryDate = now + oneDay * 65, // ~65 дней (Действует)
                    issuingAuthority = "СПАО 'Ингосстрах'",
                    cost = 24500.0,
                    notes = "Без ограничений водителей, КБМ 0.5"
                )

                val dDoc2 = FleetDocument(
                    title = "Диагностическая карта (Техосмотр)",
                    docType = DocumentType.DIAGNOSTIC_CARD,
                    vehiclePlateOrDriver = "А 742 КХ 123",
                    seriesAndNumber = "1420958190382",
                    issueDate = now - oneDay * 350,
                    expiryDate = now + oneDay * 15, // 15 дней (Истекает скоро!)
                    issuingAuthority = "ПТО №04912 ООО 'Техосмотр-Юг'",
                    cost = 3500.0,
                    notes = "Срочно записаться на прохождение повторного осмотра"
                )

                val dDoc3 = FleetDocument(
                    title = "Полис КАСКО (Тягач)",
                    docType = DocumentType.KASKO,
                    vehiclePlateOrDriver = "В 518 МР 123",
                    seriesAndNumber = "КАС-881203/23",
                    issueDate = now - oneDay * 380,
                    expiryDate = now - oneDay * 15, // Просрочен на 15 дней!
                    issuingAuthority = "ПАО СК 'Росгосстрах'",
                    cost = 115000.0,
                    notes = "Требуется продление договора страхования!"
                )

                val dDoc4 = FleetDocument(
                    title = "Поверка блока СКЗИ тахографа",
                    docType = DocumentType.TACHOGRAPH_CALIBRATION,
                    vehiclePlateOrDriver = "А 742 КХ 123",
                    seriesAndNumber = "НКМ-2022-8419",
                    issueDate = now - oneDay * 200,
                    expiryDate = now + oneDay * 530, // Действует долго
                    issuingAuthority = "Мастерская РФ 0422 'ТахоМастер'",
                    cost = 28000.0,
                    notes = "Тахограф VDO DTCO 3283"
                )

                val dDoc5 = FleetDocument(
                    title = "Спецпропуск в порт Новороссийск",
                    docType = DocumentType.PASS,
                    vehiclePlateOrDriver = "А 742 КХ 123 / Иванов С. М.",
                    seriesAndNumber = "ПОРТ-НВР-2026/89",
                    issueDate = now - oneDay * 90,
                    expiryDate = now + oneDay * 92,
                    issuingAuthority = "Служба безопасности НМТП",
                    cost = 5000.0,
                    notes = "Электронный пропуск на территорию терминала"
                )

                docDao.insertDocument(dDoc1)
                docDao.insertDocument(dDoc2)
                docDao.insertDocument(dDoc3)
                docDao.insertDocument(dDoc4)
                docDao.insertDocument(dDoc5)
            }
        }
    }
}
