package com.hexis.bi.ui.main.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.ui.main.scan.startscan.StartScanScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onScanComplete: () -> Unit,
    onConnectSuit: () -> Unit,
    onBuySuit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.suitConnected) {
        StartScanScreen(
            onBack = onBack,
            onScanComplete = onScanComplete,
            modifier = modifier,
        )
    } else {
        ScanErrorScreen(
            onBack = onBack,
            onConnectSuit = onConnectSuit,
            onBuySuit = onBuySuit,
            modifier = modifier,
        )
    }
}
