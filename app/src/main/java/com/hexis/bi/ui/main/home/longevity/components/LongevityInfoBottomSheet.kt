package com.hexis.bi.ui.main.home.longevity.components

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
fun LongevityInfoBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseBottomSheet(
        title = stringResource(R.string.longevity_screen_title),
        onDismiss = onDismiss,
        modifier = modifier.fillMaxHeight(0.8f),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.longevity_info_sheet_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            InfoSection(
                R.string.longevity_info_sheet_heading_1,
                R.string.longevity_info_sheet_body_1
            )
            InfoSection(
                R.string.longevity_info_sheet_heading_2,
                R.string.longevity_info_sheet_body_2
            )
            InfoSection(
                R.string.longevity_info_sheet_heading_3,
                R.string.longevity_info_sheet_body_3
            )
            InfoSection(
                R.string.longevity_info_sheet_heading_4,
                R.string.longevity_info_sheet_body_4
            )
            InfoSection(
                R.string.longevity_info_sheet_heading_5,
                R.string.longevity_info_sheet_body_5
            )
            InfoSection(
                R.string.longevity_info_sheet_heading_6,
                R.string.longevity_info_sheet_body_6
            )
            InfoSection(
                R.string.longevity_info_sheet_heading_7,
                R.string.longevity_info_sheet_body_7
            )
            InfoSection(
                R.string.longevity_info_sheet_heading_8,
                R.string.longevity_info_sheet_body_8
            )
            InfoSection(
                R.string.longevity_info_sheet_heading_9,
                R.string.longevity_info_sheet_body_9
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

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
private fun InfoSection(heading: Int, body: Int) {
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
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
