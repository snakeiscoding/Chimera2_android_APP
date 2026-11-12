package com.jht.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jht.resources.R

/** Places a locked overlay on top of the given [content] */
@Composable
fun LockedOverlay(
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    lockTestTag: String = "",
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier) {
        content()
        if (isLocked) { // old UI used tiers_app_disabled.xml
            // bottom start triangle
            Canvas(
                Modifier.matchParentSize()
            ) {
                val path = Path().apply {
                    val width = size.width
                    val height = size.height
                    moveTo(0f, height)
                    lineTo(width, height)
                    lineTo(0f, 0f)
                    close()
                }
                drawPath(path, Color.Black.copy(0.8f))
            }
            // lock icon
            Icon(
                painterResource(R.drawable.icon_lock),
                contentDescription = null,
                Modifier
                    .fillMaxHeight(0.3f)
                    .aspectRatio(1f)
                    .padding(5.dp)
                    .align(Alignment.BottomStart)
                    .testTag(lockTestTag),
                tint = Color.White,
            )
        }
    }
}
