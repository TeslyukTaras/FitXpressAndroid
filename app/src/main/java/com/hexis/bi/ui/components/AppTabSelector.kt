package com.hexis.bi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.theme.Lime100

@Composable
fun <T> AppTabSelector(
    tabs: List<T>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    tabLabel: @Composable (T) -> String = { it.toString() },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
            .padding(dimensionResource(R.dimen.spacer_xxs)),
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) Modifier.background(Lime100, CircleShape)
                        else Modifier
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = dimensionResource(R.dimen.spacer_2xs)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tabLabel(tab),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
