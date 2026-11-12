package com.jht.customhiit.ui.topbar

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.customhiit.R
import com.jht.customhiit.common.HiitUtils
import com.jht.customhiit.common.models.CustomHIITInterval
import com.jht.customhiit.common.models.HIITIntervalEnum
import com.jht.customhiit.common.models.HIITWorkoutMap
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.enums.UiPanelType
import com.jht.enums.WorkoutId
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.interfaces.ui.BasePanelData
import com.jht.interfaces.ui.InnerPanelBase
import com.jht.interfaces.ui.InnerPanelHolder
import com.jht.interfaces.ui.UiPanelOperations
import com.jht.theme.DynamicTheme
import com.jht.theme.defaultDynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.unit.UnitFormatter
import com.jht.unit.kphUnit
import com.jht.widgets.helpers.getInjectionEntryPoint
import kotlinx.parcelize.Parcelize
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

internal val CUSTOM_HII_TOP_BAR_PANEL_ID: UUID =
    UUID.fromString("4d3350f9-5f9d-43fc-b6d2-711a44f66d2c")

// Taken directly from app id used by the system
private const val HIIT_APP_UUID = "67ae3963-bc1a-4914-a0c1-9f7e2962872c"

private const val tag= "CustomHIIT:AppBar"

/** Float comparison tolerance */
private const val TOLERANCE = 0.05f // displayed values will have at most one decimal point, so if it rounds to the tenth it is close enough

@Parcelize
internal class CustomHIITTopBarHolder : InnerPanelHolder() {
    override fun createPanel(
        context: Context, panelOperations: UiPanelOperations
    ): InnerPanelBase {
        return CustomHIITTopBar(context, panelOperations)
    }
}

internal class CustomHIITTopBar(
    context: Context,
    panelOperations: UiPanelOperations
) : InnerPanelBase(
    panelId = CUSTOM_HII_TOP_BAR_PANEL_ID,
    panelType = UiPanelType.TopPanel,
    panelOperations = panelOperations
) {
    private val uiServiceModel: UIServiceModel
    private val workoutModel: WorkoutModel
    private val navigationModel: NavigationModel

    private var customHIITData = mutableStateOf(HIITWorkoutMap(emptyMap()))
    private var selectedInterval = mutableStateOf(0)
    private var selectionTime = mutableStateOf(0)
    private var indicatorParameters = mutableStateOf(emptyMap<String, Float>())
    private var timeElapsed = mutableStateOf(0)
    private var isMetric = mutableStateOf(false)
    private var lastPanelData = mutableStateOf<CustomHIITPanelData?>(null)
    private var isWorkoutStopped = false
    private var isFullScreen = false
    private var isInHIITWorkout = false

    init {
        val injection = getInjectionEntryPoint(context)
        uiServiceModel = injection.ui()
        workoutModel = injection.workout()
        navigationModel = injection.navigation()

        workoutModel.apply {
            workoutId.listenAndTrigger {
                val matchingId = (it == WorkoutId.HIIT)
                if (matchingId != isInHIITWorkout) {
                    isInHIITWorkout = matchingId
                    updateTopBarVisibility()
                }
            }

            updateFromMap(HiitUtils.extractBundle(workoutAppSetupBundle.value))

            uiServiceModel.stateFullScreen.listenAndTrigger {
                isFullScreen = it
                updateTopBarVisibility()
            }

            isWorkoutStopped.listenAndTrigger { stop ->
                this@CustomHIITTopBar.isWorkoutStopped = stop
                updateTopBarVisibility()
            }

            timeElapsedMs.listenAndTrigger { time ->
                if (selectedInterval.value >= 0) {
                    timeElapsed.value = Math.round(abs(selectionTime.value - time) / 1000f)
                }
            }

            isMetric.listenAndTrigger {
                updateIndicatorParameter(ConsoleWorkoutVariables.START_SPEED, speedKph.value, true)
                this@CustomHIITTopBar.isMetric.value = it
            }

            speedKph.listenAndTrigger { speed ->
                updateIndicatorParameter(ConsoleWorkoutVariables.START_SPEED, speed)
            }

            incline.listenAndTrigger { incline ->
                updateIndicatorParameter(ConsoleWorkoutVariables.START_INCLINE, incline)
            }

            resistance.listenAndTrigger { resistance ->
                updateIndicatorParameter(ConsoleWorkoutVariables.START_RESISTANCE, resistance.toFloat())
            }
        }
    }

    private fun updateFromMap(intervalsData: HIITWorkoutMap?) {
        if (intervalsData == null) return
        // initial indicators
        if (indicatorParameters.value.isEmpty()) {
            intervalsData.intervalMap[HIITIntervalEnum.EASY_INTERVAL]?.let { interval ->
                val parameterKey = hashMapOf<String, Float>()
                interval.keys.forEach { keyName ->
                    parameterKey[keyName] = 0f
                }
                indicatorParameters.value = parameterKey
            }
        }
        // data
        customHIITData.value = intervalsData
        selectedInterval.value = intervalsData.selectedInterval
        selectionTime.value = intervalsData.selectionTime
    }

    private fun updateIndicatorParameter(keyName: String, value: Float, fromMetric: Boolean = false) {
        if (indicatorParameters.value.containsKey(keyName)) {
            if (selectedInterval.value != -1 && !fromMetric) {
                var newInterval = selectedInterval.value
                val selectedIntervalData =
                    customHIITData.value.intervalMap[HIITIntervalEnum.entries[selectedInterval.value]]
                selectedIntervalData?.let { intervalData ->
                    val expectedValue = intervalData[keyName]?.value?: 0.0f
                    if (abs(expectedValue - value) > TOLERANCE) {
                        newInterval = -1
                    }
                }
                workoutModel.apply {
                    customHIITData.value = customHIITData.value.copy(
                        selectedInterval = newInterval
                    )
                }
            }
            indicatorParameters.value = indicatorParameters.value.toMutableMap().apply {
                put(keyName, value)
            }
        }
    }

    override fun onPanelReceiveData(panelData: BasePanelData) {
        lastPanelData.value = panelData as? CustomHIITPanelData
        lastPanelData.value?.let { data ->
            updateFromMap(data.map)
            updateTopBarVisibility(data.isCustomHIITVisible)
        }
    }

    private fun updateTopBarVisibility(appInForeground: Boolean = lastPanelData.value?.isCustomHIITVisible ?: false) {
        // Top bar will be visible if workout is active and HIIT is not in foreground
        val show = !isWorkoutStopped && !appInForeground && !isFullScreen && isInHIITWorkout
        if (show) {
            show()
        } else {
            hide()
        }
    }

    private fun startHIITActivity() {
        // Clicking on this will cause HIIT application to be running in the foreground, hence sending updateTopBarVisibility() again in this one
        navigationModel.launchApp(HIIT_APP_UUID)
        updateTopBarVisibility(true)
    }

    @Composable
    override fun PanelContent() {

        TopBar(
            modifier = Modifier.fillMaxSize(),
            selectedInterval = selectedInterval.value,
            timeElapsed = timeElapsed.value,
            isMetric = isMetric.value,
            indicatorParameters = indicatorParameters.value,
            onClickTopBar = { startHIITActivity() }
        )

    }

    override fun onPanelDestroyed() {
        workoutModel.close()
        uiServiceModel.close()
        navigationModel.close()
    }

}

