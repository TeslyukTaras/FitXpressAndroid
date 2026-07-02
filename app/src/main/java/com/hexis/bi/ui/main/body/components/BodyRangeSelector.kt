package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.main.body.BodyTimeRange

@Composable
internal fun BodyRangeSelector(
    selected: BodyTimeRange,
    onSelected: (BodyTimeRange) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BodySegmentedToggleTrack(modifier = modifier) {
        BodyTimeRange.entries.forEachIndexed { index, range ->
            if (index > 0) {
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
            }
            BodySegmentedToggleChip(
                label = stringResource(range.labelRes),
                isSelected = range == selected,
                onClick = { onSelected(range) },
                enabled = enabled,
                width = dimensionResource(R.dimen.icon_large)
            )
        }
    }
}
