package com.jht.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jht.ui.computeSize
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds


@Composable
fun BoxScope.AnimatedTouchScreenIcon() {
    val showTouchPrompt = remember { mutableStateOf(false) }

    /* The icon should fade in 10 seconds after the screen timeout occurs, stay for
     * 10 seconds, then fade out.  Repeat the fade in/show for 10 seconds/fade out
     * every 60 seconds after that. */
    LaunchedEffect(Unit){
        while (true) {
            delay(10.seconds)
            showTouchPrompt.value = true
            delay(10.seconds)
            showTouchPrompt.value = false
            delay(50.seconds)
        }
    }


    Box(modifier = Modifier // AnimatedVisibility breaks .align(), so we place this box first
        .align(Alignment.BottomEnd)
        .padding(end = 50.dp.computeSize(), bottom = 50.dp.computeSize())
    ) {
        AnimatedVisibility(
            showTouchPrompt.value,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(modifier = Modifier // this is the black tint behind the hand icon
                .background(Color.Black.copy(alpha = .6f))
            ){
                Image(
                    painterResource(
                        R.drawable.ic_tap_gesture),
                    contentDescription = "Tap prompt",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(
                            horizontal = 10.dp.computeSize(),
                            vertical = 15.dp.computeSize()
                        )
                        .size(70.dp.computeSize())
                )
            }
        }
    }
}