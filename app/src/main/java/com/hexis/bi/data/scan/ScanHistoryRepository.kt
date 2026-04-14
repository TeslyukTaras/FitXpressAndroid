package com.hexis.bi.data.scan

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hexis.bi.data.scan.api.MeasurementResponse
import com.hexis.bi.utils.formatAsScanDocId
import com.hexis.bi.utils.snakeToCamel
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber
import java.util.Date

data class ScanRecord(
    val id: String = "",
    val timestamp: Long = 0L,
    val model3dUrl: String? = null,
    val measurements: Map<String, Float> = emptyMap(),
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
        getRecentScans(limit = 1).map { it.firstOrNull() }

    /**
     * Returns the second-most-recent scan. Call this after a fresh scan has been
     * persisted to fetch the prior reading for delta comparison.
     */
    suspend fun getPreviousScan(): Result<ScanRecord?> =
        getRecentScans(limit = 2).map { it.getOrNull(1) }

    private suspend fun getRecentScans(limit: Long): Result<List<ScanRecord>> = runCatching {
        val snapshot = scansCollection()
            .orderBy(FIELD_SAVED_AT, Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()

        snapshot.documents.map { doc ->
            val measurements = LinkedHashMap<String, Float>()
            MEASUREMENT_SUBCOLLECTIONS.forEach { sub ->
                val subDoc = doc.reference.collection(sub).document(PARAMS_DOC_ID).get().await()
                subDoc.data?.forEach { (key, value) ->
                    val f = (value as? Number)?.toFloat()
                        ?: (value as? String)?.toFloatOrNull()
                    if (f != null) measurements.putIfAbsent(key, f)
                }
            }

            ScanRecord(
                id = doc.id,
                timestamp = doc.getTimestamp(FIELD_SAVED_AT)?.toDate()?.time ?: 0L,
                model3dUrl = doc.getString(FIELD_MODEL_3D_URL),
                measurements = measurements,
            )
        }
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

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_SCANS = "scans"

        private const val SUB_CIRCUMFERENCE_PARAMS = "circumferenceParams"
        private const val SUB_FRONT_LINEAR_PARAMS = "frontLinearParams"
        private const val SUB_SIDE_LINEAR_PARAMS = "sideLinearParams"
        private const val SUB_SUBSCRIPTION_INFO = "subscriptionInfo"

        private const val PARAMS_DOC_ID = "data"

        private val MEASUREMENT_SUBCOLLECTIONS = listOf(
            SUB_CIRCUMFERENCE_PARAMS,
            SUB_FRONT_LINEAR_PARAMS,
            SUB_SIDE_LINEAR_PARAMS,
        )

        private const val FIELD_ID = "id"
        private const val FIELD_STATUS = "status"
        private const val FIELD_URL = "url"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_COMPLETED_AT = "completedAt"
        private const val FIELD_SAVED_AT = "savedAt"
        private const val FIELD_GENDER = "gender"
        private const val FIELD_MODEL_3D_URL = "model3dUrl"
        private const val FIELD_HEIGHT = "height"
        private const val FIELD_WEIGHT = "weight"
        private const val FIELD_AGE = "age"
        private const val FIELD_BMI = "bmi"
        private const val FIELD_BMR = "bmr"
        private const val FIELD_FAT_PERCENTAGE = "fatPercentage"
        private const val FIELD_LEAN_BODY_MASS = "leanBodyMass"
        private const val FIELD_FAT_BODY_MASS = "fatBodyMass"
        private const val FIELD_ESTIMATED_BMI = "estimatedBmi"
        private const val FIELD_ESTIMATED_BMR = "estimatedBmr"
        private const val FIELD_ESTIMATED_WEIGHT = "estimatedWeight"
        private const val FIELD_ESTIMATED_LEAN_BODY_MASS = "estimatedLeanBodyMass"
        private const val FIELD_ESTIMATED_FAT_BODY_MASS = "estimatedFatBodyMass"

    }
}
