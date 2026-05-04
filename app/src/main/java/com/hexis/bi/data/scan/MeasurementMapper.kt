package com.hexis.bi.data.scan

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.data.scan.api.MeasurementResponse
import com.hexis.bi.ui.main.scan.results.MeasurementChange
import com.hexis.bi.ui.main.scan.results.MeasurementRow
import com.hexis.bi.ui.main.scan.results.MeasurementValue
import com.hexis.bi.utils.constants.MeasurementConstants.CHANGE_EPSILON_CM
import com.hexis.bi.utils.snakeToCamel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.abs

/**
 * Maps a 3DLOOK API response (+ optional previous scan) into [MeasurementRow] list
 * for the Results screen table.
 */
data class TopChangeVsPrevious(
    @StringRes val bodyPartRes: Int,
    val deltaCm: Float,
    val change: MeasurementChange?,
)

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
        beforePrevious: ScanRecord?,
    ): List<MeasurementRow> {
        val currentMap = extractMeasurements(current)
        val previousMap = previous?.measurements
        val beforePreviousMap = beforePrevious?.measurements

        return entries.mapNotNull { entry ->
            val todayCm = currentMap[entry.apiKey] ?: return@mapNotNull null
            val prevCm = previousMap?.get(entry.apiKey)
            val beforePrevCm = beforePreviousMap?.get(entry.apiKey)

            val todayValue = MeasurementValue(
                cm = todayCm,
                deltaCm = if (prevCm != null) todayCm - prevCm else 0f,
                change = if (prevCm != null) {
                    classifyChange(todayCm - prevCm, entry.decreaseIsPositive)
                } else null,
            )

            val previousValue = prevCm?.let {
                MeasurementValue(
                    cm = it,
                    deltaCm = if (beforePrevCm != null) it - beforePrevCm else 0f,
                    change = if (beforePrevCm != null) {
                        classifyChange(it - beforePrevCm, entry.decreaseIsPositive)
                    } else null,
                )
            }

            MeasurementRow(
                bodyPartRes = entry.bodyPartRes,
                visualAnchorKey = entry.apiKey,
                today = todayValue,
                previous = previousValue,
            )
        }
    }

    /**
     * Circumference with the largest absolute delta vs [previous];
     * null if there is no prior scan, no overlapping keys, or all deltas are negligible.
     *
     * All [entries] resolve to circumference keys, so [ScanFetchProjection.LIST_SUMMARY]
     * (circumference subdoc only) is sufficient input here. If linear-param entries are
     * ever added, this method requires [ScanFetchProjection.FULL].
     */
    fun topChangeVsPreviousScan(current: ScanRecord, previous: ScanRecord?): TopChangeVsPrevious? {
        if (previous == null) return null
        var bestEntry: MappingEntry? = null
        var bestAbs = 0f
        var bestDelta = 0f
        for (entry in entries) {
            val cur = current.measurements[entry.apiKey] ?: continue
            val prev = previous.measurements[entry.apiKey] ?: continue
            val delta = cur - prev
            if (abs(delta) < CHANGE_EPSILON_CM) continue
            val a = abs(delta)
            if (a > bestAbs) {
                bestAbs = a
                bestEntry = entry
                bestDelta = delta
            }
        }
        val e = bestEntry ?: return null
        return TopChangeVsPrevious(
            bodyPartRes = e.bodyPartRes,
            deltaCm = bestDelta,
            change = classifyChange(bestDelta, e.decreaseIsPositive),
        )
    }

    fun mapFromRecords(
        current: ScanRecord,
        previous: ScanRecord?,
        beforePrevious: ScanRecord?,
    ): List<MeasurementRow> {
        val currentMap = current.measurements
        val previousMap = previous?.measurements
        val beforePreviousMap = beforePrevious?.measurements

        return entries.mapNotNull { entry ->
            val todayCm = currentMap[entry.apiKey] ?: return@mapNotNull null
            val prevCm = previousMap?.get(entry.apiKey)
            val beforePrevCm = beforePreviousMap?.get(entry.apiKey)

            val todayValue = MeasurementValue(
                cm = todayCm,
                deltaCm = if (prevCm != null) todayCm - prevCm else 0f,
                change = if (prevCm != null) classifyChange(todayCm - prevCm, entry.decreaseIsPositive) else null,
            )

            val previousValue = prevCm?.let {
                MeasurementValue(
                    cm = it,
                    deltaCm = if (beforePrevCm != null) it - beforePrevCm else 0f,
                    change = if (beforePrevCm != null) classifyChange(it - beforePrevCm, entry.decreaseIsPositive) else null,
                )
            }

            MeasurementRow(
                bodyPartRes = entry.bodyPartRes,
                visualAnchorKey = entry.apiKey,
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
            key.snakeToCamel() to f
        }.toMap()

    private fun classifyChange(delta: Float, decreaseIsPositive: Boolean): MeasurementChange? {
        if (abs(delta) < CHANGE_EPSILON_CM) return null
        val isDesirable = if (decreaseIsPositive) delta < 0 else delta > 0
        return if (isDesirable) MeasurementChange.Positive else MeasurementChange.Negative
    }

}
