package com.jht.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.ui.computeSize

/*
    File of scrolling widgets like scroll bars or scroll indicators etc
 */



/**
 * Fade scrim + pulsing chevron scroll indicator over scrollable [content].
 *
 * @param state the scroll state driving [content]: a `LazyListState` for a LazyColumn, or a
 * `ScrollState` for a Column using `verticalScroll`. Hints show only while that state can scroll.
 * @param scrimColor should match the background behind [content] so the fade blends in.
 */
@Composable
fun ScrollHintOverlay(
    state: ScrollableState,
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black,
    tint: Color = Color.White,
    fadeHeight: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        content()

        val canUp by remember(state) { derivedStateOf { state.canScrollBackward } }
        val canDown by remember(state) { derivedStateOf { state.canScrollForward } }

        // top fade
        AnimatedVisibility(
            visible = canUp,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(fadeHeight)
                    .background(
                        Brush.verticalGradient(
                            listOf(scrimColor.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
            )
        }

        // bottom fade + chevron
        AnimatedVisibility(
            visible = canDown,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(fadeHeight * 1.6f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, scrimColor.copy(alpha = 0.85f))
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                val nudge = rememberInfiniteTransition(label = "scrollHint")
                    .animateFloat(
                        initialValue = 0f,
                        targetValue = 6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(700, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "nudge"
                    )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown, // or painterResource(R.drawable.ic_chevron_down)
                    contentDescription = null,
                    tint = tint.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(24.dp.computeSize())
                        .offset(y = (-4).dp + nudge.value.dp)
                )
            }
        }
    }
}
