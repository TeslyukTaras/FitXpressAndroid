package com.hexis.bi.ui.main.home.sleep

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseBottomSheet
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.main.home.sleep.components.SleepSettingsDialogContent
import com.hexis.bi.ui.main.home.sleep.components.SleepTabSelector
import org.koin.androidx.compose.koinViewModel

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

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier.then(
                if (state.showSettingsDialog
                    || state.showRecoverySheet
                ) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                else Modifier
            ),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.sleep_screen_title),
                    onBack = onBack,
                    background = MaterialTheme.colorScheme.surfaceVariant,
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

                SleepTabSelector(
                    selectedTab = state.selectedTab,
                    onTabSelected = viewModel::selectTab,
                )

                when (state.selectedTab) {
                    SleepTab.Day -> {
                        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
                        SleepDayContent(
                            state = state,
                            onInfoClick = viewModel::showRecoverySheet,
                        )
                    }

                    SleepTab.Summary -> {
                        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
                        SleepSummaryContent(
                            state = state,
                            onInfoClick = viewModel::showRecoverySheet,
                            onPreviousWeek = viewModel::previousWeek,
                            onNextWeek = viewModel::nextWeek,
                        )
                    }
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
    }

    if (state.showRecoverySheet) {
        BaseBottomSheet(
            title = stringResource(R.string.sleep_recovery_title),
            onDismiss = viewModel::dismissRecoverySheet,
            modifier = modifier.fillMaxHeight(0.75f),
        ) {
            Text(
                text = stringResource(R.string.sleep_recovery_sheet_heading_1),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Text(
                text = stringResource(R.string.sleep_recovery_sheet_body_1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            Text(
                text = stringResource(R.string.sleep_recovery_sheet_heading_2),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Text(
                text = stringResource(R.string.sleep_recovery_sheet_body_3),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

            TextButton(
                onClick = viewModel::dismissRecoverySheet,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = stringResource(R.string.action_got_it),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
