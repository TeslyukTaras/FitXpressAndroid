package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.home.ScanOverview
import com.hexis.bi.ui.theme.NocturnePulseTheme

@Composable
internal fun ScanOverviewCard(
    data: ScanOverview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_card_scan),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.home_scan_spacer)))

                val numberStyle = MaterialTheme.typography.titleLarge.toSpanStyle()
                    .copy(color = MaterialTheme.colorScheme.onBackground)
                val unitStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
                    .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                val labelColor = when (data.changePositive) {
                    true -> NocturnePulseTheme.extendedColors.green
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val labelStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
                    .copy(color = labelColor)
                Text(
                    text = buildAnnotatedString {
                        withStyle(numberStyle) { append(data.value) }
                        data.unit?.let {
                            append(" ")
                            withStyle(unitStyle) { append(it) }
                        }
                        data.valueLabel?.let {
                            append(" ")
                            withStyle(labelStyle) { append(it) }
                        }
                    },
                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

                Text(
                    text = data.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))

            ScanSparkline(
                points = data.trend,
                modifier = Modifier
                    .weight(1f)
                    .height(dimensionResource(R.dimen.home_sparkline_height)),
            )
        }
    }
}
