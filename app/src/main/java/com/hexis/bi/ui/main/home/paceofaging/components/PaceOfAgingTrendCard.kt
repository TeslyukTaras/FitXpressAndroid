package com.hexis.bi.ui.main.home.paceofaging.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.home.paceofaging.PaceOfAgingState
import com.hexis.bi.ui.theme.MeasurementTitleValueStyle
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.TitleHighlightTextStyle

@Composable
internal fun PaceOfAgingTrendCard(
    modifier: Modifier = Modifier,
    state: PaceOfAgingState,
) {
    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(
            top = dimensionResource(R.dimen.spacer_m),
            start = dimensionResource(R.dimen.spacer_m),
            end = dimensionResource(R.dimen.spacer_m),
            bottom = dimensionResource(R.dimen.spacer_l),
        )
    ) {
        Text(
            text = stringResource(R.string.pace_of_aging_trend_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        if (state.hasData) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.paceText,
                    style = MeasurementTitleValueStyle.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xl)))
                Column {
                    Text(
                        text = state.percentText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
                    Text(
                        text = stringResource(R.string.pace_of_aging_than_average),
                        style = TitleDimTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            PaceOfAgingMeter(fraction = state.meterFraction)

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
        }

        Text(
            text = state.description,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        Text(
            text = stringResource(R.string.pace_of_aging_synced_format, state.syncedDate),
            style = TitleHighlightTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
