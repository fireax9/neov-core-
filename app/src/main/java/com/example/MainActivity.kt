package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.NeovModule
import com.example.ui.NeovCoreViewModel
import com.example.ui.components.LtrTelemetryText
import com.example.ui.components.PrivacyBottomSheet
import com.example.ui.screens.BatteryScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SensorsScreen
import com.example.ui.screens.SocScreen
import com.example.ui.screens.SystemScreen
import com.example.ui.theme.LocalTelemetryColors
import com.example.ui.theme.NeovCoreTheme
import com.example.ui.theme.TelemetryTextStyles

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: NeovCoreViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val liveSensorReadings by viewModel.liveSensorReadings.collectAsStateWithLifecycle()

            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = uiState.isDarkThemeOverride ?: true

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                NeovCoreTheme(darkTheme = useDarkTheme) {
                    NeovCoreApp(
                        viewModel = viewModel,
                        useDarkTheme = useDarkTheme,
                        liveSensorReadings = liveSensorReadings
                    )
                }
            }
        }
    }
}

private data class NavDestinationMeta(
    val module: NeovModule,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val moduleDestinations = listOf(
    NavDestinationMeta(
        module = NeovModule.SOC,
        labelRes = R.string.nav_soc,
        selectedIcon = Icons.Filled.Memory,
        unselectedIcon = Icons.Outlined.Dns
    ),
    NavDestinationMeta(
        module = NeovModule.SYSTEM,
        labelRes = R.string.nav_system,
        selectedIcon = Icons.Filled.PhoneAndroid,
        unselectedIcon = Icons.Outlined.PhoneAndroid
    ),
    NavDestinationMeta(
        module = NeovModule.MEMORY,
        labelRes = R.string.nav_memory,
        selectedIcon = Icons.Filled.Dns,
        unselectedIcon = Icons.Outlined.Memory
    ),
    NavDestinationMeta(
        module = NeovModule.BATTERY,
        labelRes = R.string.nav_battery,
        selectedIcon = Icons.Filled.BatteryChargingFull,
        unselectedIcon = Icons.Outlined.BatteryChargingFull
    ),
    NavDestinationMeta(
        module = NeovModule.SENSORS,
        labelRes = R.string.nav_sensors,
        selectedIcon = Icons.Filled.Sensors,
        unselectedIcon = Icons.Outlined.Sensors
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeovCoreApp(
    viewModel: NeovCoreViewModel,
    useDarkTheme: Boolean,
    liveSensorReadings: Map<Int, com.example.data.model.LiveSensorReading>
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val telemetryColors = LocalTelemetryColors.current

    val isDiagnosticsRoute = currentDestination?.route == "diagnostics"

    fun navigateToModule(module: NeovModule) {
        viewModel.selectModule(module)
        navController.navigate(module.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (uiState.showPrivacySheet) {
        PrivacyBottomSheet(onDismiss = { viewModel.setPrivacySheetVisible(false) })
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        if (isDiagnosticsRoute) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.testTag("diag_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.action_close)
                                )
                            }
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.neov_core_white_red_logo_1795639367962),
                                contentDescription = stringResource(R.string.app_name),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                LtrTelemetryText(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isDiagnosticsRoute) {
                                        stringResource(R.string.diag_title)
                                    } else {
                                        stringResource(R.string.badge_offline_secure)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = telemetryColors.optimal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        val liveColor = if (uiState.isLiveEnabled) {
                            telemetryColors.optimal
                        } else {
                            telemetryColors.warm
                        }
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(50))
                                .clickable { viewModel.toggleLive() }
                                .testTag("live_toggle_chip"),
                            shape = RoundedCornerShape(50),
                            color = liveColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, liveColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.isLiveEnabled) {
                                        Icons.Outlined.Pause
                                    } else {
                                        Icons.Outlined.PlayArrow
                                    },
                                    contentDescription = stringResource(R.string.action_toggle_live),
                                    tint = liveColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isLiveEnabled) {
                                        stringResource(R.string.status_live)
                                    } else {
                                        stringResource(R.string.status_paused)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = liveColor
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (!isDiagnosticsRoute) {
                                    navController.navigate("diagnostics") {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("diagnostics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FactCheck,
                                contentDescription = stringResource(R.string.diag_button_desc),
                                tint = if (isDiagnosticsRoute) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleTheme(useDarkTheme) },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (useDarkTheme) {
                                    Icons.Outlined.LightMode
                                } else {
                                    Icons.Outlined.DarkMode
                                },
                                contentDescription = stringResource(R.string.action_toggle_theme),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setPrivacySheetVisible(true) },
                            modifier = Modifier.testTag("privacy_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = stringResource(R.string.action_privacy_info),
                                tint = telemetryColors.optimal
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        moduleDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == dest.module.route
                            } == true
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToModule(dest.module) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_item_${dest.module.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.weight(0.15f))
                        moduleDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == dest.module.route
                            } == true
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToModule(dest.module) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("rail_item_${dest.module.route}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = NeovModule.SOC.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                    ) {
                        composable(NeovModule.SOC.route) {
                            SocScreen(
                                cpuInfo = uiState.cpuInfo,
                                clockHistoryMhz = uiState.cpuClockHistoryMhz,
                                loadHistory = uiState.cpuLoadHistory
                            )
                        }
                        composable(NeovModule.SYSTEM.route) {
                            SystemScreen(systemInfo = uiState.systemInfo)
                        }
                        composable(NeovModule.MEMORY.route) {
                            MemoryScreen(
                                memoryInfo = uiState.memoryInfo,
                                ramHistory = uiState.ramUsageHistory
                            )
                        }
                        composable(NeovModule.BATTERY.route) {
                            BatteryScreen(
                                batteryInfo = uiState.batteryInfo,
                                tempHistory = uiState.batteryTempHistory
                            )
                        }
                        composable(NeovModule.SENSORS.route) {
                            SensorsScreen(
                                sensors = uiState.sensors,
                                liveReadings = liveSensorReadings,
                                monitoredIds = uiState.monitoredSensorIds,
                                expandedSensorId = uiState.expandedSensorId,
                                searchQuery = uiState.sensorSearchQuery,
                                selectedCategory = uiState.selectedSensorCategory,
                                onSearchChange = viewModel::updateSensorSearch,
                                onCategorySelect = viewModel::selectSensorCategory,
                                onToggleExpand = viewModel::toggleSensorExpand,
                                onToggleMonitor = viewModel::toggleSensorMonitor
                            )
                        }
                        composable("diagnostics") {
                            DiagnosticsScreen(
                                cpuInfo = uiState.cpuInfo,
                                systemInfo = uiState.systemInfo,
                                memoryInfo = uiState.memoryInfo,
                                batteryInfo = uiState.batteryInfo,
                                sensors = uiState.sensors,
                                liveReadings = liveSensorReadings
                            )
                        }
                    }
                }
            }
        }
    }
}
