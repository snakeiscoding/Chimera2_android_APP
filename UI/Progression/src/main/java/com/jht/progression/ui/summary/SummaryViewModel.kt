package com.jht.progression.ui.summary

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import com.jht.enums.SummaryStage
import com.jht.enums.SummaryStat
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.UserModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.interfaces.models.WorkoutSummaryModel
import com.jht.progression.interfaces.ProgressionModel
import com.jht.progression.R
import com.jht.progression.data.EventLabel
import com.jht.progression.data.CompletedWorkoutStats
import com.jht.progression.data.Program
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutCompletionResult
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.data.WorkoutPerformanceMetric
import com.jht.progression.data.applyWorkoutCompletion
import com.jht.progression.data.calculateExpectedWorkoutAverageSpeed
import com.jht.progression.data.calculateExpectedWorkoutTotals
import com.jht.progression.data.getProgramOfType
import com.jht.progression.ui.setup.journey.EventStatus
import com.jht.progression.userinfo.UserInfoManager
import com.jht.unit.UnitFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@HiltViewModel
class SummaryViewModel @Inject constructor(
    summaryModel: WorkoutSummaryModel,
    private val progressionModel: ProgressionModel,
    userModel: UserModel,
    private val navigationModel: NavigationModel,
    workoutModel: WorkoutModel,
) : ViewModel() {
    private val program: Program
    val isMetric: Boolean = workoutModel.isMetric.value //TODO: set this value correctly
    private val workoutDurationSec: Float
    private val totalDurationSec: Float
    private val totalDistanceKm: Float
    private val workoutAverageSpeedKph: Float
    private val workoutAverageIncline: Float
    private val totalAverageIncline: Float
    val workoutCalories: Float
    val workoutDistanceKm: Float
    val stats: Map<Int, StatValue>
    val titleTextId: Int
    val subTextId: Int
    val assessmentIncomplete: Boolean

    private val totalTimeSec: Float
    private val totalCalories: Float
    private val totalAverageSpeedKph: Float
    private val totalPeakSpeedKph: Float
    private val totalAverageHR: Float
    private val totalElevationMeters: Float
    private val workoutBaseLineKph: Float
    private val workoutGoalTimeExisted: Boolean

    private val userInfoManager = UserInfoManager(userModel, progressionModel)

    init {
        val programType = progressionModel.eventIndex.value.programType
        program = getProgramOfType(programType)
        userInfoManager.loadUserInfo()
        workoutBaseLineKph = userInfoManager.userInfo.baselineKph
        workoutGoalTimeExisted = userInfoManager.userInfo.goalBaselineKph != null

        workoutDurationSec = summaryModel.getWorkoutStat(SummaryStat.TIME_ELAPSED, SummaryStage.WORKOUT)
        totalDurationSec = summaryModel.getWorkoutStat(SummaryStat.TIME_ELAPSED, SummaryStage.TOTAL)
        totalDistanceKm = summaryModel.getWorkoutStat(SummaryStat.DISTANCE, SummaryStage.TOTAL)
        workoutCalories = summaryModel.getWorkoutStat(SummaryStat.CALORIES, SummaryStage.WORKOUT)
        workoutAverageSpeedKph = summaryModel.getWorkoutStat(SummaryStat.AVERAGE_SPEED, SummaryStage.WORKOUT)
        workoutAverageIncline = summaryModel.getWorkoutStat(SummaryStat.AVERAGE_INCLINE, SummaryStage.WORKOUT)
        workoutDistanceKm = summaryModel.getWorkoutStat(SummaryStat.DISTANCE, SummaryStage.WORKOUT)
        totalTimeSec = summaryModel.getWorkoutStat(SummaryStat.TIME_ELAPSED, SummaryStage.TOTAL)
        totalCalories = summaryModel.getWorkoutStat(SummaryStat.CALORIES, SummaryStage.TOTAL)
        totalAverageSpeedKph = summaryModel.getWorkoutStat(SummaryStat.AVERAGE_SPEED, SummaryStage.TOTAL)
        totalPeakSpeedKph = summaryModel.getWorkoutStat(SummaryStat.PEAK_SPEED_KPH, SummaryStage.TOTAL)
        totalAverageHR = summaryModel.getWorkoutStat(SummaryStat.AVERAGE_HEART_RATE, SummaryStage.TOTAL)
        totalElevationMeters = summaryModel.getWorkoutStat(SummaryStat.ELEVATION, SummaryStage.TOTAL)
        totalAverageIncline = summaryModel.getWorkoutStat(SummaryStat.AVERAGE_INCLINE, SummaryStage.TOTAL)

        var shouldWrite = false

        if (!isAssessment()) {
            //Workout Summary
            shouldWrite = setWorkoutComplete()
            var baseline = userInfoManager.userInfo.baselineKph
            baseline = adjustBaseline(baseline, userInfoManager.userInfo.programEvents, program)
            var goalBaseline = userInfoManager.userInfo.goalBaselineKph
            val autoAdjustGoalBaseline = userInfoManager.userInfo.autoAdjustGoalBaseline
            if (autoAdjustGoalBaseline && program.workoutInterface.isGoalTimeChangePermitted(userInfoManager) && goalBaseline != null) {
                val adjustedGoalBaseline = adjustGoalBaseline(baseline, goalBaseline, userInfoManager.userInfo.programEvents, program)
                // Only save goal baseline if it has changed as this will recalculate the goal increment
                if (goalBaseline != adjustedGoalBaseline) {
                    goalBaseline = adjustedGoalBaseline
                    userInfoManager.setGoalBaseline(goalBaseline)
                }
            }
            if (userInfoManager.getNumOfCompletedWorkouts(true) in program.workoutInterface.goalAdjustmentWorkoutRange) {
                baseline = addGoalIncrement(baseline)
                userInfoManager.setBaseline(baseline)
            }

            titleTextId = R.string.workout_complete_exclamation_uc
            subTextId = 0 //0 is not a valid resource ID, this will not be displayed
            stats = getWorkoutStats()
            assessmentIncomplete = false

        } else {

            //Assessment Summary
            val assessmentBaseline = assessmentBaseline(workoutDurationSec, workoutAverageSpeedKph)
            stats = getAssessmentStats(assessmentBaseline)
            assessmentIncomplete = assessmentIncomplete(workoutAverageSpeedKph)
            if (assessmentIncomplete) {
                titleTextId = R.string.assessment_incomplete_uc
                subTextId = R.string.return_assessment_setup_text1_pg
            } else {
                titleTextId = R.string.congratulations_uc
                subTextId = R.string.access_progression_journey_pg
                userInfoManager.setBaseline(assessmentBaseline)
                if (userInfoManager.userInfo.assessmentBaselineKph == 0f) {
                    userInfoManager.setAssessmentBaseline(assessmentBaseline)
                }
                userInfoManager.setGoalBaseline(null)
                shouldWrite = true
            }
        }
        if (shouldWrite) userInfoManager.writeUserInfoToFile()
    }


    private fun assessmentBaseline(workoutDurationSec: Float, workoutAverageSpeed: Float): Float{
        val fullWorkoutTimeSec = 15 * 60f //the time of full assessment workout is 15 minutes

        // If the 15 minutes assessment workout doesn't complete, the baseline will have
        // 70% of the average speed. Otherwise, the baseline will equal to the average speed
        val assessmentBaseline = if (workoutDurationSec < fullWorkoutTimeSec){
            workoutAverageSpeed * 0.7f
        }else{
            workoutAverageSpeed
        }
        return assessmentBaseline.coerceIn(
            minimumValue = program.workoutInterface.maxRaceTimeBaseline,
            maximumValue = program.workoutInterface.minRaceTimeBaseline
        )
    }

    private fun assessmentIncomplete(workoutAverageSpeed: Float): Boolean{
        // Incomplete assessment scene only shows if user doesn't finish the warmup.
        return workoutAverageSpeed <= 0f
    }

    fun navigateToSplashScreen() {
        navigationModel.launchSplashScreen()
    }

    /**
     * Gets if the program is completed to show the program complete screen
     */
    fun isProgramCompleted() : Boolean {
        // If this is the summary of the final event in the program
        val programSize = userInfoManager.userInfo.programEvents.events.size
        return progressionModel.eventIndex.value.eventIndex >= programSize - 1
    }

    fun isAssessment(): Boolean {
        // Event index will be -1 if it's an assessment
        return progressionModel.eventIndex.value.eventIndex < 0
    }

    /** False if the event couldn't be marked complete, so the caller skips persisting. */
    private fun setWorkoutComplete(): Boolean {
        val index = progressionModel.eventIndex.value.eventIndex
        return when (val result = applyWorkoutCompletion(index, userInfoManager.userInfo.programEvents, completedWorkoutStats())) {
            is WorkoutCompletionResult.Applied -> {
                userInfoManager.setProgramEvents(result.events)
                true
            }
            is WorkoutCompletionResult.IndexOutOfRange -> {
                Log.e("SummaryViewModel", "Event index ${result.index} invalid for ${result.eventCount} events, skipping completion")
                false
            }
        }
    }

    private fun completedWorkoutStats() = CompletedWorkoutStats(
        workoutDistanceKm = workoutDistanceKm,
        workoutTimeMs = (workoutDurationSec * 1000).toInt(),
        totalTimeSec = totalTimeSec.toInt(),
        totalDistanceKm = totalDistanceKm,
        totalCalories = totalCalories,
        totalAverageSpeedKph = totalAverageSpeedKph,
        totalPeakSpeedKph = totalPeakSpeedKph,
        totalAverageHR = totalAverageHR.toInt(),
        totalElevationMeters = totalElevationMeters,
        baselineKph = workoutBaseLineKph,
        goalTimeExisted = workoutGoalTimeExisted,
    )

    /**
     * Adds the amount needed for the goal to be achieved over the course of the program
     */
    private fun addGoalIncrement(baseline: Float) : Float {
        val goalIncrement = userInfoManager.userInfo.goalIncrementKph

        if (goalIncrement != null) {
            Log.d("SummaryViewModel", "Adding $goalIncrement KPH to baseline for goal adjustment")
            return baseline + goalIncrement
        }

        return baseline
    }

    private fun getWorkoutStats(): Map<Int, StatValue> {
        val distanceUnit = if (isMetric) R.string.km_uc else R.string.mi_uc
        val paceUnit = if (isMetric) R.string.kph_uc else R.string.mph_uc

        return mapOf(
            R.string.total_duration_uc to StatValue(UnitFormatter.formatTimeSeconds(totalDurationSec.roundToInt())),
            R.string.total_distance_uc to StatValue(UnitFormatter.formatDistance(totalDistanceKm, isMetric, 1), distanceUnit),
            R.string.average_speed_uc to StatValue(UnitFormatter.formatSpeed(totalAverageSpeedKph, isMetric), paceUnit),
            R.string.average_incline_tc to StatValue(UnitFormatter.formatIncline(totalAverageIncline, true)),
            R.string.total_calories_uc to StatValue(UnitFormatter.formatCalories(totalCalories)),
        )
    }

    data class StatValue(val value: String, val unit: Int? = null)

    private fun getAssessmentStats(assessmentBaseline: Float): Map<Int, StatValue> {
        val (_, raceSpeedKph, raceTimeSeconds) = program.workoutInterface.getRaceMetrics(assessmentBaseline)

        val distanceUnit = if (isMetric) R.string.km_uc else R.string.mi_uc
        val workoutPaceString = if (isMetric) R.string.estimated_km_time_uc else R.string.estimated_mile_time_uc


        // Assessment has workout distance, race pace, 5k time
        return mapOf(
            R.string.active_distance_uc to StatValue(UnitFormatter.formatDistance(workoutDistanceKm, isMetric, 1), distanceUnit),
            workoutPaceString to StatValue(UnitFormatter.formatPace(raceSpeedKph, isMetric)),
            R.string.estimated_five_k_time_uc to StatValue(UnitFormatter.formatTimeSeconds(raceTimeSeconds)),
        )
    }
}

