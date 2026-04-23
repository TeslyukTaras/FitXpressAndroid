package com.hexis.bi.data.sleep

import com.hexis.bi.data.terra.TerraApi
import com.hexis.bi.data.terra.TerraMultiSourceRangeRepository
import com.hexis.bi.data.terra.TerraRangeJsonFetcher
import com.hexis.bi.data.terra.TerraRestSourceResolver
import com.hexis.bi.data.terra.redactSensitiveId
import kotlinx.serialization.json.JsonElement
import timber.log.Timber
import java.time.LocalDate

class TerraApiSleepRepository(
    private val api: TerraApi,
    sourceResolver: TerraRestSourceResolver,
) : TerraMultiSourceRangeRepository<TerraSleepSession>(sourceResolver), TerraSleepRepository {

    override suspend fun getSessionForNight(date: LocalDate): Result<TerraSleepSession?> =
        getSessionsForRange(date.minusDays(1), date).map { sessions ->
            sessions.firstOrNull { it.wakeTime.toLocalDate() == date }
                ?: sessions.maxByOrNull { it.wakeTime }
        }

    override suspend fun getSessionsForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<TerraSleepSession>> =
        fetchMergedRange(start, end).also { result ->
            if (result.getOrNull()?.isEmpty() == true) {
                Timber.d("Terra /sleep merged no sessions for [%s..%s]", start, end)
            }
        }

    override suspend fun fetchJsonForUser(
        terraUserId: String,
        start: LocalDate,
        end: LocalDate,
    ): Result<List<JsonElement>> {
        Timber.d(
            "Terra /sleep request user_id=%s range=[%s..%s]",
            redactSensitiveId(terraUserId),
            start,
            end,
        )
        return TerraRangeJsonFetcher.fetchJsonRows(start, end) { rangeStart, rangeEnd ->
            api.getSleep(terraUserId = terraUserId, startDate = rangeStart, endDate = rangeEnd)
        }.also { result ->
            result.exceptionOrNull()?.let { e ->
                Timber.e(
                    e,
                    "Terra /sleep failed user=%s [%s..%s]",
                    redactSensitiveId(terraUserId),
                    start,
                    end,
                )
            }
        }
    }

    override fun parseElements(rows: List<JsonElement>): List<TerraSleepSession> =
        rows.mapNotNull(TerraSleepJsonMapper::sessionOrNull)

    override fun mergeGapFillPrioritized(perSource: List<List<TerraSleepSession>>): List<TerraSleepSession> {
        val byWakeDay = LinkedHashMap<LocalDate, TerraSleepSession>()
        for (sessions in perSource) {
            val ordered = sessions.sortedByDescending { it.wakeTime }
            for (session in ordered) {
                val day = session.wakeTime.toLocalDate()
                if (day !in byWakeDay) {
                    byWakeDay[day] = session
                }
            }
        }
        return byWakeDay.values.sortedBy { it.wakeTime }
    }
}
