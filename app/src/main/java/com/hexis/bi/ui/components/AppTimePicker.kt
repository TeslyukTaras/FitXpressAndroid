package com.hexis.bi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.DialogWindowBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePicker(
    state: TimePickerState,
    onDismissRequest: () -> Unit,
    onSelect: (hour: Int, minute: Int) -> Unit,
) {
    val colors = TimePickerDefaults.colors(
        clockDialColor = MaterialTheme.colorScheme.tertiary,
        clockDialSelectedContentColor = DialogWindowBackground,
        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onBackground,
        selectorColor = MaterialTheme.colorScheme.primary,
        containerColor = DialogWindowBackground,
        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.tertiary,
        timeSelectorSelectedContentColor = DialogWindowBackground,
        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onBackground,
        periodSelectorBorderColor = MaterialTheme.colorScheme.outline,
        periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
        periodSelectorUnselectedContainerColor = DialogWindowBackground,
        periodSelectorSelectedContentColor = DialogWindowBackground,
        periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onBackground,
    )

    AppDialog(hasCloseButton = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_medium)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimePicker(state = state, colors = colors)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                TextButton(onClick = { onSelect(state.hour, state.minute) }) {
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
