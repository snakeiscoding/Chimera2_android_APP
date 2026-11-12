package com.jht.simpleapp

import androidx.compose.runtime.mutableStateOf
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.theme.ThemedViewModel
import com.jht.theme.defaultDynamicTheme
import com.jht.widgets.helpers.StatInfo
import com.jht.widgets.helpers.StatInfoCollection
import com.jht.widgets.helpers.StatType
import com.jht.widgets.helpers.StatType.HEART_RATE
import com.jht.widgets.helpers.StatType.TIME_ELAPSED
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
open class SimpleAppViewModel @Inject constructor(
    private val uiBaseModel: UIBaseModel,
    private val deviceModel: DeviceModel,
    val workoutModel: WorkoutModel,
    private val uiServiceModel: UIServiceModel,
) : ThemedViewModel() {

    private var lastWorkoutId = workoutModel.workoutId.value // could we use a private data class to track last config? or hoist StatFormatter?
    val statInfoCollection = mutableStateOf(createStatList())
    private fun createStatList(): StatInfoCollection = StatInfoCollection.getList(
        frameType = uiBaseModel.frameType,
        showWatts = workoutModel.wattsDisplayAllowed,
        isTargetHeartRate = lastWorkoutId.isTargetHeartRate,
        showCadence = workoutModel.cadenceDisplayAllowed,
    )

    private var selectStatType: StatType = TIME_ELAPSED
    val allowGoalChange by lazy { mutableStateOf(true) }
    val theme by lazy { mutableStateOf(defaultDynamicTheme) }
    val mainColor by lazy { mutableStateOf(theme.value.colors.primary) }
    val isFullscreen by lazy { mutableStateOf(false) }

    fun decrementGoal() {
        workoutModel.decreaseWorkoutGoal()
    }

    fun incrementGoal() {
        workoutModel.increaseWorkoutGoal()
    }

    fun onSelectCard(statInfo: StatInfo) {
        selectStatType = statInfo.type
        updatePrimaryColor()
    }

    fun onSizeClick() {
        uiServiceModel.toggleFullscreen()
    }

    private fun updatePrimaryColor() {
        when (selectStatType) {
            HEART_RATE -> {
                val heartRate = workoutModel.heartRate.value
                // TODO figure the range which is probably based on percent of max based on user age?
                mainColor.value = when (heartRate) {
                    in 30..100 -> theme.value.colors.heart1
                    in 100..125 -> theme.value.colors.heart2
                    in 125..150 -> theme.value.colors.heart3
                    in 150..175 -> theme.value.colors.heart4
                    in 175..Int.MAX_VALUE -> theme.value.colors.heart5
                    else -> theme.value.colors.gray4
                }
            }
            else -> {
                mainColor.value = theme.value.colors.primary
            }
        }
    }

    private fun resetStats(recreateList: Boolean) {
        close()
        if (recreateList) statInfoCollection.value = createStatList() // you will loose previous selections if you recreate
        statInfoCollection.value.registerListener(uiBaseModel, deviceModel, workoutModel)

        workoutModel.allowUserGoalChanges.listenAndTrigger { allowGoalChange.value = it }
        workoutModel.clockUpdateEvent.listen { updatePrimaryColor() }
        uiServiceModel.stateFullScreen.listenAndTrigger { isFullscreen.value = it }
        uiBaseModel.themeUri.listenAndTrigger { theme.value = loadTheme(it) }
        /* change workout would likely push this UI into pause, so I don't think we need this?
        // DO NOT TRIGGER IMMEDIATE THESE OR YOU WILL LOOP BACK IN !
        workoutModel.workoutId.listen {
            // workout ID is set as part of workout setup and therefore a precursor to a new workout starting
            resetStats(true) // reset selections and rebuild for change in frame or workout
        }*/
    }
    private fun close() {
        uiBaseModel.close()
        deviceModel.close()
        workoutModel.close()
        uiServiceModel.close()
    }

    /**
     * Call this to register listeners when activity is resumed
     */
    override fun start() {
        // since we are not listening for workout ID while paused, be sure to check on resume
        // catching frame changes don't seem to be problem at the moment, but maybe consider hoisting the StatFormatter to better track config changes, or monitor frame config itself?
        val currentWorkoutId = workoutModel.workoutId.value
        val recreateList = (currentWorkoutId != lastWorkoutId)
        lastWorkoutId = currentWorkoutId
        resetStats(recreateList)
    }

    /**
     * Call this to register listeners when activity is paused
     */
    override fun stop() {
        close()
    }

}
