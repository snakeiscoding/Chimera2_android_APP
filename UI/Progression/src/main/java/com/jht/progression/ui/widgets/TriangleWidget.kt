package com.jht.progression.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.widgets.scaleSize

/**The two opposite triangles on the circle pattern that implement on the workout screen
 * The color will match the interval label and the current zone color
 * */
@Composable
fun TwoOppositeTriangles(
    color: Color,
    triangleWidth: Dp = 8.dp.scaleSize(),
    triangleHeight: Dp = 12.dp.scaleSize()
) {
    Box(
        modifier = Modifier
            .size(362.dp.scaleSize(), 52.dp.scaleSize())
            .testTag("two_opposite_triangles"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.width(320.dp.scaleSize()).fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawTriangle(
                color = color,
                triangleWidth = triangleWidth,
                triangleHeight = triangleHeight,
                isLeft = true
            )
            DrawTriangle(
                color = color,
                triangleWidth = triangleWidth,
                triangleHeight = triangleHeight,
                isLeft = false
            )
        }
    }
}

@Composable
private fun DrawTriangle(
    color: Color,
    triangleWidth: Dp,
    triangleHeight: Dp,
    isLeft: Boolean
) {
    Canvas(modifier = Modifier.size(triangleWidth, triangleHeight)) {
        val path = Path().apply {
            if (isLeft) {
                // Left triangle (pointing right)
                moveTo(0f, 0f)
                lineTo(0f, size.height)
                lineTo(size.width, size.height / 2)
                close()
            } else {
                // Right triangle (pointing left)
                moveTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height / 2)
                close()
            }
        }
        drawPath(color = color, path = path)
    }
}

@Composable
fun convertToPx(floatValue: Float): Float {
    val density = LocalDensity.current
    return with(density) { floatValue.toDp().scaleSize().toPx() }
}
