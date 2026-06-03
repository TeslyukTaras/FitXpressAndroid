package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.hexis.bi.ui.dark.AppVerticalGradientDivider
import com.hexis.bi.ui.dark.BodyGlassCard

@Composable
fun UserStatsCard(
    weight: String,
    height: String,
    age: String,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.spacer_m)),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem(
                label = stringResource(R.string.home_stat_weight),
                value = weight,
                modifier = Modifier.weight(1f),
            )
            AppVerticalGradientDivider()
            StatItem(
                label = stringResource(R.string.home_stat_height),
                value = height,
                modifier = Modifier.weight(1f),
            )
            AppVerticalGradientDivider()
            StatItem(
                label = stringResource(R.string.home_stat_age),
                value = age,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        val unitSpanStyle = MaterialTheme.typography.labelMedium.toSpanStyle()
        Text(
            text = buildAnnotatedString {
                value.split(" ").forEachIndexed { index, token ->
                    if (index > 0) append(" ")
                    if (token.firstOrNull()?.isDigit() == true) {
                        append(token)
                    } else {
                        withStyle(unitSpanStyle) { append(token) }
                    }
                }
            },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
