package com.hexis.bi.utils.constants

object MeasurementConstants {
    const val CM_TO_IN = 2.54f
    const val KG_TO_LB = 2.20462f
    const val KM_TO_MI = 0.621371f
    const val INCHES_PER_FOOT = 12

    const val UNIT_SYSTEM_METRIC = "Metric"
    const val UNIT_SYSTEM_IMPERIAL = "Imperial"

    /** Below this cm delta a measurement is treated as unchanged (rounding noise). */
    const val CHANGE_EPSILON_CM = 0.01f

    /** Animation timings for the Results preview exit fade transition (ms). */
    const val RESULTS_PREVIEW_EXIT_FADE_MS = 200
    const val RESULTS_PREVIEW_EXIT_SETTLE_MS = 24
}
