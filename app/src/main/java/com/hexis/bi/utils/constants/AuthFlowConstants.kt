package com.hexis.bi.utils.constants

internal object AuthFlowConstants {

    const val HERO_TOP_SPACER_WEIGHT = 34f
    const val HERO_BOTTOM_SPACER_WEIGHT = 125f

    /** Opacity of the brand-teal tint applied over the auth gradient image. */
    const val GRADIENT_TINT_ALPHA = 0.5f

    /** Number of digits in the email-verification code. Must match the backend's `emailCodeLength`. */
    const val EMAIL_CODE_LENGTH = 5

    /** Resend cooldown in seconds. Must match the backend's `emailResendCooldownMs`. */
    const val EMAIL_RESEND_COOLDOWN_SECONDS = 30
}
