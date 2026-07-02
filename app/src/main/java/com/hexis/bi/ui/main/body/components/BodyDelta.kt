package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.TitleHighlightTextStyle
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun BodyDelta(
    modifier: Modifier = Modifier,
    delta: Float,
    decreaseIsPositive: Boolean = false,
) {
    val rising = delta > 0f
    val color = when {
        delta == 0f -> MaterialTheme.colorScheme.onSurfaceVariant
        rising != decreaseIsPositive -> NocturnePulseTheme.extendedColors.positive
        else -> NocturnePulseTheme.extendedColors.negative
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xxs)),
    ) {
        Icon(
            painter = painterResource(if (rising) R.drawable.ic_trend_up else R.drawable.ic_trend_down),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
        )
        Text(
            text = formatDelta(delta),
            style = TitleHighlightTextStyle,
            color = color,
        )
    }
}

@Composable
internal fun formatDelta(delta: Float): String =
    stringResource(R.string.delta_value, deltaSign(delta), deltaMagnitude(delta))

@Composable
internal fun formatDelta(delta: Float, unit: String): String =
    stringResource(R.string.delta_value_with_unit, deltaSign(delta), deltaMagnitude(delta), unit)

@Composable
private fun deltaSign(delta: Float): String =
    stringResource(if (delta >= 0f) R.string.delta_sign_positive else R.string.delta_sign_negative)

private fun deltaMagnitude(delta: Float): String = String.format(Locale.US, "%.1f", abs(delta))
