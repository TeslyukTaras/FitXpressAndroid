package com.hexis.bi.ui.main.home.recovery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseBottomSheet
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppTabSelector
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

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier.then(
                if (state.showInfoSheet) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                else Modifier
            ),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.recovery_screen_title),
                    onBack = onBack,
                    background = MaterialTheme.colorScheme.surfaceVariant,
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

                AppTabSelector(
                    tabs = RecoveryTab.entries,
                    selectedTab = state.selectedTab,
                    onTabSelected = viewModel::selectTab,
                )

                when (state.selectedTab) {
                    RecoveryTab.Day -> {
                        RecoveryDayContent(
                            state = state,
                            onInfoClick = viewModel::showInfoSheet,
                            onPreviousDay = viewModel::previousDay,
                            onNextDay = viewModel::nextDay,
                            onRetry = viewModel::retryDayLoad,
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

        if (state.showInfoSheet) {
            BaseBottomSheet(
                title = stringResource(R.string.recovery_info_sheet_title),
                onDismiss = viewModel::dismissInfoSheet,
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
                    onClick = viewModel::dismissInfoSheet,
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
}
