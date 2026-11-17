package com.hexis.bi.domain.scan

import com.hexis.bi.utils.constants.ScanReminderRules
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

enum class ReminderKind { TODAY, NUDGE, MISSED }

sealed interface ReminderBasis {
    data class Plan(
        val periodEnd: LocalDate,
        val renewsAt: ZonedDateTime?,
        val scansUsed: Int,
        val scansAllowed: Int,
    ) : ReminderBasis

    data object FreeScan : ReminderBasis
}

data class ReminderOverride(val date: LocalDate, val hour: Int, val scanMillis: Long)

data class SentReminders(
    val todayKey: String? = null,
    val todayOn: LocalDate? = null,
    val nudgeKey: String? = null,
    val missedKey: String? = null,
) {
    companion object {
        fun parse(today: String?, nudge: String?, missed: String?): SentReminders {
            val parts = today?.split(ScanReminderRules.SENT_SEPARATOR)
            val on = parts?.getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            return SentReminders(
                todayKey = parts?.firstOrNull()?.takeIf { on != null },
                todayOn = on,
                nudgeKey = nudge,
                missedKey = missed,
            )
        }

        fun encodeToday(key: String, on: LocalDate): String = key + ScanReminderRules.SENT_SEPARATOR + on
    }
}

data class ReminderEvent(val kind: ReminderKind, val key: String, val at: ZonedDateTime)

data class ReminderInput(
    val now: ZonedDateTime,
    val basis: ReminderBasis?,
    val lastScanAt: ZonedDateTime?,
    val defaultHour: Int,
    val override: ReminderOverride?,
    val sent: SentReminders,
)

data class ReminderForecast(
    val events: List<ReminderEvent>,
    val next: ReminderEvent?,
    val nextHour: Int,
    val pickerRange: ClosedRange<LocalDate>?,
    val baselineScanMillis: Long,
) {
    companion object {
        fun empty(baselineScanMillis: Long = 0L) =
            ReminderForecast(emptyList(), null, ScanReminderRules.DEFAULT_HOUR, null, baselineScanMillis)
    }
}

object ScanReminderPlan {

    private data class Candidate(
        val key: String,
        val due: LocalDate,
        val cap: LocalDate?,
        val hour: Int,
        val periodEnd: LocalDate?,
        val notBefore: ZonedDateTime?,
    )

    fun forecast(input: ReminderInput): ReminderForecast {
        val baseline = input.lastScanAt?.toInstant()?.toEpochMilli() ?: 0L
        val candidates = when (val basis = input.basis) {
            null -> return ReminderForecast.empty(baseline)
            is ReminderBasis.Plan -> planCandidates(input, basis)
            ReminderBasis.FreeScan -> freeCandidates(input)
        }
        if (candidates.isEmpty()) return ReminderForecast.empty(baseline)

        val resolved = applyOverride(input, candidates)
        val events = resolved.flatMap { eventsFor(input, it) }.sortedBy { it.at }
        val firstPending = resolved.firstOrNull { c -> eventsFor(input, c).any { it.kind == ReminderKind.TODAY } }
        val today = input.now.toLocalDate()
        return ReminderForecast(
            events = events,
            next = events.firstOrNull { it.kind == ReminderKind.TODAY },
            nextHour = firstPending?.hour ?: input.defaultHour,
            pickerRange = firstPending?.let {
                today..maxOf(today, it.cap ?: it.due.plusDays(ScanReminderRules.PICKER_EXTRA_DAYS))
            },
            baselineScanMillis = baseline,
        )
    }

