package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.BORDER_GRAY
import com.jht.sprint8.utils.Sprint8Colors.TEXT_COLOR_1
import com.jht.ui.AllDevicePreviews

/**
 * Displays a button that toggles between fullscreen and normal view states.
 *
 * @param modifier Modifier applied to the button for layout, styling, and interaction.
 * @param maxWidth Maximum width the button can occupy.
 * @param maxHeight Maximum height the button can occupy.
 * @param isFullscreen Indicates whether the current view is in fullscreen mode.
 * Used to update the button’s icon or appearance.
 * @param onFullscreenClick Callback invoked when the button is clicked to toggle
 * fullscreen mode.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun FullScreenButton(
    modifier: Modifier,
    maxWidth: Dp,
    maxHeight: Dp,
    isFullscreen: Boolean,
    onFullscreenClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(
                end = maxWidth * 16 / 1600f, bottom = maxHeight * 16 / 913f
            )
            .size(width = maxWidth * 98 / 1600f, height = maxHeight * 78 / 913f)
            .background(
                color = TEXT_COLOR_1, shape = RoundedCornerShape(maxWidth * 18 / 1600f)
            )
            .border(
                BorderStroke(maxWidth * 1 / 1600f, BORDER_GRAY),
                RoundedCornerShape(maxWidth * 18 / 1600f)
            )
            .semantics { testTagsAsResourceId = true }
            .testTag("FullScreenButton:ToggleFullscreen:Box")
            .clickable(onClick = onFullscreenClick),
        contentAlignment = Alignment.Center
    ) {
        val iconType = if (isFullscreen) "Collapse" else "Expand"
        Icon(
            tint = Color.White,
            painter = painterResource(if (isFullscreen) R.drawable.arrows_in_collapse_bold else R.drawable.arrows_out_expand_bold),
            contentDescription = "Screen $iconType Button",
            modifier = Modifier
                .size(maxWidth * 28 / 1600f)
                .testTag("FullScreenButton:${iconType}Icon:Image")
        )
    }
}

@AllDevicePreviews
@Composable
private fun FullScreenButtonPreview() {
    val maxWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray),
        contentAlignment = Alignment.BottomEnd
    ) {
        FullScreenButton(
            modifier = Modifier,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            isFullscreen = true,
            onFullscreenClick = {})
    }
}