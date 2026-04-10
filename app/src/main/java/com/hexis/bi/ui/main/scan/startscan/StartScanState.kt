package com.hexis.bi.ui.main.scan.startscan

import androidx.annotation.StringRes
import com.hexis.bi.R

data class ScanInstruction(
    @StringRes val textRes: Int,
    val completed: Boolean = false,
)

data class StartScanState(
    val currentStep: Int = 1,
    val voiceVolume: Float = 0.7f,
    val steps: List<List<ScanInstruction>> = defaultSteps,
    val isComplete: Boolean = false,
) {
    val currentInstructions: List<ScanInstruction>
        get() = steps.getOrElse(currentStep - 1) { emptyList() }
}

private val defaultSteps = listOf(
    listOf(
        ScanInstruction(R.string.scan_step_1_a),
        ScanInstruction(R.string.scan_step_1_b),
    ),
    listOf(
        ScanInstruction(R.string.scan_step_2_a),
        ScanInstruction(R.string.scan_step_2_b),
    ),
    listOf(
        ScanInstruction(R.string.scan_step_3_a),
        ScanInstruction(R.string.scan_step_3_b),
    ),
)
