package com.hexis.bi.data.terra

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object TerraRangeJsonFetcher {

    suspend fun fetchJsonRows(
        requestedStart: LocalDate,
        requestedEnd: LocalDate,
        options: TerraRangeFetchOptions = TerraRangeFetchOptions(),
        fetch: suspend (LocalDate, LocalDate) -> Result<TerraDataListResponse>,
    ): Result<List<JsonElement>> {
        require(!requestedStart.isAfter(requestedEnd)) { "start after end" }

        val maxDays = options.maxDaysPerChunk.coerceAtLeast(1)
        val (apiStart, apiEnd) = apiDateRangeInclusive(requestedStart, requestedEnd, options)

        val out = ArrayList<JsonElement>()
        return try {
            var cursor = apiStart
            while (!cursor.isAfter(apiEnd)) {
                val chunkEnd =
                    minOf(cursor.plusDays((maxDays - 1).toLong()), apiEnd)
                mergeChunk(cursor, chunkEnd, options, out, fetch)
                cursor = chunkEnd.plusDays(1)
            }
            Result.success(out)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Terra range fetch failed [%s..%s]", requestedStart, requestedEnd)
            Result.failure(e)
        }
    }

    private fun apiDateRangeInclusive(
        requestedStart: LocalDate,
        requestedEnd: LocalDate,
        options: TerraRangeFetchOptions,
    ): Pair<LocalDate, LocalDate> {
        if (options.expandSingleDayToIsoWeek && requestedStart == requestedEnd) {
            val weekStart = requestedStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekEnd = requestedStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            return weekStart to weekEnd
        }
        return requestedStart to requestedEnd
    }

    private suspend fun mergeChunk(
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        options: TerraRangeFetchOptions,
        into: MutableList<JsonElement>,
        fetch: suspend (LocalDate, LocalDate) -> Result<TerraDataListResponse>,
    ) {
        val response = fetch(rangeStart, rangeEnd).getOrElse { throw it }
        if (response.data.isNotEmpty()) {
            into.addAll(response.data)
            return
        }
        if (
            !options.bisectOnAsyncEmptyData ||
            !isLargeRequestPending(response.message) ||
            !rangeStart.isBefore(rangeEnd)
        ) {
            return
        }
        val spanDays = ChronoUnit.DAYS.between(rangeStart, rangeEnd) + 1
        val leftLen = (spanDays / 2).coerceAtLeast(1)
        val midEnd = rangeStart.plusDays((leftLen - 1).toLong())
        mergeChunk(rangeStart, midEnd, options, into, fetch)
        mergeChunk(midEnd.plusDays(1), rangeEnd, options, into, fetch)
    }

    private fun isLargeRequestPending(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        val m = message.lowercase()
        return m.contains("large request") ||
            m.contains("chunks") ||
            m.contains("being processed")
    }
}

data class TerraRangeFetchOptions(
    val maxDaysPerChunk: Int = Companion.DEFAULT_MAX_DAYS_PER_CHUNK,
    val expandSingleDayToIsoWeek: Boolean = false,
    val bisectOnAsyncEmptyData: Boolean = true,
) {
    companion object {
        const val DEFAULT_MAX_DAYS_PER_CHUNK: Int = 31
    }
}
