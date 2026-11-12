package com.jht.chimera.lib.workout.workoutlist.workoutmap

import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.lib.common.config.frames.FrameConfig
import com.jht.chimera.lib.workout.WorkoutIds
import com.jht.chimera.lib.workoutapps.AppIds

/**
 * Maps workout IDs and workout info to each frame
 */
class ChimeraWorkoutMap : WorkoutMap() {
    override fun loadTreadmillWorkouts(config: FrameConfig) {
        loadStandardWorkouts(isTreadmill = true)
        addWorkoutItemById(WorkoutIds.HIIT)
        addWorkoutItemById(WorkoutIds.INTERVALS_SPEED)

        addWorkoutItemById(WorkoutIds.THR_INCLINE)
        addWorkoutItemById(WorkoutIds.THR_SPEED)
        addWorkoutItemById(WorkoutIds.HYBRID_TD_SWITCH_STRIDE)

        addWorkoutItemById(WorkoutIds.ARMY_FITNESS_TEST)
        addWorkoutItemById(WorkoutIds.MARINES_FITNESS_TEST)
        addWorkoutItemById(WorkoutIds.AIR_FORCE_FITNESS_TEST)
        addWorkoutItemById(WorkoutIds.NAVY_FITNESS_TEST)
        addWorkoutItemById(WorkoutIds.PEB_FITNESS_TEST)
        addWorkoutItemById(WorkoutIds.GERKIN_FITNESS_TEST)

        // Only on treadmill, should be on more modalities later
        addWorkoutItemById(WorkoutIds.PROGRESSIVE_WORKOUT)
    }

    override fun loadBikeWorkouts(config: FrameConfig) {
        loadStandardWorkouts()
        addWorkoutItemById(WorkoutIds.HIIT)
        addWorkoutItemById(WorkoutIds.CONSTANT_WATTS)
        addWorkoutItemById(WorkoutIds.THR_RESISTANCE)
        addWorkoutItemById(WorkoutIds.COOPER_FITNESS_TEST)
    }

    override fun loadEllipticalWorkouts(config: FrameConfig) {
        loadStandardWorkouts()
        addWorkoutItemById(WorkoutIds.HIIT)
        addWorkoutItemById(WorkoutIds.CONSTANT_WATTS)
        addWorkoutItemById(WorkoutIds.THR_RESISTANCE)
        addWorkoutItemById(WorkoutIds.COOPER_FITNESS_TEST)
    }

    override fun loadAscentWorkouts(config: FrameConfig) {
        loadStandardWorkouts()
        addWorkoutItemById(WorkoutIds.HIIT)
        addWorkoutItemById(WorkoutIds.CONSTANT_WATTS)
        addWorkoutItemById(WorkoutIds.GLUTES)

        addWorkoutItemById(WorkoutIds.THR_RESISTANCE)
        addWorkoutItemById(WorkoutIds.THR_INCLINE)

        addWorkoutItemById(WorkoutIds.COOPER_FITNESS_TEST)
    }

    override fun loadClimbmillWorkouts(config: FrameConfig) {
        loadStandardWorkouts(isClimbmill = true)
        addWorkoutItemById(WorkoutIds.THR_RESISTANCE)
        addWorkoutItemById(WorkoutIds.CPAT_FITNESS_TEST)
        addWorkoutItemById(WorkoutIds.SUBMAX_FITNESS_TEST)
        addWorkoutItemById(WorkoutIds.WFI_FITNESS_TEST)
    }

    override fun loadStepperWorkouts(config: FrameConfig) {
        loadStandardWorkouts(isClimbmill = true)
        addWorkoutItemById(WorkoutIds.THR_RESISTANCE)
        addWorkoutItemById(WorkoutIds.STEPPER_FITNESS_TEST)
    }

    override fun loadSpinBikeWorkouts(config: FrameConfig) {
        loadSpecialAndThirdPartyWorkouts()
        addWorkoutItemById(WorkoutIds.MANUAL, optionalBackground = "cover_images_manual.jpg")
        // CGC-25017: these use the default workout app like every other frame type, target training replaces the
        // individual distance/calories run screens that spin bikes used to launch here.
        addWorkoutItemById(WorkoutIds.ROLLING_HILLS, optionalBackground = "cover_images_rolling_hills.jpg")
        addWorkoutItemById(WorkoutIds.INTERVALS, optionalAppId = AppIds.INTERVALS_UUID, optionalBackground = "cover_images_intervals.jpg")
        addWorkoutItemById(WorkoutIds.FAT_BURN, optionalBackground = "cover_images_fat_burn.jpg")

        addWorkoutItemById(WorkoutIds.TIME_GOAL, optionalBackground = "cover_images_time_goal.jpg")
        addWorkoutItemById(WorkoutIds.DISTANCE_GOAL, optionalBackground = "cover_images_distance_goal.jpg")
        addWorkoutItemById(WorkoutIds.CALORIES_GOAL, optionalBackground = "cover_images_calories_goal.jpg")

        addWorkoutItemById(WorkoutIds.TARGET_TRAINING)
        addWorkoutItemById(WorkoutIds.PACE_TIME_GOAL)
        addWorkoutItemById(WorkoutIds.PACE_DISTANCE_GOAL)

        addWorkoutItemById(WorkoutIds.SPRINT8, optionalBackground = "cover_images_sprint8.jpg")
        addWorkoutItemById(WorkoutIds.VIRTUAL_ACTIVE)

        addWorkoutItemById(WorkoutIds.FTP_TEST_20)
        addWorkoutItemById(WorkoutIds.FTP_TEST_8)
        addWorkoutItemById(WorkoutIds.MAX_HR_TEST)
        addWorkoutItemById(WorkoutIds.SPIN_BIKE_INTERVALS_DISTANCE)
        addWorkoutItemById(WorkoutIds.SPIN_BIKE_INTERVALS_TIME)

        if (config.machineType != JHTCommonTypes.MachineType.ManualResistanceSpinBike) {
            addWorkoutItemById(WorkoutIds.HIIT)
            addWorkoutItemById(WorkoutIds.CONSTANT_WATTS)
            addWorkoutItemById(WorkoutIds.THR_RESISTANCE, optionalBackground = "cover_images_thr_resistance.jpg")
            addWorkoutItemById(WorkoutIds.COOPER_FITNESS_TEST, optionalBackground = "cover_images_cooper.jpg")
        }
    }

    /**
     * Workouts that every frame has
     */
    private fun loadStandardWorkouts(isClimbmill: Boolean = false, isTreadmill : Boolean = false) {
        addWorkoutItemById(WorkoutIds.MANUAL)
        addWorkoutItemById(WorkoutIds.ROLLING_HILLS, isTreadmill)  //isTreadmill for incline in title and description
        addWorkoutItemById(WorkoutIds.FAT_BURN)
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
        //Marathon Workout
        addWorkoutItemById(WorkoutIds.MARATHON)

        //FitPark Workouts
        addWorkoutItemById(WorkoutIds.MIGROS_POTENTIAL)
        addWorkoutItemById(WorkoutIds.MIGROS_EXHAUSTION)
        addWorkoutItemById(WorkoutIds.MIGROS_FATIGUE)

        //Egym Workouts
        addWorkoutItemById(WorkoutIds.EGYM)

        //IFIT Workouts
        addWorkoutItemById(WorkoutIds.IFIT)

        //YMCA 360 Workouts
        addWorkoutItemById(WorkoutIds.YMCA_360)

        //seca
        addWorkoutItemById(WorkoutIds.SECA_PACE)
    }
}