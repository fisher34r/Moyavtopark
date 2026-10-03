package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SettingsPreferences
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CostCalculatorScreen(
    settings: SettingsPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State initialized from SettingsPreferences
    var loadedDistText by remember { mutableStateOf(settings.calcLoadedDistanceKm.let { if (it > 0) it.toString() else "280" }) }
    var emptyDistText by remember { mutableStateOf(settings.calcEmptyDistanceKm.let { if (it > 0) it.toString() else "100" }) }
    var weightTonsText by remember { mutableStateOf(settings.calcWeightTons.let { if (it > 0) it.toString() else "28.5" }) }

    var fuelRateText by remember { mutableStateOf(settings.calcFuelConsumptionRate.let { if (it > 0) it.toString() else "33.0" }) }
    var fuelPriceText by remember { mutableStateOf(settings.calcFuelPricePerLiter.let { if (it > 0) it.toString() else "65.0" }) }

    var driverPayMode by remember { mutableStateOf(settings.calcDriverPayMode) } // "PER_KM" or "PERCENT"
    var driverRatePerKmText by remember { mutableStateOf(settings.calcDriverRatePerKm.let { if (it > 0) it.toString() else "12.0" }) }
    var driverSalaryPercentText by remember { mutableStateOf(settings.calcDriverSalaryPercent.let { if (it > 0) it.toString() else "20.0" }) }
    var driverPerDiemText by remember { mutableStateOf(settings.calcDriverPerDiemDaily.let { if (it > 0) it.toString() else "1500" }) }
    var tripDaysText by remember { mutableStateOf(settings.calcTripDays.let { if (it > 0) it.toString() else "1" }) }

    var platonRateText by remember { mutableStateOf(settings.calcPlatonRatePerKm.let { if (it > 0) it.toString() else "3.05" }) }
    var tollRoadsText by remember { mutableStateOf(settings.calcTollRoadsCost.let { if (it >= 0) it.toString() else "0" }) }
    var depreciationRateText by remember { mutableStateOf(settings.calcDepreciationRatePerKm.let { if (it > 0) it.toString() else "5.0" }) }

    var marginPercentText by remember { mutableStateOf(settings.calcDesiredMarginPercent.let { if (it > 0) it.toString() else "20.0" }) }

    // Parsed numerical values
    val loadedDist = loadedDistText.toDoubleOrNull() ?: 0.0
    val emptyDist = emptyDistText.toDoubleOrNull() ?: 0.0
    val totalDist = loadedDist + emptyDist
    val weightTons = weightTonsText.toDoubleOrNull() ?: 0.0

    val fuelRate = fuelRateText.toDoubleOrNull() ?: 0.0
    val fuelPrice = fuelPriceText.toDoubleOrNull() ?: 0.0

    val driverRatePerKm = driverRatePerKmText.toDoubleOrNull() ?: 0.0
    val driverSalaryPercent = driverSalaryPercentText.toDoubleOrNull() ?: 0.0
    val driverPerDiemDaily = driverPerDiemText.toDoubleOrNull() ?: 0.0
    val tripDays = tripDaysText.toIntOrNull() ?: 1

    val platonRate = platonRateText.toDoubleOrNull() ?: 0.0
    val tollRoads = tollRoadsText.toDoubleOrNull() ?: 0.0
    val depreciationRate = depreciationRateText.toDoubleOrNull() ?: 0.0

    val marginPercent = marginPercentText.toDoubleOrNull() ?: 0.0

    // Calculations
    val fuelLiters = if (totalDist > 0 && fuelRate > 0) totalDist * (fuelRate / 100.0) else 0.0
    val fuelCost = fuelLiters * fuelPrice

    val platonCost = totalDist * platonRate
    val tollCost = tollRoads
    val depreciationCost = totalDist * depreciationRate
    val totalPerDiem = tripDays * driverPerDiemDaily

    val directFixedCosts = fuelCost + platonCost + tollCost + depreciationCost + totalPerDiem

    val driverDistancePay: Double
    val totalDriverPay: Double
    val totalTripCost: Double
    val recommendedClientFreight: Double

    if (driverPayMode == "PERCENT") {
        // Driver gets % of the recommended freight
        // Freight = (directFixedCosts / (1 - (driverPercent + marginPercent) / 100.0))
        val totalDeductionPercent = (driverSalaryPercent + marginPercent) / 100.0
        recommendedClientFreight = if (totalDeductionPercent < 0.95) {
            directFixedCosts / (1.0 - totalDeductionPercent)
        } else {
            directFixedCosts * (1.0 + (marginPercent / 100.0)) * (1.0 + (driverSalaryPercent / 100.0))
        }
        val driverPctPay = recommendedClientFreight * (driverSalaryPercent / 100.0)
        driverDistancePay = driverPctPay
        totalDriverPay = driverPctPay + totalPerDiem
        totalTripCost = directFixedCosts - totalPerDiem + totalDriverPay
    } else {
        // PER_KM
        driverDistancePay = totalDist * driverRatePerKm
        totalDriverPay = driverDistancePay + totalPerDiem
        totalTripCost = directFixedCosts + driverDistancePay
        recommendedClientFreight = totalTripCost * (1.0 + (marginPercent / 100.0))
    }

    val netProfit = recommendedClientFreight - totalTripCost
    val costPerKm = if (totalDist > 0) totalTripCost / totalDist else 0.0
    val costPerTon = if (weightTons > 0) totalTripCost / weightTons else 0.0
    val freightPerTon = if (weightTons > 0) recommendedClientFreight / weightTons else 0.0
    val freightPerKm = if (totalDist > 0) recommendedClientFreight / totalDist else 0.0

    // Auto-save changes to settings in background
    LaunchedEffect(
        loadedDist, emptyDist, weightTons, fuelRate, fuelPrice,
        driverPayMode, driverRatePerKm, driverSalaryPercent, driverPerDiemDaily,
        tripDays, platonRate, tollRoads, depreciationRate, marginPercent
    ) {
        settings.calcLoadedDistanceKm = loadedDist
        settings.calcEmptyDistanceKm = emptyDist
        settings.calcWeightTons = weightTons
        settings.calcFuelConsumptionRate = fuelRate
        settings.calcFuelPricePerLiter = fuelPrice
        settings.calcDriverPayMode = driverPayMode
        settings.calcDriverRatePerKm = driverRatePerKm
        settings.calcDriverSalaryPercent = driverSalaryPercent
        settings.calcDriverPerDiemDaily = driverPerDiemDaily
        settings.calcTripDays = tripDays
        settings.calcPlatonRatePerKm = platonRate
        settings.calcTollRoadsCost = tollRoads
        settings.calcDepreciationRatePerKm = depreciationRate
        settings.calcDesiredMarginPercent = marginPercent
    }

    // Helper text report generator
    fun generateCalculationReport(): String {
        return buildString {
            appendLine("=== РАСЧЁТ СЕБЕСТОИМОСТИ РЕЙСА ===")
            appendLine("Маршрут: ${String.format(Locale.US, "%.0f", totalDist)} км (гружёный: ${String.format(Locale.US, "%.0f", loadedDist)} км, порожний: ${String.format(Locale.US, "%.0f", emptyDist)} км)")
            appendLine("Вес груза: ${String.format(Locale.US, "%.2f", weightTons)} т")
            appendLine()
            appendLine("СТАТЬИ РАСХОДОВ:")
            appendLine("• Топливо: ${String.format(Locale.US, "%.2f", fuelCost)} ₽ (${String.format(Locale.US, "%.1f", fuelLiters)} л по ${String.format(Locale.US, "%.2f", fuelPrice)} ₽/л)")
            if (driverPayMode == "PER_KM") {
                appendLine("• Зарплата водителя (${String.format(Locale.US, "%.2f", driverRatePerKm)} ₽/км): ${String.format(Locale.US, "%.2f", driverDistancePay)} ₽")
            } else {
                appendLine("• Зарплата водителя (${String.format(Locale.US, "%.1f", driverSalaryPercent)}% от фрахта): ${String.format(Locale.US, "%.2f", driverDistancePay)} ₽")
            }
            if (totalPerDiem > 0) {
                appendLine("• Суточные ($tripDays дн. × ${String.format(Locale.US, "%.0f", driverPerDiemDaily)} ₽): ${String.format(Locale.US, "%.2f", totalPerDiem)} ₽")
            }
            if (platonCost > 0) {
                appendLine("• Платон (${String.format(Locale.US, "%.2f", platonRate)} ₽/км): ${String.format(Locale.US, "%.2f", platonCost)} ₽")
            }
            if (tollCost > 0) {
                appendLine("• Платные дороги: ${String.format(Locale.US, "%.2f", tollCost)} ₽")
            }
            if (depreciationCost > 0) {
                appendLine("• Амортизация и ТО (${String.format(Locale.US, "%.2f", depreciationRate)} ₽/км): ${String.format(Locale.US, "%.2f", depreciationCost)} ₽")
            }
            appendLine()
            appendLine("ИТОГИ:")
            appendLine("Полная себестоимость рейса: ${String.format(Locale.US, "%.2f", totalTripCost)} ₽")
            appendLine("Себестоимость 1 км: ${String.format(Locale.US, "%.2f", costPerKm)} ₽/км")
            if (weightTons > 0) {
                appendLine("Себестоимость 1 т: ${String.format(Locale.US, "%.2f", costPerTon)} ₽/т")
            }
            appendLine("Желаемая рентабельность: ${String.format(Locale.US, "%.1f", marginPercent)}%")
            appendLine("РЕКОМЕНДУЕМАЯ СТАВКА КЛИЕНТУ: ${String.format(Locale.US, "%.2f", recommendedClientFreight)} ₽")
            if (weightTons > 0) {
                appendLine("Ставка за тонну клиенту: ${String.format(Locale.US, "%.2f", freightPerTon)} ₽/т")
            }
            appendLine("Ставка за км: ${String.format(Locale.US, "%.2f", freightPerKm)} ₽/км")
            appendLine("Планируемая прибыль: ${String.format(Locale.US, "%.2f", netProfit)} ₽")
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Калькулятор рейса",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val text = generateCalculationReport()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Расчет рейса", text))
                            Toast.makeText(context, "Расчёт скопирован", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать")
                    }
                    IconButton(
                        onClick = {
                            val text = generateCalculationReport()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Расчёт себестоимости перевозки")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Поделиться расчётом"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться")
                    }
                    IconButton(
                        onClick = {
                            loadedDistText = "280"
                            emptyDistText = "100"
                            weightTonsText = "28.5"
                            fuelRateText = "33.0"
                            fuelPriceText = "65.0"
                            driverPayMode = "PER_KM"
                            driverRatePerKmText = "12.0"
                            driverSalaryPercentText = "20.0"
                            driverPerDiemText = "1500"
                            tripDaysText = "1"
                            platonRateText = "3.05"
                            tollRoadsText = "0"
                            depreciationRateText = "5.0"
                            marginPercentText = "20.0"
                            Toast.makeText(context, "Сброшено к стандартам", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сброс")
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- RESULT SUMMARY CARD ---
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_result_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Рекомендуемая ставка клиенту",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+${String.format(Locale.US, "%.0f", marginPercent)}% наценка",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${String.format(Locale.US, "%,.0f", recommendedClientFreight).replace(',', ' ')} ₽",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (weightTons > 0 || totalDist > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (weightTons > 0) {
                                Text(
                                    text = "Ставка за т: ${String.format(Locale.US, "%.0f", freightPerTon)} ₽/т",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (totalDist > 0) {
                                Text(
                                    text = "За км: ${String.format(Locale.US, "%.1f", freightPerKm)} ₽/км",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Summary details grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Себестоимость",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%,.0f", totalTripCost).replace(',', ' ')} ₽",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", costPerKm)} ₽/км" +
                                        if (weightTons > 0) " • ${String.format(Locale.US, "%.0f", costPerTon)} ₽/т" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Плановая прибыль",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "+${String.format(Locale.US, "%,.0f", netProfit).replace(',', ' ')} ₽",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", (if (recommendedClientFreight > 0) (netProfit / recommendedClientFreight) * 100 else 0.0))}% от фрахта",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Breakdown bars
                    CostBreakdownItem(
                        label = "Топливо (${String.format(Locale.US, "%.1f", fuelLiters)} л)",
                        amount = fuelCost,
                        total = totalTripCost
                    )
                    CostBreakdownItem(
                        label = "Водитель (з/п + суточные)",
                        amount = totalDriverPay,
                        total = totalTripCost
                    )
                    if (platonCost > 0) {
                        CostBreakdownItem(
                            label = "Платон",
                            amount = platonCost,
                            total = totalTripCost
                        )
                    }
                    if (tollCost > 0) {
                        CostBreakdownItem(
                            label = "Платные дороги",
                            amount = tollCost,
                            total = totalTripCost
                        )
                    }
                    if (depreciationCost > 0) {
                        CostBreakdownItem(
                            label = "Амортизация и ТО",
                            amount = depreciationCost,
                            total = totalTripCost
                        )
                    }
                }
            }

            // --- SECTION 1: ROUTE & CARGO ---
            CalculatorCard(
                title = "Маршрут и груз",
                icon = Icons.Default.Route
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = loadedDistText,
                        onValueChange = { loadedDistText = it },
                        label = { Text("С грузом (км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_input_loaded_km"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = emptyDistText,
                        onValueChange = { emptyDistText = it },
                        label = { Text("Порожний (км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_input_empty_km"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Общий пробег рейса: ${String.format(Locale.US, "%.0f", totalDist)} км",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedButton(
                        onClick = { emptyDistText = loadedDistText },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Кругорейс (1:1)", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = weightTonsText,
                    onValueChange = { weightTonsText = it },
                    label = { Text("Вес груза (тонн)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_input_weight_tons"),
                    singleLine = true
                )
            }

            // --- SECTION 2: FUEL ---
            CalculatorCard(
                title = "Топливо",
                icon = Icons.Default.LocalGasStation
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = fuelRateText,
                        onValueChange = { fuelRateText = it },
                        label = { Text("Расход (л / 100 км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_input_fuel_rate"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fuelPriceText,
                        onValueChange = { fuelPriceText = it },
                        label = { Text("Цена за 1 л (₽)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_input_fuel_price"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Расход: ${String.format(Locale.US, "%.1f", fuelLiters)} л • Затраты: ${String.format(Locale.US, "%,.0f", fuelCost).replace(',', ' ')} ₽",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- SECTION 3: DRIVER PAYMENT ---
            CalculatorCard(
                title = "Водитель",
                icon = Icons.Default.Person
            ) {
                Text(
                    text = "Тип оплаты:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = driverPayMode == "PER_KM",
                        onClick = { driverPayMode = "PER_KM" },
                        label = { Text("За км (₽/км)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = driverPayMode == "PERCENT",
                        onClick = { driverPayMode = "PERCENT" },
                        label = { Text("% от фрахта") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (driverPayMode == "PER_KM") {
                    OutlinedTextField(
                        value = driverRatePerKmText,
                        onValueChange = { driverRatePerKmText = it },
                        label = { Text("Ставка водителя (₽ за км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_input_driver_rate_km"),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = driverSalaryPercentText,
                        onValueChange = { driverSalaryPercentText = it },
                        label = { Text("Процент водителя от фрахта (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_input_driver_percent"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = driverPerDiemText,
                        onValueChange = { driverPerDiemText = it },
                        label = { Text("Суточные (₽/сутки)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("calc_input_per_diem"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tripDaysText,
                        onValueChange = { tripDaysText = it },
                        label = { Text("Дней") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("calc_input_days"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Итого водителю: ${String.format(Locale.US, "%,.0f", totalDriverPay).replace(',', ' ')} ₽ (з/п ${String.format(Locale.US, "%.0f", driverDistancePay)} ₽ + суточные ${String.format(Locale.US, "%.0f", totalPerDiem)} ₽)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- SECTION 4: ROAD EXPENSES & DEPRECIATION ---
            CalculatorCard(
                title = "Дороги, Платон и Амортизация",
                icon = Icons.Default.Toll
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = platonRateText,
                        onValueChange = { platonRateText = it },
                        label = { Text("Платон (₽/км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_input_platon"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tollRoadsText,
                        onValueChange = { tollRoadsText = it },
                        label = { Text("Платные дороги (₽)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_input_toll"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = depreciationRateText,
                    onValueChange = { depreciationRateText = it },
                    label = { Text("ТО, шины и амортизация (₽/км)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_input_deprec"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                val roadDeprecTotal = platonCost + tollCost + depreciationCost
                Text(
                    text = "Всего дорожные + ТО: ${String.format(Locale.US, "%,.0f", roadDeprecTotal).replace(',', ' ')} ₽",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- SECTION 5: PROFIT MARGIN ---
            CalculatorCard(
                title = "Желаемая наценка (маржа)",
                icon = Icons.AutoMirrored.Filled.TrendingUp
            ) {
                OutlinedTextField(
                    value = marginPercentText,
                    onValueChange = { marginPercentText = it },
                    label = { Text("Желаемая рентабельность / наценка (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_input_margin"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick margin preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10.0, 15.0, 20.0, 25.0, 30.0).forEach { preset ->
                        FilterChip(
                            selected = (marginPercent == preset),
                            onClick = { marginPercentText = preset.toString() },
                            label = { Text("${preset.toInt()}%") }
                        )
                    }
                }
            }

            // --- BOTTOM ACTIONS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val text = generateCalculationReport()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Расчет рейса", text))
                        Toast.makeText(context, "Скопировано в буфер", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Скопировать")
                }

                Button(
                    onClick = {
                        val text = generateCalculationReport()
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Расчёт перевозки")
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Отправить расчет"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Поделиться")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CalculatorCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun CostBreakdownItem(
    label: String,
    amount: Double,
    total: Double
) {
    val pct = if (total > 0) (amount / total) * 100 else 0.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${String.format(Locale.US, "%,.0f", amount).replace(',', ' ')} ₽ (${String.format(Locale.US, "%.0f", pct)}%)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
