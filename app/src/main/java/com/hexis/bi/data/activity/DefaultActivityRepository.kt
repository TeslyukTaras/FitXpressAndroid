package com.hexis.bi.data.activity

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
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

internal class DefaultActivityRepository(
    api: TerraApi,
    remote: HealthRemoteDataSource,
    local: HealthLocalDataSource,
    auth: FirebaseAuth,
) : ActivityRepository {

    private val sync = HealthDomainSync(local, remote, auth, ActivitySpec(api))

    override val updates: Flow<Unit> = sync.updates

    override suspend fun coverage(start: LocalDate, end: LocalDate): HealthRangeCoverage =
        sync.coverage(start, end)

    override suspend fun sync(start: LocalDate, end: LocalDate): Result<Unit> = sync.sync(start, end)

    override suspend fun getSummariesForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<ActivitySummary>> = sync.range(start, end)

    override suspend fun getSummaryForDate(date: LocalDate): Result<ActivitySummary?> =
        getSummariesForRange(date, date).map { it.firstOrNull() }
}

/** What makes activity different from the other health domains; the rest lives in [HealthDomainSync]. */
private class ActivitySpec(private val api: TerraApi) : HealthDomainSpec<ActivitySummary> {

    override val source = HealthLocalDataSource.SOURCE_DAILY
    override val label = "Activity"

    override fun dayOf(item: ActivitySummary): LocalDate = item.date

    override fun parse(rows: List<Any?>): List<ActivitySummary> =
        rows.mapNotNull(TerraActivityJsonMapper::summaryOrNull)

    override fun merge(perSource: List<List<ActivitySummary>>): List<ActivitySummary> {
        val byDate = LinkedHashMap<LocalDate, ActivitySummary>()
        for (rows in perSource) {
            for (summary in rows.sortedByDescending { it.date }) {
                if (summary.date !in byDate) byDate[summary.date] = summary
            }
        }
        return byDate.values.sortedBy { it.date }
    }

    override fun toAggregate(item: ActivitySummary): CanonicalDailyAggregate = item.toCanonicalAggregate()

    override fun toDomain(aggregate: CanonicalDailyAggregate): ActivitySummary = aggregate.toActivitySummary()

    // Terra's end date is exclusive, hence the extra day.
    override suspend fun fetchJson(
        terraUserId: String,
        start: LocalDate,
        end: LocalDate,
    ): Result<List<Any?>> = TerraRangeJsonFetcher.fetchJsonRows(start, end.plusDays(1)) { rs, re ->
        api.getDaily(terraUserId = terraUserId, startDate = rs, endDate = re, detail = TerraDetail.FULL)
    }
}
