package com.hexis.bi.ui.main.home.intelligence

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.intelligence.engine.Domains
import com.hexis.bi.utils.constants.InsightSubjects
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.components.MedicalDisclaimerFooter
import com.hexis.bi.ui.theme.screenBackground
import org.koin.androidx.compose.koinViewModel

@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LightStatusBarIcons()

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier
                .fillMaxSize()
                .screenBackground(),
            containerColor = Color.Transparent,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.insights_screen_title),
                    onBack = onBack,
                    background = Color.Transparent,
                    actions = {
                        IconButton(onClick = viewModel::showInfoSheet) {
                            Icon(
                                painter = painterResource(R.drawable.ic_info),
                                contentDescription = stringResource(R.string.cd_info),
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                            )
                        }
                    },
                )
            },
        ) {
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
            AnimatedContent(
                targetState = state.cards.isEmpty(),
                transitionSpec = { insightCrossfade() },
                label = "insights-content",
            ) { isEmpty ->
            if (isEmpty) {
                Column(modifier = contentModifier) {
                    InsightsHeader(state)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { InsightsEmpty() }
                }
            } else {
                Column(modifier = contentModifier.verticalScroll(rememberScrollState())) {
                    InsightsHeader(state)

                    val ranked = state.cards.groupBy { it.confidence == FindingConfidence.LOW }
                    ranked[false].orEmpty().forEach { card ->
                        Spacer(Modifier.height(dimensionResource(R.dimen.insight_card_spacing)))
                        InsightCardView(card)
                    }
                    val low = ranked[true].orEmpty()
                    if (low.isNotEmpty()) {
                        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
                        Text(
                            text = stringResource(R.string.insights_low_section),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
                        Text(
                            text = stringResource(R.string.insights_low_caption),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        low.forEach { card ->
                            Spacer(Modifier.height(dimensionResource(R.dimen.insight_card_spacing)))
                            InsightCardView(card)
                        }
                    }

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                    MedicalDisclaimerFooter()

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
                }
            }
            }
        }

        if (state.showInfoSheet) InsightsInfoBottomSheet(onDismiss = viewModel::dismissInfoSheet)
    }
}

@Composable
private fun InsightsHeader(state: InsightsState) {
    InsightsUpdatingHeader(visible = state.updating)
    if (!state.updating) {
        state.latestScanDate?.let { date ->
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Text(
                text = stringResource(R.string.insights_subtitle, date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InsightsEmpty() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.engine_findings_empty_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        Text(
            text = stringResource(R.string.engine_findings_empty_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun InsightCardView(card: InsightCard) {
    BodyGlassCard(
        contentPadding = PaddingValues(dimensionResource(R.dimen.insight_card_padding)),
        shape = RoundedCornerShape(dimensionResource(R.dimen.insight_card_corner)),
    ) {
        InsightHeader(title = card.area.areaLabel(), confidence = card.confidence)
        if (card.values.isNotEmpty()) {
            Spacer(Modifier.height(dimensionResource(R.dimen.insight_card_value_gap)))
            InsightValues(card.values, showLabels = card.values.size > 1)
        } else if (card.confidence == FindingConfidence.LOW) {
            Spacer(Modifier.height(dimensionResource(R.dimen.insight_card_value_gap)))
            Text(
                text = stringResource(R.string.insights_value_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (card.explanation.isNotBlank()) {
            Spacer(Modifier.height(dimensionResource(R.dimen.insight_card_text_gap)))
            Text(
                text = card.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun String.areaLabel(): String = when (this) {
    Domains.SLEEP -> stringResource(R.string.insights_area_sleep)
    Domains.RECOVERY -> stringResource(R.string.insights_area_recovery)
    Domains.ACTIVITY -> stringResource(R.string.insights_area_activity)
    Domains.BODY -> stringResource(R.string.insights_area_body)
    Domains.AGING -> stringResource(R.string.insights_area_aging)
    Domains.STRESS -> stringResource(R.string.insights_area_stress)
    InsightSubjects.RECOMPOSITION -> stringResource(R.string.insights_area_recomposition)
    InsightSubjects.PHYSIQUE_DRIFT -> stringResource(R.string.insights_area_physique_drift)
    InsightSubjects.LONGEVITY -> stringResource(R.string.insights_area_longevity)
    InsightSubjects.PACE_OF_AGING -> stringResource(R.string.insights_area_aging)
    else -> stringResource(R.string.engine_findings_title)
}
