package com.hexis.bi.data.scan

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hexis.bi.data.scan.api.MeasurementResponse
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        firestore.collection("users")
            .document(auth.currentUser?.uid ?: error("Not authenticated"))
            .collection("scans")

    suspend fun saveScan(
        response: MeasurementResponse,
        savedAtMillis: Long = System.currentTimeMillis(),
    ): Result<Unit> = runCatching {
        val docId = docIdFormat.format(Date(savedAtMillis))
        val scanRef = scansCollection().document(docId)

        val mainData = buildMap<String, Any?> {
            put("id", response.id)
            put("status", response.status)
            put("url", response.url)
            put("createdAt", response.createdAt)
            put("completedAt", response.completedAt)
            put("savedAt", Timestamp(Date(savedAtMillis)))
            put("gender", response.gender)
            put("model3dUrl", response.model3dUrl)
            put("height", response.height)
            put("weight", response.weight)
            put("age", response.age)
            put("bmi", response.bmi)
            put("bmr", response.bmr)
            put("fatPercentage", response.fatPercentage)
            put("leanBodyMass", response.leanBodyMass)
            put("fatBodyMass", response.fatBodyMass)
            put("estimatedBmi", response.estimatedBmi)
            put("estimatedBmr", response.estimatedBmr)
            put("estimatedWeight", response.estimatedWeight)
            put("estimatedLeanBodyMass", response.estimatedLeanBodyMass)
            put("estimatedFatBodyMass", response.estimatedFatBodyMass)
        }.filterValues { it != null }

        val batch = firestore.batch()
        batch.set(scanRef, mainData)

        response.circumferenceParams?.let {
            batch.set(scanRef.collection("circumferenceParams").document("data"), jsonObjectToNumericMap(it))
        }
        response.frontLinearParams?.let {
            batch.set(scanRef.collection("frontLinearParams").document("data"), jsonObjectToNumericMap(it))
        }
        response.sideLinearParams?.let {
            batch.set(scanRef.collection("sideLinearParams").document("data"), jsonObjectToNumericMap(it))
        }
        response.subscriptionInfo?.let {
            batch.set(scanRef.collection("subscriptionInfo").document("data"), jsonObjectToAnyMap(it))
        }

        batch.commit().await()
        Log.d(TAG, "saveScan: wrote $docId with ${mainData.size} fields")
    }

    suspend fun getLatestScan(): Result<ScanRecord?> =
        getRecentScans(limit = 1).map { it.firstOrNull() }

    /** Returns the second most recent scan (i.e. the one before the just-saved result). */
    suspend fun getPreviousScan(excludeId: String? = null): Result<ScanRecord?> =
        getRecentScans(limit = 2).map { scans ->
            if (excludeId != null) scans.firstOrNull { it.id != excludeId }
            else scans.getOrNull(1)
        }

    private suspend fun getRecentScans(limit: Long): Result<List<ScanRecord>> = runCatching {
        val snapshot = scansCollection()
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()

        snapshot.documents.map { doc ->
            val measurements = mutableMapOf<String, Float>()
            listOf("circumferenceParams", "frontLinearParams", "sideLinearParams").forEach { sub ->
                val subDoc = doc.reference.collection(sub).document("data").get().await()
                subDoc.data?.forEach { (key, value) ->
                    val f = (value as? Number)?.toFloat()
                        ?: (value as? String)?.toFloatOrNull()
                    if (f != null) measurements[key] = f
                }
            }

            ScanRecord(
                id = doc.id,
                timestamp = doc.getTimestamp("savedAt")?.toDate()?.time ?: 0L,
                model3dUrl = doc.getString("model3dUrl"),
                measurements = measurements,
            )
        }
    }

    private fun jsonObjectToNumericMap(obj: JsonObject): Map<String, Double> =
        obj.mapNotNull { (key, value) ->
            val prim = value as? JsonPrimitive ?: return@mapNotNull null
            val d = prim.content.toDoubleOrNull() ?: return@mapNotNull null
            snakeToCamel(key) to d
        }.toMap()

    private fun jsonObjectToAnyMap(obj: JsonObject): Map<String, Any> =
        obj.mapNotNull { (key, value) ->
            val prim = value as? JsonPrimitive ?: return@mapNotNull null
            val converted: Any = prim.content.toDoubleOrNull() ?: prim.content
            snakeToCamel(key) to converted
        }.toMap()

    private fun snakeToCamel(input: String): String {
        if (!input.contains('_')) return input
        return input.split('_').mapIndexed { i, part ->
            if (i == 0) part else part.replaceFirstChar { it.uppercase() }
        }.joinToString("")
    }

    companion object {
        private const val TAG = "ScanHistoryRepo"
        private val docIdFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    }
}
