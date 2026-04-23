package com.hexis.bi.data.terra

/** Shortens Terra dev-ids, user UUIDs, and similar secrets for Timber (never log full values). */
internal fun redactSensitiveId(value: String): String =
    if (value.length <= 8) "***" else "${value.take(4)}…${value.takeLast(4)}"
