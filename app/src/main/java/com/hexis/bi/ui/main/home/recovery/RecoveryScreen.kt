package com.hexis.bi.ui.main.home.recovery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.dark.DarkTabSelector
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.main.home.recovery.components.RecoveryInfoBottomSheet
import com.hexis.bi.ui.theme.dark.DarkTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecoveryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LightStatusBarIcons()

    DarkTheme {
        Box(modifier = modifier) {
            BaseScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (state.showInfoSheet) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                        else Modifier
                    )
                    .darkScreenBackground(),
                containerColor = Color.Transparent,
                isLoading = isLoading,
                error = error,
                onDismissError = viewModel::clearError,
                topBar = {
                    BaseTopBar(
                        title = stringResource(R.string.recovery_screen_title),
                        onBack = onBack,
                        background = Color.Transparent,
                    )
                },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                ) {
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

                    DarkTabSelector(
                        tabs = RecoveryTab.entries,
                        selectedTab = state.selectedTab,
                        onTabSelected = viewModel::selectTab,
                        tabLabel = { stringResource(it.labelRes) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    when (state.selectedTab) {
                        RecoveryTab.Day -> {
                            RecoveryDayContent(
                                state = state,
                                onInfoClick = viewModel::showInfoSheet,
                                onPreviousDay = viewModel::previousDay,
                                onNextDay = viewModel::nextDay,
                            )
                        }

                        RecoveryTab.Summary -> {
                            RecoverySummaryContent(
                                state = state,
                                onPreviousWeek = viewModel::previousWeek,
                                onNextWeek = viewModel::nextWeek,
                                onRetry = viewModel::retrySummaryLoad,
                            )
                        }
                    }

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xl)))
                }
            }

            if (state.showInfoSheet) RecoveryInfoBottomSheet(onDismiss = viewModel::dismissInfoSheet)
        }
    }
}
