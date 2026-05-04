package com.hexis.bi.ui.main.scan.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.hexis.bi.ui.main.scan.results.MeasurementChange
import com.hexis.bi.ui.theme.GrayText
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.ui.theme.OneTimeGrey
import com.hexis.bi.ui.theme.Red100
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScanHistoryScreen(
    onBack: () -> Unit,
    onOpenScan: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCalendarClick: () -> Unit = {},
    viewModel: ScanHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dateRangeText = state.dateRangeText
    val background = MaterialTheme.colorScheme.background

    BaseScreen(
        modifier = modifier,
        containerColor = background,
        isLoading = state.isLoading,
        error = state.error,
        onDismissError = viewModel::clearStateError,
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(background),
            ) {
                BaseTopBar(
                    title = stringResource(R.string.scan_history_title),
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = onCalendarClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = stringResource(R.string.cd_scan_history_calendar),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    },
                )
                if (dateRangeText != null) Text(
                    text = dateRangeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.padding_small)),
                    textAlign = TextAlign.Center,
                )
            }
        },
    ) {

        if (!state.isLoading && state.items.isEmpty()) Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.body_scan_history_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
            )
        }
        else LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = dimensionResource(R.dimen.padding_medium),
                    start = dimensionResource(R.dimen.padding_medium),
                    end = dimensionResource(R.dimen.padding_medium)
                ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
        ) {
            items(state.items, key = { it.scanId }) { item ->
                ScanHistoryCard(
                    item = item,
                    onClick = { onOpenScan(item.scanId) },
                )
            }
        }
    }
}

@Composable
private fun ScanHistoryCard(
    item: ScanHistoryListItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(OneTimeGrey)
            .clickable(onClick = onClick)
            .padding(dimensionResource(R.dimen.spacer_m)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(dimensionResource(R.dimen.scan_history_thumb_width))
                .clip(MaterialTheme.shapes.extraSmall),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_body_filled),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_normalized)),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.size(dimensionResource(R.dimen.spacer_m)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.dateLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
            Text(
                text = item.timeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
            Text(
                text = topChangeAnnotated(item.topChange),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun topChangeAnnotated(topChange: TopChangeVsPrevious?): AnnotatedString {
    val labelColor = MaterialTheme.colorScheme.secondary
    val nameColor = MaterialTheme.colorScheme.onSurface
    return buildAnnotatedString {
        withStyle(SpanStyle(color = labelColor)) {
            append(stringResource(R.string.scan_history_top_change_prefix))
            append(" ")
        }
        if (topChange == null) {
            withStyle(SpanStyle(color = nameColor)) { append(stringResource(R.string.scan_history_top_change_dash)) }
            return@buildAnnotatedString
        }
        withStyle(SpanStyle(color = nameColor)) {
            append(stringResource(topChange.bodyPartRes))
            append(" ")
        }
        val valueColor = when (topChange.change) {
            MeasurementChange.Positive -> Green
            MeasurementChange.Negative -> Red100
            null -> MaterialTheme.colorScheme.onSurface
        }
        withStyle(SpanStyle(color = valueColor)) {
            append(
                stringResource(
                    R.string.format_delta_neutral,
                    topChange.deltaCm,
                    stringResource(R.string.unit_cm),
                ),
            )
        }
    }
}
