package com.hexis.bi.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.terra.TerraCallbackHandler
import com.hexis.bi.data.terra.TerraSdkSync
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.ui.navigation.AppNavGraph
import com.hexis.bi.ui.theme.FitXpressTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()
    private val firebaseAuth: FirebaseAuth by inject()
    private val terraCallbackHandler: TerraCallbackHandler by inject()

    // Re-init Terra on sign-in/out so the SDK stays bound to the current Firebase UID.
    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        lifecycleScope.launch {
            TerraManagerHolder.init(activity = this@MainActivity, referenceId = uid)
                .onFailure { Timber.e(it, "Terra re-init after auth change failed") }
                .onSuccess {
                    TerraSdkSync.syncLinkedConnections(
                        TerraManagerHolder.current,
                        reason = "auth_state",
                        force = false,
                    )
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !viewModel.isReady.value }
        firebaseAuth.addAuthStateListener(authListener)
        handleTerraDeepLink(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ),
        )
        setContent {
            FitXpressTheme {
                AppNavGraph(modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val uid = firebaseAuth.currentUser?.uid
        lifecycleScope.launch {
            TerraManagerHolder.init(activity = this@MainActivity, referenceId = uid)
                .onFailure { Timber.e(it, "Terra foreground init failed") }
                .onSuccess {
                    TerraSdkSync.syncLinkedConnections(
                        TerraManagerHolder.current,
                        reason = "foreground",
                        force = false,
                    )
                }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTerraDeepLink(intent)
    }

    override fun onDestroy() {
        firebaseAuth.removeAuthStateListener(authListener)
        super.onDestroy()
    }

    private fun handleTerraDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        lifecycleScope.launch { terraCallbackHandler.handle(data) }
    }
}
