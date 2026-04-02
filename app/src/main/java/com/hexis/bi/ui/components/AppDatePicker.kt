package com.hexis.bi.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.utils.millisToDobString

@Composable
fun AppDatePicker(
    state: DatePickerState,
    onDismissRequest: () -> Unit,
    onSelect: (String) -> Unit
) {
    val datePickerColors = DatePickerDefaults.colors(
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        headlineContentColor = MaterialTheme.colorScheme.onBackground,
        weekdayContentColor = MaterialTheme.colorScheme.onBackground,
        subheadContentColor = MaterialTheme.colorScheme.onBackground,
        navigationContentColor = MaterialTheme.colorScheme.onBackground,
        yearContentColor = MaterialTheme.colorScheme.onBackground,
        disabledYearContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
        currentYearContentColor = MaterialTheme.colorScheme.primary,
        selectedYearContentColor = MaterialTheme.colorScheme.background,
        disabledSelectedYearContentColor = MaterialTheme.colorScheme.background.copy(alpha = 0.38f),
        selectedYearContainerColor = MaterialTheme.colorScheme.primary,
        disabledSelectedYearContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
        dayContentColor = MaterialTheme.colorScheme.onBackground,
        disabledDayContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
        selectedDayContentColor = MaterialTheme.colorScheme.background,
        disabledSelectedDayContentColor = MaterialTheme.colorScheme.background.copy(alpha = 0.38f),
        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
        disabledSelectedDayContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
        todayContentColor = MaterialTheme.colorScheme.primary,
        todayDateBorderColor = MaterialTheme.colorScheme.primary,
        dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onBackground,
        dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        dividerColor = MaterialTheme.colorScheme.outline,
        dateTextFieldColors = null
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        colors = datePickerColors,
        shape = MaterialTheme.shapes.medium,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) onSelect(millis.millisToDobString())
                onDismissRequest()
            }) {
                Text(
                    text = stringResource(R.string.action_save),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
    ) {
        DatePicker(state = state, colors = datePickerColors)
    }
}

