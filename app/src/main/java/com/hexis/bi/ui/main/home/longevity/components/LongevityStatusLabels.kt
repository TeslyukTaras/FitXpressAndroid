package com.hexis.bi.ui.main.home.longevity.components

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.hexis.bi.R
import com.hexis.bi.domain.longevity.FoundationStatus
import com.hexis.bi.domain.longevity.LongevityDirection
import com.hexis.bi.ui.theme.NocturnePulseTheme

@get:StringRes
internal val LongevityDirection.labelRes: Int
    get() = when (this) {
        LongevityDirection.Strengthening -> R.string.longevity_direction_strengthening
        LongevityDirection.Holding -> R.string.longevity_direction_holding
        LongevityDirection.Mixed -> R.string.longevity_direction_mixed
        LongevityDirection.Weakening -> R.string.longevity_direction_weakening
        LongevityDirection.BuildingYourTrend -> R.string.longevity_direction_building
    }

@get:StringRes
internal val LongevityDirection.descriptionRes: Int
    get() = when (this) {
        LongevityDirection.Strengthening -> R.string.longevity_direction_strengthening_body
        LongevityDirection.Holding -> R.string.longevity_direction_holding_body
        LongevityDirection.Mixed -> R.string.longevity_direction_mixed_body
        LongevityDirection.Weakening -> R.string.longevity_direction_weakening_body
        LongevityDirection.BuildingYourTrend -> R.string.longevity_direction_building_body
    }

@get:StringRes
internal val FoundationStatus.labelRes: Int
    get() = when (this) {
        FoundationStatus.Strengthening -> R.string.longevity_status_strengthening
        FoundationStatus.Holding -> R.string.longevity_status_holding
        FoundationStatus.Mixed -> R.string.longevity_status_mixed
        FoundationStatus.Weakening -> R.string.longevity_status_weakening
        FoundationStatus.InsufficientData -> R.string.longevity_status_insufficient
    }

@Composable
@ReadOnlyComposable
internal fun statusColor(status: FoundationStatus): Color = when (status) {
    FoundationStatus.Strengthening -> MaterialTheme.colorScheme.primary
    FoundationStatus.Holding -> MaterialTheme.colorScheme.primary
    FoundationStatus.Mixed -> NocturnePulseTheme.extendedColors.yellow
    FoundationStatus.Weakening -> NocturnePulseTheme.extendedColors.negative
    FoundationStatus.InsufficientData -> NocturnePulseTheme.extendedColors.gray200
}

@Composable
@ReadOnlyComposable
internal fun directionColor(direction: LongevityDirection): Color = when (direction) {
    LongevityDirection.Strengthening -> MaterialTheme.colorScheme.primary
    LongevityDirection.Holding -> MaterialTheme.colorScheme.primary
    LongevityDirection.Mixed -> NocturnePulseTheme.extendedColors.yellow
    LongevityDirection.Weakening -> NocturnePulseTheme.extendedColors.negative
    LongevityDirection.BuildingYourTrend -> NocturnePulseTheme.extendedColors.gray200
}
