package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BisInfoBottomSheet(
    onDismiss: () -> Unit,
) {
    BaseBottomSheet(
        title = stringResource(R.string.body_bis_sheet_title),
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(0.76f),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.body_bis_sheet_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PhysiqueInfoSection(
                heading = R.string.body_bis_sheet_heading_drift,
                body = R.string.body_bis_sheet_body_drift,
            )
            PhysiqueInfoSection(
                heading = R.string.body_bis_sheet_heading_next_scan,
                body = R.string.body_bis_sheet_body_next_scan,
            )
            PhysiqueInfoSection(
                heading = R.string.body_bis_sheet_heading_next_scan_estimate,
                body = R.string.body_bis_sheet_body_next_scan_estimate,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
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
}

@Composable
private fun PhysiqueInfoSection(
    heading: Int,
    body: Int,
) {
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    Text(
        text = stringResource(heading),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
    Text(
        text = stringResource(body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
