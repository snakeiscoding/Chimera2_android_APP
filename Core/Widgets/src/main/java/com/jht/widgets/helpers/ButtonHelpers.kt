package com.jht.widgets.helpers

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import kotlinx.coroutines.delay

/**
 * Hooks into the given [interactionSource] to trigger an action on pressed, then after an initial delay, periodically triggers action until released.
 */
@Composable
fun PressedAction(
    interactionSource: MutableInteractionSource, // must be declared where it can be shared with UI element
    //delay: Duration = 500.milliseconds, // would it be useful to expose these or purposely encapsulate them to keep them consistent?
    //period: Duration = 100.milliseconds,
    action: () -> Unit,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(isPressed) {
        if (isPressed) {
            action()
            delay(500) // initial delay
            while (true) {
                action()
                delay(100) // periodic scan rate
            }
        }
    }
}


/** Provide a means to alter the default [ViewConfiguration] for touch durations */
@Composable
fun ViewConfigurationOverride(
    longPressTimeoutMillis: Long? = null,
    doubleTapTimeoutMillis: Long? = null,
    doubleTapMinTimeMillis: Long? = null,
    touchSlop: Float? = null,
    content: @Composable () -> Unit
) {
    fun ViewConfiguration.updateViewConfiguration() = object : ViewConfiguration {
        override val longPressTimeoutMillis
            get() = longPressTimeoutMillis ?: this@updateViewConfiguration.longPressTimeoutMillis

        override val doubleTapTimeoutMillis
            get() = doubleTapTimeoutMillis ?: this@updateViewConfiguration.doubleTapTimeoutMillis

        override val doubleTapMinTimeMillis
            get() = doubleTapMinTimeMillis ?: this@updateViewConfiguration.doubleTapMinTimeMillis

        override val touchSlop: Float
            get() = touchSlop ?: this@updateViewConfiguration.touchSlop
    }

    CompositionLocalProvider(
        LocalViewConfiguration provides LocalViewConfiguration.current.updateViewConfiguration()
    ) {
        content()
    }
}
