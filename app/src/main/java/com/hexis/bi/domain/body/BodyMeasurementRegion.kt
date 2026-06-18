package com.hexis.bi.domain.body

/**
 * Universal set of body regions the app measures and visualises. The declaration order
 * is the canonical display order used by every screen (scan results table, Body Visual
 * tab, home/history highlights). Add a region here and it propagates everywhere.
 */
enum class BodyMeasurementRegion(
    /** true = a decrease is the desirable direction for this region (e.g. waistlines). */
    val decreaseIsPositive: Boolean = false,
) {
    FullBody,
    Neck,
    Shoulders,
    Chest,
    Forearm,
    Bicep,
    UpperWaist(decreaseIsPositive = true),
    Waist(decreaseIsPositive = true),
    LowerWaist(decreaseIsPositive = true),
    HipsGlutes,
    Thigh,
    Calf,
    Ankle;

    companion object {
        /** User-facing measurement parameters in display order; excludes [FullBody]. */
        val measurableRegions: List<BodyMeasurementRegion> = entries.filter { it != FullBody }

        /** Region matching a persisted [name] key, or null if unknown (e.g. renamed/removed). */
        fun fromStorageKey(key: String): BodyMeasurementRegion? =
            entries.firstOrNull { it.name == key }

        /**
         * Regions the user has chosen to show in measurement tables (Scan Preference).
         * A null [storedKeys] means "not configured yet" and defaults to every measurable region;
         * an empty list means the user explicitly hid them all.
         */
        fun visibleRegionsOrDefault(storedKeys: List<String>?): Set<BodyMeasurementRegion> =
            storedKeys?.mapNotNull(::fromStorageKey)?.toSet() ?: measurableRegions.toSet()
    }
}
