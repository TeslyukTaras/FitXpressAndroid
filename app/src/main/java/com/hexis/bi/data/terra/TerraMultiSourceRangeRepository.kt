package com.hexis.bi.data.terra

import kotlinx.serialization.json.JsonElement
import java.time.LocalDate

/**
 * Shared Terra REST pipeline for multiple provider identities: resolve priority order, fetch JSON
 * per identity, parse to rows, then merge with gap-fill so higher-priority sources win per logical
 * key (e.g. wake day for sleep).
 */
abstract class TerraMultiSourceRangeRepository<T>(
    protected val sourceResolver: TerraRestSourceResolver,
) {

    protected abstract suspend fun fetchJsonForUser(
        terraUserId: String,
        start: LocalDate,
        end: LocalDate,
    ): Result<List<JsonElement>>

    protected abstract fun parseElements(rows: List<JsonElement>): List<T>

    /**
     * @param perSource results aligned with [TerraRestSourceResolver.resolveOrderedIdentities];
     * index 0 is highest priority; later lists only contribute rows not already represented.
     */
    protected abstract fun mergeGapFillPrioritized(perSource: List<List<T>>): List<T>

    protected suspend fun fetchMergedRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<T>> {
        val identities = sourceResolver.resolveOrderedIdentities().getOrElse {
            return Result.failure(it)
        }
        if (identities.isEmpty()) return Result.success(emptyList())

        val perSource = ArrayList<List<T>>(identities.size)
        for (id in identities) {
            val rows = fetchJsonForUser(id.terraUserId, start, end).getOrElse {
                return Result.failure(it)
            }
            perSource.add(parseElements(rows))
        }
        return Result.success(mergeGapFillPrioritized(perSource))
    }
}
