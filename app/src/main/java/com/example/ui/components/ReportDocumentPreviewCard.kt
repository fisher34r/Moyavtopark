package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trip
import com.example.util.Formatters
import java.util.Locale
import kotlin.math.roundToLong

enum class ReportType(
    val title: String,
    val docHeader: String,
    val subtitle: String
) {
    REGISTRY(
        title = "Реестр рейсов",
        docHeader = "СВОДНЫЙ РЕЕСТР ВЫПОЛНЕННЫХ РЕЙСОВ ЗЕРНОВОЗА",
        subtitle = "Порейсовая детализация по авто, вес, тариф и стоимость"
    ),
    FINANCIAL(
        title = "Финансовый",
        docHeader = "ФИНАНСОВЫЙ ОТЧЕТ ПО ЭКСПЛУАТАЦИИ ЗЕРНОВОЗОВ",
        subtitle = "Выручка, расходы, маржинальность и чистая прибыль"
    ),
    PAYROLL(
        title = "Ведомость ЗП",
        docHeader = "ВЕДОМОСТЬ НАЧИСЛЕНИЯ ЗАРПЛАТЫ ВОДИТЕЛЕЙ",
        subtitle = "Порейсовые начисления % от фрахта и выплаты"
    ),
    CARGO(
        title = "По культурам",
        docHeader = "ОТЧЕТ ПО ПЕРЕВОЗКЕ ЗЕРНОВЫХ КУЛЬТУР",
        subtitle = "Объемы, тоннаж, выручка и структура грузов"
    ),
    FUEL_LOGISTICS(
        title = "ГСМ и логистика",
        docHeader = "ОТЧЕТ ПО РАСХОДУ ТОПЛИВА И ТРАНСПОРТНОЙ РАБОТЕ",
        subtitle = "Дизтопливо, нормы расхода и пробег парка"
    )
}

data class DriverPayrollItem(
    val driverName: String,
    val tripsCount: Int,
    val totalWeightTons: Double,
    val totalFreight: Double,
    val avgPercent: Double,
    val totalSalary: Double
)

data class CropReportItem(
    val cropName: String,
    val tripsCount: Int,
    val weightTons: Double,
    val revenue: Double,
    val sharePercent: Float
)

