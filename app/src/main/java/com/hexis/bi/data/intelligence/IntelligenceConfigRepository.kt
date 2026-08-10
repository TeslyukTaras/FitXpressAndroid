package com.hexis.bi.data.intelligence

import android.content.Context
import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.config.EngineConfigParser
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

internal interface IntelligenceConfigSource {
    val name: String

    suspend fun load(): Result<String>
}

internal class AssetIntelligenceConfigSource(
    private val context: Context,
    private val assetName: String = BUNDLED_CONFIG_ASSET,
) : IntelligenceConfigSource {

    override val name = "asset:$assetName"

    override suspend fun load(): Result<String> = runCatching {
        context.assets.open(assetName).bufferedReader().use { it.readText() }
    }
}

internal class IntelligenceConfigRepository(
    private val overrides: List<IntelligenceConfigSource>,
    private val baseline: IntelligenceConfigSource,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    private val lastKnownGood = AtomicReference<EngineConfig>()

    suspend fun config(): Result<EngineConfig> = withContext(io) {
        val failures = mutableListOf<String>()
        val bundled = parsed(baseline, failures)

        for (source in overrides) {
            val candidate = parsed(source, failures) ?: continue
            if (bundled != null && isOlderThan(candidate.configVersion, bundled.configVersion)) {
                failures += "${source.name}: rejected (version ${candidate.configVersion} " +
                    "predates bundled ${bundled.configVersion})"
                continue
            }
            return@withContext accept(candidate, source.name, failures)
        }

        bundled?.let { return@withContext accept(it, baseline.name, failures) }

        lastKnownGood.get()?.let { cached ->
            Timber.w("Intelligence config unusable from every source %s; serving last known good", failures)
            return@withContext Result.success(cached)
        }
        Timber.e("Intelligence config unusable and nothing cached: %s", failures)
        Result.failure(IllegalStateException("no usable intelligence config: ${failures.joinToString("; ")}"))
    }

    private suspend fun parsed(
        source: IntelligenceConfigSource,
        failures: MutableList<String>,
    ): EngineConfig? {
        val text = source.load().getOrElse { error ->
            failures += "${source.name}: unavailable (${error.message ?: error::class.simpleName})"
            return null
        }
        return EngineConfigParser.parse(text).getOrElse { error ->
            failures += "${source.name}: rejected (${error.message})"
            null
        }
    }

    private fun accept(
        config: EngineConfig,
        sourceName: String,
        failures: List<String>,
    ): Result<EngineConfig> {
        if (failures.isNotEmpty()) {
            Timber.w("Intelligence config fell back to %s after %s", sourceName, failures)
        }
        lastKnownGood.set(config)
        return Result.success(config)
    }
}

private fun isOlderThan(candidate: String, floor: String): Boolean {
    val candidateParts = numericVersion(candidate) ?: return true
    val floorParts = numericVersion(floor) ?: return false
    repeat(maxOf(candidateParts.size, floorParts.size)) { index ->
        val left = candidateParts.getOrElse(index) { 0 }
        val right = floorParts.getOrElse(index) { 0 }
        if (left != right) return left < right
    }
    return false
}

private fun numericVersion(value: String): List<Int>? {
    val parts = value.trim().removePrefix(VERSION_PREFIX).split('.').map { it.trim().toIntOrNull() }
    return if (parts.any { it == null }) null else parts.filterNotNull()
}

private const val VERSION_PREFIX = "v"

internal const val BUNDLED_CONFIG_ASSET = "intelligence_config_v1.json"
