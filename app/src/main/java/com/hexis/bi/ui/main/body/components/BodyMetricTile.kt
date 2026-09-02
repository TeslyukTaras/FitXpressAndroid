package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.utils.constants.ChangeTolerance

@Composable
internal fun BodyMetricTile(
    label: String,
    value: String,
    delta: Float?,
    tolerance: ChangeTolerance,
    modifier: Modifier = Modifier,
    decreaseIsPositive: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = TitleDimTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (delta != null) BodyDelta(
                delta = delta,
                tolerance = tolerance,
                decreaseIsPositive = decreaseIsPositive,
            )
        }
    }
}
