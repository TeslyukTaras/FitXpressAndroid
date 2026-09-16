package com.hexis.bi.ui.update

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.hexis.bi.data.appupdate.AppUpdateRequirementRepository
import com.hexis.bi.utils.constants.AnimationConstants
import com.hexis.bi.utils.constants.AppUpdateStore
import org.koin.compose.koinInject

@Composable
fun UpdateRequiredGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val repository: AppUpdateRequirementRepository = koinInject()
    val updateRequired by repository.updateRequired.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            repository.refresh()
        }
    }

    Box(modifier = modifier) {
        content()

        AnimatedVisibility(
            visible = updateRequired,
            enter = fadeIn(tween(AnimationConstants.UPDATE_GATE_FADE_IN_MS)),
            exit = fadeOut(tween(AnimationConstants.UPDATE_GATE_FADE_OUT_MS)),
            modifier = Modifier.fillMaxSize(),
        ) {
            UpdateRequiredScreen(onUpdate = { context.openStoreListing() })
        }
    }
}

private fun Context.openStoreListing() {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, AppUpdateStore.MARKET_URI.toUri()))
    }.onFailure {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, AppUpdateStore.PLAY_STORE_URI.toUri()))
        }
    }
}
