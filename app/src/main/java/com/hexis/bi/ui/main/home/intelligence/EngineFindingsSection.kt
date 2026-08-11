package com.hexis.bi.ui.main.home.intelligence

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard

@Composable
fun EngineFindingsSection(
    state: EngineFindingsState,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "engine-findings",
        modifier = modifier,
    ) { current ->
        when (current) {
            is EngineFindingsState.Ready -> Column {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
                BodyGlassCard {
                    current.rows.forEachIndexed { index, row ->
                        if (index > 0) Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
                        FindingRow(row)
                    }
                }
            }

            EngineFindingsState.Hidden -> Spacer(Modifier)
        }
    }
}

@Composable
private fun FindingRow(row: EngineFindingRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.finding,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(FINDING_WEIGHT),
        )
        Text(
            text = row.rank,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(RANK_COLUMN),
        )
        Text(
            text = row.confidence,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(CONFIDENCE_COLUMN),
        )
    }
}

private const val FINDING_WEIGHT = 5f
private const val RANK_COLUMN = 1f
private const val CONFIDENCE_COLUMN = 3f
