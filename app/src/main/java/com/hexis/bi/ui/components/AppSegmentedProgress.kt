package com.hexis.bi.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.AnimationConstants

@Composable
fun AppSegmentedProgress(
    filled: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val filledColor = MaterialTheme.colorScheme.primary
    val trackColor = NocturnePulseTheme.extendedColors.stepIndicatorTrack
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xxs)),
    ) {
        repeat(total) { index ->
            val color by animateColorAsState(
                targetValue = if (index < filled) filledColor else trackColor,
                animationSpec = tween(AnimationConstants.SEGMENT_FILL_MS),
                label = "segmentFill",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(dimensionResource(R.dimen.size_indicator_bigger))
                    .background(color, CircleShape),
            )
        }
    }
}
