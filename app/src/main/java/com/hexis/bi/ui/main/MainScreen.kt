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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hexis.bi.R
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.components.AppMainNavBottomBar
import com.hexis.bi.ui.components.AppOutlinedButton
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.my_suit.BuySuitDialogContent
import com.hexis.bi.ui.main.body.BodyScreen
import com.hexis.bi.ui.main.body.PhysiqueBalanceScreen
import com.hexis.bi.ui.main.buysuit.editaddress.EditAddressScreen
import com.hexis.bi.ui.main.buysuit.shipping.ShippingDetailsScreen
import com.hexis.bi.ui.main.buysuit.suitsize.SuitSizeResultsScreen
import com.hexis.bi.ui.main.home.HomeScreen
import com.hexis.bi.ui.main.home.activity.ActivityScreen
import com.hexis.bi.ui.main.home.longevity.LongevityScreen
import com.hexis.bi.ui.main.home.paceofaging.PaceOfAgingScreen
import com.hexis.bi.ui.main.home.physiquedrift.PhysiqueDriftScreen
import com.hexis.bi.ui.main.home.recomposition.RecompositionScreen
import com.hexis.bi.ui.main.home.recovery.RecoveryScreen
import com.hexis.bi.ui.main.home.sleep.SleepScreen
import com.hexis.bi.ui.main.notifications.NotificationsScreen
import com.hexis.bi.ui.main.scan.ScanPurpose
import com.hexis.bi.ui.main.scan.ScanScreen
import com.hexis.bi.ui.main.scan.history.ScanHistoryScreen
import com.hexis.bi.ui.main.scan.howtoscan.HowToScanScreen
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
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = Route.Main.HOME,
    onExit: () -> Unit = {},
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val showBottomBar = currentRoute in Route.Main.tabRoutes

    val userRepository: UserRepository = koinInject()
    val scanResultRepository: ScanResultRepository = koinInject()
    val scope = rememberCoroutineScope()
    var showProfileIncompleteDialog by remember { mutableStateOf(false) }
    var showBuySuitDialog by remember { mutableStateOf(false) }
    val startSuitSizeScan: () -> Unit = {
        scope.launch {
            val profile = userRepository.getUser().getOrNull()
            val isComplete = profile != null
                    && profile.heightCm != null
                    && profile.gender != null
                    && profile.dateOfBirth != null
            showBuySuitDialog = false
            if (isComplete) {
                navController.navigate(Route.Main.SUIT_SIZE_SCAN) {
                    launchSingleTop = true
                }
            } else {
                showProfileIncompleteDialog = true
            }
        }
    }

    // Starts a scan only when the profile is complete; otherwise prompts to finish the profile.
    // Shared by the bottom-nav scan button and the Home "Scan" tile.
    val launchScan: () -> Unit = {
        scope.launch {
            val profile = userRepository.getUser().getOrNull()
            val isComplete = profile != null
                    && profile.heightCm != null
                    && profile.weightKg != null
                    && profile.gender != null
                    && profile.dateOfBirth != null
            if (isComplete) {
                navController.navigate(Route.Main.SCAN) {
                    launchSingleTop = true
                }
            } else {
                showProfileIncompleteDialog = true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val dialogBlurModifier =
            if (showProfileIncompleteDialog || showBuySuitDialog) {
                Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
            } else {
                Modifier
            }

        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .then(dialogBlurModifier),
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(Route.Main.HOME) {
                HomeScreen(
                    onLogout = onLogout,
                    onNotificationClick = { navController.navigate(Route.Main.NOTIFICATIONS) },
                    onSettingsClick = { navController.navigate(Route.Main.SETTINGS) },
                    onSleepClick = { navController.navigate(Route.Main.SLEEP) },
                    onRecompositionClick = { navController.navigate(Route.Main.RECOMPOSITION) },
                    onLongevityClick = { navController.navigate(Route.Main.LONGEVITY) },
                    onPhysiqueDriftClick = { navController.navigate(Route.Main.PHYSIQUE_DRIFT) },
                    onPaceOfAgingClick = { navController.navigate(Route.Main.PACE_OF_AGING) },
                    onActivityClick = { navController.navigate(Route.Main.ACTIVITY) },
                    onScanClick = launchScan,
                    onBuySuitClick = { showBuySuitDialog = true },
                    onEditOrderAddress = { orderId ->
                        navController.navigate(Route.Main.editOrderAddress(orderId))
                    },
                )
            }
            composable(Route.Main.SLEEP) {
                SleepScreen(onBack = { navController.popBackStackOnce() })
            }
            composable(Route.Main.RECOVERY) {
                RecoveryScreen(onBack = { navController.popBackStackOnce() })
            }
            composable(Route.Main.RECOMPOSITION) {
                RecompositionScreen(onBack = { navController.popBackStackOnce() })
            }
            composable(Route.Main.LONGEVITY) {
                LongevityScreen(onBack = { navController.popBackStackOnce() })
            }
            composable(Route.Main.PHYSIQUE_DRIFT) {
                PhysiqueDriftScreen(onBack = { navController.popBackStackOnce() })
            }
            composable(Route.Main.PACE_OF_AGING) {
                PaceOfAgingScreen(onBack = { navController.popBackStackOnce() })
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
                    onBuySuit = { showBuySuitDialog = true },
                    onShowHowToScan = { navController.navigate(Route.Main.HOW_TO_SCAN) },
                    onOpenScanPreferences = { navController.navigate(Route.Main.SCAN_PREFERENCES) },
                )
            }
            composable(Route.Main.SUIT_SIZE_SCAN) {
                ScanScreen(
                    onBack = { if (!navController.popBackStackOnce()) onExit() },
                    onScanComplete = {
                        navController.navigate(Route.Main.SUIT_SIZE_RESULTS) {
                            popUpTo(Route.Main.SUIT_SIZE_SCAN) { inclusive = true }
                        }
                    },
                    onConnectSuit = { navController.navigate(Route.Main.MY_SUIT) },
                    onBuySuit = { showBuySuitDialog = true },
                    onShowHowToScan = { navController.navigate(Route.Main.HOW_TO_SCAN) },
                    onOpenScanPreferences = { navController.navigate(Route.Main.SCAN_PREFERENCES) },
                    scanPurpose = ScanPurpose.SuitSizeScan,
                    requireConnectedSuit = false,
                )
            }
            composable(Route.Main.SCAN_RESULTS) {
                ResultsScreen(
                    onBack = {
                        scanResultRepository.selectedScanId = null
                        navController.popBackStackOnce()
                    },
                    onOpenScanPreferences = { navController.navigate(Route.Main.SCAN_PREFERENCES) },
                )
            }
            composable(Route.Main.SUIT_SIZE_RESULTS) {
                SuitSizeResultsScreen(
                    onBack = { navController.popBackStackOnce() },
                    onProceedToOrder = { navController.navigate(Route.Main.SHIPPING_DETAILS) },
                )
            }
            composable(Route.Main.SHIPPING_DETAILS) {
                ShippingDetailsScreen(
                    onBack = { navController.popBackStackOnce() },
                    onClose = { navController.popBackStack(Route.Main.HOME, inclusive = false) },
                )
            }
            composable(
                Route.Main.EDIT_ORDER_ADDRESS,
                arguments = listOf(
                    navArgument(Route.Main.ARG_ORDER_ID) { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString(Route.Main.ARG_ORDER_ID).orEmpty()
                EditAddressScreen(
                    orderId = orderId,
                    onBack = { navController.popBackStackOnce() },
                    onSaved = { navController.popBackStackOnce() },
                )
            }
            composable(Route.Main.SCAN_HISTORY) {
                ScanHistoryScreen(
                    onBack = { navController.popBackStackOnce() },
                    onOpenScan = { scanId ->
                        scanResultRepository.selectedScanId = scanId
                        navController.navigate(Route.Main.SCAN_RESULTS)
                    },
                )
            }
            composable(Route.Main.BODY) {
                BodyScreen(
                    onHistoryClick = { navController.navigate(Route.Main.SCAN_HISTORY) },
                    onPhysiqueBalanceClick = {
                        navController.navigate(Route.Main.PHYSIQUE_BALANCE)
                    },
                )
            }
            composable(Route.Main.PHYSIQUE_BALANCE) {
                // Reuse the Body screen's ViewModel so the loaded scans and the
                // selected time range carry over instead of refetching.
                val bodyEntry = remember(it) { navController.getBackStackEntry(Route.Main.BODY) }
                PhysiqueBalanceScreen(
                    onBack = { navController.popBackStackOnce() },
                    viewModel = koinViewModel(viewModelStoreOwner = bodyEntry),
                )
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
                    onNavigateToHowToScan = { navController.navigate(Route.Main.HOW_TO_SCAN) },
                )
            }
            composable(Route.Main.HOW_TO_SCAN) {
                HowToScanScreen(onBack = { navController.popBackStackOnce() })
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
                MySuitScreen(
                    onBack = { navController.popBackStackOnce() },
                    onBuyOne = { showBuySuitDialog = true },
                )
            }
        }

        if (showBottomBar) {
            val isHomeSelected = currentRoute == Route.Main.HOME
            val isBodySelected = currentRoute == Route.Main.BODY
            val onHomeClick: () -> Unit = {
                navController.navigate(Route.Main.HOME) {
                    launchSingleTop = true
                    popUpTo(Route.Main.HOME) { inclusive = false }
                }
            }
            val onBodyClick: () -> Unit = {
                navController.navigate(Route.Main.BODY) {
                    launchSingleTop = true
                    popUpTo(Route.Main.HOME) { inclusive = false }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .then(dialogBlurModifier),
            ) {
                AppMainNavBottomBar(
                    isHomeSelected = isHomeSelected,
                    isBodySelected = isBodySelected,
                    onHomeClick = onHomeClick,
                    onBodyClick = onBodyClick,
                    onScanClick = launchScan,
                    hazeAlpha = 1f,
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

        if (showBuySuitDialog) {
            BuySuitDialog(
                onDismiss = { showBuySuitDialog = false },
                onBuySuit = startSuitSizeScan,
            )
        }
    }
}

@Composable
private fun BuySuitDialog(
    onDismiss: () -> Unit,
    onBuySuit: () -> Unit,
) {
    AppDialog(onDismiss = onDismiss) {
        BuySuitDialogContent(onBuySuit = onBuySuit)
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
                .padding(
                    vertical = dimensionResource(R.dimen.padding_large),
                    horizontal = dimensionResource(R.dimen.padding_medium),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
                text = stringResource(R.string.profile_incomplete_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            Text(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
                text = stringResource(R.string.profile_incomplete_dialog_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.padding_small)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
            ) {
                AppOutlinedButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                AppPrimaryButton(
                    text = stringResource(R.string.profile_incomplete_dialog_action),
                    onClick = onGoToProfile,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
