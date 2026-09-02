package com.hexis.bi.data.body

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hexis.bi.data.user.FirestoreSchema
import com.hexis.bi.data.user.FirestoreSchema.PhysiquePredictionFields as Field
import com.hexis.bi.intelligence.prediction.CalibrationResult
import com.hexis.bi.intelligence.prediction.PredictionConstants
import com.hexis.bi.intelligence.prediction.PredictionSeries
import com.hexis.bi.intelligence.prediction.ScanDay
import com.hexis.bi.intelligence.prediction.WeeklyPrediction
import com.hexis.bi.intelligence.prediction.calibrate
import com.hexis.bi.intelligence.prediction.predictWeekly
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class PhysiquePredictionRepository internal constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    suspend fun calibrateAndStore(days: List<ScanDay>): Result<Map<PredictionSeries, Double>> = try {
        val uid = auth.currentUser?.uid
        if (uid == null || days.size < PredictionConstants.MIN_BUCKETS) {
            Result.success(emptyMap())
        } else {
            Result.success(runTransaction(uid, days))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "Physique prediction calibration skipped")
        Result.failure(e)
    }

    private suspend fun runTransaction(
        uid: String,
        days: List<ScanDay>,
    ): Map<PredictionSeries, Double> {
        val settingsRef = settingsDoc(uid)
        val actual = days.last()
        val historyRef = settingsRef
            .collection(FirestoreSchema.PREDICTION_HISTORY_COLLECTION)
            .document(actual.scanId)

        return firestore.runTransaction { txn ->
            val settings = txn.get(settingsRef)
            val pending = readPending(settings)
            var gains = readGains(settings)
            if (pending?.sourceScanId == actual.scanId) return@runTransaction gains

            val alreadyApplied = txn.get(historyRef).exists()
            if (alreadyApplied) return@runTransaction gains

            var updateCount = settings.getLong(Field.UPDATE_COUNT) ?: 0L

            if (pending != null) {
                val result = calibrate(pending, actual, gains)
                txn.set(historyRef, historyDocument(result))
                gains = result.gainsAfter
                updateCount += 1
            }

            val next = predictWeekly(days, gains)
            txn.set(settingsRef, settingsDocument(gains, updateCount, next))
            gains
        }.await()
    }

    private fun settingsDoc(uid: String): DocumentReference = firestore
        .collection(FirestoreSchema.USERS_COLLECTION)
        .document(uid)
        .collection(FirestoreSchema.SETTINGS_COLLECTION)
        .document(FirestoreSchema.PHYSIQUE_PREDICTION_DOC)

    private fun settingsDocument(
        gains: Map<PredictionSeries, Double>,
        updateCount: Long,
        pending: WeeklyPrediction?,
    ): Map<String, Any?> = buildMap {
        put(Field.SCHEMA_VERSION, PredictionConstants.SCHEMA_VERSION)
        put(Field.ALGORITHM_VERSION, PredictionConstants.ALGORITHM_VERSION)
        put(Field.GAINS, gains.persistedKeyed())
        put(Field.UPDATE_COUNT, updateCount)
        put(Field.UPDATED_AT, FieldValue.serverTimestamp())
        pending?.let { put(Field.PENDING, pendingDocument(it)) }
    }

    private fun pendingDocument(prediction: WeeklyPrediction): Map<String, Any?> = mapOf(
        Field.SOURCE_SCAN_ID to prediction.sourceScanId,
        Field.SOURCE_AT to prediction.sourceDate.asTimestamp(),
        Field.TARGET_AT to prediction.targetDate.asTimestamp(),
        Field.SCANS_USED to prediction.bucketsUsed,
        Field.GAINS_USED to prediction.gainsUsed.persistedKeyed(),
        Field.SLOPE_PER_DAY to prediction.slopePerDay.persistedKeyed(),
        Field.SOURCE_VALUE to prediction.sourceValue.persistedKeyed(),
        Field.PREDICTED to prediction.predicted.persistedKeyed(),
    )

    private fun historyDocument(result: CalibrationResult): Map<String, Any?> = buildMap {
        put(Field.ALGORITHM_VERSION, PredictionConstants.ALGORITHM_VERSION)
        put(Field.SOURCE_SCAN_ID, result.sourceScanId)
        put(Field.ACTUAL_SCAN_ID, result.actualScanId)
        put(Field.SOURCE_AT, result.sourceDate.asTimestamp())
        put(Field.ACTUAL_AT, result.actualDate.asTimestamp())
        put(Field.ELAPSED_DAYS, result.elapsedDays)
        put(Field.SOURCE_VALUE, result.evaluated.mapValues { it.value.sourceValue }.persistedKeyed())
        put(Field.PREDICTED, result.evaluated.mapValues { it.value.predicted }.persistedKeyed())
        put(Field.ACTUAL_VALUE, result.evaluated.mapValues { it.value.actualValue }.persistedKeyed())
        put(Field.EXPECTED_DELTA, result.evaluated.mapValues { it.value.expectedDelta }.persistedKeyed())
        put(Field.ACTUAL_DELTA, result.evaluated.mapValues { it.value.actualDelta }.persistedKeyed())
        put(Field.RATIO, result.evaluated.mapValues { it.value.ratio }.persistedKeyed())
        put(Field.GAINS_BEFORE, result.gainsBefore.persistedKeyed())
        put(Field.GAINS_AFTER, result.gainsAfter.persistedKeyed())
        if (result.skipped.isNotEmpty()) {
            put(Field.SKIPPED, result.skipped.entries.associate { it.key.key to it.value.key })
        }
    }

    private fun readGains(settings: DocumentSnapshot): Map<PredictionSeries, Double> {
        val stored = settings.get(Field.GAINS) as? Map<*, *>
        return PredictionSeries.PERSISTED.associateWith { series ->
            (stored?.get(series.key) as? Number)?.toDouble()
                ?.coerceIn(PredictionConstants.MIN_GAIN, PredictionConstants.MAX_GAIN)
                ?: PredictionConstants.DEFAULT_GAIN
        }
    }

    private fun readPending(settings: DocumentSnapshot): WeeklyPrediction? {
        val stored = settings.get(Field.PENDING) as? Map<*, *> ?: return null
        val sourceScanId = stored[Field.SOURCE_SCAN_ID] as? String ?: return null
        val sourceAt = stored[Field.SOURCE_AT] as? Timestamp ?: return null
        val targetAt = stored[Field.TARGET_AT] as? Timestamp
        return WeeklyPrediction(
            sourceScanId = sourceScanId,
            sourceDate = sourceAt.asDate(),
            targetDate = targetAt?.asDate() ?: sourceAt.asDate(),
            bucketsUsed = (stored[Field.SCANS_USED] as? Number)?.toInt() ?: 0,
            gainsUsed = stored.seriesMap(Field.GAINS_USED),
            slopePerDay = stored.seriesMap(Field.SLOPE_PER_DAY),
            sourceValue = stored.seriesMap(Field.SOURCE_VALUE),
            predicted = stored.seriesMap(Field.PREDICTED),
        )
    }

    private fun Map<*, *>.seriesMap(field: String): Map<PredictionSeries, Double> {
        val values = this[field] as? Map<*, *> ?: return emptyMap()
        return values.entries.mapNotNull { (key, value) ->
            val series = PredictionSeries.fromKey(key as? String ?: return@mapNotNull null)
            val number = value as? Number ?: return@mapNotNull null
            val finite = number.toDouble().takeIf(Double::isFinite) ?: return@mapNotNull null
            series?.let { it to finite }
        }.toMap()
    }

    private fun Map<PredictionSeries, Double>.persistedKeyed(): Map<String, Double> =
        entries.filter { it.key.persisted }.associate { it.key.key to it.value }

    private fun String.asTimestamp(): Timestamp =
        Timestamp(Date(LocalDate.parse(this).atStartOfDay(zone).toInstant().toEpochMilli()))

    private fun Timestamp.asDate(): String =
        LocalDate.ofInstant(toDate().toInstant(), zone).toString()
}
