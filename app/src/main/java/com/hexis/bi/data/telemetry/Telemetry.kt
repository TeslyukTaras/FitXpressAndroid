package com.hexis.bi.data.telemetry

import com.hexis.bi.utils.constants.TelemetryOutcomes
import com.hexis.bi.utils.constants.TelemetryStages
import com.hexis.bi.utils.constants.TelemetryTriggers

internal enum class SyncTrigger(val wire: String) {
    Background(TelemetryTriggers.BACKGROUND),
    NewConnection(TelemetryTriggers.NEW_CONNECTION),
    Foreground(TelemetryTriggers.FOREGROUND),
    Launch(TelemetryTriggers.LAUNCH),
    ManualRefresh(TelemetryTriggers.MANUAL_REFRESH),
    ;

    companion object {
        fun ofWire(wire: String?): SyncTrigger =
            entries.firstOrNull { it.wire == wire } ?: Background
    }
}

internal enum class SyncStage(val wire: String) {
    ScanSync(TelemetryStages.SCAN_SYNC),
    QuickSyncSource(TelemetryStages.QUICK_SYNC_SOURCE),
    HistoricalBackfill(TelemetryStages.HISTORICAL_BACKFILL),
}

internal enum class SyncOutcome(val wire: String) {
    Complete(TelemetryOutcomes.COMPLETE),
    Partial(TelemetryOutcomes.PARTIAL),
    Failed(TelemetryOutcomes.FAILED),
}

internal interface Telemetry {
    fun healthSyncCompleted(
        trigger: SyncTrigger,
        durationMs: Long,
        outcome: SyncOutcome,
        failedOperationCount: Int,
    )

    fun healthSyncFailed(trigger: SyncTrigger, stage: SyncStage, error: Throwable)

    fun intelligenceRunCompleted(
        durationMs: Long,
        stillLearning: Boolean,
        findingCount: Int,
        metricsOk: Int,
        metricsTotal: Int,
    )

    fun identify(userId: String?)
}
