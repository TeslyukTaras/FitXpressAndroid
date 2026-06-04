package com.hexis.bi.ui.main.home.paceofaging.components

import androidx.annotation.StringRes
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
import com.hexis.bi.R
import com.hexis.bi.ui.dark.AppVerticalGradientDivider
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.home.longevity.LongevityTrend
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun PaceOfAgingStatsCard(
    waistTrend: LongevityTrend?,
    bodyFat: String?,
    modifier: Modifier = Modifier,
) {
    val unknown = stringResource(R.string.stat_unknown)
    BodyGlassCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCell(
                labelRes = R.string.pace_of_aging_stat_waist,
                value = waistTrend?.let { stringResource(it.labelRes) } ?: unknown,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            AppVerticalGradientDivider()
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            StatCell(
                labelRes = R.string.pace_of_aging_stat_body_fat,
                value = bodyFat ?: unknown,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCell(
    modifier: Modifier = Modifier,
    @StringRes labelRes: Int,
    value: String,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(labelRes),
            style = TitleDimTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
