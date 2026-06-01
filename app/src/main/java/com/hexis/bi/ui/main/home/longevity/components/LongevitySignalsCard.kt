package com.hexis.bi.ui.main.home.longevity.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.dark.AppHorizontalGradientDivider
import com.hexis.bi.ui.dark.AppVerticalGradientDivider
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.home.longevity.LongevitySignal
import com.hexis.bi.ui.theme.TitleDimTextStyle

/** Titled card with the six numeric longevity signals in a 2×3 grid, split by gradient dividers. */
@Composable
internal fun LongevitySignalsCard(
    signals: List<LongevitySignal>,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.longevity_signals_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        signals.chunked(SIGNALS_PER_ROW).forEachIndexed { index, rowSignals ->
            if (index > 0) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
                AppHorizontalGradientDivider()
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
            }
            SignalRow(rowSignals)
        }
    }
}

/** Untitled card with the three longevity status signals in a single row, split by dividers. */
@Composable
internal fun LongevityStatusCard(
    signals: List<LongevitySignal>,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier) {
        SignalRow(signals)
    }
}

@Composable
private fun SignalRow(signals: List<LongevitySignal>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        signals.forEachIndexed { index, signal ->
            if (index > 0) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
                AppVerticalGradientDivider()
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
            }
            SignalCell(signal = signal, modifier = Modifier.weight(1f))
        }
        repeat(SIGNALS_PER_ROW - signals.size) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SignalCell(signal: LongevitySignal, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(signal.labelRes),
            style = TitleDimTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        val unit = signal.unitRes?.let { stringResource(it) }
        val valueSpan =
            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium).toSpanStyle()
                .copy(color = MaterialTheme.colorScheme.primary)
        val labelSpan = MaterialTheme.typography.bodyLarge.toSpanStyle()
            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = buildAnnotatedString {
                // Digits read as the value; letters and symbols (h, m, %) read as muted labels.
                appendClassifiedValue(signal.value, valueSpan, labelSpan)
                if (unit != null) withStyle(labelSpan) {
                    append(" ")
                    append(unit)
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

private fun AnnotatedString.Builder.appendClassifiedValue(
    text: String,
    valueSpan: SpanStyle,
    labelSpan: SpanStyle,
) {
    if (text.isEmpty()) return
    // Status words (no digits, e.g. "Improving") are the value itself — render them all primary.
    if (text.none { it.isValueChar() }) {
        withStyle(valueSpan) { append(text) }
        return
    }
    var runStart = 0
    var runIsValue = text[0].isValueChar()
    for (i in 1..text.length) {
        val atEnd = i == text.length
        val charIsValue = if (atEnd) runIsValue else text[i].isValueChar()
        if (atEnd || charIsValue != runIsValue) {
            withStyle(if (runIsValue) valueSpan else labelSpan) {
                append(text.substring(runStart, i))
            }
            if (!atEnd) {
                runStart = i
                runIsValue = charIsValue
            }
        }
    }
}

/** Digits and number separators belong to the value; everything else is a muted label. */
private fun Char.isValueChar(): Boolean = isDigit() || this == '.' || this == ','

private const val SIGNALS_PER_ROW = 3
