package com.hexis.bi.ui.dark

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.hexis.bi.R
import com.hexis.bi.utils.constants.GlassConstants
import com.hexis.bi.utils.glass

@Composable
fun <T> DarkTabSelector(
    tabs: List<T>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    tabLabel: @Composable (T) -> String = { it.toString() },
) {
    Row(
        modifier = modifier
            .glass(
                CircleShape,
                level = GlassConstants.LEVEL_RAISED,
                fill = GlassConstants.GLASS_TRACK_FILL
            )
            .padding(dimensionResource(R.dimen.spacer_xxs)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isSelected) Modifier.glass(
                            CircleShape,
                            level = GlassConstants.LEVEL_SELECTED,
                            fill = GlassConstants.GLASS_SELECTION_FILL,
                        )
                        else Modifier.clip(CircleShape)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(tab) },
                    )
                    .padding(vertical = dimensionResource(R.dimen.spacer_2xs)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tabLabel(tab),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