fun adjustGoalBaseline(
    baseline: Float,
    goalBaseline: Float,
    programEvents: ProgramEvents,
    program: Program
): Float {
    val numOfWorkoutsUsed = 2
    val goalTimeScalingFactor = 0.1F
    val workoutDeviationThreshold = -10F
    val workoutDeviations = programEvents.events
        .filterIsInstance<WorkoutEvent>()
        .filter {it.goalTimeExisted && it.completed && it.label != EventLabel.RACE }
        .takeLast(numOfWorkoutsUsed)
        .map { WorkoutPerformanceMetric(it, program).totalWorkoutDeviation }
    val adjustUserGoalSetting = workoutDeviations.size == numOfWorkoutsUsed && workoutDeviations.all { it <= workoutDeviationThreshold }
    if (adjustUserGoalSetting) {
        val absoluteDeviations = workoutDeviations.map { abs(it) }
        val avgDeviation = absoluteDeviations.average().toFloat()
        val maxDeviation = absoluteDeviations.max()
        val normalizedDeviation = avgDeviation / maxDeviation
        val (_, _, raceTimeSeconds) = program.workoutInterface.getRaceMetrics(baseline)
        val (_, _, goalTimeSeconds) = program.workoutInterface.getRaceMetrics(goalBaseline)
        val newGoalTime = goalTimeSeconds * (1 + goalTimeScalingFactor * normalizedDeviation)
        val maxRaceTimeReductionSeconds = (((raceTimeSeconds / 60) / 10).coerceIn(1, 10)) * 60
        val clampedGoalTime = newGoalTime.coerceIn((raceTimeSeconds - maxRaceTimeReductionSeconds).toFloat(), raceTimeSeconds.toFloat()-1)
        return program.workoutInterface.computeBaselineFromWorkoutTime(clampedGoalTime.roundToInt())
    }
    return goalBaseline
}


