package com.hexis.bi.data.activity

import com.hexis.bi.data.terra.TerraApi
import com.hexis.bi.data.terra.TerraRangeJsonFetcher
import com.hexis.bi.data.terra.TerraRestSourceResolver
import com.hexis.bi.data.terra.TtlCache
import com.hexis.bi.data.terra.fetchMergedFromAllSources
import com.hexis.bi.utils.constants.TerraCacheConstants
import kotlinx.serialization.json.JsonElement
import java.time.LocalDate

class TerraApiActivityRepository(
    private val api: TerraApi,
    private val sourceResolver: TerraRestSourceResolver,
) : ActivityRepository {

    private val rangeCache = TtlCache<Pair<LocalDate, LocalDate>, List<ActivitySummary>>(
        ttlMs = TerraCacheConstants.RANGE_CACHE_TTL_MS,
    )

    override suspend fun getSummaryForDate(date: LocalDate): Result<ActivitySummary?> =
        sourceResolver.fetchMergedFromAllSources(
            start = date.minusDays(1),
            end = date.plusDays(1),
            fetchJson = ::fetchJsonForUser,
            parse = { rows -> rows.mapNotNull { TerraActivityJsonMapper.summaryOrNull(it, fallbackDate = date) } },
            merge = ::mergeGapFillByDate,
        ).map { rows ->
            rows.minByOrNull { kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(it.date, date)) }
        }

    override suspend fun getSummariesForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<ActivitySummary>> {
        val key = start to end
        rangeCache.get(key)?.let { return Result.success(it) }
        return sourceResolver.fetchMergedFromAllSources(
            start = start,
            end = end,
            fetchJson = ::fetchJsonForUser,
            parse = { rows -> rows.mapNotNull(TerraActivityJsonMapper::summaryOrNull) },
            merge = ::mergeGapFillByDate,
        ).onSuccess { rangeCache.put(key, it) }
    }

    private suspend fun fetchJsonForUser(
        terraUserId: String,
        start: LocalDate,
        end: LocalDate,
    ): Result<List<JsonElement>> = TerraRangeJsonFetcher.fetchJsonRows(start, end) { rs, re ->
        api.getDaily(terraUserId = terraUserId, startDate = rs, endDate = re)
    }

    private fun mergeGapFillByDate(perSource: List<List<ActivitySummary>>): List<ActivitySummary> {
        val byDate = LinkedHashMap<LocalDate, ActivitySummary>()
        for (rows in perSource) {
            for (summary in rows.sortedByDescending { it.date }) {
                if (summary.date !in byDate) byDate[summary.date] = summary
            }
        }
        return byDate.values.sortedBy { it.date }
    }
}
