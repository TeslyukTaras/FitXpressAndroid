package com.hexis.bi.ui.main.settings.mysuit

import com.hexis.bi.utils.constants.SuitConstants

data class MySuitState(
    val isConnected: Boolean = false,
    val suitIdInput: String = "",
    val connectedSuitId: String = "",
    val connectedStatus: String = SuitConstants.STATUS_ACTIVE,
    val showReconnectDialog: Boolean = false,
)
