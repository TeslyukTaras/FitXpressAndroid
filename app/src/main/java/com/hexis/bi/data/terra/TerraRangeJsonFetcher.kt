package com.hexis.bi.data.terra

import com.hexis.bi.utils.constants.TerraSyncConstants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import timber.log.Timber
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A range Terra was still preparing when the fetch gave up. Raised rather than returned so the
 * caller fails the window instead of recording the day as confirmed-empty.
 */
class TerraRangePendingException(val day: LocalDate) :
    Exception("Terra was still preparing $day after ${TerraSyncConstants.PENDING_RANGE_RETRIES} attempts")

/**
 * Paginates a Terra REST range fetch into 31-day chunks, re-asks for any range Terra answers
 * asynchronously, and bisects one that stays pending.
 */
object TerraRangeJsonFetcher {

    private const val MAX_DAYS_PER_CHUNK = 31L

    suspend fun fetchJsonRows(
        start: LocalDate,
        end: LocalDate,
        fetch: suspend (LocalDate, LocalDate) -> Result<TerraDataListResponse>,
    ): Result<List<Any?>> {
        require(!start.isAfter(end)) { "start after end" }

        val effectiveEnd = minOf(end, LocalDate.now().plusDays(1))
        if (start.isAfter(effectiveEnd)) return Result.success(emptyList())

        val out = ArrayList<Any?>()
        return try {
            var cursor = start
            while (!cursor.isAfter(effectiveEnd)) {
                val chunkEnd = minOf(cursor.plusDays(MAX_DAYS_PER_CHUNK - 1), effectiveEnd)
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
        into: MutableList<Any?>,
        fetch: suspend (LocalDate, LocalDate) -> Result<TerraDataListResponse>,
    ) {
        val response = awaitReady(rangeStart, rangeEnd, fetch)
        if (response.data.isNotEmpty()) {
            into.addAll(response.data)
            return
        }
        if (!response.isPending) return
        if (!rangeStart.isBefore(rangeEnd)) {
            Timber.w("Terra still preparing %s; failing the window rather than recording it empty", rangeStart)
            throw TerraRangePendingException(rangeStart)
        }

        val spanDays = ChronoUnit.DAYS.between(rangeStart, rangeEnd) + 1
        val leftLen = (spanDays / 2).coerceAtLeast(1)
        val midEnd = rangeStart.plusDays(leftLen - 1)
        collectChunk(rangeStart, midEnd, into, fetch)
        collectChunk(midEnd.plusDays(1), rangeEnd, into, fetch)
    }

    private suspend fun awaitReady(
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        fetch: suspend (LocalDate, LocalDate) -> Result<TerraDataListResponse>,
    ): TerraDataListResponse {
        var response = fetch(rangeStart, rangeEnd).getOrElse { throw it }
        var attempt = 0
        while (response.data.isEmpty() && response.isPending &&
            attempt < TerraSyncConstants.PENDING_RANGE_RETRIES
        ) {
            delay(TerraSyncConstants.PENDING_RANGE_BACKOFF.toMillis() * (attempt + 1))
            attempt++
            Timber.d("Terra re-asking [%s..%s], attempt %d", rangeStart, rangeEnd, attempt)
            response = fetch(rangeStart, rangeEnd).getOrElse { throw it }
        }
        return response
    }
}

private val PENDING_STATUSES = setOf("not_ready", "processing", "pending")

private val PENDING_MESSAGE_MARKERS = listOf("large request", "chunks", "being processed", "not ready")

internal val TerraDataListResponse.isPending: Boolean
    get() {
        if (status?.lowercase()?.trim() in PENDING_STATUSES) return true
        val text = message?.lowercase() ?: return false
        return PENDING_MESSAGE_MARKERS.any { it in text }
    }
