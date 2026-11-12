package com.jht.widgets

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.jht.widgets.helpers.BlurBuilder


@Composable
fun Blur(
    backgroundUri: Uri,
    rect: Rect,
    screenWidth: Int,
    screenHeight: Int,
    backgroundBlurCache: HashMap<Uri, Bitmap>
) {
    var overlayBlur = backgroundBlurCache[backgroundUri]
    if (overlayBlur == null) {
        overlayBlur =
            BlurBuilder.blur(LocalContext.current, backgroundUri, rect, screenWidth, screenHeight)
        backgroundBlurCache[backgroundUri] = overlayBlur
    }

    Crossfade(
        targetState = overlayBlur,
        animationSpec = tween(2000)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (it != null) {
                drawImage(it.asImageBitmap())
            }
        }
    }
}
