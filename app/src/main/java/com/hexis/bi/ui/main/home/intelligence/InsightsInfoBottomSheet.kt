package com.hexis.bi.ui.main.home.intelligence

import androidx.annotation.StringRes
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsInfoBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseBottomSheet(
        title = stringResource(R.string.insights_info_title),
        onDismiss = onDismiss,
        modifier = modifier.fillMaxHeight(0.8f),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.insights_info_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            InfoSection(R.string.insights_info_heading_1, R.string.insights_info_body_1)
            InfoSection(R.string.insights_info_heading_2, R.string.insights_info_body_2)

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            ConfidenceLegend(R.string.insights_info_confidence_high, FindingConfidence.HIGH)
            ConfidenceLegend(R.string.insights_info_confidence_medium, FindingConfidence.MEDIUM)
            ConfidenceLegend(R.string.insights_info_confidence_low, FindingConfidence.LOW)

            InfoSection(R.string.insights_info_heading_3, R.string.insights_info_body_3)
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
private fun InfoSection(@StringRes heading: Int, @StringRes body: Int) {
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

@Composable
private fun ConfidenceLegend(@StringRes text: Int, confidence: FindingConfidence) {
    val label = stringResource(text)
    val name = stringResource(confidence.labelRes)
    val tint: Color = confidence.chipColor
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = tint)) { append(name) }
            append(label.removePrefix(name))
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
