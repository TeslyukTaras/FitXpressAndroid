package com.hexis.bi.domain.body

/**
 * Single mapping point between our UI body regions, 3DLOOK response fields, and
 * avatar guide anchors. API fields are stored camelCased after parsing JSON.
 */
object BodyMeasurementKeys {
    const val Neck = "neck"
    const val Shoulders = "shoulders"
    const val Chest = "chest"
    const val Forearm = "forearm"
    const val Wrist = "wrist"
    const val Bicep = "bicep"
    const val UpperArm = "upperArm"
    const val UpperWaist = "upperWaist"
    const val AlternativeWaistGirth = "alternativeWaistGirth"
    const val Waist = "waist"
    const val LowerWaist = "lowerWaist"
    const val Abdomen = "abdomen"
    const val HipsGlutes = "hipsGlutes"
    const val LowHips = "lowHips"
    const val HighHips = "highHips"
    const val Thigh = "thigh"
    const val ThighWidest = "thighWidest"
    const val Calf = "calf"
    const val Ankle = "ankle"

    fun valueKeys(region: BodyMeasurementRegion): List<String> = when (region) {
        BodyMeasurementRegion.FullBody -> emptyList()
        BodyMeasurementRegion.Neck -> listOf(Neck)
        BodyMeasurementRegion.Shoulders -> listOf(Shoulders)
        BodyMeasurementRegion.Chest -> listOf(Chest)
        BodyMeasurementRegion.Forearm -> listOf(Forearm, Wrist)
        BodyMeasurementRegion.Bicep -> listOf(UpperArm, Bicep)
        BodyMeasurementRegion.UpperWaist -> listOf(AlternativeWaistGirth, UpperWaist)
        BodyMeasurementRegion.Waist -> listOf(Waist)
        BodyMeasurementRegion.LowerWaist -> listOf(Abdomen, LowerWaist)
        BodyMeasurementRegion.HipsGlutes -> listOf(LowHips, HipsGlutes, HighHips)
        BodyMeasurementRegion.Thigh -> listOf(Thigh, ThighWidest)
        BodyMeasurementRegion.Calf -> listOf(Calf)
        BodyMeasurementRegion.Ankle -> listOf(Ankle)
    }

    fun primaryValueKey(region: BodyMeasurementRegion): String? = valueKeys(region).firstOrNull()

    fun valueFor(measurements: Map<String, Float>, region: BodyMeasurementRegion): Float? =
        valueKeys(region).firstNotNullOfOrNull { measurements[it] }

    fun visualAnchorKey(region: BodyMeasurementRegion): String? = when (region) {
        BodyMeasurementRegion.FullBody -> null
        BodyMeasurementRegion.Neck -> Neck
        BodyMeasurementRegion.Shoulders -> Shoulders
        BodyMeasurementRegion.Chest -> Chest
        BodyMeasurementRegion.Forearm -> Forearm
        BodyMeasurementRegion.Bicep -> Bicep
        BodyMeasurementRegion.UpperWaist -> UpperWaist
        BodyMeasurementRegion.Waist -> Waist
        BodyMeasurementRegion.LowerWaist -> LowerWaist
        BodyMeasurementRegion.HipsGlutes -> LowHips
        BodyMeasurementRegion.Thigh -> Thigh
        BodyMeasurementRegion.Calf -> Calf
        BodyMeasurementRegion.Ankle -> Ankle
    }
}
