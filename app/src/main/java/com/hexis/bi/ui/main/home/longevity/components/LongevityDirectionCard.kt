package com.hexis.bi.ui.main.home.longevity.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R
import com.hexis.bi.domain.longevity.LongevityDirection
import com.hexis.bi.ui.components.BodyGlassCard

@Composable
internal fun LongevityDirectionCard(
    direction: LongevityDirection,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.longevity_direction_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Text(
            text = stringResource(direction.labelRes),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
            color = directionColor(direction),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Text(
            text = stringResource(direction.descriptionRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
