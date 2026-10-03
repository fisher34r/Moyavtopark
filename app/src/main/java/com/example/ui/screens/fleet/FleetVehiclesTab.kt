package com.example.ui.screens.fleet

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import com.example.data.fleet.Driver
import com.example.data.fleet.Vehicle
import com.example.data.fleet.VehicleStatus
import com.example.data.fleet.VehicleType
import com.example.ui.viewmodel.FleetViewModel
import com.example.util.Formatters
import java.util.Locale

@Composable
fun FleetVehiclesTab(
    viewModel: FleetViewModel,
    onNavigateToTrips: ((plateNumber: String) -> Unit)? = null,
    onNavigateToReports: ((plateNumber: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val vehicles by viewModel.filteredVehicles.collectAsStateWithLifecycle()
    val searchQuery by viewModel.vehicleSearchQuery.collectAsStateWithLifecycle()
    val allDrivers by viewModel.drivers.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var vehicleToEdit by remember { mutableStateOf<Vehicle?>(null) }
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search and Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.vehicleSearchQuery.value = it },
                    label = { Text("Поиск по госномеру, марке, VIN или водителю") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.vehicleSearchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vehicle_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Vehicles List
            if (vehicles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Транспортные средства не найдены",
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
                    items(vehicles, key = { it.id }) { vehicle ->
                        VehicleCard(
                            vehicle = vehicle,
                            onEdit = {
                                vehicleToEdit = vehicle
                                showAddEditDialog = true
                            },
                            onDelete = { vehicleToDelete = vehicle },
                            onNavigateToTrips = onNavigateToTrips?.let { cb -> { cb(vehicle.plateNumber) } },
                            onNavigateToReports = onNavigateToReports?.let { cb -> { cb(vehicle.plateNumber) } }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Add Vehicle FAB
        FloatingActionButton(
            onClick = {
                vehicleToEdit = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_vehicle_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить ТС")
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        VehicleAddEditDialog(
            vehicle = vehicleToEdit,
            drivers = allDrivers,
            onDismiss = { showAddEditDialog = false },
            onSave = {
                viewModel.saveVehicle(it)
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    vehicleToDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить ТС?") },
            text = { Text("Вы действительно хотите удалить ${v.plateNumber} (${v.model}) из автопарка?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVehicle(v)
                        vehicleToDelete = null
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToTrips: (() -> Unit)? = null,
    onNavigateToReports: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusColor = when (vehicle.status) {
        VehicleStatus.ACTIVE -> Color(0xFF2E7D32)
        VehicleStatus.AVAILABLE -> Color(0xFF0288D1)
        VehicleStatus.SERVICE -> Color(0xFFED6C02)
        VehicleStatus.DECOMMISSIONED -> Color(0xFF757575)
    }

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
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (vehicle.type) {
                                VehicleType.TRACTOR -> Icons.Default.LocalShipping
                                VehicleType.TRAILER -> Icons.Default.DirectionsCar
                                VehicleType.TRUCK -> Icons.Default.LocalShipping
                                VehicleType.SPECIAL -> Icons.Default.Build
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = vehicle.plateNumber,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${vehicle.model} (${vehicle.year} г.)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = vehicle.status.label,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Odometer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${Formatters.formatDistance(vehicle.currentOdometerKm)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Fuel
                if (vehicle.fuelTankCapacityLiters > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Бак: ${vehicle.currentFuelLiters.toInt()}/${vehicle.fuelTankCapacityLiters.toInt()} л",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Type
                Text(
                    text = vehicle.type.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (vehicle.assignedDriverName.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Водитель: ${vehicle.assignedDriverName}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (vehicle.vin.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "VIN: ${vehicle.vin}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Actions
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onNavigateToTrips != null) {
                        OutlinedButton(
                            onClick = onNavigateToTrips,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Рейсы", fontSize = 11.sp)
                        }
                    }
                    if (onNavigateToReports != null) {
                        Button(
                            onClick = onNavigateToReports,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF155383),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Реестр / Отчет", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleAddEditDialog(
    vehicle: Vehicle?,
    drivers: List<Driver> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit
) {
    var plateNumber by remember { mutableStateOf(vehicle?.plateNumber ?: "") }
    var model by remember { mutableStateOf(vehicle?.model ?: "") }
    var type by remember { mutableStateOf(vehicle?.type ?: VehicleType.TRACTOR) }
    var status by remember { mutableStateOf(vehicle?.status ?: VehicleStatus.AVAILABLE) }
    var vin by remember { mutableStateOf(vehicle?.vin ?: "") }
    var yearText by remember { mutableStateOf(vehicle?.year?.toString() ?: "2022") }
    var odometerText by remember { mutableStateOf(vehicle?.currentOdometerKm?.toInt()?.toString() ?: "150000") }
    var fuelTankText by remember { mutableStateOf(vehicle?.fuelTankCapacityLiters?.toInt()?.toString() ?: "600") }
    var currentFuelText by remember { mutableStateOf(vehicle?.currentFuelLiters?.toInt()?.toString() ?: "400") }
    var driverName by remember { mutableStateOf(vehicle?.assignedDriverName ?: "") }
    var driverDropdownExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(vehicle?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (vehicle == null) "Добавить ТС" else "Редактировать ТС") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = plateNumber,
                    onValueChange = { plateNumber = it },
                    label = { Text("Госномер *") },
                    placeholder = { Text("А 742 КХ 123") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Марка и модель *") },
                    placeholder = { Text("KAMAZ-54901 / Scania / Тонар") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Type selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VehicleType.values().forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Status selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VehicleStatus.values().forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s.label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it },
                        label = { Text("Год") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = odometerText,
                        onValueChange = { odometerText = it },
                        label = { Text("Одометр (км)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = vin,
                    onValueChange = { vin = it },
                    label = { Text("VIN номер") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = driverDropdownExpanded,
                    onExpandedChange = { driverDropdownExpanded = !driverDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = driverName,
                        onValueChange = { driverName = it },
                        label = { Text("Закрепленный водитель") },
                        placeholder = { Text("Выберите из списка водителей...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = driverDropdownExpanded,
                        onDismissRequest = { driverDropdownExpanded = false }
                    ) {
                        if (drivers.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Нет водителей в базе автопарка") },
                                onClick = { driverDropdownExpanded = false }
                            )
                        } else {
                            drivers.forEach { d ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(d.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(
                                                text = "Ставка: ${String.format(Locale.US, "%.0f", d.salaryPercent)}%" +
                                                        if (d.assignedVehiclePlate.isNotBlank()) " • Закреплен за: ${d.assignedVehiclePlate}" else "",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    onClick = {
                                        driverName = d.fullName
                                        driverDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (plateNumber.isNotBlank()) {
                        val v = Vehicle(
                            id = vehicle?.id ?: 0L,
                            plateNumber = plateNumber.trim(),
                            model = model.trim().ifBlank { "Грузовой" },
                            type = type,
                            status = status,
                            vin = vin.trim(),
                            year = yearText.toIntOrNull() ?: 2021,
                            currentOdometerKm = odometerText.toDoubleOrNull() ?: 0.0,
                            fuelTankCapacityLiters = fuelTankText.toDoubleOrNull() ?: 600.0,
                            currentFuelLiters = currentFuelText.toDoubleOrNull() ?: 400.0,
                            assignedDriverName = driverName.trim(),
                            notes = notes.trim()
                        )
                        onSave(v)
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
