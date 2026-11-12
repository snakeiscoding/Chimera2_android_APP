package com.jht.chimera.lib.workout.workoutlist.workoutmap;

import android.content.Context;
import android.util.Log;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.workout.SpecialWorkouts;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.lib.workout.platform.ChimeraWorkoutManager;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem;
import com.jht.data.WorkoutItem;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This class will determine what carousel and featured content is available for each system and provide the
 * correct workout IDs based on the frame
 */
public class FeaturedContentHelper {
    private static final String TAG = "FeaturedContentHelper";

    // List of the current workout IDs for the carousel workouts (Top pager banner in workout setup)
    private List<Integer> carouselWorkoutIds;

    // List of the possible workout IDs for the carousel workout options (the list available for the MES Workout Tab)
    private List<Integer> carouselWorkoutOptionIds;

    // List of the current workout IDs for the featured workouts (items below the carousel in workout selection)
    private List<Integer> featuredWorkoutIds;

    /**
     * Creates the class
     */
    public FeaturedContentHelper(WorkoutMap workoutMap) {
        init(workoutMap);
    }

    public void init(WorkoutMap workoutMap) {
        carouselWorkoutOptionIds = getCarouselWorkoutOptions(workoutMap);
        carouselWorkoutIds = getCarouselIds();
        featuredWorkoutIds = getFeatureIds();
        checkCarouselForDisabledWorkouts();
    }

    /**
     * Gets a copy of the carousel workout ID list
     *
     * @return The carousel workout ID list
     */
    public List<Integer> getCarouselWorkoutIds() {
        return new ArrayList<>(carouselWorkoutIds);
    }

    /**
     * Gets a copy of the carousel workout option ID list
     *
     * @return The featured carousel workout option ID list
     */
    public List<Integer> getCarouselWorkoutOptionIds() {
        return new ArrayList<>(carouselWorkoutOptionIds);
    }

    /**
     * Gets a copy of the featured workout ID list
     *
     * @return The featured workout ID list
     */
    public List<Integer> getFeaturedWorkoutIds() {
        return new ArrayList<>(featuredWorkoutIds);
    }

    /**
     * Sets the featured workout. Which key is used is based on frame type, as Athena and cardio
     * frames do not share featured workouts.
     *
     * @param carouselWorkoutIds the workout IDs for the featured workouts
     */
    public void setCarouselWorkouts(List<Integer> carouselWorkoutIds) {
        String featuredWorkoutsCsv = getFeaturedWorkoutCsv(carouselWorkoutIds);
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        if (config.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike) {
            DataMatrix.putString(SettingKeys.Workout.DATA_FEATURED_WORKOUT_ATHENA_STRING, featuredWorkoutsCsv);
        } else {
            DataMatrix.putString(SettingKeys.Workout.DATA_FEATURED_WORKOUT_NON_ATHENA_STRING, featuredWorkoutsCsv);
        }

        this.carouselWorkoutIds = carouselWorkoutIds;
    }

    /**
     * If current list of carousel items contains a workout that is disabled or unavailable,
     * we want to remove it and replace it with the default value
     */
    private void checkCarouselForDisabledWorkouts() {
        List<Integer> workouts = carouselWorkoutIds;
        List<Integer> carouselOptions = carouselWorkoutOptionIds;
        boolean carouselUpdated = false;

        for (int i = 0; i < workouts.size(); i++) {
            switch (workouts.get(i)) {
                //Special Case if Sprint 8 is found, but the workout map doesn't contain it (climbmill)
                //Swap it to Landmarks instead
                case WorkoutIds.SPRINT8:
                    if (!carouselOptions.contains(WorkoutIds.SPRINT8)) {
                        workouts.set(i, WorkoutIds.LANDMARKS);
                        Log.w(TAG, "Carousel Item Sprint 8 was disabled and replaced with Landmarks");
                        carouselUpdated = true;
                    }
                    break;
                //Special Case if Landmarks is found, but the workout map doesn't contain it (non-climbmill)
                //Swap it to Sprint 8 instead
                case WorkoutIds.LANDMARKS:
                    if (!carouselOptions.contains(WorkoutIds.LANDMARKS)) {
                        workouts.set(i, WorkoutIds.SPRINT8);
                        Log.w(TAG, "Carousel Item Landmarks was disabled and replaced with Sprint 8");
                        carouselUpdated = true;
                    }
                    break;
                default:
                    //If the carousel option list does not contain the current item (e.g. a special workout was disabled),
                    //it should be replaced with the first non-duplicate option
                    if (!carouselOptions.contains(workouts.get(i))) {
                        for (int newCarouselOption : carouselOptions) {
                            if (newCarouselOption != workouts.get(i)) {
                                Log.w(TAG, "Carousel Item with Workout ID of: " + workouts.get(i) +
                                        " was disabled and replaced with Workout ID " + newCarouselOption);
                                workouts.set(i, newCarouselOption);
                                break;
                            }
                        }
                        carouselUpdated = true;
                    }
                    break;
            }
        }

        //If a carousel item has been updated
        if (carouselUpdated) {
            setCarouselWorkouts(workouts);
        }
    }

