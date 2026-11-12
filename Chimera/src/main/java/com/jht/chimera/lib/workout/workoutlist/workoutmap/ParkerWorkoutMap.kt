package com.jht.chimera.lib.workout.workoutlist.workoutmap

import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.config.frames.FrameConfig
import com.jht.chimera.lib.workout.WorkoutIds
import com.jht.chimera.lib.workoutapps.AppIds

/**
 * Maps workout IDs and workout info to each frame
 */
class ParkerWorkoutMap : WorkoutMap() {
    override fun loadTreadmillWorkouts(config: FrameConfig) {
        loadStandardWorkouts(isTreadmill = true)
        addWorkoutItemById(WorkoutIds.ROLLING_HILLS)

        // Parker treadmill for 10" do not have THR incline
        if (ProductConfig.getConsoleType().inches != 10) {
            addWorkoutItemById(WorkoutIds.THR_INCLINE)
        }
        addWorkoutItemById(WorkoutIds.THR_SPEED)
    }

    override fun loadBikeWorkouts(config: FrameConfig) {
        loadStandardWorkouts()
        // Parker bike for 10" does not have rolling hills
        if (ProductConfig.getConsoleType().inches != 10) {
            addWorkoutItemById(WorkoutIds.ROLLING_HILLS)
        }
        if (config.WATTS_INACCURATE) {
            // fat burn is for 30 series since it cannot show constant watts
            addWorkoutItemById(WorkoutIds.FAT_BURN)
        } else {
            addWorkoutItemById(WorkoutIds.CONSTANT_WATTS)
        }
        addWorkoutItemById(WorkoutIds.THR_RESISTANCE)
    }

    override fun loadEllipticalWorkouts(config: FrameConfig) {
        loadStandardWorkouts()
        // Parker elliptical for 10" does not have rolling hills
        if (ProductConfig.getConsoleType().inches != 10) {
            addWorkoutItemById(WorkoutIds.ROLLING_HILLS)
        }
        if (config.WATTS_INACCURATE) {
            // fat burn is for 30 series since it cannot show constant watts
            addWorkoutItemById(WorkoutIds.FAT_BURN)
        } else {
            addWorkoutItemById(WorkoutIds.CONSTANT_WATTS)
        }
        addWorkoutItemById(WorkoutIds.THR_RESISTANCE)
    }

    override fun loadAscentWorkouts(config: FrameConfig) {
        loadStandardWorkouts()
        // Parker ascent for 10" does not have glutes
        if (ProductConfig.getConsoleType().inches != 10) {
            addWorkoutItemById(WorkoutIds.GLUTES)
        }
        if (config.WATTS_INACCURATE) {
            // fat burn is for 30 series since it cannot show constant watts
            addWorkoutItemById(WorkoutIds.FAT_BURN)
        } else {
            addWorkoutItemById(WorkoutIds.CONSTANT_WATTS)
        }
        addWorkoutItemById(WorkoutIds.THR_RESISTANCE)
    }

    override fun loadClimbmillWorkouts(config: FrameConfig) {
        loadStandardWorkouts(isClimbmill = true)
        // Parker climbmill for 10" does not have rolling hills
        if (ProductConfig.getConsoleType().inches != 10) {
            addWorkoutItemById(WorkoutIds.ROLLING_HILLS)
        }
        addWorkoutItemById(WorkoutIds.FAT_BURN)
        addWorkoutItemById(WorkoutIds.THR_RESISTANCE)
    }

    override fun loadStepperWorkouts(config: FrameConfig) {
        loadStandardWorkouts(isClimbmill = true)
        addWorkoutItemById(WorkoutIds.ROLLING_HILLS)
        addWorkoutItemById(WorkoutIds.FAT_BURN)

        addWorkoutItemById(WorkoutIds.THR_RESISTANCE)
    }

