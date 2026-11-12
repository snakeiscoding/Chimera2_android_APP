package com.jht.progression.data

class WorkoutPerformanceMetric(
    workoutEvent: WorkoutEvent,
    program: Program
) {
    // Static Variables & Constants
    companion object {
        private const val MILLISECONDS_PER_HOUR = 3_600_000F
    }

    // Public instance variables
    val avgWorkoutSpeedDeviation: Float
    val workoutDistanceDeviation: Float
    val totalWorkoutDeviation: Float

    // Private Instance Variables
    private val expectedWorkoutTimeMs: Int
    private val expectedWorkoutAvgSpeedKph: Float
    private val expectedWorkoutDistanceKm: Float
    private val actualWorkoutAvgSpeedKph: Float
    private val actualWorkoutDistanceKm: Float

    // Initialization block
    init {
        // Get workout info needed to compute performance metrics
        val workoutInfo = program.workoutInterface.getWorkout(workoutEvent.index, workoutEvent.baselineKph)

        // Compute expected workout time and distance
        calculateExpectedWorkoutTotals(
            intervals = workoutInfo.intervalList,
            workouts = program.workoutInterface,
            baseline = workoutEvent.baselineKph
        ).let { (workoutTimeMs, workoutDistanceKm) ->
            expectedWorkoutTimeMs = workoutTimeMs
            expectedWorkoutDistanceKm = workoutDistanceKm
        }

        // Compute expected workout avg speed. Avg speed = distance (km) / time (hr)
        expectedWorkoutAvgSpeedKph = expectedWorkoutDistanceKm / (expectedWorkoutTimeMs / MILLISECONDS_PER_HOUR)

        // Compute actual workout avg speed and distance. Account for workout ending before workout stage
        if (workoutEvent.workoutTimeMs > 0) {
            actualWorkoutAvgSpeedKph = workoutEvent.workoutDistanceKm / (workoutEvent.workoutTimeMs / MILLISECONDS_PER_HOUR)
            actualWorkoutDistanceKm = workoutEvent.workoutDistanceKm
        } else {
            actualWorkoutAvgSpeedKph = 0F
            actualWorkoutDistanceKm = 0F
        }

        avgWorkoutSpeedDeviation = calculateAvgWorkoutSpeedDeviation()
        workoutDistanceDeviation = calculateWorkoutDistanceDeviation()
        totalWorkoutDeviation = calculateTotalWorkoutDeviation()
    }

    // Private helper methods for calculations
    private fun calculateAvgWorkoutSpeedDeviation(): Float =
        ((actualWorkoutAvgSpeedKph - expectedWorkoutAvgSpeedKph) / expectedWorkoutAvgSpeedKph) * 100

    private fun calculateWorkoutDistanceDeviation(): Float =
        ((actualWorkoutDistanceKm - expectedWorkoutDistanceKm) / expectedWorkoutDistanceKm) * 100

    private fun calculateTotalWorkoutDeviation(): Float =
        (avgWorkoutSpeedDeviation + workoutDistanceDeviation) / 2
}