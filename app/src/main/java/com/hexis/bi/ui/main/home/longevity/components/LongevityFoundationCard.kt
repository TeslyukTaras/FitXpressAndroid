package com.hexis.bi.ui.main.home.longevity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.hexis.bi.ui.components.AppVerticalGradientDivider
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.home.longevity.LongevityEvidenceUi
import com.hexis.bi.ui.main.home.longevity.LongevityFoundationUi
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun LongevityFoundationCard(
    foundation: LongevityFoundationUi,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(dimensionResource(R.dimen.spacer_m))
                    .height(dimensionResource(R.dimen.border_thin))
                    .background(MaterialTheme.colorScheme.onBackground, CircleShape),
            )

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))

            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(foundation.titleRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = stringResource(foundation.status.labelRes),
                style = TitleDimTextStyle,
                color = statusColor(foundation.status),
            )
        }

        if (foundation.evidence.isEmpty()) return@BodyGlassCard

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            foundation.evidence.forEachIndexed { index, evidence ->
                if (index > 0) {
                    Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
                    AppVerticalGradientDivider()
                    Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
                }
                EvidenceCell(evidence, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EvidenceCell(evidence: LongevityEvidenceUi, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = evidence.label,
            style = MaterialTheme.typography.bodyMedium,
            color = NocturnePulseTheme.extendedColors.gray200,
        )

        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))

        Text(
            text = buildAnnotatedString {
                withStyle(
                    MaterialTheme.typography.labelLarge
                        .copy(color = MaterialTheme.colorScheme.onSurface)
                        .toSpanStyle()
                ) { append(evidence.value) }
                if (evidence.unit.isNotBlank()) withStyle(
                    TitleDimTextStyle
                        .copy(color = NocturnePulseTheme.extendedColors.gray200)
                        .toSpanStyle()
                ) {
                    append(" ")
                    append(evidence.unit)
                }
            },
        )
    }
}
