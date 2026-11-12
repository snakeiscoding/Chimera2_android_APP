package com.jht.sprint8

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.jht.data.FrameControlCollection
import com.jht.data.sprint8.ISprint8WorkoutData
import com.jht.enums.FrameType
import com.jht.enums.MinMaxParam
import com.jht.enums.WorkoutStage
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutCatalogModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.sprint8.assessment.data.datastore.AssessmentDataStoreManager
import com.jht.sprint8.model.AssessmentButtonConfig
import com.jht.sprint8.model.CustomStatInfo
import com.jht.sprint8.model.CustomWorkoutUIState
import com.jht.sprint8.model.GetReadyConfig
import com.jht.sprint8.model.Sprint8DataConfig
import com.jht.sprint8.model.Sprint8UiState
import com.jht.sprint8.model.WorkoutStats
import com.jht.sprint8.sprint8classic.components.SprintStateType
import com.jht.sprint8.sprint8classic.components.TimerStyle
import com.jht.sprint8.sprint8classic.ui.CustomIntervalData
import com.jht.sprint8.sprint8immersive.components.LevelDirection
import com.jht.sprint8.ui.topbar.Sprint8PanelData
import com.jht.sprint8.ui.topbar.Sprint8TopBarHolder
import com.jht.sprint8.utils.AnimationDurations.LAST_SWEAT_SCORE_DURATION
import com.jht.sprint8.utils.AnimationDurations.RAMPING_UP_AND_GET_READY_DURATION
import com.jht.sprint8.utils.AnimationDurations.RAMPING_UP_AND_SET_DURATION
import com.jht.sprint8.utils.AnimationDurations.RAMPING_UP_DURATION
import com.jht.sprint8.utils.AnimationDurations.SPRINT_DURATION
import com.jht.sprint8.utils.AnimationDurations.WARM_UP_DURATION
import com.jht.sprint8.utils.Constants.SPRINT_THRESHOLD
import com.jht.sprint8.utils.Constants.TOTAL_SPRINTS
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_DIALOG_BUTTON_BG_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_DIALOG_BUTTON_BG_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_WARMUP_COLOR
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_CLOCK_ICON_COLOR
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_HEART_RATE_RPM_BORDER_COLORS
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_HEART_RATE_RPM_COLOR
import com.jht.sprint8.utils.Sprint8Colors.KEYPAD_ACTIVE_RECOVERY_CONFIRM_BTN_COLOR
import com.jht.sprint8.utils.Sprint8Colors.KEYPAD_SPRINTING_CONFIRM_BTN_COLOR
import com.jht.sprint8.utils.Sprint8Colors.RECOVERY_HEART_RATE_RPM_BORDER_COLORS
import com.jht.sprint8.utils.Sprint8Colors.RECOVERY_HEART_RATE_RPM_COLOR
import com.jht.sprint8.utils.Sprint8Colors.RECOVERY_ICON_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BTN_CONTENT_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_COOLDOWN_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_ACTIVE_RECOVERY_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_SPRINT_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_WARM_UP_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_HEART_RATE_RPM_BORDER_COLORS
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_HEART_RATE_RPM_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_ICON_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.TEXT_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_HEART_RATE_RPM_BORDER_COLORS
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_HEART_RATE_RPM_COLOR
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_ICON_BORDER_COLOR
import com.jht.theme.ThemedViewModel
import com.jht.unit.mphUnit
import com.jht.unit.round
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

enum class KeypadType { INCLINE, SPEED, RESISTANCE }

