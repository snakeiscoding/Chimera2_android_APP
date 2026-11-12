package com.jht.customhiit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.jht.customhiit.common.HiitUtils
import com.jht.customhiit.common.models.CustomHIITInterval
import com.jht.customhiit.common.models.HIITIntervalEnum
import com.jht.customhiit.common.models.HIITWorkoutMap
import com.jht.customhiit.ui.topbar.CustomHIITPanelData
import com.jht.customhiit.ui.topbar.CustomHIITTopBarHolder
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.enums.FrameType
import com.jht.enums.WorkoutId
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs

/** Float comparison tolerance */
private const val TOLERANCE = 0.05f // displayed values will have at most one decimal point, so if it rounds to the tenth it is close enough

@HiltViewModel
open class CustomHIITViewModel @Inject constructor(
    private val systemModel: SystemModel,
    private val deviceModel: DeviceModel,
    private val workoutModel: WorkoutModel,
    private val uiServiceModel: UIServiceModel,
    private val uiBaseModel: UIBaseModel,
) : ThemedViewModel() {
    internal val intervals by lazy { mutableStateOf(CustomHIITInterval.entries) }
    internal val intervalData by lazy { mutableStateOf(mapOf<HIITIntervalEnum, Map<String, MutableState<Float>>>()) }
    internal val hiitWorkoutMapData by lazy { mutableStateOf(HIITWorkoutMap(emptyMap())) }
    internal val selectedInterval by lazy { mutableStateOf(0) }
    // Keeps the track  of current time
    private val timer by lazy { mutableStateOf(0) }
    private var selectionTime = 0
    val timeElapsed by lazy { mutableStateOf(0) }
    val speedUnit by lazy { mutableStateOf("") }
    var isMetric = mutableStateOf(false)
    val indicatorParameters by lazy { mutableStateOf(emptyMap<String, Float>()) }
    private val panelHolder by lazy { mutableStateOf<CustomHIITTopBarHolder?>(null) }
    private var isVisible = true

    private var previousSelectedInterval: HIITIntervalEnum? = HIITIntervalEnum.EASY_INTERVAL

    val frameType by lazy { mutableStateOf(uiBaseModel.frameType) }

    // initialize workout listeners so they are only started once- they are only stopped on destroy
    override fun create() {
        workoutModel.workoutId.listen {
            if (it != WorkoutId.HIIT) { // if we change workout to something else, close this
                destroy() // go ahead and cleanup now, do not always know when activity will destroy
                closeParent()
            }
        }
        /* CGC-21388 Workout should start at default intensity
        workoutModel.onStartWorkout.listen {
            updateInterval(HIITIntervalEnum.EASY_INTERVAL)
        }*/

        workoutModel.isWorkoutPaused.listen { isPaused ->
            if (isPaused) {
                previousSelectedInterval = getHiitEnumFromOrdinal(selectedInterval.value)
                selectedInterval.value = -1
            }
            else {
                resetIntervalTimer()
                when (frameType.value) {
                    // Do nothing for treadmills or climbmills since the speed will get reset
                    FrameType.Treadmill, FrameType.Climbmill -> {}

                    else -> {
                        if (previousSelectedInterval != null) {
                            updateInterval(previousSelectedInterval ?: HIITIntervalEnum.EASY_INTERVAL)
                        }
                    }
                }
            }
        }
        initializeWorkoutListenersForUi() // if workout model is not being closed in stop, probably should not attach in start
    }

    override fun start() {
        super.start()
        isVisible = true
        panelHolder.value?.let {
            sendPanelUpdate()
        }
    }

    private fun sendPanelUpdate() {
        val panelData = CustomHIITPanelData(isVisible, hiitWorkoutMapData.value)
        uiServiceModel.sendUiPanelData((panelData))
    }

    internal fun updateInterval(interval: HIITIntervalEnum) {
        if (workoutModel.isWorkoutPaused.value) return

        previousSelectedInterval = interval

        /** THIS IS EXPERIMENTAL FOR NOW
         * Selecting already selected interval will only update frame's speed, incline, and incline.
         * This has been added to reset parameters back to how it has been configured by the user, so that if workout is paused and then resumed OR
         * user manually override frame parameters and comes back to HIIT then by clicking on the selected interval, it will send configured speed, incline, and resistance to the frame.
         */
        if (interval.ordinal != selectedInterval.value) {
            resetIntervalTimer()
        }
        val hiitWorkoutMap = hiitWorkoutMapData.value.copy(
            selectedInterval = interval.ordinal,
            selectionTime = selectionTime //if (interval.ordinal == selectedInterval.value) hiitWorkoutMapData.value.selectionTime else selectionTime
        )
        if (interval.ordinal != selectedInterval.value)
            selectedInterval.value = interval.ordinal

        /**
         * Updates Frame's speed, incline and resistance depending on the available key
         * 1. Treadmill -> Updates speed and incline
         * 2. Ascent -> Updates resistance and incline
         * 3. Rest all frames except Climbmill -> Updates resistance
        **/
        workoutModel.apply {
            hiitWorkoutMap.intervalMap[HIITIntervalEnum.entries[selectedInterval.value]]?.let { intervalMap ->
                if (intervalMap.containsKey(ConsoleWorkoutVariables.START_SPEED)) {
                    intervalMap[ConsoleWorkoutVariables.START_SPEED]?.let { speed ->
                        updateWorkoutSpeed(speed.value)
                    }
                }
                if (intervalMap.containsKey(ConsoleWorkoutVariables.START_INCLINE)) {
                    intervalMap[ConsoleWorkoutVariables.START_INCLINE]?.let { incline ->
                        updateWorkoutIncline(incline.value)
                    }
                }
                if (intervalMap.containsKey(ConsoleWorkoutVariables.START_RESISTANCE)) {
                    intervalMap[ConsoleWorkoutVariables.START_RESISTANCE]?.let { resistance ->
                        updateWorkoutResistance(resistance.value.toInt())
                    }
                }
            }
        }
        hiitWorkoutMapData.value = hiitWorkoutMap // update our copy and send to panel
        sendPanelUpdate()
    }

    private fun resetIntervalTimer() {
        selectionTime = timer.value
        timeElapsed.value = 0
    }

    private fun initializeWorkoutListenersForUi() {
        workoutModel.apply {
            /**
             * Parameters to be shown at the top of the screen inside HIIT application
             * Treadmill -> Incline & Speed
             * Ascent -> Incline & Resistance
             * Resistance -> Resistance
             */
            HiitUtils.extractBundle(workoutAppSetupBundle.value)?.intervalMap[HIITIntervalEnum.EASY_INTERVAL]?.let { interval ->
                val parameterKey = hashMapOf<String, Float>()
                interval.keys.forEach { keyName ->
                    parameterKey[keyName] = 0f
                }
                indicatorParameters.value = parameterKey
            }
            workoutAppSetupBundle.listenAndTrigger { bundle ->
                HiitUtils.extractBundle(bundle)?.let { hiitInterval ->
                    hiitWorkoutMapData.value = hiitInterval
                    intervalData.value = hiitInterval.intervalMap
                    selectedInterval.value = hiitInterval.selectedInterval
                }
            }
            timeElapsedMs.listenAndTrigger { time ->
                timer.value = time
                timeElapsed.value = abs(hiitWorkoutMapData.value.selectionTime - time)
            }

            isMetric.listenAndTrigger { isMetric ->
                speedUnit.value = if (isMetric) "KPH" else "MPH"
                updateIndicatorParameter(ConsoleWorkoutVariables.START_SPEED, speedKph.value)
                this@CustomHIITViewModel.isMetric.value = isMetric
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

    private fun updateIndicatorParameter(keyName: String, value: Float) {
        if (!indicatorParameters.value.containsKey(keyName)) return

        // Update the indicator with the new value
        indicatorParameters.value = indicatorParameters.value.toMutableMap().apply {
            put(keyName, value)
        }

        // If we have a selected interval, check if we need to reset it the selected interval
        if (selectedInterval.value == -1 || workoutModel.isWorkoutPaused.value) return

        getRelevantIntervalValue(keyName)?.let {
            if (abs(it - value) > TOLERANCE) {
                selectedInterval.value = -1
                hiitWorkoutMapData.value = hiitWorkoutMapData.value.copy(
                    selectedInterval = -1
                )
                sendPanelUpdate()
            }
        }
    }

    private fun getRelevantIntervalValue(keyName: String) : Float? {
        val hiitIntervals = hiitWorkoutMapData.value.intervalMap
        val selectedIntervalData = hiitIntervals[HIITIntervalEnum.entries[selectedInterval.value]]
        var value: Float? = null
        selectedIntervalData?.let { intervalData ->
            value = intervalData[keyName]?.value
        }
        return value
    }

    private fun getHiitEnumFromOrdinal(ordinal: Int) : HIITIntervalEnum? {
        return when (ordinal) {
            HIITIntervalEnum.EASY_INTERVAL.ordinal -> HIITIntervalEnum.EASY_INTERVAL
            HIITIntervalEnum.MEDIUM_INTERVAL.ordinal -> HIITIntervalEnum.MEDIUM_INTERVAL
            HIITIntervalEnum.HARD_INTERVAL.ordinal -> HIITIntervalEnum.HARD_INTERVAL
            else -> null
        }
    }

    // Update UI panel the moment user enters the App Library by clicking on the Home button
    private fun handleTopBarVisibility() {
        if (panelHolder.value == null) {
            panelHolder.value = CustomHIITTopBarHolder()
            panelHolder.value?.let { innerPanelHolder ->
                uiServiceModel.addUiPanel(innerPanelHolder)
            }
        }
    }

    private fun close() {
        isVisible = false
        handleTopBarVisibility()
        sendPanelUpdate()
        systemModel.close()
        deviceModel.close()
        uiServiceModel.close()
    }

    /**
     * Call this to register listeners when activity is paused
     */
    override fun stop() {
        close()
    }

    override fun destroy() {
        workoutModel.close()
    }
}
