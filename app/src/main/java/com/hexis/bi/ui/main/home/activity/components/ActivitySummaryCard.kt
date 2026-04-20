package com.hexis.bi.ui.main.home.activity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R

data class SummaryRow(
    val label: String,
    val value: String,
    val unit: String,
)

@Composable
fun ActivitySummaryCard(
    title: String,
    rows: List<SummaryRow>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_none)),
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacer_m)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            rows.forEach { row -> SummaryRowLine(row) }
        }
    }
}

@Composable
private fun SummaryRowLine(row: SummaryRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = row.value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
            Text(
                text = row.unit,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}
