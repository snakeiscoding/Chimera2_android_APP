package com.jht.widgets.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.jht.enums.UiEvent

/** Emits [active] when composable launches and [inactive] when composable disposes */
@Composable
fun UiEventEmitter(
    active: UiEvent? = null,
    inactive: UiEvent? = null,
) {
    val context = LocalContext.current
    val model = remember { getInjectionEntryPoint(context).system() }
    LaunchedEffect(Unit) {
        active?.let { model.sendUiEvent(it) }
    }
    DisposableEffect(Unit) {
        onDispose {
            inactive?.let { model.sendUiEvent(it) }
        }
    }
}
