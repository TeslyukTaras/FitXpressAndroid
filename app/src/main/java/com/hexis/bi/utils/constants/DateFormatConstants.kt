package com.hexis.bi.utils.constants

object DateFormatConstants {
    /** Short "Apr 14" style label shown on the Results screen header. */
    const val SHORT_MONTH_DAY = "MMM d"

    /** Full "April 14" style label shown on day-based detail screens. */
    const val FULL_MONTH_DAY = "MMMM d"

    /** Sortable timestamp used as the Firestore document ID for a saved scan. */
    const val SCAN_DOC_ID_TIMESTAMP = "yyyy-MM-dd_HH-mm-ss"
}
