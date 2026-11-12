package com.jht.landmarks.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.jht.landmarks.R
import com.jht.landmarks.common.utils.scaledDp

import com.jht.landmarks.common.utils.Colors

@Composable
internal fun ToggleFullScreenButton(
    isFullScreen: Boolean,
    onClick: () -> Unit
) {
    val componentTag = "ToggleFullScreenButton"
    val cornerShape = RoundedCornerShape(12.scaledDp)

    Box(
        modifier = Modifier
            .size(72.scaledDp)
            .clip(cornerShape)
            .background(Colors.PanelBackgroundColor)
            .border(1.scaledDp, Colors.PanelBorderColor, cornerShape)
            .clickable(onClick = onClick)
            .testTag("$componentTag:Toggle:Button"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                if (isFullScreen) R.drawable.ic_shrink_white else R.drawable.ic_expand_white
            ),
            contentDescription = "fullscreen toggle",
            modifier = Modifier
                .size(32.scaledDp)
                .testTag("$componentTag:ToggleIcon:Image")
        )
    }
}