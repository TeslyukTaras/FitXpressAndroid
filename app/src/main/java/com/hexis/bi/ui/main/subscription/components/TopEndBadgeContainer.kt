package com.hexis.bi.ui.main.subscription.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

@Composable
internal fun TopEndBadgeContainer(
    badge: @Composable () -> Unit,
    badgeEndPadding: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = {
            badge()
            content()
        },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val badgePlaceable = measurables[0].measure(Constraints())
        val contentPlaceable = measurables[1].measure(constraints.copy(minHeight = 0))
        val halfBadge = badgePlaceable.height / 2
        layout(contentPlaceable.width, contentPlaceable.height + halfBadge) {
            contentPlaceable.place(0, halfBadge)
            badgePlaceable.place(
                x = contentPlaceable.width - badgePlaceable.width - badgeEndPadding.roundToPx(),
                y = 0,
            )
        }
    }
}
