package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.theme.promoBannerFillBrush
import com.hexis.bi.utils.constants.HomeConstants

@Composable
fun PromoBanner(
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    // The card has no content padding; its height is set by the text column. The images sit in a
    // matchParentSize overlay so they take that height without contributing to it.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBehind { drawRect(brush = promoBannerFillBrush(size)) }
            .border(
                width = dimensionResource(R.dimen.border_hairline),
                color = MaterialTheme.colorScheme.outline,
                shape = shape,
            ),
    ) {
        var manWidthPx by remember { mutableIntStateOf(0) }
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = dimensionResource(R.dimen.spacer_m)),
        ) {
            // Man behind, woman in front; the woman's right edge lands at the man's horizontal center.
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                Image(
                    painter = painterResource(R.drawable.img_man),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxHeight()
                        .aspectRatio(HomeConstants.PROMO_MAN_ASPECT_RATIO)
                        .onSizeChanged { manWidthPx = it.width },
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomEnd,
                )
                Image(
                    painter = painterResource(R.drawable.img_woman),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset { IntOffset(x = -manWidthPx / 2, y = 0) }
                        .fillMaxHeight()
                        .aspectRatio(HomeConstants.PROMO_WOMAN_ASPECT_RATIO),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomEnd,
                )
            }
        }

        Column(
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.spacer_l),
                top = dimensionResource(R.dimen.spacer_l),
                bottom = dimensionResource(R.dimen.spacer_l),
            ),
        ) {
            Text(
                text = stringResource(R.string.home_banner_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
            Text(
                text = stringResource(R.string.home_banner_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
            AppPrimaryButton(
                text = stringResource(R.string.home_action_buy_suit),
                onClick = onBuyClick,
                width = dimensionResource(R.dimen.promo_button_width),
                height = dimensionResource(R.dimen.promo_button_height),
            )
        }
    }
}
