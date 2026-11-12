package com.jht.chimera.lib.workout.workoutlist.workoutmap

import android.net.Uri
import android.util.Log
import android.util.SparseArray
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.jht.androidcommonalgorithms.type.JHTCommonTypes.MachineClassificationType
import com.jht.chimera.R
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.config.frames.FrameConfig
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.getSDCardBackgroundUri
import com.jht.chimera.lib.common.getSDCardThumbnailUri
import com.jht.chimera.lib.common.getSDCardUri
import com.jht.chimera.lib.workout.WorkoutIds
import com.jht.chimera.lib.workoutapps.AppIds
import com.jht.data.WorkoutBadge
import com.jht.data.WorkoutItem
import com.jht.datamatrixlib.DataMatrix

/**
 * An abstract class that acts as a base for a workout map. A workout map provides a mapping of
 * which workouts should be available on each frame. An example is the [ChimeraWorkoutMap].
 *
 *
 * A workout map is created and used to find available workouts for each frame. It is passed into
 * [com.jht.chimera.lib.workout.WorkoutManager] to allow the workout manager to load only the
 * available workouts. The workout manager will handle getting the correct script based on the
 * frame min/max values and frame type.
 *
 *
 * Workouts are loaded automatically when constructing the class. To reload workouts call
 * [.loadWorkouts].
 */
abstract class WorkoutMap internal constructor() {
    private var loadedWorkouts = SparseArray<WorkoutItem>()

    /**
     * Workouts are loaded by default when creating the workout map
     */
    init {
        loadWorkouts()
    }

    /**
     * Loads workouts based on the current frame type. This is called automatically when
     * constructing the class.
     */
    fun loadWorkouts() {
        Log.e(TAG, "loadWorkouts: initialize")
        loadedWorkouts.clear()
        val frameConfig = FrameConfigManager.getInstance().frameConfig
        when (frameConfig.machineClassType) {
            MachineClassificationType.Treadmill -> loadTreadmillWorkouts(frameConfig)
            MachineClassificationType.Bike -> loadBikeWorkouts(frameConfig)
            //recumbent elliptical should get bike workouts
            MachineClassificationType.RecumbentElliptical -> loadBikeWorkouts(frameConfig)
            MachineClassificationType.Elliptical -> loadEllipticalWorkouts(frameConfig)
            MachineClassificationType.Ascent -> loadAscentWorkouts(frameConfig)
            MachineClassificationType.Climbmill -> loadClimbmillWorkouts(frameConfig)
            MachineClassificationType.Stepper -> loadStepperWorkouts(frameConfig)
            MachineClassificationType.SpinBike -> loadSpinBikeWorkouts(frameConfig)
            else -> Log.e(
                TAG,
                "Invalid frame type: " + frameConfig.machineClassType.name + ". Loading no workouts."
            )
        }
    }

    // Each overridden method should load appropriate workouts for the frame.
    protected abstract fun loadTreadmillWorkouts(config: FrameConfig)
    protected abstract fun loadBikeWorkouts(config: FrameConfig)
    protected abstract fun loadEllipticalWorkouts(config: FrameConfig)
    protected abstract fun loadAscentWorkouts(config: FrameConfig)
    protected abstract fun loadClimbmillWorkouts(config: FrameConfig)
    protected abstract fun loadStepperWorkouts(config: FrameConfig)
    protected abstract fun loadSpinBikeWorkouts(config: FrameConfig)


    /**
     * Gets a workout with a particular ID from the loaded workouts
     *
     * @param workoutId The workout ID to find
     * @return the workout with that ID or null
     */
    fun getWorkout(workoutId: Int): WorkoutItem? {
        return loadedWorkouts.get(workoutId)
    }

    /**
     * Gets all loaded workouts
     *
     * @return The loaded workouts
     */
    fun getLoadedWorkouts(): SparseArray<WorkoutItem> {
        return loadedWorkouts
    }

