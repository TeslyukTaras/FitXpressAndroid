package com.hexis.bi.ui

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.reminder.ScanReminderScheduler
import com.hexis.bi.data.health.sync.HealthSyncCoordinator
import com.hexis.bi.data.health.sync.HealthSyncTrigger
import com.hexis.bi.data.health.sync.HealthSyncScheduler
import com.hexis.bi.data.terra.TerraCallbackHandler
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.data.terra.TerraSdkConnectionOwnership
import com.hexis.bi.data.terra.TerraSdkSync
import com.hexis.bi.ui.navigation.AppNavGraph
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.permissions.NotificationPermissionCoordinator
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()
    private val firebaseAuth: FirebaseAuth by inject()
    private val terraCallbackHandler: TerraCallbackHandler by inject()
    private val terraManagerHolder: TerraManagerHolder by inject()
    private val notificationPermissionCoordinator: NotificationPermissionCoordinator by inject()
    private val scanReminderScheduler: ScanReminderScheduler by inject()
    private val healthSyncCoordinator: HealthSyncCoordinator by inject()
    private val healthSyncScheduler: HealthSyncScheduler by inject()
    private val terraSdkConnectionOwnership: TerraSdkConnectionOwnership by inject()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val uid = firebaseAuth.currentUser?.uid ?: return@registerForActivityResult
        lifecycleScope.launch {
            notificationPermissionCoordinator.onPermissionResult(uid, granted)
        }
    }

    private var lastAuthUid: String? = null

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        val identityChanged = uid != lastAuthUid
        lastAuthUid = uid
        // Re-init Terra on sign-in/out so the SDK stays bound to the current Firebase UID.
        if (identityChanged) {
            lifecycleScope.launch {
                terraManagerHolder.init(activity = this@MainActivity, referenceId = uid)
                    .onFailure { Timber.e(it, "Terra re-init after auth change failed") }
                    .onSuccess { pullOwnedSdkConnections(reason = "auth_state") }
            }
            if (uid != null) startHealthSync(HealthSyncTrigger.SignIn)
        }
        if (uid != null) lifecycleScope.launch {
            notificationPermissionCoordinator.reconcilePushSetting()
            if (notificationPermissionCoordinator.shouldPromptOnSignIn(uid) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            scanReminderScheduler.onNotificationSettingsOrScanChanged()
        }
        else lifecycleScope.launch {
            scanReminderScheduler.onNotificationSettingsOrScanChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !viewModel.isReady.value }
        lastAuthUid = firebaseAuth.currentUser?.uid
        firebaseAuth.addAuthStateListener(authListener)
        handleTerraDeepLink(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrim = Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(scrim = Color.TRANSPARENT),
        )
        setContent {
            NocturnePulseTheme {
                AppNavGraph(modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val uid = firebaseAuth.currentUser?.uid
        lifecycleScope.launch {
            terraManagerHolder.init(activity = this@MainActivity, referenceId = uid)
                .onFailure { Timber.e(it, "Terra foreground init failed") }
                .onSuccess { pullOwnedSdkConnections(reason = "foreground") }
        }
        startHealthSync(
            if (firstForegroundHandled) {
                HealthSyncTrigger.Foreground
            } else {
                firstForegroundHandled = true
                HealthSyncTrigger.Launch
            },
        )
    }

    private fun startHealthSync(trigger: HealthSyncTrigger) {
        lifecycleScope.launch {
            if (firebaseAuth.currentUser == null) return@launch
            healthSyncScheduler.enqueueHistoryBackfill(trigger)
            healthSyncScheduler.schedulePeriodicSync()
            healthSyncCoordinator.syncRecentWindow()
        }
    }

    private suspend fun pullOwnedSdkConnections(reason: String) {
        if (firebaseAuth.currentUser == null) return
        val owned = terraSdkConnectionOwnership.syncableSdkUserIds().getOrElse { error ->
            Timber.w(
                error,
                "IDENTITY-OWNERSHIP sdk: ownership unknown for %s (%s); skipping the pull",
                firebaseAuth.currentUser?.uid ?: "signed-out",
                reason,
            )
            return
        }
        TerraSdkSync.syncLinkedConnections(
            manager = terraManagerHolder.current,
            ownedUserIds = owned,
            reason = reason,
            force = false,
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTerraDeepLink(intent)
    }

    override fun onDestroy() {
        firebaseAuth.removeAuthStateListener(authListener)
        terraManagerHolder.clearLocalManager()
        super.onDestroy()
    }

    private fun handleTerraDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        lifecycleScope.launch { terraCallbackHandler.handle(data) }
    }

    private companion object {
        var firstForegroundHandled = false
    }
}
