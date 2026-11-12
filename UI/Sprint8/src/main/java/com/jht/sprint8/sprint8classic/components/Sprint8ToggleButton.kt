package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_TEXT_FIELD_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_DISABLED_BTN_COLOR
import com.jht.sprint8.utils.Sprint8Colors.TOGGLE_BUTTON_BACKGROUND_COLOR
import com.jht.sprint8.utils.Sprint8TypeData
import com.jht.sprint8.utils.WorkoutMode
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.dpToPx

/**
 * Displays a toggle button that allows the user to switch between
 * Sprint8 Plus and Sprint8 Classic modes.
 * This composable shows the current mode and updates its state when toggled.
 */
@OptIn(ExperimentalComposeUiApi::class)
@AllDevicePreviews
@Composable
internal fun Sprint8ToggleButton() {
    val selectedMode = remember { mutableStateOf(Sprint8TypeData.selectedMode) }
    val newText = stringResource(R.string.immersive_tc)
    val classicText = stringResource(R.string.classic_tc)
    val selectedColor = CUSTOM_WORKOUT_TEXT_FIELD_BORDER_COLOR
    val unselectedColor = SPRINT8_DISABLED_BTN_COLOR
    val stroke = DynamicTheme.borders.pillbox.width.value
    val cornerRadius = 12.dp.dpToPx()
    val baseTag = "Sprint8ToggleButton"
    fun updateSelectedMode(mode: WorkoutMode) {
        Sprint8TypeData.selectedMode = mode
        selectedMode.value = mode
    }
    Column(
        modifier = Modifier
            .wrapContentSize()
            .semantics {
                testTagsAsResourceId = true
            }
    ) {
        Text(
            modifier = Modifier
                .padding(
                    horizontal = 12.dp.computeSize(),
                    vertical = 6.dp.computeSize()
                )
                .testTag("$baseTag:Label:Text"),
            text = stringResource(R.string.version_tc),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 12.sp.computeSize(),
                color = DynamicTheme.colors.text1
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(0.48f)
                .padding(start = 8.dp.computeSize())
                .height(50.dp.computeSize())
                .clip(RoundedCornerShape(cornerRadius)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clickable { updateSelectedMode(WorkoutMode.Immersive) }
                    .background(TOGGLE_BUTTON_BACKGROUND_COLOR)
                    .fillMaxHeight()
                    .weight(1f)
                    .drawBehind {
                        val borderColor =
                            if (selectedMode.value == WorkoutMode.Immersive) selectedColor else unselectedColor
                        drawArc(
                            color = borderColor,
                            startAngle = 180f,
                            sweepAngle = 90f,
                            useCenter = false,
                            size = Size(cornerRadius * 2, cornerRadius * 2),
                            topLeft = Offset(0f, 0f),
                            style = Stroke(width = stroke)
                        )
                        drawArc(
                            color = borderColor,
                            startAngle = 90f,
                            sweepAngle = 90f,
                            useCenter = false,
                            size = Size(cornerRadius * 2, cornerRadius * 2),
                            topLeft = Offset(0f, size.height - cornerRadius * 2),
                            style = Stroke(width = stroke)
                        )
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = stroke
                        )
                        drawLine(
                            color = selectedColor,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = stroke
                        )
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = stroke
                        )

                        drawLine(
                            color = borderColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = stroke
                        )
                    }
                    .testTag("CustomToggleButton:$newText"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = newText,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 18.sp.computeSize(),
                        color = if (selectedMode.value == WorkoutMode.Immersive) selectedColor else unselectedColor
                    ),
                    modifier = Modifier.testTag("$baseTag:Label:Text"),
                )
            }

            Box(
                modifier = Modifier
                    .clickable { updateSelectedMode(WorkoutMode.Classic) }
                    .background(TOGGLE_BUTTON_BACKGROUND_COLOR)
                    .fillMaxHeight()
                    .weight(1f)
                    .drawBehind {
                        val borderColor =
                            if (selectedMode.value == WorkoutMode.Classic) selectedColor else unselectedColor
                        drawArc(
                            color = borderColor,
                            startAngle = 270f,
                            sweepAngle = 90f,
                            useCenter = false,
                            size = Size(cornerRadius * 2, cornerRadius * 2),
                            topLeft = Offset(size.width - cornerRadius * 2, 0f),
                            style = Stroke(width = stroke)
                        )
                        drawArc(
                            color = borderColor,
                            startAngle = 0f,
                            sweepAngle = 90f,
                            useCenter = false,
                            size = Size(cornerRadius * 2, cornerRadius * 2),
                            topLeft = Offset(
                                size.width - cornerRadius * 2,
                                size.height - cornerRadius * 2
                            ),
                            style = Stroke(width = stroke)
                        )

                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = stroke
                        )

                        drawLine(
                            color = borderColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = stroke
                        )
                        drawLine(
                            color = borderColor,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = stroke
                        )
                    }
                    .testTag("CustomToggleButton:$classicText"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = classicText,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 18.sp.computeSize(),
                        color = if (selectedMode.value == WorkoutMode.Classic) selectedColor else unselectedColor
                    ),
                    modifier = Modifier.testTag("$baseTag:Label:Text"),
                )
            }
        }
    }
}



