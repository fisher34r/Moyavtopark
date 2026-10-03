package com.example.ui.screens.fleet

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.fleet.ServiceRecord
import com.example.data.fleet.ServiceType
import com.example.data.fleet.Vehicle
import com.example.ui.viewmodel.FleetViewModel
import com.example.util.Formatters

@Composable
fun FleetServiceTab(
    viewModel: FleetViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.filteredServiceRecords.collectAsStateWithLifecycle()
    val selectedType by viewModel.serviceTypeFilter.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<ServiceRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<ServiceRecord?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Type filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { viewModel.serviceTypeFilter.value = null },
                    label = { Text("Все работы (${records.size})", fontSize = 12.sp) }
                )
                ServiceType.values().forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            viewModel.serviceTypeFilter.value = if (selectedType == type) null else type
                        },
                        label = { Text(type.label, fontSize = 12.sp) }
                    )
                }
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Записи ТО и ремонтов отсутствуют",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        ServiceRecordCard(
                            record = record,
                            onEdit = {
                                recordToEdit = record
                                showAddDialog = true
                            },
                            onDelete = { recordToDelete = record }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                recordToEdit = null
                showAddDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_service_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить запись ТО")
        }
    }

    if (showAddDialog) {
        ServiceRecordAddEditDialog(
            record = recordToEdit,
            vehicles = vehicles,
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.saveServiceRecord(it)
                showAddDialog = false
            }
        )
    }

    recordToDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Удалить запись ТО?") },
            text = { Text("Удалить запись по автомобилю ${r.vehiclePlate} (${r.orderNumber})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteServiceRecord(r)
                        recordToDelete = null
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ServiceRecordCard(
    record: ServiceRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = record.vehiclePlate,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${record.serviceType.label} • ${Formatters.formatDate(record.date)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Cost Badge
                Text(
                    text = Formatters.formatMoney(record.cost),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Work Description
            Text(
                text = record.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (record.odometerKm > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("На ${Formatters.formatDistance(record.odometerKm)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (record.orderNumber.isNotBlank()) {
                    Text("№ ${record.orderNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }

                if (record.serviceStation.isNotBlank()) {
                    Text(record.serviceStation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (record.nextPlannedKm != null && record.nextPlannedKm > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "След. ТО запланировано на ${Formatters.formatDistance(record.nextPlannedKm)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Actions
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRecordAddEditDialog(
    record: ServiceRecord?,
    vehicles: List<Vehicle> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (ServiceRecord) -> Unit
) {
    var vehiclePlate by remember { mutableStateOf(record?.vehiclePlate ?: vehicles.firstOrNull()?.plateNumber ?: "") }
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
    var serviceType by remember { mutableStateOf(record?.serviceType ?: ServiceType.TO_1) }
    var odometerText by remember { mutableStateOf(record?.odometerKm?.toInt()?.toString() ?: "150000") }
    var costText by remember { mutableStateOf(record?.cost?.toInt()?.toString() ?: "25000") }
    var orderNumber by remember { mutableStateOf(record?.orderNumber ?: "") }
    var serviceStation by remember { mutableStateOf(record?.serviceStation ?: "") }
    var description by remember { mutableStateOf(record?.description ?: "") }
    var nextKmText by remember { mutableStateOf(record?.nextPlannedKm?.toInt()?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (record == null) "Запись сервиса / ТО" else "Редактировать запись") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = vehicleDropdownExpanded,
                    onExpandedChange = { vehicleDropdownExpanded = !vehicleDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = vehiclePlate,
                        onValueChange = { vehiclePlate = it },
                        label = { Text("Госномер ТС *") },
                        placeholder = { Text("Выберите из автопарка...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = vehicleDropdownExpanded,
                        onDismissRequest = { vehicleDropdownExpanded = false }
                    ) {
                        if (vehicles.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Нет ТС в автопарке") },
                                onClick = { vehicleDropdownExpanded = false }
                            )
                        } else {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(v.plateNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${v.model} • Одометр: ${v.currentOdometerKm.toInt()} км", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        vehiclePlate = v.plateNumber
                                        if (v.currentOdometerKm > 0) {
                                            odometerText = v.currentOdometerKm.toInt().toString()
                                        }
                                        vehicleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Service type chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServiceType.values().forEach { st ->
                        FilterChip(
                            selected = serviceType == st,
                            onClick = { serviceType = st },
                            label = { Text(st.label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Стоимость (₽)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = odometerText,
                        onValueChange = { odometerText = it },
                        label = { Text("Пробег (км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Перечень выполненных работ *") },
                    placeholder = { Text("Замена масла ДВС, фильтров, колодок...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = orderNumber,
                        onValueChange = { orderNumber = it },
                        label = { Text("Заказ-наряд №") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = serviceStation,
                        onValueChange = { serviceStation = it },
                        label = { Text("СТО / Сервис") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = nextKmText,
                    onValueChange = { nextKmText = it },
                    label = { Text("Следующее ТО на пробеге (км)") },
                    placeholder = { Text("Например: 175000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (vehiclePlate.isNotBlank() && description.isNotBlank()) {
                        val r = ServiceRecord(
                            id = record?.id ?: 0L,
                            vehiclePlate = vehiclePlate.trim(),
                            serviceType = serviceType,
                            odometerKm = odometerText.toDoubleOrNull() ?: 0.0,
                            cost = costText.toDoubleOrNull() ?: 0.0,
                            description = description.trim(),
                            orderNumber = orderNumber.trim(),
                            serviceStation = serviceStation.trim(),
                            nextPlannedKm = nextKmText.toDoubleOrNull()
                        )
                        onSave(r)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
