package com.jht.progression.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.progression.oxaniumStyle


@Composable
fun StatBoxWidget(
    modifier: Modifier = Modifier,
    width: Dp = 720.dp,
    height: Dp = 250.dp,
    border: @Composable () -> Unit = {
        androidx.compose.foundation.Canvas(
            modifier = Modifier,
            onDraw = {
                drawStatBorder()
            })
    },
    content: @Composable () -> Unit
) {
    Surface (
        modifier = Modifier.size(width = width, height = height),
        color = Color.Transparent
            ) {
        border()
        content()
    }
}

@Composable
private fun StatRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row (modifier = modifier) {
        Layout(
            content = {
                Text(
                    text = key,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .layoutId("key")
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    style = oxaniumStyle,
                )
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.layoutId("line"),
                    onDraw = {
                        drawLine(
                            color = Color.White,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2f,
                            cap = StrokeCap.Square
                        )
                    })
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .layoutId("value")
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    style = oxaniumStyle,
                )
            }
        ) { measurables, constraints ->
            val keyPlaceable = measurables.first { it.layoutId == "key" }.measure(constraints)
            val valuePlaceable = measurables.first { it.layoutId == "value" }.measure(constraints)
            val lineConstaints = constraints.copy(
                minHeight = keyPlaceable.height,
                maxHeight = keyPlaceable.height,
                minWidth = constraints.maxWidth - keyPlaceable.width - valuePlaceable.width,
                maxWidth = constraints.maxWidth - keyPlaceable.width - valuePlaceable.width
            )
            val linePlaceable = measurables.first { it.layoutId == "line" }.measure(lineConstaints)
            layout(width = constraints.maxWidth, height = keyPlaceable.height) {
                keyPlaceable.placeRelative(0, 0)
                linePlaceable.placeRelative(keyPlaceable.width, 0)
                valuePlaceable.placeRelative(constraints.maxWidth - valuePlaceable.width, 0)

            }
        }
    }
}

@Composable
fun StatBoxContent(
    stats: Map<String, String>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal =  20.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        stats.forEach { stat ->
            item{
                StatRow(key = stat.key, value = stat.value)
            }
        }
    }
}

