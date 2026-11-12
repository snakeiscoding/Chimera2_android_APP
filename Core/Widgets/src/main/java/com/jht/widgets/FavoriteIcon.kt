package com.jht.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jht.widgets.helpers.getInjectionEntryPoint


/** Icon for toggling favorite status for a workout (PLACEHOLDER - retail feature, so doing it later) */
@Composable
fun FavoriteIcon(
    modifier: Modifier = Modifier,
    // probably need to pass workout ID of some kind?
) {
    val context = LocalContext.current
    val state = remember { FavoriteIconState(context) }
    LaunchedEffect(Unit) { state.start() }
    DisposableEffect(Unit) { onDispose { state.stop() } }
    if (state.isSupported) {
        // TODO - favorite icon (maybe as a self contained widget that hides itself in non retail?)
        // Heart icon filled if already favorite, empty otherwise
        // clicking on heart icon should favorite the current workout
    }
}

private class FavoriteIconState(context: Context) {
    val isSupported: Boolean
    init {
        val injection = getInjectionEntryPoint(context)
        isSupported = injection.uiBase().isRetail
    }
    fun start() {}
    fun stop() {}
}
