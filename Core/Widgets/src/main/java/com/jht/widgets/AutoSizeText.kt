package com.jht.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.ui.Device22inPreviews

private val defaultMaxFontSize = 100.sp
private const val defaultScaleDown = 0.9f


@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxFontSize: TextUnit = defaultMaxFontSize,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true, // note: when maxLines == 1, we force this to false for more predictable fitting
    maxLines: Int = 1, // Normal Text() defaults maxLines = Int.MAX_VALUE, but most auto size uses are one line and that is more efficient
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    suggestedFontSizes: List<TextUnit> = emptyList(),
    testTag: String = "",
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = when(textAlign) {
            TextAlign.Start, TextAlign.Left -> Alignment.CenterStart
            TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
            else -> Alignment.Center
        }
    ) {
        val textMeasurer = rememberTextMeasurer()
        if (maxLines == 1) {
            // this method seems fast and reliable, but multiline messes with it
            // chameleon training tab pillboxes do not size right with multi line on compose 1.6 for as yet unknown reason
            val combinedTextStyle = (LocalTextStyle.current + style).copy(fontSize = maxFontSize)
            val rawLayout = textMeasurer.measure(text, combinedTextStyle, overflow, false, maxLines, constraints)
            val heightRatio = if (rawLayout.multiParagraph.height == 0f) 1f else rawLayout.size.height / rawLayout.multiParagraph.height
            val widthRatio = if (rawLayout.multiParagraph.width == 0f) 1f else rawLayout.size.width / rawLayout.multiParagraph.width
            val minRatio = minOf(heightRatio, widthRatio, 1f)
            val fitFontSize = maxFontSize * minRatio
            Text(
                text = text,
                modifier = Modifier.testTag(testTag),
                color = color,
                fontSize = fitFontSize,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineHeight = lineHeight,
                overflow = overflow,
                softWrap = false,
                maxLines = maxLines,
                minLines = 1,
                onTextLayout = onTextLayout,
                style = style
            )
        } else {
            //Set font size close to the max size
            var combinedTextStyle = (LocalTextStyle.current + style).copy(
                fontSize = minOf(minOf(maxWidth, maxHeight).value, maxFontSize.value).toInt().sp
            )
            val fontSizes: List<TextUnit> =
                if (suggestedFontSizes.isEmpty()) {
                    //Generate list of default font sizes starting at max size
                    MutableList(combinedTextStyle.fontSize.value.toInt()) {
                        (combinedTextStyle.fontSize.value - it).sp
                    }
                } else {
                    suggestedFontSizes.sortedByDescending { it.value }
                }
            //Decreases font size until it fits
            for (currentFontIndex in fontSizes.indices) {
                val rawLayout = textMeasurer.measure(text, combinedTextStyle, overflow, softWrap, maxLines, constraints)
                if (!rawLayout.hasVisualOverflow) break
                combinedTextStyle = combinedTextStyle.copy(fontSize = fontSizes[currentFontIndex]) // we don't technically check the last one, but fallback to it
            }

            Text(
                text = text,
                modifier = Modifier.testTag(testTag),
                color = color,
                fontSize = TextUnit.Unspecified,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineHeight = lineHeight,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = 1,
                onTextLayout = onTextLayout,
                style = combinedTextStyle
            )
        }
    }
}
@Composable
@Device22inPreviews
private fun PreviewAutoSizeText() {
    val widths = 2..6
    val heights = 2..6
    Column(Modifier.background(Color.Gray)) {
        // various fixed sizes with basic one line text
        PreviewVaryingSizes(widths, heights) { AutoSizeText("Test string", maxLines = 1) }
        PreviewVaryingSizes(widths, heights) { AutoSizeText("Test string in a longer form to fit", maxLines = Int.MAX_VALUE) }
        // testing wrapping implicit
        heights.forEach {
            AutoSizeText("Test string $it as a long string for testing the things here! Hurray!",
                Modifier
                    .fillMaxHeight(1f / it)
                    .background(Color.Yellow), softWrap = true)
        }
        // testing wrapping explicit
        AutoSizeText("Test\nstring",
            Modifier
                .size(60.dp, 20.dp)
                .background(Color.Magenta))
    }
}
@Composable
private fun PreviewVaryingSizes(widths: Iterable<Int>, heights: Iterable<Int>, content: @Composable ()-> Unit) {
    Column {
        widths.forEach { w ->
            val width = 20.dp * w
            Row {
                heights.forEach { h ->
                    val height = 10.dp * h
                    Box(
                        Modifier.size(width, height).background(Color.Cyan).border(1.dp, Color.Blue),
                        contentAlignment = Alignment.Center,
                    ) {
                        content()
                    }
                }
            }
        }
    }
}


