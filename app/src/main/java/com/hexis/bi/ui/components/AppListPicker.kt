package com.hexis.bi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.utils.constants.GlassConstants

@Composable
fun <T> AppListPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    itemLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    AppDialog(onDismiss = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
        ) {
            Text(
                text = title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            items.forEach { item ->
                val selected = item == selectedItem
                val label = itemLabel(item)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = GlassConstants.SELECTION_HIGHLIGHT_ALPHA)
                            else Color.Transparent
                        )
                        .clickable { onItemSelected(item) }
                        .padding(
                            vertical = dimensionResource(R.dimen.spacer_m),
                            horizontal = dimensionResource(R.dimen.spacer_s),
                        ),
                )
            }
        }
    }
}
