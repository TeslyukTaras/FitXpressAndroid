package com.hexis.bi.ui.main.home.intelligence

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.utils.constants.AnimationConstants

@Composable
fun InsightsUpdatingHeader(
    visible: Boolean,
    modifier: Modifier = Modifier,
    animateDots: Boolean = true,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(AnimationConstants.UPDATING_BANNER_MS)) +
            expandVertically(tween(AnimationConstants.UPDATING_BANNER_MS)),
        exit = fadeOut(tween(AnimationConstants.UPDATING_BANNER_MS)) +
            shrinkVertically(tween(AnimationConstants.UPDATING_BANNER_MS)),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            UpdatingTitle(animateDots)
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
            Text(
                text = stringResource(R.string.insights_updating_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
        }
    }
}

@Composable
private fun UpdatingTitle(animateDots: Boolean) {
    val activeDot = if (animateDots) {
        val transition = rememberInfiniteTransition(label = "insights-updating-dots")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = UPDATING_DOT_COUNT.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = UPDATING_DOT_STEP_MS * UPDATING_DOT_COUNT,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "insights-updating-dot-phase",
        )
        phase.toInt() % UPDATING_DOT_COUNT
    } else {
        0
    }

    Row {
        Text(
            text = stringResource(R.string.insights_updating),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(Modifier.width(UPDATING_DOT_SPACING))
        Row(
            modifier = Modifier.alignBy { it.measuredHeight },
            horizontalArrangement = Arrangement.spacedBy(UPDATING_DOT_SPACING),
            verticalAlignment = Alignment.Bottom,
        ) {
            repeat(UPDATING_DOT_COUNT) { index ->
                val size by animateDpAsState(
                    targetValue = if (index == activeDot) UPDATING_DOT_ACTIVE_SIZE else UPDATING_DOT_SIZE,
                    animationSpec = tween(UPDATING_DOT_SIZE_ANIMATION_MS),
                    label = "insights-updating-dot-$index",
                )
                Box(Modifier.size(UPDATING_DOT_ACTIVE_SIZE), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(UpdatingDotColor, CircleShape),
                    )
                }
            }
        }
    }
}

private val UpdatingDotColor = Color(0xFF1DC4B3)
private val UPDATING_DOT_SIZE = 3.dp
private val UPDATING_DOT_ACTIVE_SIZE = 4.dp
private val UPDATING_DOT_SPACING = 4.dp
private const val UPDATING_DOT_COUNT = 3
private const val UPDATING_DOT_STEP_MS = 400
private const val UPDATING_DOT_SIZE_ANIMATION_MS = 160
