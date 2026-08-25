package com.hexis.bi.domain.activity

import com.hexis.bi.data.health.model.CanonicalDailyAggregate
import com.hexis.bi.data.health.model.METERS_PER_KILOMETER
import com.hexis.bi.domain.enums.GenderOption
import com.hexis.bi.utils.constants.StrideConstants

internal fun strideDistanceKm(steps: Double?, heightCm: Double?, gender: GenderOption?): Double? {
    if (steps == null || steps <= 0.0) return null
    if (heightCm == null || heightCm <= 0.0) return null
    val factor = when (gender) {
        GenderOption.Female -> StrideConstants.FEMALE_STRIDE_FACTOR
        else -> StrideConstants.MALE_STRIDE_FACTOR
    }
    return steps * (heightCm * factor) / StrideConstants.CM_PER_KILOMETER
}

internal fun CanonicalDailyAggregate.withStrideDistance(
    heightCm: Double?,
    gender: GenderOption?,
): CanonicalDailyAggregate {
    val km = strideDistanceKm(metrics.steps, heightCm, gender) ?: return this
    return copy(metrics = metrics.copy(distanceMeters = km * METERS_PER_KILOMETER))
}
