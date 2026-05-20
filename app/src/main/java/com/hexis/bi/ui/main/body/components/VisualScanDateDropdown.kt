package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.main.body.VisualScanOption
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.constants.GlassConstants
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun VisualScanDateDropdown(
    label: String,
    selectedTimestamp: Long?,
    options: List<VisualScanOption>,
    dateFormatter: SimpleDateFormat,
    onScanSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val accentColor = DarkTheme.extendedColors.accentBlue
    val selectedDate = selectedTimestamp?.let { dateFormatter.format(Date(it)) }
        ?: stringResource(R.string.body_visual_value_missing)

    Row(
        modifier = modifier
            .height(dimensionResource(R.dimen.icon_normalized))
            .clickable(enabled = options.isNotEmpty()) { expanded = true },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = selectedDate,
            style = MaterialTheme.typography.bodyMedium,
            color = accentColor,
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacer_xs)),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
                .size(dimensionResource(R.dimen.icon_small))
                .rotate(if (expanded) 270f else 90f),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.72f),
        ) {
            options.forEach { option ->
                val selected = option.timestamp == selectedTimestamp
                Text(
                    text = dateFormatter.format(Date(option.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = GlassConstants.SELECTION_HIGHLIGHT_ALPHA,
                                )
                            } else {
                                Color.Transparent
                            },
                        )
                        .clickable {
                            expanded = false
                            onScanSelected(option.timestamp)
                        }
                        .padding(
                            vertical = dimensionResource(R.dimen.spacer_m),
                            horizontal = dimensionResource(R.dimen.spacer_s),
                        ),
                )
            }
        }
    }
}
