package com.hexis.bi.data.subscription.billing

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CurrentActivityProvider : Application.ActivityLifecycleCallbacks {

    private var current: WeakReference<Activity>? = null

    private val _resumed = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val resumed: SharedFlow<Unit> = _resumed.asSharedFlow()

    fun get(): Activity? = current?.get()

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
        _resumed.tryEmit(Unit)
    }

    override fun onActivityPaused(activity: Activity) {
        if (current?.get() === activity) current = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
