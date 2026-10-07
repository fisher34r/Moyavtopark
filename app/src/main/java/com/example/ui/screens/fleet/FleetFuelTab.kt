package com.example.ui.screens.fleet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
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
import com.example.ui.viewmodel.FleetViewModel
import com.example.util.Formatters
import kotlinx.coroutines.launch

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
    
    Box(modifier = modifier.fillMaxSize()) {
        if (fuelRecords.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.LocalGasStation, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Нет записей о заправках", color = Color.Gray, fontSize = 16.sp)
                Text("Нажмите + чтобы добавить чек", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
            ) {
                items(fuelRecords, key = { it.id }) { record ->
                    FuelRecordCard(
                        record = record,
                        onClick = { selectedRecord = record; showAddDialog = true },
                        onDeleteClick = { showDeleteConfirm = record }
                    )
                }
            }
        }
        
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
    
    if (showAddDialog) {
        AddFuelRecordDialog(
            record = selectedRecord,
            vehicles = vehicles.map { it.plateNumber },
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
}

@Composable
fun FuelRecordCard(
    record: FuelRecord,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Text(
                    text = "${record.liters} л • ${Formatters.formatMoney(record.totalCost)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "ТС: ${record.vehiclePlate}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Formatters.formatDate(record.date) + if (record.stationName.isNotBlank()) " • ${record.stationName}" else "",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFuelRecordDialog(
    record: FuelRecord?,
    vehicles: List<String>,
    onDismiss: () -> Unit,
    onSave: (FuelRecord) -> Unit
) {
    var vehiclePlate by remember { mutableStateOf(record?.vehiclePlate ?: "") }
    var litersText by remember { mutableStateOf(record?.liters?.toString() ?: "") }
    var totalCostText by remember { mutableStateOf(record?.totalCost?.toString() ?: "") }
    var stationName by remember { mutableStateOf(record?.stationName ?: "") }
    var odometerText by remember { mutableStateOf(record?.odometerKm?.let { if(it>0) it.toString() else "" } ?: "") }
    
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
    
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
                        vehicles.forEach { plate ->
                            DropdownMenuItem(
                                text = { Text(plate) },
                                onClick = {
                                    vehiclePlate = plate
                                    vehicleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = litersText,
                        onValueChange = { litersText = it },
                        label = { Text("Литры *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = totalCostText,
                        onValueChange = { totalCostText = it },
                        label = { Text("Сумма (₽) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                OutlinedTextField(
                    value = stationName,
                    onValueChange = { stationName = it },
                    label = { Text("АЗС (необязательно)") },
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
                    if (vehiclePlate.isNotBlank() && liters != null && totalCost != null) {
                        onSave(
                            FuelRecord(
                                id = record?.id ?: 0L,
                                vehiclePlate = vehiclePlate.trim(),
                                date = record?.date ?: System.currentTimeMillis(),
                                liters = liters,
                                pricePerLiter = totalCost / liters,
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