    protected fun addWorkoutItemById(
        id: Int,
        isTreadmill: Boolean = false,
        optionalAppId: String = "",
        optionalBackground: String = ""
    ) {
        //Get the default workout item data
        val workoutItem = getDefaultWorkoutItemById(id, isTreadmill)

        //Optional setting of a different background
        if (optionalBackground.isNotEmpty()) {
            //Insert the optional background as the first element
            val list = ArrayList(workoutItem.background)
            list.add(0, getSDCardBackgroundUri(optionalBackground))
            workoutItem.background = list
            workoutItem.thumbnail = getSDCardThumbnailUri(optionalBackground)
        }

        //Optional setting of a different appId
        if (optionalAppId.isNotEmpty()) {
            workoutItem.appId = optionalAppId
        }

        loadedWorkouts.append(id, workoutItem)
    }

    private fun getDefaultWorkoutItemById(
        id: Int,
        isTreadmill: Boolean = false
    ): WorkoutItem {
        @StringRes
        val title: Int
        @StringRes
        val description: Int
        @StringRes
        var carouselButtonId: Int? = null
        val background: ArrayList<Uri> = arrayListOf()
        val thumbnail: Uri
        val icon: WorkoutBadge
        var appId: String = AppIds.DEFAULT_APP_UUID
        @DrawableRes
        var carouselLogo: Int? = null
        @StringRes
        var badgeLabel = 0

        when (id) {
            //*****TRAINING*****
            WorkoutIds.MANUAL -> {
                title = R.string.manual_tc
                description = R.string.help_manual_pg
                background.add(getSDCardBackgroundUri("bg_manual.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_manual.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
            }

            WorkoutIds.ROLLING_HILLS -> {
                title = R.string.rolling_hills_tc
                description =
                    if (isTreadmill) R.string.help_rolling_hills_i_pg else R.string.help_rolling_hills_r_pg
                background.add(getSDCardBackgroundUri("bg_rolling_hills.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_rolling_hills.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
            }

            WorkoutIds.FAT_BURN -> {
                title = R.string.fat_burn_tc
                description = R.string.help_fat_burn_pg
                background.add(getSDCardBackgroundUri("bg_fat_burn.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_fat_burn.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
            }

            //Treadmills and Resistance machines differentiate incline and resistance, but share the same id
            WorkoutIds.INTERVALS -> {
                if (isTreadmill) {
                    title = R.string.intervals_incline_tc
                    description = R.string.help_intervals_incline_pg
                    background.add(getSDCardBackgroundUri("bg_intervals_incline.jpg"))
                    thumbnail = getSDCardThumbnailUri("bg_intervals_incline.jpg")
                } else {
                    title = R.string.intervals_resistance_tc
                    description = R.string.help_intervals_level_pg
                    background.add(getSDCardBackgroundUri("bg_intervals.jpg"))
                    thumbnail = getSDCardThumbnailUri("bg_intervals.jpg")
                }

                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
            }

            WorkoutIds.INTERVALS_SPEED -> {
                title = R.string.intervals_speed_tc
                description = R.string.help_intervals_speed_pg
                background.add(getSDCardBackgroundUri("bg_intervals_speed.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_intervals_speed.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
            }

            //RPM based machines
            WorkoutIds.CONSTANT_WATTS -> {
                title = R.string.constant_watts_tc
                description = R.string.help_constant_watts_pg
                background.add(getSDCardBackgroundUri("bg_constant_watts.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_constant_watts.jpg")
                appId = AppIds.CONSTANT_WATTS_UUID
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
            }

            //Ascent Specific
            WorkoutIds.GLUTES -> {
                title = R.string.glutes_tc
                description = R.string.help_glutes_pg
                background.add(getSDCardBackgroundUri("bg_glutes.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_glutes.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
            }

            //Spin Bike (Athena) Specific
            WorkoutIds.TARGET_TRAINING -> {
                title = R.string.target_training_tc
                description = R.string.target_training_info_pg
                background.add(getSDCardBackgroundUri("cover_images_target_training.jpg"))
                thumbnail = getSDCardThumbnailUri("cover_images_target_training.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
                appId = AppIds.TARGET_TRAINING
            }

            WorkoutIds.PACE_TIME_GOAL -> {
                title = R.string.pace_time_tc
                description = R.string.help_manual_pg
                background.add(getSDCardBackgroundUri("cover_images_pace_time.jpg"))
                thumbnail = getSDCardThumbnailUri("cover_images_pace_time.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_trophy)
                appId = AppIds.PACE_UUID
            }

            WorkoutIds.PACE_DISTANCE_GOAL -> {
                title = R.string.pace_distance_tc
                description = R.string.help_manual_pg
                background.add(getSDCardBackgroundUri("cover_images_pace_distance.jpg"))
                thumbnail = getSDCardThumbnailUri("cover_images_pace_distance.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_trophy)
                appId = AppIds.PACE_UUID
            }

            //*****GOALS*****
            WorkoutIds.TIME_GOAL -> {
                title = R.string.time_goal_tc
                description = R.string.help_time_goal_pg
                background.add(getSDCardBackgroundUri("bg_time_goal.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_time_goal.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_trophy)
            }

            WorkoutIds.DISTANCE_GOAL -> {
                title = R.string.distance_goal_tc
                description = R.string.help_distance_goal_pg
                background.add(getSDCardBackgroundUri("bg_distance_goal.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_distance_goal.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_trophy)
            }

            WorkoutIds.FLOORS_GOAL -> {
                title = R.string.floor_goal_tc
                description = R.string.help_floor_goal_pg
                background.add(getSDCardBackgroundUri("bg_floor_goal.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_floor_goal.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_trophy)
            }

            WorkoutIds.CALORIES_GOAL -> {
                title = R.string.calorie_goal_tc
                description = R.string.help_calorie_goal_pg
                background.add(getSDCardBackgroundUri("bg_calorie_goal.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_calorie_goal.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_trophy)
            }

            //*****TARGET HEART RATE*****
            WorkoutIds.THR_INCLINE -> {
                title = R.string.thr_incline_tc
                description = R.string.help_target_heart_rate_incline_pg
                background.add(getSDCardBackgroundUri("bg_thr_incline.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_thr_incline.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_heart)
                appId = AppIds.TARGET_HEART_RATE_UUID
            }

            WorkoutIds.THR_SPEED -> {
                title = R.string.thr_speed_tc
                description = R.string.help_target_heart_rate_speed_tm_pg
                background.add(getSDCardBackgroundUri("bg_thr_speed.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_thr_speed.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_heart)
                appId = AppIds.TARGET_HEART_RATE_UUID
            }

            WorkoutIds.THR_RESISTANCE -> {
                title = R.string.thr_resistance_tc
                description = R.string.help_target_heart_rate_resistance_pg
                background.add(getSDCardBackgroundUri("bg_thr_resistance.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_thr_resistance.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_heart)
                appId = AppIds.TARGET_HEART_RATE_UUID
            }

            //*****VIRTUAL ACTIVE*****
            WorkoutIds.VIRTUAL_ACTIVE -> {
                title = R.string.virtual_active_tc
                description = R.string.help_va_pg
                background.add(getSDCardBackgroundUri("bg_virtual_active.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_virtual_active.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_va)
                appId = AppIds.VA_UUID
            }

            //*****SPRINT 8*****
            WorkoutIds.SPRINT8 -> {
                title = R.string.sprint8_tc
                description = R.string.help_sprint8_pg
                background.add(getSDCardBackgroundUri("bg_sprint8.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_sprint8.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_sprint8)
                appId = AppIds.SPRINT8_UUID
            }

            //*****SWITCH STRIDE*****
            WorkoutIds.HYBRID_TD_SWITCH_STRIDE -> {
                title = R.string.switch_stride_tc
                description = R.string.help_switch_stride_pg
                background.add(getSDCardBackgroundUri("bg_switch_stride.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_switch_stride.jpg")
                icon = WorkoutBadge(R.drawable.ic_switch_stride)
                appId = AppIds.HYBRID_TD_SWITCH_STRIDE_UUID
            }

            //*****LANDMARKS*****
            WorkoutIds.LANDMARKS -> {
                title = R.string.landmarks_tc
                description = R.string.help_landmarks_pg
                background.add(getSDCardBackgroundUri("bg_landmarks.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_landmarks.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_landmarks)
                appId = AppIds.LANDMARKS_UUID
            }

            //*****MATRIX METHOD*****
            WorkoutIds.PROGRESSIVE_WORKOUT -> {
                title = R.string.matrix_method_tc
                description = R.string.matrix_method_description_pg
                background.add(getSDCardBackgroundUri("bg_matrix_method_wide.jpg"))
                thumbnail = getSDCardBackgroundUri("bg_matrix_method.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_method)
                appId = AppIds.PROGRESSION_UUID
            }

            //******SECA*******
            WorkoutIds.SECA_PACE -> {
                title = R.string.pace_uc
                description = R.string.pace_description_pg
                background.add(getSDCardBackgroundUri(secaBackground()))
                thumbnail = getSDCardThumbnailUri("bg_pace.png")
                icon = WorkoutBadge()
                appId = AppIds.SECA_UUID
                carouselLogo = R.drawable.ic_pace_logo_white
                carouselButtonId = R.string.start_workout_tc
            }

            //*****FITNESS TESTS*****
            //Treadmill Specific
            WorkoutIds.ARMY_FITNESS_TEST -> {
                title = R.string.army_tc
                description = R.string.help_army_pg
                background.add(getSDCardBackgroundUri("bg_army.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_army.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.TRACK_UUID
            }

            WorkoutIds.MARINES_FITNESS_TEST -> {
                title = R.string.marines_tc
                description = R.string.help_marines_pg
                background.add(getSDCardBackgroundUri("bg_marines.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_marines.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.TRACK_UUID
            }

            WorkoutIds.AIR_FORCE_FITNESS_TEST -> {
                title = R.string.air_force_tc
                description = R.string.help_air_force_pg
                background.add(getSDCardBackgroundUri("bg_air_force.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_air_force.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.TRACK_UUID
            }

            WorkoutIds.NAVY_FITNESS_TEST -> {
                title = R.string.navy_tc
                description = R.string.help_navy_pg
                background.add(getSDCardBackgroundUri("bg_navy.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_navy.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.TRACK_UUID
            }

            WorkoutIds.PEB_FITNESS_TEST -> {
                title = R.string.peb_uc
                description = R.string.help_peb_pg
                background.add(getSDCardBackgroundUri("bg_peb.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_peb.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.TRACK_UUID
            }

            WorkoutIds.GERKIN_FITNESS_TEST -> {
                title = R.string.gerkin_tc
                description = R.string.help_gerkin_pg
                background.add(getSDCardBackgroundUri("bg_gerkin.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_gerkin.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.TARGET_HEART_RATE_UUID
            }

            //RPM based machines
            WorkoutIds.COOPER_FITNESS_TEST -> {
                title = R.string.cooper_tc
                description = R.string.help_cooper_pg
                background.add(getSDCardBackgroundUri("bg_cooper_fitness_test.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_cooper_fitness_test.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.CONSTANT_WATTS_UUID
            }

            //Climbmill Specific
            WorkoutIds.CPAT_FITNESS_TEST -> {
                title = R.string.cpat_test_tc
                description = R.string.help_cpat_pg
                background.add(getSDCardBackgroundUri("bg_cpat_fitness_test.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_cpat_fitness_test.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.SIMPLE_UUID
            }

            WorkoutIds.SUBMAX_FITNESS_TEST -> {
                title = R.string.submaximal_test_tc
                description = R.string.help_submaximal__pg
                background.add(getSDCardBackgroundUri("bg_submaximal_fitness_test.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_submaximal_fitness_test.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.TARGET_HEART_RATE_UUID
            }

            WorkoutIds.WFI_FITNESS_TEST -> {
                title = R.string.wfi_test_tc
                description = R.string.help_wfi_pg
                background.add(getSDCardBackgroundUri("bg_wfi_fitness_test.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_wfi_fitness_test.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.TARGET_HEART_RATE_UUID
            }

            //Stepper Specific
            WorkoutIds.STEPPER_FITNESS_TEST -> {
                title = R.string.stepper_fitness_test_tc
                description = R.string.stepper_fitness_test_pg
                background.add(getSDCardBackgroundUri("bg_stepper_fitness_test.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_stepper_fitness_test.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.TARGET_HEART_RATE_UUID
            }

            //Spin Bike (Athena) Specific
            WorkoutIds.FTP_TEST_20 -> {
                title = R.string.ftp_tc_20
                description = R.string.help_fatigue_pg
                background.add(getSDCardBackgroundUri("cover_images_ftp20.jpg"))
                thumbnail = getSDCardThumbnailUri("cover_images_ftp20.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.FTP_UUID
            }

            WorkoutIds.SPIN_BIKE_INTERVALS_DISTANCE -> {
                title = R.string.spin_bike_intervals_distance_tc
                description = R.string.spin_bike_intervals_pg
                background.add(getSDCardBackgroundUri("bg_spin_bike_intervals_distance.png"))
                thumbnail = getSDCardThumbnailUri("bg_spin_bike_intervals_distance.png")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
                badgeLabel = R.string.training_workout_tc
                appId = AppIds.SPIN_BIKE_INTERVALS_UUID
            }

            WorkoutIds.SPIN_BIKE_INTERVALS_TIME -> {
                title = R.string.spin_bike_intervals_time_tc
                description = R.string.spin_bike_intervals_pg
                background.add(getSDCardBackgroundUri("bg_spin_bike_intervals_time.png"))
                thumbnail = getSDCardThumbnailUri("bg_spin_bike_intervals_time.png")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
                badgeLabel = R.string.training_workout_tc
                appId = AppIds.SPIN_BIKE_INTERVALS_UUID
            }

            WorkoutIds.FTP_TEST_8 -> {
                title = R.string.ftp_tc_8
                description = R.string.help_fatigue_pg
                background.add(getSDCardBackgroundUri("cover_images_ftp8.jpg"))
                thumbnail = getSDCardThumbnailUri("cover_images_ftp8.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.FTP_UUID
            }

            WorkoutIds.MAX_HR_TEST -> {
                title = R.string.maxhrtest_tc
                description = R.string.help_target_heart_rate_resistance_pg
                background.add(getSDCardBackgroundUri("cover_images_max_bpm_test.jpg"))
                thumbnail = getSDCardThumbnailUri("cover_images_max_bpm_test.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_fitness_test)
                appId = AppIds.MAX_HR_TEST_UUID
            }

            //*****SPECIAL AND THIRD PARTY WORKOUTS*****
            WorkoutIds.CUSTOM_WORKOUT -> {
                title = R.string.custom_no_ellipes_tc
                description = R.string.custom_description_pg
                background.add(getSDCardBackgroundUri("bg_custom.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_custom.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
            }

            WorkoutIds.MARATHON -> {
                title = R.string.marathon_tc
                description = R.string.help_marathon_pg
                background.add(getSDCardBackgroundUri("bg_marathon.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_marathon.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
            }

            WorkoutIds.HIIT -> {
                title = R.string.high_intensity_tc
                description = R.string.hiit_description_pg
                background.add(getSDCardBackgroundUri("bg_hiit.png"))
                background.add(getSDCardBackgroundUri("bg_hiit_short.png"))
                thumbnail = getSDCardThumbnailUri("bg_hiit.png")
                icon = WorkoutBadge(R.drawable.ic_badge_hiit)
                appId = AppIds.HIIT_APP_UUID
            }

            //FitPark (Migros)
            WorkoutIds.MIGROS_POTENTIAL -> {
                title = R.string.potential_tc
                description = R.string.help_potential_pg
                background.add(getSDCardUri("migros_potential_background.png", "migros"))
                thumbnail = getSDCardUri("migros_potential_background.png", "migros")
                icon = WorkoutBadge(R.drawable.ic_badge_migros)
                appId = AppIds.TARGET_HEART_RATE_UUID
            }

            WorkoutIds.MIGROS_EXHAUSTION -> {
                title = R.string.exhaustion_tc
                description = R.string.help_exhaustion_pg
                background.add(getSDCardUri("migros_exhaustion_background.png", "migros"))
                thumbnail = getSDCardUri("migros_exhaustion_background.png", "migros")
                icon = WorkoutBadge(R.drawable.ic_badge_migros)
                appId = AppIds.TARGET_HEART_RATE_UUID
            }

            WorkoutIds.MIGROS_FATIGUE -> {
                title = R.string.fatigue_tc
                description = R.string.help_fatigue_pg
                background.add(getSDCardUri("migros_fatigue_background.png", "migros"))
                thumbnail = getSDCardUri("migros_fatigue_background.png", "migros")
                icon = WorkoutBadge(R.drawable.ic_badge_migros)
                appId = AppIds.TARGET_HEART_RATE_UUID
            }

            //Egym
            WorkoutIds.EGYM -> {
                title = R.string.manual_tc
                description = R.string.manual_tc
                background.add(getSDCardBackgroundUri("bg_training_manual.jpg"))
                thumbnail = getSDCardThumbnailUri("bg_training_manual.jpg")
                icon = WorkoutBadge(R.drawable.ic_badge_egym)
                appId = AppIds.EGYM_UUID
            }

            //Ifit
            WorkoutIds.IFIT -> {
                title = R.string.ifit_tc
                description = R.string.ifit_2_pg
                background.add(getSDCardUri("ifit_featured_chimera.jpg", "ifit"))
                thumbnail = (getSDCardUri("ifit_icon_chimera.jpg", "ifit"))
                icon = WorkoutBadge(R.drawable.ic_badge_ifit)
                appId = AppIds.IFIT_UUID
            }

            //YMCA 360
            WorkoutIds.YMCA_360 -> {
                title = R.string.ymca_360_tc
                description = R.string.ymca_360_description_pg
                background.add(getSDCardUri("MatrixArt_Chevron1_2022.png", "ymca360"))
                background.add(getSDCardUri("ymca360_featured_chimera.jpg", "ymca360"))
                background.add(getSDCardUri("MatrixArt_Chevron2_2022.png", "ymca360"))
                thumbnail = (getSDCardUri("ymca360_featured_icon.png", "ymca360"))
                icon = WorkoutBadge(R.drawable.ic_badge_ymca)
                appId = AppIds.YMCA_360_FEATURED_UUID
            }

            //Default
            else -> {
                title = R.string.manual_tc
                description = R.string.help_manual_pg
                background.add(Uri.EMPTY)
                thumbnail = Uri.EMPTY
                icon = WorkoutBadge()
            }
        }
        return WorkoutItem(
            workoutId = id,
            appId = appId,
            titleId = title,
            descriptionId = description,
            background = background,
            thumbnail = thumbnail,
            badge = icon,
            carouselLogoId = carouselLogo,
            carouselButtonId = carouselButtonId,
            badgeLabelId = badgeLabel
        )
    }

    companion object {
        private const val TAG = "WorkoutMap"

        /**
         * Creates the appropriate derived instance of WorkoutMap for flavor
         *
         * @return The new derived WorkoutMap instance
         */
        fun createWorkoutMap(): WorkoutMap {
            Log.e(TAG, "createWorkoutMap: ")
            return if (ProductConfig.isParker()) ParkerWorkoutMap() else ChimeraWorkoutMap()
        }

        private fun secaBackground(): String {
            val useVortexUi = DataMatrix.getInt(SettingKeys.Experimental.DATA_UI_SYSTEM_ORDINAL_INT) == SettingKeys.UiSystem.Vortex.ordinal
            return if (useVortexUi) "bg_pace_vortex.png" else "bg_pace.png"
        }
    }
}