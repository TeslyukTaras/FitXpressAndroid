package com.hexis.bi.data.sleep

import com.hexis.bi.data.terra.TerraApi
import com.hexis.bi.data.terra.TerraRangeJsonFetcher
import com.hexis.bi.data.terra.TerraRestSourceResolver
import com.hexis.bi.data.terra.TerraSdkSync
import com.hexis.bi.data.terra.TtlCache
import com.hexis.bi.data.terra.fetchMergedFromAllSources
import com.hexis.bi.utils.constants.TerraCacheConstants
import com.hexis.bi.utils.redactSensitiveId
import kotlinx.serialization.json.JsonElement
import timber.log.Timber
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private object SleepRepositoryConstants {
    const val DAY_LOOKBACK_DAYS = 1L
    const val DAY_LOOKAHEAD_DAYS = 1L
}

class TerraApiSleepRepository(
    private val api: TerraApi,
    private val sourceResolver: TerraRestSourceResolver,
) : SleepRepository {

    private val rangeCache = TtlCache<Pair<LocalDate, LocalDate>, List<SleepSession>>(
        ttlMs = TerraCacheConstants.RANGE_CACHE_TTL_MS,
        generation = { TerraSdkSync.syncGeneration },
    )

    override suspend fun getSessionForNight(date: LocalDate): Result<SleepSession?> =
        getSessionsForRange(
            date.minusDays(SleepRepositoryConstants.DAY_LOOKBACK_DAYS),
            date.plusDays(SleepRepositoryConstants.DAY_LOOKAHEAD_DAYS),
        ).map { sessions -> selectSessionForNight(sessions, date) }

    override suspend fun getSessionsForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<SleepSession>> {
        val key = start to end
        rangeCache.get(key)?.let { return Result.success(it) }
        return sourceResolver.fetchMergedFromAllSources(
            start = start,
            end = end,
            fetchJson = ::fetchJsonForUser,
            parse = { rows -> rows.mapNotNull(TerraSleepJsonMapper::sessionOrNull) },
            merge = ::mergeGapFillByWakeDay,
        ).map { sessions ->
            sessions.filter { session ->
                val wakeDay = session.wakeTime.toLocalDate()
                !wakeDay.isBefore(start) && !wakeDay.isAfter(end)
            }
        }.onSuccess { rangeCache.put(key, it) }
    }

    private suspend fun fetchJsonForUser(
        terraUserId: String,
        start: LocalDate,
        end: LocalDate,
    ): Result<List<JsonElement>> {
        val apiEnd = end.plusDays(1)
        Timber.d("Terra /sleep request user_id=%s range=[%s..%s]", redactSensitiveId(terraUserId), start, end)
        return TerraRangeJsonFetcher.fetchJsonRows(start, apiEnd) { rs, re ->
            api.getSleep(terraUserId = terraUserId, startDate = rs, endDate = re)
        }.also { result ->
            result.exceptionOrNull()?.let { e ->
                Timber.e(e, "Terra /sleep failed user=%s [%s..%s]", redactSensitiveId(terraUserId), start, end)
            }
        }
    }

    private fun mergeGapFillByWakeDay(perSource: List<List<SleepSession>>): List<SleepSession> {
        val byWakeDay = LinkedHashMap<LocalDate, SleepSession>()
        for (sessions in perSource) {
            val aggregatedByWakeDay = sessions
                .groupBy { it.wakeTime.toLocalDate() }
                .mapValues { (_, daySessions) -> aggregateSleepSessionsForWakeDay(daySessions) }

            for ((day, session) in aggregatedByWakeDay.toSortedMap()) {
                if (day !in byWakeDay && session != null) byWakeDay[day] = session
            }
        }
        return byWakeDay.values.sortedBy { it.wakeTime }
    }
}

/**
 * Picks the session shown for [date]: nearest wake day, never a nap when a real
 * night is available, and on equidistant ties the night that started on [date].
 */
internal fun selectSessionForNight(sessions: List<SleepSession>, date: LocalDate): SleepSession? {
    val candidates = sessions.filterNot { it.isNap }.ifEmpty { sessions }
    return candidates.minWithOrNull(
        compareBy<SleepSession> { abs(ChronoUnit.DAYS.between(it.wakeTime.toLocalDate(), date)) }
            .thenBy { it.bedtime.toLocalDate() != date }
            .thenByDescending { it.wakeTime },
    )
}

internal fun aggregateSleepSessionsForWakeDay(sessions: List<SleepSession>): SleepSession? {
    val primary = sessions.primarySleepSessionForWakeDay() ?: return null
    return primary.copy(
        durationMinutes = sessions.sumOf { it.durationMinutes },
        efficiencyPercent = weightedAverageFloat(sessions) { it.efficiencyPercent }
            ?: primary.efficiencyPercent,
        restingHeartRateBpm = weightedAverageInt(sessions) { it.restingHeartRateBpm }
            ?: primary.restingHeartRateBpm,
        hrvMs = weightedAverageInt(sessions) { it.hrvMs }
            ?: primary.hrvMs,
        sdnnMs = weightedAverageInt(sessions) { it.sdnnMs }
            ?: primary.sdnnMs,
        isNap = sessions.all { it.isNap },
    )
}

private fun List<SleepSession>.primarySleepSessionForWakeDay(): SleepSession? =
    maxWithOrNull(
        compareBy<SleepSession> { !it.isNap }
            .thenBy { it.durationMinutes }
            .thenBy { it.wakeTime },
    )

/**
 * Duration-weighted average over the sessions that actually report the metric;
 * sessions with a missing (non-positive) value must not dilute the average.
 */
private inline fun weightedAverageFloat(
    sessions: List<SleepSession>,
    value: (SleepSession) -> Float,
): Float? {
    val measured = sessions.filter { value(it) > 0f && it.durationMinutes > 0 }
    val totalDurationMinutes = measured.sumOf { it.durationMinutes }
    if (totalDurationMinutes <= 0) return null
    val weightedTotal = measured.sumOf { value(it).toDouble() * it.durationMinutes }
    return (weightedTotal / totalDurationMinutes).toFloat()
}

private inline fun weightedAverageInt(
    sessions: List<SleepSession>,
    crossinline value: (SleepSession) -> Int,
): Int? = weightedAverageFloat(sessions) { value(it).toFloat() }?.toInt()
