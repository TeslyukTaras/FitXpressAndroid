package com.hexis.bi.ui.main.settings.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.enums.ReminderDay
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppHorizontalGradientDivider
import com.hexis.bi.ui.components.AppListPicker
import com.hexis.bi.ui.components.AppScrollPicker
import com.hexis.bi.ui.components.AppSwitch
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.components.PickerColumnData
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.utils.constants.TimeConstants
import com.hexis.bi.utils.hour12ToHour24
import com.hexis.bi.utils.hour24ToHour12
import com.hexis.bi.utils.isHour24Pm
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val showAnyDialog = state.showDayPicker || state.showTimePicker
    val context = LocalContext.current

    LightStatusBarIcons()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.toggleNotifications(true)
        else viewModel.notifyNotificationsPermissionDenied()
    }

    fun onNotificationToggleRequest(enabled: Boolean) {
        if (enabled) {
            if (Build.VERSION.SDK_INT >= 33) {
                val has = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (!has) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return
                }
            }
            viewModel.toggleNotifications(true)
        } else viewModel.toggleNotifications(false)
    }

    Box(modifier = modifier) {
        BaseScreen(
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showAnyDialog)
                        Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    else Modifier
                )
                .screenBackground(),
            containerColor = Color.Transparent,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.notification_settings_title),
                    background = Color.Transparent,
                    onBack = onBack,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            ) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                BodyGlassCard {
                    SwitchRow(
                        label = stringResource(R.string.notifications_toggle),
                        checked = state.notificationsEnabled,
                        onCheckedChange = ::onNotificationToggleRequest,
                        enabled = state.settingsLoaded,
                    )

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
                    AppHorizontalGradientDivider()
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

                    SwitchRow(
                        label = stringResource(R.string.notifications_voice),
                        checked = state.voiceEnabled,
                        onCheckedChange = viewModel::toggleVoice,
                        enabled = state.settingsLoaded,
                    )
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                BodyGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AppSwitch(
                            checked = state.remindToScanEnabled,
                            onCheckedChange = viewModel::toggleRemindToScan,
                            enabled = state.settingsLoaded,
                        )
                    }

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
                    AppHorizontalGradientDivider()
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

                    PickerRow(
                        label = stringResource(R.string.notifications_day),
                        value = stringResource(state.reminderDay.labelRes()),
                        enabled = state.remindToScanEnabled,
                        onClick = viewModel::showDayPicker,
                    )

                    PickerRow(
                        label = stringResource(R.string.notifications_time),
                        value = formatHourOnly(state.reminderHour),
                        enabled = state.remindToScanEnabled,
                        onClick = viewModel::showTimePicker,
                    )
                }
            }
        }

        if (state.showDayPicker) {
            AppListPicker(
                items = ReminderDay.entries,
                selectedItem = state.reminderDay,
                onItemSelected = viewModel::selectDay,
                onDismiss = viewModel::hideDayPicker,
                itemLabel = { stringResource(it.labelRes()) },
                title = stringResource(R.string.notifications_day).trimEnd(':'),
            )
        }


        if (state.showTimePicker) {
            val hourState = rememberAppHourPickerState(initialHour = state.reminderHour)

            AppScrollPicker(
                title = stringResource(R.string.notifications_select_time),
                pickerColumns = hourState.toColumns(),
                onDismiss = viewModel::hideTimePicker,
                onConfirm = { viewModel.selectTime(hourState.selectedHour24) },
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
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        AppSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
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
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = dimensionResource(R.dimen.spacer_xs)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val valueColor =
            if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))
        Icon(
            painter = painterResource(R.drawable.ic_arrow),
            contentDescription = null,
            tint = valueColor,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
    }
}

private fun formatHourOnly(hour24: Int): String {
    val h12 = if (hour24 % TimeConstants.HOURS_IN_HALF_DAY == 0) TimeConstants.HOURS_IN_HALF_DAY
    else hour24 % TimeConstants.HOURS_IN_HALF_DAY
    val amPm = if (hour24 < TimeConstants.HOURS_IN_HALF_DAY) TimeConstants.AM else TimeConstants.PM
    return "$h12 $amPm"
}

@OptIn(ExperimentalFoundationApi::class)
private class AppHourPickerState(
    val hourPagerState: PagerState,
    val amPmPagerState: PagerState,
    val hours: List<Int> = TimeConstants.HOURS_12,
    val amPm: List<String> = TimeConstants.AM_PM,
) {
    val selectedHour24: Int
        get() = hour12ToHour24(
            hour12 = hours[hourPagerState.currentPage],
            isPm = amPmPagerState.currentPage == 1,
        )

    fun toColumns() = listOf(
        PickerColumnData(hourPagerState, hours.map { it.toString() }),
        PickerColumnData(amPmPagerState, amPm),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberAppHourPickerState(initialHour: Int): AppHourPickerState {
    val initialHour12 = initialHour.hour24ToHour12()
    val hourIndex = TimeConstants.HOURS_12.indexOf(initialHour12).coerceAtLeast(0)
    val hourPagerState = rememberPagerState(initialPage = hourIndex) { TimeConstants.HOURS_12.size }
    val amPmPagerState =
        rememberPagerState(initialPage = if (initialHour.isHour24Pm()) 1 else 0) { TimeConstants.AM_PM.size }

    return remember(hourPagerState, amPmPagerState) {
        AppHourPickerState(hourPagerState, amPmPagerState)
    }
}
