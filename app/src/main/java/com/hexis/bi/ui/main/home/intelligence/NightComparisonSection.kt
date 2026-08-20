package com.hexis.bi.ui.main.home.intelligence

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.theme.NocturnePulseTheme

@Composable
fun NightComparisonSection(
    comparisons: List<NightComparison>,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = comparisons.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Column {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
            BodyGlassCard(
                contentPadding = PaddingValues(dimensionResource(R.dimen.insight_card_padding)),
                shape = RoundedCornerShape(dimensionResource(R.dimen.insight_card_corner)),
            ) {
                Text(
                    text = comparisons.first().title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.insight_card_value_gap)))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacer_xxs),
                    ),
                ) {
                    comparisons.forEach { comparison -> NightComparisonRow(comparison) }
                }
            }
        }
    }
}

@Composable
private fun NightComparisonRow(comparison: NightComparison) {
    val muted = NocturnePulseTheme.extendedColors.gray200
    val unitSize = MaterialTheme.typography.bodyMedium.fontSize
    val usual = buildAnnotatedString {
        comparison.usual.forEach { part ->
            if (part.muted) {
                withStyle(SpanStyle(color = muted, fontSize = unitSize)) { append(part.text) }
            } else {
                append(part.text)
            }
        }
        if (comparison.unit.isNotBlank()) {
            withStyle(SpanStyle(color = muted, fontSize = unitSize)) { append(" ${comparison.unit}") }
        }
    }
    val actual = buildAnnotatedString {
        comparison.value.forEach { part ->
            if (part.muted) {
                withStyle(SpanStyle(color = muted, fontSize = unitSize)) { append(part.text) }
            } else {
                append(part.text)
            }
        }
    }
    Text(
        text = comparison.template
            .replace(METRIC_PLACEHOLDER, "${comparison.label.replaceFirstChar { it.uppercase() }} $actual")
            .replace(USUAL_PLACEHOLDER, usual.text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}
