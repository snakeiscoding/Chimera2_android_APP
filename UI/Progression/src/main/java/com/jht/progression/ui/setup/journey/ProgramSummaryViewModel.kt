package com.jht.progression.ui.setup.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jht.progression.R
import com.jht.progression.data.EventLabel
import com.jht.progression.data.Program
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.data.secondsToHrMinSecString
import com.jht.progression.userinfo.UserInfoManager
import com.jht.unit.UnitFormatter
import com.jht.unit.secondsPerKmPaceUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt


class ProgramSummaryViewModel(
    val userInfoManager: UserInfoManager,
    val program: Program,
    useMetric: Boolean,
    isEndOfProgram: Boolean? = null,
) : ViewModel() {
    // Private instance variables
    private var workoutsCompleted = 0
    private var workoutsMissed = 0
    private var totalWorkoutTimeSec = 0
    private var totalCalories = 0f
    private var totalDistanceKm = 0f
    private var averageSpeedKph = 0f
    private var peakSpeedKph = 0f
    private var estimatedRaceTimeSec = 0
    private var currentRacePaceSecPerKm = 0
    private var startingRacePaceSecPerKm = 0
    private var bestRacePaceSecPerKm = 0
    private var overallAverageHR = 0
    private var averagePeakHR = 0
    private var totalElevationMeters = 0f
    private var overallTotalAverageHR = 0
    private var overallTotalAverageSpeedKph = 0f
    private val minDisplayedHeartRate = 30
    private var count = 0
    private val firstPageData: List<ProgramSummaryLine>
    private val secondPageData: List<ProgramSummaryLine>
    private var estimatedTimeChange = 0
    private var estimatedPaceChange = 0
    private var currentPaceValue: String = ""
    private var estimatedRaceTime: String = ""

    // Public instance variables
    val uiState: StateFlow<MmProgramSummaryState>
    private var _uiState: MutableStateFlow<MmProgramSummaryState>

    // Initialization logic
    init {
        val programEvents = userInfoManager.userInfo.programEvents
        val baseline = userInfoManager.userInfo.baselineKph
        val currentTime = System.currentTimeMillis()
        val endOfProgram = isEndOfProgram ?: isLastEventCompletedOrMissed(programEvents, currentTime)

        // Calculate and format program summary statistics
        calculateUserProgramSummaryData(programEvents, baseline, currentTime, endOfProgram, useMetric)
        firstPageData = formatFirstPageData(useMetric)
        secondPageData = formatSecondPageData(useMetric, endOfProgram)

        // Obtain user's goal time and determine if it was automatically adjusted
        val (goalTime, goalTimeAdjusted) = fetchGoalTimeAndAdjustmentStatus()

        // Configure default UI state
        _uiState = MutableStateFlow(
            MmProgramSummaryState(
                firstPageData = firstPageData,
                secondPageData = secondPageData,
                isEndOfProgram = endOfProgram,
                currentPaceValue = currentPaceValue,
                estimatedRaceTime = estimatedRaceTime,
                goalTime = goalTime,
                goalTimeAdjusted = goalTimeAdjusted,
                estimatedTimeChange = estimatedTimeChange,
                estimatedPaceChange = estimatedPaceChange,
            )
        )
        uiState = _uiState.asStateFlow()
    }

    /**
     * Confirms the user's acceptance or rejection of the automatically adjusted goal time.
     *
     * @param goalTimeAdjustmentAccepted A Boolean indicating whether the user has accepted the
     *                                   automatically adjusted goal time (true if accepted, false otherwise).
     */
    fun confirmGoalTimeAdjustment(goalTimeAdjustmentAccepted: Boolean) {
        // Update user data with goal time adjustment confirmation
        if (goalTimeAdjustmentAccepted) {
            userInfoManager.setBackupGoalBaseline(userInfoManager.userInfo.goalBaselineKph)
        } else {
            userInfoManager.setGoalBaseline(userInfoManager.userInfo.backupGoalBaseline)
            userInfoManager.setAutoAdjustGoalBaseline(false)
        }
        
        userInfoManager.writeUserInfoToFile()

        // Update UI state to reflect goal time adjustment confirmation
        val (goalTime, goalTimeAdjusted) = fetchGoalTimeAndAdjustmentStatus()
        _uiState.update { mmProgramSummaryState ->
            mmProgramSummaryState.copy(
                goalTime = goalTime,
                goalTimeAdjusted = goalTimeAdjusted
            )
        }
    }

    /**
     * Fetches the user's goal race time and determines if the goal race time has been automatically adjusted.
     *
     * @return A Pair containing the formatted goal race time as a String and a Boolean indicating if the
     *         goal race time has been automatically adjusted (true if adjusted, false otherwise).
     */
    private fun fetchGoalTimeAndAdjustmentStatus(): Pair<String, Boolean> {
        // Get and format the user's goal time
        val goalTime = userInfoManager.userInfo.goalBaselineKph?.let {
            val (_, _, goalTimeSeconds) = program.workoutInterface.getRaceMetrics(it)
            secondsToHrMinSecString(goalTimeSeconds)
        } ?: "--"

        // Determine if user's goal time has been automatically adjusted
        val goalTimeAdjusted = userInfoManager.userInfo.goalBaselineKph != userInfoManager.userInfo.backupGoalBaseline

        return Pair(goalTime, goalTimeAdjusted)
    }

    /**
     * Calculates the summary data for the user's training program based on the provided workout events.
     *
     * @param programEvents The object containing all the events of the training program.
     * @param baseline The user's baseline running speed in kilometers per hour.
     * @param currentTime The current time in milliseconds.
     * @param isProgramComplete A Boolean indicating whether the training program is complete.
     */
    private fun calculateUserProgramSummaryData(
        programEvents: ProgramEvents,
        baseline: Float,
        currentTime: Long,
        isProgramComplete: Boolean,
        useMetric: Boolean
    ) {
        setEstimatedRaceTimeAndCurrentRacePace(baseline)
        programEvents.events
            .filterIsInstance<WorkoutEvent>()
            .forEach { event -> processWorkoutEvent(
                event = event,
                currentTime = currentTime,
                isProgramComplete = isProgramComplete,
                useMetric = useMetric
            ) }

        if (workoutsCompleted != 0) {
            averageSpeedKph = overallTotalAverageSpeedKph / workoutsCompleted
        }
    }

    private fun processWorkoutEvent(
        event: WorkoutEvent,
        currentTime: Long,
        isProgramComplete: Boolean,
        useMetric: Boolean
    ) {
        if (event.label == EventLabel.RACE) {
            processRaceEvent(event, isProgramComplete)
            return
        }

        if (event.getEventStatus(currentTime) == EventStatus.COMPLETED) workoutsCompleted++
        if (event.getEventStatus(currentTime) == EventStatus.MISSED) {
            workoutsMissed++
            return
        }

        if (event.completed) {
            accumulateWorkoutStats(event, isProgramComplete, useMetric)
        }
    }

    private fun processRaceEvent(event: WorkoutEvent, isProgramComplete: Boolean) {
        if (!isProgramComplete) return

        val raceTimeSeconds = event.workoutTimeMs / 1000
        val (_, _, assessmentRaceTimeSeconds) = program.workoutInterface.getRaceMetrics(userInfoManager.userInfo.assessmentBaselineKph)
        val raceFinished = event.workoutDistanceKm >= 5f

        // TODO: update to support programs other than 5k when added back in.
        estimatedRaceTimeSec    = if (raceFinished) raceTimeSeconds else 0
        currentRacePaceSecPerKm = if (raceFinished) (raceTimeSeconds / event.workoutDistanceKm).roundToInt() else 0
        estimatedTimeChange     = if (raceFinished) raceTimeSeconds - assessmentRaceTimeSeconds else 0
    }

    private fun accumulateWorkoutStats(
        event: WorkoutEvent,
        isProgramComplete: Boolean,
        useMetric: Boolean
    ) {
        totalWorkoutTimeSec += event.totalTimeSec
        totalCalories       += event.totalCalories
        totalDistanceKm     += event.totalDistanceKm
        totalElevationMeters += event.totalElevationMeters
        overallTotalAverageSpeedKph += event.totalAverageSpeedKph

        setPeakSpeed(event.totalPeakSpeedKph)
        setStartingAndBestRacePace(event.baselineKph)
        setHeartRateValues(event.totalAverageHR)

        if (!isProgramComplete) {
            estimatedTimeChange = estimatedTimeChange(event.baselineKph)
            estimatedPaceChange = estimatePaceChange(event.baselineKph, useMetric)
        }
    }

    /**
     * Updates the peak speed if the provided speed is higher than the current peak speed.
     *
     * @param speed The peak speed recorded in kilometers per hour during a workout event.
     */
    private fun setPeakSpeed(speed: Float) {
        if (peakSpeedKph < speed) {
            peakSpeedKph = speed
        }
    }

    /**
     * Sets the estimated race time and current race pace based on the provided baseline speed.
     *
     * @param baseline The user's baseline running speed in kilometers per hour.
     */
    private fun setEstimatedRaceTimeAndCurrentRacePace(baseline: Float) {
        val (racePaceSecPerKm, _, raceTimeSeconds) = program.workoutInterface.getRaceMetrics(baseline)
        estimatedRaceTimeSec = raceTimeSeconds
        currentRacePaceSecPerKm = racePaceSecPerKm
    }

    /**
     * Sets the starting race pace and the best race pace based on the provided baseline speed.
     *
     * @param baseline The user's baseline running speed in kilometers per hour.
     */
    private fun setStartingAndBestRacePace(baseline: Float) {
        val (racePaceSecPerKm, _, _) = program.workoutInterface.getRaceMetrics(baseline)
        if (workoutsCompleted == 1) {
            startingRacePaceSecPerKm = racePaceSecPerKm
        }

        if (bestRacePaceSecPerKm == 0 || bestRacePaceSecPerKm > racePaceSecPerKm) {
            bestRacePaceSecPerKm = racePaceSecPerKm
            if (bestRacePaceSecPerKm > currentRacePaceSecPerKm){
                bestRacePaceSecPerKm = currentRacePaceSecPerKm
            }
        }
    }

    /**
     * Sets the heart rate values based on the provided average heart rate.
     *
     * @param totalAverageHR The average heart rate recorded during a workout event.
     */
    private fun setHeartRateValues(totalAverageHR: Int) {
        if (totalAverageHR > minDisplayedHeartRate) {
            count++
            overallTotalAverageHR += totalAverageHR
            overallAverageHR = overallTotalAverageHR / count

            if (averagePeakHR < totalAverageHR) {
                averagePeakHR = totalAverageHR
            }
        }
    }

    /**
     * Estimates the change in race time based on the provided baseline speed.
     *
     * @param baseline The baseline running speed in kilometers per hour.
     * @return The difference in estimated race time in seconds.
     */
    private fun estimatedTimeChange(baseline: Float): Int{
        val (_, _, raceTimeSeconds) = program.workoutInterface.getRaceMetrics(baseline)
        var estimatedTimeDiff = 0
        if (estimatedRaceTimeSec > 0 && raceTimeSeconds > 0) {
            estimatedTimeDiff = estimatedRaceTimeSec - raceTimeSeconds //this compares the current estimated time and the last estimated time
        }
        return estimatedTimeDiff
    }

    /**
     * Estimates changes in pace based on the provided baseline speed
     */
    private fun estimatePaceChange(baseline: Float, useMetric: Boolean): Int {
        val (paceSecPerKm, _, _) = program.workoutInterface.getRaceMetrics(baseline)
        var estimatedTimeDiff = 0
        if (currentRacePaceSecPerKm > 0 && paceSecPerKm > 0) {
            estimatedTimeDiff = if (useMetric) {
                currentRacePaceSecPerKm - paceSecPerKm // this compares the current estimated time and the last estimated time
            } else {
                convertToMilePace(currentRacePaceSecPerKm) - convertToMilePace(paceSecPerKm)
            }
        }
        return estimatedTimeDiff
    }

    private fun convertToMilePace(secPerKm: Int) : Int {
        return secPerKm.toFloat().secondsPerKmPaceUnit.toSecondsPerMilePace.toInt()
    }

    /**
     * Formats the first page of the program summary data for display.
     *
     * @param isMetric A Boolean indicating whether the metric system is used (true for metric, false for imperial).
     * @return A list of `ProgramSummaryLine` objects for the first page of the program summary.
     */
    private fun formatFirstPageData(isMetric: Boolean): List<ProgramSummaryLine> {
        val totalDistanceUnit = if (isMetric) R.string.kilometers_uc else R.string.miles_uc
        val totalCaloriesUnit = R.string.kcal_uc
        val completedWorkoutsFormat = R.string.completed_workouts_format_uc
        return listOf(
            ProgramSummaryLine(
                R.string.total_workout_time_uc,
                secondsToHrMinSecString(totalWorkoutTimeSec)
            ),
            ProgramSummaryLine(
                R.string.total_distance_uc, UnitFormatter.formatDistance(
                    totalDistanceKm,
                    isMetric
                ), totalDistanceUnit
            ),
            ProgramSummaryLine(
                R.string.total_calories_uc, UnitFormatter.formatCalories(
                    totalCalories
                ), totalCaloriesUnit
            ),
            ProgramSummaryLine(
                R.string.completed_workouts_uc, UnitFormatter.formatInt(
                    workoutsCompleted
                ), completedWorkoutsFormat
            )
        )
    }

    /**
     * Formats the second page of the program summary data for display.
     *
     * @param isMetric A Boolean indicating whether the metric system is used (true for metric, false for imperial).
     * @param showEndOfProgram A Boolean indicating whether the program has ended (true if the program has ended, false otherwise).
     * @return A list of `ProgramSummaryLine` objects for the second page of the program summary.
     */
    private fun formatSecondPageData(
        isMetric: Boolean,
        showEndOfProgram: Boolean
    ): List<ProgramSummaryLine> {
        val distanceUnit = if (isMetric) R.string.kilometers_lc else R.string.miles_lc
        val elevationUnit = if (isMetric) R.string.meters_lc else R.string.feet_lc
        val speedUnit = if (isMetric) R.string.kph_lc else R.string.mph_lc
        val heartRateUnit = R.string.bpm_lc

        // Show current pace in sec/km for metric and sec/mi for imperial
        currentPaceValue = if (currentRacePaceSecPerKm > 0) {
            val convertedRacePace = if (isMetric) {
                currentRacePaceSecPerKm
            } else {
                (currentRacePaceSecPerKm * 1.609344f).toInt()
            }
            secondsToHrMinSecString(convertedRacePace)
        } else "--"

        // Show starting pace in sec/km for metric and sec/mi for imperial
        val convertedStartingRacePace = if (isMetric) {
            startingRacePaceSecPerKm
        } else {
            (startingRacePaceSecPerKm * 1.609344f).toInt()
        }

        // Show best pace in sec/km for metric and sec/mi for imperial
        val convertedBestRacePace = if (isMetric) {
            bestRacePaceSecPerKm
        } else {
            (bestRacePaceSecPerKm * 1.609344f).toInt()
        }

        estimatedRaceTime = if (estimatedRaceTimeSec > 0) secondsToHrMinSecString(estimatedRaceTimeSec) else "--"

        // TODO: will need to update program summary line titles to be program-independent
        return listOf(
            ProgramSummaryLine(
                R.string.workout_completed_tc,
                UnitFormatter.formatInt(workoutsCompleted)
            ),
            ProgramSummaryLine(
                R.string.workouts_missed_tc,
                UnitFormatter.formatInt(workoutsMissed)
            ),
            ProgramSummaryLine(
                R.string.total_workout_time_tc,
                secondsToHrMinSecString(totalWorkoutTimeSec)
            ),
            ProgramSummaryLine(
                R.string.total_calories_tc,
                UnitFormatter.formatCalories(totalCalories)
            ),
            ProgramSummaryLine(
                R.string.total_distance_tc,
                UnitFormatter.formatDistance(totalDistanceKm, isMetric),
                distanceUnit
            ),
            ProgramSummaryLine(
                R.string.average_speed_tc,
                UnitFormatter.formatSpeed(averageSpeedKph, isMetric),
                speedUnit
            ),
            ProgramSummaryLine(
                R.string.peak_speed_tc,
                UnitFormatter.formatSpeed(peakSpeedKph, isMetric),
                speedUnit
            ),
            ProgramSummaryLine(
                if (showEndOfProgram) R.string.final_five_k_time_tc else R.string.estimated_five_k_time_tc,
                estimatedRaceTime
            ),
            ProgramSummaryLine(
                if (showEndOfProgram) R.string.finishing_pace_uc else R.string.current_five_k_pace_tc,
                currentPaceValue
            ),
            ProgramSummaryLine(
                R.string.starting_five_k_pace_tc,
                UnitFormatter.formatPace(convertedStartingRacePace)
            ),
            ProgramSummaryLine(
                R.string.best_five_k_pace_tc,
                UnitFormatter.formatPace(convertedBestRacePace)
            ),
            ProgramSummaryLine(
                R.string.overall_average_hr_tc,
                UnitFormatter.formatHeartRate(overallAverageHR),
                heartRateUnit
            ),
            ProgramSummaryLine(
                R.string.highest_average_hr_tc,
                UnitFormatter.formatHeartRate(averagePeakHR),
                heartRateUnit
            ),
            ProgramSummaryLine(
                R.string.total_elevation_tc,
                UnitFormatter.formatElevation(totalElevationMeters, isMetric),
                elevationUnit
            )
        )
    }

    companion object {
        /**This function provides a factory for creating instances of ProgramSummaryViewModel*/
        fun provideFactory(
            userInfoManager: UserInfoManager,
            useMetric: Boolean,
            program: Program,
            isEndOfProgram: Boolean? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProgramSummaryViewModel(userInfoManager, program, useMetric, isEndOfProgram) as T
            }
        }
    }

    /**
     * Represents a line of data in the program summary.
     *
     * @property title The resource ID for the title of the summary line.
     * @property value The formatted value of the summary line.
     * @property unit The optional resource ID for the unit of the summary line. Default is null.
     */
    data class ProgramSummaryLine(
        val title: Int,
        val value: String,
        val unit: Int? = null
    )

    /**
     * Represents the UI state of the program summary screen.
     *
     * @property firstPageData A list of `ProgramSummaryLine` objects representing the data for the first page of the program summary screen.
     * @property secondPageData A list of `ProgramSummaryLine` objects representing the data for the second page of the program summary screen.
     * @property isEndOfProgram A Boolean indicating whether the end of the program is shown.
     * @property currentPaceValue The user's current pace as a formatted string.
     * @property estimatedRaceTime The user's current estimated race time as a formatted string.
     * @property goalTime The user's goal race time as a formatted string.
     * @property goalTimeAdjusted A Boolean indicating whether the user's goal race time has been automatically adjusted.
     * @property estimatedTimeChange The estimated change in race time in seconds.
     */
    data class MmProgramSummaryState(
        val firstPageData: List<ProgramSummaryLine>,
        val secondPageData: List<ProgramSummaryLine>,
        val isEndOfProgram: Boolean,
        val currentPaceValue: String,
        val estimatedRaceTime: String,
        val goalTime: String,
        val goalTimeAdjusted: Boolean,
        val estimatedTimeChange: Int,
        val estimatedPaceChange: Int,
    )
}

/**
 * Checks if the last event in the program is either completed or missed.
 *
 * @param programEvents The object containing all the events of the training program.
 * @param currentTime The current time in milliseconds used to determine the status of events.
 * @return A Boolean indicating whether the last event is either completed or missed (true if completed or missed, false otherwise).
 */
fun isLastEventCompletedOrMissed(programEvents: ProgramEvents, currentTime: Long): Boolean {
    return if (programEvents.events.isNotEmpty()) {
        val lastEventStatus = programEvents.events.last().getEventStatus(currentTime)
        lastEventStatus == EventStatus.COMPLETED || lastEventStatus == EventStatus.MISSED
    } else {
        false
    }
}



