package com.jht.streamingapps

import android.view.MotionEvent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.isSpecified
import com.jht.data.AppInfo
import com.jht.interfaces.models.VideoInput

import com.jht.theme.*
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.VideoInputBox

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StreamingAppsScreen(
    theme: DynamicThemeData = defaultDynamicTheme,
    videoInput: VideoInput = VideoInput(),
    onTouchDown: (Float, Float)->Unit = {_,_->},
    onDrag: (Float, Float)->Unit = {_,_->},
    onHScroll: (Float)->Unit = {},
    onVScroll: (Float)->Unit = {},
    onTapUp: ()->Unit = {},
    showingOverlay: Boolean = true,
    onShowOverlay: () -> Unit = {},
    onCloseOverlay: () -> Unit = {},
    onBackButton: () -> Unit = {},
    noSignal: State<Boolean> = mutableStateOf(false),
    safeRegionPx: android.graphics.Rect = android.graphics.Rect(),
) {
    val safeRegionOffset = with(LocalDensity.current) {
        if (safeRegionPx.isEmpty) DpOffset.Unspecified
        else DpOffset(safeRegionPx.left.toDp(), safeRegionPx.top.toDp())
    }
    val safeRegionSize = with(LocalDensity.current) {
        if (safeRegionPx.isEmpty) DpSize.Unspecified
        else DpSize(safeRegionPx.width().toDp(), safeRegionPx.height().toDp())
    }
    DynamicTheme(theme) {
        Box(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .fillMaxSize()
                .background(DynamicTheme.colors.gradient1.vertical()),
        ) {
            var videoRectPx by remember { mutableStateOf(Rect(0f,0f,0f,0f)) }
            var dragTotalOffset = remember { Offset(0f,0f) }
            with(LocalDensity.current) {
                VideoInputBox(
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            val sizePixels = coordinates.size
                            //val offsetPixels = coordinates.positionInRoot()
                            videoRectPx = calculateVideoArea(sizePixels.width.toFloat(), sizePixels.height.toFloat())
                        }
                        .pointerInteropFilter { // raw touch events tend to be in Px, not Dp
                            when (it.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    onTouchDown(
                                        if (videoRectPx.width == 0f) 0f else clampFloat0to1((it.x - videoRectPx.left) / videoRectPx.width),
                                        if (videoRectPx.height == 0f) 0f else clampFloat0to1((it.y - videoRectPx.top) / videoRectPx.height),
                                    )
                                }
                            }
                            false // let event through for gesture detection
                        }
                        /* moving away from scroll to remote keys, favoring swipe to remote key
                        .scrollable(
                            orientation = Orientation.Horizontal,
                            state = rememberScrollableState { delta ->
                                onHScroll(delta)
                                delta
                            }
                        )
                        .scrollable(
                            orientation = Orientation.Vertical,
                            state = rememberScrollableState { delta ->
                                onVScroll(delta)
                                delta
                            }
                        )*/
                        .pointerInput(Unit) {
                            // detectDragGestures as alternative to scrollable blocks (single complete swipe as event)
                            detectDragGestures(
                                onDragStart = { dragTotalOffset = Offset(0f, 0f) },
                                onDragEnd = { onDrag(dragTotalOffset.x, dragTotalOffset.y) },
                            ) { change, dragAmount ->
                                dragTotalOffset =
                                    Offset(dragTotalOffset.x + dragAmount.x, dragTotalOffset.y + dragAmount.y)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    onTapUp()
                                },
                                onLongPress = {
                                    onShowOverlay()
                                }
                            )
                        },
                    videoInput = videoInput,
                )
            }

            if (noSignal.value) Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                Text(
                    stringResource(R.string.no_hdmi_detected_pg),
                    Modifier.align(Alignment.Center),
                    color = Color.White,
                    style = DynamicTheme.typography.textStyle3,
                    textAlign = TextAlign.Center,
                )
            }

            // keep the overlay within the safe region (avoid areas covered by system panels)
            val overlayModifier =
                if (safeRegionSize.isSpecified) Modifier.offset(safeRegionOffset.x, safeRegionOffset.y).size(safeRegionSize)
                else Modifier.fillMaxSize()
            StreamAppsOverlay(
                showOverlay = showingOverlay,
                modifier = overlayModifier,
                onClose = onCloseOverlay,
                onBack = onBackButton,
            )
        }
    }
}


// the camerax preview enforces aspect ratio, so actual video may be offset by letterbox (this assumes 16:9)
private const val ratioW2H = 16f / 9f
private fun calculateVideoArea(viewW: Float, viewH: Float): Rect {
    // https://stackoverflow.com/a/75111726
    val streamH: Float
    val streamW: Float
    val left: Float
    val top: Float
    if (viewW / viewH > ratioW2H) {
        streamH = viewH
        streamW = viewH * ratioW2H
        left = (viewW - streamW) / 2
        top = 0f
    } else {
        streamW = viewW
        streamH = viewW / ratioW2H
        left = 0f
        top = (viewH - streamH) / 2
    }
    return Rect(left, top, left + streamW, top + streamH)
}
private fun clampFloat0to1(v : Float): Float = maxOf(minOf(v, 1f), 0f)


//region PREVIEWS
@AllDevicePreviews
@Composable
private fun PreviewStreamingAppsScreenDark() {
    StreamingAppsScreen(
        theme = darkDynamicTheme,
    )
}

@AllDevicePreviews
@Composable
private fun PreviewStreamingAppsScreenLight() {
    StreamingAppsScreen(
        theme = lightDynamicTheme,
    )
}
//endregion PREVIEWS
