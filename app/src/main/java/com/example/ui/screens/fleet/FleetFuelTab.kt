package com.example.ui.screens.fleet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.fleet.FuelRecord
import com.example.data.fleet.Vehicle
import com.example.ui.viewmodel.FleetViewModel
import com.example.util.Formatters
import java.util.Locale

@Composable
fun FleetFuelTab(
    viewModel: FleetViewModel,
    modifier: Modifier = Modifier
) {
    val fuelRecords by viewModel.fuelRecords.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<FuelRecord?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<FuelRecord?>(null) }

    var isBatchMode by remember { mutableStateOf(false) }
    val selectedRecordIds = remember { mutableStateListOf<Long>() }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isBatchMode) {
                // Batch Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isBatchMode = false
                            selectedRecordIds.clear()
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Отмена")
                    }
                    Text(
                        text = "Выбрано: ${selectedRecordIds.size}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (selectedRecordIds.size == fuelRecords.size) {
                                selectedRecordIds.clear()
                            } else {
                                selectedRecordIds.clear()
                                selectedRecordIds.addAll(fuelRecords.map { it.id })
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selectedRecordIds.size == fuelRecords.size) Icons.Default.Close else Icons.Default.SelectAll,
                            contentDescription = "Выбрать все"
                        )
                    }
                    IconButton(
                        onClick = { showBatchEditDialog = true },
                        enabled = selectedRecordIds.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Пакетное изменение",
                            tint = if (selectedRecordIds.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { showBatchDeleteConfirm = true },
                        enabled = selectedRecordIds.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Пакетное удаление",
                            tint = if (selectedRecordIds.isNotEmpty()) MaterialTheme.colorScheme.error else Color.Gray
                        )
                    }
                }
            } else {
                // Header with stats & batch toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val totalLiters = fuelRecords.sumOf { it.liters }
                    val totalCost = fuelRecords.sumOf { it.totalCost }
                    Column {
                        Text(
                            text = "Чеков: ${fuelRecords.size} • ${String.format(Locale.US, "%.0f", totalLiters)} л",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Всего затрат: ${Formatters.formatMoney(totalCost)}",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    if (fuelRecords.isNotEmpty()) {
                        IconButton(onClick = { isBatchMode = true }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Пакетный выбор", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (fuelRecords.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.LocalGasStation, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Нет записей о заправках", color = Color.Gray, fontSize = 16.sp)
                    Text("Нажмите + чтобы добавить чек с АЗС", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                ) {
                    items(fuelRecords, key = { it.id }) { record ->
                        val isSelected = selectedRecordIds.contains(record.id)
                        val v = vehicles.find { it.plateNumber == record.vehiclePlate }
                        FuelRecordCard(
                            record = record,
                            vehicle = v,
                            isSelectionMode = isBatchMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                if (isSelected) selectedRecordIds.remove(record.id)
                                else selectedRecordIds.add(record.id)
                            },
                            onLongClick = {
                                if (!isBatchMode) {
                                    isBatchMode = true
                                    selectedRecordIds.add(record.id)
                                }
                            },
                            onClick = { selectedRecord = record; showAddDialog = true },
                            onDeleteClick = { showDeleteConfirm = record }
                        )
                    }
                }
            }
        }
        
        if (!isBatchMode) {
            FloatingActionButton(
                onClick = { selectedRecord = null; showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить заправку")
            }
        }
    }
    
    if (showAddDialog) {
        AddFuelRecordDialog(
            record = selectedRecord,
            vehicles = vehicles,
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.saveFuelRecord(it)
                showAddDialog = false
            }
        )
    }
    
    showDeleteConfirm?.let { record ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Удаление заправки") },
            text = { Text("Вы уверены, что хотите удалить чек на ${record.liters} л (${Formatters.formatMoney(record.totalCost)})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFuelRecord(record)
                        showDeleteConfirm = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Отмена") }
            }
        )
    }

    // Batch Delete Dialog
    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("Удалить выбранные заправки?") },
            text = { Text("Будет безвозвратно удалено ${selectedRecordIds.size} чеков. Топливо будет списано из баков соответствующих ТС.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.batchDeleteFuelRecords(selectedRecordIds.toSet())
                        selectedRecordIds.clear()
                        isBatchMode = false
                        showBatchDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить (${selectedRecordIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("Отмена") }
            }
        )
    }

    // Batch Edit Dialog
    if (showBatchEditDialog) {
        BatchEditFuelRecordsDialog(
            selectedCount = selectedRecordIds.size,
            vehicles = vehicles,
            onDismiss = { showBatchEditDialog = false },
            onSave = { newPlate, newStation ->
                viewModel.batchUpdateFuelRecords(
                    ids = selectedRecordIds.toSet(),
                    newVehiclePlate = newPlate,
                    newStationName = newStation
                )
                selectedRecordIds.clear()
                isBatchMode = false
                showBatchEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FuelRecordCard(
    record: FuelRecord,
    vehicle: Vehicle? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelect()
                    else onClick()
                },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = Color(0xFF2E7D32))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", record.liters)} л • ${Formatters.formatMoney(record.totalCost)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (record.pricePerLiter > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${String.format(Locale.US, "%.2f", record.pricePerLiter)} ₽/л)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ТС: ${record.vehiclePlate}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (vehicle != null && vehicle.fuelTankCapacityLiters > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Бак: ${vehicle.currentFuelLiters.toInt()}/${vehicle.fuelTankCapacityLiters.toInt()} л",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
                Text(
                    text = Formatters.formatDate(record.date) + if (record.stationName.isNotBlank()) " • ${record.stationName}" else "",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            if (!isSelectionMode) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchEditFuelRecordsDialog(
    selectedCount: Int,
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (newPlate: String?, newStation: String?) -> Unit
) {
    var changeVehicle by remember { mutableStateOf(false) }
    var selectedPlate by remember { mutableStateOf("") }
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }

    var changeStation by remember { mutableStateOf(false) }
    var stationText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пакетное изменение ($selectedCount чеков)") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = changeVehicle, onCheckedChange = { changeVehicle = it })
                    Text("Изменить автомобиль ТС", fontSize = 13.sp)
                }
                if (changeVehicle) {
                    ExposedDropdownMenuBox(
                        expanded = vehicleDropdownExpanded,
                        onExpandedChange = { vehicleDropdownExpanded = !vehicleDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedPlate,
                            onValueChange = { selectedPlate = it },
                            label = { Text("Автомобиль ТС") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = vehicleDropdownExpanded,
                            onDismissRequest = { vehicleDropdownExpanded = false }
                        ) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("${v.plateNumber} (${v.model})") },
                                    onClick = {
                                        selectedPlate = v.plateNumber
                                        vehicleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = changeStation, onCheckedChange = { changeStation = it })
                    Text("Изменить сеть / название АЗС", fontSize = 13.sp)
                }
                if (changeStation) {
                    OutlinedTextField(
                        value = stationText,
                        onValueChange = { stationText = it },
                        label = { Text("Название АЗС") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val plate = if (changeVehicle && selectedPlate.isNotBlank()) selectedPlate else null
                    val station = if (changeStation && stationText.isNotBlank()) stationText else null
                    onSave(plate, station)
                }
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFuelRecordDialog(
    record: FuelRecord?,
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (FuelRecord) -> Unit
) {
    var vehiclePlate by remember { mutableStateOf(record?.vehiclePlate ?: vehicles.firstOrNull()?.plateNumber ?: "") }
    var pricePerLiterText by remember { mutableStateOf(record?.pricePerLiter?.let { if (it > 0) String.format(Locale.US, "%.2f", it) else "" } ?: "66.00") }
    var litersText by remember { mutableStateOf(record?.liters?.let { String.format(Locale.US, "%.1f", it) } ?: "") }
    var totalCostText by remember { mutableStateOf(record?.totalCost?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var stationName by remember { mutableStateOf(record?.stationName ?: "") }
    var odometerText by remember { mutableStateOf(record?.odometerKm?.let { if (it > 0) it.toInt().toString() else "" } ?: "") }
    
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }

    val currentSelectedVehicle = vehicles.find { it.plateNumber.equals(vehiclePlate, ignoreCase = true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (record == null) "Новая заправка" else "Редактировать заправку") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = vehicleDropdownExpanded,
                    onExpandedChange = { vehicleDropdownExpanded = !vehicleDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = vehiclePlate,
                        onValueChange = { vehiclePlate = it },
                        label = { Text("Автомобиль *") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleDropdownExpanded) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = vehicleDropdownExpanded,
                        onDismissRequest = { vehicleDropdownExpanded = false }
                    ) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text("${v.plateNumber} (${v.model})") },
                                onClick = {
                                    vehiclePlate = v.plateNumber
                                    vehicleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Show tank remaining status for selected vehicle
                currentSelectedVehicle?.let { v ->
                    if (v.fuelTankCapacityLiters > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Текущий остаток в баке: ${v.currentFuelLiters.toInt()} / ${v.fuelTankCapacityLiters.toInt()} л",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                // Price per liter field
                OutlinedTextField(
                    value = pricePerLiterText,
                    onValueChange = { input ->
                        pricePerLiterText = input
                        val p = input.toDoubleOrNull()
                        val l = litersText.toDoubleOrNull()
                        val total = totalCostText.toDoubleOrNull()
                        if (p != null && l != null && l > 0) {
                            totalCostText = String.format(Locale.US, "%.2f", p * l)
                        } else if (p != null && p > 0 && total != null && total > 0) {
                            litersText = String.format(Locale.US, "%.2f", total / p)
                        }
                    },
                    label = { Text("Цена за литр (₽/л)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = litersText,
                        onValueChange = { input ->
                            litersText = input
                            val l = input.toDoubleOrNull()
                            val p = pricePerLiterText.toDoubleOrNull()
                            if (l != null && p != null && p > 0) {
                                totalCostText = String.format(Locale.US, "%.2f", l * p)
                            } else if (l != null && l > 0 && totalCostText.toDoubleOrNull() != null) {
                                val t = totalCostText.toDouble()
                                pricePerLiterText = String.format(Locale.US, "%.2f", t / l)
                            }
                        },
                        label = { Text("Литры *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = totalCostText,
                        onValueChange = { input ->
                            totalCostText = input
                            val t = input.toDoubleOrNull()
                            val p = pricePerLiterText.toDoubleOrNull()
                            if (t != null && p != null && p > 0) {
                                litersText = String.format(Locale.US, "%.2f", t / p)
                            } else if (t != null && litersText.toDoubleOrNull() != null && litersText.toDouble() > 0) {
                                val l = litersText.toDouble()
                                pricePerLiterText = String.format(Locale.US, "%.2f", t / l)
                            }
                        },
                        label = { Text("Сумма (₽) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                OutlinedTextField(
                    value = stationName,
                    onValueChange = { stationName = it },
                    label = { Text("АЗС (необязательно)") },
                    placeholder = { Text("Лукойл, Роснефть, Газпром...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it },
                    label = { Text("Одометр (необязательно)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val liters = litersText.toDoubleOrNull()
                    val totalCost = totalCostText.toDoubleOrNull()
                    val price = pricePerLiterText.toDoubleOrNull() ?: if (liters != null && liters > 0 && totalCost != null) totalCost / liters else 0.0
                    if (vehiclePlate.isNotBlank() && liters != null && totalCost != null) {
                        onSave(
                            FuelRecord(
                                id = record?.id ?: 0L,
                                vehiclePlate = vehiclePlate.trim(),
                                date = record?.date ?: System.currentTimeMillis(),
                                liters = liters,
                                pricePerLiter = price,
                                totalCost = totalCost,
                                stationName = stationName.trim(),
                                odometerKm = odometerText.toDoubleOrNull()
                            )
                        )
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
