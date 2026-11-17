package com.hexis.bi.ui.main.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.config.AppConfig
import com.hexis.bi.domain.subscription.ScanAccess
import com.hexis.bi.ui.main.scan.access.FirstFreeScanDialog
import com.hexis.bi.ui.main.scan.access.PaymentIssueScanDialog
import com.hexis.bi.ui.main.scan.access.ScanAccessViewModel
import com.hexis.bi.ui.main.scan.access.ScanGateBackdrop
import com.hexis.bi.ui.main.scan.access.SubscriptionRequiredDialog
import com.hexis.bi.ui.main.scan.error.ScanErrorScreen
import com.hexis.bi.ui.main.scan.startscan.StartScanScreen
import com.hexis.bi.ui.main.subscription.openManageSubscription
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onScanComplete: () -> Unit,
    onConnectSuit: () -> Unit,
    onBuySuit: () -> Unit,
    onShowHowToScan: () -> Unit,
    onOpenScanPreferences: () -> Unit,
    onChoosePlan: () -> Unit,
    onScansUsed: () -> Unit,
    modifier: Modifier = Modifier,
    scanPurpose: ScanPurpose = ScanPurpose.BodyScan,
    requireConnectedSuit: Boolean = true,
    viewModel: ScanViewModel = koinViewModel(),
    accessViewModel: ScanAccessViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val access by accessViewModel.access.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (!AppConfig.suitVisible && scanPurpose == ScanPurpose.SuitSizeScan) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    if (!AppConfig.suitVisible || !requireConnectedSuit || state.suitConnected) when (access) {
        null -> ScanGateBackdrop(onBack = onBack, blurred = false, modifier = modifier)
        ScanAccess.Allowed -> StartScanScreen(
            onBack = onBack,
            onScanComplete = onScanComplete,
            onShowHowToScan = onShowHowToScan,
            onOpenScanPreferences = onOpenScanPreferences,
            modifier = modifier,
            scanPurpose = scanPurpose,
            onScanBlocked = accessViewModel::onScanBlocked,
        )

        ScanAccess.FirstFreeScan -> {
            ScanGateBackdrop(onBack = onBack, blurred = true, modifier = modifier)
            FirstFreeScanDialog(
                onContinue = accessViewModel::onFirstFreeScanAccepted,
                onMaybeLater = onBack,
                onViewGuide = onShowHowToScan,
            )
        }

        ScanAccess.SubscriptionRequired -> {
            ScanGateBackdrop(onBack = onBack, blurred = true, modifier = modifier)
            SubscriptionRequiredDialog(onChoosePlan = onChoosePlan, onMaybeLater = onBack)
        }

        ScanAccess.PaymentIssue -> {
            ScanGateBackdrop(onBack = onBack, blurred = true, modifier = modifier)
            PaymentIssueScanDialog(
                onResolve = { openManageSubscription(context, accessViewModel.storePlatform) },
                onMaybeLater = onBack,
            )
        }

        ScanAccess.AllScansUsed -> {
            ScanGateBackdrop(onBack = onBack, blurred = false, modifier = modifier)
            LaunchedEffect(Unit) { onScansUsed() }
        }
    }
    else ScanErrorScreen(
        onBack = onBack,
        onConnectSuit = onConnectSuit,
        onBuySuit = onBuySuit,
        onShowHowToScan = onShowHowToScan,
        modifier = modifier,
    )
}
