package com.hexis.bi.ui.main.home.sleep

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseBottomSheet
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.dark.DarkTabSelector
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.main.home.sleep.components.SleepRecoverySheetBody
import com.hexis.bi.ui.main.home.sleep.components.SleepSettingsDialogContent
import com.hexis.bi.ui.theme.dark.DarkTheme
import org.koin.androidx.compose.koinViewModel

/** Fraction of the screen height occupied by the "Sleep and Recovery" info sheet. */
private const val RECOVERY_SHEET_HEIGHT_FRACTION = 0.8f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SleepViewModel = koinViewModel(),
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
                        if (state.showSettingsDialog || state.showRecoverySheet)
                            Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                        else Modifier
                    )
                    .darkScreenBackground(),
                containerColor = Color.Transparent,
                isLoading = isLoading,
                error = error,
                onDismissError = viewModel::clearError,
                topBar = {
                    BaseTopBar(
                        title = stringResource(R.string.sleep_screen_title),
                        onBack = onBack,
                        background = Color.Transparent,
                        actions = {
                            IconButton(onClick = viewModel::showSettingsDialog) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings),
                                    contentDescription = stringResource(R.string.cd_settings),
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                                )
                            }
                        },
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
                        tabs = SleepTab.entries,
                        selectedTab = state.selectedTab,
                        onTabSelected = viewModel::selectTab,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    when (state.selectedTab) {
                        SleepTab.Day -> SleepDayContent(
                            state = state,
                            onInfoClick = viewModel::showRecoverySheet,
                            onPreviousDay = viewModel::previousDay,
                            onNextDay = viewModel::nextDay,
                            onRetry = viewModel::retryLoad,
                        )

                        SleepTab.Summary -> SleepSummaryContent(
                            state = state,
                            onInfoClick = viewModel::showRecoverySheet,
                            onPreviousWeek = viewModel::previousWeek,
                            onNextWeek = viewModel::nextWeek,
                            onRetry = viewModel::retrySummaryLoad,
                        )
                    }

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xl)))
                }
            }

            if (state.showSettingsDialog) {
                AppDialog(onDismiss = viewModel::dismissSettingsDialog) {
                    SleepSettingsDialogContent(
                        sleepGoalHours = state.sleepGoalHoursDraft,
                        dataSource = state.dataSource,
                        onGoalChange = viewModel::updateSleepGoalDraft,
                        onCancel = viewModel::dismissSettingsDialog,
                        onSave = viewModel::saveSettings,
                    )
                }
            }

            if (state.showRecoverySheet) {
                BaseBottomSheet(
                    title = stringResource(R.string.sleep_recovery_sheet_title),
                    onDismiss = viewModel::dismissRecoverySheet,
                    modifier = modifier.fillMaxHeight(RECOVERY_SHEET_HEIGHT_FRACTION),
                ) {
                    SleepRecoverySheetBody(onDismiss = viewModel::dismissRecoverySheet)
                }
            }
        }
    }
}
