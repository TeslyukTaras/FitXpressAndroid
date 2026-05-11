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
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.ui.theme.Red100
import java.util.Locale
import kotlin.math.abs

/** Trend arrow + signed delta (e.g. "↗ + 0.6"). Colour: secondary when flat, green when improving, red otherwise. */
@Composable
internal fun BodyDelta(
    delta: Float,
    decreaseIsPositive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val rising = delta > 0f
    val color = when {
        delta == 0f -> MaterialTheme.colorScheme.secondary
        rising != decreaseIsPositive -> Green
        else -> Red100
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_3xs)),
    ) {
        Icon(
            painter = painterResource(if (rising) R.drawable.ic_trend_up else R.drawable.ic_trend_down),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
        )
        Text(
            text = formatDelta(delta),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}

/** Signed delta without a unit, e.g. "+ 0.6" / "− 1.2". */
@Composable
internal fun formatDelta(delta: Float): String =
    stringResource(R.string.delta_value, deltaSign(delta), deltaMagnitude(delta))

/** Signed delta with a unit suffix, e.g. "+ 0.4 kg" / "− 0.6 %". */
@Composable
internal fun formatDelta(delta: Float, unit: String): String =
    stringResource(R.string.delta_value_with_unit, deltaSign(delta), deltaMagnitude(delta), unit)

@Composable
private fun deltaSign(delta: Float): String =
    stringResource(if (delta >= 0f) R.string.delta_sign_positive else R.string.delta_sign_negative)

private fun deltaMagnitude(delta: Float): String = String.format(Locale.US, "%.1f", abs(delta))
