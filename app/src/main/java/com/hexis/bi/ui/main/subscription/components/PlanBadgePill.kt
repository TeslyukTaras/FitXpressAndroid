package com.hexis.bi.ui.main.subscription.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R

@Composable
internal fun PlanBadgePill(
    text: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val container = if (highlighted) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceContainerHighest
    val content = if (highlighted) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onBackground

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = content,
        modifier = modifier
            .background(container, CircleShape)
            .padding(
                horizontal = dimensionResource(R.dimen.spacer_s),
                vertical = dimensionResource(R.dimen.spacer_xxs),
            ),
    )
}

internal fun Modifier.offsetUpByHalfHeight(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.place(0, -placeable.height / 2)
    }
}
