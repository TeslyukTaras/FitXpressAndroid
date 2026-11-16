package com.hexis.bi.domain.subscription

enum class PlanFeature { FULL_ACCESS, SCANS, FULL_SCAN, MEASUREMENTS, INSIGHTS }

private val CoreFeatures = listOf(
    PlanFeature.SCANS,
    PlanFeature.FULL_SCAN,
    PlanFeature.MEASUREMENTS,
    PlanFeature.INSIGHTS,
)

val PlanId.features: List<PlanFeature>
    get() = when (this) {
        PlanId.ESSENTIAL, PlanId.PROGRESS, PlanId.TRANSFORMATION -> CoreFeatures
    }

val PlanId.detailedFeatures: List<PlanFeature>
    get() = listOf(PlanFeature.FULL_ACCESS) + features
