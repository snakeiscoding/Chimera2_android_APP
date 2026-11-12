package com.jht.landmarks.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.jht.landmarks.R
import com.jht.landmarks.common.utils.scaledDp

import com.jht.landmarks.common.utils.Colors

@Composable
internal fun PauseControlPanel(
    isPaused: Boolean,
    onPauseClick: () -> Unit
) {
    val componentTag = "PauseControlPanel"
    val cornerShape = RoundedCornerShape(12.scaledDp)

    Box(
        modifier = Modifier
            .width(140.scaledDp)
            .height(80.scaledDp)
            .clip(cornerShape)
            .background(Colors.PanelBackgroundColor)
            .border(1.scaledDp, Colors.PanelBorderColor, cornerShape)
            .clickable { onPauseClick() }
            .testTag("$componentTag:PauseToggle:Button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = if (isPaused) R.drawable.ic_play_white else R.drawable.ic_pause_white),
            contentDescription = if (isPaused) "Resume" else "Pause",
            tint = Color.White,
            modifier = Modifier.size(32.scaledDp)
                .testTag("$componentTag:PauseIcon:Image")
        )
    }
}