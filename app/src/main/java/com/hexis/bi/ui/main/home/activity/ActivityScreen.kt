package com.hexis.bi.ui.main.home.activity

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.hexis.bi.ui.main.home.activity.components.ActivitySettingsDialogContent
import com.hexis.bi.ui.theme.dark.DarkTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LightStatusBarIcons()

    DarkTheme {
        Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier
                .then(
                    if (state.showInfoSheet || state.showSettingsDialog)
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
                    title = stringResource(R.string.activity_screen_title),
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
                    tabs = ActivityTab.entries,
                    selectedTab = state.selectedTab,
                    onTabSelected = viewModel::selectTab,
                    tabLabel = { stringResource(it.labelRes) },
                    modifier = Modifier.fillMaxWidth(),
                )

                when (state.selectedTab) {
                    ActivityTab.Day -> ActivityDayContent(
                        state = state,
                        onInfoClick = viewModel::showInfoSheet,
                        onPreviousDay = viewModel::previousDay,
                        onNextDay = viewModel::nextDay,
                        onRetry = viewModel::retryDayLoad,
                    )

                    ActivityTab.Week -> ActivityWeekContent(
                        state = state,
                        onPreviousWeek = viewModel::previousWeek,
                        onNextWeek = viewModel::nextWeek,
                        onSelectWeekDay = viewModel::selectWeekDay,
                        onClearWeekDay = viewModel::clearWeekDaySelection,
                        onInfoClick = viewModel::showInfoSheet,
                        onRetry = viewModel::retryWeekLoad,
                    )

                    ActivityTab.Month -> ActivityMonthContent(
                        state = state,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth,
                        onRetry = viewModel::retryMonthLoad,
                    )

                    ActivityTab.Year -> ActivityYearContent(
                        state = state,
                        onPreviousYear = viewModel::previousYear,
                        onNextYear = viewModel::nextYear,
                        onRetry = viewModel::retryYearLoad,
                    )
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
            }
        }

        if (state.showSettingsDialog) {
            AppDialog(onDismiss = viewModel::dismissSettingsDialog) {
                ActivitySettingsDialogContent(
                    stepsGoal = state.stepsGoalDraft,
                    showActiveCalories = state.showActiveCaloriesDraft,
                    dataSource = state.dataSourceName,
                    onStepsGoalChange = viewModel::updateStepsGoalDraft,
                    onShowActiveCaloriesChange = viewModel::updateActiveCaloriesDraft,
                    onCancel = viewModel::dismissSettingsDialog,
                    onSave = viewModel::saveSettings,
                )
            }
        }

        if (state.showInfoSheet) {
            BaseBottomSheet(
                title = stringResource(R.string.activity_info_sheet_title),
                onDismiss = viewModel::dismissInfoSheet,
                modifier = modifier.fillMaxHeight(0.75f),
            ) {
                Text(
                    text = stringResource(R.string.activity_info_heading_1),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
                Text(
                    text = stringResource(R.string.activity_info_body_1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                Text(
                    text = stringResource(R.string.activity_info_heading_2),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
                Text(
                    text = stringResource(R.string.activity_info_body_2),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                Text(
                    text = stringResource(R.string.activity_info_heading_3),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
                Text(
                    text = stringResource(R.string.activity_info_body_3),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                Text(
                    text = stringResource(R.string.activity_info_heading_4),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
                Text(
                    text = stringResource(R.string.activity_info_body_4),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

                TextButton(
                    onClick = viewModel::dismissInfoSheet,
                    modifier = Modifier.align(Alignment.End),
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
    }
}
