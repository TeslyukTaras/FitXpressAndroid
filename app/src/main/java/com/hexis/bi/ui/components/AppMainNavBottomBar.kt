package com.hexis.bi.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.mainNavBarFillBrush
import com.hexis.bi.ui.theme.scanFabFillBrush
import com.hexis.bi.utils.constants.GlassConstants
import com.hexis.bi.utils.glass

@Composable
fun AppMainNavBottomBar(
    isHomeSelected: Boolean,
    isBodySelected: Boolean,
    onHomeClick: () -> Unit,
    onBodyClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeAlpha: Float? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glass(
                    tint = NocturnePulseTheme.extendedColors.glassRimHighlight,
                    shape = CircleShape,
                    level = GlassConstants.LEVEL_DEFAULT,
                    fill = Color.Transparent,
                    fillBrush = { mainNavBarFillBrush(it) },
                    backgroundBlur = dimensionResource(R.dimen.glass_background_blur),
                    rimWidth = dimensionResource(R.dimen.glass_rim_width),
                    backgroundAlpha = 1f,
                    hazeAlpha = hazeAlpha,
                )
                // Absorb taps on the bar so they don't fall through to content behind it.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(dimensionResource(R.dimen.spacer_xs)),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppNavTab(
                iconRes = R.drawable.ic_home_filled,
                label = stringResource(R.string.home_nav_home),
                selected = isHomeSelected,
                onClick = onHomeClick,
            )

            ScanFab(onClick = onScanClick)

            AppNavTab(
                iconRes = R.drawable.ic_body_filled,
                label = stringResource(R.string.home_nav_body),
                selected = isBodySelected,
                onClick = onBodyClick,
            )
        }
    }
}

@Composable
private fun ScanFab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(dimensionResource(R.dimen.size_bottom_nav_center))
            .glass(
                tint = NocturnePulseTheme.extendedColors.glassRimHighlight,
                shape = CircleShape,
                level = GlassConstants.LEVEL_RAISED,
                fill = Color.Transparent,
                fillBrush = { scanFabFillBrush(it) },
                backgroundBlur = dimensionResource(R.dimen.glass_background_blur),
                rimWidth = dimensionResource(R.dimen.glass_rim_width),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_scan),
            contentDescription = stringResource(R.string.cd_scan_action),
            tint = Color.White,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
    }
}

@Composable
private fun AppNavTab(
    @DrawableRes iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.onSurface
    else NocturnePulseTheme.extendedColors.textTertiary
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(dimensionResource(R.dimen.size_bottom_nav_center))
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color,
            )
        }
    }
}
