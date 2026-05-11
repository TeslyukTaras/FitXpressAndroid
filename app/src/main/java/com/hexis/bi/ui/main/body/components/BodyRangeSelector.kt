package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.main.body.BodyTimeRange
import com.hexis.bi.ui.theme.Blue100

@Composable
internal fun BodyRangeSelector(
    selected: BodyTimeRange,
    onSelected: (BodyTimeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .padding(dimensionResource(R.dimen.spacer_3xs)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyTimeRange.entries.forEach { range ->
            val isSelected = range == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .then(
                        if (isSelected) Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        else Modifier
                    )
                    .clickable { onSelected(range) }
                    .padding(
                        horizontal = dimensionResource(R.dimen.spacer_m),
                        vertical = dimensionResource(R.dimen.spacer_xxs),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(range.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) Blue100 else MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