@Composable
fun ReportDocumentPreviewCard(
    reportType: ReportType,
    periodTitle: String,
    generatedDateTime: String,
    trips: List<Trip>,
    totalRevenue: Double,
    totalFuelExpenses: Double,
    totalFuelLiters: Double,
    totalDriverSalary: Double,
    totalOtherExpenses: Double,
    totalExpenses: Double,
    totalProfit: Double,
    totalWeight: Double,
    totalDistance: Double,
    totalTonKm: Double,
    driverItems: List<DriverPayrollItem>,
    cropItems: List<CropReportItem>,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onExportPdf: (() -> Unit)? = null,
    onExportExcel: (() -> Unit)? = null,
    truckPlate: String? = null,
    driverName: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF155383).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF155383),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Печатная форма документа",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF155383)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Document Paper Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .background(Color(0xFFFAFAFA))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header of Document
                    if (reportType == ReportType.REGISTRY) {
                        // Точный заголовок из референса пользователя
                        val displayTruck = (truckPlate ?: trips.firstOrNull()?.truckPlate?.takeIf { it.isNotBlank() } ?: "ВСЕ ТС").uppercase(Locale.forLanguageTag("ru"))
                        val displayDriver = driverName ?: trips.firstOrNull()?.driverName?.takeIf { it.isNotBlank() } ?: "Все водители"

                        Text(
                            text = "АВТОМОБИЛЬ $displayTruck",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF104A75) // Фирменный синий цвет из референса
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Водитель: $displayDriver | Период: $periodTitle",
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF475569)
                        )
                    } else {
                        Text(
                            text = "ООО «АГРОТРАНС-СЕРВИС» • УЧЕТ ЗЕРНОВОЗОВ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF616161),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reportType.docHeader,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF212121),
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Отчетный период: $periodTitle",
                                fontSize = 11.sp,
                                color = Color(0xFF424242)
                            )
                            Text(
                                text = "Дата: $generatedDateTime",
                                fontSize = 10.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Body based on report type
                    when (reportType) {
                        ReportType.REGISTRY -> {
                            RegistryDocumentTable(trips = trips)
                        }
                        ReportType.FINANCIAL -> {
                            FinancialDocumentTable(
                                totalRevenue = totalRevenue,
                                totalFuelExpenses = totalFuelExpenses,
                                totalFuelLiters = totalFuelLiters,
                                totalDriverSalary = totalDriverSalary,
                                totalOtherExpenses = totalOtherExpenses,
                                totalExpenses = totalExpenses,
                                totalProfit = totalProfit,
                                totalWeight = totalWeight,
                                totalDistance = totalDistance
                            )
                        }
                        ReportType.PAYROLL -> {
                            PayrollDocumentTable(
                                driverItems = driverItems,
                                totalSalary = totalDriverSalary,
                                totalFreight = totalRevenue
                            )
                        }
                        ReportType.CARGO -> {
                            CargoDocumentTable(
                                cropItems = cropItems,
                                totalWeight = totalWeight,
                                totalRevenue = totalRevenue
                            )
                        }
                        ReportType.FUEL_LOGISTICS -> {
                            FuelLogisticsDocumentTable(
                                totalDistance = totalDistance,
                                totalTonKm = totalTonKm,
                                totalFuelLiters = totalFuelLiters,
                                totalFuelExpenses = totalFuelExpenses,
                                totalRevenue = totalRevenue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Signatures block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Отчет сформирован:", fontSize = 9.sp, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Диспетчер ___________", fontSize = 9.sp, color = Color(0xFF334155))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Утверждаю:", fontSize = 9.sp, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Руководитель / М.П. ___________", fontSize = 9.sp, color = Color(0xFF334155))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Реестр рейсов по предоставленному референсу:
 * Шапка: #155383, колонки:
 * № п/п | Дата | Пункт отправления | Вес нетто (кг) | Тариф (руб/кг) | Стоимость (руб.)
 * ИТОГО: суммарный вес и суммарная стоимость
 */
@Composable
private fun RegistryDocumentTable(
    trips: List<Trip>
) {
    // В отчете «реестр рейсов» сортировать рейсы по дате от меньшего к большему
    val sortedTrips = remember(trips) { trips.sortedBy { it.loadingDate } }
    val totalWeightKg = sortedTrips.sumOf { it.weightTons * 1000.0 }.roundToLong()
    val totalPrice = sortedTrips.sumOf { it.totalPrice }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCBD5E1))
    ) {
        // Таблица: Шапка (Corporate Blue #155383)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF155383))
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("№ п/п", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color.White, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
            Text("Дата", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color.White, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
            Text("Пункт отправления", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color.White, modifier = Modifier.weight(1.8f))
            Text("Вес нетто (кг)", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color.White, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
            Text("Тариф (руб/кг)", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color.White, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
            Text("Стоимость (руб.)", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color.White, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
        }

        if (sortedTrips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет рейсов за выбранный период", fontSize = 11.sp, color = Color.Gray)
            }
        } else {
            sortedTrips.forEachIndexed { index, trip ->
                val weightKg = (trip.weightTons * 1000.0).roundToLong()
                val tariffKg = if (weightKg > 0) trip.totalPrice / weightKg else 0.0
                val isAlt = index % 2 == 1

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isAlt) Color(0xFFF8FAFC) else Color.White)
                        .padding(vertical = 4.5.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}", fontSize = 8.5.sp, color = Color(0xFF334155), modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                    Text(Formatters.formatDate(trip.loadingDate), fontSize = 8.sp, color = Color(0xFF1E293B), modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                    Text(trip.loadingLocation.ifBlank { "Манойлино" }, fontSize = 8.5.sp, color = Color(0xFF0F172A), maxLines = 1, modifier = Modifier.weight(1.8f))
                    Text(Formatters.formatWeightKgExact(weightKg), fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                    Text(String.format(Locale.forLanguageTag("ru"), "%.2f", tariffKg), fontSize = 8.5.sp, color = Color(0xFF334155), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                    Text(Formatters.formatMoneyRub(trip.totalPrice), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                }
            }

            // Итоговая строка
            HorizontalDivider(color = Color(0xFF155383), thickness = 1.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFF6FF))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ИТОГО:", fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, color = Color(0xFF104A75), modifier = Modifier.weight(3.5f))
                Text(Formatters.formatWeightKgExact(totalWeightKg), fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, color = Color(0xFF104A75), modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                Text("", modifier = Modifier.weight(1.1f))
                Text(Formatters.formatMoneyRub(totalPrice), fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, color = Color(0xFF0F5132), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun FinancialDocumentTable(
    totalRevenue: Double,
    totalFuelExpenses: Double,
    totalFuelLiters: Double,
    totalDriverSalary: Double,
    totalOtherExpenses: Double,
    totalExpenses: Double,
    totalProfit: Double,
    totalWeight: Double,
    totalDistance: Double
) {
    val margin = if (totalRevenue > 0) (totalProfit / totalRevenue * 100.0) else 0.0

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DocTableRow("1. Выручка от фрахта (начислено заказчиками)", Formatters.formatMoney(totalRevenue), isBold = true, valueColor = Color(0xFF1B5E20))
        DocTableRow("2. Расходы на дизельное топливо (${String.format(Locale.US, "%.1f", totalFuelLiters)} л)", "- ${Formatters.formatMoney(totalFuelExpenses)}", valueColor = Color(0xFFB71C1C))
        DocTableRow("3. Фонд оплаты труда водителей (начислено ЗП)", "- ${Formatters.formatMoney(totalDriverSalary)}", valueColor = Color(0xFFB71C1C))
        DocTableRow("4. Прочие расходы (Платон, весовая, стоянки)", "- ${Formatters.formatMoney(totalOtherExpenses)}", valueColor = Color(0xFFB71C1C))
        HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 0.5.dp)
        DocTableRow("ИТОГО РАСХОДОВ НА ПЕРЕВОЗКИ:", "- ${Formatters.formatMoney(totalExpenses)}", isBold = true, valueColor = Color(0xFFB71C1C))
        DocTableRow("ЧИСТАЯ ПРИБЫЛЬ АВТОПАРКА:", Formatters.formatMoney(totalProfit), isBold = true, fontSize = 12.sp, valueColor = if (totalProfit >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C))
        DocTableRow("Рентабельность рейсов (маржа):", String.format(Locale.US, "%.1f %%", margin))
    }
}

@Composable
private fun PayrollDocumentTable(
    driverItems: List<DriverPayrollItem>,
    totalSalary: Double,
    totalFreight: Double
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEEEEEE))
                .padding(vertical = 5.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Водитель", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1.3f))
            Text("Рейсов", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
            Text("Вес нетто", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End)
            Text("Фрахт", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
            Text("К выплате", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
        }

        HorizontalDivider(color = Color(0xFFBDBDBD))

        if (driverItems.isEmpty()) {
            Text("Нет данных по водителям", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            driverItems.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (index % 2 == 1) Color(0xFFF9F9F9) else Color.Transparent)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.driverName, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.3f))
                    Text("${item.tripsCount}", fontSize = 9.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                    Text(Formatters.formatWeight(item.totalWeightTons), fontSize = 8.5.sp, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End)
                    Text(Formatters.formatMoney(item.totalFreight), fontSize = 8.5.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                    Text(Formatters.formatMoney(item.totalSalary), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, color = Color(0xFF1B5E20))
                }
            }
        }

        HorizontalDivider(color = Color(0xFF424242), thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ИТОГО К ВЫПЛАТЕ:", fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, modifier = Modifier.weight(1.9f))
            Text(Formatters.formatMoney(totalFreight), fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
            Text(Formatters.formatMoney(totalSalary), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = Color(0xFF1B5E20), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun CargoDocumentTable(
    cropItems: List<CropReportItem>,
    totalWeight: Double,
    totalRevenue: Double
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEEEEEE))
                .padding(vertical = 5.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Культура", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1.3f))
            Text("Рейсов", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
            Text("Вес", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
            Text("Доля", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
            Text("Выручка", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
        }

        HorizontalDivider(color = Color(0xFFBDBDBD))

        if (cropItems.isEmpty()) {
            Text("Нет данных по культурам", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            cropItems.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (index % 2 == 1) Color(0xFFF9F9F9) else Color.Transparent)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.cropName, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.3f))
                    Text("${item.tripsCount}", fontSize = 9.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                    Text(Formatters.formatWeight(item.weightTons), fontSize = 8.5.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                    Text(String.format(Locale.US, "%.1f%%", item.sharePercent), fontSize = 8.5.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                    Text(Formatters.formatMoney(item.revenue), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                }
            }
        }

        HorizontalDivider(color = Color(0xFF424242), thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ИТОГО КУЛЬТУР:", fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, modifier = Modifier.weight(1.3f))
            Text("${cropItems.sumOf { it.tripsCount }}", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
            Text(Formatters.formatWeight(totalWeight), fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, color = Color(0xFF1B5E20), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
            Text("100%", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
            Text(Formatters.formatMoney(totalRevenue), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = Color(0xFF1B5E20), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun FuelLogisticsDocumentTable(
    totalDistance: Double,
    totalTonKm: Double,
    totalFuelLiters: Double,
    totalFuelExpenses: Double,
    totalRevenue: Double
) {
    val avgLitersPer100Km = if (totalDistance > 0) (totalFuelLiters / totalDistance) * 100.0 else 0.0
    val costPerKm = if (totalDistance > 0) totalFuelExpenses / totalDistance else 0.0
    val costShareOfRevenue = if (totalRevenue > 0) (totalFuelExpenses / totalRevenue) * 100.0 else 0.0

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DocTableRow("1. Общий пробег автопарка", Formatters.formatDistance(totalDistance), isBold = true)
        DocTableRow("2. Выполненная транспортная работа", Formatters.formatTonKm(totalTonKm))
        DocTableRow("3. Расход дизельного топлива", "${String.format(Locale.US, "%.1f", totalFuelLiters)} л")
        DocTableRow("4. Средний фактический расход топлива", "${String.format(Locale.US, "%.1f", avgLitersPer100Km)} л / 100 км", isBold = true)
        DocTableRow("5. Затраты на топливо (ГСМ)", Formatters.formatMoney(totalFuelExpenses), isBold = true, valueColor = Color(0xFFB71C1C))
        DocTableRow("6. Доля расходов на ГСМ в общей выручке", String.format(Locale.US, "%.1f %%", costShareOfRevenue))
        DocTableRow("7. Затраты топлива на 1 километр пути", String.format(Locale.US, "%.2f ₽ / км", costPerKm))
    }
}

@Composable
private fun DocTableRow(
    title: String,
    value: String,
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    valueColor: Color = Color(0xFF212121)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) Color(0xFF212121) else Color(0xFF424242),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}
