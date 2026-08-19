package com.hexis.bi.ui.main.home.intelligence

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal enum class BackfillTransition {
    ACTIVE,
    SETTLED,
    IDLE,
}

internal fun backfillTransition(wasInFlight: Boolean, isInFlight: Boolean): BackfillTransition =
    when {
        isInFlight -> BackfillTransition.ACTIVE
        wasInFlight -> BackfillTransition.SETTLED
        else -> BackfillTransition.IDLE
    }

internal class LatestJobController {
    private var job: Job? = null

    fun replace(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit) {
        job?.cancel()
        job = scope.launch(block = block)
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
