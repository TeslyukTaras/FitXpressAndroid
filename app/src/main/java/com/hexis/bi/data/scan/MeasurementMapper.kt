package com.hexis.bi.data.scan

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.data.scan.api.MeasurementResponse
import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.body.BodyMeasurementRegion
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
    val region: BodyMeasurementRegion,
)

object MeasurementMapper {

    fun map(
        current: MeasurementResponse,
        previous: ScanRecord?,
        beforePrevious: ScanRecord?,
    ): List<MeasurementRow> = buildRows(
        currentMap = extractMeasurements(current),
        previousMap = previous?.measurements,
        beforePreviousMap = beforePrevious?.measurements,
    )

    fun mapFromRecords(
        current: ScanRecord,
        previous: ScanRecord?,
        beforePrevious: ScanRecord?,
    ): List<MeasurementRow> = buildRows(
        currentMap = current.measurements,
        previousMap = previous?.measurements,
        beforePreviousMap = beforePrevious?.measurements,
    )

    /**
     * One [MeasurementRow] per [BodyMeasurementRegion.measurableRegions] entry that has a
     * value, in the universal display order. Regions absent from [currentMap] are dropped.
     */
    private fun buildRows(
        currentMap: Map<String, Float>,
        previousMap: Map<String, Float>?,
        beforePreviousMap: Map<String, Float>?,
    ): List<MeasurementRow> =
        BodyMeasurementRegion.measurableRegions.mapNotNull { region ->
            val todayCm = BodyMeasurementKeys.valueFor(currentMap, region) ?: return@mapNotNull null
            val prevCm = previousMap?.let { BodyMeasurementKeys.valueFor(it, region) }
            val beforePrevCm = beforePreviousMap?.let { BodyMeasurementKeys.valueFor(it, region) }

            val todayValue = MeasurementValue(
                cm = todayCm,
                deltaCm = if (prevCm != null) todayCm - prevCm else 0f,
                change = if (prevCm != null) {
                    classifyChange(todayCm - prevCm, region.decreaseIsPositive)
                } else null,
            )

            val previousValue = prevCm?.let {
                MeasurementValue(
                    cm = it,
                    deltaCm = if (beforePrevCm != null) it - beforePrevCm else 0f,
                    change = if (beforePrevCm != null) {
                        classifyChange(it - beforePrevCm, region.decreaseIsPositive)
                    } else null,
                )
            }

            MeasurementRow(
                bodyPartRes = bodyPartRes(region),
                visualAnchorKey = visualAnchorKey(region),
                today = todayValue,
                previous = previousValue,
            )
        }

    /**
     * Circumference with the largest absolute delta vs [previous];
     * null if there is no prior scan, no overlapping keys, or all deltas are negligible.
     *
     * All [BodyMeasurementRegion.measurableRegions] resolve to circumference keys, so
     * [ScanFetchProjection.LIST_SUMMARY] (circumference subdoc only) is sufficient input
     * here. If linear-param regions are ever added, this method requires [ScanFetchProjection.FULL].
     */
    fun topChangeVsPreviousScan(current: ScanRecord, previous: ScanRecord?): TopChangeVsPrevious? {
        if (previous == null) return null
        var bestRegion: BodyMeasurementRegion? = null
        var bestAbs = 0f
        var bestDelta = 0f
        for (region in BodyMeasurementRegion.measurableRegions) {
            val cur = BodyMeasurementKeys.valueFor(current.measurements, region) ?: continue
            val prev = BodyMeasurementKeys.valueFor(previous.measurements, region) ?: continue
            val delta = cur - prev
            if (abs(delta) < CHANGE_EPSILON_CM) continue
            val a = abs(delta)
            if (a > bestAbs) {
                bestAbs = a
                bestRegion = region
                bestDelta = delta
            }
        }
        val region = bestRegion ?: return null
        return TopChangeVsPrevious(
            bodyPartRes = bodyPartRes(region),
            deltaCm = bestDelta,
            change = classifyChange(bestDelta, region.decreaseIsPositive),
            region = region,
        )
    }

    /** Results-table row label for a region. Lives here because the copy is fixed per region. */
    @StringRes
    private fun bodyPartRes(region: BodyMeasurementRegion): Int = when (region) {
        BodyMeasurementRegion.Neck -> R.string.scan_measurement_neck
        BodyMeasurementRegion.Shoulders -> R.string.scan_measurement_shoulders
        BodyMeasurementRegion.Chest -> R.string.scan_measurement_chest
        BodyMeasurementRegion.Forearm -> R.string.scan_measurement_forearms
        BodyMeasurementRegion.Bicep -> R.string.scan_measurement_biceps
        BodyMeasurementRegion.UpperWaist -> R.string.scan_measurement_upper_waist
        BodyMeasurementRegion.Waist -> R.string.scan_measurement_mid_waist
        BodyMeasurementRegion.LowerWaist -> R.string.scan_measurement_lower_waist
        BodyMeasurementRegion.HipsGlutes -> R.string.scan_measurement_hips_glutes
        BodyMeasurementRegion.Thigh -> R.string.scan_measurement_thigh
        BodyMeasurementRegion.Calf -> R.string.scan_measurement_calf
        BodyMeasurementRegion.Ankle -> R.string.scan_measurement_ankles
        BodyMeasurementRegion.FullBody -> error("FullBody is not a measurement row")
    }

    /** Avatar-guide leader key for a region's row (can differ from the API value key). */
    private fun visualAnchorKey(region: BodyMeasurementRegion): String =
        BodyMeasurementKeys.visualAnchorKey(region)
            ?: BodyMeasurementKeys.primaryValueKey(region)
            ?: error("Measurement row requires a body-region key for $region")

    private fun extractMeasurements(response: MeasurementResponse): Map<String, Float> =
        mergeMeasurementParams(
            circumference = response.circumferenceParams?.let(::jsonObjectToFloatMap).orEmpty(),
            frontLinear = response.frontLinearParams?.let(::jsonObjectToFloatMap).orEmpty(),
            sideLinear = response.sideLinearParams?.let(::jsonObjectToFloatMap).orEmpty(),
        )

    /** Merges API measurement blocks with circumference, front, then side precedence. */
    fun mergeMeasurementParams(
        circumference: Map<String, Float>,
        frontLinear: Map<String, Float>,
        sideLinear: Map<String, Float>,
    ): Map<String, Float> {
        val merged = LinkedHashMap<String, Float>()
        merged.putAll(circumference)
        frontLinear.forEach { (key, value) -> merged.putIfAbsent(key, value) }
        sideLinear.forEach { (key, value) -> merged.putIfAbsent(key, value) }
        return merged
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
