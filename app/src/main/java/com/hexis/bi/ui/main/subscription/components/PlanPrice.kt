package com.hexis.bi.ui.main.subscription.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.utils.constants.AnimationConstants
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun PlanPrice(
    price: String?,
    modifier: Modifier = Modifier,
    gap: Dp = 0.dp,
) {
    val priceAlpha by animateFloatAsState(
        targetValue = if (price != null) 1f else 0f,
        animationSpec = tween(AnimationConstants.CONTENT_SWAP_FADE_MS),
        label = "planPrice",
    )
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = price.orEmpty(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End,
            modifier = Modifier.alpha(priceAlpha),
        )
        Spacer(Modifier.height(gap))
        Text(
            text = stringResource(R.string.plan_per_month),
            style = TitleDimTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}
