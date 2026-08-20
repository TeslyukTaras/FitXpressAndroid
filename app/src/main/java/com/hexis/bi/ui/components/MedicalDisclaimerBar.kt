package com.hexis.bi.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.bodyGlassCardFillBrush
import kotlin.math.floor

@Composable
fun MedicalDisclaimerBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = NocturnePulseTheme.extendedColors.disclaimerBarBorder
    val borderWidth = dimensionResource(R.dimen.border_hairline)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = dimensionResource(R.dimen.size_disclaimer_bar))
            .drawBehind {
                drawRect(brush = bodyGlassCardFillBrush(size))
                // Snapped to whole device pixels: 0.5dp lands mid-pixel on most densities and
                // antialiases into two rows, which reads thicker than the hairline it should be.
                val stroke = floor(borderWidth.toPx()).coerceAtLeast(1f)
                drawRect(
                    color = borderColor,
                    topLeft = Offset.Zero,
                    size = Size(size.width, stroke),
                )
                drawRect(
                    color = borderColor,
                    topLeft = Offset(0f, floor(size.height) - stroke),
                    size = Size(size.width, stroke),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.medical_disclaimer_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = stringResource(R.string.cd_medical_disclaimer_info),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
    }
}