@HiltViewModel
class Sprint8ViewModel @Inject constructor(
    private val workoutModel: WorkoutModel,
    private val uiBaseModel: UIBaseModel,
    private val uiServiceModel: UIServiceModel,
    val navigationModel: NavigationModel,
    @ApplicationContext private val context: Context,
    private val catalogModel: WorkoutCatalogModel,
    private val systemModel: SystemModel,
    private val dataStoreManager: AssessmentDataStoreManager
) : ThemedViewModel() {

    companion object {
        const val CUSTOM_CATEGORY_WORKOUT_LEVEL = 21
    }

    private val _uiState = MutableStateFlow(Sprint8UiState())
    val uiState: StateFlow<Sprint8UiState> = _uiState
    val heartRate by lazy { mutableIntStateOf(0) }
    val rpm by lazy { mutableIntStateOf(0) }
    val spm by lazy { mutableIntStateOf(0) }
    val frameType by lazy { mutableStateOf(uiBaseModel.frameType) }
    val hasFrameControl by lazy {mutableStateOf(workoutModel.hasFrameControl())} // Indicates whether resistance is controlled from the console
    val frameControls = mutableStateOf(FrameControlCollection())
    private val levelHistory = arrayListOf<Int>()
    private val speedKph by lazy { mutableFloatStateOf(0f) }
    private val isMetricUnit by lazy { mutableStateOf(workoutModel.isMetric.value) }
    private val isWorkoutRunning by lazy { mutableStateOf(false) }
    private val isWorkoutPaused by lazy { mutableStateOf(false) }
    private val isWorkoutStopped by lazy { mutableStateOf(false) }
    private val isWorkoutSwitching by lazy { mutableStateOf(false) }
    private val sprint8IsSprintingBool by lazy { mutableStateOf(false) }
    private val sprint8IsRamping by lazy { mutableStateOf(false) }
    private val intervalGoalRemaining by lazy { mutableFloatStateOf(0f) }
    private val panelHolder by lazy { mutableStateOf<Sprint8TopBarHolder?>(null) }
    private val workoutStage by lazy { mutableStateOf<WorkoutStage?>(WorkoutStage.WARM_UP) }
    private val workoutLevelState by lazy { mutableIntStateOf(1) }

    /**
     * lastStep holds the last sprint8State in integer.
     * This value increases strictly as the workout progresses from warmup,
     * going through sprints and recoveries and ultimately cooldown.
     *
     * It ranges in 0 <= lastStep <= 16
     *
     * This is later on used to detect every new recovery stage and to reset the assessment dialog state.
     */
    var lastStep = 0


    val isRpmVisible by lazy { mutableStateOf(false) }
    val isCadenceVisible by lazy { mutableStateOf(false) } //Mutually exclusive to isRpmVisible. Only true for Treadmills that support Cadence tracking
    val speedLabel by lazy { context.getString(if (workoutModel.isMetric.value) R.string.kph_lc else R.string.mph_lc) }

    private val _selectedCategory = MutableStateFlow(0)
    val selectedCategory: StateFlow<Int> = _selectedCategory

    private val _selectedLevel = MutableStateFlow(1)
    private val _workoutData = MutableStateFlow<List<ISprint8WorkoutData>?>(null)
    val workoutData: StateFlow<List<ISprint8WorkoutData>?> = _workoutData

    private val _currentLevelData = MutableStateFlow<List<Float>?>(null)
    val currentLevelData: StateFlow<List<Float>?> = _currentLevelData

    private val _secondaryLevelData = MutableStateFlow<List<Float>?>(null)

    // computed StateFlow for workout stats that updates preview screen ui
    private val _workoutStats = MutableStateFlow(WorkoutStats())
    val workoutStats: StateFlow<WorkoutStats> = _workoutStats

    private val _categorySprintSpeedRange: MutableStateFlow<List<Pair<Float, Float>>> =
        MutableStateFlow(listOf())
    val categorySprintSpeedRange: StateFlow<List<Pair<Float, Float>>> = _categorySprintSpeedRange

    private val _categoryInclineRange: MutableStateFlow<List<Pair<Float?, Float?>>> =
        MutableStateFlow(listOf())
    val categoryInclineRange: StateFlow<List<Pair<Float?, Float?>>> = _categoryInclineRange

    val isRPMBasedMachine: Boolean
        get() = frameType.value in setOf(
            FrameType.Elliptical,
            FrameType.Ascent,
            FrameType.Stepper,
            FrameType.SpinBike,
            FrameType.Bike
        )
    private var assessmentFollowUpJob: Job? = null
    private var assessmentDialogTimeoutJob: Job? = null

    val categoryList = listOf(
        context.getString(R.string.beginner_tc),
        context.getString(R.string.intermediate_tc),
        context.getString(R.string.advanced_tc),
        context.getString(R.string.elite_tc),
        context.getString(R.string.custom_tc)
    )

    private val _customWorkoutScreenUIState = MutableStateFlow(CustomWorkoutUIState())
    internal val customWorkoutScreenUIState: StateFlow<CustomWorkoutUIState> = _customWorkoutScreenUIState.asStateFlow()

    fun getLevelName(level: Int) = "${categoryList[(level - 1) / 5]} $level"

    private val isInclineBasedMachine: Boolean
        get() = frameType.value in setOf(
            FrameType.Treadmill,
            FrameType.Ascent,
        )

    fun onAssessmentButtonClicked() {
        viewModelScope.launch {
            dataStoreManager.saveAssessmentVisibility(true)
        }
    }

    fun isAssessmentVisible(): Flow<Boolean> {
        return dataStoreManager.isAssessmentVisible
    }

    override fun start() {
        initStat()
        handleTopBarVisibility()
        sendPanelUpdate(false)
    }

    private fun sendPanelUpdate(isVisible: Boolean) {
        val panelData = Sprint8PanelData(isVisible)
        uiServiceModel.sendUiPanelData((panelData))
    }

    // Registers the panel once per workout session; null guard prevents duplicate managers.
    private fun handleTopBarVisibility() {
        if (panelHolder.value == null) {
            panelHolder.value = Sprint8TopBarHolder()
            panelHolder.value?.let { innerPanelHolder ->
                uiServiceModel.addUiPanel(innerPanelHolder)
            }
        }
    }

    private fun close() {
        uiServiceModel.close()
        // Do not sendPanelUpdate(false) here, it was causing race condition w/ stop()/start() transitions
    }


    /**
     * Call this to register listeners when activity is paused
     */
    override fun stop() {
        frameControls.value.stopIncrementing()
        sendPanelUpdate(true)
        close()
        // Do not reset panelHolder here, resetting was creating race condition w/ duplicate managers
        uiBaseModel.close()
        navigationModel.close()
        workoutModel.close()
        viewModelScope.launch {
            dataStoreManager.saveRecommendedLevel(assessmentLevel = assessmentSummaryLevelRecommendation())
        }
        assessmentSummaryLevelRecommendation()
    }

    private fun initStat() {
        workoutModel.let {
            it.frameControls.listenAndTrigger { newFrameControls->
                frameControls.value.stop()
                frameControls.value =
                    if (hasFrameControl.value) newFrameControls else FrameControlCollection.EMPTY
                frameControls.value.start()
            }
            it.heartRate.listenAndTrigger { value ->
                if (value != -1)
                    heartRate.intValue = value
            }
            it.rpm.listenAndTrigger { value ->
                if (value != -1)
                    rpm.intValue = value
            }
            it.spm.listenAndTrigger { value ->
                if (value != -1)
                    spm.intValue = value
            }
            it.sweatScore.listenAndTrigger { value ->
                updateUi { copy(sweatScore = round(value).toInt()) }
            }
            it.sweatScoreParcel.listenAndTrigger { value ->
                updateUi { copy(sweatScoreParcel = value) }
            }
            it.sprint8State.listenAndTrigger { value ->
                //Maximum sprint8State step can be 16
                if (value in 0..16) {
                    updateUi { copy(sprint8State = value + 1) }
                }
                if(it.workoutStage.value == WorkoutStage.WORKOUT && !it.isSprinting.value && value > lastStep){
                    resetAssessmentStateForNewRecovery()
                }
                lastStep = value
            }
            it.isRamping.listenAndTrigger { value ->
                updateUi { copy(isRamping = value) }
            }
            it.intervalRemaining.listenAndTrigger { value ->
                updateUi { copy(intervalRemaining = value.toInt()) }
            }
            it.intervalGoalRemaining.listenAndTrigger { value ->
                if (value > 0) {
                    updateUi { copy(remainingTime = value) }
                }
            }
            it.intervalGoal.listenAndTrigger { value ->
                updateUi { copy(totalTime = value) }
            }
            it.isWorkoutRunning.listenAndTrigger { value ->
                updateUi { copy(isRunning = value) }
            }
            it.workoutStage.listenAndTrigger { value ->
                val previousStage = workoutStage.value
                updateUi {
                    copy(
                        timerStyle = getColorStyle(value),
                        progressStyle = getProgressBarStyle(value),
                        workoutStage = value
                    )
                }
                workoutStage.value = value
                if (previousStage != value) {
                    if (value != WorkoutStage.WORKOUT) {
                        resetAssessmentStateForNewRecovery()
                    } else if (!sprint8IsSprintingBool.value) {
                        resetAssessmentStateForNewRecovery()
                    }
                }
            }
            it.isSprinting.listenAndTrigger { value ->
                val previouslySprinting = sprint8IsSprintingBool.value
                if (!previouslySprinting && value) {
                    // transition into next sprint
                    updateUi { copy(isSprinting = true, sprintCount = _uiState.value.sprintCount + 1) }
                } else {
                    updateUi { copy(isSprinting = value) }
                }
                if (!value && previouslySprinting && workoutStage.value == WorkoutStage.WORKOUT) {
                    resetAssessmentStateForNewRecovery()
                }
            }
            it.isMetric.listenAndTrigger { value ->
                isMetricUnit.value = value
            }
            it.speedKph.listenAndTrigger { value ->
                speedKph.floatValue = value
            }
            it.scriptIsSprinting.listenAndTrigger { value ->
                sprint8IsSprintingBool.value = value
            }
            it.isWorkoutRunning.listenAndTrigger { value ->
                isWorkoutRunning.value = value
            }
            it.isWorkoutPaused.listenAndTrigger { value ->
                isWorkoutPaused.value = value
            }
            it.isWorkoutStopped.listenAndTrigger { value ->
                isWorkoutStopped.value = value
            }
            it.isWorkoutSwitching.listenAndTrigger { value ->
                isWorkoutSwitching.value = value
            }
            it.intervalGoalRemaining.listenAndTrigger { value ->
                intervalGoalRemaining.floatValue = value
            }
            it.scriptIsRamping.listenAndTrigger { value ->
                sprint8IsRamping.value = value
            }
            it.numberOfSprints.listenAndTrigger { value ->
                updateUi { copy(numberOfSprints = value) }
            }
            uiServiceModel.stateFullScreen.listenAndTrigger { value ->
                updateUi { copy(isFullScreen = value) }
            }
            it.workoutLevel.listenAndTrigger { level ->
                workoutLevelState.intValue = level
                updateUi { copy(workoutLevel = level) }
            }
        }
        isRpmVisible.value = frameType.value.isBike || frameType.value.isSpinBike ||
                frameType.value.isElliptical || frameType.value.isStepper || frameType.value.isAscent
        isCadenceVisible.value = workoutModel.cadenceDisplayAllowed
    }

    private fun getColorStyle(value: WorkoutStage): TimerStyle {
        return when (value) {
            WorkoutStage.WORKOUT -> TimerStyle.Recovery
            WorkoutStage.WARM_UP -> TimerStyle.Warmup
            WorkoutStage.COOL_DOWN -> TimerStyle.Cooldown
        }
    }

    fun fullScreen() {
        uiServiceModel.toggleFullscreen()
    }

    init {
        saveLastLevelSelected(workoutModel.workoutLevel.value)
        updateWorkoutData(frameType.value)
    }

    private fun saveLastLevelSelected(level: Int) {
        levelHistory.add(level)
    }

    /**
     * Calculates the recommended assessment level based on completed sprints and level history.
     *
     * The recommendation logic is as follows:
     * - If the user has completed all sprints, the recommended level is the average of past levels.
     * - If the total sprints completed exceed a threshold, the recommendation is slightly lower than the last level.
     * - If the total sprints completed are below the threshold, the recommendation is further reduced.
     * - If no level history is available, the default recommendation is 1.
     *
     * The returned level is always at least 1.
     *
     * @return The recommended assessment level as an integer.
     */
    private fun assessmentSummaryLevelRecommendation(): Int {
        val totalSprintCompleted = uiState.value.sprintCount + 1
        val lastLevel = levelHistory.last()
        val recommendedLevel = if (levelHistory.isNotEmpty()) {
            when {
                totalSprintCompleted == TOTAL_SPRINTS -> levelHistory.sum() / TOTAL_SPRINTS
                totalSprintCompleted > SPRINT_THRESHOLD -> lastLevel - 2
                totalSprintCompleted < SPRINT_THRESHOLD -> lastLevel - 4
                else -> 1
            }
        } else {
            1
        }
        return recommendedLevel.coerceAtLeast(1)
    }

    // Function to set the initial value of the Sprint State to its default values,
    // such as setting the timer to 3:00 when the activity is launched.
    fun onReset() {
        _uiState.value = Sprint8UiState()
    }

    private fun getProgressBarStyle(value: WorkoutStage): SprintStateType {
        return when (value) {
            WorkoutStage.WORKOUT -> SprintStateType.Recovery
            WorkoutStage.WARM_UP -> SprintStateType.WarmUp
            WorkoutStage.COOL_DOWN -> SprintStateType.Cooldown
        }
    }

    /**
     * Handles the play/pause button click in the Sprint8 workout.
     *
     * Toggles the workout state: if the workout is currently running, it pauses it;
     * otherwise, it resumes the workout.
     */
    fun onPlayPauseButtonClick() =
        if (_uiState.value.isRunning) workoutModel.pauseWorkout() else workoutModel.resumeWorkout()

    /**
     * Skips the current interval in the Sprint8 workout.
     *
     * This function only performs the skip action if the workout is currently running.
     */
    fun skipInterval() {
        if (_uiState.value.isRunning) {
            workoutModel.actionSkipInterval()
        }
    }

    /**
     * Navigates the user back to the home (splash) screen.
     */
    fun onHome() {
        navigationModel.launchSplashScreen()
    }

    private inline fun updateUi(update: Sprint8UiState.() -> Sprint8UiState) =
        _uiState.update(update)


    val sprint8DataConfig: State<Sprint8DataConfig> by lazy {
        derivedStateOf {
            if (sprint8IsSprintingBool.value && (isWorkoutPaused.value || isWorkoutRunning.value)) {
                Sprint8DataConfig(
                    backgroundColor = SPRINT_ICON_BORDER_COLOR,
                    classicHeartRateAndRpmBgColor = SPRINT_HEART_RATE_RPM_COLOR,
                    getBorderColors = SPRINT_HEART_RATE_RPM_BORDER_COLORS,
                    workoutTitle = context.getString(R.string.sprint_tc),
                    sprint8NewImage = R.drawable.ic_mountains_sprint,
                    sprint8NewColors = SPRINT8_NEW_SPRINT_COLOR,
                    sprint8NewForwardMotionSpeed = 5,
                    isSprint8KeyPadBtnVisible = false,
                    keyPadConfirmBtnColor = KEYPAD_SPRINTING_CONFIRM_BTN_COLOR
                )
            } else {
                val workoutState = workoutStage.value
                if (workoutState == WorkoutStage.WARM_UP || isWorkoutStopped.value || isWorkoutSwitching.value) {
                    Sprint8DataConfig(
                        backgroundColor = WARMUP_ICON_BORDER_COLOR,
                        classicHeartRateAndRpmBgColor = WARMUP_HEART_RATE_RPM_COLOR,
                        getBorderColors = WARMUP_HEART_RATE_RPM_BORDER_COLORS,
                        workoutTitle = context.getString(R.string.warmup_tc),
                        sprint8NewImage = R.drawable.ic_mountains_warm_up,
                        sprint8NewColors = SPRINT8_NEW_WARM_UP_COLOR,
                        sprint8NewForwardMotionSpeed = 2,
                        isSprint8KeyPadBtnVisible = true,
                        keyPadConfirmBtnColor = ASSESSMENT_WARMUP_COLOR
                    )
                } else if (workoutState == WorkoutStage.WORKOUT && !sprint8IsSprintingBool.value) {
                    Sprint8DataConfig(
                        backgroundColor = RECOVERY_ICON_BORDER_COLOR,
                        classicHeartRateAndRpmBgColor = RECOVERY_HEART_RATE_RPM_COLOR,
                        getBorderColors = RECOVERY_HEART_RATE_RPM_BORDER_COLORS,
                        workoutTitle = context.getString(R.string.active_recover_tc),
                        sprint8NewImage = R.drawable.ic_mountains_recovery,
                        sprint8NewColors = SPRINT8_NEW_ACTIVE_RECOVERY_COLOR,
                        sprint8NewForwardMotionSpeed = 3,
                        isSprint8KeyPadBtnVisible = true,
                        keyPadConfirmBtnColor = KEYPAD_ACTIVE_RECOVERY_CONFIRM_BTN_COLOR
                    )
                } else if (workoutState == WorkoutStage.COOL_DOWN) {
                    Sprint8DataConfig(
                        backgroundColor = COOLDOWN_CLOCK_ICON_COLOR,
                        classicHeartRateAndRpmBgColor = COOLDOWN_HEART_RATE_RPM_COLOR,
                        getBorderColors = COOLDOWN_HEART_RATE_RPM_BORDER_COLORS,
                        workoutTitle = context.getString(R.string.cooldown_tc),
                        sprint8NewImage = R.drawable.ic_mountains_cooldown,
                        sprint8NewColors = SPRINT8_COOLDOWN_COLOR,
                        sprint8NewForwardMotionSpeed = 2,
                        isSprint8KeyPadBtnVisible = false,
                        keyPadConfirmBtnColor = SUMMARY_GRAPH_BAR_COLOR_2
                    )
                } else {
                    Sprint8DataConfig()
                }
            }
        }
    }

    val getReadyConfig: State<Sprint8DataConfig> by lazy {
        derivedStateOf {
            val workoutState = _uiState.value.workoutStage
            val intervalGoalRemaining = intervalGoalRemaining.floatValue
            val isRamping = sprint8IsRamping.value
            Sprint8DataConfig(
                getReadyConfig = when {
                    isWorkoutStopped.value || isWorkoutSwitching.value -> {
                        GetReadyConfig(
                            title = context.getString(R.string.get_ready_tc),
                            isVisible = true,
                            isBlinking = false
                        )
                    }

                    workoutState == WorkoutStage.WARM_UP && intervalGoalRemaining >= WARM_UP_DURATION -> {
                        GetReadyConfig(
                            title = context.getString(R.string.go_exclamation_uc),
                            isBlinking = true
                        )
                    }

                    sprint8IsSprintingBool.value && intervalGoalRemaining >= SPRINT_DURATION -> {
                        GetReadyConfig(
                            title = context.getString(R.string.sprinting_description_pg),
                            isBlinking = true
                        )
                    }

                    sprint8IsSprintingBool.value -> {
                        // SweatScore Component Logic PR needs to be reviewed and approved ,logic needed to be added here
                        null
                    }

                    intervalGoalRemaining <= RAMPING_UP_AND_SET_DURATION && isRamping -> {
                        GetReadyConfig(
                            title = context.getString(R.string.set_uc),
                            isVisible = true,
                            isBlinking = false
                        )
                    }

                    intervalGoalRemaining <= RAMPING_UP_AND_GET_READY_DURATION && isRamping -> {
                        GetReadyConfig(
                            title = context.getString(R.string.get_ready_tc),
                            isVisible = true,
                            isBlinking = false
                        )
                    }

                    intervalGoalRemaining <= RAMPING_UP_DURATION && isRamping && hasFrameControl.value -> {
                        GetReadyConfig(
                            title = context.getString(R.string.ramping_up_uc),
                            isBlinking = true
                        )
                    }

                    isRamping && hasFrameControl.value -> {
                        GetReadyConfig(
                            title = context.getString(R.string.ramping_up_uc),
                            isVisible = true,
                            isBlinking = false
                        )
                    }

                    intervalGoalRemaining >= LAST_SWEAT_SCORE_DURATION && workoutState != WorkoutStage.WARM_UP && workoutState != WorkoutStage.COOL_DOWN -> {
                        GetReadyConfig(
                            title = context.getString(R.string.last_sweat_score_uc) + " " + uiState.value.sweatScore,
                            isVisible = true,
                            isBlinking = false
                        )
                    }

                    workoutState == WorkoutStage.WORKOUT && !sprint8IsSprintingBool.value -> {
                        GetReadyConfig(
                            title = context.getString(R.string.active_recovery_good_job_pg),
                            isVisible = true,
                            isBlinking = false
                        )
                    }

                    workoutState == WorkoutStage.COOL_DOWN -> {
                        GetReadyConfig(
                            title = context.getString(R.string.cool_down_good_job_pg),
                            isVisible = true,
                            isBlinking = false
                        )
                    }

                    else -> {
                        null
                    }
                } ?: GetReadyConfig()
            )
        }
    }

    internal val assessmentDialogButtonConfig: State<List<AssessmentButtonConfig>> by lazy {
        derivedStateOf {
            val level = workoutLevelState.intValue
            mutableListOf<AssessmentButtonConfig>().apply {
                if (level < 20) {
                    add(
                        AssessmentButtonConfig(
                            buttonName = context.getString(R.string.level_up_tc),
                            buttonIcon = R.drawable.ic_sprint8_up,
                            buttonBackgroundColor = ASSESSMENT_DIALOG_BUTTON_BG_COLOR_1,
                            buttonTextColor = SUMMARY_GRAPH_BAR_COLOR_1,
                            onButtonClick = {
                                onAssessmentSelection(LevelDirection.LEVEL_UP) {
                                    // need to send targetLevel with 0 based indexing
                                    val newLevel = workoutModel.workoutLevel.value
                                    workoutModel.hotLevelUpdateSprint8(newLevel)
                                    saveLastLevelSelected(newLevel + 1)
                                }
                            }
                        )
                    )
                }

                if (level > 1) {
                    add(
                        AssessmentButtonConfig(
                            buttonName = context.getString(R.string.level_down_tc),
                            buttonIcon = R.drawable.ic_sprint8_down,
                            buttonBackgroundColor = ASSESSMENT_DIALOG_BUTTON_BG_COLOR_2,
                            buttonTextColor = SUMMARY_GRAPH_BAR_COLOR_2,
                            onButtonClick = {
                                onAssessmentSelection(LevelDirection.LEVEL_DOWN) {
                                    // need to send targetLevel with 0 based indexing
                                    val newLevel = workoutModel.workoutLevel.value - 2
                                    workoutModel.hotLevelUpdateSprint8(newLevel)
                                    saveLastLevelSelected(newLevel + 1)
                                }
                            }
                        ))
                }

                add(
                    AssessmentButtonConfig(
                        buttonName = context.getString(R.string.stay_here_tc),
                        buttonIcon = R.drawable.ic_sprint8_check,
                        buttonBackgroundColor = TEXT_COLOR_1,
                        buttonTextColor = SPRINT8_BTN_CONTENT_COLOR_2,
                        onButtonClick = {
                            onAssessmentSelection(LevelDirection.STAY_HERE)
                            saveLastLevelSelected(workoutModel.workoutLevel.value)
                        }
                    )
                )
            }
        }
    }

    fun getSprint8Data() {
        catalogModel.getSprint8WorkoutData { data ->
            // Set the workout data and update the UI
            getCategorySprintSpeedRange(data)
            if (isInclineBasedMachine) {
                getCategoryInclineRange(data)
            }
            setWorkoutData(data)
        }
    }

    private fun setWorkoutData(data: List<ISprint8WorkoutData>) {
        _workoutData.value = data
        updateCurrentLevelData()
    }

    fun selectCategory(categoryIndex: Int) {
        _selectedCategory.value = categoryIndex
        // Reset to first level of the new category
        val baseLevel = getBaseLevelForCategory(categoryIndex)
        _selectedLevel.value = baseLevel
        updateCurrentLevelData()
    }

    fun getSelectedWorkoutLevel(): Int {
        return if (_selectedCategory.value == 4) {
            CUSTOM_CATEGORY_WORKOUT_LEVEL
        } else {
            _selectedLevel.value // direct level value, not calculated
        }
    }

    // get the available levels for the current category
    fun getAvailableLevels(): List<Int>? =
        // omit custom category
        _selectedCategory.value.takeIf { it != 4 }?.let { category ->
            val baseLevel = getBaseLevelForCategory(category)
            (baseLevel..baseLevel + 4).toList() // 5 levels per category
        }

    // get the display level (1-5) for UI buttons
    fun getDisplayLevel(): Int? = _selectedCategory.value.takeIf { it != 4 }?.let {
        _selectedLevel.value - getBaseLevelForCategory(it) + 1 // convert to 1-5 range for display
    }


    // set level by display index (1-5)
    fun setLevelByDisplayIndex(displayIndex: Int) {
        if (_selectedCategory.value == 4) {
            _selectedLevel.value = CUSTOM_CATEGORY_WORKOUT_LEVEL
        } else {
            val baseLevel = getBaseLevelForCategory(_selectedCategory.value)
            val workoutLevel = baseLevel + displayIndex - 1
            _selectedLevel.value = workoutLevel
        }
        updateCurrentLevelData()
    }

    private fun getBaseLevelForCategory(categoryIndex: Int): Int {
        return when (categoryIndex) {
            0 -> 1          // beginner category
            1 -> 6          // intermediate category
            2 -> 11         // advanced category
            3 -> 16         // elite category
            else -> 1       // custom
        }
    }

    fun getTotalWorkoutTime(): String? =
        _currentLevelData.value
            ?.takeIf { it.isNotEmpty() }
            ?.let { "${((120 * (it.size - 1)) / 2 + 240) / 60} ${context.getString(R.string.minutes_lc)}" }


    private fun calculateWorkoutStats(): WorkoutStats {
        val levelData = _currentLevelData.value
        val secondaryData = _secondaryLevelData.value

        var minSprintValue = Float.MAX_VALUE
        var maxSprintValue = -1f
        var minRecoveryValue = Float.MAX_VALUE
        var maxRecoveryValue = -1f
        var minSprintInclineValue = Float.MAX_VALUE
        var maxSprintInclineValue = -1f
        var minRecoveryInclineValue = Float.MAX_VALUE
        var maxRecoveryInclineValue = -1f
        var warmupValue = 0f
        var cooldownValue = 0f
        var warmupInclineValue = 0f
        var cooldownInclineValue = 0f

        // warmup and cooldown speed
        if (levelData?.isNotEmpty() == true) {
            warmupValue = levelData[0]
            cooldownValue = levelData[levelData.size - 1]
        }
        // warmup and cooldown incline
        if (secondaryData?.isNotEmpty() == true) {
            warmupInclineValue = secondaryData[0]
            cooldownInclineValue = secondaryData[secondaryData.size - 1]
        }

        // sprint, recovery min - max speed and incline value
        for (i in 1 until (levelData?.size ?: 0) - 1) {
            // odd index = sprint
            if ((i and 1) != 0) {
                minSprintValue = minOf(minSprintValue, levelData!![i])
                maxSprintValue = maxOf(maxSprintValue, levelData[i])

                if ((secondaryData?.size ?: 0) > i) {
                    minSprintInclineValue =
                        minOf(minSprintInclineValue, secondaryData?.get(i) ?: minSprintInclineValue)
                    maxSprintInclineValue =
                        maxOf(maxSprintInclineValue, secondaryData?.get(i) ?: maxSprintInclineValue)
                }
            } else {
                minRecoveryValue = minOf(minRecoveryValue, levelData!![i])
                maxRecoveryValue = maxOf(maxRecoveryValue, levelData[i])

                if ((secondaryData?.size ?: 0) > i) {
                    minRecoveryInclineValue = minOf(
                        minRecoveryInclineValue,
                        secondaryData?.get(i) ?: minRecoveryInclineValue
                    )
                    maxRecoveryInclineValue = maxOf(
                        maxRecoveryInclineValue,
                        secondaryData?.get(i) ?: maxRecoveryInclineValue
                    )
                }
            }
        }

        return WorkoutStats(
            warmup = warmupValue,
            cooldown = cooldownValue,
            sprintMin = if (minSprintValue == Float.MAX_VALUE) 0f else minSprintValue,
            sprintMax = if (maxSprintValue == -1f) 0f else maxSprintValue,
            recoveryMin = if (minRecoveryValue == Float.MAX_VALUE) 0f else minRecoveryValue,
            recoveryMax = if (maxRecoveryValue == -1f) 0f else maxRecoveryValue,
            warmupIncline = warmupInclineValue,
            cooldownIncline = cooldownInclineValue,
            sprintInclineMin = if (minSprintInclineValue == Float.MAX_VALUE) 0f else minSprintInclineValue,
            sprintInclineMax = if (maxSprintInclineValue == -1f) 0f else maxSprintInclineValue,
            recoveryInclineMin = if (minRecoveryInclineValue == Float.MAX_VALUE) 0f else minRecoveryInclineValue,
            recoveryInclineMax = if (maxRecoveryInclineValue == -1f) 0f else maxRecoveryInclineValue
        )
    }

    /**
     * Updates the current level data for the Sprint8 workout.
     *
     * This function maps the selected workout level to the corresponding data
     * in the interval datasets. It updates both the primary and secondary level
     * data (e.g., speed and incline) and recalculates the workout statistics for UI updates.
     *
     * If the selected level exceeds the available data range, the current and
     * secondary level data are set to null.
     */
    private fun updateCurrentLevelData() {
        val workoutData = _workoutData.value

        if (!workoutData.isNullOrEmpty()) {
            // Regular levels - map workout level to data index
            val workoutLevel = _selectedLevel.value
            val dataIndex = workoutLevel - 1 // Convert workout level to 0-based data index
            if (dataIndex < workoutData[0].intervalData.size) {
                _currentLevelData.value = workoutData[0].intervalData[dataIndex]
                _secondaryLevelData.value = if (workoutData.size > 1) {
                    workoutData[1].intervalData[dataIndex]
                } else null
            } else {
                // Index out of bounds...what the heck? -- selected level is greater than max level of sprint8
                _currentLevelData.value = null
                _secondaryLevelData.value = null
            }
        } else {
            _currentLevelData.value = null
            _secondaryLevelData.value = null
        }

        // Update workout stats StateFlow for  UI updates
        _workoutStats.value = calculateWorkoutStats()
    }

    /**
     * Calculates the sprint speed range for each category in a Sprint8 workout.
     *
     * This function iterates over the workout interval data, filtering to include
     * only the relevant speed values (based on odd indices), and determines
     * the minimum and maximum speeds for each category. Every 5 intervals, a new
     * category range is recorded and the min/max values are reset.
     *
     * @param workoutData List of [ISprint8WorkoutData] containing the workout intervals.
     * The function expects the first item (index 0) to contain speed data.
     */
    private fun getCategorySprintSpeedRange(workoutData: List<ISprint8WorkoutData>?) {
        val categorySprintSpeedRange = mutableListOf<Pair<Float, Float>>()
        val workoutLevelsSpeeds = workoutData?.get(0)
        var minValue = Float.MAX_VALUE
        var maxValue = Float.MIN_VALUE
        val size = (workoutLevelsSpeeds?.intervalData?.size ?: 0)
        for (i in 0 until size) {
            val levelSpeeds = workoutLevelsSpeeds?.intervalData?.get(i)
            levelSpeeds?.let {
                val filteredList = it.filterIndexed { index, _ -> (index and 1) != 0 }
                minValue = min(filteredList.minOrNull() ?: minValue, minValue)
                maxValue = max(filteredList.maxOrNull() ?: maxValue, maxValue)
            }
            if ((i + 1) % 5 == 0) {
                //reset on every new category
                categorySprintSpeedRange.add(Pair(minValue, maxValue))
                minValue = Float.MAX_VALUE
                maxValue = Float.MIN_VALUE
            }
        }
        this._categorySprintSpeedRange.value = categorySprintSpeedRange
    }

    /**
     * Calculates the incline range for each category in a Sprint8 workout.
     *
     * This function iterates over the workout interval data, filtering to include
     * only the relevant incline values (based on odd indices), and determines
     * the minimum and maximum values for each category. Every 5 intervals, a new
     * category range is recorded and the min/max values are reset.
     *
     * @param workoutData List of [ISprint8WorkoutData] containing the workout intervals.
     * The function expects the second item (index 1) to contain incline data.
     */
    private fun getCategoryInclineRange(workoutData: List<ISprint8WorkoutData>?) {
        val categorySprintSpeedRange = mutableListOf<Pair<Float?, Float?>>()
        val workoutLevelsIncline = workoutData?.get(1)
        var minValue = Float.MAX_VALUE
        var maxValue = Float.MIN_VALUE
        val size = (workoutLevelsIncline?.intervalData?.size ?: 0)
        for (i in 0 until size) {
            val levelSpeeds = workoutLevelsIncline?.intervalData?.get(i)
            levelSpeeds?.let {
                val filteredList = it.filterIndexed { index, _ -> (index and 1) != 0 }
                minValue = min(filteredList.minOrNull() ?: minValue, minValue)
                maxValue = max(filteredList.maxOrNull() ?: maxValue, maxValue)
            }
            if ((i + 1) % 5 == 0) {
                //reset on every new category
                categorySprintSpeedRange.add(Pair(minValue, maxValue))
                minValue = Float.MAX_VALUE
                maxValue = Float.MIN_VALUE
            }
        }
        this._categoryInclineRange.value = categorySprintSpeedRange
    }

    fun getCustomStatMinMax(frame: FrameType): CustomStatInfo {
        return when (frame) {
            FrameType.Treadmill -> {
                CustomStatInfo(
                    Pair(
                        MinMaxParam.Speed,
                        systemModel.getMinMaxInfoForParameter(MinMaxParam.Speed)
                    ),
                    Pair(
                        MinMaxParam.Incline,
                        systemModel.getMinMaxInfoForParameter(MinMaxParam.Incline)
                    ),
                )
            }

            FrameType.Elliptical,
            FrameType.Bike,
            FrameType.Stepper,
            FrameType.Climbmill,
            FrameType.SpinBike -> {
                CustomStatInfo(
                    Pair(
                        MinMaxParam.Resistance,
                        systemModel.getMinMaxInfoForParameter(MinMaxParam.Resistance)
                    ),
                    null,
                )
            }

            FrameType.Ascent -> {
                CustomStatInfo(
                    Pair(
                        MinMaxParam.Resistance,
                        systemModel.getMinMaxInfoForParameter(MinMaxParam.Resistance)
                    ),
                    Pair(
                        MinMaxParam.Incline,
                        systemModel.getMinMaxInfoForParameter(MinMaxParam.Incline)
                    ),
                )
            }
        }
    }
    /**
     * Handles the selection of an assessment level by the user.
     *
     * This function updates the UI state to reflect the selected level direction,
     * cancels any existing follow-up or dialog timers, marks the assessment as
     * answered, shows a brief follow-up message, and then hides the follow-up
     * after a delay.
     *
     * @param direction The [LevelDirection] chosen by the user.
     * @param action Optional callback to invoke immediately upon selection.
     */
    private fun onAssessmentSelection(
        direction: LevelDirection,
        action: (() -> Unit)? = null
    ) {
        action?.invoke()
        updateUi { copy(levelDirection = direction) }
        assessmentFollowUpJob?.cancel()
        assessmentDialogTimeoutJob?.cancel()
        updateUi {
            copy(
                hasAssessmentBeenAnswered = true,
                showAssessmentFollowUp = true
            )
        }
        assessmentFollowUpJob = viewModelScope.launch {
            delay(5_000)
            updateUi { copy(showAssessmentFollowUp = false) }
        }
    }

    /**
     * Resets the assessment state when starting a new recovery period.
     *
     * This function cancels any ongoing assessment follow-up or dialog timers,
     * resets the UI state to indicate that the assessment has not been answered
     * and that the follow-up should not be shown, and then starts a new
     * assessment dialog timer if needed.
     */
    private fun resetAssessmentStateForNewRecovery() {
        assessmentFollowUpJob?.cancel()
        assessmentDialogTimeoutJob?.cancel()
        updateUi {
            copy(
                hasAssessmentBeenAnswered = false,
                showAssessmentFollowUp = false
            )
        }
        startAssessmentDialogTimerIfNeeded()
    }

    /**
     * Starts a timer for the assessment dialog if conditions are met.
     *
     * This function checks if the workout stage is in progress and the Sprint8
     * sprint is not active. If so, it cancels any existing assessment timer
     * and launches a new coroutine that waits for 30 seconds. If the assessment
     * has not been answered after the delay, it triggers the default assessment
     * selection action.
     */
    private fun startAssessmentDialogTimerIfNeeded() {
        if (workoutStage.value == WorkoutStage.WORKOUT && !sprint8IsSprintingBool.value) {
            assessmentDialogTimeoutJob?.cancel()
            assessmentDialogTimeoutJob = viewModelScope.launch {
                delay(30_000)
                if (!_uiState.value.hasAssessmentBeenAnswered) {
                    onAssessmentSelection(LevelDirection.STAY_HERE)
                }
            }
        }
    }

    /**
     * Updates workout data with default intervals for the specified equipment type
     */
    fun updateWorkoutData(frameType: FrameType) {
        _customWorkoutScreenUIState.update { it.copy(
            customIntervalDataList = savedCustomWorkout() ?: getDefaultWorkout(frameType)
        )}
    }

    /**
     * The custom workout this user last started, in display units, or null when they have not saved
     * one. A Sprint8 workout is a warmup, sprint/recovery pairs and a cooldown, so a saved list with
     * an even number of intervals is treated as unusable rather than trusted.
     */
    private fun savedCustomWorkout(): List<CustomIntervalData>? {
        // runs during view model construction, on the way into the workout screen as well as the
        // setup, so a host that throws in here must not stop the workout from starting
        val saved = runCatching { catalogModel.loadSprint8CustomWorkoutData() }.getOrNull() ?: return null
        val primary = saved.firstOrNull()?.intervalData?.firstOrNull() ?: return null
        if (primary.size < 3 || primary.size % 2 == 0) return null
        val secondary = saved.getOrNull(1)?.intervalData?.firstOrNull()
        return primary.mapIndexed { index, value ->
            CustomIntervalData(
                mutableFloatStateOf(value),
                secondary?.getOrNull(index)?.let { mutableFloatStateOf(it) }
            )
        }
    }

    /**
     * Updates the currently selected sprint index in the UI
     */
    fun updateSelectedSprint(index: Int) {
        _customWorkoutScreenUIState.update { it.copy(selectedSprint = index) }
    }

    /**
     * Adds a new sprint/recovery interval pair to the end of the workout
     */
    fun addInterval() {
        _customWorkoutScreenUIState.update {
            it.copy(customIntervalDataList = addSegment(it.customIntervalDataList))
        }
    }

    /**
     * Deletes the specified sprint/recovery interval pair from the workout
     */
    fun deleteInterval(selectedSprintIndex: Int) {
        _customWorkoutScreenUIState.update {
            it.copy(customIntervalDataList = deleteSegment(selectedSprintIndex, it.customIntervalDataList))
        }
    }

    /**
     * Returns default workout intervals based on equipment type
     */
    private fun getDefaultWorkout(frameType: FrameType): List<CustomIntervalData> {
        val isMetric = isMetricUnit.value
        fun CI(primary: Float, secondary: Float? = null) = CustomIntervalData(
            mutableFloatStateOf(convertSpeed(primary, isMetric)),
            if (secondary != null) mutableFloatStateOf(convertSpeed(secondary, isMetric))
            else null
        )

        return when (frameType) {
            FrameType.Treadmill -> mutableListOf(
                CI(0.5f, 0f),           // warmup
                CI(7f, 0f), CI(2.5f, 0f),   // sprint1 / recovery
                CI(7f, 0f), CI(2.5f, 0f),   // sprint2 / recovery
                CI(7.2f, 0f), CI(2.5f, 0f),   // sprint3 / recovery
                CI(7.2f, 0f), CI(2.5f, 0f),   // sprint4 / recovery
                CI(7.5f, 0f), CI(2.5f, 0f),   // sprint5 / recovery
                CI(7.5f, 0f), CI(2.5f, 0f),   // sprint6 / recovery
                CI(7.5f, 0f), CI(2.5f, 0f),   // sprint7 / recovery
                CI(7.5f, 0f),           // sprint8
                CI(2.5f, 0f)          // cooldown
            )

            FrameType.Elliptical,
            FrameType.Bike,
            FrameType.Stepper,
            FrameType.SpinBike -> mutableListOf(
                CI(2f),           // warmup
                CI(13f), CI(2f),   // sprint1 / recovery
                CI(13f), CI(2f),   // sprint2 / recovery
                CI(14f), CI(2f),   // sprint3 / recovery
                CI(14f), CI(2f),   // sprint4 / recovery
                CI(16f), CI(2f),   // sprint5 / recovery
                CI(16f), CI(2f),   // sprint6 / recovery
                CI(16f), CI(2f),   // sprint7 / recovery
                CI(16f),           // sprint8
                CI(2f)          // cooldown
            )

            FrameType.Ascent -> mutableListOf(
                CI(2f, 0f),     // warmup
                CI(13f, 0f), CI(2f, 0f),
                CI(13f, 0f), CI(2f, 0f),
                CI(14f, 0f), CI(2f, 0f),
                CI(14f, 0f), CI(2f, 0f),
                CI(16f, 0f), CI(2f, 0f),
                CI(16f, 0f), CI(2f, 0f),
                CI(16f, 0f), CI(2f, 0f),
                CI(16f, 0f),
                CI(2f, 0f)      // cooldown
            )

            FrameType.Climbmill -> mutableListOf(
                CI(-1f)
            )
        }
    }

    /**
     * Adds a sprint/recovery pair at the end, copying values from the last pair
     */
    private fun addSegment(workoutData: List<CustomIntervalData>): List<CustomIntervalData> {
        if (workoutData.size < 2 ) {
            return workoutData //Silent failure
        }
        // can only add at end, add new interval with same info as previous last one
        val updatedWorkoutData = workoutData.toMutableList()
        val lastSprintInterval = workoutData[workoutData.lastIndex - 1]
        val lastRecoveryInterval = workoutData[workoutData.lastIndex]
        val newSprintInterval = CustomIntervalData(
            mutableFloatStateOf(lastSprintInterval.primaryValue.value),
            if (lastSprintInterval.secondaryValue != null) mutableFloatStateOf(lastSprintInterval.secondaryValue.value) else null
        )
        val newRecoveryInterval = CustomIntervalData(
            mutableFloatStateOf(lastRecoveryInterval.primaryValue.value),
            if (lastRecoveryInterval.secondaryValue != null) mutableFloatStateOf(lastRecoveryInterval.secondaryValue.value) else null
        )
        updatedWorkoutData.add(newSprintInterval)
        updatedWorkoutData.add(newRecoveryInterval)
        return updatedWorkoutData
    }

    /**
     * Removes the sprint/recovery pair at the specified segment number
     */
    private fun deleteSegment(
        segmentIndex: Int,
        workoutData: List<CustomIntervalData>
    ): List<CustomIntervalData> {
        // remove the two intervals aligned with the current segment
        val segment = segmentIndex.coerceAtLeast(1)
        val removedIndices = listOf(segment * 2 - 1, segment * 2)
        val updatedData = workoutData.filterIndexed { index, data -> !removedIndices.contains(index) }
        return updatedData
    }
}

