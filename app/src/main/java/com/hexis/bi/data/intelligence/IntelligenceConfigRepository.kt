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
    private val sources: List<IntelligenceConfigSource>,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    private val lastKnownGood = AtomicReference<EngineConfig>()

    suspend fun config(): Result<EngineConfig> = withContext(io) {
        val failures = mutableListOf<String>()
        for (source in sources) {
            val text = source.load().getOrElse { error ->
                failures += "${source.name}: unavailable (${error.message ?: error::class.simpleName})"
                continue
            }
            EngineConfigParser.parse(text).fold(
                onSuccess = { config ->
                    if (failures.isNotEmpty()) {
                        Timber.w("Intelligence config fell back to %s after %s", source.name, failures)
                    }
                    lastKnownGood.set(config)
                    return@withContext Result.success(config)
                },
                onFailure = { error ->
                    failures += "${source.name}: rejected (${error.message})"
                },
            )
        }

        lastKnownGood.get()?.let { cached ->
            Timber.w("Intelligence config unusable from every source %s; serving last known good", failures)
            return@withContext Result.success(cached)
        }
        Timber.e("Intelligence config unusable and nothing cached: %s", failures)
        Result.failure(IllegalStateException("no usable intelligence config: ${failures.joinToString("; ")}"))
    }
}

internal const val BUNDLED_CONFIG_ASSET = "intelligence_config_v1.json"
