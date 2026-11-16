package com.hexis.bi.ui.main.subscription.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.hexis.bi.R
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun InfoNote(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.ic_info,
    textStyle: TextStyle = TitleDimTextStyle,
    gap: Dp = dimensionResource(R.dimen.spacer_m),
) {
    Layout(
        modifier = modifier,
        content = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
            Text(
                text = text,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val iconPlaceable = measurables[0].measure(loose)
        val textMaxWidth = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth - iconPlaceable.width - gapPx).coerceAtLeast(0)
        } else {
            loose.maxWidth
        }
        val textPlaceable = measurables[1].measure(loose.copy(maxWidth = textMaxWidth))
        val height = maxOf(iconPlaceable.height, textPlaceable.height)
        val isMultiline = textPlaceable.height > iconPlaceable.height
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            iconPlaceable.width + gapPx + textPlaceable.width
        }
        layout(width, height) {
            iconPlaceable.placeRelative(0, if (isMultiline) 0 else (height - iconPlaceable.height) / 2)
            textPlaceable.placeRelative(
                iconPlaceable.width + gapPx,
                if (isMultiline) 0 else (height - textPlaceable.height) / 2,
            )
        }
    }
}
