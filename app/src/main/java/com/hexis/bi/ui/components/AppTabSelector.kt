package com.hexis.bi.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.AnimationConstants
import com.hexis.bi.utils.constants.GlassConstants
import com.hexis.bi.utils.glass

@Composable
fun <T> AppTabSelector(
    tabs: List<T>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    tabLabel: @Composable (T) -> String = { it.toString() },
) {
    Row(
        modifier = modifier
            .glass(
                tint = NocturnePulseTheme.extendedColors.glassRimHighlight,
                shape = CircleShape,
                level = GlassConstants.LEVEL_RAISED,
                fill = NocturnePulseTheme.extendedColors.glassTrackFill,
                backgroundBlur = dimensionResource(R.dimen.glass_background_blur),
                rimWidth = dimensionResource(R.dimen.glass_rim_width),
            )
            .padding(dimensionResource(R.dimen.spacer_xxs)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            val selectionFraction by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(durationMillis = AnimationConstants.TAB_SELECTION_MS),
                label = "tabSelection",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(tab) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(selectionFraction)
                        .glass(
                            tint = NocturnePulseTheme.extendedColors.glassRimHighlight,
                            shape = CircleShape,
                            level = GlassConstants.LEVEL_SELECTED,
                            fill = NocturnePulseTheme.extendedColors.glassSelectionFill,
                            backgroundBlur = dimensionResource(R.dimen.glass_background_blur),
                            rimWidth = dimensionResource(R.dimen.glass_rim_width),
                        ),
                )
                Text(
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacer_xs)),
                    text = tabLabel(tab),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    ),
                    color = lerp(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.onSurface,
                        selectionFraction,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
