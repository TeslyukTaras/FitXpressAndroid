package com.hexis.bi.intelligence.config

import kotlinx.serialization.json.Json

object EngineConfigParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        allowSpecialFloatingPointValues = false
    }

    fun parse(text: String): Result<EngineConfig> =
        runCatching { json.decodeFromString<EngineConfig>(text) }
            .mapCatching { EngineConfigValidator.validate(it).getOrThrow() }
}
