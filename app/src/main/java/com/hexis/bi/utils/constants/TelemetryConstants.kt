package com.hexis.bi.utils.constants

internal object TelemetryEvents {
    const val HEALTH_SYNC_COMPLETED = "health_sync_completed"

    const val HEALTH_SYNC_FAILED = "health_sync_failed"

    const val INTELLIGENCE_RUN_COMPLETED = "intelligence_run_completed"
}

internal object TelemetryParams {
    const val TRIGGER = "trigger"

    const val DURATION_MS = "duration_ms"

    const val OUTCOME = "outcome"

    const val FAILED_OPERATION_COUNT = "failed_operation_count"

    const val STAGE = "stage"

    const val ERROR_CODE = "error_code"

    const val STILL_LEARNING = "still_learning"

    const val FINDING_COUNT = "finding_count"

    const val METRICS_OK = "metrics_ok"

    const val METRICS_TOTAL = "metrics_total"
}

internal object TelemetryUserProperties {
    const val STILL_LEARNING = "still_learning"
}

internal object TelemetryTriggers {
    const val BACKGROUND = "background"

    const val NEW_CONNECTION = "new_connection"

    const val FOREGROUND = "foreground"

    const val LAUNCH = "launch"

    const val MANUAL_REFRESH = "manual_refresh"
}

internal object TelemetryOutcomes {
    const val COMPLETE = "complete"

    const val PARTIAL = "partial"

    const val FAILED = "failed"
}

internal object TelemetryStages {
    const val SCAN_SYNC = "scan_sync"

    const val QUICK_SYNC_SOURCE = "quick_sync_source"

    const val HISTORICAL_BACKFILL = "historical_backfill"
}

internal object TelemetryErrorCodes {
    const val UNKNOWN = "unknown"

    const val SOURCE_UNAVAILABLE = "health/source_unavailable"

    const val NOT_AUTHENTICATED = "health/not_authenticated"

    const val BACKFILL_FAILED = "health/backfill_failed"

    const val FIRESTORE_DOMAIN = "firestore"

    const val FUNCTIONS_DOMAIN = "functions"

    const val AUTH_DOMAIN = "auth"

    const val IO_DOMAIN = "io"
}

internal object TelemetryWorkData {
    const val KEY_TRIGGER = "telemetry_trigger"
}
