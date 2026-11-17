package com.hexis.bi.domain.subscription

enum class ScanBlockReason { NO_SUBSCRIPTION, ALL_SCANS_USED, ON_HOLD, PENDING }

sealed interface ScanAccess {
    data object Allowed : ScanAccess
    data object FirstFreeScan : ScanAccess
    data object SubscriptionRequired : ScanAccess
    data object PaymentIssue : ScanAccess
    data object AllScansUsed : ScanAccess
}

fun SubscriptionStatus.toScanAccess(): ScanAccess? = when (this) {
    SubscriptionStatus.Loading -> null
    is SubscriptionStatus.None -> if (freeScanUsed) ScanAccess.SubscriptionRequired else ScanAccess.FirstFreeScan
    is SubscriptionStatus.Expired -> if (paymentIssue) ScanAccess.PaymentIssue else ScanAccess.SubscriptionRequired
    is SubscriptionStatus.Subscribed -> if (scansRemaining > 0) ScanAccess.Allowed else ScanAccess.AllScansUsed
}

fun ScanBlockReason.toScanAccess(): ScanAccess = when (this) {
    ScanBlockReason.ALL_SCANS_USED -> ScanAccess.AllScansUsed
    ScanBlockReason.ON_HOLD, ScanBlockReason.PENDING -> ScanAccess.PaymentIssue
    ScanBlockReason.NO_SUBSCRIPTION -> ScanAccess.SubscriptionRequired
}