//region AutoSizeText from simple app
sealed class AutoSizeConstraint(open val min: TextUnit = TextUnit.Unspecified) {
    data class Width(override val min: TextUnit = TextUnit.Unspecified) : AutoSizeConstraint(min)
    data class Height(override val min: TextUnit = TextUnit.Unspecified) : AutoSizeConstraint(min)
    data class Both(override val min: TextUnit = TextUnit.Unspecified) : AutoSizeConstraint(min)
}
@Composable
fun AutoSizeTextConstraint(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = defaultMaxFontSize,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    constraint: AutoSizeConstraint = AutoSizeConstraint.Both(),
) {
    var fontAutoSize by remember { mutableStateOf(fontSize) } // should this be a parameter so caller can pass a shared state for matching font sizes?
    var readyToDraw  by remember { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        color = color,
        fontSize = fontAutoSize,
        fontStyle = null,
        fontWeight = null,
        fontFamily = null,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = TextOverflow.Clip,
        maxLines = 1,
        softWrap = false,
        style = style,
        onTextLayout = {
            // reduce size until specified min is reached or text no longer overflow
            fun constrain(downScale: Float) {
                val reducedSize = fontAutoSize * downScale
                if (constraint.min != TextUnit.Unspecified && reducedSize <= constraint.min) {
                    fontAutoSize = constraint.min
                    readyToDraw = true
                } else {
                    fontAutoSize = reducedSize
                }
            }

            // when overflow, multiParagraph is larger than size, compute ratio to get multiParagraph into size
            val heightRatio = minOf(defaultScaleDown, if (it.multiParagraph.height == 0f) 1f else it.size.height / it.multiParagraph.height)
            val widthRatio = minOf(defaultScaleDown, if (it.multiParagraph.width == 0f) 1f else it.size.width / it.multiParagraph.width)
            val minRatio = minOf(heightRatio, widthRatio)

            // perform the constrain based on height or width
            when (constraint) {
                is AutoSizeConstraint.Height -> {
                    if (it.didOverflowHeight) {
                        readyToDraw = false
                        constrain(heightRatio)
                    } else {
                        readyToDraw = true
                    }
                }
                is AutoSizeConstraint.Width -> {
                    if (it.didOverflowWidth) {
                        readyToDraw = false
                        constrain(widthRatio)
                    } else {
                        readyToDraw = true
                    }
                }
                is AutoSizeConstraint.Both -> {
                    if (it.didOverflowWidth || it.didOverflowHeight) {
                        readyToDraw = false
                        constrain(minRatio)
                    } else {
                        readyToDraw = true
                    }
                }
            }
        }
    )
}
@Composable
fun FixedHeightAutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    constraint: AutoSizeConstraint = AutoSizeConstraint.Width(),
) {
    var height by remember { mutableIntStateOf(0) }

    // require an extra Box to contain the auto size text so the height does not shrink as the text shrinks
    // Note: if there is a better way to do this, refactor this
    Box(
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .layout { measurable, constraints ->
                // the height is saved so that the box does not resize when the text shrinks
                val placeable = measurable.measure(constraints)
                if (height == 0) {
                    height = placeable.height
                }

                layout(placeable.width, height) {
                    placeable.place(0, 0)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AutoSizeTextConstraint(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight,
            style = style,
            constraint = constraint
        )
    }
}
//endregion AutoSizeText from simple app



/** Provides a version of [Text] with an outline stroke that can be a different color than the fill color */
@Composable
fun OutlinedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    outline: OutlinedTextProperties? = null, // default is no outline, just original text
) {
    Box(
        modifier,
    ) {
        // outline text
        outline?.let {
            val outlineStyle = style.copy(
                drawStyle = outline.stroke,
            )
            Text(
                text = text,
                modifier = Modifier,
                color = outline.color,
                fontSize = fontSize,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineHeight = lineHeight,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                onTextLayout = onTextLayout,
                style = outlineStyle
            )
        }
        // normal filled text
        Text(
            text = text,
            modifier = Modifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = style
        )
    }
}

class OutlinedTextProperties(
    val color: Color,
    val stroke: Stroke,
) {
    constructor(color: Color, widthPx: Float = Stroke.HairlineWidth) : this(color, Stroke(widthPx, join = StrokeJoin.Round))
    // can you make a static fun composable to take Dp ?
}
