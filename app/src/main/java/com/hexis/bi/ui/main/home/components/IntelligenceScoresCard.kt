package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.IntelligenceScoreData
import com.hexis.bi.ui.main.home.ScoreLevel
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.ui.theme.Red300
import com.hexis.bi.ui.theme.Yellow

@Composable
fun IntelligenceScoresCard(
    scores: List<IntelligenceScoreData>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
    ) {
        scores.forEach { score ->
            ScoreItem(
                score = score,
                modifier = Modifier.weight(1f),
            )
        }

    }
}

@Composable
private fun ScoreItem(
    score: IntelligenceScoreData,
    modifier: Modifier = Modifier,
) {
    val valueColor: Color = when (score.level) {
        ScoreLevel.Low -> Red300
        ScoreLevel.Medium -> Yellow
        ScoreLevel.High -> Green
    }
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_none)),
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacer_m)),
        ) {
            Text(
                text = score.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
            Text(
                text = score.value,
                style = MaterialTheme.typography.titleLarge,
                color = valueColor,
            )
        }
    }

}
