package com.hexis.bi.data.intelligence

import android.content.Context
import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.config.EngineConfigParser
import com.hexis.bi.intelligence.config.WordingConfigParser
import com.hexis.bi.intelligence.config.WordingDocument
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.hexis.bi.utils.constants.IntelligenceCacheConstants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

internal class VersionedConfigRepository<T : Any>(
    private val label: String,
    private val overrides: List<IntelligenceConfigSource>,
    private val baseline: IntelligenceConfigSource,
    private val parse: (String) -> Result<T>,
    private val versionOf: (T) -> String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {

    private data class Resolved<V : Any>(val value: V, val atMillis: Long)

    private val lastKnownGood = AtomicReference<T>()
    private val resolved = AtomicReference<Resolved<T>>()
    private val lastOutcome = AtomicReference<String>()
    private val resolveLock = Mutex()

    suspend fun config(): Result<T> = withContext(io) {
        fresh()?.let { return@withContext Result.success(it) }
        resolveLock.withLock {
            fresh()?.let { return@withLock Result.success(it) }
            resolve().onSuccess { resolved.set(Resolved(it, nowMillis())) }
        }
    }

    private fun fresh(): T? = resolved.get()
        ?.takeIf { nowMillis() - it.atMillis < IntelligenceCacheConstants.CONFIG_TTL.toMillis() }
        ?.value

    private suspend fun resolve(): Result<T> {
        val failures = mutableListOf<String>()
        val bundled = parsed(baseline, failures)

        for (source in overrides) {
            val candidate = parsed(source, failures) ?: continue
            if (bundled != null && isOlderThan(versionOf(candidate), versionOf(bundled))) {
                failures += "${source.name}: rejected (version ${versionOf(candidate)} " +
                    "predates bundled ${versionOf(bundled)})"
                continue
            }
            return accept(candidate, source.name, failures)
        }

        bundled?.let { return accept(it, baseline.name, failures) }

        lastKnownGood.get()?.let { cached ->
            Timber.w("%s unusable from every source %s; serving last known good", label, failures)
            return Result.success(cached)
        }
        Timber.e("%s unusable and nothing cached: %s", label, failures)
        return Result.failure(IllegalStateException("no usable $label: ${failures.joinToString("; ")}"))
    }

    private suspend fun parsed(
        source: IntelligenceConfigSource,
        failures: MutableList<String>,
    ): T? {
        val text = source.load().getOrElse { error ->
            failures += "${source.name}: unavailable (${error.message ?: error::class.simpleName})"
            return null
        }
        return parse(text).getOrElse { error ->
            failures += "${source.name}: rejected (${error.message})"
            null
        }
    }

    private fun accept(
        config: T,
        sourceName: String,
        failures: List<String>,
    ): Result<T> {
        val outcome = "$sourceName|${versionOf(config)}|$failures"
        if (lastOutcome.getAndSet(outcome) != outcome) {
            if (failures.isNotEmpty()) {
                Timber.w("%s fell back to %s after %s", label, sourceName, failures)
            } else {
                Timber.i("%s accepted %s from %s", label, versionOf(config), sourceName)
            }
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
internal const val BUNDLED_WORDING_ASSET = "intelligence_wording_v1.json"

internal class IntelligenceConfigRepository(
    overrides: List<IntelligenceConfigSource>,
    baseline: IntelligenceConfigSource,
) {
    private val delegate = VersionedConfigRepository(
        label = "Intelligence config",
        overrides = overrides,
        baseline = baseline,
        parse = EngineConfigParser::parse,
        versionOf = EngineConfig::configVersion,
    )

    suspend fun config(): Result<EngineConfig> = delegate.config()
}

internal class IntelligenceWordingRepository(
    overrides: List<IntelligenceConfigSource>,
    baseline: IntelligenceConfigSource,
) {
    private val delegate = VersionedConfigRepository(
        label = "Intelligence wording",
        overrides = overrides,
        baseline = baseline,
        parse = WordingConfigParser::parse,
        versionOf = WordingDocument::wordingVersion,
    )

    suspend fun config(): Result<WordingDocument> = delegate.config()
}
