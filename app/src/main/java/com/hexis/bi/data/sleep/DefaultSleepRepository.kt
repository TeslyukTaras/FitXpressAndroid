package com.hexis.bi.data.sleep

import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.health.local.HealthLocalDataSource
import com.hexis.bi.data.health.model.CanonicalDailyAggregate
import com.hexis.bi.data.health.remote.HealthRemoteDataSource
import com.hexis.bi.data.health.sync.HealthDomainSpec
import com.hexis.bi.data.health.sync.HealthDomainSync
import com.hexis.bi.data.health.sync.HealthRangeCoverage
import com.hexis.bi.data.terra.TerraApi
import com.hexis.bi.data.terra.TerraDetail
import com.hexis.bi.data.terra.TerraRangeJsonFetcher
import com.hexis.bi.utils.redactSensitiveId
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

private object SleepRepositoryConstants {
    /** A night can start the day before and end the day after, so both edges are pulled. */
    const val DAY_LOOKBACK_DAYS = 1L
    const val DAY_LOOKAHEAD_DAYS = 1L
}

internal class DefaultSleepRepository(
    api: TerraApi,
    remote: HealthRemoteDataSource,
    local: HealthLocalDataSource,
    auth: FirebaseAuth,
) : SleepRepository {

    private val sync = HealthDomainSync(local, remote, auth, SleepSpec(api))

    override val updates: Flow<Unit> = sync.updates

    override suspend fun coverage(start: LocalDate, end: LocalDate): HealthRangeCoverage =
        sync.coverage(start, end)

    override suspend fun sync(start: LocalDate, end: LocalDate): Result<Unit> = sync.sync(start, end)

    override suspend fun getSessionsForRange(
        start: LocalDate,
        end: LocalDate,
        detail: TerraDetail,
    ): Result<List<SleepSession>> = sync.range(start, end, withSamples = detail == TerraDetail.FULL)

    override suspend fun getSessionForNight(date: LocalDate): Result<SleepSession?> =
        getSessionsForRange(
            date.minusDays(SleepRepositoryConstants.DAY_LOOKBACK_DAYS),
            date.plusDays(SleepRepositoryConstants.DAY_LOOKAHEAD_DAYS),
            detail = TerraDetail.FULL,
        ).map { sessions -> selectSessionForNight(sessions, date) }
}

private class SleepSpec(private val api: TerraApi) : HealthDomainSpec<SleepSession> {

    override val source = HealthLocalDataSource.SOURCE_SLEEP
    override val label = "Sleep"

    override val fetchLookbackDays = SleepRepositoryConstants.DAY_LOOKBACK_DAYS

    /** Sleep is filed under the day you woke up, not the day you went to bed. */
    override fun dayOf(item: SleepSession): LocalDate = item.wakeTime.toLocalDate()

    override fun parse(rows: List<Any?>): List<SleepSession> =
        rows.mapNotNull(TerraSleepJsonMapper::sessionOrNull)

    override fun merge(perSource: List<List<SleepSession>>): List<SleepSession> {
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

    override fun toAggregate(item: SleepSession): CanonicalDailyAggregate = item.toCanonicalAggregate()

    override fun toDomain(aggregate: CanonicalDailyAggregate): SleepSession = aggregate.toSleepSession()

    override suspend fun fetchJson(
        terraUserId: String,
        start: LocalDate,
        end: LocalDate,
    ): Result<List<Any?>> {
        Timber.d("Terra /sleep request user_id=%s range=[%s..%s]", redactSensitiveId(terraUserId), start, end)
        return TerraRangeJsonFetcher.fetchJsonRows(start, end.plusDays(1)) { rs, re ->
            api.getSleep(terraUserId = terraUserId, startDate = rs, endDate = re, detail = TerraDetail.FULL)
        }.also { result ->
            result.exceptionOrNull()?.let { e ->
                Timber.e(e, "Terra /sleep failed user=%s [%s..%s]", redactSensitiveId(terraUserId), start, end)
            }
        }
    }
}

internal fun selectSessionForNight(sessions: List<SleepSession>, date: LocalDate): SleepSession? =
    sessions.firstOrNull { it.wakeTime.toLocalDate() == date }
internal fun aggregateSleepSessionsForWakeDay(sessions: List<SleepSession>): SleepSession? {
    sessions.singleOrNull()?.let { return it }
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
        sessionCount = sessions.size,
        aggregateStageTotals = SleepStageTotals(
            deepMinutes = sessions.sumOf { it.stageSecondsFor(SleepStage.Deep) }.wholeMinutes(),
            lightMinutes = sessions.sumOf { it.stageSecondsFor(SleepStage.Light) }.wholeMinutes(),
            remMinutes = sessions.sumOf { it.stageSecondsFor(SleepStage.REM) }.wholeMinutes(),
            awakeMinutes = sessions.sumOf { it.stageSecondsFor(SleepStage.Awake) }.wholeMinutes(),
        ),
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
