package com.hexis.bi.utils

import com.hexis.bi.utils.constants.TerraProviders

internal fun String.toProviderDisplayName(): String =
    when {
        equals(TerraProviders.HEALTH_CONNECT, ignoreCase = true) -> "Google Health"
        equals("GoogleHealth", ignoreCase = true) -> "Google Health"
        equals("GOOGLE", ignoreCase = true) -> "Google"
        equals("AppleHealth", ignoreCase = true) -> "Apple Health"
        equals("APPLE", ignoreCase = true) ||
                equals("APPLE_HEALTH", ignoreCase = true) -> "Apple Health"
        else -> split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase().replaceFirstChar { it.titlecase() }
            }
            .ifBlank { this }
    }
