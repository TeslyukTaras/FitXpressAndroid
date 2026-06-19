package com.hexis.bi.ui.main.home.paceofaging.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun PaceOfAgingInsightCard(
    insight: String,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.pace_of_aging_insight_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        Text(
            text = insight,
            style = TitleDimTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
