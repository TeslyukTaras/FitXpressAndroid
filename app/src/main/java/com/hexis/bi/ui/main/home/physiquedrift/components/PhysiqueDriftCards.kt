package com.hexis.bi.ui.main.home.physiquedrift.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppHorizontalGradientDivider
import com.hexis.bi.ui.components.AppVerticalGradientDivider
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.body.components.BodyDelta
import com.hexis.bi.ui.main.home.physiquedrift.PhysiqueDriftLevel
import com.hexis.bi.ui.main.home.physiquedrift.PhysiqueDriftState
import com.hexis.bi.ui.main.home.physiquedrift.PhysiqueMetric
import com.hexis.bi.ui.theme.MeasurementTitleValueStyle
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun PhysiqueScoreCard(
    state: PhysiqueDriftState,
    modifier: Modifier = Modifier,
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
            text = stringResource(R.string.physique_drift_score_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val scoreScale = stringResource(R.string.physique_drift_score_scale)
            val scoreText = buildAnnotatedString {
                withStyle(
                    MeasurementTitleValueStyle
                        .copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        .toSpanStyle(),
                ) {
                    append(state.score)
                }
                withStyle(
                    MaterialTheme.typography.bodyLarge
                        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        .toSpanStyle(),
                ) {
                    append(scoreScale)
                }
            }
            Text(
                text = scoreText,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xl)))
            Column {
                Text(
                    text = stringResource(state.level.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
                Text(
                    text = state.description,
                    style = TitleDimTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Text(
            text = stringResource(R.string.physique_drift_score_body),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        Text(
            text = if (state.scanDate.isBlank()) stringResource(R.string.physique_drift_latest_scan_unknown)
            else stringResource(R.string.physique_drift_latest_scan_format, state.scanDate),
            style = TitleDimTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun PhysiqueCompositionCard(
    bodyFat: PhysiqueMetric,
    leanBody: PhysiqueMetric,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(
            top = dimensionResource(R.dimen.spacer_m),
            start = dimensionResource(R.dimen.spacer_m),
            end = dimensionResource(R.dimen.spacer_m),
            bottom = dimensionResource(R.dimen.spacer_xl),
        )
    ) {
        Text(
            text = stringResource(R.string.physique_drift_body_composition),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricCell(
                labelRes = R.string.physique_drift_body_fat,
                metric = bodyFat,
                decreaseIsPositive = true,
                modifier = Modifier.weight(1f),
            )
            AppVerticalGradientDivider()
            MetricCell(
                labelRes = R.string.physique_drift_lean_body,
                metric = leanBody,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun PhysiqueContributorsCard(
    state: PhysiqueDriftState,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.physique_drift_contributors),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContributorCell(
                Modifier.weight(1f),
                R.string.physique_drift_waist_profile,
                state.waistProfile,
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            AppVerticalGradientDivider()
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            ContributorCell(
                Modifier.weight(1f),
                R.string.physique_drift_proportions,
                state.proportions,
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        AppHorizontalGradientDivider()
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContributorCell(
                Modifier.weight(1f),
                R.string.physique_drift_symmetry,
                state.symmetry,
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            AppVerticalGradientDivider()
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            ContributorCell(
                Modifier.weight(1f),
                R.string.physique_drift_shoulder_waist_ratio,
                state.shoulderToWaistRatio,
            )
        }
    }
}

@Composable
internal fun PhysiqueInsightCard(
    insight: String,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.physique_drift_insight_title),
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

@Composable
private fun MetricCell(
    @StringRes labelRes: Int,
    metric: PhysiqueMetric,
    modifier: Modifier = Modifier,
    decreaseIsPositive: Boolean = false,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = metric.value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            metric.delta?.let {
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_s)))
                BodyDelta(delta = it, decreaseIsPositive = decreaseIsPositive)
            }
        }
    }
}

@Composable
private fun ContributorCell(
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

private val PhysiqueDriftLevel.labelRes: Int
    get() = when (this) {
        PhysiqueDriftLevel.Athletic -> R.string.physique_drift_level_athletic
        PhysiqueDriftLevel.Improving -> R.string.physique_drift_level_improving
        PhysiqueDriftLevel.NeedsWork -> R.string.physique_drift_level_needs_work
        PhysiqueDriftLevel.NoData -> R.string.stat_unknown
    }
