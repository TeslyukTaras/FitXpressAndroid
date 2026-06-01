package com.hexis.bi.ui.main.home.longevity.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.theme.MeasurementTitleValueStyle
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun LongevityScoreCard(
    score: Int,
    syncedDate: String,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.longevity_score_card_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        val scoreText = stringResource(R.string.longevity_score_value, score)
        val maxText = stringResource(R.string.longevity_score_max)
        val valueColor = MaterialTheme.colorScheme.primary
        val maxColor = MaterialTheme.colorScheme.onSurfaceVariant
        val maxStyle = MaterialTheme.typography.bodyLarge
        Text(
            text = buildAnnotatedString {
                withStyle(MeasurementTitleValueStyle.toSpanStyle().copy(color = valueColor)) {
                    append(scoreText)
                }
                withStyle(maxStyle.toSpanStyle().copy(color = maxColor)) {
                    append(" ")
                    append(maxText)
                }
            },
            style = maxStyle,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        Text(
            text = stringResource(R.string.longevity_score_description),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        Text(
            text = stringResource(R.string.longevity_synced_format, syncedDate),
            style = TitleDimTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
