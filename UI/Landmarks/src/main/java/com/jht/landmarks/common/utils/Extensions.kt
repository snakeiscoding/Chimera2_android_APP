package com.jht.landmarks.common.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

object Extensions {

    // Modifier extension to clip an image based on progress
    fun Modifier.clipToProgress(progress: Float): Modifier = composed {
        this
            .graphicsLayer {
                clip = true
                shape = RectangleShape
            }
            .drawWithContent {
                val height = size.height
                val progressHeight = height * progress
                clipRect(
                    left = 0f,
                    top = height - progressHeight,
                    right = size.width,
                    bottom = height
                ) {
                    this@drawWithContent.drawContent()
                }
            }
    }
}

val Int.scaledDp: androidx.compose.ui.unit.Dp
    @androidx.compose.runtime.Composable
    get() {
        val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
        // 1920 is the reference width
        return (this * (screenWidth / 1920f)).dp
    }