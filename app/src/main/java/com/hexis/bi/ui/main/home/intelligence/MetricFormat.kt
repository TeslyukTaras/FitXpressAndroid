package com.hexis.bi.ui.main.home.intelligence

import com.hexis.bi.utils.constants.EngineUnits
import com.hexis.bi.utils.constants.FindingValues
import com.hexis.bi.utils.constants.MeasurementConstants
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

data class ValuePart(val text: String, val muted: Boolean)

internal class MetricFormat private constructor(
    private val style: Style,
    private val unit: String?,
    private val divisor: Double = 1.0,
) {

    private enum class Style { WHOLE, DECIMAL, PERCENT_OF_RATIO, DURATION }

    fun render(raw: Double): List<ValuePart> {
        val value = raw / divisor
        return when (style) {
            Style.WHOLE -> listOf(ValuePart(integerFormat().format(value.roundToInt()), muted = false))

            Style.DECIMAL -> listOf(
                ValuePart(String.format(Locale.getDefault(), FindingValues.ONE_DECIMAL, value), muted = false),
            )

            Style.PERCENT_OF_RATIO -> listOf(
                ValuePart(
                    integerFormat().format((value * FindingValues.RATIO_TO_PERCENT).roundToInt()),
                    muted = false,
                ),
            )

            Style.DURATION -> {
                val total = (value * FindingValues.MINUTES_PER_HOUR).roundToInt().coerceAtLeast(0)
                val hours = total / FindingValues.MINUTES_PER_HOUR
                val minutes = total % FindingValues.MINUTES_PER_HOUR
                buildList {
                    if (hours > 0) {
                        add(ValuePart("$hours", muted = false))
                        add(ValuePart(FindingValues.HOUR_SUFFIX, muted = true))
                        add(ValuePart(" $minutes", muted = false))
                    } else {
                        add(ValuePart("$minutes", muted = false))
                    }
                    add(ValuePart(FindingValues.MINUTE_SUFFIX, muted = true))
                }
            }
        }
    }

    fun unit(metricLabel: String): String = unit ?: metricLabel

    companion object {

        private fun integerFormat(): NumberFormat =
            NumberFormat.getIntegerInstance(Locale.getDefault())

        fun of(engineUnit: String, isMetric: Boolean): MetricFormat? = when (engineUnit) {
            EngineUnits.COUNT -> MetricFormat(Style.WHOLE, unit = null)
            EngineUnits.KCAL -> MetricFormat(Style.WHOLE, engineUnit)
            EngineUnits.MINUTES, EngineUnits.BPM, EngineUnits.MILLISECONDS ->
                MetricFormat(Style.DECIMAL, engineUnit)

            EngineUnits.PERCENT, EngineUnits.VO2MAX -> MetricFormat(Style.DECIMAL, engineUnit)
            EngineUnits.RATIO -> MetricFormat(Style.PERCENT_OF_RATIO, EngineUnits.PERCENT)
            EngineUnits.HOURS -> MetricFormat(Style.DURATION, unit = "")
            EngineUnits.SCORE_100, EngineUnits.SCORE_10 -> MetricFormat(Style.DECIMAL, unit = "")

            EngineUnits.METRES -> if (isMetric) {
                MetricFormat(Style.DECIMAL, EngineUnits.KILOMETRES, FindingValues.METRES_PER_KM)
            } else {
                MetricFormat(Style.DECIMAL, EngineUnits.MILES, FindingValues.METRES_PER_MILE)
            }

            EngineUnits.KILOGRAMS -> if (isMetric) {
                MetricFormat(Style.DECIMAL, EngineUnits.KILOGRAMS)
            } else {
                MetricFormat(Style.DECIMAL, EngineUnits.POUNDS, 1.0 / MeasurementConstants.KG_TO_LB)
            }

            EngineUnits.CENTIMETRES -> if (isMetric) {
                MetricFormat(Style.DECIMAL, EngineUnits.CENTIMETRES)
            } else {
                MetricFormat(Style.DECIMAL, EngineUnits.INCHES, MeasurementConstants.CM_TO_IN.toDouble())
            }

            else -> null
        }
    }
}
