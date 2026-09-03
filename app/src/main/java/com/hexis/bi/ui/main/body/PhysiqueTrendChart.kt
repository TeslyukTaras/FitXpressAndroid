package com.hexis.bi.ui.main.body

import com.hexis.bi.intelligence.prediction.PredictionConstants
import com.hexis.bi.intelligence.prediction.PredictionSeries
import com.hexis.bi.intelligence.prediction.ScanDay
import com.hexis.bi.intelligence.prediction.predictWeekly
import com.hexis.bi.utils.constants.BodyConstants
import com.hexis.bi.utils.constants.DateFormatConstants
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

internal object PhysiqueTrendChart {

    fun build(
        days: List<ScanDay>,
        range: BodyTimeRange,
        nowMillis: Long,
        gains: Map<PredictionSeries, Double> = emptyMap(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): BodyChartData {
        val today = LocalDate.ofInstant(Date(nowMillis).toInstant(), zone)
        val window = window(days, range, today)
        val rangeStart = window.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeEnd = window.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val confirmed = confirmedPoints(days, zone)
        val visible = (confirmed + listOfNotNull(predictedPoint(days, range, today, gains, zone)))
            .filter { it.timestamp in rangeStart..rangeEnd }
            .sortedBy { it.timestamp }
        val anchored = listOfNotNull(anchorPoint(confirmed, window.start, rangeStart, zone)) + visible
        val points = applyBaselineDeltas(anchored)
        val (yAxisBound, gridLines) = dynamicBounds(points)

        return BodyChartData(
            rangeStartMillis = rangeStart,
            rangeEndMillis = rangeEnd,
            points = points,
            axisLabels = axisLabels(window, range, days, today, zone),
            rangeLabel = formatRangeLabel(rangeStart, rangeEnd),
            yAxisBound = yAxisBound,
            gridLines = gridLines,
        )
    }

    data class Window(val start: LocalDate, val end: LocalDate)

    fun window(days: List<ScanDay>, range: BodyTimeRange, today: LocalDate): Window = when (range) {
        BodyTimeRange.SixMonths ->
            Window(today.minusMonths(BodyConstants.SIX_MONTH_SPAN), today)

        BodyTimeRange.OneYear ->
            Window(today.minusMonths(BodyConstants.ONE_YEAR_SPAN_MONTHS), today)

        BodyTimeRange.FourWeeks -> {
            val lastDay = days.lastOrNull()?.date?.let(LocalDate::parse) ?: today
            val dueDay = lastDay.plusDays(BodyConstants.SCAN_CADENCE_DAYS)
            val end = when {
                days.size < PredictionConstants.MIN_BUCKETS -> lastDay
                today.isAfter(dueDay) -> dueDay.plusDays(BodyConstants.SCAN_CADENCE_DAYS)
                else -> dueDay
            }
            Window(end.minusDays(BodyConstants.FOUR_WEEK_SPAN_DAYS), end)
        }
    }

    private fun anchorPoint(
        confirmed: List<BodyTrendPoint>,
        windowStart: LocalDate,
        rangeStart: Long,
        zone: ZoneId,
    ): BodyTrendPoint? {
        val previous = confirmed.lastOrNull { it.timestamp < rangeStart } ?: return null
        return previous.copy(timestamp = midday(windowStart, zone), isInterpolated = true)
    }

    private fun confirmedPoints(days: List<ScanDay>, zone: ZoneId): List<BodyTrendPoint> =
        days.mapNotNull { day ->
            val lean = day.values[PredictionSeries.LEAN_PCT] ?: return@mapNotNull null
            val fat = day.values[PredictionSeries.FAT_PCT] ?: return@mapNotNull null
            BodyTrendPoint(
                timestamp = midday(LocalDate.parse(day.date), zone),
                deltaFat = 0f,
                deltaMuscle = 0f,
                absoluteFat = fat.toFloat(),
                absoluteMuscle = lean.toFloat(),
                phase = BodyTrendPhase.ConfirmedScan,
            )
        }

    private fun predictedPoint(
        days: List<ScanDay>,
        range: BodyTimeRange,
        today: LocalDate,
        gains: Map<PredictionSeries, Double>,
        zone: ZoneId,
    ): BodyTrendPoint? {
        if (!range.predicts) return null
        val lastDay = days.lastOrNull()?.date?.let(LocalDate::parse) ?: return null
        if (today.isAfter(lastDay.plusDays(BodyConstants.SCAN_CADENCE_DAYS))) return null
        val prediction = predictWeekly(days, gains) ?: return null
        val lean = prediction.predicted[PredictionSeries.LEAN_PCT] ?: return null
        val fat = prediction.predicted[PredictionSeries.FAT_PCT] ?: return null
        return BodyTrendPoint(
            timestamp = midday(LocalDate.parse(prediction.targetDate), zone),
            deltaFat = 0f,
            deltaMuscle = 0f,
            absoluteFat = fat.toFloat(),
            absoluteMuscle = lean.toFloat(),
            isInterpolated = true,
            phase = BodyTrendPhase.WeeklyPrediction,
        )
    }

    fun axisLabels(
        window: Window,
        range: BodyTimeRange,
        days: List<ScanDay>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<BodyChartAxisLabel> = when (range) {
        BodyTimeRange.FourWeeks -> weeklyLabels(window, days, today, zone)
        BodyTimeRange.SixMonths -> monthlyLabels(window, zone, BodyConstants.SIX_MONTH_LABEL_STEP)
        BodyTimeRange.OneYear -> monthlyLabels(window, zone, BodyConstants.ONE_YEAR_LABEL_STEP)
    }

    private fun weeklyLabels(
        window: Window,
        days: List<ScanDay>,
        today: LocalDate,
        zone: ZoneId,
    ): List<BodyChartAxisLabel> {
        val formatter = SimpleDateFormat(DateFormatConstants.MONTH_SHORT_DAY, Locale.getDefault())
        val lastDay = days.lastOrNull()?.date?.let(LocalDate::parse)
        val dueDay = lastDay?.plusDays(BodyConstants.SCAN_CADENCE_DAYS)
        return generateSequence(window.end) { it.minusDays(BodyConstants.SCAN_CADENCE_DAYS) }
            .takeWhile { !it.isBefore(window.start) }
            .map { day ->
                val timestamp = midday(day, zone)
                BodyChartAxisLabel(
                    timestamp = timestamp,
                    text = formatter.format(Date(timestamp)),
                    emphasis = when {
                        dueDay != null && day == dueDay && today.isAfter(dueDay) ->
                            BodyAxisLabelEmphasis.Overdue

                        lastDay != null && day.isAfter(lastDay) -> BodyAxisLabelEmphasis.Upcoming
                        else -> BodyAxisLabelEmphasis.Confirmed
                    },
                )
            }
            .toList()
            .asReversed()
    }

    private fun monthlyLabels(
        window: Window,
        zone: ZoneId,
        step: Long,
    ): List<BodyChartAxisLabel> {
        val formatter = SimpleDateFormat(DateFormatConstants.MONTH_SHORT, Locale.getDefault())
        return generateSequence(window.end.withDayOfMonth(1)) { it.minusMonths(step) }
            .takeWhile { !it.isBefore(window.start) }
            .map { month ->
                val timestamp = midday(month, zone)
                BodyChartAxisLabel(timestamp = timestamp, text = formatter.format(Date(timestamp)))
            }
            .toList()
            .asReversed()
    }

    private fun midday(day: LocalDate, zone: ZoneId): Long =
        day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun applyBaselineDeltas(points: List<BodyTrendPoint>): List<BodyTrendPoint> {
        val baseline = points.firstOrNull() ?: return points
        return points.map {
            it.copy(
                deltaFat = it.absoluteFat - baseline.absoluteFat,
                deltaMuscle = it.absoluteMuscle - baseline.absoluteMuscle,
            )
        }
    }

    private fun dynamicBounds(points: List<BodyTrendPoint>): Pair<Float, List<Float>> {
        val maxAbs = points.maxOfOrNull { max(abs(it.deltaFat), abs(it.deltaMuscle)) } ?: 0f
        val halfRange = BodyConstants.niceYHalfRange(maxAbs)
        return halfRange to BodyConstants.gridLinesFor(halfRange)
    }

    private fun formatRangeLabel(startMillis: Long, endMillis: Long): String {
        val startFormatter =
            SimpleDateFormat(DateFormatConstants.DATE_RANGE_DAY_MONTH, Locale.getDefault())
        val endFormatter =
            SimpleDateFormat(DateFormatConstants.DATE_RANGE_DAY_MONTH_YEAR, Locale.getDefault())
        return "${startFormatter.format(Date(startMillis))} – ${endFormatter.format(Date(endMillis))}"
    }
}
