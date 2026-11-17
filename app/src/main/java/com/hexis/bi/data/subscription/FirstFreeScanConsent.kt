package com.hexis.bi.data.subscription

import com.hexis.bi.utils.constants.SubscriptionConstants

class FirstFreeScanConsent(private val clock: () -> Long = System::currentTimeMillis) {

    @Volatile
    private var grantedAtMillis: Long? = null

    fun grant() {
        grantedAtMillis = clock()
    }

    fun consume(): Boolean {
        val granted = grantedAtMillis ?: return false
        grantedAtMillis = null
        return clock() - granted <= SubscriptionConstants.FIRST_FREE_SCAN_CONSENT_TTL_MS
    }
}
