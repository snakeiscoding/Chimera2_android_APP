package com.jht.progression.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jht.widgets.scaleSize

private val gray = Color(0xFFCCCCCC)

/**
 * Creates a basic UI element to display on the matrix method workout screen.
 */
@Composable
fun ColoredWorkoutBar(barColor: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .testTag("workout_reminder_text_bar")
            .size(116.dp.scaleSize(), 6.dp.scaleSize())
    ) {
        // the front bar
        Canvas(
            modifier = Modifier
                .size(82.dp.scaleSize(), 4.dp.scaleSize())
                .align(Alignment.Center)
                .clip(shape = RoundedCornerShape(2.dp.scaleSize())),
            onDraw = {
                drawLinearBar(barColor, stroke = 10f)
            }
        )

        // the image at back of the bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp.scaleSize())
                .align(Alignment.Center)
                .offset(y = 2.dp.scaleSize()),
            onDraw = {
                drawLine(gray, 0f, 0f, size.width, 0f, 1f, this)
                drawLine(gray, -3f, -3f, 0f, 0f, 1f, this)
                drawLine(gray, -3f, 3f, 0f, 0f, 1f, this)
                drawLine(gray, size.width + 3, -3f, size.width, 0f, 1f, this)
                drawLine(gray, size.width + 3, 3f, size.width, 0f, 1f, this)
            }
        )
    }
}

private fun drawLine(
    lineColor: Color,
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    stroke: Float,
    drawScope: DrawScope
) {
    drawScope.drawLine(
        color = lineColor,
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = stroke
    )
}

private fun DrawScope.drawLinearBar(
    color: Color,
    stroke: Float
) {
    val width = size.width

    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(width, 0f),
        strokeWidth = stroke
    )
}