package com.hexis.bi.data.terra

import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal typealias TerraNode = Map<*, *>

internal fun terraScalar(value: Any?): String? = when (value) {
    is String -> value
    is Number -> value.toString()
    is Boolean -> value.toString()
    else -> null
}

internal fun TerraNode.float(key: String): Float? = terraScalar(this[key])?.toFloatOrNull()

internal fun TerraNode.int(key: String): Int? = terraScalar(this[key])?.toIntOrNull()

internal fun TerraNode.boolean(key: String): Boolean? =
    terraScalar(this[key])?.toBooleanStrictOrNull()

internal fun TerraNode.objectOrNull(key: String): TerraNode? = this[key] as? TerraNode

internal fun terraArray(value: Any?): List<*>? = when (value) {
    is List<*> -> value
    is Array<*> -> value.asList()
    else -> null
}

internal fun TerraNode.arrayOrNull(key: String): List<*>? = terraArray(this[key])

internal fun terraNumberAsInt(value: Any?): Int? =
    terraScalar(value)?.let { it.toIntOrNull() ?: it.toFloatOrNull()?.toInt() }

internal fun terraNumberAsFloat(value: Any?): Float? =
    terraScalar(value)?.let { it.toFloatOrNull() ?: it.toIntOrNull()?.toFloat() }

internal fun TerraNode.parseTerraDateTimeField(key: String): LocalDateTime? {
    val raw = terraScalar(this[key])?.trim() ?: return null
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
