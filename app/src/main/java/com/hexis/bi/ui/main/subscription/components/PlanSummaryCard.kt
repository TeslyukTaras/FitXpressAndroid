package com.hexis.bi.ui.main.subscription.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.AnimationConstants
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun PlanSummaryCard(
    name: String,
    price: String?,
    label: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.primary,
    scansText: String? = null,
    showPrice: Boolean = true,
) {
    val priceAlpha by animateFloatAsState(
        targetValue = if (price != null) 1f else 0f,
        animationSpec = tween(AnimationConstants.CONTENT_SWAP_FADE_MS),
        label = "summaryPrice",
    )
    BodyGlassCard(
        contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_xl)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_l)),
        glow = true,
        loading = showPrice && price == null,
    ) {
        if (label != null) Text(
            text = label,
            style = TitleDimTextStyle,
            color = labelColor,
        )

        Text(
            text = name,
            style = MaterialTheme.typography.headlineLarge,
            color = NocturnePulseTheme.extendedColors.textEmphasis,
        )
        if (showPrice) Row(
            modifier = Modifier.alpha(priceAlpha),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xxs)),
        ) {
            Text(
                text = price.orEmpty(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.plan_per_month),
                style = TitleDimTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (scansText != null) Text(
            text = scansText,
            style = TitleDimTextStyle,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
