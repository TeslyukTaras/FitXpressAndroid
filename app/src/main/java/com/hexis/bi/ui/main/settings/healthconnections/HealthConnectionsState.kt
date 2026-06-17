package com.hexis.bi.ui.main.settings.healthconnections

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.data.healthconnections.HealthConnection
import com.hexis.bi.domain.enums.HealthProvider

data class TerraProviderUi(
    val code: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int = R.drawable.ic_connect,
)

data class HealthConnectionsState(
    val connectedProviders: Set<HealthProvider> = emptySet(),
    val wearableConnections: List<HealthConnection> = emptyList(),
    val widgetUrl: String? = null,
    val sdkProviders: List<TerraProviderUi> = emptyList(),
    val wearableProviders: List<TerraProviderUi> = emptyList(),
    val otherProviders: List<TerraProviderUi> = emptyList(),
    val pendingAuthUserId: String? = null,
    val pendingAuthProvider: String? = null,
)
