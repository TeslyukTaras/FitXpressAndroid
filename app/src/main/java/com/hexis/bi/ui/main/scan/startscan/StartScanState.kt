package com.hexis.bi.ui.main.scan.startscan

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.data.scan.ScanProgress

data class ScanInstruction(
    @StringRes val textRes: Int,
    val completed: Boolean = false,
)

data class StartScanState(
    val currentStep: Int = 1,
    val voiceVolume: Float = 0.7f,
    val steps: List<List<ScanInstruction>> = defaultSteps,
    val isComplete: Boolean = false,
    val scanProgress: ScanProgress? = null,
    val shouldLaunchCamera: Boolean = true,
    val shouldNavigateBack: Boolean = false,
    val retakeOnErrorDismiss: Boolean = false,
) {
    val isProcessing: Boolean
        get() = scanProgress is ScanProgress.Submitting || scanProgress is ScanProgress.Processing
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
