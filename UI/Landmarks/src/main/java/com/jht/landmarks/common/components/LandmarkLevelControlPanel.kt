package com.jht.landmarks.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.landmarks.R
import com.jht.landmarks.common.utils.scaledDp
import com.jht.theme.disabledBackground
import com.jht.ui.clickableNoEffect
import com.jht.widgets.NumPadEntry
import com.jht.widgets.PopupFullscreen

import com.jht.landmarks.common.utils.Colors
import com.jht.ui.computeSize

@Composable
internal fun LevelControlPanel(
    level: Int,
    maxLevel: Int,
    onLevelUp: () -> Unit,
    onLevelDown: () -> Unit,
    onLevelSet: (Int) -> Unit
) {
    val componentTag = "LevelControlPanel"
    val cornerShape = RoundedCornerShape(12.scaledDp)

    var showKeypad by remember { mutableStateOf(false) }
    var buttonPosition by remember { mutableStateOf(DpOffset.Zero) }
    var buttonSize by remember { mutableStateOf(DpSize.Zero) }

    val upInteractionSource = remember { MutableInteractionSource() }
    val isUpPressed by upInteractionSource.collectIsPressedAsState()
    var upChangesCount by remember { mutableIntStateOf(0) }
    val downInteractionSource = remember { MutableInteractionSource() }
    val isDownPressed by downInteractionSource.collectIsPressedAsState()
    var downChangesCount by remember { mutableIntStateOf(0) }

    // If the component is removed or `isUpPressed/ isDownPressed` becomes false, the Coroutine is immediately cancelled, preventing memory leaks and runaway timers.
    // Behavior matches the legacy StatIncrementer.java
    LaunchedEffect(isUpPressed) {
        if (isUpPressed) {
            onLevelUp()
            upChangesCount = 1
            delay(900)
            while (upChangesCount < 3) {
                delay(100)
                onLevelUp()
                upChangesCount++
            }
        } else {
            upChangesCount = 0 // Reset when finger lifted
        }
    }
    LaunchedEffect(isDownPressed) {
        if (isDownPressed) {
            if (level > 1) {
                onLevelDown()
                downChangesCount = 1
                delay(900)
                while (true) {
                    delay(100)
                    onLevelDown()
                    downChangesCount++
                }
            }
        } else {
            downChangesCount = 0
        }
    }

    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .width(140.scaledDp)
            .height(388.scaledDp)
            .clip(cornerShape)
            .background(Colors.PanelBackgroundColor)
            .border(1.scaledDp, Colors.PanelBorderColor, cornerShape)
            .testTag("$componentTag:Container:Scrollable"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { coordinates ->
                    with(density) {
                        val sizePixels = coordinates.size
                        buttonSize = DpSize(sizePixels.width.toDp(), sizePixels.height.toDp())
                        val offsetPixels = coordinates.positionInRoot()
                        buttonPosition = DpOffset(offsetPixels.x.toDp(), offsetPixels.y.toDp())
                    }
                }
                .clickable { showKeypad = true }
                .testTag("$componentTag:KeypadEntry:Button")
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_keypad_black),
                contentDescription = "Keypad Entry",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.scaledDp, start = 8.scaledDp)
                    .size(12.scaledDp)
                    .testTag("$componentTag:KeypadIcon:Image")
            )

            Column(
                modifier = Modifier.align(Alignment.Center)
                    .testTag("$componentTag:LevelDisplay:Scrollable"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.level_tc),
                    style = MaterialTheme.typography.bodySmall.copy(),
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("$componentTag:LevelLabel:Text")
                )
                Text(
                    text = level.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(),
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontSize = 42.sp,
                    modifier = Modifier.testTag("$componentTag:LevelValue:Text")
                )
            }
        }

        HorizontalDivider(color = Colors.PanelBorderColor, thickness = 1.scaledDp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(
                    interactionSource = upInteractionSource,
                    indication = LocalIndication.current, // pass the default UI effect
                    onClick = { /* Handled by LaunchedEffect on first press */ }
                )
            .testTag("$componentTag:LevelIncrease:Button"),
        contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_up),
                contentDescription = "Increase Level",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(48.scaledDp)
                    .testTag("$componentTag:LevelIncreaseIcon:Image")
            )
        }

        HorizontalDivider(color = Colors.PanelBorderColor, thickness = 1.scaledDp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(
                    interactionSource = downInteractionSource,
                    indication = LocalIndication.current,
                    onClick = { /* Handled by LaunchedEffect on first press */ }
                )
                .testTag("$componentTag:LevelDecrease:Button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = "Decrease Level",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(48.scaledDp)
                    .testTag("$componentTag:LevelDecreaseIcon:Image")
            )
        }
    }

    if (showKeypad) {
        PopupFullscreen(onDismissRequest = { showKeypad = false }) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(disabledBackground)
                    .clickableNoEffect(onClick = { showKeypad = false })
                    .testTag("$componentTag:KeypadOverlay:Scrollable")
            ) {
                val keypadWidth = LocalConfiguration.current.screenWidthDp.dp / 2
                val keypadX = buttonPosition.x - keypadWidth

                Box(
                    Modifier
                        .absoluteOffset(keypadX, 0.dp)
                        .fillMaxHeight()
                        .width(keypadWidth)
                        .testTag("$componentTag:KeypadContainer:Scrollable")
                ) {
                    NumPadEntry(
                        numPadValue = level.toFloat(),
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .testTag("$componentTag:NumPadEntry:Scrollable"),
                        decimalPlaces = 0,
                        min = 1f,
                        max = maxLevel.toFloat(),
                        stepValue = 1f,
                        forceOnlyStepValues = true,
                        usePercentageBar = true,
                        unitLabel = stringResource(id = R.string.level_tc),
                        onDismiss = { showKeypad = false },
                        onValueCommit = { value ->
                            onLevelSet(value.toInt())
                            showKeypad = false
                        }
                    )
                }
            }
        }
    }
}