package com.hexis.bi.domain.trend

import com.hexis.bi.utils.constants.ChangeTolerance
import com.hexis.bi.utils.constants.ChangeTolerances
import kotlin.math.abs

enum class ChangeDirection { Up, Down, None }

enum class ChangeBand { Settled, Monitoring, Directional }

internal fun changeBand(delta: Float, tolerance: Float): ChangeBand = when {
    abs(delta) >= tolerance -> ChangeBand.Directional
    abs(delta) >= tolerance * ChangeTolerances.MONITORING_BAND_FRACTION -> ChangeBand.Monitoring
    else -> ChangeBand.Settled
}

internal fun changeDirection(delta: Float, tolerance: Float): ChangeDirection = when {
    abs(delta) < tolerance -> ChangeDirection.None
    delta > 0f -> ChangeDirection.Up
    else -> ChangeDirection.Down
}

internal fun changeDirection(
    delta: Float,
    tolerance: ChangeTolerance,
    baseline: Float?,
): ChangeDirection = changeDirection(delta, tolerance.forBaseline(baseline))

internal fun changeBand(
    delta: Float,
    tolerance: ChangeTolerance,
    baseline: Float?,
): ChangeBand = changeBand(delta, tolerance.forBaseline(baseline))
