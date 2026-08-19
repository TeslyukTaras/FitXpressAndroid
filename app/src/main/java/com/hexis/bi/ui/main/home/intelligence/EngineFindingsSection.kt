package com.hexis.bi.ui.main.home.intelligence

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.theme.NocturnePulseTheme

@Composable
fun EngineFindingsSection(
    state: EngineFindingsState,
    modifier: Modifier = Modifier,
    updating: Boolean = false,
    animateUpdatingDots: Boolean = true,
    topSpacing: Dp = dimensionResource(R.dimen.spacer_l),
) {
    Column(modifier = modifier) {
        InsightsUpdatingHeader(visible = updating, animateDots = animateUpdatingDots)
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "engine-findings",
        ) { current ->
            when (current) {
            is EngineFindingsState.Ready -> Column {
                Spacer(Modifier.height(topSpacing))
                BodyGlassCard(
                    contentPadding = PaddingValues(dimensionResource(R.dimen.insight_card_padding)),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.insight_card_corner)),
                ) {
                    InsightHeader(stringResource(R.string.engine_findings_title), current.confidence)
                    if (current.values.isNotEmpty()) {
                        Spacer(Modifier.height(dimensionResource(R.dimen.insight_card_value_gap)))
                        InsightValues(current.values, showLabels = true)
                    }
                    current.rows.forEach { row ->
                        Spacer(Modifier.height(dimensionResource(R.dimen.insight_card_text_gap)))
                        FindingRow(row)
                    }
                }
            }

                EngineFindingsState.Empty -> if (showEngineFindingsEmpty(updating)) {
                    Column {
                        Spacer(Modifier.height(topSpacing))
                        BodyGlassCard(
                            contentPadding = PaddingValues(dimensionResource(R.dimen.insight_card_padding)),
                            shape = RoundedCornerShape(dimensionResource(R.dimen.insight_card_corner)),
                        ) { EmptyFindings() }
                    }
                } else {
                    Spacer(Modifier)
                }

                EngineFindingsState.Hidden -> Spacer(Modifier)
            }
        }
    }
}

@Composable
internal fun InsightHeader(title: String, confidence: FindingConfidence) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(confidence.labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = confidence.chipColor,
        )
    }
}

@Composable
internal fun InsightValues(values: List<FindingValue>, showLabels: Boolean) {
    val bright = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val muted = NocturnePulseTheme.extendedColors.gray200
    val unitSize = MaterialTheme.typography.bodyMedium.fontSize
    val arrow = stringResource(R.string.engine_findings_value_arrow)
    val labelSeparator = stringResource(R.string.engine_findings_label_separator)
    val separator = stringResource(R.string.engine_findings_value_separator)
    val rowGap = dimensionResource(R.dimen.spacer_xxs)

    val texts = values.map { value ->
        buildAnnotatedString {
            if (showLabels && value.label.isNotBlank()) {
                withStyle(SpanStyle(color = bright)) {
                    append(value.label.toInsightLabel())
                    append(labelSeparator)
                }
            }
            value.from.forEach { part ->
                withStyle(part.span(bright, muted, unitSize)) { append(part.text) }
            }
            withStyle(SpanStyle(color = bright)) { append(" $arrow ") }
            value.to.forEach { part ->
                withStyle(part.span(accent, muted, unitSize)) { append(part.text) }
            }
            if (value.unit.isNotBlank()) {
                withStyle(SpanStyle(color = muted, fontSize = unitSize)) {
                    append(" ${value.unit}")
                }
            }
        }
    }

    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            texts.forEachIndexed { index, text ->
                if (index > 0) {
                    Text(text = separator, style = MaterialTheme.typography.bodyLarge, color = muted)
                }
                Text(text = text, style = MaterialTheme.typography.bodyLarge)
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val rowGapPx = rowGap.roundToPx()
        var measurableIndex = 0
        val separators = mutableListOf<androidx.compose.ui.layout.Placeable?>()
        val valuePlaceables = mutableListOf<androidx.compose.ui.layout.Placeable>()
        texts.indices.forEach { index ->
            separators += if (index > 0) measurables[measurableIndex++].measure(loose) else null
            valuePlaceables += measurables[measurableIndex++].measure(loose)
        }
        val separatorSize = separators.filterNotNull().firstOrNull()?.let {
            InsightValueSize(it.width, it.height)
        } ?: InsightValueSize(0, 0)
        val result = calculateInsightValueLayout(
            values = valuePlaceables.map { InsightValueSize(it.width, it.height) },
            separator = separatorSize,
            maxWidth = constraints.maxWidth,
            rowGap = rowGapPx,
        )

        layout(constraints.maxWidth, result.height) {
            result.positions.forEachIndexed { index, position ->
                if (position.separatorX != null && position.separatorY != null) {
                    separators[index]?.placeRelative(position.separatorX, position.separatorY)
                }
                valuePlaceables[index].placeRelative(position.valueX, position.valueY)
            }
        }
    }
}

private fun String.toInsightLabel(): String = split(' ').joinToString(" ") { word ->
    word.replaceFirstChar { it.uppercase() }
}

private fun ValuePart.span(value: Color, muted: Color, unitSize: TextUnit): SpanStyle =
    if (this.muted) SpanStyle(color = muted, fontSize = unitSize) else SpanStyle(color = value)

@Composable
private fun EmptyFindings() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.engine_findings_empty_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        Text(
            text = stringResource(R.string.engine_findings_empty_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FindingRow(row: EngineFindingRow) {
    Column(modifier = Modifier.fillMaxWidth()) {
        row.heading?.let { heading ->
            Text(
                text = heading,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (row.explanation.isNotBlank()) {
            if (row.heading != null) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
            }
            Text(
                text = row.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal val FindingConfidence.labelRes: Int
    get() = when (this) {
        FindingConfidence.HIGH -> R.string.engine_findings_confidence_high
        FindingConfidence.MEDIUM -> R.string.engine_findings_confidence_medium
        FindingConfidence.LOW -> R.string.engine_findings_confidence_low
    }

internal val FindingConfidence.chipColor: Color
    @Composable get() = when (this) {
        FindingConfidence.HIGH -> MaterialTheme.colorScheme.primary
        FindingConfidence.MEDIUM -> NocturnePulseTheme.extendedColors.accentBlue
        FindingConfidence.LOW -> NocturnePulseTheme.extendedColors.gray200
    }
