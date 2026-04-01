package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R
import com.hexis.bi.ui.theme.Blue100
import com.hexis.bi.ui.theme.Blue200
import com.hexis.bi.ui.theme.Lime200
import com.hexis.bi.utils.gradientBackground

@Composable
fun PromoBanner(
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .gradientBackground(
                brush = Brush.verticalGradient(listOf(Blue100, Blue200)),
                shape = MaterialTheme.shapes.medium,
            )
    ) {
        Image(
            painter = painterResource(R.drawable.img_home_banner),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .padding(
                    end = dimensionResource(R.dimen.padding_small),
                    top = dimensionResource(R.dimen.padding_small)
                ),
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomEnd
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .padding(
                    start = dimensionResource(R.dimen.padding_medium),
                    top = dimensionResource(R.dimen.padding_medium),
                    bottom = dimensionResource(R.dimen.padding_medium),
                ),
        ) {
            Text(
                text = stringResource(R.string.home_banner_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onPrimary,
                minLines = 2
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
            Text(
                text = stringResource(R.string.home_banner_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary,
                minLines = 2
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Button(
                modifier = Modifier // Removed 'modifier' inheritance here to avoid conflicts
                    .height(dimensionResource(R.dimen.promo_button_height))
                    .width(dimensionResource(R.dimen.promo_button_width)),
                onClick = onBuyClick,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Lime200,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = dimensionResource(R.dimen.elevation_none),
                ),
            ) {
                Text(
                    text = stringResource(R.string.home_action_buy_suit),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}