/**
 * To indicate parameters heading and it's value for Speed, Incline, and Resistance,
 * @param heading -> Speed, Incline, and Resistance
 * @param value -> Passed in using current parameter value
 * @param unit -> For Speed it can be MPH/KPH depending on the unit system. For Incline, it's "%", whereas for resistance it will stay empty
 */
@Composable
private fun ParameterIndicator(
    modifier: Modifier = Modifier,
    heading: String,
    value: Float,
    unit: String = ""
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(defaultDynamicTheme.colors.background1),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = heading,
                    fontSize = 14.sp.computeSize(),
                    color = Color.White,
                    modifier = Modifier.testTag("$tag:${heading}Title:Text") // eg: CustomHIIT:AppBar:SpeedTitle:Text
                )
                Text(
                    text = String.format(Locale.ENGLISH, "%.1f", value),
                    fontSize = 32.sp.computeSize(),
                    style = DynamicTheme.typography.textStyle1,
                    color = Color.White,
                    modifier = Modifier.testTag("$tag:${heading}Value:Text") // eg: CustomHIIT:AppBar:SpeedTitle:Text
                )
            }
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    fontSize = 12.sp.computeSize(),
                    style = DynamicTheme.typography.textStyle1,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier
                        .padding(5.dp.computeSize())
                        .align(Alignment.Bottom)
                        .testTag("$tag:${heading}Unit:Text") // eg: CustomHIIT:AppBar:SpeedUnit:Text
                )
            }
        }
    }
}

