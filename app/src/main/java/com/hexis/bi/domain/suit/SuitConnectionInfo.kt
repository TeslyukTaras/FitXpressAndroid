package com.hexis.bi.domain.suit

import com.hexis.bi.utils.constants.SuitConstants

data class SuitConnectionInfo(
    val suitId: String,
    val status: String = SuitConstants.STATUS_ACTIVE,
)
