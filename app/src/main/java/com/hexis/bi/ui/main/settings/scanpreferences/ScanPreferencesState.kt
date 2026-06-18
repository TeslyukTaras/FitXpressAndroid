package com.hexis.bi.ui.main.settings.scanpreferences

import com.hexis.bi.domain.body.BodyMeasurementRegion

data class ScanPreferencesState(
    val isMetric: Boolean = true,
    val voiceGuidanceEnabled: Boolean = true,
    val selectedZones: Set<BodyMeasurementRegion> = BodyMeasurementRegion.measurableRegions.toSet(),
    val isSaving: Boolean = false,
) {
    val allZonesSelected: Boolean
        get() = selectedZones.size == BodyMeasurementRegion.measurableRegions.size
}
