package com.hexis.bi.ui.main.settings.mysuit

data class MySuitState(
    val isConnected: Boolean = false,
    val suitIdInput: String = "",
    val connectedSuitId: String = "",
    val connectedStatus: String = "Active",
    val showReconnectDialog: Boolean = false,
)