    /**
     * Gets the selected carousel workout ID list based on frame type.
     *
     * @return The list of carousel workout IDs
     */
    private List<Integer> getCarouselIds() {
        List<Integer> carouselItems;
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        String selectedWorkoutsCsv;
        if (config.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike) {
            selectedWorkoutsCsv = DataMatrix.getString(SettingKeys.Workout.DATA_FEATURED_WORKOUT_ATHENA_STRING);
        } else {
            selectedWorkoutsCsv = DataMatrix.getString(SettingKeys.Workout.DATA_FEATURED_WORKOUT_NON_ATHENA_STRING);
        }
        try {
            carouselItems = parseFeaturedWorkoutsCSV(selectedWorkoutsCsv);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Number format exception for carousel items: \"" + selectedWorkoutsCsv + "\". Replacing with default values");
            carouselItems = getDefaultCarouselOptions();
            setCarouselWorkouts(carouselItems);
        }

        return carouselItems;
    }

    /**
     * Gets a copy of the featured workout id list (workout selection items below the carousel)
     *
     * @return The featured workout id list
     */
    private List<Integer> getFeatureIds() {
        List<Integer> featuredItems = new ArrayList<>();

        // Special Workouts
        featuredItems.addAll(getEnabledSpecialWorkouts());

        // Do not show matrix method for guest users since they can't save workouts?
        if (carouselWorkoutOptionIds.contains(WorkoutIds.PROGRESSIVE_WORKOUT)) {
            featuredItems.add(WorkoutIds.PROGRESSIVE_WORKOUT);
        }
        if (carouselWorkoutOptionIds.contains(WorkoutIds.SPRINT8)) {
            featuredItems.add(WorkoutIds.SPRINT8);
        }
        if (carouselWorkoutOptionIds.contains(WorkoutIds.LANDMARKS)) {
            featuredItems.add(WorkoutIds.LANDMARKS);
        }
        if (carouselWorkoutOptionIds.contains(WorkoutIds.HIIT)) {
            featuredItems.add(WorkoutIds.HIIT);
        }
        if (carouselWorkoutOptionIds.contains(WorkoutIds.HYBRID_TD_SWITCH_STRIDE)) {
            featuredItems.add(WorkoutIds.HYBRID_TD_SWITCH_STRIDE);
        }

        if (FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.SpinBike)) {
            featuredItems.add(WorkoutIds.TARGET_TRAINING);
        }

        featuredItems.add(WorkoutIds.VIRTUAL_ACTIVE);
        return featuredItems;
    }

    /**
     * Gets the carousel workout options. These are displayed in the MES workout tab as options for the carousel order.
     *
     * @return List of possible carousel workout IDs
     */
    private List<Integer> getCarouselWorkoutOptions(WorkoutMap workoutMap) {
        //Add special workouts that are enabled
        List<Integer> carouselOptions = new ArrayList<>(getEnabledSpecialWorkouts());

        // Add basic workout options from the list of workout IDs
        //Validate against the workout map to make sure the workouts are valid
        for (int workoutId : getBasicWorkoutCarouselOptions()) {
            WorkoutItem workout = workoutMap.getWorkout(workoutId);
            if (workout != null) {
                carouselOptions.add(workout.getWorkoutId()); // NON-NLS
            }
        }

        return carouselOptions;
    }

    /**
     * Gets the default carousel workout options. This method should only run if the carousel list fails to parse,
     * or is empty (Chimera/Onyx versions prior to 2025).
     *
     * @return List of the default carousel options
     */
    private List<Integer> getDefaultCarouselOptions() {
        List<Integer> carouselDefaultIds = new ArrayList<>();
        List<Integer> carouselItems = carouselWorkoutOptionIds;

        if (carouselItems.size() >= 3) {
            //We want to focus the 0th item as the center object (usually sprint8/landmarks/special workout)
            carouselDefaultIds.add(carouselItems.get(1));
            carouselDefaultIds.add(carouselItems.get(0));
            carouselDefaultIds.add(carouselItems.get(2));
        }

        return carouselDefaultIds;
    }

    /**
     * Gets the special workouts that are enabled. This covers IFIT, YMCA 360, and Migros
     *
     * @return List of the special workouts that are enabled
     */
    private List<Integer> getEnabledSpecialWorkouts() {
        List<Integer> enabledSpecialWorkouts = new ArrayList<>();
        // Add special workouts. Check if they are enabled then add to the list
        List<SpecialWorkouts.SpecialWorkout> specialWorkouts = SpecialWorkouts.getSpecialWorkouts();
        for (SpecialWorkouts.SpecialWorkout specialWorkout : specialWorkouts) {
            boolean isEnabled = DataMatrix.getBoolean(specialWorkout.key(), false);
            if (isEnabled) {
                switch (specialWorkout.keycode()) {
                    case SpecialWorkouts.IFIT_KEYCODE:
                    case SpecialWorkouts.YMCA360_KEYCODE:
                        enabledSpecialWorkouts.add(specialWorkout.workoutId());
                        break;
                    case SpecialWorkouts.MIGROS_KEYCODE:
                        //Migros has 3 workouts associated with it being enabled
                        enabledSpecialWorkouts.add(WorkoutIds.MIGROS_POTENTIAL);
                        enabledSpecialWorkouts.add(WorkoutIds.MIGROS_EXHAUSTION);
                        enabledSpecialWorkouts.add(WorkoutIds.MIGROS_FATIGUE);
                        break;
                }
            }
        }
        return enabledSpecialWorkouts;
    }

    /**
     * Gets a list of basic workouts that are included as options for the carousel display. This can
     * be modified to add or remove possibilities.
     *
     * @return an array of IDs for workouts that should be included.
     */
    private int[] getBasicWorkoutCarouselOptions() {
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        // If we are a spin bike we want to display different possible workouts
        if (config.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike) {
            return new int[]{
                    WorkoutIds.SPRINT8, WorkoutIds.ROLLING_HILLS,
                    WorkoutIds.VIRTUAL_ACTIVE, WorkoutIds.MANUAL, WorkoutIds.TARGET_TRAINING,
                    WorkoutIds.INTERVALS, WorkoutIds.PACE_TIME_GOAL, WorkoutIds.PACE_DISTANCE_GOAL,
                    WorkoutIds.CONSTANT_WATTS, WorkoutIds.FAT_BURN, WorkoutIds.TIME_GOAL, WorkoutIds.DISTANCE_GOAL,
                    WorkoutIds.CALORIES_GOAL, WorkoutIds.THR_RESISTANCE, WorkoutIds.FTP_TEST_20,
                    WorkoutIds.FTP_TEST_8, WorkoutIds.COOPER_FITNESS_TEST, WorkoutIds.MAX_HR_TEST, WorkoutIds.HIIT
            };
        }

        if (config.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
            if (config.TM_SUPPORTS_REVERSE_BELT) {
                return new int[]{
                        WorkoutIds.SPRINT8, WorkoutIds.LANDMARKS, WorkoutIds.PROGRESSIVE_WORKOUT,
                        WorkoutIds.ROLLING_HILLS, WorkoutIds.VIRTUAL_ACTIVE, WorkoutIds.MANUAL,
                        WorkoutIds.FAT_BURN, WorkoutIds.TIME_GOAL, WorkoutIds.CALORIES_GOAL, WorkoutIds.HIIT,
                        WorkoutIds.HYBRID_TD_SWITCH_STRIDE
                };
            } else {
                return new int[]{
                        WorkoutIds.SPRINT8, WorkoutIds.LANDMARKS, WorkoutIds.PROGRESSIVE_WORKOUT,
                        WorkoutIds.ROLLING_HILLS, WorkoutIds.VIRTUAL_ACTIVE, WorkoutIds.MANUAL,
                        WorkoutIds.FAT_BURN, WorkoutIds.TIME_GOAL, WorkoutIds.CALORIES_GOAL, WorkoutIds.HIIT,
                };

            }
        }

        return new int[]{
                WorkoutIds.SPRINT8, WorkoutIds.LANDMARKS, WorkoutIds.ROLLING_HILLS,
                WorkoutIds.VIRTUAL_ACTIVE, WorkoutIds.MANUAL,
                WorkoutIds.FAT_BURN, WorkoutIds.TIME_GOAL, WorkoutIds.CALORIES_GOAL, WorkoutIds.HIIT
        };
    }

    /**
     * Takes a csv of the featured workouts and converts it to a list form
     *
     * @param featuredWorkoutsCsv The csv for the featured workouts
     * @return The list form of the featured workouts
     */
    private List<Integer> parseFeaturedWorkoutsCSV(String featuredWorkoutsCsv) {
        String[] featuredWorkoutsArray = featuredWorkoutsCsv.split(",");
        List<Integer> featuredWorkouts = new ArrayList<>();
        for (String workout : featuredWorkoutsArray) {
            featuredWorkouts.add(Integer.parseInt(workout));
        }

        return featuredWorkouts;
    }

    /**
     * Create a string with each of the featured workouts so we can save it in the DataMatrix efficiently
     *
     * @param featuredWorkouts The featured workouts in list form
     * @return The string form of the featured workouts
     */
    private static String getFeaturedWorkoutCsv(List<Integer> featuredWorkouts) {
        StringBuilder featuredWorkoutString = new StringBuilder();

        for (int i = 0; i < featuredWorkouts.size(); i++) {
            featuredWorkoutString.append(featuredWorkouts.get(i));
            if ((i + 1) < featuredWorkouts.size()) {
                featuredWorkoutString.append(",");
            }
        }

        return featuredWorkoutString.toString();
    }

    /**
     * Deprecated. This method is used for the old Go screen activity. New features should use the
     * workout map meta data instead of featured items
     *
     * @return the featured item of the workout
     */
    @Deprecated
    public FeaturedItem getFeaturedItem(int featuredId, Context context, WorkoutMap workoutMap) {
        FeaturedItem featuredItem = convertWorkoutItemtoFeaturedItem(workoutMap.getWorkout(featuredId), context);
        switch (featuredId) {
            case WorkoutIds.YMCA_360:
                featuredItem.startButtonText = context.getString(R.string.start_tc).toUpperCase(Locale.ENGLISH);
                featuredItem.workoutType = 95; // this is same as workoutId
                featuredItem.setupApp = view -> {
                    LaunchAppUtil.initialLaunchApp(context, AppIds.YMCA_360_FEATURED_UUID, view);
                    DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.YMCA360_LOADING.name());
                };
                break;
            case WorkoutIds.IFIT:
                featuredItem.identifier = ChimeraWorkoutManager.IFIT;
                featuredItem.workoutType = FeaturedItem.WORKOUT_IFIT;
                featuredItem.setupApp = view -> {
                    LaunchAppUtil.initialLaunchApp(context, AppIds.IFIT_UUID, view);
                    DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.IFIT_LOADING.name());
                };
                break;
            case WorkoutIds.PROGRESSIVE_WORKOUT:
                featuredItem.workoutType = WorkoutIds.PROGRESSIVE_WORKOUT;
                featuredItem.setupApp = view -> {
                    LaunchAppUtil.initialLaunchApp(context, AppIds.PROGRESSION_UUID, view);
                };
                break;
            case WorkoutIds.SPRINT8:
                featuredItem.startButtonText = context.getString(R.string.setup_uc);
                featuredItem.workoutType = FeaturedItem.WORKOUT_SPRINT8;
                break;
            case WorkoutIds.HIIT:
                featuredItem.startButtonText = context.getString(R.string.setup_uc);
                featuredItem.workoutType = FeaturedItem.WORKOUT_CUSTOM_HIIT;
                break;
            case WorkoutIds.LANDMARKS:
                featuredItem.workoutType = FeaturedItem.WORKOUT_LANDMARKS;
                break;
            case WorkoutIds.VIRTUAL_ACTIVE:
                featuredItem.workoutType = FeaturedItem.WORKOUT_VA;
                break;
            case WorkoutIds.TARGET_TRAINING:
                featuredItem.workoutType = FeaturedItem.WORKOUT_TARGET_TRAINING;
                break;
            default:
                break;
        }
        return featuredItem;
    }

    /**
     * Deprecated. This method is used for the old Go screen activity. New features should use the
     * workout map meta data instead of featured items
     *
     * @return the featured item of the workout based on the workout item
     */
    @Deprecated
    private FeaturedItem convertWorkoutItemtoFeaturedItem(WorkoutItem workoutItem, Context context) {
        if (workoutItem != null) {
            return new FeaturedItem(workoutItem.getWorkoutId(), context.getString(workoutItem.getTitleId()), "",
                    "", workoutItem.getBackground(), null,
                    workoutItem.getThumbnail(), null, String.valueOf(workoutItem.getWorkoutId()), context.getString(workoutItem.getDescriptionId()));
        } else {
            return new FeaturedItem("", 0);
        }
    }
}
