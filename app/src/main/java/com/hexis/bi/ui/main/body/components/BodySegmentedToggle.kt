package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.TitleHighlightTextStyle

@Composable
internal fun BodySegmentedToggleTrack(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(CircleShape)
            .border(
                width = dimensionResource(R.dimen.border_line),
                color = NocturnePulseTheme.extendedColors.bodyToggleTrackBorder,
                shape = CircleShape,
            )
            .padding(
                horizontal = dimensionResource(R.dimen.spacer_xxs),
                vertical = dimensionResource(R.dimen.spacer_xxs),
            ),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
internal fun BodySegmentedToggleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    width: Dp = dimensionResource(R.dimen.body_switch_button_width),
) {
    Box(
        modifier = modifier
            .height(dimensionResource(R.dimen.body_toggle_height))
            .width(width)
            .clip(CircleShape)
            .then(if (isSelected) Modifier.background(NocturnePulseTheme.extendedColors.bodyToggleSelectedChipFill) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (isSelected) TitleHighlightTextStyle else TitleDimTextStyle,
            color = if (isSelected) NocturnePulseTheme.extendedColors.bodyToggleSelectedLabel else NocturnePulseTheme.extendedColors.bodyToggleUnselectedLabel,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
