package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import android.app.DatePickerDialog
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RateType
import com.example.data.model.Trip
import com.example.data.model.TripStatus
import com.example.ui.components.StatSummaryCard
import com.example.ui.components.TripCard
import com.example.ui.viewmodel.TripListPeriod
import com.example.ui.viewmodel.TripSortOrder
import com.example.ui.viewmodel.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    viewModel: TripViewModel,
    onTripClick: (Trip) -> Unit,
    onAddNewTrip: () -> Unit,
    onNavigateToReports: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val trips by viewModel.filteredTrips.collectAsStateWithLifecycle()
    val allTrips by viewModel.allTrips.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val currentFilterStatus by viewModel.filterStatus.collectAsStateWithLifecycle()
    val currentFilterCargo by viewModel.filterCargo.collectAsStateWithLifecycle()
    val currentFilterPeriod by viewModel.filterPeriod.collectAsStateWithLifecycle()
    val currentFilterDriver by viewModel.filterDriver.collectAsStateWithLifecycle()
    val currentFilterTruck by viewModel.filterTruck.collectAsStateWithLifecycle()
    val currentSortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val registeredDrivers by viewModel.registeredDrivers.collectAsStateWithLifecycle()
    val registeredVehicles by viewModel.registeredVehicles.collectAsStateWithLifecycle()

    val customDateStart by viewModel.customDateStart.collectAsStateWithLifecycle()
    val customDateEnd by viewModel.customDateEnd.collectAsStateWithLifecycle()

    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    val showCustomStartDatePicker = {
        val cal = Calendar.getInstance().apply {
            customDateStart?.let { timeInMillis = it }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.setCustomPeriod(newCal.timeInMillis, customDateEnd)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val showCustomEndDatePicker = {
        val cal = Calendar.getInstance().apply {
            customDateEnd?.let { timeInMillis = it }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                viewModel.setCustomPeriod(customDateStart, newCal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var isFilterSpoilerExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Batch Editing State
    var isBatchMode by remember { mutableStateOf(false) }
    val selectedTripIds = remember { mutableStateListOf<Long>() }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    val currentMonthKey = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(System.currentTimeMillis())
    }
    val monthTitleFormatter = remember {
        SimpleDateFormat("LLLL yyyy", Locale.forLanguageTag("ru"))
    }
    val monthKeyFormatter = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault())
    }

    var expandedMonths by rememberSaveable {
        mutableStateOf(setOf(currentMonthKey))
    }

    val groupedTrips = remember(trips) {
        trips.groupBy { trip ->
            monthKeyFormatter.format(trip.loadingDate)
        }
    }

    // Distinct crops, drivers, and trucks from actual trips for filter chips
    val availableCrops = remember(allTrips) {
        allTrips.map { it.cargoType }.filter { it.isNotBlank() }.distinct()
    }
    val availableDrivers = remember(allTrips) {
        allTrips.map { it.driverName }.filter { it.isNotBlank() }.distinct()
    }
    val availableTrucks = remember(allTrips) {
        allTrips.map { it.truckPlate }.filter { it.isNotBlank() }.distinct()
    }

    val activeFiltersCount = (if (currentFilterStatus != null) 1 else 0) +
        (if (currentFilterCargo != null) 1 else 0) +
        (if (currentFilterDriver != null) 1 else 0) +
        (if (currentFilterTruck != null) 1 else 0) +
        (if (currentFilterPeriod != TripListPeriod.ALL) 1 else 0) +
        (if (searchQuery.isNotBlank()) 1 else 0) +
        (if (currentSortOrder != TripSortOrder.DATE_DESC) 1 else 0)

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Фильтры и сортировка", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    if (activeFiltersCount > 0) {
                        TextButton(onClick = { viewModel.resetAllFilters() }) {
                            Text("Сбросить", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Period
                    Column {
                        Text("Период рейсов:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TripListPeriod.values().forEach { period ->
                                FilterChip(
                                    selected = currentFilterPeriod == period,
                                    onClick = {
                                        if (period == TripListPeriod.CUSTOM) {
                                            viewModel.setFilterPeriod(TripListPeriod.CUSTOM)
                                            if (customDateStart == null) {
                                                showCustomStartDatePicker()
                                            }
                                        } else {
                                            viewModel.setFilterPeriod(period)
                                        }
                                    },
                                    label = { Text(period.label) }
                                )
                            }
                        }

                        if (currentFilterPeriod == TripListPeriod.CUSTOM) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showCustomStartDatePicker() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = customDateStart?.let { "С: " + dateFormatter.format(it) } ?: "Дата С",
                                        fontSize = 12.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = { showCustomEndDatePicker() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = customDateEnd?.let { "По: " + dateFormatter.format(it) } ?: "Дата По",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 2. Status
                    Column {
                        Text("Статус рейса:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = currentFilterStatus == null,
                                onClick = { viewModel.setFilterStatus(null) },
                                label = { Text("Все статусы") }
                            )
                            TripStatus.values().forEach { status ->
                                FilterChip(
                                    selected = currentFilterStatus == status,
                                    onClick = {
                                        viewModel.setFilterStatus(if (currentFilterStatus == status) null else status)
                                    },
                                    label = { Text(status.label) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 3. Driver
                    if (availableDrivers.isNotEmpty()) {
                        Column {
                            Text("Водитель:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = currentFilterDriver == null,
                                    onClick = { viewModel.setFilterDriver(null) },
                                    label = { Text("Все водители") }
                                )
                                availableDrivers.forEach { driver ->
                                    FilterChip(
                                        selected = currentFilterDriver == driver,
                                        onClick = {
                                            viewModel.setFilterDriver(if (currentFilterDriver == driver) null else driver)
                                        },
                                        label = { Text(driver) }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    // 4. Cargo / Culture
                    if (availableCrops.isNotEmpty()) {
                        Column {
                            Text("Культура / груз:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = currentFilterCargo == null,
                                    onClick = { viewModel.setFilterCargo(null) },
                                    label = { Text("Все культуры") }
                                )
                                availableCrops.forEach { crop ->
                                    FilterChip(
                                        selected = currentFilterCargo == crop,
                                        onClick = {
                                            viewModel.setFilterCargo(if (currentFilterCargo == crop) null else crop)
                                        },
                                        label = { Text(crop) }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    // 5. Sort Order
                    Column {
                        Text("Сортировка:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TripSortOrder.values().forEach { sort ->
                                FilterChip(
                                    selected = currentSortOrder == sort,
                                    onClick = { viewModel.setSortOrder(sort) },
                                    label = { Text(sort.label) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFilterDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Применить")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (isBatchMode) {
                        Text(
                            text = "Выбрано: ${selectedTripIds.size} из ${trips.size}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "История рейсов",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Журнал зерновоза",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (isBatchMode) {
                        IconButton(
                            onClick = {
                                isBatchMode = false
                                selectedTripIds.clear()
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Выйти из режима выбора")
                        }
                    }
                },
                actions = {
                    if (isBatchMode) {
                        // Select all / Deselect all
                        IconButton(
                            onClick = {
                                if (selectedTripIds.size == trips.size) {
                                    selectedTripIds.clear()
                                } else {
                                    selectedTripIds.clear()
                                    selectedTripIds.addAll(trips.map { it.id })
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = if (selectedTripIds.size == trips.size) "Снять выбор" else "Выбрать все"
                            )
                        }

                        // Batch edit button
                        IconButton(
                            onClick = { showBatchEditDialog = true },
                            enabled = selectedTripIds.isNotEmpty(),
                            modifier = Modifier.testTag("batch_edit_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Редактировать выбранные",
                                tint = if (selectedTripIds.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }

                        // Batch delete button
                        IconButton(
                            onClick = { showBatchDeleteConfirm = true },
                            enabled = selectedTripIds.isNotEmpty(),
                            modifier = Modifier.testTag("batch_delete_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить выбранные",
                                tint = if (selectedTripIds.isNotEmpty()) MaterialTheme.colorScheme.error else Color.Gray
                            )
                        }
                    } else {
                        if (onNavigateToReports != null) {
                            IconButton(
                                onClick = onNavigateToReports,
                                modifier = Modifier.testTag("nav_to_reports_from_list_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = "Отчеты и реестры",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = { isSearchExpanded = !isSearchExpanded },
                            modifier = Modifier.testTag("toggle_search_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = "Поиск"
                            )
                        }

                        // Sort Button with Dropdown Menu
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_trips_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Сортировка рейсов",
                                    tint = if (currentSortOrder != TripSortOrder.DATE_DESC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                TripSortOrder.values().forEach { sort ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (currentSortOrder == sort) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text(
                                                    text = sort.label,
                                                    fontWeight = if (currentSortOrder == sort) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (currentSortOrder == sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setSortOrder(sort)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Filters button
                        IconButton(
                            onClick = { showFilterDialog = true },
                            modifier = Modifier.testTag("open_filters_button")
                        ) {
                            if (activeFiltersCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text("$activeFiltersCount")
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = "Фильтры")
                                }
                            } else {
                                Icon(Icons.Default.Tune, contentDescription = "Фильтры")
                            }
                        }

                        // Batch Edit Mode Toggle
                        IconButton(
                            onClick = { isBatchMode = true },
                            modifier = Modifier.testTag("batch_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "Пакетное редактирование"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!isBatchMode) {
                ExtendedFloatingActionButton(
                    onClick = onAddNewTrip,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Новый рейс", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_trip_fab")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Bar (if expanded or text entered)
            if (isSearchExpanded || searchQuery.isNotBlank()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Поиск по месту, ТТН, культуре, водителю...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_field"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true
                    )
                }
            }

            // Summary Dashboard Header
            item {
                StatSummaryCard(summary = summary)
            }

            // Header for List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBatchMode) "Выбор рейсов (${selectedTripIds.size} из ${trips.size})" else "Список всех рейсов (${trips.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isBatchMode) {
                        TextButton(
                            onClick = {
                                if (selectedTripIds.size == trips.size) {
                                    selectedTripIds.clear()
                                } else {
                                    selectedTripIds.clear()
                                    selectedTripIds.addAll(trips.map { it.id })
                                }
                            }
                        ) {
                            Text(
                                text = if (selectedTripIds.size == trips.size) "Снять всё" else "Выбрать все (${trips.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (currentSortOrder != TripSortOrder.DATE_DESC) {
                        Text(
                            text = currentSortOrder.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (currentFilterStatus != null || currentFilterCargo != null || searchQuery.isNotBlank()) {
                        Text(
                            text = "Фильтры применены",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Empty State
            if (trips.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || currentFilterStatus != null || currentFilterCargo != null) {
                                    "Рейсы не найдены"
                                } else {
                                    "История рейсов пуста"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || currentFilterStatus != null || currentFilterCargo != null) {
                                    "Попробуйте изменить параметры поиска или сбросить фильтры."
                                } else {
                                    "Добавьте свой первый рейс зерновоза, указав места погрузки, выгрузки, вес и расстояние."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (searchQuery.isNotBlank() || currentFilterStatus != null || currentFilterCargo != null) {
                                Button(
                                    onClick = {
                                        viewModel.setSearchQuery("")
                                        viewModel.setFilterStatus(null)
                                        viewModel.setFilterCargo(null)
                                    }
                                ) {
                                    Text("Сбросить фильтры")
                                }
                            } else {
                                Button(onClick = onAddNewTrip) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Зафиксировать рейс")
                                }
                            }
                        }
                    }
                }
            } else {
                // Trip Cards List grouped by Month
                groupedTrips.forEach { (monthKey, monthTrips) ->
                    val isExpanded = monthKey in expandedMonths
                    val firstTripDate = monthTrips.firstOrNull()?.loadingDate ?: System.currentTimeMillis()
                    val rawMonthTitle = monthTitleFormatter.format(firstTripDate)
                    val monthTitle = rawMonthTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("ru")) else it.toString() }
                    val totalTons = monthTrips.sumOf { it.weightTons }
                    val totalPrice = monthTrips.sumOf { it.totalPrice }

                    item(key = "header_$monthKey") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedMonths = if (isExpanded) {
                                        expandedMonths - monthKey
                                    } else {
                                        expandedMonths + monthKey
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (monthKey == currentMonthKey) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = monthTitle,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "${monthTrips.size}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "%.1f т • %,d ₽".format(Locale.getDefault(), totalTons, totalPrice.toLong()),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        expandedMonths = if (isExpanded) {
                                            expandedMonths - monthKey
                                        } else {
                                            expandedMonths + monthKey
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (isExpanded) {
                        items(
                            items = monthTrips,
                            key = { it.id }
                        ) { trip ->
                            TripCard(
                                trip = trip,
                                isSelectionMode = isBatchMode,
                                isSelected = trip.id in selectedTripIds,
                                onToggleSelect = {
                                    if (trip.id in selectedTripIds) {
                                        selectedTripIds.remove(trip.id)
                                    } else {
                                        selectedTripIds.add(trip.id)
                                    }
                                },
                                onClick = { onTripClick(trip) },
                                onStatusChange = { newStatus ->
                                    viewModel.updateTripStatus(trip, newStatus)
                                }
                            )
                        }
                    }
                }
            }

            // Bottom spacer so FAB doesn't cover last item
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Dialog for Batch Editing Selected Trips
    if (showBatchEditDialog && selectedTripIds.isNotEmpty()) {
        BatchEditTripsDialog(
            selectedCount = selectedTripIds.size,
            registeredDrivers = registeredDrivers,
            registeredVehicles = registeredVehicles,
            availableCrops = availableCrops,
            onDismiss = { showBatchEditDialog = false },
            onApply = { status, driver, truck, cargo, rateType, rateValue, salaryPercent ->
                viewModel.batchUpdateTrips(
                    tripIds = selectedTripIds.toSet(),
                    newStatus = status,
                    newDriverName = driver,
                    newTruckPlate = truck,
                    newCargoType = cargo,
                    newRateType = rateType,
                    newRateValue = rateValue,
                    newSalaryPercent = salaryPercent
                )
                val count = selectedTripIds.size
                isBatchMode = false
                selectedTripIds.clear()
                showBatchEditDialog = false
                Toast.makeText(context, "Обновлено $count рейсов", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog for Batch Deleting Selected Trips
    if (showBatchDeleteConfirm && selectedTripIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = {
                Text(
                    text = "Удалить выбранные рейсы?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text("Вы действительно хотите удалить ${selectedTripIds.size} выбранных рейсов? Это действие нельзя отменить.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = selectedTripIds.size
                        viewModel.batchDeleteTrips(selectedTripIds.toSet())
                        isBatchMode = false
                        selectedTripIds.clear()
                        showBatchDeleteConfirm = false
                        Toast.makeText(context, "Удалено $count рейсов", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Удалить ($selectedTripIds.size)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchEditTripsDialog(
    selectedCount: Int,
    registeredDrivers: List<com.example.data.fleet.Driver>,
    registeredVehicles: List<com.example.data.fleet.Vehicle>,
    availableCrops: List<String>,
    onDismiss: () -> Unit,
    onApply: (
        status: TripStatus?,
        driver: String?,
        truck: String?,
        cargo: String?,
        rateType: RateType?,
        rateValue: Double?,
        salaryPercent: Double?
    ) -> Unit
) {
    var updateStatus by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(TripStatus.IN_TRANSIT) }

    var updateDriver by remember { mutableStateOf(false) }
    var driverText by remember { mutableStateOf("") }
    var driverDropdownExpanded by remember { mutableStateOf(false) }

    var updateTruck by remember { mutableStateOf(false) }
    var truckText by remember { mutableStateOf("") }
    var truckDropdownExpanded by remember { mutableStateOf(false) }

    var updateCargo by remember { mutableStateOf(false) }
    var cargoText by remember { mutableStateOf("") }

    var updateRate by remember { mutableStateOf(false) }
    var rateType by remember { mutableStateOf(RateType.PER_TON_KM) }
    var rateValueText by remember { mutableStateOf("4.60") }

    var updateSalary by remember { mutableStateOf(false) }
    var salaryPercentText by remember { mutableStateOf("20.0") }

    val defaultCrops = listOf("Пшеница 3 класс", "Пшеница 4 класс", "Ячмень", "Кукуруза", "Подсолнечник", "Соя", "Рапс")
    val allCropSuggestions = (availableCrops + defaultCrops).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Пакетное редактирование",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Будет обновлено рейсов: $selectedCount",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Отметьте поля, которые необходимо изменить во всех выбранных рейсах:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Статус
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
                            Text("Изменить статус рейса", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (updateStatus) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TripStatus.values().forEach { st ->
                                    FilterChip(
                                        selected = selectedStatus == st,
                                        onClick = { selectedStatus = st },
                                        label = { Text(st.label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Водитель
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (updateDriver) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = updateDriver, onCheckedChange = { updateDriver = it })
                            Text("Назначить водителя", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (updateDriver) {
                            ExposedDropdownMenuBox(
                                expanded = driverDropdownExpanded,
                                onExpandedChange = { driverDropdownExpanded = !driverDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = driverText,
                                    onValueChange = { driverText = it },
                                    label = { Text("ФИО водителя") },
                                    placeholder = { Text("Выберите из автопарка...") },
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
                                    if (registeredDrivers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Нет водителей в автопарке") },
                                            onClick = { driverDropdownExpanded = false }
                                        )
                                    } else {
                                        registeredDrivers.forEach { d ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(d.fullName, fontWeight = FontWeight.SemiBold)
                                                        if (d.assignedVehiclePlate.isNotBlank()) {
                                                            Text("Тягач: ${d.assignedVehiclePlate}", fontSize = 11.sp, color = Color.Gray)
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    driverText = d.fullName
                                                    driverDropdownExpanded = false
                                                    if (d.assignedVehiclePlate.isNotBlank() && truckText.isBlank()) {
                                                        truckText = d.assignedVehiclePlate
                                                        updateTruck = true
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Автомобиль
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (updateTruck) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = updateTruck, onCheckedChange = { updateTruck = it })
                            Text("Назначить тягач / автомобиль", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (updateTruck) {
                            ExposedDropdownMenuBox(
                                expanded = truckDropdownExpanded,
                                onExpandedChange = { truckDropdownExpanded = !truckDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = truckText,
                                    onValueChange = { truckText = it },
                                    label = { Text("Госномер тягача") },
                                    placeholder = { Text("Выберите из автопарка...") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = truckDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryEditable),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = truckDropdownExpanded,
                                    onDismissRequest = { truckDropdownExpanded = false }
                                ) {
                                    if (registeredVehicles.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Нет ТС в автопарке") },
                                            onClick = { truckDropdownExpanded = false }
                                        )
                                    } else {
                                        registeredVehicles.forEach { v ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(v.plateNumber, fontWeight = FontWeight.Bold)
                                                        Text("${v.model} • ${v.assignedDriverName.ifBlank { "Без водителя" }}", fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                },
                                                onClick = {
                                                    truckText = v.plateNumber
                                                    truckDropdownExpanded = false
                                                    if (v.assignedDriverName.isNotBlank() && driverText.isBlank()) {
                                                        driverText = v.assignedDriverName
                                                        updateDriver = true
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Культура / груз
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (updateCargo) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = updateCargo, onCheckedChange = { updateCargo = it })
                            Text("Изменить культуру / груз", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (updateCargo) {
                            OutlinedTextField(
                                value = cargoText,
                                onValueChange = { cargoText = it },
                                label = { Text("Наименование культуры") },
                                placeholder = { Text("Пшеница 3 класс") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allCropSuggestions.take(6).forEach { crop ->
                                    FilterChip(
                                        selected = cargoText == crop,
                                        onClick = { cargoText = crop },
                                        label = { Text(crop, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Тариф
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (updateRate) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = updateRate, onCheckedChange = { updateRate = it })
                            Text("Изменить тариф и пересчитать стоимость", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (updateRate) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (rt in RateType.entries) {
                                    FilterChip(
                                        selected = rateType == rt,
                                        onClick = {
                                            rateType = rt
                                            val cur = rateValueText.toDoubleOrNull() ?: 0.0
                                            when (rt) {
                                                RateType.PER_TON_KM -> if (cur > 30) rateValueText = "4.60"
                                                RateType.PER_TON -> if (cur < 10) rateValueText = "1400"
                                                RateType.PER_KM -> if (cur < 10) rateValueText = "85"
                                                RateType.FIXED -> if (cur < 1000) rateValueText = "35000"
                                            }
                                        },
                                        label = { Text(rt.title, fontSize = 11.sp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = rateValueText,
                                onValueChange = { rateValueText = it },
                                label = { Text("Ставка (${rateType.unitLabel})") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // 6. ЗП Водителя
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
                            Text("Изменить % зарплаты водителя", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (updateSalary) {
                            OutlinedTextField(
                                value = salaryPercentText,
                                onValueChange = { salaryPercentText = it },
                                label = { Text("Ставка ЗП (% от фрахта)") },
                                placeholder = { Text("20.0") },
                                trailingIcon = { Text("%", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalStatus = if (updateStatus) selectedStatus else null
                    val finalDriver = if (updateDriver && driverText.isNotBlank()) driverText.trim() else null
                    val finalTruck = if (updateTruck && truckText.isNotBlank()) truckText.trim() else null
                    val finalCargo = if (updateCargo && cargoText.isNotBlank()) cargoText.trim() else null
                    val finalRateType = if (updateRate) rateType else null
                    val finalRateValue = if (updateRate) rateValueText.replace(',', '.').toDoubleOrNull() else null
                    val finalSalaryPercent = if (updateSalary) salaryPercentText.replace(',', '.').toDoubleOrNull() else null

                    onApply(
                        finalStatus,
                        finalDriver,
                        finalTruck,
                        finalCargo,
                        finalRateType,
                        finalRateValue,
                        finalSalaryPercent
                    )
                },
                enabled = updateStatus || updateDriver || updateTruck || updateCargo || updateRate || updateSalary
            ) {
                Text("Применить к $selectedCount рейсам")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
