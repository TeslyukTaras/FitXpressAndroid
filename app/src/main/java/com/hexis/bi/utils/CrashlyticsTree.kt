package com.hexis.bi.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashlyticsTree : Timber.Tree() {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        crashlytics.log(if (tag != null) "[$tag] $message" else message)
        if (t != null && priority >= Log.ERROR) {
            crashlytics.recordException(t)
        }
    }
}
