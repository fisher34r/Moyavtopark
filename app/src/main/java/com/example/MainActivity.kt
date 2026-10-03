package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsPreferences
import com.example.data.model.Trip
import com.example.data.repository.FleetRepository
import com.example.data.repository.TripRepository
import com.example.ui.screens.TripDetailScreen
import com.example.ui.screens.TripEditScreen
import com.example.ui.screens.TripListScreen
import com.example.ui.screens.TripReportsScreen
import com.example.ui.screens.TripSettingsScreen
import com.example.ui.screens.fleet.FleetMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FleetViewModel
import com.example.ui.viewmodel.FleetViewModelFactory
import com.example.ui.viewmodel.TripViewModel
import com.example.ui.viewmodel.TripViewModelFactory

sealed class NavigationDestination {
    object TripList : NavigationDestination()
    object Fleet : NavigationDestination()
    object TripReports : NavigationDestination()
    object TripSettings : NavigationDestination()
    data class TripDetail(val trip: Trip) : NavigationDestination()
    data class TripEdit(val trip: Trip?) : NavigationDestination()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            // Initialize Database and Repositories
            val database = remember { AppDatabase.getDatabase(context, scope) }
            val settings = remember { SettingsPreferences(context) }
            val repository = remember { TripRepository(database.tripDao(), settings) }
            val fleetRepository = remember {
                FleetRepository(
                    vehicleDao = database.vehicleDao(),
                    driverDao = database.driverDao(),
                    serviceDao = database.serviceRecordDao(),
                    waybillDao = database.waybillDao(),
                    docDao = database.fleetDocumentDao()
                )
            }

            val tripViewModel: TripViewModel = viewModel(
                factory = TripViewModelFactory(repository, fleetRepository)
            )
            val fleetViewModel: FleetViewModel = viewModel(
                factory = FleetViewModelFactory(fleetRepository, repository)
            )

            val currentThemeMode by tripViewModel.currentThemeMode.collectAsStateWithLifecycle()
            val currentColorStyle by tripViewModel.currentColorStyle.collectAsStateWithLifecycle()