/**
 * Formats a range of minimum and maximum float values into a display string.
 *
 * If the min and max values are equal, only a single value is returned.
 * Otherwise, the values are concatenated with a dash ("min-max").
 * Values with no fractional part are displayed as integers, otherwise
 * they are formatted with one decimal place.
 *
 * @param minValue The minimum value in the range.
 * @param maxValue The maximum value in the range.
 * @return A formatted string representing the range.
 */
fun formatMinMaxText(minValue: Float, maxValue: Float): String {
    fun formatValue(value: Float): String =
        if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)

    return if (minValue == maxValue) {
        formatValue(minValue)
    } else {
        "${formatValue(minValue)}-${formatValue(maxValue)}"
    }
}
/**
 * Formats a single float value for display.
 *
 * Values with no fractional part are displayed as integers, otherwise
 * they are formatted with one decimal place.
 *
 * @param value The float value to format.
 * @return A formatted string representing the value.
 */
fun formatValue(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        "%.1f".format(value)
    }
}

/**
 * Converts mph to kph (rounded to 1 decimal) when metric is enabled.
 * Returns original mph if imperial.
 */
private fun convertSpeed(
    mph: Float,
    isMetric: Boolean
): Float {
    return if (isMetric) {
        mph.mphUnit.toKph.round(1)
    } else {
        mph
    }
}
