package com.hexis.bi.ui.main.home.sleep.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R

/**
 * Body of the "Sleep and Recovery" info sheet: a scrollable list of numbered sections with a
 * pinned "Got it" action. Designed to live inside [com.hexis.bi.ui.base.BaseBottomSheet].
 */
@Composable
fun ColumnScope.SleepRecoverySheetBody(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.sleep_recovery_sheet_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RECOVERY_SECTIONS.forEach { (heading, body) ->
            SheetSection(headingRes = heading, bodyRes = body)
        }
    }

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    TextButton(
        onClick = onDismiss,
        modifier = Modifier.align(Alignment.End),
    ) {
        Text(
            text = stringResource(R.string.action_got_it),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SheetSection(
    @StringRes headingRes: Int,
    @StringRes bodyRes: Int,
) {
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
    Text(
        text = stringResource(headingRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
    Text(
        text = stringResource(bodyRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val RECOVERY_SECTIONS = listOf(
    R.string.sleep_recovery_sheet_heading_status to R.string.sleep_recovery_sheet_body_status,
    R.string.sleep_recovery_sheet_heading_timeline to R.string.sleep_recovery_sheet_body_timeline,
    R.string.sleep_recovery_sheet_heading_stages to R.string.sleep_recovery_sheet_body_stages,
    R.string.sleep_recovery_sheet_heading_metrics to R.string.sleep_recovery_sheet_body_metrics,
    R.string.sleep_recovery_sheet_heading_hrv to R.string.sleep_recovery_sheet_body_hrv,
    R.string.sleep_recovery_sheet_heading_rhr to R.string.sleep_recovery_sheet_body_rhr,
    R.string.sleep_recovery_sheet_heading_weekly to R.string.sleep_recovery_sheet_body_weekly,
)
