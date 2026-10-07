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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
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
    
    var vehicleDropdownExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var driverDropdownExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var dateDropdownExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val dateOptions = listOf("За все время", "Этот месяц", "Прошлый месяц")
    var selectedDateText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(dateOptions[0]) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Filters
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Date Filter
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Vehicle Filter
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

            // Driver Filter
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
        // Hero Card: TCO (РЎРѕРІРѕРєСѓРїРЅР°СЏ СЃС‚РѕРёРјРѕСЃС‚СЊ РІР»Р°РґРµРЅРёСЏ)
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
                            text = "TCO (РЎРѕРІРѕРєСѓРїРЅР°СЏ СЃС‚РѕРёРјРѕСЃС‚СЊ)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "РџРѕР»РЅС‹Рµ СЌРєСЃРїР»СѓР°С‚Р°С†РёРѕРЅРЅС‹Рµ Р·Р°С‚СЂР°С‚С‹ РїР°СЂРєР°",
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
                            text = "РЈРґРµР»СЊРЅР°СЏ СЃРµР±РµСЃС‚РѕРёРјРѕСЃС‚СЊ 1 РєРј:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.2f в‚Ѕ / РєРј", summary.costPerKm),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Breakdown Cards: РўРѕРїР»РёРІРѕ, РўРћ Рё РЎРµСЂРІРёСЃ, Р”РѕРєСѓРјРµРЅС‚С‹
        Text(
            text = "РЎС‚СЂСѓРєС‚СѓСЂР° СЂР°СЃС…РѕРґРѕРІ Р°РІС‚РѕРїР°СЂРєР°",
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
                    Text("Р Р°СЃС…РѕРґ Р“РЎРњ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMoney(summary.totalFuelCost),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${summary.totalFuelLiters.toInt()} Р»РёС‚СЂРѕРІ Р”Рў",
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
                    Text("РЎРµСЂРІРёСЃ Рё РўРћ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMoney(summary.totalServiceCost),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("РўРћ-1, РўРћ-2, СЂРµРјРѕРЅС‚С‹", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
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
                    Text("РЎС‚СЂР°С…РѕРІРєРё Рё Р”Рљ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatMoney(summary.totalDocumentsCost),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("РћРЎРђР“Рћ, РљРђРЎРљРћ, РїСЂРѕРїСѓСЃРєР°", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
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
                    Text("РћР±С‰РёР№ РїСЂРѕР±РµРі", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatDistance(summary.totalFleetDistanceKm),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Р’СЃРµ СЂРµР№СЃС‹ Рё РџР›", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // Driver Payroll & Profitability Card
        Text(
            text = "Р¤РћРў РІРѕРґРёС‚РµР»РµР№ Рё СЌРєРѕРЅРѕРјРёРєР° РїРµСЂРµРІРѕР·РѕРє",
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
                                text = "Р¤РѕРЅРґ РѕРїР»Р°С‚С‹ С‚СЂСѓРґР° РІРѕРґРёС‚РµР»РµР№",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "РќР°С‡РёСЃР»РµРЅРёСЏ (% РѕС‚ С„СЂР°С…С‚Р° СЂРµР№СЃРѕРІ)",
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
                            text = "Р’С‹СЂСѓС‡РєР° (С„СЂР°С…С‚):",
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
                            text = "РЁС‚Р°С‚ РІРѕРґРёС‚РµР»РµР№:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${summary.totalDrivers} С‡РµР».",
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
                            text = "Р§РёСЃС‚Р°СЏ РїСЂРёР±С‹Р»СЊ:",
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
            text = "Р“РѕС‚РѕРІРЅРѕСЃС‚СЊ Рё СЌРєСЃРїР»СѓР°С‚Р°С†РёСЏ С‚РµС…РЅРёРєРё",
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
                        Text("Р’СЃРµРіРѕ С‚РµС…РЅРёРєРё РІ РїР°СЂРєРµ:", fontWeight = FontWeight.SemiBold)
                    }
                    Text("${summary.totalVehicles} РµРґ.", fontWeight = FontWeight.ExtraBold)
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
                        Text("Р’ СЂРµР№СЃРµ РЅР° Р»РёРЅРёРё:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("${summary.activeVehiclesOnTrip} РµРґ.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
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
                        Text("Р“РѕС‚РѕРІС‹ Рє СЂРµР№СЃСѓ (СЃРІРѕР±РѕРґРЅС‹):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("${summary.availableVehicles} РµРґ.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
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
                        Text("РќР° РўРћ / Р РµРјРѕРЅС‚Рµ:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("${summary.vehiclesInService} РµРґ.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFED6C02))
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
                        Text("Р’РѕРґРёС‚РµР»СЊСЃРєРёР№ СЃРѕСЃС‚Р°РІ:", fontWeight = FontWeight.SemiBold)
                    }
                    Text("${summary.totalDrivers} С‡РµР».", fontWeight = FontWeight.ExtraBold)
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
                            text = "РљРѕРЅС‚СЂРѕР»СЊ РґРѕРєСѓРјРµРЅС‚РѕРІ Рё СЃСЂРѕРєРѕРІ",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "РџСЂРѕСЃСЂРѕС‡РµРЅРѕ: ${summary.expiredDocsCount} вЂў РўСЂРµР±СѓСЋС‚ СЃРєРѕСЂРѕРіРѕ РїСЂРѕРґР»РµРЅРёСЏ (<30 РґРЅРµР№): ${summary.expiringDocsCount}.",
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
                Text("РџРµСЂРµР№С‚Рё Рє СЃРІРѕРґРЅС‹Рј РѕС‚С‡РµС‚Р°Рј Рё СЂРµРµСЃС‚СЂР°Рј", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}







