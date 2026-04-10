package com.hexis.bi.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hexis.bi.ui.components.MainNavBottomBar
import com.hexis.bi.ui.main.body.BodyScreen
import com.hexis.bi.ui.main.home.HomeScreen
import com.hexis.bi.ui.main.home.sleep.SleepScreen
import com.hexis.bi.ui.main.notifications.NotificationsScreen
import com.hexis.bi.ui.main.scan.ScanScreen
import com.hexis.bi.ui.main.scan.ScanViewModel
import com.hexis.bi.ui.main.scan.results.ResultsScreen
import com.hexis.bi.ui.main.scan.startscan.StartScanScreen
import com.hexis.bi.ui.main.settings.SettingsScreen
import com.hexis.bi.ui.main.settings.editprofile.EditProfileScreen
import com.hexis.bi.ui.main.settings.healthconnections.HealthConnectionsScreen
import com.hexis.bi.ui.main.settings.mysuit.MySuitScreen
import com.hexis.bi.ui.main.settings.notifications.NotificationsSettingsScreen
import com.hexis.bi.ui.main.settings.scanpreferences.ScanPreferencesScreen
import com.hexis.bi.ui.navigation.Route
import com.hexis.bi.ui.navigation.popBackStackOnce
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val showBottomBar = currentRoute in Route.Main.tabRoutes

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Route.Main.HOME,
            ) {
                composable(Route.Main.HOME) {
                    HomeScreen(
                        onLogout = onLogout,
                        onNotificationClick = { navController.navigate(Route.Main.NOTIFICATIONS) },
                        onSettingsClick = { navController.navigate(Route.Main.SETTINGS) },
                        onSleepClick = { navController.navigate(Route.Main.SLEEP) },
                    )
                }
                composable(Route.Main.SLEEP) {
                    SleepScreen(onBack = { navController.popBackStackOnce() })
                }
                composable(Route.Main.SCAN) {
                    val scanViewModel: ScanViewModel = koinViewModel()
                    val scanState by scanViewModel.state.collectAsStateWithLifecycle()
                    if (scanState.suitConnected) {
                        StartScanScreen(
                            onBack = { navController.popBackStackOnce() },
                            onScanComplete = {
                                navController.navigate(Route.Main.SCAN_RESULTS) {
                                    popUpTo(Route.Main.SCAN) { inclusive = true }
                                }
                            },
                        )
                    } else {
                        ScanScreen(
                            onBack = { navController.popBackStackOnce() },
                            onConnectSuit = { navController.navigate(Route.Main.MY_SUIT) },
                            onBuySuit = {},
                        )
                    }
                }
                composable(Route.Main.SCAN_RESULTS) {
                    ResultsScreen(onBack = { navController.popBackStackOnce() })
                }
                composable(Route.Main.BODY) {
                    BodyScreen()
                }
                composable(Route.Main.NOTIFICATIONS) {
                    NotificationsScreen(onBack = { navController.popBackStackOnce() })
                }
                composable(Route.Main.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStackOnce() },
                        onLogout = onLogout,
                        onDeleteAccount = onDeleteAccount,
                        onNavigateToEditProfile = { navController.navigate(Route.Main.EDIT_PROFILE) },
                        onNavigateToNotificationSettings = { navController.navigate(Route.Main.NOTIFICATION_SETTINGS) },
                        onNavigateToHealthConnections = { navController.navigate(Route.Main.HEALTH_CONNECTIONS) },
                        onNavigateToScanPreferences = { navController.navigate(Route.Main.SCAN_PREFERENCES) },
                        onNavigateToMySuit = { navController.navigate(Route.Main.MY_SUIT) },
                    )
                }
                composable(Route.Main.EDIT_PROFILE) {
                    EditProfileScreen(onBack = { navController.popBackStackOnce() })
                }
                composable(Route.Main.NOTIFICATION_SETTINGS) {
                    NotificationsSettingsScreen(onBack = { navController.popBackStackOnce() })
                }
                composable(Route.Main.HEALTH_CONNECTIONS) {
                    HealthConnectionsScreen(onBack = { navController.popBackStackOnce() })
                }
                composable(Route.Main.SCAN_PREFERENCES) {
                    ScanPreferencesScreen(onBack = { navController.popBackStackOnce() })
                }
                composable(Route.Main.MY_SUIT) {
                    MySuitScreen(onBack = { navController.popBackStackOnce() })
                }
            }
        }

        if (showBottomBar) {
            MainNavBottomBar(
                isHomeSelected = currentRoute == Route.Main.HOME,
                isBodySelected = currentRoute == Route.Main.BODY,
                onHomeClick = {
                    navController.navigate(Route.Main.HOME) {
                        launchSingleTop = true
                        popUpTo(Route.Main.HOME) { inclusive = false }
                    }
                },
                onBodyClick = {
                    navController.navigate(Route.Main.BODY) {
                        launchSingleTop = true
                        popUpTo(Route.Main.HOME) { inclusive = false }
                    }
                },
                onScanClick = {
                    navController.navigate(Route.Main.SCAN) {
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
