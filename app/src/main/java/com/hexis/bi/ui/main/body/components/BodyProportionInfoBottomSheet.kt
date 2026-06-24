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
internal fun BodyProportionInfoBottomSheet(
    onDismiss: () -> Unit,
) {
    BaseBottomSheet(
        title = stringResource(R.string.body_proportion_info_title),
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(0.85f),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.body_proportion_info_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ProportionInfoSection(
                heading = R.string.body_proportion_info_upper_heading,
                items = listOf(
                    R.string.body_proportion_label_shoulder_waist to
                        R.string.body_proportion_info_shoulder_waist_body,
                    R.string.body_proportion_label_shoulder_hip to
                        R.string.body_proportion_info_shoulder_hip_body,
                ),
            )
            ProportionInfoSection(
                heading = R.string.body_proportion_info_mid_heading,
                items = listOf(
                    R.string.body_proportion_label_waist_height to
                        R.string.body_proportion_info_waist_height_body,
                    R.string.body_proportion_label_waist_hip to
                        R.string.body_proportion_info_waist_hip_body,
                ),
            )
            ProportionInfoSection(
                heading = R.string.body_proportion_info_lower_heading,
                items = listOf(
                    R.string.body_proportion_label_thigh_waist to
                        R.string.body_proportion_info_thigh_waist_body,
                    R.string.body_proportion_label_calf_thigh to
                        R.string.body_proportion_info_calf_thigh_body,
                ),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            Text(
                text = stringResource(R.string.body_proportion_info_note_heading),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Text(
                text = stringResource(R.string.body_proportion_info_note_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun ProportionInfoSection(
    heading: Int,
    items: List<Pair<Int, Int>>,
) {
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    Text(
        text = stringResource(heading),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    items.forEach { (label, body) ->
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
