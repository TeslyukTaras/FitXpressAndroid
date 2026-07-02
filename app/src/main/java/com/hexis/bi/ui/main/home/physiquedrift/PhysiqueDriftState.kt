package com.hexis.bi.ui.main.home.physiquedrift

enum class PhysiqueDriftLevel {
    Athletic,
    Improving,
    NeedsWork,
    NoData,
}

data class PhysiqueMetric(
    val value: String,
    val delta: Float? = null,
)

data class PhysiqueDriftState(
    val hasData: Boolean = false,
    val score: String = "",
    val level: PhysiqueDriftLevel = PhysiqueDriftLevel.NoData,
    val description: String = "",
    val scanDate: String = "",
    val bodyFat: PhysiqueMetric = PhysiqueMetric(""),
    val leanBody: PhysiqueMetric = PhysiqueMetric(""),
    val waistProfile: String = "",
    val proportions: String = "",
    val symmetry: String = "",
    val shoulderToWaistRatio: String = "",
    val insight: String = "",
    val showInfoSheet: Boolean = false,
)
