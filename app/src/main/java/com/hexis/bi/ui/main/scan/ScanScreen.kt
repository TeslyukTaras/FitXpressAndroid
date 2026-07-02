package com.hexis.bi.ui.main.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.ui.main.scan.error.ScanErrorScreen
import com.hexis.bi.ui.main.scan.startscan.StartScanScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onScanComplete: () -> Unit,
    onConnectSuit: () -> Unit,
    onBuySuit: () -> Unit,
    onShowHowToScan: () -> Unit,
    onOpenScanPreferences: () -> Unit,
    modifier: Modifier = Modifier,
    scanPurpose: ScanPurpose = ScanPurpose.BodyScan,
    requireConnectedSuit: Boolean = true,
    viewModel: ScanViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (!requireConnectedSuit || state.suitConnected) StartScanScreen(
        onBack = onBack,
        onScanComplete = onScanComplete,
        onShowHowToScan = onShowHowToScan,
        onOpenScanPreferences = onOpenScanPreferences,
        modifier = modifier,
        scanPurpose = scanPurpose,
    )
    else ScanErrorScreen(
        onBack = onBack,
        onConnectSuit = onConnectSuit,
        onBuySuit = onBuySuit,
        onShowHowToScan = onShowHowToScan,
        modifier = modifier,
    )
}