@Composable
internal fun HIITIconWithTimeElapsed(
    modifier: Modifier = Modifier,
    timeElapsed: Int = 0,
    selectedInterval: Int = 0,
    intervalIcon: Int = R.drawable.ic_custom_hiit_easy
) {
    Box(
        modifier = modifier
            .padding(horizontal = 30.dp.computeSize()),
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(R.drawable.ic_hiit_brand),
            contentDescription = "HIIT Icon",
            modifier = Modifier
                .fillMaxHeight()
                .padding(5.dp.computeSize())
        )

        // Intervals can be de-selected in-case if user manually changes speed/resistance or incline via quick keys
        if (selectedInterval != -1) {
            val intervalColor = CustomHIITInterval.entries[selectedInterval].intervalColor
            Row(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(
                    UnitFormatter.formatTimeSeconds(seconds = timeElapsed),
                    fontSize = 32.sp.computeSize(),
                    modifier = Modifier.align(Alignment.CenterVertically)
                        .testTag("$tag:TimeElapsed:Text"),    // eg: CustomHIIT:AppBar:TimeElapsed:Text
                    style = DynamicTheme.typography.textStyle1,
                    color = intervalColor
                )
                Image(
                    painter = painterResource(intervalIcon),
                    contentDescription = "INTERVAL ICON",
                    modifier = Modifier.fillMaxHeight(),
                    colorFilter = ColorFilter.tint(intervalColor)
                )
            }
        }

    }
}

/**
 * @param availableParameters provides the list of frame's supported parameters such as Speed/Resistance & Incline
 * @param selectedInterval provides the selected interval, if no interval is selected then it is set to -1
 * @param timeElapsed based on the interval selection time made by the user, it is calculated based on current time - interval selection time
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TopBar(
    modifier: Modifier,
    selectedInterval: Int,
    timeElapsed: Int = 0,
    isMetric: Boolean = false,
    indicatorParameters: Map<String, Float> = emptyMap(),
    onClickTopBar: () -> Unit
) {

    val parameterNames = mutableListOf<String>()
    val parameterValues = mutableListOf<Float>()

    // Setting speed unit initially which can be later overwritten if Resistance is found as a parameter
    var speedUnit = if (isMetric) "KPH" else "MPH"

    val selectedIntervalRes = when (selectedInterval) {
        0 -> R.drawable.ic_custom_hiit_easy
        1 -> R.drawable.ic_custom_hiit_medium
        2 -> R.drawable.ic_custom_hiit_hard
        else -> -1
    }

    indicatorParameters.keys.sorted().forEach { parameterName ->
        when (parameterName) {
            ConsoleWorkoutVariables.START_INCLINE -> {
                parameterNames.add("Incline")
                parameterValues.add(indicatorParameters[parameterName] ?: 0f)
            }
            ConsoleWorkoutVariables.START_SPEED -> {
                parameterNames.add("Speed")
                parameterValues.add(indicatorParameters[parameterName]?.let {speed-> if(isMetric) speed else speed.kphUnit.toMph } ?: 0f)
            }
            ConsoleWorkoutVariables.START_RESISTANCE -> {
                parameterNames.add("Resistance")
                parameterValues.add(indicatorParameters[parameterName] ?: 0f)
                speedUnit = ""
            }
        }
    }

    Row(modifier = modifier
        .background(defaultDynamicTheme.colors.gray1)
        .clickable { onClickTopBar() }
        .semantics {
            testTagsAsResourceId = true
            contentDescription = "Standard Workout Top Bar"
        }
        .testTag(tag)
    ) {
        /**
         * This check is more like a hard constraint,
         * Currently max parameter supported in this implementation is 2 which is Speed/Resistance & Incline For (Ascent and Treadmill based frames)
         * And only one parameter in case if frame type is resistance
         */
        indicatorParameters.let { map ->
            if (map.size >= 2) {
                // Note: First item will always be Incline
                ParameterIndicator(
                    modifier = Modifier.weight(1.2f),
                    heading = parameterNames[0],
                    value = parameterValues[0],
                    unit = "%"
                )

                // Center Design
                HIITIconWithTimeElapsed(
                    modifier = Modifier.weight(6.6f),
                    selectedInterval = selectedInterval,
                    intervalIcon = selectedIntervalRes,
                    timeElapsed = timeElapsed
                )

                ParameterIndicator(
                    modifier = Modifier.weight(1.2f),
                    heading = parameterNames[1],
                    value = parameterValues[1],
                    unit = speedUnit
                )
            } else {
                HIITIconWithTimeElapsed(
                    modifier = Modifier.weight(7.8f),
                    selectedInterval = selectedInterval,
                    intervalIcon = selectedIntervalRes,
                    timeElapsed = timeElapsed
                )

                // Don't send unit for Resistance
                ParameterIndicator(
                    modifier = Modifier.weight(1.2f),
                    heading = parameterNames[0],
                    value = parameterValues[0]
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun CustomHIITTopBarPreview() {

    val mockParameters = hashMapOf<String, MutableState<Float>>()

    mockParameters.apply {
        put("START_SPEED", mutableStateOf(5.0f))
        put("START_INCLINE", mutableStateOf(0.5f))
    }

    TopBar(
        modifier = Modifier.aspectRatio(1080 / 80f),
        selectedInterval = 0,
        timeElapsed = 90,
        onClickTopBar = {}
    )
}