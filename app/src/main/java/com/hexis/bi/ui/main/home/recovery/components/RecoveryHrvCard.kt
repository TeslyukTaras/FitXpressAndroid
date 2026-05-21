package com.hexis.bi.ui.main.home.recovery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.hexis.bi.R
import com.hexis.bi.ui.dark.AppVerticalGradientDivider
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.theme.MeasurementValueStyle
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.TitleHighlightTextStyle

@Composable
fun RecoveryHrvCard(
    rmssdMs: Int,
    sdnnMs: Int,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.recovery_hrv_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            HrvInfoButton()
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            HrvColumn(
                label = stringResource(R.string.recovery_hrv_rmssd),
                valueMs = rmssdMs,
                caption = stringResource(R.string.recovery_hrv_rmssd_caption),
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))
            AppVerticalGradientDivider()
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))

            HrvColumn(
                label = stringResource(R.string.recovery_hrv_sdnn),
                valueMs = sdnnMs,
                caption = stringResource(R.string.recovery_hrv_sdnn_caption),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Info icon that toggles a small anchored popup spelling out the HRV acronym. */
@Composable
private fun HrvInfoButton() {
    var showPopup by remember { mutableStateOf(false) }
    val iconSize = dimensionResource(R.dimen.icon_medium)
    val density = LocalDensity.current

    Box {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = stringResource(R.string.cd_info),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .clickable { showPopup = !showPopup },
        )
        if (showPopup) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = with(density) { IntOffset(0, iconSize.roundToPx()) },
                onDismissRequest = { showPopup = false },
                properties = PopupProperties(focusable = true),
            ) {
                Text(
                    text = stringResource(R.string.recovery_hrv_full),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(dimensionResource(R.dimen.spacer_2xs))
                )
            }
        }
    }
}

@Composable
private fun HrvColumn(
    label: String,
    valueMs: Int,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val unit = stringResource(R.string.unit_ms)
    val numberStyle = MeasurementValueStyle.toSpanStyle()
    val unitStyle = MaterialTheme.typography.titleSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    ).toSpanStyle()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = TitleDimTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
        Text(
            text = buildAnnotatedString {
                if (valueMs > 0) {
                    withStyle(numberStyle) { append(valueMs.toString()) }
                    append(stringResource(R.string.space))
                    withStyle(unitStyle) { append(unit) }
                } else append(stringResource(R.string.stat_unknown))
            },
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        Text(
            text = caption,
            style = TitleHighlightTextStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
