package com.hexis.bi.ui.main.subscription.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.utils.constants.SubscriptionConstants

@Composable
internal fun EmptyStateContent(
    title: String,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
    messages: @Composable ColumnScope.() -> Unit,
) {
    val contentAlpha = if (hidden) 0f else 1f
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = TopFractionArrangement,
        ) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.subscription_empty_icon_tile))
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = SubscriptionConstants.EMPTY_TILE_ALPHA),
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_layers),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(dimensionResource(R.dimen.subscription_empty_icon_size)),
                )
            }
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(contentAlpha),
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            Column(
                modifier = Modifier.alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_l)),
                content = messages,
            )
        }
    }
}

private object TopFractionArrangement : Arrangement.Vertical {
    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        val free = (totalSize - sizes.sum()).coerceAtLeast(0)
        var position = (free * SubscriptionConstants.EMPTY_STATE_TOP_FRACTION).toInt()
        sizes.forEachIndexed { index, size ->
            outPositions[index] = position
            position += size
        }
    }
}

@Composable
internal fun EmptyStateMessage(text: AnnotatedString) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun EmptyStateMessage(text: String) = EmptyStateMessage(AnnotatedString(text))

@Composable
internal fun SubscriptionActionBar(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium))
            .padding(
                top = dimensionResource(R.dimen.spacer_m),
                bottom = dimensionResource(R.dimen.spacer_xl),
            )
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_l)),
        content = content,
    )
}
