package com.jht.customhiit.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.customhiit.R
import com.jht.customhiit.common.models.CustomHIITInterval
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.unit.UnitFormatter
import com.jht.unit.kphUnit
import java.util.Locale

@Composable
internal fun IntervalCard(
    modifier: Modifier,
    availableParameters: Map<String, Float>,
    intervalType: CustomHIITInterval,
    selectedInterval: Int,
    timeElapsed: Int = 0,
    isMetric: Boolean = true,
    testTag: String,
    onClick: () -> Unit = {},
) {

    val isPressed = remember {
        mutableStateOf(false)
    }

    val isToggled = intervalType.ordinal == selectedInterval

    val time = Math.round(timeElapsed / 1000f)

    val parameterIcons = mutableSetOf<Int>()
    if (availableParameters.containsKey(ConsoleWorkoutVariables.START_RESISTANCE)) {
        parameterIcons.add(R.drawable.ic_resistance)
    }
    if (availableParameters.containsKey(ConsoleWorkoutVariables.START_SPEED)) {
        parameterIcons.add(R.drawable.ic_speed)
    }
    if (availableParameters.containsKey(ConsoleWorkoutVariables.START_INCLINE)) {
        parameterIcons.add(R.drawable.ic_incline)
    }

    Row(
        modifier = modifier
            .clickable {
                onClick()
            }
            .clip(shape = RoundedCornerShape(10.dp.computeSize()))
            .background(if (isToggled) intervalType.intervalColor else Color.Transparent)
            .border(
                BorderStroke(5.dp.computeSize(), intervalType.intervalColor),
                shape = RoundedCornerShape(10.dp.computeSize())
            )
            .testTag(testTag)
    ) {

        val simpleNameFromWorkoutParameter = mapOf(
            ConsoleWorkoutVariables.START_SPEED to "Speed",
            ConsoleWorkoutVariables.START_RESISTANCE to "Resistance",
            ConsoleWorkoutVariables.START_INCLINE to "Incline"
        )

        if (isToggled) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.72f)
                ) {
                    Image(
                        painter = painterResource(id = intervalType.intervalImage),
                        contentDescription = intervalType.intervalName,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize(0.5f)
                        ,
                        colorFilter = ColorFilter.tint(Color(0xFF191919))
                    )
                }
                Text(
                    text = UnitFormatter.formatTimeSeconds(seconds = time),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("$testTag:SelectedInterval:Text") // eg: CustomHIIT:Hard:SelectedInterval:Text
                    ,
                    textAlign = TextAlign.Center,
                    fontSize = 35.sp.computeSize(),
                    style = DynamicTheme.typography.textStyle1,
                    color = Color.Black
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .fillMaxHeight()
                    .background(
                        if (isPressed.value) intervalType.intervalColor.copy(alpha = 0.3f) else Color(
                            0xFF191919
                        )
                    )
                    .align(Alignment.CenterVertically)
            ) {
                Image(
                    painter = painterResource(id = intervalType.intervalImage),
                    contentDescription = intervalType.intervalName,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight(0.45f)
                        .fillMaxWidth(0.51f),
                    colorFilter = ColorFilter.tint(intervalType.intervalColor)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2C2B2B)),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // If availableParameters size is 2 then it is assumed that incline always exists, only change would be speed or resistance.
                if (availableParameters.keys.size == 2) {
                    var index = 0
                    availableParameters.forEach { (key, value) ->
                        val formattedText = when {
                            key == ConsoleWorkoutVariables.START_RESISTANCE -> {
                                value.toInt().toString()
                            }

                            !isMetric && key == ConsoleWorkoutVariables.START_SPEED -> {
                                String.format(Locale.ENGLISH, "%.1f", value.kphUnit.toMph)
                            }

                            else -> {
                                String.format(Locale.ENGLISH, "%.1f", value)
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (index == 0) {
                                        Modifier
                                            .padding(bottom = 20.dp.computeSize())
                                    } else {
                                        Modifier
                                            .padding(top = 20.dp.computeSize())
                                    }
                                ),
                            verticalArrangement = if (index == 0) Arrangement.Bottom else Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formattedText,
                                style = DynamicTheme.typography.textStyle3,
                                fontSize = (if (index == 0) 30 else 25).sp.computeSize(),
                                color = Color.White,
                                modifier = Modifier.testTag("$testTag:${simpleNameFromWorkoutParameter[key]}:Text") // eg: CustomHIIT:Medium:Speed:Text
                            )
                            Image(painterResource(id = parameterIcons.elementAt(index)), "Frame Parameters")
                        }

                        if (index == 0) {
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth(0.46f)
                                    .height(2.dp.computeSize())
                                    .align(Alignment.CenterHorizontally)
                                    .alpha(0.2f),
                                color = Color(0xFFF5F5F5)
                            )
                        }
                        index++
                    }
                } else {
                    var index = 0
                    // It is presumed that if only one parameter is available then it is resistance and no further checks needed
                    availableParameters.forEach { (key, value) ->
                        Column(
                            modifier = Modifier
                                .weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format(Locale.ENGLISH, "%d", value.toInt()),
                                style = DynamicTheme.typography.textStyle3,
                                fontSize = 30.sp.computeSize(),
                                color = Color.White,
                                modifier = Modifier.testTag("$testTag:${simpleNameFromWorkoutParameter[key]}:Text") // eg: CustomHIIT:Easy:Speed:Text
                            )
                            Image(painterResource(id = parameterIcons.elementAt(0)), "Frame Parameter")
                        }
                        index++
                    }
                }
            }
        }
    }
}

@Composable
internal fun IndicatorComponent(
    modifier: Modifier,
    title: String,
    value: String,
    subUnit: String,
    testTag: String
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 16.sp.computeSize(),
            modifier = Modifier.testTag("$testTag:IndicatorTitle:Text") // eg: CustomHIIT:Speed:IndicatorTitle:Text
        )
        Text(
            value,
            color = Color.White,
            fontSize = 30.sp.computeSize(),
            style = DynamicTheme.typography.textStyle3,
            modifier = Modifier.testTag("$testTag:IndicatorValue:Text") // eg: CustomHIIT:Speed:IndicatorValue:Text
        )
        Text(
            subUnit,
            color = Color.White,
            fontSize = 16.sp.computeSize(),
            modifier = Modifier.testTag("$testTag:IndicatorUnit:Text") // eg: CustomHIIT:Speed:IndicatorUnit:Text
        )
    }
}

@AllDevicePreviews
@Composable
private fun IntervalCardPreview() {
    val intervalList = listOf(
        CustomHIITInterval.EASY_INTERVAL,
        CustomHIITInterval.MEDIUM_INTERVAL,
        CustomHIITInterval.HARD_INTERVAL
    )

    // Dummy variable
    val dummyParameters = hashMapOf<String, Float>()
    dummyParameters.apply {
        put(ConsoleWorkoutVariables.START_RESISTANCE, 15.0f)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp.computeSize())
    ) {
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(60.dp.computeSize(), Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            intervalList.forEach { interval ->
                IntervalCard(
                    modifier = Modifier.size(screenWidth * 0.28f, screenHeight * 0.33f),
                    intervalType = interval,
                    availableParameters = dummyParameters,
                    selectedInterval = 0,
                    timeElapsed = 40,
                    testTag = ""
                )
            }
        }
    }
}