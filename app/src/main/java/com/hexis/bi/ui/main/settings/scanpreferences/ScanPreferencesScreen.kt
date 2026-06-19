package com.hexis.bi.ui.main.settings.scanpreferences

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.components.AppCheckbox
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.AppRadioButton
import com.hexis.bi.ui.components.AppSwitch
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.utils.constants.BodyVisualConstants
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScanPreferencesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanPreferencesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LightStatusBarIcons()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is UiEvent.NavigateBack) onBack()
        }
    }

    BaseScreen(
        modifier = modifier
            .fillMaxSize()
            .screenBackground(),
        containerColor = Color.Transparent,
        isLoading = isLoading,
        error = error,
        onDismissError = viewModel::clearError,
        viewModel = viewModel,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.scan_preferences_title),
                background = Color.Transparent,
                onBack = onBack,
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium))
                    .padding(
                        top = dimensionResource(R.dimen.spacer_m),
                        bottom = dimensionResource(R.dimen.spacer_l),
                    )
                    .navigationBarsPadding(),
            ) {
                AppPrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = viewModel::save,
                    isLoading = state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        ) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

            UnitsCard(
                isMetric = state.isMetric,
                onSelectMetric = viewModel::selectMetric,
                onSelectImperial = viewModel::selectImperial,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            VoiceGuidanceCard(
                enabled = state.voiceGuidanceEnabled,
                onToggle = viewModel::toggleVoiceGuidance,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            MeasurementZonesCard(
                allSelected = state.allZonesSelected,
                selectedZones = state.selectedZones,
                onToggleAll = viewModel::toggleAllZones,
                onToggleZone = viewModel::toggleZone,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun UnitsCard(
    isMetric: Boolean,
    onSelectMetric: () -> Unit,
    onSelectImperial: () -> Unit,
) {
    BodyGlassCard(
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacer_m),
            top = dimensionResource(R.dimen.spacer_m),
            end = dimensionResource(R.dimen.spacer_m),
            bottom = dimensionResource(R.dimen.spacer_2xs),
        ),
    ) {
        SectionHeader(
            title = stringResource(R.string.scan_preferences_units),
            subtitle = stringResource(R.string.scan_preferences_units_subtitle),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
        Row(modifier = Modifier.selectableGroup()) {
            UnitOption(
                label = stringResource(R.string.scan_preferences_metric),
                selected = isMetric,
                onClick = onSelectMetric,
                modifier = Modifier.weight(1f),
            )
            UnitOption(
                label = stringResource(R.string.scan_preferences_imperial),
                selected = !isMetric,
                onClick = onSelectImperial,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun UnitOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = dimensionResource(R.dimen.spacer_2xs)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppRadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_2xs)))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun VoiceGuidanceCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    BodyGlassCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                SectionHeader(
                    title = stringResource(R.string.scan_preferences_voice_guidance),
                    subtitle = stringResource(R.string.scan_preferences_voice_guidance_subtitle),
                )
            }
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_2xl)))
            AppSwitch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun MeasurementZonesCard(
    allSelected: Boolean,
    selectedZones: Set<BodyMeasurementRegion>,
    onToggleAll: () -> Unit,
    onToggleZone: (BodyMeasurementRegion) -> Unit,
) {
    BodyGlassCard(
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacer_m),
            top = dimensionResource(R.dimen.spacer_m),
            end = dimensionResource(R.dimen.spacer_m),
            bottom = dimensionResource(R.dimen.spacer_2xs),
        ),
    ) {
        SectionHeader(
            title = stringResource(R.string.scan_preferences_zones_title),
            subtitle = stringResource(R.string.scan_preferences_zones_subtitle),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
        ZoneRow(
            label = stringResource(R.string.scan_preferences_zone_all),
            checked = allSelected,
            onToggle = onToggleAll,
        )
        BodyMeasurementRegion.measurableRegions.forEach { region ->
            ZoneRow(
                label = stringResource(BodyVisualConstants.visualHeaderLabelRes(region)),
                checked = region in selectedZones,
                onToggle = { onToggleZone(region) },
            )
        }
    }
}

@Composable
private fun ZoneRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(vertical = dimensionResource(R.dimen.spacer_2xs)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppCheckbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_2xs)))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
