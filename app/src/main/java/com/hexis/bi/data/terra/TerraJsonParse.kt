package com.hexis.bi.data.terra

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal fun JsonPrimitive.contentOrNullSafe(): String? =
    if (isString) content else content.takeIf { it.isNotBlank() }

internal fun JsonObject.float(key: String): Float? =
    this[key]?.jsonPrimitive?.floatOrNull

internal fun JsonObject.int(key: String): Int? =
    this[key]?.jsonPrimitive?.intOrNull

internal fun JsonObject.parseTerraDateTimeField(key: String): LocalDateTime? {
    val raw = this[key]?.jsonPrimitive?.contentOrNullSafe()?.trim() ?: return null
    return parseTerraDateTime(raw)
}

internal fun parseTerraDateTime(raw: String): LocalDateTime? =
    runCatching { OffsetDateTime.parse(raw).toLocalDateTime() }
        .recoverCatching { LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        .recoverCatching { LocalDateTime.parse(raw) }
        .recoverCatching {
            val withZ = when {
                raw.endsWith('Z') -> raw
                Regex("[+-]\\d{2}:?\\d{2}$").containsMatchIn(raw) -> raw
                else -> raw + 'Z'
            }
            Instant.parse(withZ).atZone(ZoneId.systemDefault()).toLocalDateTime()
        }
        .onFailure { if (it is DateTimeParseException) Timber.w(it, "parseTerraDateTime: %s", raw) }
        .getOrNull()
