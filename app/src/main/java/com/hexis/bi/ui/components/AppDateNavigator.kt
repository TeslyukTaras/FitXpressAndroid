package com.hexis.bi.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R

@Composable
fun AppDateNavigator(
    modifier: Modifier = Modifier,
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    canGoNext: Boolean,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.date_navigator_height)),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_small))
                        .graphicsLayer(rotationY = 180f),
                )
            }
            Text(
                modifier = Modifier.defaultMinSize(minWidth = dimensionResource(R.dimen.date_navigator_label_min_width)),
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(
                onClick = onNext,
                enabled = canGoNext,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = null,
                    tint = if (canGoNext)
                        MaterialTheme.colorScheme.onBackground
                    else
                        MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
                )
            }
        }
    }
}
