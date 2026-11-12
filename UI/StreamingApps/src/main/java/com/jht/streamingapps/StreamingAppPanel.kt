package com.jht.streamingapps

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.jht.interfaces.models.UIServiceModel
import com.jht.theme.DynamicTheme
import com.jht.theme.themePadding
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.helpers.getInjectionEntryPoint


/** Our UI overlay for streaming apps screen */
@Composable
fun StreamAppsOverlay(
    showOverlay: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    if (showOverlay) BoxWithConstraints (
        modifier,
    ) {
        StreamAppsControls(
            parentSizeDp = DpSize(maxWidth, maxHeight),
            onBack = onBack,
        )
    }
}

/** Movable controls element */
@Composable
private fun StreamAppsControls(
    parentSizeDp: DpSize,
    onBack: () -> Unit = {},
) {
    var sizeDp by remember { mutableStateOf(DpSize.Zero) }
    var offsetDp by remember(parentSizeDp, sizeDp) {
        // default to lower right corner
        val floatingPad = 10.dp
        mutableStateOf(DpOffset(parentSizeDp.width - sizeDp.width - floatingPad,parentSizeDp.height - sizeDp.height - floatingPad))
    }
    val shape = DynamicTheme.corners.medium.makeCornerShape()
    with(LocalDensity.current) {
        Box(
            Modifier
                .offset(offsetDp.x, offsetDp.y)
                .background(DynamicTheme.colors.gray2, shape)
                .clip(shape)
                .onGloballyPositioned { coordinates ->
                    sizeDp = DpSize(coordinates.size.width.toDp(), coordinates.size.height.toDp())
                }
                .pointerInput(parentSizeDp) {
                    detectDragGestures { change, dragAmount ->
                        change.consume() // Consume the event to prevent propagation
                        offsetDp = DpOffset(
                            (offsetDp.x + dragAmount.x.toDp()).coerceIn(0.dp, parentSizeDp.width - sizeDp.width),
                            (offsetDp.y + dragAmount.y.toDp()).coerceIn(0.dp, parentSizeDp.height - sizeDp.height)
                        )
                    }
                },
        ) {
            Row(
                modifier = Modifier.padding(themePadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Move icon
                Icon(
                    painterResource(R.drawable.move),
                    "move",
                    modifier = Modifier.size(20.dp),
                    tint = DynamicTheme.colors.text1,
                )
                // Buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(themePadding),
                ) {
                    BackButton(onClick = onBack)
                    FullscreenButton()
                }
            }
        }
    }
}


private val buttonColor @Composable get() = DynamicTheme.colors.gray15
private val buttonIconSize = 32.dp
/** Comp is not typically theme button, so style a base here for now */
@Composable
private fun CustomButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        border = DynamicTheme.borders.small.borderStroke(buttonColor),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(20.dp),
        content = content,
    )
}


@Composable
private fun FullscreenButton(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state = remember { FullscreenButtonState(context) }
    LaunchedEffect(Unit) { state.start() }
    DisposableEffect(Unit) { onDispose { state.stop() } }

    CustomButton(
        modifier = modifier.testTag("ResizeButton"),
        onClick = state::toggleFullscreen,
    ) {
        Icon(
            painterResource(if (state.isFullScreen.value) R.drawable.ic_shrink_white else R.drawable.ic_expand_white),
            null,
            Modifier.size(buttonIconSize),
            tint = buttonColor,
        )
    }
}
private class FullscreenButtonState(context: Context)  {
    val isFullScreen = mutableStateOf(false)
    private val uiModel: UIServiceModel = getInjectionEntryPoint(context).ui()
    fun start() {
        uiModel.stateFullScreen.listenAndTrigger { isFullScreen.value = it }
    }
    fun stop() {
        uiModel.close()
    }
    fun toggleFullscreen() {
        uiModel.toggleFullscreen()
    }
}

@Composable
private fun BackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    CustomButton(
        modifier = modifier.testTag("BackButton"),
        onClick = onClick,
    ) {
        Icon(
            painterResource(R.drawable.ic_back_button_white),
            null,
            Modifier.size(buttonIconSize),
            tint = buttonColor,
        )
    }
}



//region PREVIEWS
@AllDevicePreviews
@Composable
private fun PreviewStreamButtons() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        StreamAppsControls(parentSizeDp = DpSize(600.dp, 400.dp))
    }
}

@AllDevicePreviews
@Composable
private fun PreviewStreamAppsMenu() {
    // Not sure why preview does not handle the math right?
    Box(Modifier.fillMaxSize().background(Color.Green)) {
        StreamAppsOverlay(
            modifier = Modifier.fillMaxSize(0.5f).background(Color.Black),
            showOverlay = true,
        )
    }
}
//endregion PREVIEWS
