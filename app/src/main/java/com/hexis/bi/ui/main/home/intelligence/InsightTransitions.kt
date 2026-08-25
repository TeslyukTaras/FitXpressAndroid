package com.hexis.bi.ui.main.home.intelligence

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.hexis.bi.utils.constants.AnimationConstants

internal fun insightCrossfade(): ContentTransform =
    fadeIn(tween(AnimationConstants.TAB_CONTENT_FADE_IN_MS)) togetherWith
        fadeOut(tween(AnimationConstants.TAB_CONTENT_FADE_OUT_MS))

internal fun insightEnter(): EnterTransition =
    fadeIn(tween(AnimationConstants.TAB_CONTENT_FADE_IN_MS)) +
        expandVertically(tween(AnimationConstants.TAB_CONTENT_FADE_IN_MS))

internal fun insightExit(): ExitTransition =
    fadeOut(tween(AnimationConstants.TAB_CONTENT_FADE_OUT_MS)) +
        shrinkVertically(tween(AnimationConstants.TAB_CONTENT_FADE_OUT_MS))
