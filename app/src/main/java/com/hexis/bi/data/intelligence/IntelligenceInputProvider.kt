package com.hexis.bi.data.intelligence

import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.health.local.HealthLocalDataSource
import com.hexis.bi.data.terra.decodeTerraRestIdentity
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.enums.GenderOption
import com.hexis.bi.domain.activity.withStrideDistance
import com.hexis.bi.intelligence.model.EngineInput
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class IntelligenceInputProvider(
    private val local: HealthLocalDataSource,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun load(
        analysisDays: Int,
        baselineDays: Int,
        historyDays: Int = analysisDays,
    ): Result<EngineInput> = withContext(io) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        runCatching {
            val runDate = LocalDate.now(clock)
            val days = observationWindow(runDate, historyDays, baselineDays)
            val identities = local.storedIdentityOrder(uid)
                .mapNotNull(::decodeTerraRestIdentity)
                .map { it.terraUserId }

            val profile = userRepository.getUser().getOrNull()
            val heightCm = profile?.heightCm?.toDouble()
            val gender = profile?.gender?.let { name ->
                GenderOption.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
            }

            val daily = CanonicalEngineInputAdapter.mergeByDay(
                local.allAggregatesByIdentity(
                    uid, identities, HealthLocalDataSource.SOURCE_DAILY, days, withSamples = false,
                ),
            ).map { it.withStrideDistance(heightCm, gender) }
            val sleep = CanonicalEngineInputAdapter.mergeByDay(
                local.allAggregatesByIdentity(
                    uid, identities, HealthLocalDataSource.SOURCE_SLEEP, days, withSamples = false,
                ),
            )
            val scans = local.scans(uid)

            CanonicalEngineInputAdapter.toEngineInput(
                daily = daily,
                sleep = sleep,
                scans = scans,
                window = days.first()..days.last(),
                runDate = runDate,
                analysisDays = analysisDays,
                heightCm = heightCm,
            )
        }
    }
}

internal fun observationWindow(runDate: LocalDate, analysisDays: Int, baselineDays: Int): List<LocalDate> {
    val span = (analysisDays + baselineDays).coerceAtLeast(1)
    val start = runDate.minusDays(span - 1L)
    return generateSequence(start) { it.plusDays(1).takeIf { next -> !next.isAfter(runDate) } }.toList()
}
