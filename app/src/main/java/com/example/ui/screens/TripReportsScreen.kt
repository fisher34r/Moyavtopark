package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import com.example.ui.components.CropReportItem
import com.example.ui.components.DriverPayrollItem
import com.example.ui.components.ReportDocumentPreviewCard
import com.example.ui.components.ReportType
import com.example.util.ReportExportHelper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Trip
import com.example.data.model.TripStatus
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.TripViewModel
import com.example.util.Formatters
import java.util.Calendar
import java.util.Locale

enum class ReportPeriod(val title: String) {
    ALL_TIME("Все время"),
    TODAY("Сегодня"),
    WEEK("Неделя"),
    MONTH("Текущий месяц"),
    QUARTER("Квартал"),
    YEAR("Год"),
    CUSTOM("Свой период")
}

enum class ReportDisplayMode(val title: String) {
    ALL("Полный отчет"),
    DOCUMENT("Печатная форма"),
    CARDS("Сводка и показатели")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripReportsScreen(
    viewModel: TripViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val allTrips by viewModel.allTrips.collectAsStateWithLifecycle()
    val registeredDriversList by viewModel.registeredDrivers.collectAsStateWithLifecycle()
    val registeredVehiclesList by viewModel.registeredVehicles.collectAsStateWithLifecycle()

    var isFiltersSpoilerExpanded by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.ALL_TIME) }
    var selectedReportType by remember { mutableStateOf(ReportType.REGISTRY) }
    var displayMode by remember { mutableStateOf(ReportDisplayMode.ALL) }

    var selectedDriver by remember { mutableStateOf<String?>(null) }
    var selectedTruck by remember { mutableStateOf<String?>(null) }

    val availableDrivers = remember(allTrips, registeredDriversList) {
        val fromTrips = allTrips.map { it.driverName.trim() }.filter { it.isNotBlank() }
        val fromReg = registeredDriversList.map { it.fullName.trim() }.filter { it.isNotBlank() }
        (fromTrips + fromReg).distinct().sorted()
    }

    val availableTrucks = remember(allTrips, registeredVehiclesList) {
        val fromTrips = allTrips.map { it.truckPlate.trim() }.filter { it.isNotBlank() }
        val fromReg = registeredVehiclesList.map { it.plateNumber.trim() }.filter { it.isNotBlank() }
        (fromTrips + fromReg).distinct().sorted()
    }

    val now = remember { System.currentTimeMillis() }
    val oneDayMs = 24L * 60 * 60 * 1000

    var customStartDate by remember {
        mutableLongStateOf(now - 30L * oneDayMs)
    }
    var customEndDate by remember {
        mutableLongStateOf(now)
    }

    // Filter trips according to chosen period
    val periodTrips = remember(allTrips, selectedPeriod, customStartDate, customEndDate) {
        when (selectedPeriod) {
            ReportPeriod.ALL_TIME -> allTrips
            ReportPeriod.TODAY -> {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val endOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                allTrips.filter { it.loadingDate in startOfDay..endOfDay }
            }
            ReportPeriod.WEEK -> {
                val startOfWeek = now - 7L * oneDayMs
                allTrips.filter { it.loadingDate >= startOfWeek }
            }
            ReportPeriod.MONTH -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                allTrips.filter { it.loadingDate >= startOfMonth }
            }
            ReportPeriod.QUARTER -> {
                val startOfQuarter = now - 90L * oneDayMs
                allTrips.filter { it.loadingDate >= startOfQuarter }
            }
            ReportPeriod.YEAR -> {
                val startOfYear = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                allTrips.filter { it.loadingDate >= startOfYear }
            }
            ReportPeriod.CUSTOM -> {
                val endOfCustomDay = customEndDate + oneDayMs
                allTrips.filter { it.loadingDate in customStartDate..endOfCustomDay }
            }
        }
    }

    // Filter trips by Vehicle (ТС) and Driver (водитель)
    val filteredTrips = remember(periodTrips, selectedDriver, selectedTruck) {
        periodTrips.filter { trip ->
            val matchDriver = selectedDriver == null || trip.driverName.equals(selectedDriver, ignoreCase = true)
            val matchTruck = selectedTruck == null || trip.truckPlate.equals(selectedTruck, ignoreCase = true)
            matchDriver && matchTruck
        }
    }

    // Calculations for the filtered selection
    val tripCount = filteredTrips.size
    val totalWeight = filteredTrips.sumOf { it.weightTons }
    val totalVolume = filteredTrips.mapNotNull { it.volumeM3 }.sum()
    val totalDistance = filteredTrips.sumOf { it.distanceKm }
    val totalTonKm = filteredTrips.sumOf { it.tonKilometers }
    val totalRevenue = filteredTrips.sumOf { it.totalPrice }

    // Fuel & Expense calculations with reliable effective fuel calculation
    val totalFuelExpenses = filteredTrips.sumOf { it.effectiveFuelExpenses }
    val totalFuelLiters = filteredTrips.sumOf { it.effectiveFuelLiters }
    val totalOtherExpenses = filteredTrips.sumOf { it.otherExpenses }
    val totalDriverSalary = filteredTrips.sumOf { it.effectiveDriverSalary }
    val totalExpenses = totalFuelExpenses + totalOtherExpenses + totalDriverSalary
    val totalProfit = totalRevenue - totalExpenses

    val fuelShareOfRevenue = if (totalRevenue > 0) (totalFuelExpenses / totalRevenue * 100.0) else 0.0
    val salaryShareOfRevenue = if (totalRevenue > 0) (totalDriverSalary / totalRevenue * 100.0) else 0.0
    val avgFuelRatePer100Km = if (totalDistance > 0) (totalFuelLiters / totalDistance) * 100.0 else 0.0

    val completedTripsCount = filteredTrips.count { it.status == TripStatus.UNLOADED || it.status == TripStatus.PAID }
    val paidTripsCount = filteredTrips.count { it.status == TripStatus.PAID }

    val avgRatePerTon = if (totalWeight > 0) totalRevenue / totalWeight else 0.0
    val avgRatePerKm = if (totalDistance > 0) totalRevenue / totalDistance else 0.0

    // Driver payroll breakdown
    val driverGroups = remember(filteredTrips) {
        filteredTrips.groupBy { it.driverName.ifBlank { "Без указания водителя" } }
            .map { (driverName, trips) ->
                val freight = trips.sumOf { it.totalPrice }
                val salary = trips.sumOf { it.effectiveDriverSalary }
                val weight = trips.sumOf { it.weightTons }
                val avgPercent = if (freight > 0) (salary / freight) * 100.0 else 20.0
                DriverPayrollStat(
                    driverName = driverName,
                    tripsCount = trips.size,
                    totalWeightTons = weight,
                    totalFreight = freight,
                    avgPercent = avgPercent,
                    totalSalary = salary
                )
            }
            .sortedByDescending { it.totalSalary }
    }

    // Crop breakdown
    val cropGroups = remember(filteredTrips) {
        filteredTrips.groupBy { it.cargoType }
            .map { (crop, trips) ->
                val weight = trips.sumOf { it.weightTons }
                val rev = trips.sumOf { it.totalPrice }
                CropStat(
                    cropName = crop.ifBlank { "Прочее" },
                    tripsCount = trips.size,
                    weightTons = weight,
                    revenue = rev,
                    sharePercent = if (totalWeight > 0) (weight / totalWeight * 100).toFloat() else 0f
                )
            }
            .sortedByDescending { it.weightTons }
    }

    val driverReportItems = remember(driverGroups) {
        driverGroups.map { dg ->
            DriverPayrollItem(
                driverName = dg.driverName,
                tripsCount = dg.tripsCount,
                totalWeightTons = dg.totalWeightTons,
                totalFreight = dg.totalFreight,
                avgPercent = dg.avgPercent,
                totalSalary = dg.totalSalary
            )
        }
    }

    val cropReportItems = remember(cropGroups) {
        cropGroups.map { cg ->
            CropReportItem(
                cropName = cg.cropName,
                tripsCount = cg.tripsCount,
                weightTons = cg.weightTons,
                revenue = cg.revenue,
                sharePercent = cg.sharePercent
            )
        }
    }

    val currentPeriodTitle = remember(selectedPeriod, customStartDate, customEndDate, selectedDriver, selectedTruck) {
        val basePeriod = when (selectedPeriod) {
            ReportPeriod.ALL_TIME -> "За все время"
            ReportPeriod.TODAY -> "Сегодня (${Formatters.formatDate(now)})"
            ReportPeriod.WEEK -> "Последние 7 дней"
            ReportPeriod.MONTH -> "Текущий месяц"
            ReportPeriod.QUARTER -> "Квартал"
            ReportPeriod.YEAR -> "Год"
            ReportPeriod.CUSTOM -> "${Formatters.formatDate(customStartDate)} — ${Formatters.formatDate(customEndDate)}"
        }
        buildString {
            append(basePeriod)
            if (selectedDriver != null) append(" | Водитель: $selectedDriver")
            if (selectedTruck != null) append(" | ТС: $selectedTruck")
        }
    }

    // Formatted report string for export/sharing based on selected report type
    val reportText = remember(filteredTrips, selectedPeriod, selectedReportType, currentPeriodTitle, selectedDriver, selectedTruck) {
        val periodName = currentPeriodTitle

        buildString {
            appendLine("═════════════════════════════════════")
            appendLine("   ${selectedReportType.docHeader}")
            appendLine("   Период: $periodName")
            if (selectedDriver != null) appendLine("   Водитель: $selectedDriver")
            if (selectedTruck != null) appendLine("   Транспортное средство (ТС): $selectedTruck")
            appendLine("   Дата формирования: ${Formatters.formatDateTime(now)}")
            appendLine("═════════════════════════════════════")
            appendLine()

            when (selectedReportType) {
                ReportType.FINANCIAL -> {
                    appendLine("💰 ФИНАНСОВЫЕ ИТОГИ:")
                    appendLine("• Суммарный доход (выручка): ${Formatters.formatMoney(totalRevenue)}")
                    appendLine("• Расходы на топливо (ДТ): -${Formatters.formatMoney(totalFuelExpenses)}")
                    appendLine("• Зарплата водителей (ФОТ): -${Formatters.formatMoney(totalDriverSalary)}")
                    if (totalOtherExpenses > 0) {
                        appendLine("• Прочие расходы: -${Formatters.formatMoney(totalOtherExpenses)}")
                    }
                    appendLine("• Суммарные затраты: -${Formatters.formatMoney(totalExpenses)}")
                    appendLine("-------------------------------------")
                    appendLine("• ЧИСТАЯ ПРИБЫЛЬ: ${Formatters.formatMoney(totalProfit)}")
                    if (totalRevenue > 0) {
                        val margin = (totalProfit / totalRevenue) * 100.0
                        appendLine("• Рентабельность (маржа): ${String.format(Locale.US, "%.1f", margin)}%")
                    }
                    appendLine()
                    appendLine("📊 ОПЕРАЦИОННЫЕ ПОКАЗАТЕЛИ:")
                    appendLine("• Всего рейсов: $tripCount (Выгружено: $completedTripsCount, Оплачено: $paidTripsCount)")
                    appendLine("• Общий перевезенный объем: ${Formatters.formatWeight(totalWeight)}")
                    appendLine("• Пробег автопарка: ${Formatters.formatDistance(totalDistance)}")
                    appendLine("• Средняя ставка за тонну: ${Formatters.formatMoney(avgRatePerTon)}/т")
                    appendLine("• Средняя ставка за км: ${Formatters.formatMoney(avgRatePerKm)}/км")
                }
                ReportType.PAYROLL -> {
                    appendLine("👨‍✈️ ВЕДОМОСТЬ НАЧИСЛЕНИЯ ЗАРПЛАТЫ ВОДИТЕЛЕЙ:")
                    appendLine("• Общий фонд оплаты труда (ФОТ): ${Formatters.formatMoney(totalDriverSalary)}")
                    if (totalRevenue > 0) {
                        appendLine("• Доля зарплат в выручке: ${String.format(Locale.US, "%.1f", salaryShareOfRevenue)}%")
                    }
                    appendLine()
                    for (dg in driverGroups) {
                        appendLine("• Водитель: ${dg.driverName}")
                        appendLine("  Выполнено рейсов: ${dg.tripsCount} | Объем: ${Formatters.formatWeight(dg.totalWeightTons)}")
                        appendLine("  Сумма фрахта: ${Formatters.formatMoney(dg.totalFreight)}")
                        appendLine("  Ставка водителя: ${String.format(Locale.US, "%.1f", dg.avgPercent)}%")
                        appendLine("  К ВЫПЛАТЕ ЗП: ${Formatters.formatMoney(dg.totalSalary)}")
                        appendLine()
                    }
                }
                ReportType.CARGO -> {
                    appendLine("🌾 СТРУКТУРА ПЕРЕВОЗКИ ЗЕРНОВЫХ КУЛЬТУР:")
                    appendLine("• Общий вес: ${Formatters.formatWeight(totalWeight)}")
                    appendLine("• Общая выручка: ${Formatters.formatMoney(totalRevenue)}")
                    appendLine()
                    for (cg in cropGroups) {
                        appendLine("• ${cg.cropName}: ${Formatters.formatWeight(cg.weightTons)} (${String.format(Locale.US, "%.1f", cg.sharePercent)}%) | Рейсов: ${cg.tripsCount} | Выручка: ${Formatters.formatMoney(cg.revenue)}")
                    }
                }
                ReportType.FUEL_LOGISTICS -> {
                    appendLine("⛽️ ОТЧЕТ ПО РАСХОДУ ТОПЛИВА И ЛОГИСТИКЕ:")
                    appendLine("• Общий пробег: ${Formatters.formatDistance(totalDistance)}")
                    appendLine("• Транспортная работа: ${Formatters.formatTonKm(totalTonKm)}")
                    appendLine("• Израсходовано ДТ: ${Formatters.formatFuelLiters(totalFuelLiters)}")
                    appendLine("• Затраты на топливо: ${Formatters.formatMoney(totalFuelExpenses)}")
                    if (totalDistance > 0) {
                        appendLine("• Средний расход: ${String.format(Locale.US, "%.1f", avgFuelRatePer100Km)} л / 100 км")
                    }
                    if (totalRevenue > 0) {
                        appendLine("• Доля топлива в выручке: ${String.format(Locale.US, "%.1f", fuelShareOfRevenue)}%")
                    }
                }
                ReportType.REGISTRY -> {
                    appendLine("📝 СВОДНЫЙ РЕЕСТР ВЫПОЛНЕННЫХ РЕЙСОВ:")
                    filteredTrips.forEachIndexed { index, t ->
                        appendLine("${index + 1}. №${t.tripNumber} | ${Formatters.formatDate(t.loadingDate)} | ТТН: ${t.ttnNumber.ifBlank { "б/н" }}")
                        appendLine("   Культура: ${t.cargoType} | Водитель: ${t.driverName.ifBlank { "—" }} | ТС: ${t.truckPlate.ifBlank { "—" }}")
                        appendLine("   Маршрут: ${t.loadingLocation} ➔ ${t.unloadingLocation}")
                        appendLine("   Вес: ${Formatters.formatWeight(t.weightTons)} | Дистанция: ${Formatters.formatDistance(t.distanceKm)}")
                        appendLine("   Фрахт: ${Formatters.formatMoney(t.totalPrice)} | Статус: ${t.status.label}")
                        if (t.effectiveDriverSalary > 0) {
                            appendLine("   ЗП водителя: ${Formatters.formatMoney(t.effectiveDriverSalary)} | Топливо: ${Formatters.formatMoney(t.effectiveFuelExpenses)} | Чистая: ${Formatters.formatMoney(t.netProfit)}")
                        }
                        appendLine()
                    }
                }
            }
            appendLine("═════════════════════════════════════")
            appendLine("Отчет составил: диспетчер / Иванов А.В. /")
            appendLine("Утверждаю: руководитель ООО «Агротранс-Сервис» / М.П. /")
        }
    }

    val shareReport = {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, reportText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Отправить отчет по рейсам"))
    }

    val copyReport = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Отчет по рейсам зерновоза", reportText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Отчет скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
    }

    val showCustomStartDatePicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = customStartDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                }
                customStartDate = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val showCustomEndDatePicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = customEndDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }
                customEndDate = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val onExportPdf: () -> Unit = {
        ReportExportHelper.exportRegistryToPdf(
            context = context,
            truckPlate = selectedTruck ?: "",
            driverName = selectedDriver ?: "",
            periodTitle = currentPeriodTitle,
            trips = filteredTrips
        )
    }

    val onExportExcel: () -> Unit = {
        ReportExportHelper.exportRegistryToExcel(
            context = context,
            truckPlate = selectedTruck ?: "",
            driverName = selectedDriver ?: "",
            periodTitle = currentPeriodTitle,
            trips = filteredTrips
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Генерация отчетов",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Сводка, реестр рейсов, экспорт в PDF и Excel",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("reports_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Экспорт в PDF",
                            tint = Color(0xFFC62828)
                        )
                    }
                    IconButton(
                        onClick = onExportExcel,
                        modifier = Modifier.testTag("export_excel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Выгрузка в Excel",
                            tint = Color(0xFF1E7E34)
                        )
                    }
                    IconButton(
                        onClick = copyReport,
                        modifier = Modifier.testTag("copy_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Скопировать отчет"
                        )
                    }
                    IconButton(
                        onClick = shareReport,
                        modifier = Modifier.testTag("share_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поделиться отчетом"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Primary Report Actions Card (ЕДИНСТВЕННЫЙ блок кнопок формирования отчетов сверху страницы)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("top_report_actions_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Сформировать и выгрузить отчет:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onExportPdf,
                            modifier = Modifier
                                .weight(1.1f)
                                .height(44.dp)
                                .testTag("top_export_pdf_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC62828),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onExportExcel,
                            modifier = Modifier
                                .weight(1.1f)
                                .height(44.dp)
                                .testTag("top_export_excel_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E7E34),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = copyReport,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Копия", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = shareReport,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("top_share_report_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Поделиться", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Unified Filters & Settings Card under spoiler
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reports_filters_spoiler_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Clickable Header to toggle spoiler
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isFiltersSpoilerExpanded = !isFiltersSpoilerExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Фильтры и параметры отчета",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    val activeFiltersCount = (if (selectedPeriod != ReportPeriod.ALL_TIME) 1 else 0) +
                                            (if (selectedDriver != null) 1 else 0) +
                                            (if (selectedTruck != null) 1 else 0)
                                    if (activeFiltersCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$activeFiltersCount",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = buildString {
                                        append(selectedReportType.title)
                                        append(" • ")
                                        append(selectedPeriod.title)
                                        selectedDriver?.let { append(" • $it") }
                                        selectedTruck?.let { append(" • $it") }
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedPeriod != ReportPeriod.ALL_TIME || selectedDriver != null || selectedTruck != null) {
                                TextButton(
                                    onClick = {
                                        selectedPeriod = ReportPeriod.ALL_TIME
                                        selectedDriver = null
                                        selectedTruck = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Сбросить", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                }
                            }
                            IconButton(
                                onClick = { isFiltersSpoilerExpanded = !isFiltersSpoilerExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFiltersSpoilerExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isFiltersSpoilerExpanded) "Свернуть" else "Развернуть",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Collapsible contents
                    AnimatedVisibility(visible = isFiltersSpoilerExpanded) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            // 1. Вариант отчета
                            Column {
                                Text(
                                    text = "1. Вариант отчета:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ReportType.values().forEach { type ->
                                        FilterChip(
                                            selected = selectedReportType == type,
                                            onClick = { selectedReportType = type },
                                            label = { Text(type.title, fontSize = 12.sp, fontWeight = if (selectedReportType == type) FontWeight.Bold else FontWeight.Normal) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = selectedReportType.subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            // 2. Период отчета
                            Column {
                                Text(
                                    text = "2. Период отчета:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ReportPeriod.values().forEach { period ->
                                        FilterChip(
                                            selected = selectedPeriod == period,
                                            onClick = { selectedPeriod = period },
                                            label = { Text(period.title, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }

                                if (selectedPeriod == ReportPeriod.CUSTOM) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = showCustomStartDatePicker,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("С: ${Formatters.formatDate(customStartDate)}", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = showCustomEndDatePicker,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("По: ${Formatters.formatDate(customEndDate)}", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // 3. Водитель
                            Column {
                                Text(
                                    text = "3. Водитель:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedDriver == null,
                                        onClick = { selectedDriver = null },
                                        label = { Text("Все водители (${periodTrips.size})", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                    availableDrivers.forEach { driver ->
                                        val driverCount = periodTrips.count { it.driverName.equals(driver, ignoreCase = true) }
                                        FilterChip(
                                            selected = selectedDriver == driver,
                                            onClick = { selectedDriver = if (selectedDriver == driver) null else driver },
                                            label = { Text("$driver ($driverCount)", fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        )
                                    }
                                }
                            }

                            // 4. ТС (Транспортное средство)
                            Column {
                                Text(
                                    text = "4. Транспортное средство (ТС):",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedTruck == null,
                                        onClick = { selectedTruck = null },
                                        label = { Text("Все ТС (${periodTrips.size})", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                    availableTrucks.forEach { truck ->
                                        val truckCount = periodTrips.count { it.truckPlate.equals(truck, ignoreCase = true) }
                                        FilterChip(
                                            selected = selectedTruck == truck,
                                            onClick = { selectedTruck = if (selectedTruck == truck) null else truck },
                                            label = { Text("$truck ($truckCount)", fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        )
                                    }
                                }
                            }

                            // 5. Отображение на экране
                            Column {
                                Text(
                                    text = "5. Режим отображения на экране:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ReportDisplayMode.values().forEach { mode ->
                                        FilterChip(
                                            selected = displayMode == mode,
                                            onClick = { displayMode = mode },
                                            label = { Text(mode.title, fontSize = 12.sp, fontWeight = if (displayMode == mode) FontWeight.Bold else FontWeight.Normal) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Summary Indicator Card (Выборка данных)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Рейсов в выборке: $tripCount",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (selectedDriver != null || selectedTruck != null || selectedPeriod != ReportPeriod.ALL_TIME) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("Фильтр активен", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = "Объем: ${Formatters.formatWeight(totalWeight)} • Выручка: ${Formatters.formatMoney(totalRevenue)} • Прибыль: ${Formatters.formatMoney(totalProfit)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedDriver != null || selectedTruck != null || selectedPeriod != ReportPeriod.ALL_TIME) {
                        TextButton(
                            onClick = {
                                selectedDriver = null
                                selectedTruck = null
                                selectedPeriod = ReportPeriod.ALL_TIME
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Сбросить все", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Document Preview Card (Печатная форма)
            if (displayMode == ReportDisplayMode.ALL || displayMode == ReportDisplayMode.DOCUMENT) {
                ReportDocumentPreviewCard(
                    reportType = selectedReportType,
                    periodTitle = currentPeriodTitle,
                    generatedDateTime = Formatters.formatDateTime(now),
                    trips = filteredTrips,
                    totalRevenue = totalRevenue,
                    totalFuelExpenses = totalFuelExpenses,
                    totalFuelLiters = totalFuelLiters,
                    totalDriverSalary = totalDriverSalary,
                    totalOtherExpenses = totalOtherExpenses,
                    totalExpenses = totalExpenses,
                    totalProfit = totalProfit,
                    totalWeight = totalWeight,
                    totalDistance = totalDistance,
                    totalTonKm = totalTonKm,
                    driverItems = driverReportItems,
                    cropItems = cropReportItems,
                    onCopy = copyReport,
                    onShare = shareReport,
                    onExportPdf = onExportPdf,
                    onExportExcel = onExportExcel,
                    truckPlate = selectedTruck,
                    driverName = selectedDriver
                )
            }

            // 6. Interactive analytical cards
            if (displayMode == ReportDisplayMode.ALL || displayMode == ReportDisplayMode.CARDS) {
                // Interactive analytical cards
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Суммарный доход за период",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = Formatters.formatMoney(totalRevenue),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Все расходы:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "-${Formatters.formatMoney(totalExpenses)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = ExpenseRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Чистая прибыль:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = Formatters.formatMoney(totalProfit),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (totalProfit >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            // DEDICATED FUEL REPORT CARD (НОВОЕ ТРЕБОВАНИЕ!)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalGasStation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Расходы на топливо (ДТ)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Затраты дизельного топлива за период",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = Formatters.formatMoney(totalFuelExpenses),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportMetricTile(
                            title = "Объем топлива",
                            value = Formatters.formatFuelLiters(totalFuelLiters),
                            subValue = "израсходовано ДТ",
                            icon = Icons.Default.LocalGasStation,
                            modifier = Modifier.weight(1f)
                        )

                        ReportMetricTile(
                            title = "Средний расход",
                            value = "${String.format(Locale.US, "%.1f", avgFuelRatePer100Km)} л",
                            subValue = "на 100 км пробега",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (totalRevenue > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Доля топлива в выручке:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", fuelShareOfRevenue)}%",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (fuelShareOfRevenue / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    if (totalOtherExpenses > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Прочие расходы (Платон, весовая):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatMoney(totalOtherExpenses),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // DEDICATED DRIVER PAYROLL REPORT CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ФОТ и Зарплата водителей",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Начисления в % от ставки фрахта",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", salaryShareOfRevenue)}% выручки",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Всего начислено ЗП:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Formatters.formatMoney(totalDriverSalary),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Водителей в рейсах:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${driverGroups.size}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (driverGroups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Начисления по водителям:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            driverGroups.forEach { dg ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = dg.driverName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${dg.tripsCount} рейсов • Фрахт: ${Formatters.formatMoney(dg.totalFreight)}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = Formatters.formatMoney(dg.totalSalary),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            )
                                            Text(
                                                text = "Ставка ~${String.format(Locale.US, "%.0f", dg.avgPercent)}%",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4 Grid Metric Cards (Trips count, Total Volume, Total Distance, Ton-Kilometers)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportMetricTile(
                    title = "Количество рейсов",
                    value = "$tripCount",
                    subValue = "$completedTripsCount завершено",
                    icon = Icons.Default.LocalShipping,
                    modifier = Modifier.weight(1f)
                )

                ReportMetricTile(
                    title = "Перевезено груза",
                    value = Formatters.formatWeight(totalWeight),
                    subValue = if (totalVolume > 0) "$totalVolume м³" else "общий вес",
                    icon = Icons.Default.Scale,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportMetricTile(
                    title = "Пройденное расстояние",
                    value = Formatters.formatDistance(totalDistance),
                    subValue = "общий пробег",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )

                ReportMetricTile(
                    title = "Транспортная работа",
                    value = Formatters.formatTonKm(totalTonKm),
                    subValue = "тонно-километры",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }

            // Rates Breakdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Эффективность перевозок",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Средняя ставка за тонну:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${Formatters.formatMoney(avgRatePerTon)} / т",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Средняя ставка за километр:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${Formatters.formatMoney(avgRatePerKm)} / км",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Breakdown by Grain Crop
            if (cropGroups.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Распределение по культурам",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        cropGroups.forEach { cropStat ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${cropStat.cropName} (${cropStat.tripsCount} р.)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${Formatters.formatWeight(cropStat.weightTons)} • ${Formatters.formatMoney(cropStat.revenue)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (cropStat.sharePercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // List of trips for this period (Summary table)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Включенные рейсы ($tripCount)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredTrips.isEmpty()) {
                        Text(
                            text = "По заданным фильтрам рейсы не найдены.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        filteredTrips.forEachIndexed { idx, trip ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${trip.tripNumber} • ${trip.cargoType}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${Formatters.formatShortDate(trip.loadingDate)} | ${trip.loadingLocation} ➔ ${trip.unloadingLocation}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        if (trip.driverName.isNotBlank() || trip.truckPlate.isNotBlank()) {
                                            Text(
                                                text = "${trip.driverName.ifBlank { "—" }} • ${trip.truckPlate.ifBlank { "—" }}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        if (trip.effectiveFuelExpenses > 0) {
                                            Text(
                                                text = "ДТ: ${Formatters.formatFuelLiters(trip.effectiveFuelLiters)} (${Formatters.formatMoney(trip.effectiveFuelExpenses)})",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                color = ExpenseRed
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Formatters.formatMoney(trip.totalPrice),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${Formatters.formatWeight(trip.weightTons)} • ${Formatters.formatDistance(trip.distanceKm)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (idx < filteredTrips.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

private data class DriverPayrollStat(
    val driverName: String,
    val tripsCount: Int,
    val totalWeightTons: Double,
    val totalFreight: Double,
    val avgPercent: Double,
    val totalSalary: Double
)

private data class CropStat(
    val cropName: String,
    val tripsCount: Int,
    val weightTons: Double,
    val revenue: Double,
    val sharePercent: Float
)

@Composable
private fun ReportMetricTile(
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
