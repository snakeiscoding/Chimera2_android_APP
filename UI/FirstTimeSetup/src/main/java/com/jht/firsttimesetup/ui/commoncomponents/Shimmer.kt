package com.jht.firsttimesetup.ui.commoncomponents

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun ShimmerBrush(
    colorOne: Color = Color.LightGray,
    colorTwo: Color = Color.Gray,
    colorThree: Color = Color.LightGray
) : Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "shimmerX"
    )

    return Brush.linearGradient(
        colors = listOf(
            colorOne.copy(alpha = 0.6f),
            colorTwo.copy(alpha = 0.3f),
            colorThree.copy(alpha = 0.6f)
        ),
        start = Offset.Zero,
        end = Offset(x = shimmerX.value, y = shimmerX.value)
    )
}