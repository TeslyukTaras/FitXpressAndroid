package com.hexis.bi.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hexis.bi.R
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.components.AppButton
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.components.AppOutlinedButton
import com.hexis.bi.ui.components.MainNavBottomBar
import com.hexis.bi.ui.main.body.BodyScreen
import com.hexis.bi.ui.main.home.HomeScreen
import com.hexis.bi.ui.main.home.activity.ActivityScreen
import com.hexis.bi.ui.main.home.recovery.RecoveryScreen
import com.hexis.bi.ui.main.home.sleep.SleepScreen
import com.hexis.bi.ui.main.notifications.NotificationsScreen
import com.hexis.bi.ui.main.scan.ScanScreen
import com.hexis.bi.ui.main.scan.results.ResultsScreen
import com.hexis.bi.ui.main.settings.SettingsScreen
import com.hexis.bi.ui.main.settings.editprofile.EditProfileScreen
import com.hexis.bi.ui.main.settings.healthconnections.HealthConnectionsScreen
import com.hexis.bi.ui.main.settings.mysuit.MySuitScreen
import com.hexis.bi.ui.main.settings.notifications.NotificationsSettingsScreen
import com.hexis.bi.ui.main.settings.scanpreferences.ScanPreferencesScreen
import com.hexis.bi.ui.navigation.Route
import com.hexis.bi.ui.navigation.popBackStackOnce
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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

    val userRepository: UserRepository = koinInject()
    val scope = rememberCoroutineScope()
    var showProfileIncompleteDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        onRecoveryClick = { navController.navigate(Route.Main.RECOVERY) },
                        onActivityClick = { navController.navigate(Route.Main.ACTIVITY) },
                    )
                }
                composable(Route.Main.SLEEP) {
                    SleepScreen(onBack = { navController.popBackStackOnce() })
                }
                composable(Route.Main.RECOVERY) {
                    RecoveryScreen(onBack = { navController.popBackStackOnce() })
                }
                composable(Route.Main.ACTIVITY) {
                    ActivityScreen(onBack = { navController.popBackStackOnce() })
                }
                composable(Route.Main.SCAN) {
                    ScanScreen(
                        onBack = { navController.popBackStackOnce() },
                        onScanComplete = {
                            navController.navigate(Route.Main.SCAN_RESULTS) {
                                popUpTo(Route.Main.SCAN) { inclusive = true }
                            }
                        },
                        onConnectSuit = { navController.navigate(Route.Main.MY_SUIT) },
                        onBuySuit = {},
                    )
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
                    scope.launch {
                        val profile = userRepository.getUser().getOrNull()
                        val isComplete = profile != null
                                && profile.heightCm != null
                                && profile.weightKg != null
                                && profile.dateOfBirth != null
                        if (isComplete) {
                            navController.navigate(Route.Main.SCAN) {
                                launchSingleTop = true
                            }
                        } else {
                            showProfileIncompleteDialog = true
                        }
                    }
                },
            )
        }
        }

        if (showProfileIncompleteDialog) {
            ProfileIncompleteDialog(
                onDismiss = { showProfileIncompleteDialog = false },
                onGoToProfile = {
                    showProfileIncompleteDialog = false
                    navController.navigate(Route.Main.EDIT_PROFILE) {
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}

@Composable
private fun ProfileIncompleteDialog(
    onDismiss: () -> Unit,
    onGoToProfile: () -> Unit,
) {
    AppDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_large)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.profile_incomplete_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

            Text(
                text = stringResource(R.string.profile_incomplete_dialog_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
            ) {
                AppOutlinedButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = stringResource(R.string.profile_incomplete_dialog_action),
                    onClick = onGoToProfile,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