    private fun planCandidates(input: ReminderInput, plan: ReminderBasis.Plan): List<Candidate> {
        val today = input.now.toLocalDate()
        if (plan.periodEnd < today || plan.scansAllowed <= 0) return emptyList()
        val periodStart = plan.periodEnd.minusMonths(1)
        val periodDays = ChronoUnit.DAYS.between(periodStart, plan.periodEnd).coerceAtLeast(1)
        val interval = (periodDays.toDouble() / plan.scansAllowed).roundToLong().coerceAtLeast(1)
        val base = input.lastScanAt?.toLocalDate()?.plusDays(interval)
        val latest = plan.periodEnd.minusDays(ScanReminderRules.END_BUFFER_DAYS)
        val nextPeriodDue = maxOf(plan.periodEnd, base ?: plan.periodEnd)
        val nextPeriod = Candidate(
            key = planKey(nextPeriodDue),
            due = nextPeriodDue,
            cap = null,
            hour = input.defaultHour,
            periodEnd = plan.periodEnd.plusMonths(1),
            notBefore = plan.renewsAt?.plusHours(ScanReminderRules.RENEWAL_SETTLE_HOURS),
        )
        if (plan.scansAllowed - plan.scansUsed <= 0) return listOf(nextPeriod)
        val currentDue = minOf(maxOf(base ?: periodStart, periodStart), latest)
        val current = Candidate(
            key = planKey(currentDue),
            due = currentDue,
            cap = latest,
            hour = input.defaultHour,
            periodEnd = plan.periodEnd,
            notBefore = null,
        )
        return listOf(current, nextPeriod)
    }

    private fun freeCandidates(input: ReminderInput): List<Candidate> {
        val last = input.lastScanAt ?: return emptyList()
        val due = last.toLocalDate().plusDays(ScanReminderRules.FREE_SCAN_REMINDER_DAYS)
        return listOf(
            Candidate(
                key = freeKey(due),
                due = due,
                cap = null,
                hour = input.defaultHour,
                periodEnd = null,
                notBefore = null,
            ),
        )
    }

    private fun applyOverride(input: ReminderInput, candidates: List<Candidate>): List<Candidate> {
        val override = input.override ?: return candidates
        val baseline = input.lastScanAt?.toInstant()?.toEpochMilli() ?: 0L
        if (override.scanMillis != baseline || override.date < input.now.toLocalDate()) return candidates
        val index = candidates.indexOfFirst { c -> eventsFor(input, c).any { it.kind == ReminderKind.TODAY } }
        if (index < 0) return candidates
        val target = candidates[index]
        val key = if (target.key.startsWith(ScanReminderRules.KEY_FREE)) freeKey(override.date) else planKey(override.date)
        return candidates.toMutableList().also {
            it[index] = target.copy(key = key, due = override.date, cap = null, hour = override.hour, notBefore = null)
        }
    }

    private fun eventsFor(input: ReminderInput, c: Candidate): List<ReminderEvent> {
        val zone = input.now.zone
        val sentOn = if (input.sent.todayKey == c.key) input.sent.todayOn else null
        val firesOn = sentOn ?: maxOf(c.due, earliestDate(input.now, c.hour))
        if (sentOn == null && c.cap != null && firesOn > c.cap) return emptyList()

        val events = mutableListOf<ReminderEvent>()
        if (sentOn == null) {
            val at = firesOn.atTime(c.hour, 0).atZone(zone)
            events += ReminderEvent(ReminderKind.TODAY, c.key, c.notBefore?.let { maxOf(at, it) } ?: at)
        }
        if (c.periodEnd != null) {
            if (input.sent.nudgeKey != c.key) {
                events += ReminderEvent(
                    ReminderKind.NUDGE,
                    c.key,
                    firesOn.plusDays(ScanReminderRules.NEXT_DAY_OFFSET).atTime(c.hour, 0).atZone(zone),
                )
            }
            if (input.sent.missedKey != c.key) {
                val missedOn = maxOf(
                    minOf(
                        firesOn.plusDays(ScanReminderRules.MISSED_AFTER_DAYS),
                        c.periodEnd.minusDays(ScanReminderRules.NEXT_DAY_OFFSET),
                    ),
                    firesOn.plusDays(ScanReminderRules.MIN_MISSED_AFTER_DAYS),
                )
                events += ReminderEvent(ReminderKind.MISSED, c.key, missedOn.atTime(c.hour, 0).atZone(zone))
            }
        }
        val oldest = input.now.minusHours(ScanReminderRules.STALE_EVENT_HOURS)
        return events.filter { it.at.isAfter(oldest) }
    }

    private fun earliestDate(now: ZonedDateTime, hour: Int): LocalDate =
        if (now.toLocalTime() < LocalTime.of(hour, 0)) now.toLocalDate() else now.toLocalDate().plusDays(1)

    private fun planKey(due: LocalDate) = ScanReminderRules.KEY_PLAN + ":" + due

    private fun freeKey(due: LocalDate) = ScanReminderRules.KEY_FREE + ":" + due
}
