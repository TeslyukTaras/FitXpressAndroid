package com.hexis.bi.ui.main.home.intelligence

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.utils.constants.FindingValues

@Composable
fun HomeInsightsSection(
    cards: List<InsightCard>,
    loaded: Boolean,
    updating: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = loaded, onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.insights_home_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (loaded) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = stringResource(R.string.insights_screen_title),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }
        }

        InsightsUpdatingHeader(visible = updating)

        AnimatedVisibility(
            visible = showHomeInsightContent(loaded, updating, cards.isNotEmpty()),
            enter = fadeIn(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (cards.isEmpty()) {
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
                    Text(
                        text = stringResource(R.string.engine_findings_empty_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
                    Text(
                        text = stringResource(R.string.engine_findings_empty_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    cards.take(FindingValues.HOME_PREVIEW_CARDS).forEach { card ->
                        Spacer(Modifier.height(dimensionResource(R.dimen.insight_card_spacing)))
                        InsightCardView(card)
                    }
                }
            }
        }
    }
}
