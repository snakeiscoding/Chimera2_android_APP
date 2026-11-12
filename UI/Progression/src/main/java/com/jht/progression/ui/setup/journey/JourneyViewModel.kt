package com.jht.progression.ui.setup.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jht.progression.data.IEvent
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.data.getProgramOfType
import com.jht.progression.data.program.redoEvent
import com.jht.progression.userinfo.UserInfoManager
import com.jht.theme.ThemedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Timer
import kotlin.concurrent.timer

private const val TAG = "JourneyViewModel"

class JourneyViewModel(
    private val userInfoManager: UserInfoManager
) : ThemedViewModel() {
    private val _uiState: MutableStateFlow<MmJourneyState>

    init {
        val program = getProgramOfType(userInfoManager.userInfo.programType)

        val programEvents = userInfoManager.userInfo.programEvents
        val currentTime = System.currentTimeMillis()
        val pagedEvents = pageEvents(programEvents)
        val nextFutureEvent = getNextFutureEventIndex(programEvents)

        _uiState = MutableStateFlow(
            MmJourneyState(
                eventPages = pagedEvents,
                currentTime = currentTime,
                timeUntilNextWorkout = getTimeUntilNextWorkout(currentTime, pagedEvents),
                selectedPage = getStartPage(currentTime, pagedEvents),
                autoDisplayGoalTimeInfoPopup = autoDisplayGoalTimeInfoPopup(currentTime),
                goalSettingsEnabled = program.workoutInterface.isGoalTimeChangePermitted(userInfoManager),
                nextFutureWorkoutIndex = nextFutureEvent
            )
        )
    }

    private fun getStartPage(currentTime: Long, pagedEvents: List<List<IEvent>>): Int {
        pagedEvents.forEachIndexed { i, page ->
            page.forEach { event ->
                if (event.startTime <= currentTime && event.endTime >= currentTime) {
                    return i
                }
            }
        }

        return 0
    }

    val uiState: StateFlow<MmJourneyState> = _uiState.asStateFlow()
    private val updateTimer: Timer

    init {
        // This updates every second, not sure if it needs to be more/less
        updateTimer = timer(name = "update", period = 1000) {
            val currentTime = System.currentTimeMillis()
            val nextWorkout = getTimeUntilNextWorkout(currentTime, _uiState.value.eventPages)
            _uiState.update { currentState ->
                currentState.copy(
                    currentTime = currentTime,
                    timeUntilNextWorkout = nextWorkout,
                    autoDisplayGoalTimeInfoPopup = autoDisplayGoalTimeInfoPopup(currentTime)
                )
            }
        }
    }

    private fun pageEvents(programEvents: ProgramEvents): List<List<IEvent>> {

        // There are 7 elements per page, the last page can have 1-7 elements
        val numPages = (programEvents.events.size - 1) / 7 + 1
        val pageList = ArrayList<ArrayList<IEvent>>(numPages)

        programEvents.events.forEachIndexed { index, event ->
            val page = (index / 7f).toInt()
            if (pageList.size <= page) {
                pageList.add(ArrayList(7))
            }

            pageList[page].add(event)
        }

        return pageList
    }

    // Get the first future workout event index so we can display it differently
    private fun getNextFutureEventIndex(programEvents: ProgramEvents): Int {
        val currentTime = System.currentTimeMillis()

        programEvents.events.forEach { event ->
            if (event is WorkoutEvent) {
                val status = event.getEventStatus(currentTime)

                // If there's a current workout in this program, we don't want to show a next workout
                if (status == EventStatus.CURRENT) {
                    return -1
                }
                // Otherwise return the first future workout we find
                else if (status == EventStatus.FUTURE) {
                    return event.index
                }
            }
        }

        return -1
    }

    fun updateSelectedPage(selectedPage: Int) {
        if (selectedPage >= 0 && selectedPage < _uiState.value.eventPages.size) {
            _uiState.update { currentState ->
                currentState.copy(selectedPage = selectedPage)
            }
        }
    }

    /**
     * Resets program progress to the selected event
     */
    fun resetToWorkout(event: IEvent) {
        if (event is WorkoutEvent) {
            val programEvents = userInfoManager.userInfo.programEvents
            val updatedProgram = redoEvent(event, programEvents)
            userInfoManager.setProgramEvents(updatedProgram)
            userInfoManager.writeUserInfoToFile()
            updateProgramEvents(userInfoManager.userInfo.programEvents)
        }
    }

    private fun updateProgramEvents(programEvents: ProgramEvents) {
        val program = getProgramOfType(userInfoManager.userInfo.programType)
        _uiState.update { currentState ->
            val currentTime = System.currentTimeMillis()
            val pagedEvents = pageEvents(programEvents)
            currentState.copy(
                eventPages = pagedEvents,
                currentTime = currentTime,
                timeUntilNextWorkout = getTimeUntilNextWorkout(currentTime, pagedEvents),
                goalSettingsEnabled = program.workoutInterface.isGoalTimeChangePermitted(userInfoManager)
            )
        }
    }

    fun deleteUserData() {
        userInfoManager.deleteUserData()
    }

    /**
     * Gets the time until the next available workout event
     */
    private fun getTimeUntilNextWorkout(currentTime: Long, events: List<List<IEvent>>): Long {
        for (eventList in events) {
            for (event in eventList) {
                if (event is WorkoutEvent) {
                    // time is 0 if there's a workout we're in the time for that is not completed
                    // otherwise time is the first one that current time is less than start time
                    if (event.startTime <= currentTime && event.endTime > currentTime && !event.completed) {
                        return 0
                    } else if (event.startTime > currentTime) {
                        return event.startTime - currentTime
                    }
                }
            }
        }

        return 0
    }

    /**
     * Acknowledges the goal time information popup has been seen by the user.
     *
     * Sets the goal time information as acknowledged and updates the UI state to prevent
     * the popup from being displayed automatically in the future.
     */
    fun acknowledgeGoalTimeInfoPopup() {
        userInfoManager.setGoalTimeInfoAcknowledged(true)
        userInfoManager.writeUserInfoToFile()
        _uiState.update { currentState -> currentState.copy(autoDisplayGoalTimeInfoPopup = false) }
    }

    /**
     * Checks if the goal time information popup should be automatically displayed.
     *
     * @param currentTime The current time in milliseconds.
     * @return `true` if the goal time information popup should be displayed, `false` otherwise.
     */
    private fun autoDisplayGoalTimeInfoPopup(currentTime: Long): Boolean {
        // User previously acknowledged goal time info popup. Return false now for performance gain
        if (userInfoManager.userInfo.goalTimeInfoAcknowledged) return false

        // User already set a goal time and thus knows about the feature. Return false now for performance gain.
        if (userInfoManager.userInfo.goalBaselineKph != null) return false

        // Get start time of the first workout event
        val programEvents = userInfoManager.userInfo.programEvents.events
        val firstWorkoutStartTime = (programEvents.firstOrNull() {it is WorkoutEvent})?.startTime

        // Return true if current time exceeds start time of first workout event, false otherwise.
        return firstWorkoutStartTime?.let { currentTime >= it } ?: false
    }

    override fun onCleared() {
        updateTimer.cancel()
    }

    companion object {
        fun provideFactory(
            userInfoManager: UserInfoManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return JourneyViewModel(userInfoManager) as T
            }
        }
    }
}

data class MmJourneyState(
    val eventPages: List<List<IEvent>>,
    val currentTime: Long = System.currentTimeMillis(),
    val timeUntilNextWorkout: Long,
    val selectedPage: Int,
    val autoDisplayGoalTimeInfoPopup: Boolean,
    val goalSettingsEnabled: Boolean,
    val nextFutureWorkoutIndex: Int
)