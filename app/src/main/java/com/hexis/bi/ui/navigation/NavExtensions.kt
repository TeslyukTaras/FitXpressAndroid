package com.hexis.bi.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

/**
 * Pops the back stack only if the current destination is fully resumed.
 * This prevents a rapid double-tap on a back button from popping twice
 * (the first tap pops, but the second runs before the destination changes).
 */
fun NavController.popBackStackOnce(): Boolean {
    val resumed = currentBackStackEntry
        ?.lifecycle
        ?.currentState
        ?.isAtLeast(Lifecycle.State.RESUMED) == true
    return if (resumed) popBackStack() else false
}
