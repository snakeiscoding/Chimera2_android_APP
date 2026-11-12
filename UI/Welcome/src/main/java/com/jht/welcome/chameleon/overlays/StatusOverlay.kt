package com.jht.welcome.chameleon.overlays

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.theme.DynamicTheme
import com.jht.widgets.Blur
import com.jht.widgets.DebugIndicator
import com.jht.widgets.IndicatorBank
import com.jht.widgets.SmartWatchBank
import kotlin.math.roundToInt

@Composable
fun StatusOverlay(
    backgroundUri: Uri,
    blur: Boolean,
    screenWidthDp : Dp,
    screenHeightDp : Dp,
    rightBackgroundBlurCache: HashMap<Uri, Bitmap>,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(186 / 768f)
    ) {
        if (blur) {
            with (LocalDensity.current) {
                val screenWidth = screenWidthDp.toPx().roundToInt()
                val screenHeight = screenHeightDp.toPx().roundToInt()
                val thisWidth = maxWidth.toPx().roundToInt()
                val thisHeight = maxHeight.toPx().roundToInt()
                Blur(
                    backgroundUri = backgroundUri,
                    rect = Rect(
                        screenWidth - thisWidth, 0, thisWidth,
                        thisHeight
                    ),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    backgroundBlurCache = rightBackgroundBlurCache
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = DynamicTheme.colors.glass1.vertical()
                ),

            ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(140 / 186f)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.End
            ) {
                IndicatorBank(
                    modifier = Modifier
                        .height(60.dp)
                        .padding(vertical = 20.dp),
                )

                SmartWatchBank(
                    badgeModifier = Modifier.fillMaxWidth(),
                    orientation = Orientation.Vertical,
                    contentSpacing =  20.dp,
                )

                Spacer(modifier = Modifier.weight(1f))

                DebugIndicator(Modifier.fillMaxWidth().padding(vertical = 10.dp))
            }

        }
    }
}

