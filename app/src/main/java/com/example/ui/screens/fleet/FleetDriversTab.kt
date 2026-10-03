package com.example.ui.screens.fleet

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.fleet.Driver
import com.example.data.fleet.DriverStatus
import com.example.data.fleet.Vehicle
import com.example.data.model.Trip
import com.example.ui.viewmodel.FleetViewModel
import com.example.util.Formatters
import java.util.Locale

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.runtime.mutableStateListOf

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FleetDriversTab(
    viewModel: FleetViewModel,
    onNavigateToTrips: ((driverName: String) -> Unit)? = null,
    onNavigateToReports: ((driverName: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val drivers by viewModel.filteredDrivers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.driverSearchQuery.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.driverStatusFilter.collectAsStateWithLifecycle()
    val allVehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val trips by viewModel.trips.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var driverToEdit by remember { mutableStateOf<Driver?>(null) }
    var driverToDelete by remember { mutableStateOf<Driver?>(null) }
    var driverForStatement by remember { mutableStateOf<Driver?>(null) }

    // Пакетное редактирование водителей
    var isBatchMode by remember { mutableStateOf(false) }
    val selectedDriverIds = remember { mutableStateListOf<Long>() }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search, Batch Action Bar, and Status Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isBatchMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    isBatchMode = false
                                    selectedDriverIds.clear()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Выйти из режима выбора")
                                }
                                Text(
                                    text = "Выбрано: ${selectedDriverIds.size}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Выбрать все / Снять
                                IconButton(onClick = {
                                    if (selectedDriverIds.size == drivers.size) {
                                        selectedDriverIds.clear()
                                    } else {
                                        selectedDriverIds.clear()
                                        selectedDriverIds.addAll(drivers.map { it.id })
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.SelectAll,
                                        contentDescription = "Выбрать все",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                // Редактировать выбранные
                                IconButton(
                                    onClick = { showBatchEditDialog = true },
                                    enabled = selectedDriverIds.isNotEmpty()
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Редактировать выбранные",
                                        tint = if (selectedDriverIds.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }

                                // Удалить выбранные
                                IconButton(
                                    onClick = { showBatchDeleteConfirm = true },
                                    enabled = selectedDriverIds.isNotEmpty()
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить выбранные",
                                        tint = if (selectedDriverIds.isNotEmpty()) MaterialTheme.colorScheme.error else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.driverSearchQuery.value = it },
                            label = { Text("Поиск по ФИО, телефону или ВУ") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.driverSearchQuery.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("driver_search_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                isBatchMode = true
                                selectedDriverIds.clear()
                            }
                        ) {
                            Icon(
                                Icons.Default.Checklist,
                                contentDescription = "Пакетное редактирование",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedStatus == null,
                            onClick = { viewModel.driverStatusFilter.value = null },
                            label = { Text("Все водители (${drivers.size})", fontSize = 12.sp) }
                        )
                        DriverStatus.values().forEach { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = {
                                    viewModel.driverStatusFilter.value = if (selectedStatus == status) null else status
                                },
                                label = { Text(status.label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Drivers list
            if (drivers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Водители не найдены",
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
                    items(drivers, key = { it.id }) { driver ->
                        val isSelected = selectedDriverIds.contains(driver.id)
                        DriverCard(
                            driver = driver,
                            trips = trips,
                            isSelectionMode = isBatchMode,
                            isSelected = isSelected,
                            onToggleSelection = {
                                if (isSelected) selectedDriverIds.remove(driver.id)
                                else selectedDriverIds.add(driver.id)
                            },
                            onLongClick = {
                                if (!isBatchMode) {
                                    isBatchMode = true
                                    selectedDriverIds.add(driver.id)
                                }
                            },
                            onStatusChange = { newStatus ->
                                viewModel.updateDriverStatus(driver, newStatus)
                            },
                            onViewStatement = {
                                driverForStatement = driver
                            },
                            onEdit = {
                                driverToEdit = driver
                                showAddEditDialog = true
                            },
                            onDelete = { driverToDelete = driver },
                            onNavigateToTrips = onNavigateToTrips?.let { cb -> { cb(driver.fullName) } },
                            onNavigateToReports = onNavigateToReports?.let { cb -> { cb(driver.fullName) } }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Add Driver FAB
        if (!isBatchMode) {
            FloatingActionButton(
                onClick = {
                    driverToEdit = null
                    showAddEditDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_driver_fab"),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить водителя")
            }
        }
    }

    if (showAddEditDialog) {
        DriverAddEditDialog(
            driver = driverToEdit,
            vehicles = allVehicles,
            onDismiss = { showAddEditDialog = false },
            onSave = {
                viewModel.saveDriver(it)
                showAddEditDialog = false
            }
        )
    }

    driverForStatement?.let { d ->
        DriverStatementDialog(
            driver = d,
            trips = trips,
            onDismiss = { driverForStatement = null }
        )
    }

    driverToDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { driverToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить водителя?") },
            text = { Text("Вы действительно хотите удалить ${d.fullName} из базы?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDriver(d)
                        driverToDelete = null
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { driverToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Batch Delete Confirmation
    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить выбранных водителей?") },
            text = { Text("Вы действительно хотите удалить выбранных водителей (${selectedDriverIds.size} чел.) из базы? Это действие нельзя отменить.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.batchDeleteDrivers(selectedDriverIds.toSet())
                        selectedDriverIds.clear()
                        isBatchMode = false
                        showBatchDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить (${selectedDriverIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Batch Edit Dialog
    if (showBatchEditDialog) {
        BatchEditDriversDialog(
            selectedCount = selectedDriverIds.size,
            onDismiss = { showBatchEditDialog = false },
            onApply = { newStatus, newSalaryPercent, newShiftSchedule ->
                viewModel.batchUpdateDrivers(
                    driverIds = selectedDriverIds.toSet(),
                    newStatus = newStatus,
                    newSalaryPercent = newSalaryPercent,
                    newShiftSchedule = newShiftSchedule
                )
                selectedDriverIds.clear()
                isBatchMode = false
                showBatchEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DriverCard(
    driver: Driver,
    trips: List<Trip> = emptyList(),
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onStatusChange: (DriverStatus) -> Unit,
    onViewStatement: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToTrips: (() -> Unit)? = null,
    onNavigateToReports: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val statusColor = when (driver.status) {
        DriverStatus.AVAILABLE -> Color(0xFF2E7D32)
        DriverStatus.ON_TRIP -> Color(0xFF0288D1)
        DriverStatus.REST -> Color(0xFF757575)
        DriverStatus.VACATION -> Color(0xFFED6C02)
        DriverStatus.SICK -> Color(0xFFC62828)
    }

    val driverTrips = remember(trips, driver.fullName) {
        trips.filter { t ->
            t.driverName.isNotBlank() && (
                t.driverName.equals(driver.fullName, ignoreCase = true) ||
                driver.fullName.contains(t.driverName, ignoreCase = true) ||
                t.driverName.contains(driver.fullName, ignoreCase = true)
            )
        }
    }
    val driverTotalRevenue = driverTrips.sumOf { it.totalPrice }
    val driverTotalSalary = driverTrips.sumOf { it.effectiveDriverSalary }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelection()
                    else onEdit()
                },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection() }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = driver.fullName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Стаж: ${driver.experienceYears} лет • ВУ: ${driver.licenseNumber}",
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
                        text = driver.status.label,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges: Salary %, Categories, DOPOG, SKZI, Schedule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Salary % Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "💰 ЗП: ${String.format(Locale.US, "%.1f", driver.salaryPercent)}% от фрахта",
                        color = Color(0xFF1B5E20),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Category badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Категории: ${driver.categories}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                if (driver.hasSkziCard) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Карта СКЗИ", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (driver.hasDopog) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFF3E0))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("ДОПОГ", color = Color(0xFFE65100), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(driver.shiftSchedule, fontSize = 11.sp)
                }
            }

            if (driver.assignedVehiclePlate.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Закреплен: ${driver.assignedVehiclePlate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Driver Salary & Earnings Stats Card
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Начисления ЗП (${String.format(Locale.US, "%.1f", driver.salaryPercent)}% от фрахта)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (driverTrips.isNotEmpty()) {
                            TextButton(
                                onClick = onViewStatement,
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Ведомость", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Рейсов", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${driverTrips.size}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column {
                            Text("Выручка (фрахт)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Formatters.formatMoney(driverTotalRevenue),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Начислено ЗП", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Formatters.formatMoney(driverTotalSalary),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2E7D32)
                                )
                            )
                        }
                    }
                }
            }

            // Quick Status Changer & Actions
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Call button if phone exists
                if (driver.phone.isNotBlank()) {
                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${driver.phone}"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(driver.phone, fontSize = 11.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

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
                            Text("Ведомость / Реестр", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
fun DriverAddEditDialog(
    driver: Driver?,
    vehicles: List<Vehicle> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Driver) -> Unit
) {
    var fullName by remember { mutableStateOf(driver?.fullName ?: "") }
    var phone by remember { mutableStateOf(driver?.phone ?: "") }
    var licenseNumber by remember { mutableStateOf(driver?.licenseNumber ?: "") }
    var categories by remember { mutableStateOf(driver?.categories ?: "B, C, CE") }
    var status by remember { mutableStateOf(driver?.status ?: DriverStatus.AVAILABLE) }
    var shiftSchedule by remember { mutableStateOf(driver?.shiftSchedule ?: "15/15 вахта") }
    var assignedVehicle by remember { mutableStateOf(driver?.assignedVehiclePlate ?: "") }
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
    var salaryPercentText by remember {
        mutableStateOf(driver?.salaryPercent?.let { String.format(Locale.US, "%.1f", it) } ?: "20.0")
    }
    var hasSkzi by remember { mutableStateOf(driver?.hasSkziCard ?: true) }
    var hasDopog by remember { mutableStateOf(driver?.hasDopog ?: false) }
    var expYearsText by remember { mutableStateOf(driver?.experienceYears?.toString() ?: "8") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (driver == null) "Добавить водителя" else "Редактировать водителя") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("ФИО водителя *") },
                    placeholder = { Text("Иванов Сергей Михайлович") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Телефон") },
                        placeholder = { Text("+7 918...") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = expYearsText,
                        onValueChange = { expYearsText = it },
                        label = { Text("Стаж (лет)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Salary Percent Field
                OutlinedTextField(
                    value = salaryPercentText,
                    onValueChange = { salaryPercentText = it },
                    label = { Text("Процент зарплаты от фрахта (%) *") },
                    placeholder = { Text("20.0") },
                    supportingText = { Text("Ставка водителя от суммы фрахта (обычно 18–25%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = licenseNumber,
                        onValueChange = { licenseNumber = it },
                        label = { Text("Номер ВУ") },
                        placeholder = { Text("23 12 345678") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = categories,
                        onValueChange = { categories = it },
                        label = { Text("Категории") },
                        placeholder = { Text("C, CE") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = shiftSchedule,
                    onValueChange = { shiftSchedule = it },
                    label = { Text("График работы / смен") },
                    placeholder = { Text("15/15 вахта, 20/10, 5/2") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = vehicleDropdownExpanded,
                    onExpandedChange = { vehicleDropdownExpanded = !vehicleDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = assignedVehicle,
                        onValueChange = { assignedVehicle = it },
                        label = { Text("Закрепленное ТС (госномер)") },
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
                                            Text(
                                                text = "${v.model} (${v.type.label})" +
                                                        if (v.assignedDriverName.isNotBlank()) " • Водитель: ${v.assignedDriverName}" else "",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    onClick = {
                                        assignedVehicle = v.plateNumber
                                        vehicleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = hasSkzi, onCheckedChange = { hasSkzi = it })
                    Text("Карта тахографа СКЗИ", fontSize = 12.sp)

                    Spacer(modifier = Modifier.width(8.dp))

                    Checkbox(checked = hasDopog, onCheckedChange = { hasDopog = it })
                    Text("ДОПОГ", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        val parsedPercent = salaryPercentText.replace(',', '.').toDoubleOrNull() ?: 20.0
                        val d = Driver(
                            id = driver?.id ?: 0L,
                            fullName = fullName.trim(),
                            phone = phone.trim(),
                            licenseNumber = licenseNumber.trim(),
                            categories = categories.trim(),
                            hasDopog = hasDopog,
                            hasSkziCard = hasSkzi,
                            status = status,
                            shiftSchedule = shiftSchedule.trim(),
                            assignedVehiclePlate = assignedVehicle.trim(),
                            experienceYears = expYearsText.toIntOrNull() ?: 5,
                            salaryPercent = parsedPercent
                        )
                        onSave(d)
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

@Composable
fun DriverStatementDialog(
    driver: Driver,
    trips: List<Trip>,
    onDismiss: () -> Unit
) {
    val driverTrips = remember(trips, driver.fullName) {
        trips.filter { t ->
            t.driverName.isNotBlank() && (
                t.driverName.equals(driver.fullName, ignoreCase = true) ||
                driver.fullName.contains(t.driverName, ignoreCase = true) ||
                t.driverName.contains(driver.fullName, ignoreCase = true)
            )
        }.sortedByDescending { it.loadingDate }
    }
    val totalRevenue = driverTrips.sumOf { it.totalPrice }
    val totalSalary = driverTrips.sumOf { it.effectiveDriverSalary }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Ведомость начислений ЗП", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "${driver.fullName} • ставка ${String.format(Locale.US, "%.1f", driver.salaryPercent)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Header KPIs
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Всего рейсов", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            Text("${driverTrips.size}", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                        }
                        Column {
                            Text("Фрахт", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            Text(Formatters.formatMoney(totalRevenue), fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Начислено ЗП", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            Text(Formatters.formatMoney(totalSalary), fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (driverTrips.isEmpty()) {
                    Text(
                        text = "Для этого водителя пока нет зарегистрированных рейсов в базе.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(driverTrips, key = { it.id }) { t ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = t.tripNumber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = Formatters.formatDate(t.loadingDate),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${t.cargoType} • ${t.loadingLocation} → ${t.unloadingLocation}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Фрахт: ${Formatters.formatMoney(t.totalPrice)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "ЗП (${String.format(Locale.US, "%.0f", t.driverSalaryPercent)}%): ${Formatters.formatMoney(t.effectiveDriverSalary)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun BatchEditDriversDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onApply: (newStatus: DriverStatus?, newSalaryPercent: Double?, newShiftSchedule: String?) -> Unit
) {
    var updateStatus by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(DriverStatus.AVAILABLE) }

    var updateSalary by remember { mutableStateOf(false) }
    var salaryText by remember { mutableStateOf("20") }

    var updateSchedule by remember { mutableStateOf(false) }
    var scheduleText by remember { mutableStateOf("15/15 вахта") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пакетное редактирование водителей ($selectedCount)") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Выберите параметры, которые нужно применить ко всем отмеченным водителям:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Статус водителя
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (updateStatus) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = updateStatus, onCheckedChange = { updateStatus = it })
                            Text("Изменить статус водителя", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (updateStatus) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DriverStatus.values().forEach { st ->
                                    FilterChip(
                                        selected = selectedStatus == st,
                                        onClick = { selectedStatus = st },
                                        label = { Text(st.label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Ставка (% от фрахта)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (updateSalary) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = updateSalary, onCheckedChange = { updateSalary = it })
                            Text("Изменить ставку (% от фрахта)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (updateSalary) {
                            OutlinedTextField(
                                value = salaryText,
                                onValueChange = { salaryText = it },
                                label = { Text("Процент водителя (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // 3. График работы
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (updateSchedule) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = updateSchedule, onCheckedChange = { updateSchedule = it })
                            Text("Изменить график работы", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (updateSchedule) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("15/15 вахта", "20/10 вахта", "30/15 вахта", "5/2 постоянный", "Без графика").forEach { sch ->
                                    FilterChip(
                                        selected = scheduleText == sch,
                                        onClick = { scheduleText = sch },
                                        label = { Text(sch, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val salaryVal = if (updateSalary) salaryText.replace(',', '.').toDoubleOrNull() else null
                    onApply(
                        if (updateStatus) selectedStatus else null,
                        salaryVal,
                        if (updateSchedule) scheduleText else null
                    )
                },
                enabled = updateStatus || updateSalary || updateSchedule
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
