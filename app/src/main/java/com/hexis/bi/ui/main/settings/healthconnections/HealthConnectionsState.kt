package com.hexis.bi.ui.main.settings.healthconnections

import com.hexis.bi.domain.enums.HealthProvider

data class HealthConnectionsState(
    val connectedProviders: Set<HealthProvider> = emptySet(),
)
