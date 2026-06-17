package com.hexis.bi.utils

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.utils.constants.TerraProviders

private val brandLabelRes: Map<String, Int> = mapOf(
    TerraProviders.HEALTH_CONNECT to R.string.health_provider_google_health,
    "GOOGLEHEALTH" to R.string.health_provider_google_health,
    "GOOGLE" to R.string.provider_google,
    "APPLEHEALTH" to R.string.health_provider_apple_health,
    "APPLE" to R.string.health_provider_apple_health,
    "APPLE_HEALTH" to R.string.health_provider_apple_health,
)

@StringRes
internal fun String.providerDisplayNameResOrNull(): Int? = brandLabelRes[uppercase()]

@Composable
internal fun String.providerDisplayName(): String =
    providerDisplayNameResOrNull()?.let { stringResource(it) } ?: titleCasedProviderName()

private fun String.titleCasedProviderName(): String =
    split('_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part -> part.lowercase().replaceFirstChar { it.titlecase() } }
        .ifBlank { this }
