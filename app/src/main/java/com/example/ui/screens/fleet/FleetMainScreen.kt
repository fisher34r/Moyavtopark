package com.example.ui.screens.fleet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.FleetViewModel

data class FleetTabItem(
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetMainScreen(
    viewModel: FleetViewModel,
    onNavigateToTripsForVehicle: ((plateNumber: String) -> Unit)? = null,
    onNavigateToReportsForVehicle: ((plateNumber: String) -> Unit)? = null,
    onNavigateToTripsForDriver: ((driverName: String) -> Unit)? = null,
    onNavigateToReportsForDriver: ((driverName: String) -> Unit)? = null,
    onNavigateToReports: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val summary by viewModel.analyticsSummary.collectAsStateWithLifecycle()

    val tabs = listOf(
        FleetTabItem("Транспорт", Icons.Default.LocalShipping, summary.totalVehicles),
        FleetTabItem("Водители", Icons.Default.Group, summary.totalDrivers),
        FleetTabItem("ТО и Ремонт", Icons.Default.Build, summary.vehiclesInService),
        FleetTabItem("Заправки", Icons.Default.LocalGasStation),
        FleetTabItem("Документы", Icons.Default.Policy),
        FleetTabItem("Аналитика", Icons.Default.Analytics)
    )

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
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
                            text = "РЈРїСЂР°РІР»РµРЅРёРµ Р°РІС‚РѕРїР°СЂРєРѕРј",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "РўСЏРіР°С‡Рё вЂў РџСЂРёС†РµРїС‹ вЂў Р’РѕРґРёС‚РµР»Рё вЂў РЎРµСЂРІРёСЃ вЂў РђРЅР°Р»РёС‚РёРєР°",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab.coerceIn(0, tabs.size - 1),
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().testTag("fleet_tab_row")
        ) {
            tabs.forEachIndexed { index, tabItem ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.selectedTab.value = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tabItem.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tabItem.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                            if (tabItem.badgeCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${tabItem.badgeCount}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("fleet_tab_$index")
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> FleetVehiclesTab(
                    viewModel = viewModel,
                    onNavigateToTrips = onNavigateToTripsForVehicle,
                    onNavigateToReports = onNavigateToReportsForVehicle
                )
                1 -> FleetDriversTab(
                    viewModel = viewModel,
                    onNavigateToTrips = onNavigateToTripsForDriver,
                    onNavigateToReports = onNavigateToReportsForDriver
                )
                2 -> FleetServiceTab(viewModel = viewModel)
                3 -> FleetFuelTab(viewModel = viewModel)
                4 -> FleetDocumentsTab(viewModel = viewModel)
                5 -> FleetAnalyticsTab(
                    viewModel = viewModel,
                    onNavigateToReports = onNavigateToReports
                )
                else -> FleetVehiclesTab(
                    viewModel = viewModel,
                    onNavigateToTrips = onNavigateToTripsForVehicle,
                    onNavigateToReports = onNavigateToReportsForVehicle
                )
            }
        }
    }
}






