package com.jht.trackapp.common.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private fun getValuePercentageForRange(value: Float, max: Float, min: Float) =
    (value - min) / (max - min)

@OptIn(ExperimentalComposeUiApi::class)
@Preview
@Composable
fun ProfComponent(){
    Box(modifier = Modifier.semantics { testTagsAsResourceId = true }.fillMaxWidth().background(Color.Black).height(300.dp)){
        PerformanceChart(color = Color(0xFFFE281D).copy(alpha = 0.5f), list =  listOf<Float>(10f,20f,30f,5f), lineColor = Color(0xFFFE281D))
        PerformanceChart(color = Color(0xFF00AFCE).copy(alpha = 0.5f), list = listOf<Float>(20f,12f,2f,11f), lineColor = Color(0xFF00AFCE))
        PerformanceChart(color = Color(0xFFFBD700).copy(alpha = 0.5f), list = listOf<Float>(3f,17f,25f,18f), lineColor = Color(0xFFFBD700))
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PerformanceChart(modifier: Modifier = Modifier,color: Color,list: List<Float>,lineColor: Color) {
    Row(modifier = modifier) {
        val max = list.maxOrNull() ?: 0f
        val min = list.minOrNull() ?: 0f

        Canvas(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .fillMaxSize()
                .weight(1f),
            onDraw = {
                val path = Path().apply {
                    if (list.isNotEmpty()) {
                        moveTo(0f, size.height)
                        val lastIndex = list.lastIndex
                        for (index in 0 until lastIndex) {
                            val fromValuePercentage = getValuePercentageForRange(list[index], max, min)
                            val toValuePercentage = getValuePercentageForRange(list[index + 1], max, min)

                            val fromPoint = Offset(x = index * (size.width / lastIndex), y = size.height * (1 - fromValuePercentage))
                            val toPoint = Offset(x = (index + 1) * (size.width / lastIndex), y = size.height * (1 - toValuePercentage))

                            lineTo(fromPoint.x, fromPoint.y)
                            lineTo(toPoint.x, toPoint.y)
                        }
                        lineTo(size.width, size.height)
                        close()
                    }
                }

                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        listOf(
                            color,
                            Color.Transparent,
                        ),
                        endY = size.height
                    ),
                )

                for (index in 0 until list.lastIndex) {
                    val fromValuePercentage = getValuePercentageForRange(list[index], max, min)
                    val toValuePercentage = getValuePercentageForRange(list[index + 1], max, min)

                    val fromPoint = Offset(x = index * (size.width / list.size), y = size.height * (1 - fromValuePercentage))
                    val toPoint = Offset(x = (index + 1) * (size.width / list.size), y = size.height * (1 - toValuePercentage))

                    drawLine(
                        color = lineColor,
                        start = fromPoint,
                        end = toPoint,
                        strokeWidth = 3f
                    )
                }
            }
        )
    }
}
