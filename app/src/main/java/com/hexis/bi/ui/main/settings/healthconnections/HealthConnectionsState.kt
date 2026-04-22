package com.hexis.bi.ui.main.settings.healthconnections

import com.hexis.bi.data.healthconnections.HealthConnection
import com.hexis.bi.domain.enums.HealthProvider

data class HealthConnectionsState(
    val connectedProviders: Set<HealthProvider> = emptySet(),
    val wearableConnections: List<HealthConnection> = emptyList(),
    val widgetUrl: String? = null,
)
