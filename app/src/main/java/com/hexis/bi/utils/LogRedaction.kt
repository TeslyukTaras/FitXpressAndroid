package com.hexis.bi.utils

/**
 * Shortens API keys, tokens, UUIDs, and similar secrets for Timber (never log full values).
 * Safe for any provider or backend id, not Terra-specific.
 */
fun redactSensitiveId(value: String): String =
    if (value.length <= 8) "***" else "${value.take(4)}…${value.takeLast(4)}"
