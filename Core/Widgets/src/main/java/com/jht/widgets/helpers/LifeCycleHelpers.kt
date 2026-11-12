package com.jht.widgets.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Provides a lifecycle callback within Composable */
@Composable
fun ComposableLifecycle(
    lifeCycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onEvent: (LifecycleOwner, Lifecycle.Event) -> Unit
) {
    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { source, event ->
            onEvent(source, event)
        }
        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/** Performs an [action] when [LifecycleOwner] emits [Lifecycle.Event.ON_PAUSE] */
@Composable
fun LifeCycleOnPause(action: ()-> Unit) {
    ComposableLifecycle { _, event ->
        if (event == Lifecycle.Event.ON_PAUSE) {
            action()
        }
    }
}
