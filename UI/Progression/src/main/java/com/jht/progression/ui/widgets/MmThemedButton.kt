package com.jht.progression.ui.widgets

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.progression.R
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.MmColors
import com.jht.ui.clickableNoEffect
import com.jht.widgets.AutoSizeText
import com.jht.widgets.scaleSize

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun  MmThemedButton(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: TextStyle = oxaniumStyle.copy(fontWeight = FontWeight.Bold,  textAlign = TextAlign.Center),
    color: Color = Color.White,
    secondaryColor: Color = Color.Black,
    onPressColor: Color = MmColors.deepBlue,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    requireLongPress: Boolean = false,
    longPressSec: Float = 1.5f,
    horizontalTextPadding: Dp = 20.dp.scaleSize(),
    verticalTextPadding: Dp = 25.dp.scaleSize(),
    debounceTime: Int = 1000,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    iconSpacing: Dp = 8.dp.scaleSize(),
    fitBackgroundToBounds: Boolean = false,
    onClick: () -> Unit,
) {
    // Used to prevent multiple invocations of longPress lambda for a single Press
    val longPressFinished = remember { mutableStateOf(false)}
    val isLongPressed = remember { mutableStateOf(false) }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface (
        modifier = modifier
            .height(100.dp.scaleSize())
            .width(250.dp.scaleSize())
            .clickableNoEffect(
                interactionSource = interactionSource,
                enabled = enabled,
                debounceTimeMs = debounceTime
            ) {
                if (!requireLongPress && isPressed) onClick()
            }
            .animateContentSize(
                animationSpec = tween(durationMillis = 100)
            ),
        color = Color.Transparent,
    ) {
        //Float used to know how far into the longPress the user is. Used to animate bars filling
        val temp: Float by animateFloatAsState(
            targetValue = if(isLongPressed.value) 1f else 0f,
            animationSpec = if(isLongPressed.value) tween((longPressSec * 1000).toInt()) else tween(0),
            label = ""
        )
        if(requireLongPress && interactionSource.collectIsPressedAsState().value) {
            isLongPressed.value = true

            //If temp = 1 that means the user has held the button for the full length of the longPress
            if (temp == 1f && !longPressFinished.value) {
                longPressFinished.value = true
                onClick()
            }
        } else {
            isLongPressed.value = false
            longPressFinished.value = false
        }
        Layout(
            content = {
                val backgroundColor = if (isPressed) color else secondaryColor
                Image(painter = painterResource(id = R.drawable.progression_button_boarder),
                    contentDescription = "Progression Button Boarder",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                        .then(if (fitBackgroundToBounds) Modifier.graphicsLayer {
                            scaleX = 1.0837f
                            scaleY = 1.2400f
                            transformOrigin = TransformOrigin.Center
                        } else Modifier),
                    colorFilter = ColorFilter.tint(color)
                )
                Image(painter = painterResource(id = R.drawable.progression_button_background),
                    contentDescription = "Progression Button Background",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                        .then(if (fitBackgroundToBounds) Modifier.graphicsLayer {
                            scaleX = 1.0837f
                            scaleY = 1.2400f
                            transformOrigin = TransformOrigin.Center
                        } else Modifier),
                    colorFilter = ColorFilter.tint(backgroundColor)
                )
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = horizontalTextPadding, vertical = verticalTextPadding)
                    ) {
                        leadingIcon?.let { icon ->
                            CompositionLocalProvider(
                                LocalContentColor provides (if (isPressed) onPressColor else color)
                            ) {
                                icon()
                            }
                            Spacer(modifier = Modifier.width(iconSpacing))
                        }

                        AutoSizeText(
                            text,
                            color = if (isPressed) onPressColor else color,
                            overflow = TextOverflow.Clip,
                            softWrap = false,
                            style = textStyle,
                            maxFontSize = 35.sp.scaleSize()
                        )
                    }

                }
                androidx.compose.foundation.Canvas(modifier = Modifier) {
                    drawLongPressAnimation(color = color)
                }
            }
        ) {measurables, constraints ->
            val imageBoarderPlaceable = measurables[0].measure(constraints)
            val imageBackgroundPlaceable = measurables[1].measure(constraints)
            val textPlaceable = measurables[2].measure(constraints)

            val longPressWidth = ((imageBackgroundPlaceable.width * 0.785f) * temp).toInt()
            val longPressHeight = (imageBackgroundPlaceable.height * 0.79f).toInt()
            val longPressConstraints = constraints.copy(
                minHeight = longPressHeight,
                maxHeight = longPressHeight,
                minWidth = longPressWidth,
                maxWidth = longPressWidth
            )
            val longPressAnimation = measurables[3].measure(longPressConstraints)
            layout(width = imageBoarderPlaceable.width, height = imageBoarderPlaceable.height) {
                imageBackgroundPlaceable.placeRelative(0,0)
                imageBoarderPlaceable.placeRelative(0,0)
                longPressAnimation.placeRelative((imageBoarderPlaceable.width * 0.11f).toInt(),(imageBoarderPlaceable.height * 0.11f).toInt())
                textPlaceable.placeRelative((imageBoarderPlaceable.width - textPlaceable.width) / 2, (imageBoarderPlaceable.height - textPlaceable.height) / 2)
            }
        }
    }
}

fun DrawScope.drawLongPressAnimation(
    color: Color
) {
    val barHPer = 0.02f

    val  bars = Path()
    bars.moveTo(0f, 0f)
    bars.lineTo(size.width, 0f)
    bars.moveTo(0f, size.height)
    bars.lineTo(size.width, size.height)

    drawPath(bars, color, style = Stroke(width = size.height * barHPer))
}

@Preview(device = Devices.AUTOMOTIVE_1024p)
@Composable
fun LongPressTesting() {
    MaterialTheme {
        MmThemedButton(text = "Long Press", onClick = { /*TODO*/ }, color = MmColors.futuristicBlue)
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p)
@Composable
fun SBoxPreviewSmall() {
    MaterialTheme {
        var state by remember {
            mutableStateOf(false)
        }
        Surface (
            color = Color.Transparent,
        ) {
            MmThemedButton(
                text = "NEXT",
                verticalTextPadding = 10.dp.scaleSize(),
                onClick = {
                    state = !state
                },
                modifier = Modifier
                    .width(100.dp)
                    .height(50.dp)
            )
        }
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p)
@Composable
fun SBoxWidgetPreview_Unselected() {
    MmThemedButton(
        text = "NEXT",
        onClick = {},
    )
}

@Preview(device = Devices.AUTOMOTIVE_1024p)
@Composable
fun SBoxWidgetPreview_UnselectedFillBounds() {
    MmThemedButton(
        text = "NEXT",
        onClick = {},
        verticalTextPadding = 10.dp.scaleSize(),
        modifier = Modifier
            .width(500.dp)
            .height(50.dp),
    )
}

@Preview(device = Devices.AUTOMOTIVE_1024p)
@Composable
fun SBoxWidgetPreview_Selected() {
    MmThemedButton(
        text = "NEXT",
        onClick = {},
        color = MmColors.futuristicBlue
    )
}