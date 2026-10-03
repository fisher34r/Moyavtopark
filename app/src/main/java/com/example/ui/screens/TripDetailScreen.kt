package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.RateType
import com.example.data.model.Trip
import com.example.data.model.TripStatus
import com.example.ui.components.TripStatusBadge
import com.example.ui.components.toColor
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.TripViewModel
import java.util.Locale
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: Trip,
    viewModel: TripViewModel,
    onBack: () -> Unit,
    onEdit: (Trip) -> Unit,
    onNavigateToReports: ((truckPlate: String?, driverName: String?) -> Unit)? = null,
    onFilterByVehicle: ((truckPlate: String) -> Unit)? = null,
    onFilterByDriver: ((driverName: String) -> Unit)? = null,
    onNavigateToFleetVehicle: ((truckPlate: String) -> Unit)? = null,
    onNavigateToFleetDriver: ((driverName: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun shareTripDetails() {
        val shareText = buildString {
            appendLine("📋 ДЕТАЛИ РЕЙСА ЗЕРНОВОЗА: ${trip.tripNumber}")
            appendLine("Культура: ${trip.cargoType}")
            appendLine("Статус: ${trip.status.label}")
            appendLine("─────────────────────")
            appendLine("📍 ПОГРУЗКА:")
            appendLine("  Место: ${trip.loadingLocation}")
            appendLine("  Время: ${Formatters.formatDateTime(trip.loadingDate)}")
            appendLine("🏁 ВЫГРУЗКА:")
            appendLine("  Место: ${trip.unloadingLocation}")
            if (trip.unloadingDate != null && trip.unloadingDate > 0) {
                appendLine("  Время: ${Formatters.formatDateTime(trip.unloadingDate)}")
            }
            appendLine("─────────────────────")
            appendLine("⚖️ ПАРАМЕТРЫ ГРУЗА И ПУТИ:")
            appendLine("  Вес: ${Formatters.formatWeight(trip.weightTons)}")
            if (trip.volumeM3 != null && trip.volumeM3 > 0) {
                appendLine("  Объем: ${trip.volumeM3} м³")
            }
            appendLine("  Расстояние: ${Formatters.formatDistance(trip.distanceKm)}")
            appendLine("  Транспортная работа: ${Formatters.formatTonKm(trip.tonKilometers)}")
            appendLine("─────────────────────")
            appendLine("⛽️ РАСХОД ТОПЛИВА (ДТ):")
            appendLine("  Израсходовано: ${Formatters.formatFuelLiters(trip.effectiveFuelLiters)}")
            if (trip.fuelConsumptionRate != null && trip.fuelConsumptionRate > 0) {
                appendLine("  Норма: ${Formatters.formatFuelRate(trip.fuelConsumptionRate)}")
            }
            if (trip.fuelPricePerLiter != null && trip.fuelPricePerLiter > 0) {
                appendLine("  Цена ДТ: ${Formatters.formatFuelPrice(trip.fuelPricePerLiter)}")
            }
            appendLine("  Стоимость топлива: ${Formatters.formatMoney(trip.fuelExpenses)}")
            if (trip.effectiveDriverSalary > 0 || trip.driverSalaryPercent > 0) {
                appendLine("  Зарплата водителя (${String.format(Locale.US, "%.1f", trip.driverSalaryPercent)}%): ${Formatters.formatMoney(trip.effectiveDriverSalary)}")
            }
            if (trip.otherExpenses > 0) {
                appendLine("  Прочие расходы: ${Formatters.formatMoney(trip.otherExpenses)}")
            }
            appendLine("─────────────────────")
            appendLine("💰 СТОИМОСТЬ И ФИНАНСЫ:")
            appendLine("  Тариф: ${trip.rateValue} ${trip.rateType.unitLabel} (${trip.rateType.title})")
            appendLine("  ИТОГО ДОХОД (фрахт): ${Formatters.formatMoney(trip.totalPrice)}")
            if (trip.totalExpenses > 0) {
                appendLine("  Все расходы (ГСМ + ЗП + прочее): ${Formatters.formatMoney(trip.totalExpenses)}")
                appendLine("  Чистая прибыль перевозчика: ${Formatters.formatMoney(trip.netProfit)}")
            }
            if (trip.ttnNumber.isNotBlank()) appendLine("📄 ТТН: ${trip.ttnNumber}")
            if (trip.customerName.isNotBlank()) appendLine("🏢 Заказчик: ${trip.customerName}")
            if (trip.driverName.isNotBlank()) appendLine("👤 Водитель: ${trip.driverName}")
            if (trip.truckPlate.isNotBlank()) appendLine("🚛 Зерновоз: ${trip.truckPlate}")
            if (trip.notes.isNotBlank()) appendLine("📝 Примечания: ${trip.notes}")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Поделиться рейсом"))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = trip.tripNumber.ifBlank { "Детали рейса" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    if (onNavigateToReports != null) {
                        IconButton(
                            onClick = {
                                onNavigateToReports(
                                    trip.truckPlate.takeIf { it.isNotBlank() },
                                    trip.driverName.takeIf { it.isNotBlank() }
                                )
                            },
                            modifier = Modifier.testTag("detail_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Реестр / отчет по рейсу",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = { shareTripDetails() },
                        modifier = Modifier.testTag("detail_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поделиться"
                        )
                    }
                    IconButton(
                        onClick = { onEdit(trip) },
                        modifier = Modifier.testTag("detail_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать"
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("detail_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = ExpenseRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Quick Status Actions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (trip.status != TripStatus.UNLOADED && trip.status != TripStatus.PAID) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.updateTripStatus(trip, TripStatus.UNLOADED)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Выгружен", fontSize = 13.sp)
                        }
                    }

                    if (trip.status != TripStatus.PAID) {
                        Button(
                            onClick = {
                                viewModel.updateTripStatus(trip, TripStatus.PAID)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Оплачен", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
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
                        Column {
                            Text(
                                text = trip.tripNumber,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = trip.cargoType,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        TripStatusBadge(
                            status = trip.status,
                            onClick = {}
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Price Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Рассчитанная стоимость перевозки",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatMoney(trip.totalPrice),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when (trip.rateType) {
                                    RateType.PER_TON_KM -> "${Formatters.formatWeight(trip.weightTons)} × ${Formatters.formatDistance(trip.distanceKm)} × ${Formatters.formatMoneyExact(trip.rateValue)}/т·км"
                                    RateType.PER_TON -> "${Formatters.formatWeight(trip.weightTons)} × ${Formatters.formatMoneyExact(trip.rateValue)}/т"
                                    RateType.PER_KM -> "${Formatters.formatDistance(trip.distanceKm)} × ${Formatters.formatMoneyExact(trip.rateValue)}/км"
                                    RateType.FIXED -> "Фиксированная ставка"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (trip.totalExpenses > 0 || trip.effectiveDriverSalary > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (trip.fuelExpenses > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Топливо (ГСМ):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "-${Formatters.formatMoney(trip.fuelExpenses)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ExpenseRed
                                )
                            }
                        }
                        if (trip.effectiveDriverSalary > 0) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Зарплата водителя (${String.format(Locale.US, "%.1f", trip.driverSalaryPercent)}%):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "-${Formatters.formatMoney(trip.effectiveDriverSalary)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ExpenseRed
                                )
                            }
                        }
                        if (trip.otherExpenses > 0) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Прочие расходы:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "-${Formatters.formatMoney(trip.otherExpenses)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ExpenseRed
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Все расходы:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "-${Formatters.formatMoney(trip.totalExpenses)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = ExpenseRed
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Чистая прибыль с рейса:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = Formatters.formatMoney(trip.netProfit),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (trip.netProfit >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            // Route Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Детали маршрута",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Loading point
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ПОГРУЗКА",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = trip.loadingLocation.ifBlank { "Не указано" },
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = Formatters.formatDateTime(trip.loadingDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Path indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.width(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(30.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Расстояние пути: ${Formatters.formatDistance(trip.distanceKm)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Unloading point
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ВЫГРУЗКА",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = trip.unloadingLocation.ifBlank { "Не указано" },
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (trip.unloadingDate != null && trip.unloadingDate > 0) {
                                    Formatters.formatDateTime(trip.unloadingDate)
                                } else {
                                    "Ожидается выгрузка"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // DEDICATED FUEL DETAILS CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Расход и стоимость топлива (ДТ)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    DetailItemRow(
                        icon = Icons.Default.LocalGasStation,
                        title = "Затрачено топлива",
                        value = Formatters.formatFuelLiters(trip.effectiveFuelLiters)
                    )

                    DetailItemRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Стоимость топлива",
                        value = Formatters.formatMoney(trip.fuelExpenses)
                    )

                    if (trip.fuelConsumptionRate != null && trip.fuelConsumptionRate > 0) {
                        DetailItemRow(
                            icon = Icons.Default.Speed,
                            title = "Норма расхода",
                            value = Formatters.formatFuelRate(trip.fuelConsumptionRate)
                        )
                    }

                    if (trip.fuelPricePerLiter != null && trip.fuelPricePerLiter > 0) {
                        DetailItemRow(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = "Цена за 1 литр ДТ",
                            value = Formatters.formatFuelPrice(trip.fuelPricePerLiter)
                        )
                    }
                }
            }

            // Cargo & Transportation Metrics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Параметры груза и перевозки",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    DetailItemRow(
                        icon = Icons.Default.Scale,
                        title = "Вес груза (нетто)",
                        value = Formatters.formatWeight(trip.weightTons)
                    )

                    if (trip.volumeM3 != null && trip.volumeM3 > 0) {
                        DetailItemRow(
                            icon = Icons.Default.Scale,
                            title = "Объем груза",
                            value = "${trip.volumeM3} м³"
                        )
                    }

                    DetailItemRow(
                        icon = Icons.Default.Speed,
                        title = "Расстояние пути",
                        value = Formatters.formatDistance(trip.distanceKm)
                    )

                    DetailItemRow(
                        icon = Icons.Default.LocalShipping,
                        title = "Транспортная работа",
                        value = Formatters.formatTonKm(trip.tonKilometers)
                    )

                    DetailItemRow(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "Тариф",
                        value = "${trip.rateValue} ${trip.rateType.unitLabel} (${trip.rateType.title})"
                    )

                    DetailItemRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Эффективная ставка за тонну",
                        value = "${Formatters.formatMoney(trip.effectiveRatePerTon)} / т"
                    )
                }
            }

            // Additional details (TTN, Customer, Driver, Truck, Notes)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Документы и автомобиль",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (trip.ttnNumber.isNotBlank()) {
                        DetailItemRow(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = "Номер ТТН",
                            value = trip.ttnNumber
                        )
                    }

                    if (trip.customerName.isNotBlank()) {
                        DetailItemRow(
                            icon = Icons.Default.Person,
                            title = "Заказчик / Контрагент",
                            value = trip.customerName
                        )
                    }

                    if (trip.driverName.isNotBlank()) {
                        DetailItemRow(
                            icon = Icons.Default.Person,
                            title = "Водитель",
                            value = trip.driverName
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (onFilterByDriver != null) {
                                OutlinedButton(
                                    onClick = { onFilterByDriver(trip.driverName) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Все рейсы", fontSize = 11.sp)
                                }
                            }
                            if (onNavigateToReports != null) {
                                OutlinedButton(
                                    onClick = { onNavigateToReports(null, trip.driverName) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Ведомость ЗП", fontSize = 11.sp)
                                }
                            }
                            if (onNavigateToFleetDriver != null) {
                                OutlinedButton(
                                    onClick = { onNavigateToFleetDriver(trip.driverName) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("В Автопарк", fontSize = 11.sp)
                                }
                            }
                        }
                        DetailItemRow(
                            icon = Icons.Default.Payments,
                            title = "Ставка зарплаты водителя",
                            value = "${String.format(Locale.US, "%.1f", trip.driverSalaryPercent)}% от фрахта"
                        )
                        DetailItemRow(
                            icon = Icons.Default.Payments,
                            title = "Начислено водителю за рейс",
                            value = Formatters.formatMoney(trip.effectiveDriverSalary)
                        )
                    }

                    if (trip.truckPlate.isNotBlank()) {
                        DetailItemRow(
                            icon = Icons.Default.LocalShipping,
                            title = "Госномер тягача",
                            value = trip.truckPlate
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (onFilterByVehicle != null) {
                                OutlinedButton(
                                    onClick = { onFilterByVehicle(trip.truckPlate) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Все рейсы ТС", fontSize = 11.sp)
                                }
                            }
                            if (onNavigateToReports != null) {
                                OutlinedButton(
                                    onClick = { onNavigateToReports(trip.truckPlate, null) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Реестр по ТС", fontSize = 11.sp)
                                }
                            }
                            if (onNavigateToFleetVehicle != null) {
                                OutlinedButton(
                                    onClick = { onNavigateToFleetVehicle(trip.truckPlate) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("В Автопарк", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (trip.otherExpenses > 0) {
                        DetailItemRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Прочие расходы (Платон, весы)",
                            value = Formatters.formatMoney(trip.otherExpenses)
                        )
                    }

                    if (trip.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Примечания к рейсу:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = trip.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (onNavigateToReports != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onNavigateToReports(
                                trip.truckPlate.takeIf { it.isNotBlank() },
                                trip.driverName.takeIf { it.isNotBlank() }
                            )
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Сформировать реестр и отчет",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Печатная форма, выгрузка в Excel (.xls) и PDF",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Удалить рейс?") },
            text = { Text("Вы уверены, что хотите удалить ${trip.tripNumber}? Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteTrip(trip)
                        onBack()
                    }
                ) {
                    Text("Удалить", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun DetailItemRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
