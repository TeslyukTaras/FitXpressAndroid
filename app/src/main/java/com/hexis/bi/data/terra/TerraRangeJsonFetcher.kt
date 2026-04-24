package com.hexis.bi.data.terra

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement
import timber.log.Timber
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Paginates a Terra REST range fetch into 31-day chunks and bisects any chunk Terra
 * answers with an async "large request pending" response (empty data + "chunks" message).
 */
object TerraRangeJsonFetcher {

    private const val MAX_DAYS_PER_CHUNK = 31L

    suspend fun fetchJsonRows(
        start: LocalDate,
        end: LocalDate,
        fetch: suspend (LocalDate, LocalDate) -> Result<TerraDataListResponse>,
    ): Result<List<JsonElement>> {
        require(!start.isAfter(end)) { "start after end" }

        val out = ArrayList<JsonElement>()
        return try {
            var cursor = start
            while (!cursor.isAfter(end)) {
                val chunkEnd = minOf(cursor.plusDays(MAX_DAYS_PER_CHUNK - 1), end)
                collectChunk(cursor, chunkEnd, out, fetch)
                cursor = chunkEnd.plusDays(1)
            }
            Result.success(out)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Terra range fetch failed [%s..%s]", start, end)
            Result.failure(e)
        }
    }

    private suspend fun collectChunk(
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        into: MutableList<JsonElement>,
        fetch: suspend (LocalDate, LocalDate) -> Result<TerraDataListResponse>,
    ) {
        val response = fetch(rangeStart, rangeEnd).getOrElse { throw it }
        if (response.data.isNotEmpty()) {
            into.addAll(response.data)
            return
        }
        if (!isLargeRequestPending(response.message) || !rangeStart.isBefore(rangeEnd)) return

        val spanDays = ChronoUnit.DAYS.between(rangeStart, rangeEnd) + 1
        val leftLen = (spanDays / 2).coerceAtLeast(1)
        val midEnd = rangeStart.plusDays(leftLen - 1)
        collectChunk(rangeStart, midEnd, into, fetch)
        collectChunk(midEnd.plusDays(1), rangeEnd, into, fetch)
    }

    private fun isLargeRequestPending(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        val m = message.lowercase()
        return m.contains("large request") || m.contains("chunks") || m.contains("being processed")
    }
}
