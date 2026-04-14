package com.hexis.bi.data.scan

import com.hexis.bi.R
import com.hexis.bi.data.scan.api.MeasurementResponse
import com.hexis.bi.ui.main.scan.results.MeasurementChange
import com.hexis.bi.ui.main.scan.results.MeasurementRow
import com.hexis.bi.ui.main.scan.results.MeasurementValue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.abs

/**
 * Maps a 3DLOOK API response (+ optional previous scan) into [MeasurementRow] list
 * for the Results screen table.
 */
object MeasurementMapper {

    /** Entries define which API fields map to which table rows, in display order. */
    private data class MappingEntry(
        val bodyPartRes: Int,
        val apiKey: String,
        /** true = decrease is good (e.g. waist), false = increase is good (e.g. chest) */
        val decreaseIsPositive: Boolean = false,
    )

    private val entries = listOf(
        MappingEntry(R.string.scan_measurement_neck, "neck"),
        MappingEntry(R.string.scan_measurement_shoulders, "shoulders"),
        MappingEntry(R.string.scan_measurement_chest, "chest"),
        MappingEntry(R.string.scan_measurement_forearms, "forearm"),
        MappingEntry(R.string.scan_measurement_biceps, "bicep"),
        MappingEntry(R.string.scan_measurement_upper_waist, "upperWaist", decreaseIsPositive = true),
        MappingEntry(R.string.scan_measurement_mid_waist, "waist", decreaseIsPositive = true),
        MappingEntry(R.string.scan_measurement_lower_waist, "lowerWaist", decreaseIsPositive = true),
        MappingEntry(R.string.scan_measurement_thigh, "thigh"),
        MappingEntry(R.string.scan_measurement_calf, "calf"),
    )

    fun map(
        current: MeasurementResponse,
        previous: ScanRecord?,
    ): List<MeasurementRow> {
        val currentMap = extractMeasurements(current)
        val previousMap = previous?.measurements

        return entries.mapNotNull { entry ->
            val todayCm = currentMap[entry.apiKey] ?: return@mapNotNull null
            val prevCm = previousMap?.get(entry.apiKey)

            val todayDelta = if (prevCm != null) todayCm - prevCm else 0f
            val todayChange = if (prevCm != null) classifyChange(todayDelta, entry.decreaseIsPositive) else null

            val todayValue = MeasurementValue(
                cm = todayCm,
                deltaCm = todayDelta,
                change = todayChange,
            )

            val previousValue = prevCm?.let {
                MeasurementValue(cm = it, deltaCm = 0f, change = null)
            }

            MeasurementRow(
                bodyPartRes = entry.bodyPartRes,
                today = todayValue,
                previous = previousValue,
            )
        }
    }

    private fun extractMeasurements(response: MeasurementResponse): Map<String, Float> {
        val result = mutableMapOf<String, Float>()
        response.circumferenceParams?.let { result += jsonObjectToFloatMap(it) }
        response.frontLinearParams?.let { result += jsonObjectToFloatMap(it) }
        response.sideLinearParams?.let { result += jsonObjectToFloatMap(it) }
        return result
    }

    private fun jsonObjectToFloatMap(obj: JsonObject): Map<String, Float> =
        obj.mapNotNull { (key, value) ->
            val prim = value as? JsonPrimitive ?: return@mapNotNull null
            val f = prim.content.toFloatOrNull() ?: return@mapNotNull null
            snakeToCamel(key) to f
        }.toMap()

    private fun snakeToCamel(input: String): String {
        if (!input.contains('_')) return input
        return input.split('_').mapIndexed { i, part ->
            if (i == 0) part else part.replaceFirstChar { it.uppercase() }
        }.joinToString("")
    }

    private fun classifyChange(delta: Float, decreaseIsPositive: Boolean): MeasurementChange? {
        if (abs(delta) < 0.01f) return null
        val isDesirable = if (decreaseIsPositive) delta < 0 else delta > 0
        return if (isDesirable) MeasurementChange.Positive else MeasurementChange.Negative
    }
}