fun DrawScope.drawStatBorder(
    topbotSecondaryH : Float = 1f,
    topbotPrimaryH : Float = 5f,
    topbotPrimaryWPerc : Float = 0.1f,
    topbotGap : Float = 4f,
    topbotSTotalWidthPerc : Float = 0.8f,
    topbotSBreakPerc : Float = 0.2f,
    sideWidth : Float = 5f,
    sideCThickness : Float = 7f,
    cornerPerc : Float = 0.15f,
    borderPrimary : Color = Color.White,
    borderSecondary : Color = Color.White.copy(alpha = 0.5f),
    backgroundColor: Color = Color.Black.copy(alpha = 0.25f)
) {
    val topbotPrimaryW = size.width * topbotPrimaryWPerc
    val topbotSTotalWidth = size.width * topbotSTotalWidthPerc
    val topbotSBreak = size.width * topbotSBreakPerc
    val cornerCut = if (size.width < size.height) size.width * cornerPerc else size.height * cornerPerc
    val clipBuffer = 2f

    val topPrimary = Path()
    topPrimary.moveTo(cornerCut - sideCThickness / 2f, 0f)
    topPrimary.relativeLineTo(topbotPrimaryW,0f)
    topPrimary.relativeLineTo(topbotPrimaryH, topbotPrimaryH)
    topPrimary.relativeLineTo(-topbotPrimaryW - topbotPrimaryH * 2f - topbotGap, 0f)
    topPrimary.relativeLineTo(0f, -topbotPrimaryH)
    topPrimary.close()


    val topPClip = Path()
    topPClip.moveTo((cornerCut - sideCThickness / 2f) - topbotGap - topbotPrimaryH - clipBuffer, 0f)
    topPClip.relativeLineTo(sideCThickness + clipBuffer + topbotGap + topbotPrimaryH, 0f)
    topPClip.relativeLineTo(-topbotPrimaryH + topbotSecondaryH,topbotPrimaryH - topbotSecondaryH)
    topPClip.relativeLineTo(-sideCThickness - clipBuffer - topbotGap - topbotPrimaryH, 0f)
    topPClip.close()


    val topSecondary = Path()
    topSecondary.moveTo(
        (cornerCut + sideCThickness / 2f) - (topbotPrimaryH + topbotGap),
        (topbotPrimaryH + topbotGap)
    )
    topSecondary.relativeLineTo(topbotSTotalWidth, 0f)
    topSecondary.relativeLineTo(0f, topbotSecondaryH)
    topSecondary.relativeLineTo(-topbotSTotalWidth - topbotSecondaryH, 0f)
    topSecondary.close()

    val topSClip = Path()
    topSClip.moveTo(cornerCut + topbotPrimaryW, (topbotGap + topbotPrimaryH) - clipBuffer)
    topSClip.relativeLineTo(topbotSBreak, 0f)
    topSClip.relativeLineTo(0f, clipBuffer * 2 + topbotSecondaryH)
    topSClip.relativeLineTo(-topbotSBreak, 0f)
    topSClip.close()

    val leftSide = Path()
    leftSide.moveTo(0f, cornerCut - sideCThickness / 2f)
    leftSide.lineTo(
        (cornerCut - sideCThickness / 2f) - (topbotPrimaryH + topbotGap),
        (topbotPrimaryH + topbotGap)
    )
    leftSide.lineTo(
        (cornerCut + sideCThickness / 2f) - (topbotPrimaryH + topbotGap),
        (topbotPrimaryH + topbotGap)
    )
    leftSide.lineTo(0f, cornerCut + sideCThickness / 2f)
    leftSide.close()

    leftSide.moveTo(0f, cornerCut - sideCThickness / 2f)
    leftSide.lineTo(sideWidth, cornerCut - sideCThickness / 2f)
    leftSide.lineTo(sideWidth, size.height - topbotGap - topbotPrimaryH - topbotSecondaryH)
    leftSide.lineTo(0f, size.height - topbotGap - topbotPrimaryH - topbotSecondaryH)
    leftSide.close()

    val botPrimary = Path()
    botPrimary.moveTo(size.width - cornerCut + sideCThickness / 2f, size.height)
    botPrimary.relativeLineTo(-topbotPrimaryW,0f)
    botPrimary.relativeLineTo(-topbotPrimaryH, -topbotPrimaryH)
    botPrimary.relativeLineTo(topbotPrimaryW + topbotPrimaryH * 2f + topbotGap, 0f)
    botPrimary.relativeLineTo(0f, topbotPrimaryH)
    botPrimary.close()

    val botPClip = Path()
    botPClip.moveTo(size.width - cornerCut + sideCThickness / 2f + clipBuffer + topbotPrimaryH + topbotGap, size.height)
    botPClip.relativeLineTo(-sideCThickness - clipBuffer - topbotGap - topbotPrimaryH, 0f)
    botPClip.relativeLineTo(topbotPrimaryH - topbotSecondaryH,-topbotPrimaryH + topbotSecondaryH)
    botPClip.relativeLineTo(sideCThickness + clipBuffer + topbotGap + topbotPrimaryH, 0f)
    botPClip.relativeLineTo(0f, topbotPrimaryH)
    botPClip.close()

    val botSecondary = Path()
    botSecondary.moveTo(
        size.width - (cornerCut + sideCThickness / 2f) + (topbotPrimaryH + topbotGap),
        size.height - (topbotPrimaryH + topbotGap)
    )
    botSecondary.relativeLineTo(-topbotSTotalWidth, 0f)
    botSecondary.relativeLineTo(0f, -topbotSecondaryH)
    botSecondary.relativeLineTo(topbotSTotalWidth + topbotSecondaryH, 0f)
    botSecondary.close()

    val botSClip = Path()
    botSClip.moveTo(size.width - (cornerCut + topbotPrimaryW), size.height - (topbotGap + topbotPrimaryH) + clipBuffer)
    botSClip.relativeLineTo(-topbotSBreak, 0f)
    botSClip.relativeLineTo(0f, -clipBuffer * 2 - topbotSecondaryH)
    botSClip.relativeLineTo(topbotSBreak, 0f)
    botSClip.close()

    val rightSide = Path()
    rightSide.moveTo(size.width, size.height - (cornerCut - sideCThickness / 2f))
    rightSide.lineTo(
        size.width - (cornerCut - sideCThickness / 2f) + (topbotPrimaryH + topbotGap),
        size.height - (topbotPrimaryH + topbotGap)
    )
    rightSide.lineTo(
        size.width - (cornerCut + sideCThickness / 2f) + (topbotPrimaryH + topbotGap),
        size.height - (topbotPrimaryH + topbotGap)
    )
    rightSide.lineTo(size.width,  size.height - (cornerCut + sideCThickness / 2f))
    rightSide.close()

    rightSide.moveTo(size.width, size.height - (cornerCut - sideCThickness / 2f))
    rightSide.lineTo(size.width - sideWidth, size.height - (cornerCut - sideCThickness / 2f))
    rightSide.lineTo(size.width - sideWidth, topbotGap + topbotPrimaryH + topbotSecondaryH)
    rightSide.lineTo(size.width, topbotGap + topbotPrimaryH + topbotSecondaryH)
    rightSide.close()

    drawBackground(
        topLeftCornerC =  cornerCut,
        topRightCornerC = 0f,
        botRightCornerC = cornerCut,
        botLeftCornerC = 0f,
        topbotOffset = (topbotGap + topbotPrimaryH + topbotSecondaryH),
        sideOffset = sideWidth,
        color = backgroundColor
    )
    drawPath(
        path = rightSide,
        color = borderPrimary
    )
    drawPath(
        path = leftSide,
        color = borderPrimary,
    )
    clipPath(
        path = topSClip,
        clipOp = ClipOp.Difference,
        block = {
            drawPath(
                path = topSecondary,
                color = borderSecondary
            )

        }
    )
    clipPath(
        path = topPClip,
        clipOp = ClipOp.Difference,
        block = {
            drawPath(
                path = topPrimary,
                color = borderPrimary
            )

        }
    )
    clipPath(
        path = botPClip,
        clipOp = ClipOp.Difference,
        block = {
            drawPath(
                path = botPrimary,
                color = borderPrimary
            )

        }
    )
    clipPath(
        path = botSClip,
        clipOp = ClipOp.Difference,
        block = {
            drawPath(
                path = botSecondary,
                color = borderSecondary
            )

        }
    )
}

