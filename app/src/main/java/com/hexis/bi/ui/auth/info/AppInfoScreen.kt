package com.hexis.bi.ui.auth.info

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppLogo
import com.hexis.bi.ui.components.FigmaGradientAccentLayer
import com.hexis.bi.ui.dark.DarkPrimaryButton
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.theme.AppInfoAccentGradientEnd
import com.hexis.bi.ui.theme.AppInfoAccentGradientStart
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.constants.AuthFlowConstants

@Composable
fun AppInfoScreen(
    modifier: Modifier = Modifier,
    onFinish: () -> Unit = {},
) {
    LightStatusBarIcons()

    DarkTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clipToBounds(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Spacer(modifier = Modifier.weight(AuthFlowConstants.HERO_TOP_SPACER_WEIGHT))

                Image(
                    painter = painterResource(R.drawable.img_app_info_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.weight(AuthFlowConstants.HERO_BOTTOM_SPACER_WEIGHT))
            }

            TealAccentLayer()

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = dimensionResource(R.dimen.spacer_2xl)),
                contentAlignment = Alignment.Center,
            ) {
                AppLogo(tint = MaterialTheme.colorScheme.primary)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium))
                    .padding(bottom = dimensionResource(R.dimen.spacer_xxl)),
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append(stringResource(R.string.app_info_title_accent))
                        }
                        append(stringResource(R.string.space))
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                            append(stringResource(R.string.app_info_title_rest))
                        }
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_xs)))

                Text(
                    text = stringResource(R.string.app_info_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

                DarkPrimaryButton(
                    text = stringResource(R.string.action_get_started),
                    onClick = onFinish,
                    trailingIcon = R.drawable.ic_arrow,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.TealAccentLayer() {
    FigmaGradientAccentLayer(
        figmaLeftDp = AuthFlowConstants.TEAL_LAYER_VISIBLE_LEFT_DP,
        figmaTopDp = AuthFlowConstants.TEAL_LAYER_VISIBLE_TOP_DP,
        figmaWidthDp = AuthFlowConstants.TEAL_LAYER_SIZE_DP,
        figmaHeightDp = AuthFlowConstants.TEAL_LAYER_SIZE_DP,
        figmaAngleDeg = AuthFlowConstants.TEAL_LAYER_FIGMA_ANGLE_DEG,
        opacity = AuthFlowConstants.TEAL_LAYER_OPACITY,
        gradientStartColor = AppInfoAccentGradientStart,
        gradientEndColor = AppInfoAccentGradientEnd,
        cornerFadeStart = AuthFlowConstants.TEAL_CORNER_FADE_START,
        cornerFadeRadiusFactor = AuthFlowConstants.TEAL_CORNER_FADE_RADIUS_FACTOR,
        sideFadeEnd = AuthFlowConstants.TEAL_SIDE_FADE_END,
    )
}
