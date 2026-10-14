package com.hexis.bi.utils

import com.hexis.bi.config.AppConfig

object SuitContentPolicy {
    private val suitWords = Regex("\\bsuits?\\b", RegexOption.IGNORE_CASE)

    fun canDisplay(title: String, body: String, feature: String? = null): Boolean =
        AppConfig.suitVisible || (
                feature?.lowercase()?.let { it == "suit" || it.startsWith("suit_") } != true &&
                        !suitWords.containsMatchIn(title) && !suitWords.containsMatchIn(body)
                )
}
