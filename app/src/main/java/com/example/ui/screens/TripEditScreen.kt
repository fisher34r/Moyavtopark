package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import android.widget.Toast
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.util.RouteResult
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.data.model.RateType
import com.example.data.model.Trip
import com.example.data.model.TripStatus
import com.example.ui.components.LivePriceCalculatorCard
import com.example.ui.components.LocationSearchField
import com.example.ui.viewmodel.TripViewModel
import com.example.util.Formatters
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditScreen(
    tripToEdit: Trip?,
    viewModel: TripViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isEditing = tripToEdit != null
    var isCalculatingDistance by remember { mutableStateOf(false) }
    var routeCalculationNote by remember { mutableStateOf<String?>(null) }
    val knownHistoryLocations by viewModel.knownLocations.collectAsStateWithLifecycle()
    val totalSettlementsCount by viewModel.locationService.totalSettlementsCount.collectAsStateWithLifecycle()
    val isOnlineLoading by viewModel.locationService.isLoadingOnline.collectAsStateWithLifecycle()
    val registeredDrivers by viewModel.registeredDrivers.collectAsStateWithLifecycle()
    val registeredVehicles by viewModel.registeredVehicles.collectAsStateWithLifecycle()

    // Fetch remembered data from the previous trip (or settings)
    val rememberedData = remember { viewModel.getInitialNewTripData() }

    // Form State: prefilled from tripToEdit if editing, or from previous trip's remembered data!
    var tripNumber by remember {
        mutableStateOf(tripToEdit?.tripNumber ?: rememberedData.nextTripNumber)
    }
    var cargoType by remember {
        mutableStateOf(tripToEdit?.cargoType ?: rememberedData.cargoType)
    }
    var loadingDate by remember {
        mutableLongStateOf(tripToEdit?.loadingDate ?: System.currentTimeMillis())
    }
    var hasUnloadingDate by remember {
        mutableStateOf(if (tripToEdit != null) tripToEdit.unloadingDate != null else true)
    }
    var unloadingDate by remember {
        mutableLongStateOf(tripToEdit?.unloadingDate ?: (tripToEdit?.loadingDate ?: System.currentTimeMillis()))
    }
    var loadingLocation by remember {
        mutableStateOf(tripToEdit?.loadingLocation ?: rememberedData.loadingLocation)
    }
    var unloadingLocation by remember {
        mutableStateOf(tripToEdit?.unloadingLocation ?: rememberedData.unloadingLocation)
    }

    var weightText by remember {
        val initialWeight = if (tripToEdit != null && tripToEdit.weightTons > 0) {
            tripToEdit.weightTons
        } else if (rememberedData.weightTons > 0) {
            rememberedData.weightTons
        } else 28.50
        mutableStateOf(String.format(Locale.US, "%.2f", initialWeight))
    }
    var volumeText by remember {
        val initialVol = tripToEdit?.volumeM3 ?: rememberedData.volumeM3
        mutableStateOf(if (initialVol != null && initialVol > 0) initialVol.toString() else "")
    }
    var distanceText by remember {
        val initialDist = if (tripToEdit != null && tripToEdit.distanceKm > 0) {
            tripToEdit.distanceKm
        } else if (rememberedData.distanceKm > 0) {
            rememberedData.distanceKm
        } else 280.0
        mutableStateOf(String.format(Locale.US, "%.0f", initialDist))
    }

    var selectedRateType by remember {
        mutableStateOf(tripToEdit?.rateType ?: rememberedData.rateType)
    }
    var rateValueText by remember {
        val initialRate = tripToEdit?.rateValue ?: rememberedData.rateValue
        mutableStateOf(initialRate.toString())
    }

    // Fuel State
    var fuelRateText by remember {
        val initialFuelRate = tripToEdit?.fuelConsumptionRate ?: rememberedData.fuelRate
        mutableStateOf(initialFuelRate.toString())
    }
    var fuelPriceText by remember {
        val initialFuelPrice = tripToEdit?.fuelPricePerLiter ?: rememberedData.fuelPrice
        mutableStateOf(initialFuelPrice.toString())
    }

    val parsedDistanceForInit = distanceText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val parsedFuelRateForInit = fuelRateText.replace(',', '.').toDoubleOrNull() ?: 38.0
    val parsedFuelPriceForInit = fuelPriceText.replace(',', '.').toDoubleOrNull() ?: 66.0

    var fuelLitersText by remember {
        val initialLiters = tripToEdit?.fuelLiters ?: if (parsedDistanceForInit > 0 && parsedFuelRateForInit > 0) {
            (parsedDistanceForInit / 100.0) * parsedFuelRateForInit
        } else 0.0
        mutableStateOf(if (initialLiters > 0) String.format(Locale.US, "%.1f", initialLiters) else "")
    }

    var fuelExpensesText by remember {
        val initialCost = if (tripToEdit != null && tripToEdit.fuelExpenses > 0) {
            tripToEdit.fuelExpenses
        } else if (viewModel.settings.autoCalculateFuel && parsedDistanceForInit > 0) {
            (parsedDistanceForInit / 100.0) * parsedFuelRateForInit * parsedFuelPriceForInit
        } else 0.0
        mutableStateOf(if (initialCost > 0) String.format(Locale.US, "%.0f", initialCost) else "")
    }

    var otherExpensesText by remember {
        mutableStateOf(if (tripToEdit != null && tripToEdit.otherExpenses > 0) tripToEdit.otherExpenses.toString() else "")
    }

    var status by remember {
        mutableStateOf(tripToEdit?.status ?: TripStatus.IN_TRANSIT)
    }
    var ttnNumber by remember {
        mutableStateOf(tripToEdit?.ttnNumber ?: "")
    }
    var customerName by remember {
        mutableStateOf(tripToEdit?.customerName ?: rememberedData.customerName)
    }
    var driverName by remember {
        mutableStateOf(tripToEdit?.driverName ?: rememberedData.driverName)
    }
    var truckPlate by remember {
        mutableStateOf(tripToEdit?.truckPlate ?: rememberedData.truckPlate)
    }
    var driverSalaryPercentText by remember {
        val initialPercent = tripToEdit?.driverSalaryPercent
            ?: if (rememberedData.driverSalaryPercent > 0) rememberedData.driverSalaryPercent else 20.0
        mutableStateOf(String.format(Locale.US, "%.1f", initialPercent))
    }
    var notes by remember {
        mutableStateOf(tripToEdit?.notes ?: "")
    }

    var driverDropdownExpanded by remember { mutableStateOf(false) }
    var truckDropdownExpanded by remember { mutableStateOf(false) }

    var showAdvancedDetails by remember {
        mutableStateOf(isEditing || ttnNumber.isNotBlank() || customerName.isNotBlank())
    }

    // Parsed numeric values for dynamic auto-calculation
    val parsedWeight = weightText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val parsedDistance = distanceText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val parsedRateValue = rateValueText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val parsedFuelRate = fuelRateText.replace(',', '.').toDoubleOrNull() ?: 38.0
    val parsedFuelPrice = fuelPriceText.replace(',', '.').toDoubleOrNull() ?: 66.0
    val parsedSalaryPercent = driverSalaryPercentText.replace(',', '.').toDoubleOrNull() ?: 20.0

    val currentCalculatedPrice = remember(parsedWeight, parsedDistance, selectedRateType, parsedRateValue) {
        Trip.calculatePrice(
            weight = parsedWeight,
            distance = parsedDistance,
            rateType = selectedRateType,
            rateValue = parsedRateValue
        )
    }

    // Auto-update fuel expenses when distance changes
    fun recalculateFuel() {
        if (parsedDistance > 0 && parsedFuelRate > 0) {
            val liters = (parsedDistance / 100.0) * parsedFuelRate
            val cost = liters * parsedFuelPrice
            fuelLitersText = String.format(Locale.US, "%.1f", liters)
            fuelExpensesText = String.format(Locale.US, "%.0f", cost)
        }
    }

    val scrollState = rememberScrollState()

    // Date & Time pickers for loading
    val showLoadingDatePicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = loadingDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = loadingDate
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                val prevLoading = loadingDate
                loadingDate = newCal.timeInMillis
                if (tripToEdit == null || unloadingDate == prevLoading) {
                    unloadingDate = newCal.timeInMillis
                    hasUnloadingDate = true
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val showLoadingTimePicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = loadingDate }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = loadingDate
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                loadingDate = newCal.timeInMillis
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    // Date & Time pickers for unloading
    val showUnloadingDatePicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = unloadingDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = unloadingDate
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                unloadingDate = newCal.timeInMillis
                hasUnloadingDate = true
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Редактирование рейса" else "Новый рейс зерновоза",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val finalPrice = Trip.calculatePrice(
                            weight = parsedWeight,
                            distance = parsedDistance,
                            rateType = selectedRateType,
                            rateValue = parsedRateValue
                        )
                        val trip = Trip(
                            id = tripToEdit?.id ?: 0L,
                            tripNumber = tripNumber.trim(),
                            cargoType = cargoType.trim(),
                            loadingDate = loadingDate,
                            unloadingDate = if (hasUnloadingDate) unloadingDate else (if (tripToEdit == null) loadingDate else null),
                            loadingLocation = loadingLocation.trim(),
                            unloadingLocation = unloadingLocation.trim(),
                            weightTons = parsedWeight,
                            volumeM3 = volumeText.replace(',', '.').toDoubleOrNull(),
                            distanceKm = parsedDistance,
                            rateType = selectedRateType,
                            rateValue = parsedRateValue,
                            totalPrice = finalPrice,
                            status = status,
                            ttnNumber = ttnNumber.trim(),
                            customerName = customerName.trim(),
                            driverName = driverName.trim(),
                            truckPlate = truckPlate.trim(),
                            fuelConsumptionRate = parsedFuelRate,
                            fuelPricePerLiter = parsedFuelPrice,
                            fuelLiters = fuelLitersText.replace(',', '.').toDoubleOrNull(),
                            fuelExpenses = fuelExpensesText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            otherExpenses = otherExpensesText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            driverSalaryPercent = parsedSalaryPercent,
                            driverSalaryAmount = if (finalPrice > 0) finalPrice * (parsedSalaryPercent / 100.0) else 0.0,
                            notes = notes.trim(),
                            createdAt = tripToEdit?.createdAt ?: System.currentTimeMillis()
                        )
                        viewModel.saveTrip(trip) {
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_trip_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) "Сохранить изменения" else "Зафиксировать рейс",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Price Calculator Card
            LivePriceCalculatorCard(
                weightTons = parsedWeight,
                distanceKm = parsedDistance,
                rateType = selectedRateType,
                rateValue = parsedRateValue,
                driverSalaryPercent = parsedSalaryPercent
            )

            // Auto-fill notice banner when creating a new trip with remembered values
            if (!isEditing && rememberedData.isAutoFilledFromPrevious) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Данные (маршрут, груз, тариф) подставлены из предыдущего рейса",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        TextButton(
                            onClick = {
                                loadingLocation = ""
                                unloadingLocation = ""
                                customerName = ""
                                notes = ""
                            }
                        ) {
                            Text("Очистить", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            // Section 1: Культура и Номер рейса
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Культура и номер рейса",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (!isEditing) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "+1 от предыдущего",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tripNumber,
                        onValueChange = { tripNumber = it },
                        label = { Text("Номер или наименование рейса") },
                        placeholder = { Text("Например: Рейс №12") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_trip_number"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cargoType,
                        onValueChange = { cargoType = it },
                        label = { Text("Культура / Груз") },
                        placeholder = { Text("Пшеница, Ячмень, Кукуруза...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_cargo_type"),
                        singleLine = true
                    )
                }
            }

            // Section 2: Параметры груза и пути (Вес, Расстояние, Объем)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Объем груза и расстояние пути",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Вес (тонн) *") },
                            placeholder = { Text("28.5") },
                            trailingIcon = { Text("т", modifier = Modifier.padding(end = 12.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_weight"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = distanceText,
                            onValueChange = {
                                distanceText = it
                                val newDist = it.replace(',', '.').toDoubleOrNull() ?: 0.0
                                if (viewModel.settings.autoCalculateFuel && newDist > 0 && parsedFuelRate > 0) {
                                    val liters = (newDist / 100.0) * parsedFuelRate
                                    val cost = liters * parsedFuelPrice
                                    fuelLitersText = String.format(Locale.US, "%.1f", liters)
                                    fuelExpensesText = String.format(Locale.US, "%.0f", cost)
                                }
                            },
                            label = { Text("Расстояние *") },
                            placeholder = { Text("300") },
                            trailingIcon = { Text("км", modifier = Modifier.padding(end = 12.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_distance"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = volumeText,
                        onValueChange = { volumeText = it },
                        label = { Text("Объем кузова/груза (опционально)") },
                        placeholder = { Text("Например: 42") },
                        trailingIcon = { Text("м³", modifier = Modifier.padding(end = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_volume"),
                        singleLine = true
                    )
                }
            }

            // Section 3: Расчет затраченного топлива
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalGasStation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3. Расчет затраченного топлива",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { recalculateFuel() },
                            modifier = Modifier.testTag("recalc_fuel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Пересчитать",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fuel settings applied for this trip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = fuelRateText,
                            onValueChange = {
                                fuelRateText = it
                                recalculateFuel()
                            },
                            label = { Text("Норма расхода") },
                            placeholder = { Text("38.0") },
                            trailingIcon = { Text("л/100км", fontSize = 11.sp, modifier = Modifier.padding(end = 6.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = fuelPriceText,
                            onValueChange = {
                                fuelPriceText = it
                                recalculateFuel()
                            },
                            label = { Text("Цена топлива") },
                            placeholder = { Text("66.00") },
                            trailingIcon = { Text("₽/л", fontSize = 11.sp, modifier = Modifier.padding(end = 6.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calculated Fuel Liters and Fuel Cost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = fuelLitersText,
                            onValueChange = {
                                fuelLitersText = it
                                val l = it.replace(',', '.').toDoubleOrNull() ?: 0.0
                                if (l > 0 && parsedFuelPrice > 0) {
                                    fuelExpensesText = String.format(Locale.US, "%.0f", l * parsedFuelPrice)
                                }
                            },
                            label = { Text("Затрачено ДТ (литров)") },
                            placeholder = { Text("114.0") },
                            trailingIcon = { Text("л", modifier = Modifier.padding(end = 12.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_fuel_liters"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = fuelExpensesText,
                            onValueChange = { fuelExpensesText = it },
                            label = { Text("Стоимость ДТ (₽)") },
                            placeholder = { Text("7524") },
                            trailingIcon = { Text("₽", modifier = Modifier.padding(end = 12.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_fuel_expenses"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Breakdown calculation note
                    val calcLiters = if (parsedDistance > 0 && parsedFuelRate > 0) (parsedDistance / 100.0) * parsedFuelRate else 0.0
                    val calcCost = calcLiters * parsedFuelPrice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Расчет по норме: ${Formatters.formatDistance(parsedDistance)} ÷ 100 × ${Formatters.formatFuelRate(parsedFuelRate)} = ${Formatters.formatFuelLiters(calcLiters)} × ${Formatters.formatFuelPrice(parsedFuelPrice)} = ${Formatters.formatMoney(calcCost)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 4: Настройка формулы и тарифа для автоматического расчета цены перевозки
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. Тариф перевозки и расчет стоимости",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Метод расчета цены перевозки:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Rate Type selector chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RateType.values().forEach { type ->
                            FilterChip(
                                selected = selectedRateType == type,
                                onClick = {
                                    selectedRateType = type
                                    if (rateValueText.toDoubleOrNull() != null) {
                                        when (type) {
                                            RateType.PER_TON_KM -> if (parsedRateValue > 50) rateValueText = "4.60"
                                            RateType.PER_TON -> if (parsedRateValue < 10) rateValueText = "1400"
                                            RateType.PER_KM -> if (parsedRateValue < 10) rateValueText = "85"
                                            RateType.FIXED -> if (parsedRateValue < 1000) rateValueText = "35000"
                                        }
                                    }
                                },
                                label = { Text(type.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = rateValueText,
                        onValueChange = { rateValueText = it },
                        label = { Text("Ставка (${selectedRateType.unitLabel})") },
                        placeholder = { Text("Например: 4.60") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_rate_value"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Формула: ${selectedRateType.formulaHint}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 5: Места погрузки и выгрузки, даты
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "5. Маршрут, дата и время",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isOnlineLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "База нас. пунктов: $totalSettlementsCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Погрузка
                    LocationSearchField(
                        value = loadingLocation,
                        onValueChange = { loadingLocation = it },
                        label = "Где была погрузка (населенный пункт, ток, элеватор)",
                        placeholder = "Начните ввод (Каневская, Павловская...)",
                        locationService = viewModel.locationService,
                        knownHistoryLocations = knownHistoryLocations,
                        testTag = "input_loading_location"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = showLoadingDatePicker,
                            colors = ButtonDefaults.outlinedButtonColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Formatters.formatDate(loadingDate), fontSize = 12.sp)
                        }

                        Button(
                            onClick = showLoadingTimePicker,
                            colors = ButtonDefaults.outlinedButtonColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            val cal = Calendar.getInstance().apply { timeInMillis = loadingDate }
                            val timeStr = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                            Text(timeStr, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Выгрузка
                    LocationSearchField(
                        value = unloadingLocation,
                        onValueChange = { unloadingLocation = it },
                        label = "Где выгрузка (терминал, порт, завод, город)",
                        placeholder = "Начните ввод (Новороссийск, Тамань, Ростов...)",
                        locationService = viewModel.locationService,
                        knownHistoryLocations = knownHistoryLocations,
                        testTag = "input_unloading_location"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            if (loadingLocation.isNotBlank() && unloadingLocation.isNotBlank()) {
                                isCalculatingDistance = true
                                routeCalculationNote = null
                                coroutineScope.launch {
                                    when (val res = viewModel.calculateDistance(loadingLocation, unloadingLocation)) {
                                        is RouteResult.Success -> {
                                            val rounded = Math.round(res.distanceKm).toInt()
                                            distanceText = rounded.toString()
                                            val newDist = rounded.toDouble()
                                            if (viewModel.settings.autoCalculateFuel && newDist > 0 && parsedFuelRate > 0) {
                                                val liters = (newDist / 100.0) * parsedFuelRate
                                                val cost = liters * parsedFuelPrice
                                                fuelLitersText = String.format(Locale.US, "%.1f", liters)
                                                fuelExpensesText = String.format(Locale.US, "%.0f", cost)
                                            }
                                            val src = if (res.fromCache) "из кэша" else res.provider
                                            routeCalculationNote = "${res.distanceKm} км ($src)"
                                            Toast.makeText(context, "Рассчитано: ${res.distanceKm} км ($src)", Toast.LENGTH_SHORT).show()
                                        }
                                        is RouteResult.Error -> {
                                            routeCalculationNote = "Ошибка: ${res.message}"
                                            Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    isCalculatingDistance = false
                                }
                            } else {
                                Toast.makeText(context, "Укажите погрузку и выгрузку для расчёта", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isCalculatingDistance && loadingLocation.isNotBlank() && unloadingLocation.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_calculate_distance"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isCalculatingDistance) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Расчёт дистанции...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Рассчитать расстояние по маршруту (км)", fontSize = 12.sp)
                        }
                    }

                    if (routeCalculationNote != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Маршрут: $routeCalculationNote",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Дата выгрузки: ${Formatters.formatDate(unloadingDate)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (unloadingDate == loadingDate) "По умолчанию совпадает с датой погрузки" else "Установлена индивидуальная дата",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = showUnloadingDatePicker,
                            colors = ButtonDefaults.outlinedButtonColors(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Изменить дату", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Section 6: Статус рейса
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "6. Текущий статус рейса",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TripStatus.values().forEach { statusOption ->
                            FilterChip(
                                selected = status == statusOption,
                                onClick = {
                                    status = statusOption
                                    if (statusOption == TripStatus.UNLOADED || statusOption == TripStatus.PAID) {
                                        hasUnloadingDate = true
                                    }
                                },
                                label = { Text(statusOption.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Section 7: Дополнительные параметры (ТТН, Заказчик, Автомобиль, Прочие расходы)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedDetails = !showAdvancedDetails },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Дополнительно (ТТН, Заказчик, Прочие расходы)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (showAdvancedDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    if (showAdvancedDetails) {
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = ttnNumber,
                            onValueChange = { ttnNumber = it },
                            label = { Text("Номер ТТН / Накладной") },
                            placeholder = { Text("ТТН-04821") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_ttn"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Заказчик / Экспедитор") },
                            placeholder = { Text("ООО 'АгроЭкспорт'") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_customer"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Registered drivers quick chips
                        if (registeredDrivers.isNotEmpty()) {
                            Text(
                                text = "Водители из базы автопарка:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                registeredDrivers.forEach { d ->
                                    val isSelected = driverName == d.fullName
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            driverName = d.fullName
                                            driverSalaryPercentText = String.format(Locale.US, "%.1f", d.salaryPercent)
                                            if (d.assignedVehiclePlate.isNotBlank()) {
                                                truckPlate = d.assignedVehiclePlate
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = "${d.fullName} (${String.format(Locale.US, "%.0f", d.salaryPercent)}%)",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Водитель с выпадающим списком из базы автопарка
                            ExposedDropdownMenuBox(
                                expanded = driverDropdownExpanded,
                                onExpandedChange = { driverDropdownExpanded = !driverDropdownExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = driverName,
                                    onValueChange = { driverName = it },
                                    label = { Text("Водитель") },
                                    placeholder = { Text("ФИО водителя") },
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
                                                        Text(d.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(
                                                            text = "ЗП: ${String.format(Locale.US, "%.0f", d.salaryPercent)}%" +
                                                                    if (d.assignedVehiclePlate.isNotBlank()) " • Тягач: ${d.assignedVehiclePlate}" else "",
                                                            fontSize = 11.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    driverName = d.fullName
                                                    driverSalaryPercentText = String.format(Locale.US, "%.1f", d.salaryPercent)
                                                    if (d.assignedVehiclePlate.isNotBlank()) {
                                                        truckPlate = d.assignedVehiclePlate
                                                    }
                                                    driverDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Госномер тягача с выпадающим списком из базы автопарка
                            ExposedDropdownMenuBox(
                                expanded = truckDropdownExpanded,
                                onExpandedChange = { truckDropdownExpanded = !truckDropdownExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = truckPlate,
                                    onValueChange = { truckPlate = it },
                                    label = { Text("Госномер тягача") },
                                    placeholder = { Text("А 742 КХ 123") },
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
                                                        Text(v.plateNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(
                                                            text = "${v.model}" + if (v.assignedDriverName.isNotBlank()) " • ${v.assignedDriverName}" else "",
                                                            fontSize = 11.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    truckPlate = v.plateNumber
                                                    if (v.assignedDriverName.isNotBlank()) {
                                                        driverName = v.assignedDriverName
                                                        val matchingDriver = registeredDrivers.find {
                                                            it.fullName.equals(v.assignedDriverName, ignoreCase = true)
                                                        }
                                                        if (matchingDriver != null) {
                                                            driverSalaryPercentText = String.format(Locale.US, "%.1f", matchingDriver.salaryPercent)
                                                        }
                                                    } else {
                                                        val matchingDriver = registeredDrivers.find {
                                                            it.assignedVehiclePlate.equals(v.plateNumber, ignoreCase = true)
                                                        }
                                                        if (matchingDriver != null) {
                                                            driverName = matchingDriver.fullName
                                                            driverSalaryPercentText = String.format(Locale.US, "%.1f", matchingDriver.salaryPercent)
                                                        }
                                                    }
                                                    truckDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Driver salary percentage and live accrual card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = driverSalaryPercentText,
                                onValueChange = { driverSalaryPercentText = it },
                                label = { Text("Ставка ЗП водителя (% от фрахта)") },
                                placeholder = { Text("20.0") },
                                trailingIcon = {
                                    Text("%", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1.3f),
                                singleLine = true
                            )

                            val liveDriverSalary = if (currentCalculatedPrice > 0) currentCalculatedPrice * (parsedSalaryPercent / 100.0) else 0.0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Начислено ЗП:", fontSize = 10.sp, color = Color(0xFF2E7D32))
                                    Text(
                                        text = Formatters.formatMoney(liveDriverSalary),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = otherExpensesText,
                            onValueChange = { otherExpensesText = it },
                            label = { Text("Прочие расходы: Платон/весы/стоянка (₽)") },
                            placeholder = { Text("1200") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Примечания (сорность, влажность, протеин)") },
                            placeholder = { Text("Влажность 13%, клейковина 23%...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
