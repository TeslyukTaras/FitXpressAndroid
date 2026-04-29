package com.hexis.bi.data.sleep

import com.hexis.bi.data.terra.TerraApi
import com.hexis.bi.data.terra.TerraRangeJsonFetcher
import com.hexis.bi.data.terra.TerraRestSourceResolver
import com.hexis.bi.data.terra.TtlCache
import com.hexis.bi.data.terra.fetchMergedFromAllSources
import com.hexis.bi.utils.constants.TerraCacheConstants
import com.hexis.bi.utils.redactSensitiveId
import kotlinx.serialization.json.JsonElement
import timber.log.Timber
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    )

    override suspend fun getSessionForNight(date: LocalDate): Result<SleepSession?> =
        getSessionsForRange(
            date.minusDays(SleepRepositoryConstants.DAY_LOOKBACK_DAYS),
            date.plusDays(SleepRepositoryConstants.DAY_LOOKAHEAD_DAYS),
        ).map { sessions ->
            sessions.minByOrNull {
                kotlin.math.abs(ChronoUnit.DAYS.between(it.wakeTime.toLocalDate(), date))
            }
        }

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
        ).onSuccess { rangeCache.put(key, it) }
    }

    private suspend fun fetchJsonForUser(
        terraUserId: String,
        start: LocalDate,
        end: LocalDate,
    ): Result<List<JsonElement>> {
        Timber.d("Terra /sleep request user_id=%s range=[%s..%s]", redactSensitiveId(terraUserId), start, end)
        return TerraRangeJsonFetcher.fetchJsonRows(start, end) { rs, re ->
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
            for (session in sessions.sortedByDescending { it.wakeTime }) {
                val day = session.wakeTime.toLocalDate()
                if (day !in byWakeDay) byWakeDay[day] = session
            }
        }
        return byWakeDay.values.sortedBy { it.wakeTime }
    }
}
