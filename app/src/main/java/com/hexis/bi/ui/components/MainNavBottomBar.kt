package com.hexis.bi.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.Blue200
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.ShadowGray
import com.hexis.bi.utils.gradientBackground

@Composable
fun MainNavBottomBar(
    isHomeSelected: Boolean,
    isBodySelected: Boolean,
    onHomeClick: () -> Unit,
    onBodyClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = dimensionResource(R.dimen.elevation_nav_bar),
                shape = RectangleShape,
                clip = false,
                ambientColor = ShadowGray,
                spotColor = ShadowGray,
            )
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = dimensionResource(R.dimen.padding_small),
                    end = dimensionResource(R.dimen.padding_small),
                    top = dimensionResource(R.dimen.padding_small),
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavTab(
                iconRes = if (isHomeSelected) R.drawable.ic_home_filled else R.drawable.ic_home,
                label = stringResource(R.string.home_nav_home),
                selected = isHomeSelected,
                onClick = onHomeClick,
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.spacer_m))
                    .size(dimensionResource(R.dimen.size_bottom_nav_center))
                    .clip(CircleShape)
                    .gradientBackground(
                        brush = Brush.verticalGradient(listOf(Blue300, Blue200)),
                        shape = MaterialTheme.shapes.medium,
                    )
                    .clickable { onScanClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_scan),
                    contentDescription = stringResource(R.string.cd_scan_action),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }

            NavTab(
                iconRes = if (isBodySelected) R.drawable.ic_body_filled else R.drawable.ic_body,
                label = stringResource(R.string.home_nav_body),
                selected = isBodySelected,
                onClick = onBodyClick,
            )
        }
    }
}

@Composable
private fun NavTab(
    @DrawableRes iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() },
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.secondary,
        )
    }
}
