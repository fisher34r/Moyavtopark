package com.example.ui.screens.fleet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FleetViewModel
import com.example.util.Formatters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetAnalyticsTab(
    viewModel: FleetViewModel,
    onNavigateToReports: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.analyticsSummary.collectAsStateWithLifecycle()
    val drivers by viewModel.drivers.collectAsStateWithLifecycle()
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val selectedPlate by viewModel.analyticsVehiclePlate.collectAsStateWithLifecycle()
    val selectedDriver by viewModel.analyticsDriverName.collectAsStateWithLifecycle()

    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
    var driverDropdownExpanded by remember { mutableStateOf(false) }
    var dateDropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val now = remember { System.currentTimeMillis() }
    var customStartDate by remember { mutableStateOf(now - 30L * 24 * 60 * 60 * 1000) }
    var customEndDate by remember { mutableStateOf(now) }
    val dateOptions = listOf("За все время", "Этот месяц", "Прошлый месяц", "Произвольный период")
    var selectedDateText by remember { mutableStateOf(dateOptions[0]) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Фильтры
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Фильтр по периоду
            ExposedDropdownMenuBox(
                expanded = dateDropdownExpanded,
                onExpandedChange = { dateDropdownExpanded = !dateDropdownExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedDateText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Период", fontSize = 10.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = dateDropdownExpanded,
                    onDismissRequest = { dateDropdownExpanded = false }
                ) {
                    dateOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                selectedDateText = opt
                                dateDropdownExpanded = false
                                val cal = java.util.Calendar.getInstance()
                                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                cal.set(java.util.Calendar.MINUTE, 0)
                                cal.set(java.util.Calendar.SECOND, 0)
                                cal.set(java.util.Calendar.MILLISECOND, 0)
                                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)

                                when (opt) {
                                    "Этот месяц" -> {
                                        viewModel.analyticsDateStart.value = cal.timeInMillis
                                        cal.add(java.util.Calendar.MONTH, 1)
                                        viewModel.analyticsDateEnd.value = cal.timeInMillis - 1
                                    }
                                    "Прошлый месяц" -> {
                                        cal.add(java.util.Calendar.MONTH, -1)
                                        viewModel.analyticsDateStart.value = cal.timeInMillis
                                        cal.add(java.util.Calendar.MONTH, 1)
                                        viewModel.analyticsDateEnd.value = cal.timeInMillis - 1
                                    }
                                    "Произвольный период" -> {
                                        viewModel.analyticsDateStart.value = customStartDate
                                        viewModel.analyticsDateEnd.value = customEndDate
                                    }
                                    else -> {
                                        viewModel.analyticsDateStart.value = null
                                        viewModel.analyticsDateEnd.value = null
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        if (selectedDateText == "Произвольный период") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = customStartDate }
                        android.app.DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.YEAR, y)
                                    set(java.util.Calendar.MONTH, m)
                                    set(java.util.Calendar.DAY_OF_MONTH, d)
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }
                                customStartDate = newCal.timeInMillis
                                viewModel.analyticsDateStart.value = customStartDate
                            },
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH),
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("С: ${Formatters.formatDate(customStartDate)}", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = customEndDate }
                        android.app.DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.YEAR, y)
                                    set(java.util.Calendar.MONTH, m)
                                    set(java.util.Calendar.DAY_OF_MONTH, d)
                                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                                    set(java.util.Calendar.MINUTE, 59)
                                    set(java.util.Calendar.SECOND, 59)
                                    set(java.util.Calendar.MILLISECOND, 999)
                                }
                                customEndDate = newCal.timeInMillis
                                viewModel.analyticsDateEnd.value = customEndDate
                            },
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH),
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("По: ${Formatters.formatDate(customEndDate)}", fontSize = 12.sp)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Фильтр по автомобилю
            ExposedDropdownMenuBox(
                expanded = vehicleDropdownExpanded,
                onExpandedChange = { vehicleDropdownExpanded = !vehicleDropdownExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedPlate ?: "Все ТС",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Транспорт", fontSize = 10.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = vehicleDropdownExpanded,
                    onDismissRequest = { vehicleDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Все ТС", fontWeight = FontWeight.Bold) },
                        onClick = { viewModel.analyticsVehiclePlate.value = null; vehicleDropdownExpanded = false }
                    )
                    vehicles.forEach { v ->
                        DropdownMenuItem(
                            text = { Text(v.plateNumber) },
                            onClick = { viewModel.analyticsVehiclePlate.value = v.plateNumber; vehicleDropdownExpanded = false }
                        )
                    }
                }
            }

            // Фильтр по водителю
            ExposedDropdownMenuBox(
                expanded = driverDropdownExpanded,
                onExpandedChange = { driverDropdownExpanded = !driverDropdownExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedDriver ?: "Все Водители",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Водитель", fontSize = 10.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = driverDropdownExpanded,
                    onDismissRequest = { driverDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Все Водители", fontWeight = FontWeight.Bold) },
                        onClick = { viewModel.analyticsDriverName.value = null; driverDropdownExpanded = false }
                    )
                    drivers.forEach { d ->
                        DropdownMenuItem(
                            text = { Text(d.fullName) },
                            onClick = { viewModel.analyticsDriverName.value = d.fullName; driverDropdownExpanded = false }
                        )
                    }
                }
            }
        }

        // Hero Card: TCO (Совокупная стоимость владения)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "TCO (Совокупная стоимость)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Полные эксплуатационные затраты парка",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = Formatters.formatMoney(summary.totalTco),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Cost Per Km KPI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Удельная себестоимость 1 км:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.2f ₽ / км", summary.costPerKm),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Breakdown Cards: Топливо, ТО и Сервис, Документы
        Text(
            text = "Структура расходов автопарка",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Fuel KPI
            Card(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 124.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Расход ГСМ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMoney(summary.totalFuelCost),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${summary.totalFuelLiters.toInt()} литров ДТ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Service & Repair KPI
            Card(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 124.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = Color(0xFFED6C02),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Сервис и ТО", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMoney(summary.totalServiceCost),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("ТО-1, ТО-2, ремонты", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Documents & Insurance KPI
            Card(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 124.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Policy,
                        contentDescription = null,
                        tint = Color(0xFF0288D1),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Страховки и ДК", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMoney(summary.totalDocumentsCost),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("ОСАГО, КАСКО, пропуска", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }

            // Mileage KPI
            Card(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 124.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Общий пробег", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatDistance(summary.totalFleetDistanceKm),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Все рейсы и ПЛ", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // Driver Payroll & Profitability Card
        Text(
            text = "ФОТ водителей и экономика перевозок",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Фонд оплаты труда водителей",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Начисления (% от фрахта рейсов)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = Formatters.formatMoney(summary.totalDriverSalary),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1B5E20)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text(
                            text = "Выручка (фрахт):",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Formatters.formatMoney(summary.totalRevenue),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(0.9f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Штат водителей:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${summary.totalDrivers} чел.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1.1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Чистая прибыль:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Formatters.formatMoney(summary.netProfit),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (summary.netProfit >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Fleet Status & Readiness Overview
        Text(
            text = "Готовность и эксплуатация техники",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Всего техники в парке:", fontWeight = FontWeight.SemiBold)
                    }
                    Text("${summary.totalVehicles} ед.", fontWeight = FontWeight.ExtraBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("В рейсе на линии:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("${summary.activeVehiclesOnTrip} ед.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0288D1)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Готовы к рейсу (свободны):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("${summary.availableVehicles} ед.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFED6C02)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("На ТО / Ремонте:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("${summary.vehiclesInService} ед.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFED6C02))
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Водительский состав:", fontWeight = FontWeight.SemiBold)
                    }
                    Text("${summary.totalDrivers} чел.", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Document Expiration Alert Card
        if (summary.expiredDocsCount > 0 || summary.expiringDocsCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Контроль документов и сроков",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "Просрочено: ${summary.expiredDocsCount} • Требуют скорого продления (<30 дней): ${summary.expiringDocsCount}.",
                            fontSize = 11.sp,
                            color = Color(0xFFB78103)
                        )
                    }
                }
            }
        }

        if (onNavigateToReports != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onNavigateToReports,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF155383),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Assessment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Перейти к сводным отчетам и реестрам", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
