package com.jht.workoutselection.common.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun Modifier.swipeGestureModifier(
    dragOffset: Animatable<Float, AnimationVector1D>,
    screenWidthPx: Float,
    coroutineScope: CoroutineScope,
    onSwipeLeft: suspend () -> Unit,
    onSwipeRight: suspend () -> Unit
): Modifier = pointerInput(Unit) {
    detectHorizontalDragGestures(
        onHorizontalDrag = { _, dragAmount ->
            coroutineScope.launch {
                dragOffset.snapTo(dragOffset.value + dragAmount)
            }
        },
        onDragEnd = {
            when {
                dragOffset.value <= -screenWidthPx / 4 -> {
                    coroutineScope.launch { onSwipeLeft() }
                }

                dragOffset.value >= screenWidthPx / 4 -> {
                    coroutineScope.launch { onSwipeRight() }
                }

                else -> {
                    coroutineScope.launch {
                        dragOffset.animateTo(0f, animationSpec = tween(durationMillis = 300))
                    }
                }
            }
        }
    )
}

