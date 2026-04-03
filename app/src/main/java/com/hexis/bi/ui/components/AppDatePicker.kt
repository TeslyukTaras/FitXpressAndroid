package com.hexis.bi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.utils.millisToDobString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePicker(
    state: DatePickerState,
    onDismissRequest: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val colors = DatePickerDefaults.colors(
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
        dateTextFieldColors = null,
    )

    AppDialog {
        Column(modifier = Modifier.fillMaxWidth()) {
            DatePicker(
                state = state,
                colors = colors,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(R.dimen.padding_medium),
                        end = dimensionResource(R.dimen.padding_medium),
                        bottom = dimensionResource(R.dimen.spacer_s),
                    ),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
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
            }
        }
    }
}