fun DrawScope.drawBackground(
    topLeftCornerC : Float,
    topRightCornerC : Float,
    botLeftCornerC : Float,
    botRightCornerC : Float,
    topOffset : Float,
    botOffset : Float,
    leftOffset : Float,
    rightOffset : Float,
    color : Color = Color.Black.copy(alpha = 0.25f)
) {
    val background = Path()
    background.moveTo(leftOffset, topOffset)
    background.lineTo(size.width - rightOffset, topOffset)
    background.lineTo(size.width - rightOffset, size.height - (botOffset))
    background.lineTo(leftOffset, size.height - (botOffset))
    background.close()

    val backgroundClip = Path()
    backgroundClip.moveTo(0f, 0f)
    backgroundClip.lineTo(0f, topLeftCornerC)
    backgroundClip.lineTo(topLeftCornerC, 0f)
    backgroundClip.close()

    backgroundClip.moveTo(size.width, 0f)
    backgroundClip.lineTo(size.width - topRightCornerC, 0f)
    backgroundClip.lineTo(size.width, topRightCornerC)
    backgroundClip.close()

    backgroundClip.moveTo(size.width,size.height)
    backgroundClip.lineTo(size.width - botRightCornerC, size.height)
    backgroundClip.lineTo(size.width, size.height - botRightCornerC)
    backgroundClip.close()

    backgroundClip.moveTo(0f, size.height)
    backgroundClip.lineTo(botLeftCornerC, size.height)
    backgroundClip.lineTo(0f, size.height - botLeftCornerC)
    backgroundClip.close()

    clipPath(
        path = backgroundClip,
        clipOp = ClipOp.Difference,
        block = {
            drawPath(
                path = background,
                color = color
            )
        }
    )
}

fun DrawScope.drawBackground(
    topLeftCornerC : Float,
    topRightCornerC : Float,
    botLeftCornerC : Float,
    botRightCornerC : Float,
    topbotOffset : Float,
    sideOffset : Float,
    color : Color = Color.Black.copy(alpha = 0.25f)
) {
    drawBackground(
        topLeftCornerC,
        topRightCornerC,
        botLeftCornerC,
        botRightCornerC,
        topbotOffset,
        topbotOffset,
        sideOffset,
        sideOffset,
        color = color
    )
}

@Preview(device = Devices.AUTOMOTIVE_1024p)
@Composable
private fun StatBoxPreview() {
    MaterialTheme {
        val stats = HashMap<String, String>()
        stats["MILE PACE"] = "00:12:37"
        stats["RECOVERY HR"] = "160 BPM"
        stats["5K PACE"] = "00:38:19"
        stats["MILE"] = "00:12:37"
        stats["HR"] = "160 BPM"
        stats["5K"] = "00:38:19"
        stats["PACE"] = "00:12:37"
        stats["RECOVERYHR"] = "160 BPM"
        stats["5KPACE"] = "00:38:19"
        stats["MIPACE"] = "00:12:37"
        stats["REC HR"] = "160 BPM"
        stats["5K PACE"] = "00:38:19"
        stats["MILE PACE"] = "00:12:37"
        stats["RECOVERY HR"] = "160 BPM"
        stats["5K PAE"] = "00:38:19"
        stats["MILE PACE"] = "00:12:37"
        stats["RECOVERY HR"] = "160 BPM"
        stats["5K ACE"] = "00:38:19"
        stats["MILE PACE"] = "00:12:37"
        stats["RCOVERY HR"] = "160 BPM"
        stats["5K PACE"] = "00:38:19"
        stats["MILE PCE"] = "00:12:37"
        stats["REOVERY HR"] = "160 BPM"
        stats["K PACE"] = "00:38:19"
        StatBoxWidget() {
            StatBoxContent(stats = stats)
        }
    }
}

