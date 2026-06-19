package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.home.IntelligenceScoreData
import com.hexis.bi.ui.main.home.IntelligenceScoreKey

@Composable
fun IntelligenceScoresCard(
    scores: List<IntelligenceScoreData>,
    modifier: Modifier = Modifier,
    onScoreClick: (IntelligenceScoreKey) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
    ) {
        scores.chunked(2).forEach { rowScores ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
            ) {
                rowScores.forEach { score ->
                    GaugeCard(
                        score = score,
                        onClick = { onScoreClick(score.key) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                if (rowScores.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GaugeCard(
    score: IntelligenceScoreData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(
            text = stringResource(score.titleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        IntelligenceGauge(
            fraction = score.fraction,
            value = score.value,
            comingSoon = score.comingSoon,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
