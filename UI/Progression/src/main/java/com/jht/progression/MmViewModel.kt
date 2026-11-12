package com.jht.progression

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.jht.enums.ProgramType
import com.jht.enums.WorkoutId
import com.jht.interfaces.models.IdleTimerModel
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.UserModel
import com.jht.interfaces.models.WorkoutLauncherModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.progression.interfaces.ProgressionModel
import com.jht.data.EventIndex
import com.jht.progression.data.Program
import com.jht.progression.data.ProgramManager
import com.jht.progression.data.WorkoutInfo
import com.jht.progression.data.program.createInitial5kProgram
import com.jht.progression.data.program.debugShiftEvents
import com.jht.progression.ui.workout.topbar.MmTopBarHolder
import com.jht.progression.ui.workout.topbar.ProgressionPanelData
import com.jht.progression.userinfo.UserInfoManager
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class MmViewModel @Inject constructor(
    private val userModel: UserModel,
    private val progressionModel: ProgressionModel,
    private val uiServiceModel: UIServiceModel,
    private val workoutLauncherModel: WorkoutLauncherModel,
    workoutModel: WorkoutModel,
    private val navigationModel: NavigationModel,
    private val idleTimerModel: IdleTimerModel
) : ThemedViewModel() {
    private val programManager = ProgramManager()
    val userInfoManager = UserInfoManager(userModel, progressionModel)
    private val progressionAppId = "a17acf5d-18b8-482c-a835-98d06a92fe60"

    private val _programSelectState =
        MutableStateFlow(
            ProgramSetupState(
                flowToDisplay = MmFlowToDisplay.Setup,
                userName = userModel.name.value,
                programs = programManager.programs,
                loading = false, //TODO: Get whether we are actually loading and adjust this correctly
                useMetric = workoutModel.isMetric.value,
                baseline = userInfoManager.userInfo.baselineKph
            )
        )
    val programSelectState: StateFlow<ProgramSetupState> = _programSelectState.asStateFlow()

    init {
        uiServiceModel.stateIdle.listenAndTrigger { inIdle ->
            updateFlowToDisplay(inIdle, uiServiceModel.stateSummary.value)
        }

        uiServiceModel.stateSummary.listenAndTrigger { inSummary ->
            updateFlowToDisplay(uiServiceModel.stateIdle.value, inSummary)
        }

        // If we're going to load the setup or summary make sure we load the user data so it's available
        if (_programSelectState.value.flowToDisplay == MmFlowToDisplay.Summary ||
            _programSelectState.value.flowToDisplay == MmFlowToDisplay.Setup
        ) {
            Thread {
                _programSelectState.update {
                    it.copy(loading = true)
                }
                userInfoManager.loadUserInfo()
                _programSelectState.update {
                    it.copy(loading = false)
                }
            }.start()
        }
    }

    private fun updateFlowToDisplay(inIdle: Boolean, inSummary: Boolean) {
        val flow = if (inIdle) {
            MmFlowToDisplay.Setup
        } else if (inSummary) {
            MmFlowToDisplay.Summary
        } else {
            MmFlowToDisplay.Workout
        }

        _programSelectState.update { currentState ->
            currentState.copy(
                flowToDisplay = flow
            )
        }
    }

    override fun start() {
        val panelData = ProgressionPanelData(true)
        uiServiceModel.sendUiPanelData(panelData)
    }

    override fun stop() {
        val panelData = ProgressionPanelData(false)
        uiServiceModel.sendUiPanelData(panelData)
        stopIdleTimer()
    }

    /**
     * Launches a workout for the assessment
     */
    fun launchAssessment(workoutInfo: WorkoutInfo) {
        launchWorkout(workoutInfo, null, false)
    }

    /**
     * Launches a workout for the program
     */
    fun launchWorkout(
        workoutInfo: WorkoutInfo,
        eventIndex: EventIndex?,
        showCustomTopBar: Boolean = true
    ) {
        val didStart = workoutLauncherModel.launchCustomWorkout(
            WorkoutId.PROGRESSION_WORKOUT,
            progressionAppId,
            workoutInfo.workoutScript
        )

        if (didStart) {
            progressionModel.updateStartedWorkout(eventIndex, workoutInfo.intervalList)

            if (showCustomTopBar) {
                uiServiceModel.addUiPanel(MmTopBarHolder())
            }

            _programSelectState.update {
                it.copy(isWorkoutLaunched = true)
            }
        }
    }

    /**
     * Launches a random VA workout
     */
    fun launchVaWorkout() {
        viewModelScope.launch {
            val initialConditions: Map<String, Float> = mapOf(Pair("TIME_GOAL_START", 15f))
            workoutLauncherModel.launchExistingWorkout(WorkoutId.VIRTUAL_ACTIVE, initialConditions)
        }
    }

    fun initializeProgram(programType: ProgramType, dayOffset: Int) {
        when (programType) {
            ProgramType.Program5k -> {
                userInfoManager.setProgramEvents(createInitial5kProgram(dayOffset))
            }

            ProgramType.Program10k -> {
                userInfoManager.setProgramEvents(createInitial5kProgram(dayOffset))
            }
        }

        userInfoManager.writeUserInfoToFile()
    }

    fun navigateToSplash() {
        navigationModel.launchSplashScreen()
    }

    fun startIdleTimer() {
        val idleTimerName = "MatrixMethodIdleTimer"
        val timeoutMs = if (userModel.isGuest) 30000 else 90000
        idleTimerModel.addIdleTimer(idleTimerName, timeoutMs) {
            navigateToSplash()
        }

        idleTimerModel.startIdleTimers()
    }

    fun stopIdleTimer() {
        idleTimerModel.clearIdleTimers()
    }

    fun skipToNextEvent() {
        Log.e("MatrixMethod", "Updating all events to be one day sooner!")
        userInfoManager.loadUserInfo()
        val newEvents = debugShiftEvents(userInfoManager.userInfo.programEvents)
        userInfoManager.setProgramEvents(newEvents)
        userInfoManager.writeUserInfoToFile()
    }

    data class ProgramSetupState(
        val flowToDisplay: MmFlowToDisplay,

        // Setup screen
        val userName: String = "",
        val programs: List<Program> = emptyList(),
        val loading: Boolean = false,
        val useMetric: Boolean = false,
        val baseline: Float,
        val isWorkoutLaunched: Boolean = false
    )

    enum class MmFlowToDisplay {
        Setup,
        Workout,
        Summary,
        None
    }
}