    override fun loadSpinBikeWorkouts(config: FrameConfig) {
        //Retail Spin Bike (ICR70) is currently the same as Commercial (Athena)
        loadSpecialAndThirdPartyWorkouts()
        addWorkoutItemById(WorkoutIds.MANUAL, optionalBackground = "cover_images_manual.jpg")
        // CGC-25017: default workout app like commercial spin, target training replaces the individual run screens
        addWorkoutItemById(WorkoutIds.ROLLING_HILLS, optionalBackground = "cover_images_target_training.jpg")
        addWorkoutItemById(WorkoutIds.INTERVALS, optionalAppId = AppIds.INTERVALS_UUID, optionalBackground = "cover_images_intervals.jpg")
        addWorkoutItemById(WorkoutIds.FAT_BURN)
        addWorkoutItemById(WorkoutIds.TARGET_TRAINING)
        addWorkoutItemById(WorkoutIds.PACE_TIME_GOAL)
        addWorkoutItemById(WorkoutIds.PACE_DISTANCE_GOAL)
        addWorkoutItemById(WorkoutIds.CONSTANT_WATTS)
        addWorkoutItemById(WorkoutIds.THR_RESISTANCE, optionalBackground = "cover_images_thr_resistance.jpg")

        addWorkoutItemById(WorkoutIds.TIME_GOAL, optionalBackground = "cover_images_time_goal.jpg")
        addWorkoutItemById(WorkoutIds.DISTANCE_GOAL, optionalBackground = "cover_images_distance_goal.jpg")
        addWorkoutItemById(WorkoutIds.CALORIES_GOAL, optionalBackground = "cover_images_calories_goal.jpg")

        addWorkoutItemById(WorkoutIds.SPRINT8)
        addWorkoutItemById(WorkoutIds.VIRTUAL_ACTIVE)

        addWorkoutItemById(WorkoutIds.FTP_TEST_20)
        addWorkoutItemById(WorkoutIds.FTP_TEST_8)
        addWorkoutItemById(WorkoutIds.COOPER_FITNESS_TEST)
        addWorkoutItemById(WorkoutIds.MAX_HR_TEST)
    }

    /**
     * Workouts that every frame has
     */
    private fun loadStandardWorkouts(isClimbmill: Boolean = false, isTreadmill : Boolean = false) {
        addWorkoutItemById(WorkoutIds.MANUAL)
        addWorkoutItemById(WorkoutIds.INTERVALS, isTreadmill) //isTreadmill for incline in title and description

        addWorkoutItemById(WorkoutIds.TIME_GOAL)
        addWorkoutItemById(WorkoutIds.CALORIES_GOAL)
        if(isClimbmill){
            addWorkoutItemById(WorkoutIds.FLOORS_GOAL)
            addWorkoutItemById(WorkoutIds.LANDMARKS)
        }else{
            addWorkoutItemById(WorkoutIds.DISTANCE_GOAL)
            addWorkoutItemById(WorkoutIds.SPRINT8)
        }

        addWorkoutItemById(WorkoutIds.VIRTUAL_ACTIVE)

        loadSpecialAndThirdPartyWorkouts()
    }

    /**
     * Workouts for integrations and third parties
     */
    private fun loadSpecialAndThirdPartyWorkouts() {
        if (!ProductConfig.hasPremiumUi()) { // new UI follows commercial where custom is separate of training list
            //Custom
            addWorkoutItemById(WorkoutIds.CUSTOM_WORKOUT)
        }

        //Marathon
        addWorkoutItemById(WorkoutIds.MARATHON)

        //HIIT
        addWorkoutItemById(WorkoutIds.HIIT)

        //FitPark (Migros)
        addWorkoutItemById(WorkoutIds.MIGROS_POTENTIAL)
        addWorkoutItemById(WorkoutIds.MIGROS_EXHAUSTION)
        addWorkoutItemById(WorkoutIds.MIGROS_FATIGUE)

        //EGYM
        addWorkoutItemById(WorkoutIds.EGYM)

        //iFIT
        addWorkoutItemById(WorkoutIds.IFIT)
    }
}