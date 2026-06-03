package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.home.SleepOverview
import com.hexis.bi.utils.constants.HomeConstants
import com.hexis.bi.utils.constants.SleepConstants

@Composable
internal fun SleepOverviewCard(
    data: SleepOverview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier, onClick = onClick) {
        Text(
            text = stringResource(R.string.home_card_sleep),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
        Spacer(Modifier.weight(1f))

        val hours = data.durationMinutes / SleepConstants.MINUTES_PER_HOUR
        val minutes = data.durationMinutes % SleepConstants.MINUTES_PER_HOUR
        val numberStyle = MaterialTheme.typography.titleLarge.toSpanStyle()
            .copy(color = MaterialTheme.colorScheme.onBackground)
        val unitStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = buildAnnotatedString {
                withStyle(numberStyle) { append(hours.toString()) }
                append(" ")
                withStyle(unitStyle) { append(stringResource(R.string.unit_hours_short)) }
                append(" ")
                withStyle(numberStyle) { append(minutes.toString()) }
                append(" ")
                withStyle(unitStyle) { append(stringResource(R.string.unit_minutes_short)) }
            },
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))

        SleepProgressBar(fraction = data.goalFraction)

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
    }
}

@Composable
private fun SleepProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.home_sleep_progress_height))
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant
                    .copy(alpha = HomeConstants.SLEEP_PROGRESS_TRACK_ALPHA)
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
