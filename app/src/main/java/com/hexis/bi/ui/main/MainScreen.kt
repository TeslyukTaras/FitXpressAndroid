package com.hexis.bi.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hexis.bi.ui.components.MainNavBottomBar
import com.hexis.bi.ui.main.body.BodyScreen
import com.hexis.bi.ui.main.home.HomeScreen
import com.hexis.bi.ui.main.settings.SettingsScreen
import com.hexis.bi.ui.main.settings.editprofile.EditProfileScreen
import com.hexis.bi.ui.main.notifications.NotificationsScreen
import com.hexis.bi.ui.main.settings.notifications.NotificationsSettingsScreen
import com.hexis.bi.ui.navigation.Route

@Composable
fun MainScreen(
    onLogout: () -> Unit,
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
                    )
                }
                composable(Route.Main.BODY) {
                    BodyScreen()
                }
                composable(Route.Main.NOTIFICATIONS) {
                    NotificationsScreen(onBack = { navController.popBackStack() })
                }
                composable(Route.Main.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onLogout = onLogout,
                        onNavigateToEditProfile = { navController.navigate(Route.Main.EDIT_PROFILE) },
                        onNavigateToNotificationSettings = { navController.navigate(Route.Main.NOTIFICATION_SETTINGS) },
                    )
                }
                composable(Route.Main.EDIT_PROFILE) {
                    EditProfileScreen(onBack = { navController.popBackStack() })
                }
                composable(Route.Main.NOTIFICATION_SETTINGS) {
                    NotificationsSettingsScreen(onBack = { navController.popBackStack() })
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
                onScanClick = { /* TODO: navigate to scan */ },
            )
        }
    }
}
