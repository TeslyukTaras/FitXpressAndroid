package com.hexis.bi.data.scan

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hexis.bi.data.scan.api.MeasurementResponse
import com.hexis.bi.utils.constants.ScanFirestoreConstants.COLLECTION_SCANS
import com.hexis.bi.utils.constants.ScanFirestoreConstants.COLLECTION_USERS
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_AGE
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_BMI
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_BMR
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_COMPLETED_AT
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_CREATED_AT
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_ESTIMATED_BMI
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_ESTIMATED_BMR
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_ESTIMATED_FAT_BODY_MASS
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_ESTIMATED_LEAN_BODY_MASS
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_ESTIMATED_WEIGHT
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_FAT_BODY_MASS
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_FAT_PERCENTAGE
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_GENDER
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_HEIGHT
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_ID
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_LEAN_BODY_MASS
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_MODEL_3D_URL
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_MODEL_PREVIEW_PNG_BASE64
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_SAVED_AT
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_STATUS
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_URL
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_WEIGHT
import com.hexis.bi.utils.constants.ScanFirestoreConstants.PARAMS_DOC_ID
import com.hexis.bi.utils.constants.ScanFirestoreConstants.SCAN_HISTORY_DEFAULT_LIMIT
import com.hexis.bi.utils.constants.ScanFirestoreConstants.SCAN_HISTORY_TIMESTAMP_LOOKBACK
import com.hexis.bi.utils.constants.ScanFirestoreConstants.SUB_CIRCUMFERENCE_PARAMS
import com.hexis.bi.utils.constants.ScanFirestoreConstants.SUB_FRONT_LINEAR_PARAMS
import com.hexis.bi.utils.constants.ScanFirestoreConstants.SUB_SIDE_LINEAR_PARAMS
import com.hexis.bi.utils.constants.ScanFirestoreConstants.SUB_SUBSCRIPTION_INFO
import com.hexis.bi.utils.formatAsScanDocId
import com.hexis.bi.utils.snakeToCamel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber
import java.util.Date

/**
 * How much to load from Firestore for a scan query.
 *
 * Parent scan documents are always read in full (no `.select()` field mask in this build).
 * Projection controls **subcollection** reads and **merged maps** on [ScanRecord]:
 *
 * - [TIMESTAMPS_ONLY]: no subcollections — for date-range checks (e.g. reminders).
 * - [LIST_SUMMARY]: `circumferenceParams/data` only per scan — history list + top change vs previous.
 * - [FULL]: circumference + front + side linear subdocs each once; merged [measurements] + linear maps; [model3dUrl] from parent — Results / deltas.
 */
enum class ScanFetchProjection {
    TIMESTAMPS_ONLY,
    LIST_SUMMARY,
    FULL,
}

data class ScanRecord(
    val id: String = "",
    val timestamp: Long = 0L,
    val model3dUrl: String? = null,
    /** Low-res PNG of the 3D preview (base64), for history thumbnails; optional on older scans. */
    val modelPreviewPngBase64: String? = null,
    val measurements: Map<String, Float> = emptyMap(),
    /** Front-view linear / ANFA-style params (from API `front_linear_params`). */
    val frontLinearParams: Map<String, Float> = emptyMap(),
    /** Side-view linear / ANFA-style params (from API `side_linear_params`). */
    val sideLinearParams: Map<String, Float> = emptyMap(),
)

class ScanHistoryRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    private fun scansCollection() =
        firestore.collection(COLLECTION_USERS)
            .document(auth.currentUser?.uid ?: error("Not authenticated"))
            .collection(COLLECTION_SCANS)

    suspend fun saveScan(
        response: MeasurementResponse,
        savedAtMillis: Long = System.currentTimeMillis(),
    ): Result<Unit> = runCatching {
        val docId = Date(savedAtMillis).formatAsScanDocId()
        val scanRef = scansCollection().document(docId)

        val mainData = buildMap<String, Any?> {
            put(FIELD_ID, response.id)
            put(FIELD_STATUS, response.status)
            put(FIELD_URL, response.url)
            put(FIELD_CREATED_AT, response.createdAt)
            put(FIELD_COMPLETED_AT, response.completedAt)
            put(FIELD_SAVED_AT, Timestamp(Date(savedAtMillis)))
            put(FIELD_GENDER, response.gender)
            put(FIELD_MODEL_3D_URL, response.model3dUrl)
            put(FIELD_HEIGHT, response.height)
            put(FIELD_WEIGHT, response.weight)
            put(FIELD_AGE, response.age)
            put(FIELD_BMI, response.bmi)
            put(FIELD_BMR, response.bmr)
            put(FIELD_FAT_PERCENTAGE, response.fatPercentage)
            put(FIELD_LEAN_BODY_MASS, response.leanBodyMass)
            put(FIELD_FAT_BODY_MASS, response.fatBodyMass)
            put(FIELD_ESTIMATED_BMI, response.estimatedBmi)
            put(FIELD_ESTIMATED_BMR, response.estimatedBmr)
            put(FIELD_ESTIMATED_WEIGHT, response.estimatedWeight)
            put(FIELD_ESTIMATED_LEAN_BODY_MASS, response.estimatedLeanBodyMass)
            put(FIELD_ESTIMATED_FAT_BODY_MASS, response.estimatedFatBodyMass)
        }.filterValues { it != null }

        val batch = firestore.batch()
        batch.set(scanRef, mainData)

        response.circumferenceParams?.let {
            batch.set(scanRef.collection(SUB_CIRCUMFERENCE_PARAMS).document(PARAMS_DOC_ID), jsonObjectToNumericMap(it))
        }
        response.frontLinearParams?.let {
            batch.set(scanRef.collection(SUB_FRONT_LINEAR_PARAMS).document(PARAMS_DOC_ID), jsonObjectToNumericMap(it))
        }
        response.sideLinearParams?.let {
            batch.set(scanRef.collection(SUB_SIDE_LINEAR_PARAMS).document(PARAMS_DOC_ID), jsonObjectToNumericMap(it))
        }
        response.subscriptionInfo?.let {
            batch.set(scanRef.collection(SUB_SUBSCRIPTION_INFO).document(PARAMS_DOC_ID), jsonObjectToAnyMap(it))
        }

        batch.commit().await()
        Timber.d("saveScan: wrote %s with %d fields", docId, mainData.size)
    }

    suspend fun getLatestScan(): Result<ScanRecord?> =
        fetchRecentScans(limit = 1, projection = ScanFetchProjection.FULL).map { it.firstOrNull() }

    /**
     * Returns the two scans that precede the just-persisted current scan, in order
     * `(previous, beforePrevious)`. Call after saving a fresh scan so the Results
     * screen can show deltas for both the latest and the prior reading.
     *
     * Either side may be null if the user doesn't have enough history yet.
     */
    suspend fun getPreviousTwoScans(): Result<Pair<ScanRecord?, ScanRecord?>> =
        fetchRecentScans(limit = 3, projection = ScanFetchProjection.FULL).map { scans ->
            scans.getOrNull(1) to scans.getOrNull(2)
        }

    suspend fun getRecentScans(
        limit: Long = SCAN_HISTORY_DEFAULT_LIMIT,
        projection: ScanFetchProjection = ScanFetchProjection.FULL,
    ): Result<List<ScanRecord>> =
        fetchRecentScans(limit = limit, projection = projection)

    /**
     * Loads one scan by document id with full measurement subdocs (for Results fast path).
     */
    suspend fun getScanRecordById(scanId: String): Result<ScanRecord?> = runCatching {
        val doc = scansCollection().document(scanId).get().await()
        if (!doc.exists()) return@runCatching null
        buildFullScanRecord(doc)
    }

    /**
     * Scans strictly older than [savedAtCutoff], newest first (for previous / before-previous rows).
     */
    suspend fun getOlderScanRecordsBefore(
        savedAtCutoff: Timestamp,
        limit: Long = 2,
    ): Result<List<ScanRecord>> = runCatching {
        val snapshot = scansCollection()
            .whereLessThan(FIELD_SAVED_AT, savedAtCutoff)
            .orderBy(FIELD_SAVED_AT, Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        coroutineScope {
            snapshot.documents.map { doc ->
                async { buildFullScanRecord(doc) }
            }.awaitAll()
        }
    }

    /** True if any saved scan in recent history has [ScanRecord.timestamp] in the inclusive range. */
    suspend fun hasScanSavedBetween(
        startMillisInclusive: Long,
        endMillisInclusive: Long,
        lookback: Long = SCAN_HISTORY_TIMESTAMP_LOOKBACK,
    ): Result<Boolean> = fetchRecentScans(
        limit = lookback,
        projection = ScanFetchProjection.TIMESTAMPS_ONLY,
    ).map { scans ->
        scans.any { it.timestamp in startMillisInclusive..endMillisInclusive }
    }

    private suspend fun fetchRecentScans(
        limit: Long,
        projection: ScanFetchProjection,
    ): Result<List<ScanRecord>> = runCatching {
        val snapshot = scansCollection()
            .orderBy(FIELD_SAVED_AT, Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()

        coroutineScope {
            snapshot.documents.map { doc ->
                async {
                    when (projection) {
                        ScanFetchProjection.TIMESTAMPS_ONLY -> ScanRecord(
                            id = doc.id,
                            timestamp = doc.savedAtMillis(),
                        )
                        ScanFetchProjection.LIST_SUMMARY -> ScanRecord(
                            id = doc.id,
                            timestamp = doc.savedAtMillis(),
                            modelPreviewPngBase64 = doc.getString(FIELD_MODEL_PREVIEW_PNG_BASE64),
                            measurements = loadSubNumericParams(doc, SUB_CIRCUMFERENCE_PARAMS),
                        )
                        ScanFetchProjection.FULL -> buildFullScanRecord(doc)
                    }
                }
            }.awaitAll()
        }
    }

    /** Circumference + front/side linear maps merged into [measurements]; each subdoc read once. */
    private suspend fun buildFullScanRecord(doc: DocumentSnapshot): ScanRecord = coroutineScope {
        val circumferenceDeferred = async { loadSubNumericParams(doc, SUB_CIRCUMFERENCE_PARAMS) }
        val frontDeferred = async { loadSubNumericParams(doc, SUB_FRONT_LINEAR_PARAMS) }
        val sideDeferred = async { loadSubNumericParams(doc, SUB_SIDE_LINEAR_PARAMS) }

        val circumference = circumferenceDeferred.await()
        val frontLinearParams = frontDeferred.await()
        val sideLinearParams = sideDeferred.await()

        val measurements = LinkedHashMap<String, Float>()
        circumference.forEach { (k, v) -> measurements[k] = v }
        frontLinearParams.forEach { (k, v) -> measurements.putIfAbsent(k, v) }
        sideLinearParams.forEach { (k, v) -> measurements.putIfAbsent(k, v) }

        ScanRecord(
            id = doc.id,
            timestamp = doc.savedAtMillis(),
            model3dUrl = doc.getString(FIELD_MODEL_3D_URL),
            modelPreviewPngBase64 = doc.getString(FIELD_MODEL_PREVIEW_PNG_BASE64),
            measurements = measurements,
            frontLinearParams = frontLinearParams,
            sideLinearParams = sideLinearParams,
        )
    }

    private fun DocumentSnapshot.savedAtMillis(): Long =
        getTimestamp(FIELD_SAVED_AT)?.toDate()?.time ?: 0L

    private suspend fun loadSubNumericParams(
        doc: DocumentSnapshot,
        sub: String,
    ): LinkedHashMap<String, Float> {
        val subDoc = doc.reference.collection(sub).document(PARAMS_DOC_ID).get().await()
        return parseNumericMap(subDoc.data)
    }

    private fun parseNumericMap(data: Map<String, Any>?): LinkedHashMap<String, Float> {
        val out = LinkedHashMap<String, Float>()
        data?.forEach { (key, value) ->
            val f = (value as? Number)?.toFloat()
                ?: (value as? String)?.toFloatOrNull()
            if (f != null) out[key] = f
        }
        return out
    }

    private fun jsonObjectToNumericMap(obj: JsonObject): Map<String, Double> =
        obj.mapNotNull { (key, value) ->
            val prim = value as? JsonPrimitive ?: return@mapNotNull null
            val d = prim.content.toDoubleOrNull() ?: return@mapNotNull null
            key.snakeToCamel() to d
        }.toMap()

    private fun jsonObjectToAnyMap(obj: JsonObject): Map<String, Any> =
        obj.mapNotNull { (key, value) ->
            val prim = value as? JsonPrimitive ?: return@mapNotNull null
            val converted: Any = prim.content.toDoubleOrNull() ?: prim.content
            key.snakeToCamel() to converted
        }.toMap()
}
