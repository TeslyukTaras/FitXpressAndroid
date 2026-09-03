package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun PhysiqueMetricCard(
    title: String,
    value: String?,
    modifier: Modifier = Modifier,
    caption: String? = null,
    highlighted: Boolean = false,
    loading: Boolean = false,
) {
    BodyGlassCard(
        modifier = modifier,
        highlighted = highlighted,
        loading = loading,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
        Spacer(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = value ?: stringResource(R.string.stat_unknown),
                style = MaterialTheme.typography.headlineMedium,
                color = if (value == null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.alignByBaseline(),
            )
            if (caption != null) {
                Spacer(Modifier.size(dimensionResource(R.dimen.spacer_xxs)))
                Text(
                    text = caption,
                    style = TitleDimTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}
