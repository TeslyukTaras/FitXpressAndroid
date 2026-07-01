package com.hexis.bi.domain.body

import com.hexis.bi.domain.body.BodyMeasurementRegion.Companion.measurementAnchorKeys


/**
 * The single source of truth for every body part the app measures and visualises.
 *
 * Declaration order is the canonical display order used by every screen (scan results table,
 * Body Visual tab, home/history highlights). Each entry also carries the keys and grouping
 * flags the avatar overlay needs, so the avatar derives its key sets from here instead of
 * maintaining its own parallel string lists. Add a region here and it propagates everywhere.
 */
enum class BodyMeasurementRegion(
    /** true = a decrease is the desirable direction for this region (e.g. waistlines). */
    val decreaseIsPositive: Boolean = false,
    /**
     * Avatar/visual anchor key (also the OBJ guide anchor name); null for [FullBody].
     * Persisted preferences key on the enum [name], not this.
     */
    val anchorKey: String? = null,
    /** Avatar: a horizontal torso band (slice scoring differs from limbs). */
    val isTorso: Boolean = false,
    /** Avatar: a limb circumference drawn as two mirrored rings (left + right). */
    val isBilateral: Boolean = false,
    /** Avatar: a torso band that uses quantile-based gentle-fallback narrowing. */
    val usesGentleTorsoFallback: Boolean = false,
) {
    FullBody,
    Neck(anchorKey = BodyMeasurementKeys.Neck, isTorso = true),
    Shoulders(anchorKey = BodyMeasurementKeys.Shoulders, isTorso = true),
    Chest(anchorKey = BodyMeasurementKeys.Chest, isTorso = true, usesGentleTorsoFallback = true),
    Forearm(anchorKey = BodyMeasurementKeys.Forearm, isBilateral = true),
    Bicep(anchorKey = BodyMeasurementKeys.Bicep, isBilateral = true),
    UpperWaist(
        decreaseIsPositive = true,
        anchorKey = BodyMeasurementKeys.UpperWaist,
        isTorso = true,
        usesGentleTorsoFallback = true,
    ),
    Waist(
        decreaseIsPositive = true,
        anchorKey = BodyMeasurementKeys.Waist,
        isTorso = true,
        usesGentleTorsoFallback = true,
    ),
    LowerWaist(
        decreaseIsPositive = true,
        anchorKey = BodyMeasurementKeys.LowerWaist,
        isTorso = true,
        usesGentleTorsoFallback = true,
    ),
    HipsGlutes(anchorKey = BodyMeasurementKeys.LowHips),
    Thigh(anchorKey = BodyMeasurementKeys.Thigh, isBilateral = true),
    Calf(anchorKey = BodyMeasurementKeys.Calf, isBilateral = true),
    Ankle(anchorKey = BodyMeasurementKeys.Ankle);

    /** Avatar draws a mesh cross-section (ring) here rather than a single point. */
    val drawsCrossSection: Boolean get() = isTorso || isBilateral

    companion object {
        /** User-facing measurement parameters in display order; excludes [FullBody]. */
        val measurableRegions: List<BodyMeasurementRegion> = entries.filter { it != FullBody }

        /** Region matching a persisted [name] key, or null if unknown (e.g. renamed/removed). */
        fun fromStorageKey(key: String): BodyMeasurementRegion? =
            entries.firstOrNull { it.name == key }

        /** Region owning the given avatar/visual [anchorKey], or null if none. */
        fun fromAnchorKey(anchorKey: String): BodyMeasurementRegion? =
            entries.firstOrNull { it.anchorKey == anchorKey }

        /**
         * Regions the user has chosen to show in measurement tables (Scan Preference).
         * A null [storedKeys] means "not configured yet" and defaults to every measurable region;
         * an empty list means the user explicitly hid them all.
         */
        fun visibleRegionsOrDefault(storedKeys: List<String>?): Set<BodyMeasurementRegion> =
            storedKeys?.mapNotNull(::fromStorageKey)?.toSet() ?: measurableRegions.toSet()

        // ── Avatar key sets (canonical anchor-key strings), derived from the entries above ──

        /** Anchor keys, in display order, where the avatar draws a cross-section ring. */
        val measurementAnchorKeys: List<String> =
            entries.filter { it.drawsCrossSection }.mapNotNull { it.anchorKey }

        /** [measurementAnchorKeys] as a membership set. */
        val circumferenceVisualKeys: Set<String> = measurementAnchorKeys.toSet()

        /** Limb circumference anchors drawn as two mirrored rings. */
        val bilateralCircumferenceKeys: Set<String> =
            entries.filter { it.isBilateral }.mapNotNull { it.anchorKey }.toSet()

        /** Horizontal torso band anchors. */
        val torsoCircumferenceKeys: Set<String> =
            entries.filter { it.isTorso }.mapNotNull { it.anchorKey }.toSet()

        /** Torso band anchors that use quantile gentle-fallback narrowing. */
        val torsoGentleFallbackKeys: Set<String> =
            entries.filter { it.usesGentleTorsoFallback }.mapNotNull { it.anchorKey }.toSet()
    }
}
