package com.hexis.bi.utils.constants

object DateFormatConstants {
    /** Short "Apr 14" style label shown on the Results screen header. */
    const val SHORT_MONTH_DAY = "MMM d"

    /** Full "April 14" style label shown on day-based detail screens. */
    const val FULL_MONTH_DAY = "MMMM d"

    /** Day-of-month number, e.g. "14". */
    const val DAY_OF_MONTH = "d"

    /** Short month, e.g. "Jan". */
    const val MONTH_SHORT = "MMM"

    /** Full "April 2026" style label for month detail screens. */
    const val FULL_MONTH_YEAR = "MMMM yyyy"

    /** Four-digit year, e.g. "2026". */
    const val YEAR = "yyyy"

    /** Sortable timestamp used as the Firestore document ID for a saved scan. */
    const val SCAN_DOC_ID_TIMESTAMP = "yyyy-MM-dd_HH-mm-ss"

    const val SHORT_MONTH_DAY_YEAR = "MMM d, yyyy"
    const val HOUR_MINUTE_24 = "HH:mm"
}
