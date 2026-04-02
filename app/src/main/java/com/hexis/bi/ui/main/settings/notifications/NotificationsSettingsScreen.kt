package com.hexis.bi.ui.main.settings.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.enums.ReminderDay
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppListPicker
import com.hexis.bi.ui.components.AppSwitch
import com.hexis.bi.ui.components.AppTimePicker
import com.hexis.bi.utils.formatHour
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showAnyDialog = state.showDayPicker || state.showTimePicker

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showAnyDialog)
                        Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    else Modifier
                ),
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.screen_notifications),
                    onBack = onBack,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            ) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                // General notifications card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(all = dimensionResource(R.dimen.spacer_m)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m))
                ) {
                    SwitchRow(
                        label = stringResource(R.string.notifications_toggle),
                        checked = state.notificationsEnabled,
                        onCheckedChange = viewModel::toggleNotifications,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = dimensionResource(R.dimen.border_thin),
                    )
                    SwitchRow(
                        label = stringResource(R.string.notifications_voice),
                        checked = state.voiceEnabled,
                        onCheckedChange = viewModel::toggleVoice,
                    )
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                // Remind to scan card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(all = dimensionResource(R.dimen.spacer_2xs)),
                ) {
                    // Header row with toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = dimensionResource(R.dimen.spacer_2xs)),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.notifications_remind_to_scan),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
                            Text(
                                text = stringResource(R.string.notifications_remind_to_scan_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        AppSwitch(
                            checked = state.remindToScanEnabled,
                            onCheckedChange = viewModel::toggleRemindToScan,
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = dimensionResource(R.dimen.border_thin),
                        modifier = Modifier.padding(all = dimensionResource(R.dimen.spacer_2xs))
                    )

                    // Day row
                    PickerRow(
                        label = stringResource(R.string.notifications_day),
                        value = state.reminderDay.name,
                        enabled = state.remindToScanEnabled,
                        onClick = viewModel::showDayPicker,
                    )

                    // Time row
                    PickerRow(
                        label = stringResource(R.string.notifications_time),
                        value = state.reminderHour.formatHour(),
                        enabled = state.remindToScanEnabled,
                        onClick = viewModel::showTimePicker,
                    )
                }
            }
        }

        // Day picker dialog
        if (state.showDayPicker) {
            AppListPicker(
                items = ReminderDay.entries,
                selectedItem = state.reminderDay,
                onItemSelected = viewModel::selectDay,
                onDismiss = viewModel::hideDayPicker,
                itemLabel = { it.name },
                title = stringResource(R.string.notifications_day).trimEnd(':'),
            )
        }

        // Time picker dialog
        if (state.showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = state.reminderHour,
                initialMinute = 0,
                is24Hour = false,
            )
            AppTimePicker(
                state = timePickerState,
                onDismissRequest = viewModel::hideTimePicker,
                onSelect = { hour, _ -> viewModel.selectTime(hour) },
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        AppSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun PickerRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(all = dimensionResource(R.dimen.spacer_2xs)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.primaryFixed,
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))
        Icon(
            painter = painterResource(R.drawable.ic_arrow),
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.primaryFixed,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
    }
}

