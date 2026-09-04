package com.hexis.bi.data.telemetry

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import com.hexis.bi.data.health.sync.HealthBackfillFailed
import com.hexis.bi.data.health.sync.HealthSourceUnavailable
import com.hexis.bi.utils.constants.TelemetryErrorCodes
import com.hexis.bi.utils.constants.TelemetryEvents
import com.hexis.bi.utils.constants.TelemetryParams
import com.hexis.bi.utils.constants.TelemetryUserProperties
import java.io.IOException
import java.util.Locale
import timber.log.Timber

internal class FirebaseTelemetry(
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics,
) : Telemetry {

    override fun healthSyncCompleted(
        trigger: SyncTrigger,
        durationMs: Long,
        outcome: SyncOutcome,
        failedOperationCount: Int,
    ) {
        log(TelemetryEvents.HEALTH_SYNC_COMPLETED) {
            putString(TelemetryParams.TRIGGER, trigger.wire)
            putLong(TelemetryParams.DURATION_MS, durationMs.coerceAtLeast(0))
            putString(TelemetryParams.OUTCOME, outcome.wire)
            putLong(TelemetryParams.FAILED_OPERATION_COUNT, failedOperationCount.coerceAtLeast(0).toLong())
        }
    }

    override fun healthSyncFailed(trigger: SyncTrigger, stage: SyncStage, error: Throwable) {
        log(TelemetryEvents.HEALTH_SYNC_FAILED) {
            putString(TelemetryParams.TRIGGER, trigger.wire)
            putString(TelemetryParams.STAGE, stage.wire)
            putString(TelemetryParams.ERROR_CODE, errorCodeOf(error))
        }
        crashlytics.recordException(error)
    }

    override fun intelligenceRunCompleted(
        durationMs: Long,
        stillLearning: Boolean,
        findingCount: Int,
        metricsOk: Int,
        metricsTotal: Int,
    ) {
        log(TelemetryEvents.INTELLIGENCE_RUN_COMPLETED) {
            putLong(TelemetryParams.DURATION_MS, durationMs.coerceAtLeast(0))
            putLong(TelemetryParams.STILL_LEARNING, if (stillLearning) 1L else 0L)
            putLong(TelemetryParams.FINDING_COUNT, findingCount.coerceAtLeast(0).toLong())
            putLong(TelemetryParams.METRICS_OK, metricsOk.coerceAtLeast(0).toLong())
            putLong(TelemetryParams.METRICS_TOTAL, metricsTotal.coerceAtLeast(0).toLong())
        }
        analytics.setUserProperty(TelemetryUserProperties.STILL_LEARNING, stillLearning.toString())
    }

    override fun identify(userId: String?) {
        analytics.setUserId(userId)
        crashlytics.setUserId(userId.orEmpty())
    }

    private fun log(event: String, params: Bundle.() -> Unit) {
        val bundle = Bundle().apply(params)
        analytics.logEvent(event, bundle)
        Timber.d("telemetry %s %s", event, bundle)
    }
}

internal fun errorCodeOf(error: Throwable): String = when (error) {
    is HealthSourceUnavailable -> TelemetryErrorCodes.SOURCE_UNAVAILABLE
    is HealthBackfillFailed -> TelemetryErrorCodes.BACKFILL_FAILED
    is FirebaseFunctionsException -> qualify(TelemetryErrorCodes.FUNCTIONS_DOMAIN, error.code.name)
    is FirebaseFirestoreException -> qualify(TelemetryErrorCodes.FIRESTORE_DOMAIN, error.code.name)
    is FirebaseAuthException -> qualify(TelemetryErrorCodes.AUTH_DOMAIN, error.errorCode)
    is IOException -> qualify(TelemetryErrorCodes.IO_DOMAIN, error.typeName())
    else -> error.typeName()
}

private fun qualify(domain: String, code: String?): String {
    val slug = code?.takeIf { it.isNotBlank() }?.lowercase(Locale.ROOT)
        ?: return "$domain/${TelemetryErrorCodes.UNKNOWN}"
    return "$domain/$slug"
}

private fun Throwable.typeName(): String =
    this::class.java.simpleName.takeIf { it.isNotBlank() } ?: TelemetryErrorCodes.UNKNOWN
