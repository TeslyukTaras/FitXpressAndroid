package com.hexis.bi.ui.main.subscription.plans

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.RestoreResult
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.main.subscription.components.RestoreResultDialog
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.utils.legal.LegalUrls
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlanSelectionScreen(
    onBack: () -> Unit,
    onClose: () -> Unit,
    onContinue: (PlanId) -> Unit,
    onRestored: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanSelectionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LightStatusBarIcons()

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (state.restore != null) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    else Modifier
                )
                .screenBackground(),
            containerColor = Color.Transparent,
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::dismissError,
            viewModel = viewModel,
            topBar = { PlanSelectionTopBar(isChangeMode = state.isChangeMode, onBack = onBack, onClose = onClose) },
            bottomBar = {
                PlanSelectionBottomBar(
                    selected = state.selected,
                    isChangeMode = state.isChangeMode,
                    canContinue = state.canContinue,
                    onContinue = {
                        state.selected?.let { plan ->
                            viewModel.confirmPlan(plan)
                            onContinue(plan)
                        }
                    },
                    onRestore = viewModel::restorePurchases,
                    onOpenTerms = { uriHandler.openUri(LegalUrls.TERMS_AND_CONDITIONS) },
                    onOpenPrivacy = { uriHandler.openUri(LegalUrls.PRIVACY_POLICY) },
                )
            },
        ) {
            PlanSelectionBody(
                plans = state.plans,
                selected = state.selected,
                currentPlan = state.currentPlan,
                onSelect = viewModel::selectPlan,
            )
        }

        state.restore?.let { outcome ->
            RestoreResultDialog(
                outcome = outcome,
                onDismiss = {
                    viewModel.dismissRestore()
                    if (outcome.result == RestoreResult.RESTORED) onRestored()
                },
                onRetry = {
                    viewModel.dismissRestore()
                    viewModel.restorePurchases()
                },
            )
        }
    }
}