/**
 * Takes the most recent "numWorkoutsUsed" workouts and checks if they are all either above or
 * below by at least "distanceNeeded" KPH. If all "numWorkoutsUsed" workouts are above
 * "distanceNeeded" the baseline is increased by "adjustmentSize". If all "numWorkoutsUsed"
 * workouts are below "distanceNeeded" the baseline is decreased by "adjustmentSize". As soon as
 * it is found one or more are not consecutively above or below, or there is not a difference
 * greater than "distanceNeeded", it will return with no adjustment to the baseline.
 *
 * Returns when either the baseline should not update, the baseline has increased, or the
 * baseline has decreased
 */
@VisibleForTesting
fun adjustBaseline(baseline: Float, programEvents: ProgramEvents, program: Program) : Float {
    val numWorkoutsUsed = 2
    val speedNeeded = 0.1f //kph
    val workout = program.workoutInterface
    var distanceDifferenceCount = 0
    var totalDelta = 0f
    val minBaseline = 1f

    programEvents.events.asReversed().forEach { event ->
        if (event is WorkoutEvent) {
            if (event.getEventStatus(System.currentTimeMillis()) == EventStatus.MISSED) return baseline
            if (event.completed) {
                val workoutInfo = workout.getWorkout(event.index, baseline)
                val (expectedFullWorkoutTime, _) = calculateExpectedWorkoutTotals(workoutInfo.intervalList, workout, baseline)
                val averageExpectedSpeed = calculateExpectedWorkoutAverageSpeed(workoutInfo.intervalList, workout, baseline, event.workoutTimeMs)
                val workoutTimeRequired = expectedFullWorkoutTime * 0.7

                //If the logic below is implemented, a workout should finish more than 70%.
                if (event.workoutTimeMs > workoutTimeRequired) {
                    val averageActualSpeed =
                        event.workoutDistanceKm / (event.workoutTimeMs / 3600000f)
                    val averageSpeedDifference = averageActualSpeed - averageExpectedSpeed

                    if (averageSpeedDifference > speedNeeded && distanceDifferenceCount >= 0) {
                        totalDelta += averageSpeedDifference
                        if (numWorkoutsUsed == ++distanceDifferenceCount) {
                            val adjustmentSize = totalDelta / numWorkoutsUsed * 0.7f
                            return (baseline + adjustmentSize).coerceAtLeast(minBaseline)
                        }
                    } else if (averageSpeedDifference < -speedNeeded && distanceDifferenceCount <= 0) {
                        totalDelta += averageSpeedDifference
                        if (-numWorkoutsUsed == --distanceDifferenceCount) {
                            val adjustmentSize = totalDelta / numWorkoutsUsed * 0.7f
                            return (baseline + adjustmentSize).coerceAtLeast(minBaseline)
                        }
                    } else {
                        return baseline
                    }
                } else {
                    return baseline
                }
            }
        }
    }

    return baseline
}