package com.hexis.bi.data.sleep

import com.hexis.bi.data.terra.TerraApi
import com.hexis.bi.data.terra.TerraRangeJsonFetcher
import com.hexis.bi.data.terra.TerraRestSourceResolver
import com.hexis.bi.data.terra.fetchMergedFromAllSources
import com.hexis.bi.utils.redactSensitiveId
import kotlinx.serialization.json.JsonElement
import timber.log.Timber
import java.time.LocalDate

class TerraApiSleepRepository(
    private val api: TerraApi,
    private val sourceResolver: TerraRestSourceResolver,
) : SleepRepository {

    override suspend fun getSessionForNight(date: LocalDate): Result<SleepSession?> =
        getSessionsForRange(date.minusDays(1), date).map { sessions ->
            sessions.firstOrNull { it.wakeTime.toLocalDate() == date }
                ?: sessions.maxByOrNull { it.wakeTime }
        }

    override suspend fun getSessionsForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<SleepSession>> = sourceResolver.fetchMergedFromAllSources(
        start = start,
        end = end,
        fetchJson = ::fetchJsonForUser,
        parse = { rows -> rows.mapNotNull(TerraSleepJsonMapper::sessionOrNull) },
        merge = ::mergeGapFillByWakeDay,
    )

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
