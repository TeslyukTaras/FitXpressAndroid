package com.hexis.bi.intelligence.engine

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.withSign

internal fun roundHalfEven(value: Double, digits: Int): Double {
    if (!value.isFinite()) return value
    return BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN).toDouble().withSign(value)
}

internal fun normalizeNegativeZero(value: Double): Double = if (value == 0.0) 0.0 else value

internal fun formatFixed(value: Double, digits: Int): String =
    BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN).toPlainString()

internal fun formatSigned(value: Double, digits: Int): String {
    val magnitude = formatFixed(kotlin.math.abs(value), digits)
    val negative = value < 0.0 || (value == 0.0 && 1.0 / value < 0.0)
    return if (negative) "-$magnitude" else "+$magnitude"
}
