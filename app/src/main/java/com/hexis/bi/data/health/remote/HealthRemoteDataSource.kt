package com.hexis.bi.data.health.remote

import com.hexis.bi.data.terra.MergedSourceResult
import com.hexis.bi.data.terra.TerraRestIdentity
import com.hexis.bi.data.terra.TerraRestSourceResolver
import com.hexis.bi.data.terra.fetchMergedFromAllSources
import com.hexis.bi.data.terra.sourceFingerprint
import com.hexis.bi.utils.constants.TerraSyncConstants
import kotlinx.coroutines.sync.Semaphore
import java.time.LocalDate
import kotlinx.serialization.json.JsonElement

internal class HealthRemoteDataSource(
    private val sourceResolver: TerraRestSourceResolver,
) {

    private val gate = Semaphore(TerraSyncConstants.MAX_CONCURRENT_SOURCES)

    suspend fun identities(): Result<List<TerraRestIdentity>> = sourceResolver.resolveOrderedIdentities()

    fun fingerprint(identities: List<TerraRestIdentity>): String = sourceFingerprint(identities)

    suspend fun <T> fetchRange(
        identities: List<TerraRestIdentity>,
        start: LocalDate,
        end: LocalDate,
        fetchJson: suspend (terraUserId: String, LocalDate, LocalDate) -> Result<List<JsonElement>>,
        parse: (List<JsonElement>) -> List<T>,
        merge: (List<List<T>>) -> List<T>,
    ): Result<MergedSourceResult<T>> =
        fetchMergedFromAllSources(identities, gate, start, end, fetchJson, parse, merge)
}