            MyApplicationTheme(
                themeMode = currentThemeMode,
                colorStyle = currentColorStyle
            ) {
                var currentDestination by remember {
                    mutableStateOf<NavigationDestination>(NavigationDestination.TripList)
                }

                // If currently viewing a trip detail and the trip changes in the DB, sync it
                val allTrips by tripViewModel.allTrips.collectAsStateWithLifecycle()

                val activeTripInDetail = (currentDestination as? NavigationDestination.TripDetail)?.trip?.let { detailTrip ->
                    allTrips.find { it.id == detailTrip.id } ?: detailTrip
                }

                val showBottomBar = currentDestination is NavigationDestination.TripList ||
                        currentDestination is NavigationDestination.Fleet ||
                        currentDestination is NavigationDestination.TripReports ||
                        currentDestination is NavigationDestination.TripSettings

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(modifier = Modifier.testTag("main_bottom_nav")) {
                                NavigationBarItem(
                                    selected = currentDestination is NavigationDestination.TripList,
                                    onClick = { currentDestination = NavigationDestination.TripList },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentDestination is NavigationDestination.TripList)
                                                Icons.Filled.LocalShipping else Icons.Outlined.LocalShipping,
                                            contentDescription = "Рейсы"
                                        )
                                    },
                                    label = { Text("Рейсы") },
                                    modifier = Modifier.testTag("nav_tab_trips")
                                )

                                NavigationBarItem(
                                    selected = currentDestination is NavigationDestination.Fleet,
                                    onClick = { currentDestination = NavigationDestination.Fleet },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentDestination is NavigationDestination.Fleet)
                                                Icons.Filled.DirectionsCar else Icons.Outlined.DirectionsCar,
                                            contentDescription = "Автопарк"
                                        )
                                    },
                                    label = { Text("Автопарк") },
                                    modifier = Modifier.testTag("nav_tab_fleet")
                                )

                                NavigationBarItem(
                                    selected = currentDestination is NavigationDestination.TripReports,
                                    onClick = { currentDestination = NavigationDestination.TripReports },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentDestination is NavigationDestination.TripReports)
                                                Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                            contentDescription = "Отчеты"
                                        )
                                    },
                                    label = { Text("Отчеты") },
                                    modifier = Modifier.testTag("nav_tab_reports")
                                )

                                NavigationBarItem(
                                    selected = currentDestination is NavigationDestination.TripSettings,
                                    onClick = { currentDestination = NavigationDestination.TripSettings },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentDestination is NavigationDestination.TripSettings)
                                                Icons.Filled.Settings else Icons.Outlined.Settings,
                                            contentDescription = "Настройки"
                                        )
                                    },
                                    label = { Text("Настройки") },
                                    modifier = Modifier.testTag("nav_tab_settings")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (val dest = currentDestination) {
                        is NavigationDestination.TripList -> {
                            TripListScreen(
                                viewModel = tripViewModel,
                                onTripClick = { trip ->
                                    currentDestination = NavigationDestination.TripDetail(trip)
                                },
                                onAddNewTrip = {
                                    currentDestination = NavigationDestination.TripEdit(null)
                                },
                                onNavigateToReports = {
                                    currentDestination = NavigationDestination.TripReports
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        is NavigationDestination.Fleet -> {
                            FleetMainScreen(
                                viewModel = fleetViewModel,
                                onNavigateToTripsForVehicle = { plate ->
                                    tripViewModel.setFilterTruck(plate)
                                    currentDestination = NavigationDestination.TripList
                                },
                                onNavigateToReportsForVehicle = { plate ->
                                    tripViewModel.setFilterTruck(plate)
                                    currentDestination = NavigationDestination.TripReports
                                },
                                onNavigateToTripsForDriver = { driver ->
                                    tripViewModel.setFilterDriver(driver)
                                    currentDestination = NavigationDestination.TripList
                                },
                                onNavigateToReportsForDriver = { driver ->
                                    tripViewModel.setFilterDriver(driver)
                                    currentDestination = NavigationDestination.TripReports
                                },
                                onNavigateToReports = {
                                    currentDestination = NavigationDestination.TripReports
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        is NavigationDestination.TripReports -> {
                            TripReportsScreen(
                                viewModel = tripViewModel,
                                onBack = { currentDestination = NavigationDestination.TripList },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        is NavigationDestination.TripSettings -> {
                            TripSettingsScreen(
                                viewModel = tripViewModel,
                                onBack = { currentDestination = NavigationDestination.TripList },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        is NavigationDestination.TripDetail -> {
                            val targetTrip = activeTripInDetail ?: dest.trip
                            TripDetailScreen(
                                trip = targetTrip,
                                viewModel = tripViewModel,
                                onBack = { currentDestination = NavigationDestination.TripList },
                                onEdit = { tripToEdit ->
                                    currentDestination = NavigationDestination.TripEdit(tripToEdit)
                                },
                                onNavigateToReports = { truck, driver ->
                                    if (!truck.isNullOrBlank()) tripViewModel.setFilterTruck(truck)
                                    if (!driver.isNullOrBlank()) tripViewModel.setFilterDriver(driver)
                                    currentDestination = NavigationDestination.TripReports
                                },
                                onFilterByVehicle = { truck ->
                                    tripViewModel.setFilterTruck(truck)
                                    currentDestination = NavigationDestination.TripList
                                },
                                onFilterByDriver = { driver ->
                                    tripViewModel.setFilterDriver(driver)
                                    currentDestination = NavigationDestination.TripList
                                },
                                onNavigateToFleetVehicle = { truck ->
                                    fleetViewModel.selectedTab.value = 0
                                    fleetViewModel.vehicleSearchQuery.value = truck
                                    currentDestination = NavigationDestination.Fleet
                                },
                                onNavigateToFleetDriver = { driver ->
                                    fleetViewModel.selectedTab.value = 1
                                    fleetViewModel.driverSearchQuery.value = driver
                                    currentDestination = NavigationDestination.Fleet
                                }
                            )
                        }

                        is NavigationDestination.TripEdit -> {
                            TripEditScreen(
                                tripToEdit = dest.trip,
                                viewModel = tripViewModel,
                                onBack = {
                                    currentDestination = if (dest.trip != null) {
                                        NavigationDestination.TripDetail(dest.trip)
                                    } else {
                                        NavigationDestination.TripList
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
