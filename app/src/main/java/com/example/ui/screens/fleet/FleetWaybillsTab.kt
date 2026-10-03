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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.data.fleet.Driver
import com.example.data.fleet.Vehicle
import com.example.data.fleet.Waybill
import com.example.data.fleet.WaybillStatus
import com.example.ui.viewmodel.FleetViewModel
import com.example.util.Formatters

@Composable
fun FleetWaybillsTab(
    viewModel: FleetViewModel,
    modifier: Modifier = Modifier
) {
    val waybills by viewModel.waybills.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val drivers by viewModel.drivers.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var waybillToClose by remember { mutableStateOf<Waybill?>(null) }
    var waybillToDelete by remember { mutableStateOf<Waybill?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (waybills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Путевые листы отсутствуют",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(waybills, key = { it.id }) { waybill ->
                    WaybillCard(
                        waybill = waybill,
                        onCloseWaybill = { waybillToClose = waybill },
                        onDelete = { waybillToDelete = waybill }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_waybill_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Выпустить путевой лист")
        }
    }

    if (showCreateDialog) {
        WaybillCreateDialog(
            vehicles = vehicles,
            drivers = drivers,
            onDismiss = { showCreateDialog = false },
            onSave = {
                viewModel.saveWaybill(it)
                showCreateDialog = false
            }
        )
    }

    waybillToClose?.let { wb ->
        WaybillCloseDialog(
            waybill = wb,
            onDismiss = { waybillToClose = null },
            onConfirmClose = { endOdo, endFuel ->
                viewModel.closeWaybill(wb, endOdo, endFuel)
                waybillToClose = null
            }
        )
    }

    waybillToDelete?.let { wb ->
        AlertDialog(
            onDismissRequest = { waybillToDelete = null },
            title = { Text("Удалить путевой лист?") },
            text = { Text("Удалить путевой лист №${wb.number} от ${Formatters.formatDate(wb.date)}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWaybill(wb)
                        waybillToDelete = null
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { waybillToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun WaybillCard(
    waybill: Waybill,
    onCloseWaybill: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIssued = waybill.status == WaybillStatus.ISSUED
    val statusColor = if (isIssued) Color(0xFF0288D1) else Color(0xFF2E7D32)

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
                            .background(statusColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = waybill.number,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = Formatters.formatDate(waybill.date),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
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
                        text = waybill.status.label,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vehicle and Driver
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${waybill.vehiclePlate} ${if (waybill.trailerPlate.isNotBlank()) "+ [${waybill.trailerPlate}]" else ""}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = waybill.driverName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (waybill.routeDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Маршрут: ${waybill.routeDescription}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Odometer & Fuel metrics box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Одометр: выезд ${Formatters.formatDistance(waybill.startOdometerKm)}" +
                                    (if (waybill.endOdometerKm != null) " → возврат ${Formatters.formatDistance(waybill.endOdometerKm)}" else ""),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (waybill.totalDistanceKm > 0) {
                            Text(
                                text = "Итого: ${Formatters.formatDistance(waybill.totalDistanceKm)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Бак: выезд ${waybill.startFuelLiters.toInt()} л" +
                                    (if (waybill.fuelAddedLiters > 0) " + зап. ${waybill.fuelAddedLiters.toInt()} л" else "") +
                                    (if (waybill.endFuelLiters != null) " → ост. ${waybill.endFuelLiters.toInt()} л" else ""),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (waybill.totalFuelConsumedLiters > 0) {
                            Text(
                                text = "Расход: ${waybill.totalFuelConsumedLiters.toInt()} л",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inspection checks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Медосмотр и техосмотр пройдены", fontSize = 10.sp, color = Color(0xFF2E7D32))
                }

                Row {
                    if (isIssued) {
                        Button(
                            onClick = onCloseWaybill,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Закрыть рейс", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaybillCreateDialog(
    vehicles: List<Vehicle> = emptyList(),
    drivers: List<Driver> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Waybill) -> Unit
) {
    val initialVehicle = vehicles.firstOrNull()
    val initialDriver = drivers.firstOrNull()
    var number by remember { mutableStateOf("ПЛ-${(100..999).random()}") }
    var vehiclePlate by remember { mutableStateOf(initialVehicle?.plateNumber ?: "А 742 КХ 123") }
    var trailerPlate by remember { mutableStateOf("ЕК 4120 23") }
    var driverName by remember {
        mutableStateOf(initialVehicle?.assignedDriverName?.takeIf { it.isNotBlank() } ?: initialDriver?.fullName ?: "Иванов Сергей Михайлович")
    }
    var startOdoText by remember { mutableStateOf(initialVehicle?.currentOdometerKm?.toInt()?.toString() ?: "148500") }
    var startFuelText by remember { mutableStateOf(initialVehicle?.currentFuelLiters?.toInt()?.toString() ?: "450") }
    var fuelAddedText by remember { mutableStateOf("0") }
    var route by remember { mutableStateOf("ст. Каневская → порт Новороссийск (НЗТ)") }
    var medCheck by remember { mutableStateOf(true) }
    var techCheck by remember { mutableStateOf(true) }

    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
    var driverDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выпуск на линию (Путевой лист)") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Номер ПЛ *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = vehicleDropdownExpanded,
                        onExpandedChange = { vehicleDropdownExpanded = !vehicleDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = vehiclePlate,
                            onValueChange = { vehiclePlate = it },
                            label = { Text("Тягач *") },
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
                                                Text("${v.model} • ${v.assignedDriverName.ifBlank { "Без водителя" }}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        },
                                        onClick = {
                                            vehiclePlate = v.plateNumber
                                            if (v.assignedDriverName.isNotBlank()) {
                                                driverName = v.assignedDriverName
                                            }
                                            if (v.currentOdometerKm > 0) {
                                                startOdoText = v.currentOdometerKm.toInt().toString()
                                            }
                                            if (v.currentFuelLiters > 0) {
                                                startFuelText = v.currentFuelLiters.toInt().toString()
                                            }
                                            vehicleDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = trailerPlate,
                        onValueChange = { trailerPlate = it },
                        label = { Text("Полуприцеп") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = driverDropdownExpanded,
                        onExpandedChange = { driverDropdownExpanded = !driverDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = driverName,
                            onValueChange = { driverName = it },
                            label = { Text("Водитель") },
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
                                    text = { Text("Нет водителей в автопарке") },
                                    onClick = { driverDropdownExpanded = false }
                                )
                            } else {
                                drivers.forEach { d ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(d.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                if (d.assignedVehiclePlate.isNotBlank()) {
                                                    Text("Закреплен: ${d.assignedVehiclePlate}", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                        },
                                        onClick = {
                                            driverName = d.fullName
                                            if (d.assignedVehiclePlate.isNotBlank() && vehiclePlate.isBlank()) {
                                                vehiclePlate = d.assignedVehiclePlate
                                            }
                                            driverDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startOdoText,
                        onValueChange = { startOdoText = it },
                        label = { Text("Одометр выезда") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = startFuelText,
                        onValueChange = { startFuelText = it },
                        label = { Text("Топливо в баке (л)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = route,
                    onValueChange = { route = it },
                    label = { Text("Маршрут задания") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = medCheck, onCheckedChange = { medCheck = it })
                    Text("Предрейсовый медосмотр пройден", fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = techCheck, onCheckedChange = { techCheck = it })
                    Text("Предрейсовый техосмотр ТС пройден", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (number.isNotBlank() && vehiclePlate.isNotBlank()) {
                        val wb = Waybill(
                            number = number.trim(),
                            vehiclePlate = vehiclePlate.trim(),
                            trailerPlate = trailerPlate.trim(),
                            driverName = driverName.trim(),
                            startOdometerKm = startOdoText.toDoubleOrNull() ?: 0.0,
                            startFuelLiters = startFuelText.toDoubleOrNull() ?: 0.0,
                            fuelAddedLiters = fuelAddedText.toDoubleOrNull() ?: 0.0,
                            routeDescription = route.trim(),
                            medicalCheckPassed = medCheck,
                            technicalCheckPassed = techCheck,
                            status = WaybillStatus.ISSUED
                        )
                        onSave(wb)
                    }
                }
            ) {
                Text("Выпустить на линию")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun WaybillCloseDialog(
    waybill: Waybill,
    onDismiss: () -> Unit,
    onConfirmClose: (Double, Double) -> Unit
) {
    var endOdoText by remember {
        mutableStateOf((waybill.startOdometerKm + 350.0).toInt().toString())
    }
    var endFuelText by remember {
        mutableStateOf(((waybill.startFuelLiters + waybill.fuelAddedLiters) - 130.0).coerceAtLeast(0.0).toInt().toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Закрытие путевого листа №${waybill.number}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = "Зафиксируйте показания приборов при возвращении автомобиля в гараж:",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = endOdoText,
                    onValueChange = { endOdoText = it },
                    label = { Text("Одометр при возвращении (км)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = endFuelText,
                    onValueChange = { endFuelText = it },
                    label = { Text("Остаток топлива в баке (литров)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val endOdo = endOdoText.toDoubleOrNull() ?: waybill.startOdometerKm
                    val endFuel = endFuelText.toDoubleOrNull() ?: 0.0
                    onConfirmClose(endOdo, endFuel)
                }
            ) {
                Text("Закрыть рейс")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
