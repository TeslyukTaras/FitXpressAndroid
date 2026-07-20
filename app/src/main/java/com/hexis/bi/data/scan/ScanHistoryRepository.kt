package com.hexis.bi.data.scan

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.hexis.bi.data.scan.api.MeasurementResponse
import com.hexis.bi.utils.constants.ScanFirestoreConstants.COLLECTION_SCANS
import com.hexis.bi.utils.constants.ScanFirestoreConstants.COLLECTION_USERS
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_AGE
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_BMI
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_BMR
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_BODY_PROGRESS_BEFORE_MEASUREMENT_ID
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_BODY_PROGRESS_ID
import com.hexis.bi.utils.constants.ScanFirestoreConstants.FIELD_BODY_PROGRESS_MODEL_URL
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
 * - [LIST_SUMMARY]: parent summary fields + `circumferenceParams/data` only per scan — history list,
 *   top change vs previous, and Home intelligence gauges.
 * - [FULL]: circumference + front + side linear subdocs each once; merged [measurements] + linear maps; [model3dUrl] from parent — Results / deltas.
 */
enum class ScanFetchProjection {
    TIMESTAMPS_ONLY,
    LIST_SUMMARY,
    FULL,
}

data class ScanRecord(
    val id: String = "",
    /** 3DLOOK measurement UUID (the API `id`), distinct from the Firestore document id. */
    val measurementId: String? = null,
    val timestamp: Long = 0L,
    val model3dUrl: String? = null,
    val measurements: Map<String, Float> = emptyMap(),
    /** Front-view linear / ANFA-style params (from API `front_linear_params`). */
    val frontLinearParams: Map<String, Float> = emptyMap(),
    /** Side-view linear / ANFA-style params (from API `side_linear_params`). */
    val sideLinearParams: Map<String, Float> = emptyMap(),
    val weightKg: Float? = null,
    val estimatedWeightKg: Float? = null,
    val bmi: Float? = null,
    val fatPercentage: Float? = null,
    val leanBodyMassKg: Float? = null,
    val fatBodyMassKg: Float? = null,
)

data class BodyProgressCache(
    val id: String,
    val modelUrl: String?,
)

class ScanHistoryRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    private fun scansCollection() =
        firestore.collection(COLLECTION_USERS)
            .document(auth.currentUser?.uid ?: error("Not authenticated"))
            .collection(COLLECTION_SCANS)

    /** @return the saved scan's document id (as used by [getScanRecordById] and `ScanRecord.id`). */
    suspend fun saveScan(
        response: MeasurementResponse,
        savedAtMillis: Long = System.currentTimeMillis(),
    ): Result<String> = runCatching {
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
            batch.set(
                scanRef.collection(SUB_CIRCUMFERENCE_PARAMS).document(PARAMS_DOC_ID),
                jsonObjectToNumericMap(it)
            )
        }
        response.frontLinearParams?.let {
            batch.set(
                scanRef.collection(SUB_FRONT_LINEAR_PARAMS).document(PARAMS_DOC_ID),
                jsonObjectToNumericMap(it)
            )
        }
        response.sideLinearParams?.let {
            batch.set(
                scanRef.collection(SUB_SIDE_LINEAR_PARAMS).document(PARAMS_DOC_ID),
                jsonObjectToNumericMap(it)
            )
        }
        response.subscriptionInfo?.let {
            batch.set(
                scanRef.collection(SUB_SUBSCRIPTION_INFO).document(PARAMS_DOC_ID),
                jsonObjectToAnyMap(it)
            )
        }

        batch.commit().await()
        Timber.d("saveScan: wrote %s with %d fields", docId, mainData.size)
        docId
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

    fun buildScanRecordFromResponse(
        response: MeasurementResponse,
        scanId: String,
        savedAtMillis: Long,
    ): ScanRecord {
        val circumference = response.circumferenceParams?.let { jsonObjectToFloatMap(it) }.orEmpty()
        val frontLinearParams =
            response.frontLinearParams?.let { jsonObjectToFloatMap(it) }.orEmpty()
        val sideLinearParams = response.sideLinearParams?.let { jsonObjectToFloatMap(it) }.orEmpty()
        val measurements = MeasurementMapper.mergeMeasurementParams(
            circumference = circumference,
            frontLinear = frontLinearParams,
            sideLinear = sideLinearParams,
        )

        return ScanRecord(
            id = scanId,
            measurementId = response.id,
            timestamp = savedAtMillis,
            model3dUrl = response.model3dUrl,
            measurements = measurements,
            frontLinearParams = frontLinearParams,
            sideLinearParams = sideLinearParams,
            weightKg = response.weight?.toFloat() ?: response.estimatedWeight?.toFloat(),
            estimatedWeightKg = response.estimatedWeight?.toFloat(),
            bmi = response.bmi?.toFloat() ?: response.estimatedBmi?.toFloat(),
            fatPercentage = response.fatPercentage?.toFloat(),
            leanBodyMassKg = response.leanBodyMass?.toFloat()
                ?: response.estimatedLeanBodyMass?.toFloat(),
            fatBodyMassKg = response.fatBodyMass?.toFloat()
                ?: response.estimatedFatBodyMass?.toFloat(),
        )
    }

    suspend fun getRecentScans(
        limit: Long = SCAN_HISTORY_DEFAULT_LIMIT,
        projection: ScanFetchProjection = ScanFetchProjection.FULL,
    ): Result<List<ScanRecord>> =
        fetchRecentScans(limit = limit, projection = projection)

    suspend fun getScansSavedSince(
        savedAtMillisInclusive: Long,
        projection: ScanFetchProjection = ScanFetchProjection.FULL,
    ): Result<List<ScanRecord>> =
        fetchScansSavedSince(savedAtMillisInclusive = savedAtMillisInclusive, projection = projection)

    suspend fun getBodyProgressCache(
        beforeMeasurementId: String,
        afterMeasurementId: String,
    ): Result<BodyProgressCache?> = runCatching {
        val doc = scanDocumentByMeasurementId(afterMeasurementId) ?: return@runCatching null
        if (doc.getString(FIELD_BODY_PROGRESS_BEFORE_MEASUREMENT_ID) != beforeMeasurementId) {
            return@runCatching null
        }
        val id = doc.getString(FIELD_BODY_PROGRESS_ID)?.takeUnless { it.isBlank() }
            ?: return@runCatching null
        BodyProgressCache(
            id = id,
            modelUrl = doc.getString(FIELD_BODY_PROGRESS_MODEL_URL)?.takeUnless { it.isBlank() },
        )
    }

    suspend fun saveBodyProgressCache(
        beforeMeasurementId: String,
        afterMeasurementId: String,
        progressId: String,
        modelUrl: String? = null,
    ): Result<Unit> = runCatching {
        val doc = scanDocumentByMeasurementId(afterMeasurementId)
            ?: error("Saved scan not found for measurement $afterMeasurementId")
        val fields = mutableMapOf<String, Any>(
            FIELD_BODY_PROGRESS_BEFORE_MEASUREMENT_ID to beforeMeasurementId,
            FIELD_BODY_PROGRESS_ID to progressId,
        )
        modelUrl?.takeUnless { it.isBlank() }?.let {
            fields[FIELD_BODY_PROGRESS_MODEL_URL] = it
        }
        doc.reference.set(fields, SetOptions.merge()).await()
    }

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

        buildProjectedScanRecords(snapshot.documents, projection)
    }

    private suspend fun fetchScansSavedSince(
        savedAtMillisInclusive: Long,
        projection: ScanFetchProjection,
    ): Result<List<ScanRecord>> = runCatching {
        val snapshot = scansCollection()
            .whereGreaterThanOrEqualTo(FIELD_SAVED_AT, Timestamp(Date(savedAtMillisInclusive)))
            .orderBy(FIELD_SAVED_AT, Query.Direction.DESCENDING)
            .get()
            .await()

        buildProjectedScanRecords(snapshot.documents, projection)
    }

    private suspend fun buildProjectedScanRecords(
        documents: List<DocumentSnapshot>,
        projection: ScanFetchProjection,
    ): List<ScanRecord> = coroutineScope {
        documents.map { doc ->
            async {
                when (projection) {
                    ScanFetchProjection.TIMESTAMPS_ONLY -> ScanRecord(
                        id = doc.id,
                        timestamp = doc.savedAtMillis(),
                    )

                    ScanFetchProjection.LIST_SUMMARY -> ScanRecord(
                        id = doc.id,
                        timestamp = doc.savedAtMillis(),
                        measurements = loadSubNumericParams(doc, SUB_CIRCUMFERENCE_PARAMS),
                        weightKg = doc.numericField(FIELD_WEIGHT, FIELD_ESTIMATED_WEIGHT),
                        estimatedWeightKg = doc.numericField(FIELD_ESTIMATED_WEIGHT),
                        fatPercentage = doc.numericField(FIELD_FAT_PERCENTAGE),
                        leanBodyMassKg = doc.numericField(FIELD_LEAN_BODY_MASS),
                        fatBodyMassKg = doc.numericField(FIELD_FAT_BODY_MASS),
                    )

                    ScanFetchProjection.FULL -> buildFullScanRecord(doc)
                }
            }
        }.awaitAll()
    }

    private suspend fun scanDocumentByMeasurementId(measurementId: String): DocumentSnapshot? =
        scansCollection()
            .whereEqualTo(FIELD_ID, measurementId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()

    /** Circumference + front/side linear maps merged into [measurements]; each subdoc read once. */
    private suspend fun buildFullScanRecord(doc: DocumentSnapshot): ScanRecord = coroutineScope {
        val circumferenceDeferred = async { loadSubNumericParams(doc, SUB_CIRCUMFERENCE_PARAMS) }
        val frontDeferred = async { loadSubNumericParams(doc, SUB_FRONT_LINEAR_PARAMS) }
        val sideDeferred = async { loadSubNumericParams(doc, SUB_SIDE_LINEAR_PARAMS) }

        val circumference = circumferenceDeferred.await()
        val frontLinearParams = frontDeferred.await()
        val sideLinearParams = sideDeferred.await()

        val measurements = MeasurementMapper.mergeMeasurementParams(
            circumference = circumference,
            frontLinear = frontLinearParams,
            sideLinear = sideLinearParams,
        )

        ScanRecord(
            id = doc.id,
            measurementId = doc.getString(FIELD_ID),
            timestamp = doc.savedAtMillis(),
            model3dUrl = doc.getString(FIELD_MODEL_3D_URL),
            measurements = measurements,
            frontLinearParams = frontLinearParams,
            sideLinearParams = sideLinearParams,
            weightKg = doc.numericField(FIELD_WEIGHT, FIELD_ESTIMATED_WEIGHT),
            estimatedWeightKg = doc.numericField(FIELD_ESTIMATED_WEIGHT),
            bmi = doc.numericField(FIELD_BMI, FIELD_ESTIMATED_BMI),
            fatPercentage = doc.numericField(FIELD_FAT_PERCENTAGE),
            leanBodyMassKg = doc.numericField(FIELD_LEAN_BODY_MASS),
            fatBodyMassKg = doc.numericField(FIELD_FAT_BODY_MASS),
        )
    }

    private fun DocumentSnapshot.numericField(vararg keys: String): Float? {
        for (key in keys) {
            val v = (get(key) as? Number)?.toFloat()
            if (v != null) return v
        }
        return null
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

    private fun jsonObjectToFloatMap(obj: JsonObject): Map<String, Float> =
        obj.mapNotNull { (key, value) ->
            val prim = value as? JsonPrimitive ?: return@mapNotNull null
            val f = prim.content.toFloatOrNull() ?: return@mapNotNull null
            key.snakeToCamel() to f
        }.toMap()

    private fun jsonObjectToAnyMap(obj: JsonObject): Map<String, Any> =
        obj.mapNotNull { (key, value) ->
            val prim = value as? JsonPrimitive ?: return@mapNotNull null
            val converted: Any = prim.content.toDoubleOrNull() ?: prim.content
            key.snakeToCamel() to converted
        }.toMap()
}
