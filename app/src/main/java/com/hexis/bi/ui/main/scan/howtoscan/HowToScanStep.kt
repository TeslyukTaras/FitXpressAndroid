package com.hexis.bi.ui.main.scan.howtoscan

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.hexis.bi.R

/**
 * One state of the single-screen "Prepare Your Scan" flow: a subtitle, a section
 * header, three checklist items and the illustration shown for that step.
 */
data class HowToScanStep(
    @StringRes val subtitleRes: Int,
    @StringRes val headerRes: Int,
    val instructionsRes: List<Int>,
    @DrawableRes val imageRes: Int,
)

val howToScanSteps: List<HowToScanStep> = listOf(
    HowToScanStep(
        subtitleRes = R.string.how_to_scan_subtitle_default,
        headerRes = R.string.how_to_scan_step_1_header,
        instructionsRes = listOf(
            R.string.how_to_scan_step_1_a,
            R.string.how_to_scan_step_1_b,
            R.string.how_to_scan_step_1_c,
        ),
        imageRes = R.drawable.img_how_to_scan_1,
    ),
    HowToScanStep(
        subtitleRes = R.string.how_to_scan_subtitle_default,
        headerRes = R.string.how_to_scan_step_2_header,
        instructionsRes = listOf(
            R.string.how_to_scan_step_2_a,
            R.string.how_to_scan_step_2_b,
            R.string.how_to_scan_step_2_c,
        ),
        imageRes = R.drawable.img_how_to_scan_2,
    ),
    HowToScanStep(
        subtitleRes = R.string.how_to_scan_subtitle_phone,
        headerRes = R.string.how_to_scan_step_3_header,
        instructionsRes = listOf(
            R.string.how_to_scan_step_3_a,
            R.string.how_to_scan_step_3_b,
            R.string.how_to_scan_step_3_c,
        ),
        imageRes = R.drawable.img_how_to_scan_3,
    ),
    HowToScanStep(
        subtitleRes = R.string.how_to_scan_subtitle_default,
        headerRes = R.string.how_to_scan_step_4_header,
        instructionsRes = listOf(
            R.string.how_to_scan_step_4_a,
            R.string.how_to_scan_step_4_b,
            R.string.how_to_scan_step_4_c,
        ),
        imageRes = R.drawable.img_how_to_scan_4,
    ),
)
