package com.hexis.bi.ui.main.scan.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.data.scan.TopChangeVsPrevious
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppDatePicker
import com.hexis.bi.ui.components.AppHorizontalGradientDivider
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.ui.main.scan.results.MeasurementChange
import com.hexis.bi.utils.cmToInches
import org.koin.androidx.compose.koinViewModel
import com.hexis.bi.ui.theme.NocturnePulseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryScreen(
    onBack: () -> Unit,
    onOpenScan: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCalendarClick: () -> Unit = {},
    viewModel: ScanHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LightStatusBarIcons()

    Box(modifier = modifier.fillMaxSize()) {
        BaseScreen(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (state.showDateRangePicker)
                        Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    else Modifier
                )
                .screenBackground(),
            containerColor = Color.Transparent,
            isLoading = state.isLoading,
            error = state.error,
            onDismissError = viewModel::clearStateError,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.scan_history_title),
                    background = Color.Transparent,
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = {
                            onCalendarClick()
                            viewModel.showDateRangePicker()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = stringResource(R.string.cd_scan_history_calendar),
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium_small)),
                            )
                        }
                    },
                )
            },
        ) {

            if (!state.isLoading && state.items.isEmpty()) Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val hasDateFilter = state.selectedStartDateMillis != null ||
                        state.selectedEndDateMillis != null
                Text(
                    text = stringResource(
                        if (hasDateFilter) R.string.scan_history_empty_for_range
                        else R.string.body_scan_history_empty
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
                    textAlign = TextAlign.Center,
                )
            } else LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = dimensionResource(R.dimen.spacer_s),
                    bottom = dimensionResource(R.dimen.spacer_3xl),
                ),
                verticalArrangement = Arrangement.Top,
            ) {
                itemsIndexed(state.items, key = { _, item -> item.scanId }) { index, item ->
                    ScanHistoryRow(
                        item = item,
                        isMetric = state.isMetric,
                        showDivider = true, //previously index < state.items.lastIndex,
                        onClick = { onOpenScan(item.scanId) },
                    )
                }
            }
        }

        if (state.showDateRangePicker) {
            val pickerState = rememberDateRangePickerState(
                initialSelectedStartDateMillis = state.selectedStartDateMillis,
                initialSelectedEndDateMillis = state.selectedEndDateMillis,
            )
            val isWithinLimit = isScanHistoryRangeWithinLimit(
                pickerState.selectedStartDateMillis,
                pickerState.selectedEndDateMillis,
            )
            AppDatePicker(
                state = pickerState,
                onDismissRequest = viewModel::hideDateRangePicker,
                onReset = viewModel::resetDateRange,
                onSelect = { startDateMillis, endDateMillis ->
                    viewModel.applyDateRange(
                        startDateMillis,
                        endDateMillis,
                    )
                },
                modifier = Modifier.widthIn(max = dimensionResource(R.dimen.app_date_picker_dialog_max_width)),
                isRangeValid = isWithinLimit,
                rangeErrorText = stringResource(
                    R.string.scan_history_date_range_limit,
                    SCAN_HISTORY_MAX_RANGE_DAYS,
                ),
            )
        }
    }
}

@Composable
private fun ScanHistoryRow(
    item: ScanHistoryListItem,
    isMetric: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.padding_medium),
                    vertical = dimensionResource(R.dimen.spacer_m)
                ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m))
        ) {
            Text(
                text = "${item.dateLabel}, ${item.timeLabel}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m))
            ) {
                if (item.topChange != null) {
                    Text(
                        text = stringResource(R.string.scan_history_top_change_prefix),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = topChangeAnnotated(item.topChange, isMetric),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                } else Text(
                    text = stringResource(R.string.scan_history_top_change_dash),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showDivider) AppHorizontalGradientDivider()
    }
}

@Composable
private fun topChangeAnnotated(
    topChange: TopChangeVsPrevious,
    isMetric: Boolean
): AnnotatedString {
    return buildAnnotatedString {
        append(stringResource(topChange.bodyPartRes))
        append(" ")
        val valueColor = when (topChange.change) {
            MeasurementChange.Positive -> NocturnePulseTheme.extendedColors.green
            MeasurementChange.Negative -> MaterialTheme.colorScheme.error
            null -> MaterialTheme.colorScheme.onSurface
        }
        val deltaStringRes = when {
            topChange.deltaCm > 0f -> R.string.format_delta_up
            topChange.deltaCm < 0f -> R.string.format_delta_down
            else -> R.string.format_delta_neutral
        }
        val deltaValue = if (isMetric) topChange.deltaCm else topChange.deltaCm.cmToInches()
        val unit = stringResource(if (isMetric) R.string.unit_cm else R.string.unit_in)
        withStyle(SpanStyle(color = valueColor)) {
            append(stringResource(deltaStringRes, deltaValue, unit))
        }
    }
}
