package com.hexis.bi.ui.main.home.activity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.theme.Blue300
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ActivityGoalRow(
    currentSteps: Int,
    stepsGoal: Int,
    modifier: Modifier = Modifier,
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_none)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacer_m)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.activity_goal_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Blue300)) {
                        append(fmt.format(currentSteps))
                    }
                    append(
                        stringResource(
                            R.string.activity_goal_label_format,
                            fmt.format(stepsGoal)
                        )
                    )
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
