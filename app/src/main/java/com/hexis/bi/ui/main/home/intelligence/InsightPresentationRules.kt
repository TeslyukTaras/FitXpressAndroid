package com.hexis.bi.ui.main.home.intelligence

internal fun showHomeInsightContent(
    loaded: Boolean,
    updating: Boolean,
    hasCards: Boolean,
): Boolean = loaded && (!updating || hasCards)

internal fun showFullInsightsEmpty(
    loaded: Boolean,
    updating: Boolean,
    hasCards: Boolean,
): Boolean = loaded && !hasCards && !updating

internal fun showEngineFindingsEmpty(updating: Boolean): Boolean = !updating
