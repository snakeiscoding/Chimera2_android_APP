package com.jht.chimera.lib.workout.platform;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.Sprint8Keys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.landmark.LandmarkDataKt;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.lib.workout.WorkoutInput;
import com.jht.chimera.lib.workout.Sprint8WorkoutData;
import com.jht.chimera.lib.workout.WorkoutManager;
import com.jht.chimera.lib.workout.generator.AscentSprint8Config;
import com.jht.chimera.lib.workout.generator.BaseSprint8Config;
import com.jht.chimera.lib.workout.generator.ResistanceSprint8Config;
import com.jht.chimera.lib.workout.generator.TreadmillSprint8Config;
import com.jht.chimera.lib.workout.workoutlist.workoutmap.FeaturedContentHelper;
import com.jht.chimera.lib.workout.workoutlist.workoutmap.WorkoutMap;
import com.jht.chimera.lib.workout.workoutparameters.MinMaxDictionary;
import com.jht.chimera.lib.workout.workoutparameters.WorkoutInputManager;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenWorkoutDisplayItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutHelper;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter;
import com.jht.chimera.ui.mainapp.landmark.LandmarkLoader;
import com.jht.data.LandmarkWorkoutItem;
import com.jht.data.WorkoutItem;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.MinMaxParam;
import com.jht.enums.UnitMeasurement;
import com.jht.workoutcontrollerlib.model.DataModel;
import com.jht.workoutcontrollerlib.model.IntervalModel;
import com.jht.workoutcontrollerlib.model.LevelModel;
import com.jht.workoutcontrollerlib.model.StageModel;
import com.jht.workoutcontrollerlib.model.WorkoutModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class that handles getting the correct workout information for chimera.
 */
public class ChimeraWorkoutManager {

    private final String TAG = "ChimeraWorkoutManager";
    public static final String VA_PREFIX = "VA:"; //NON-NLS
    public static final String LANDMARKS_PREFIX = "Landmarks:"; //NON-NLS
    public static final String IFIT = "ifit:"; //NON-NLS

    private String mCurrentWorkout = "";

    private final Context context;

    public final WorkoutMap mWorkoutMap;
    public final WorkoutManager mWorkoutManager;
    public List<WorkoutItem> mVaWorkoutList;

    // Caches the SimpleInputConfig so we can show the same values for parameters if users change workouts
    private HashMap<String, WorkoutParameter> mInputMap = new HashMap<>();

    public ChimeraWorkoutManager(Context context) {
        this.context = context;
        mWorkoutMap = WorkoutMap.Companion.createWorkoutMap();
        mWorkoutManager = new WorkoutManager(mWorkoutMap);
    }

    /**
     * A mirror of the callback from the workout manager, used for compatibility with the go screen module
     */
    public interface IWorkoutParameterLoading {
        void workoutLoaded(List<WorkoutParameter> parameters);
    }

    /**
     * Takes a workout string and loads the correct workout based on the string that's passed in.
     * This will pass back a list of parameters that are used in the go screen module to generate
     * sliders.
     *
     * @param workout The workout to load
     */
    public void loadWorkout(String workout, ChimeraWorkoutManager.IWorkoutParameterLoading callback) {
        mCurrentWorkout = workout;
        String[] fields = workout.split(":"); // looks like workout is assumed non-null or null exception must be planned for?

        WorkoutManager.IWorkoutManager workoutLoaded = workoutInputs -> {
            List<WorkoutParameter> parameters = getParameterListFromInputs(workoutInputs);
            callback.workoutLoaded(parameters);
        };

        if (workout.startsWith(VA_PREFIX)) {
            mWorkoutManager.loadWorkoutWithId(WorkoutIds.VIRTUAL_ACTIVE, workoutLoaded);

            // Cache the selected VA workout uniqueId for later transmission if in fact the virtual active workout is started.
            String id = fields[1]; // workout = "VA:unique_id" NOT the title like landmarks is
            DataMatrix.putString(VaKeys.DATA_SELECTED_WORKOUT_UNIQUEID_STRING, id);
            // Cache the VA title for the 701 call
            if (mVaWorkoutList != null) {
                for (WorkoutItem vaWorkout : mVaWorkoutList) {
                    if (vaWorkout.getOptionalId().equalsIgnoreCase(id)) {
                        String name = context.getResources().getString(vaWorkout.getTitleId());
                        DataMatrix.putString(WorkoutKeys.DATA_SELECTED_VA_OR_LANDMARK_TITLE_STRING, name); // unique_id -> VA title
                    }
                }
            }

        } else {
            // Clear VA if this is not a VA workout
            DataMatrix.putString(VaKeys.DATA_SELECTED_WORKOUT_UNIQUEID_STRING, "");

            if (workout.startsWith(LANDMARKS_PREFIX)) {
                mWorkoutManager.loadWorkoutWithId(WorkoutIds.LANDMARKS, workoutLoaded);
                // Cache the selected landmarks destination
                String title = fields[1]; // workout = "Landmarks:Real_Title"
                DataMatrix.putString(WorkoutKeys.DATA_SELECTED_VA_OR_LANDMARK_TITLE_STRING, title); // fields = Landmarks:Real_Title
            } else if(workout.startsWith(IFIT)) {
                // No workout parameters needed for IFIT
            } else {
                // optional "workout_id:app_id" format (retail manual with app) See Featured Fragment
                String appOverrideId = fields.length > 1 ? fields[1] : null;
                int workoutId = Integer.parseInt(fields[0]);
                mWorkoutManager.loadWorkout(workoutId, appOverrideId, workoutLoaded);
            }
        }
    }

    /**
     * Returns the list of workout ids to be displayed in the workout selection carousel
     */
    public List<WorkoutItem> getCarouselWorkouts(){
        List<WorkoutItem> carouselWorkouts = new ArrayList<>();

        boolean isGuest = DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL);
        boolean isSecaUser =  DataMatrix.getBoolean(DAPIKeys.DATA_HAS_SECA_FEATURE_BOOL);

        FeaturedContentHelper featuredContentHelper = new FeaturedContentHelper(mWorkoutMap);
        for(int id : featuredContentHelper.getCarouselWorkoutIds()){
            if(mWorkoutMap.getWorkout(id) != null) {
                carouselWorkouts.add(mWorkoutMap.getWorkout(id));
            }
        }

        // Adding seca launch into the vortex carousel for seca users. Inserting at index 0 which is
        // the initial focused page on vortex. Chameleon UI has logic to determine the center index and place
        // featured content there when applicable. See orderPagerWorkouts in WorkoutSelectionViewModel
        if (isSecaUser && !isGuest){
            carouselWorkouts.add(0, mWorkoutMap.getWorkout(WorkoutIds.SECA_PACE));
        }
        return carouselWorkouts;
    }

    /**
     * Returns the list of workout ids to be displayed as featured workouts below the workout selection carousel
     */
    public List<WorkoutItem> getFeaturedWorkouts(){
        List<WorkoutItem> featuredWorkouts = new ArrayList<>();
        FeaturedContentHelper featuredContentHelper = new FeaturedContentHelper(mWorkoutMap);
        for(int id : featuredContentHelper.getFeaturedWorkoutIds()){
            if(mWorkoutMap.getWorkout(id) != null) {
                //Include all the VA workouts as featured items
                if (id == WorkoutIds.VIRTUAL_ACTIVE){
                    featuredWorkouts.addAll(getVAWorkouts());
                }
                else {
                    featuredWorkouts.add(mWorkoutMap.getWorkout(id));
                }
            }
        }
        return featuredWorkouts;
    }

    public List<WorkoutItem> getVAWorkouts(){
        // Update the list and return it (don't cache in case the list changed)
        mVaWorkoutList = mWorkoutManager.getVaWorkouts(context);
        return mVaWorkoutList;
    }

    /**
     * Starts the workout with the initial conditions, this method also handles adding some values
     * for featured and landmark workouts.
     *
     * @param workoutId The workout to start
     * @param list      the list of initial conditions
     */
    public void startWorkout(String workoutId, List<WorkoutParameter> list) {
        List<WorkoutInput> initialConditions = new ArrayList<>();
        if (list != null) {
            for (WorkoutParameter iInputConfiguration : list) {
                MinMaxParam unit = WorkoutInputManager.getUnitType(iInputConfiguration.identifier());
                if (unit != null) {
                    // Make sure the units are correct when passing to the workout controller (metric)
                    float conversionFactor = getConversionFactor(unit);
                    Log.e(TAG, "JSInterfaceGo: identifier: " + iInputConfiguration.identifier() + " value: " + iInputConfiguration.value() + " convert" + conversionFactor);
                    initialConditions.add(new WorkoutInput(iInputConfiguration.identifier(), (float) (iInputConfiguration.valueAsDouble() / conversionFactor)));
                }
            }
        }

        //Time is set as the video length for featured workouts
        if (mCurrentWorkout.contains(LANDMARKS_PREFIX)) {
            LandmarkWorkoutItem currentLandmark = LandmarkDataKt.getLandmarkDataFromTitle(workoutId.split(":")[1]); // workoutId = "Landmarks:actual_landmark_title"
            int floors = LandmarkDataKt.getFloorsFromLandmarkItem(currentLandmark);
            initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.FLOORS_GOAL_START, floors));
        }

        mWorkoutManager.startLoadedWorkout(initialConditions, false);
    }

    /**
     * Starts a custom workout with the intervals defined by the passed in CustomWorkoutItem.
     *
     * @param customWorkoutItem The definition for the custom workout
     */
    public void startCustomWorkout(CustomWorkoutItem customWorkoutItem) {
        String script = CustomWorkoutHelper.getWorkoutScript(
                customWorkoutItem,
                DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL),
                customWorkoutItem.hasCooldown || ProductConfig.forcesCustomWorkoutsToHaveCooldowns()
        );
        mWorkoutManager.loadCustomWorkout(WorkoutIds.CUSTOM_WORKOUT, script,
                workoutInputs -> mWorkoutManager.startLoadedWorkout(null, false));
    }

    /**
     * Starts a Sprint8 workout with the defined media type and level
     *
     * @param level             The level of the Sprint8 workout
     */
    public void startSprint8(int level) {
        // Load the Sprint8 workout to be sure it's loaded, then start the workout
        mWorkoutManager.loadWorkoutWithId(WorkoutIds.SPRINT8, workoutInputs -> {
            //Regular Sprint8 has 8 sprints
            DataMatrix.putInt(Sprint8Keys.DATA_SPRINT8_NUM_SPRINTS_INT, 8);
            List<WorkoutInput> initialConditions = new ArrayList<>();
            initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.LEVEL, level));
            mWorkoutManager.startLoadedWorkout(initialConditions, false);
        });
    }

    /**
     * Starts a custom Sprint8 workout
     *
     * @param list              The list of intervals for the Sprint8 workout
     */
    public void startSprint8Custom(List<Sprint8WorkoutData> list) {
        BaseSprint8Config customSprint8Config = createCustomSprint8Config(list);
        Integer numberOfSprints = list.get(0).intervalData.get(0).size() / 2;
        startSprint8Custom(customSprint8Config, numberOfSprints);
    }

    /**
     * Starts a custom Sprint8 workout
     *
     * @param config            The config for the Sprint8 workout
     * @param numberOfSprints   The number of sprints for the Sprint8 workout
     */
    public void startSprint8Custom(BaseSprint8Config config, Integer numberOfSprints) {
        DataMatrix.putInt(Sprint8Keys.DATA_SPRINT8_NUM_SPRINTS_INT, numberOfSprints);
        DataMatrix.putParcelable(WorkoutKeys.ACTION_SET_SPRINT8_WORKOUT_INFO_PARCELABLE, config);
        mWorkoutManager.startLoadedWorkout(null, false);
    }

    public interface ISprint8ModelInfo {
        void receivedModelInfo(List<Sprint8WorkoutData> sprint8WorkoutData);
    }

    public interface IModelInfo {
        void receivedModelInfo(WorkoutModel workoutModel);
    }

    /**
     * Loads a Sprint8 workout into the workout controller and gets the model (a list of the events
     * that will happen) for the Sprint8 workout. This is called so that we can show the Sprint8
     * graph correctly. When this is called the key {@link WorkoutKeys#EVENT_MODEL_INFORMATION_RESPONSE_PARCELABLE}
     * is updated with the Sprint8 model info.
     */
    public void retrieveSprint8WorkoutData(ISprint8ModelInfo callback) {
        // Setup callback for model information
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createParcelableAdapter(WorkoutKeys.EVENT_MODEL_INFORMATION_RESPONSE_PARCELABLE, WorkoutModel.class, mModel -> {
            if (callback != null) {
                List<Sprint8WorkoutData> sprint8WorkoutData = convertSprint8ModelData(mModel);
                callback.receivedModelInfo(sprint8WorkoutData);
            }
            dataMatrix.finish();
        });

        // Load the sprint8 workout and tell the workout controller to calculate the model.
        // workoutInputs is null when Sprint8 is not available on the machine (Climbmill, or sprinter)
        mWorkoutManager.loadWorkoutWithId(WorkoutIds.SPRINT8, workoutInputs -> {
            if (workoutInputs != null) {
                DataMatrix.sendEvent(WorkoutKeys.ACTION_GET_MODEL); // Loads the model so we can graph Sprint8
            } else {
                // Sprint8 not supported
                if (callback != null) {
                    callback.receivedModelInfo(null);
                }
                dataMatrix.finish();
            }
        });
    }

    /**
     * Gets workout information from the currently loaded workout
     */
    public void retrieveModelInfo(IModelInfo modelInfoCallback) {
        // Setup callback for model information
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createParcelableAdapter(WorkoutKeys.EVENT_MODEL_INFORMATION_RESPONSE_PARCELABLE, WorkoutModel.class, modelInfo -> {
            if (modelInfoCallback != null) {
                modelInfoCallback.receivedModelInfo(modelInfo);
            }
            dataMatrix.finish();
        }, false);

        DataMatrix.sendEvent(WorkoutKeys.ACTION_GET_MODEL);
    }

    /**
     * Takes a WorkoutModel for Sprint8 and converts it into a form usable by the GoScreenModule
     * to display Sprint8 graph information.
     *
     * @param sprint8Model The model to convert
     * @return The converted data
     */
    public List<Sprint8WorkoutData> convertSprint8ModelData(com.jht.workoutcontrollerlib.model.WorkoutModel sprint8Model) {
        List<Sprint8WorkoutData> sprint8WorkoutData = new ArrayList<>();
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        switch (config.machineClassType) {
            case Ascent:
                List<List<Float>> ascentResistanceChanges = getValuesForParameter(sprint8Model, ConsoleWorkoutVariables.RESISTANCE, 1);
                ascentResistanceChanges = capValuesToMax(ascentResistanceChanges, config.resistance.getMax());
                List<List<Float>> ascentInclineChanges = getValuesForParameter(sprint8Model, ConsoleWorkoutVariables.INCLINE, 1);
                ascentInclineChanges = capValuesToMax(ascentInclineChanges, DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_INCLINE_FLOAT));
                sprint8WorkoutData.add(new Sprint8WorkoutData(getMatchingUserInput(ConsoleWorkoutVariables.START_RESISTANCE), ascentResistanceChanges));
                sprint8WorkoutData.add(new Sprint8WorkoutData(getMatchingUserInput(ConsoleWorkoutVariables.START_INCLINE), ascentInclineChanges));
                break;
            case Bike:
            case SpinBike:
            case Elliptical:
            case RecumbentElliptical:
            case Climbmill:
            case Stepper:
                List<List<Float>> resistanceChanges = getValuesForParameter(sprint8Model, ConsoleWorkoutVariables.RESISTANCE, 1);
                resistanceChanges = capValuesToMax(resistanceChanges, config.resistance.getMax());
                sprint8WorkoutData.add(new Sprint8WorkoutData(getMatchingUserInput(ConsoleWorkoutVariables.START_RESISTANCE), resistanceChanges));
                break;
            case Treadmill:
            default:
                boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
                float conversionFactor = isMetric ? 1f : SpeedConvertUtil.convertKphToMph(1);
                List<List<Float>> tmSpeedChanges = getValuesForParameter(sprint8Model, ConsoleWorkoutVariables.SPEED, conversionFactor);
                float maxSpeed = isMetric ? config.speedKph.getMax() : SpeedConvertUtil.convertMphToKph(config.speedMph.getMax());
                tmSpeedChanges = capValuesToMax(tmSpeedChanges, maxSpeed * conversionFactor);
                List<List<Float>> tmInclineChanges = getValuesForParameter(sprint8Model, ConsoleWorkoutVariables.INCLINE, 1);
                tmInclineChanges = capValuesToMax(tmInclineChanges, DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_INCLINE_FLOAT));
                sprint8WorkoutData.add(new Sprint8WorkoutData(getMatchingUserInput(ConsoleWorkoutVariables.START_SPEED), tmSpeedChanges));
                sprint8WorkoutData.add(new Sprint8WorkoutData(getMatchingUserInput(ConsoleWorkoutVariables.START_INCLINE), tmInclineChanges));
                break;
        }

        return sprint8WorkoutData;
    }

    /**
     * Gets the parameters that should be displayed by a custom workout based on the current frame
     *
     * @return The parameters for a custom workout
     */
    public List<WorkoutParameter> getCustomWorkoutParameters() {
        List<WorkoutParameter> customWorkoutParameters = new ArrayList<>();

        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        switch (config.machineClassType) {
            case Treadmill:
                customWorkoutParameters.add(new WorkoutParameter(getMatchingUserInput(ConsoleWorkoutVariables.START_INCLINE), Paint.Style.FILL, 0xFFFDAF1B, context.getString(R.string.segment_incline_tc)));
                customWorkoutParameters.add(new WorkoutParameter(getMatchingUserInput(ConsoleWorkoutVariables.START_SPEED), Paint.Style.FILL_AND_STROKE, 0xFFFF0000, context.getString(R.string.segment_speed_tc)));
                break;
            case Ascent:
                customWorkoutParameters.add(new WorkoutParameter(getMatchingUserInput(ConsoleWorkoutVariables.START_INCLINE), Paint.Style.FILL, 0xFFFDAF1B, context.getString(R.string.segment_incline_tc)));
                customWorkoutParameters.add(new WorkoutParameter(getMatchingUserInput(ConsoleWorkoutVariables.START_RESISTANCE), Paint.Style.FILL_AND_STROKE, 0xFFFF0000, context.getString(R.string.segment_resistance_tc)));
                break;
            default:
                customWorkoutParameters.add(new WorkoutParameter(getMatchingUserInput(ConsoleWorkoutVariables.START_RESISTANCE), Paint.Style.FILL_AND_STROKE, 0xFFFF0000, context.getString(R.string.segment_resistance_tc)));
                break;
        }
        WorkoutParameter time = new WorkoutParameter(getMatchingUserInput(ConsoleWorkoutVariables.TIME_GOAL_START), context.getString(R.string.segment_time_tc));
        time.defaultValue(5);
        time.value(5);
        time.min(1);
        time.measurement(UnitMeasurement.Minute);
        customWorkoutParameters.add(time);

        return customWorkoutParameters;
    }

    /**
     * Converts workout inputs to workout parameters that are usable by the go screen. Also keeps
     * a list of parameters so values do not change between workouts on the same parameter.
     *
     * @param workoutInputs The inputs for the workout to convert into go screen parameters
     * @return A list of go screen parameters
     */
    private List<WorkoutParameter> getParameterListFromInputs(List<WorkoutInput> workoutInputs) {
        if (workoutInputs == null) {
            Log.e(TAG, "Workout parameters cannot be null");
            return null;
        }

        List<WorkoutParameter> inputList = new ArrayList<>();
        for (WorkoutInput input : workoutInputs) {
            String parameterId = input.identifier();
            //If landmarks, floors goal is set from the selected landmark
            if (mCurrentWorkout.contains(LANDMARKS_PREFIX) && parameterId.equals(ConsoleWorkoutVariables.FLOORS_GOAL_START)) {
                Log.d(TAG, "Current Workout Contains Landmarks");
                continue;
            }
            WorkoutParameter inputConfiguration = getMatchingUserInput(parameterId);
            if (inputConfiguration != null) {
                inputList.add(inputConfiguration);
            }
        }
        return inputList;
    }

    private TreadmillSprint8Config getTreadmillConfig(List<Sprint8WorkoutData> sprint8Data) {
        TreadmillSprint8Config treadmillSprint8Config = new TreadmillSprint8Config();

        List<Float> recoverySpeed = new ArrayList<>();
        List<Float> sprintSpeed = new ArrayList<>();
        List<Float> recoveryIncline = new ArrayList<>();
        List<Float> sprintIncline = new ArrayList<>();

        loadArrays(sprint8Data, ConsoleWorkoutVariables.START_SPEED, recoverySpeed, sprintSpeed, ConsoleWorkoutVariables.START_INCLINE, recoveryIncline, sprintIncline);

        treadmillSprint8Config.setWarmup(recoverySpeed.get(0), recoveryIncline.get(0));

        for (int i = 0; i < sprintSpeed.size(); i++) {
            treadmillSprint8Config.addInterval(sprintSpeed.get(i), sprintIncline.get(i), recoverySpeed.get(i + 1), recoveryIncline.get(i + 1));
        }

        return treadmillSprint8Config;
    }

    private AscentSprint8Config getAscentConfig(List<Sprint8WorkoutData> sprint8Data) {
        AscentSprint8Config ascentSprint8Config = new AscentSprint8Config();

        List<Float> recoveryResistance = new ArrayList<>();
        List<Float> sprintResistance = new ArrayList<>();
        List<Float> recoveryIncline = new ArrayList<>();
        List<Float> sprintIncline = new ArrayList<>();

        loadArrays(sprint8Data, ConsoleWorkoutVariables.START_RESISTANCE, recoveryResistance, sprintResistance, ConsoleWorkoutVariables.START_INCLINE, recoveryIncline, sprintIncline);

        ascentSprint8Config.setWarmup(recoveryResistance.get(0), recoveryIncline.get(0));

        for (int i = 0; i < sprintResistance.size(); i++) {
            ascentSprint8Config.addInterval(sprintResistance.get(i), sprintIncline.get(i), recoveryResistance.get(i + 1), recoveryIncline.get(i + 1));
        }

        return ascentSprint8Config;
    }

    private ResistanceSprint8Config getResistanceConfig(List<Sprint8WorkoutData> sprint8Data) {
        ResistanceSprint8Config resistanceSprint8Config = new ResistanceSprint8Config();

        List<Float> recoveryResistance = new ArrayList<>();
        List<Float> sprintResistance = new ArrayList<>();

        loadArrays(sprint8Data, ConsoleWorkoutVariables.START_RESISTANCE, recoveryResistance, sprintResistance, null, null, null);

        resistanceSprint8Config.setWarmup(recoveryResistance.get(0));

        for (int i = 0; i < sprintResistance.size(); i++) {
            resistanceSprint8Config.addInterval(sprintResistance.get(i), recoveryResistance.get(i + 1));
        }

        return resistanceSprint8Config;
    }

    private BaseSprint8Config createCustomSprint8Config(List<Sprint8WorkoutData> sprint8Data) {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        switch (frameConfig.machineClassType) {
            case Elliptical:
            case RecumbentElliptical:
            case Bike:
            case SpinBike:
            case Climbmill:
            case Stepper:
                return getResistanceConfig(sprint8Data);
            case Ascent:
                return getAscentConfig(sprint8Data);
            case Treadmill:
            default:
                return getTreadmillConfig(sprint8Data);
        }
    }

    private void loadArrays(List<Sprint8WorkoutData> sprint8Data,
                            String param1, List<Float> recoveryVals1, List<Float> sprintVals1,
                            String param2, List<Float> recoveryVals2, List<Float> sprintVals2) {
        for (Sprint8WorkoutData data : sprint8Data) {
            List<Float> recoveryVals = null;
            List<Float> sprintVals = null;

            // If this is for speed then we need to convert imperial values into metric for the workout controller
            float conversionVar;
            if (data.workoutParameter.identifier().equals(ConsoleWorkoutVariables.START_SPEED) &&
                    !DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)) {
                conversionVar = SpeedConvertUtil.convertMphToKph(1);
            } else {
                conversionVar = 1;
            }

            if (param1 != null && data.workoutParameter.identifier().equals(param1)) {
                recoveryVals = recoveryVals1;
                sprintVals = sprintVals1;
            } else if (param2 != null && data.workoutParameter.identifier().equals(param2)) {
                recoveryVals = recoveryVals2;
                sprintVals = sprintVals2;
            }

            if (recoveryVals != null && sprintVals != null) {
                for (int i = 0; i < data.intervalData.get(0).size(); i++) {
                    if ((i & 1) == 0) {
                        // Even, so it's a recovery value
                        recoveryVals.add(data.intervalData.get(0).get(i) * conversionVar);
                    } else {
                        // Odd, so it's a sprint value
                        sprintVals.add(data.intervalData.get(0).get(i) * conversionVar);
                    }
                }
            }
        }
    }

    private WorkoutParameter getMatchingUserInput(String workoutParameter) {
        if (mInputMap.containsKey(workoutParameter)) {
            if (workoutParameter.equals(ConsoleWorkoutVariables.LEVEL)) validateLevelParameter();
            return mInputMap.get(workoutParameter);
        } else {
            MinMaxParam unit = WorkoutInputManager.getUnitType(workoutParameter);
            if (unit != null) {
                MinMaxDictionary minMaxDictionary = MinMaxDictionary.getMinMaxDictionary(unit);
                WorkoutParameter config = new WorkoutParameter(
                        minMaxDictionary.min,
                        minMaxDictionary.max,
                        minMaxDictionary.defaultValue,
                        minMaxDictionary.stepSize,
                        minMaxDictionary.precision,
                        minMaxDictionary.captionText,
                        minMaxDictionary.unitString,
                        minMaxDictionary.measurement,
                        workoutParameter);

                if (workoutParameter.equals(ConsoleWorkoutVariables.WEIGHT))
                    config.showProgress(false);

                mInputMap.put(workoutParameter, config);
                return config;
            }
            return null;
        }
    }

    /**
     * Since the level parameter can change max value between workouts we need to check if the current
     * level is still equal to the previous one or we need to reset it
     */
    private void validateLevelParameter() {
        WorkoutParameter configuration = mInputMap.get(ConsoleWorkoutVariables.LEVEL);
        if (configuration != null) {
            MinMaxParam unit = WorkoutInputManager.getUnitType(ConsoleWorkoutVariables.LEVEL);
            if (unit != null) {
                MinMaxDictionary minMaxDictionary = MinMaxDictionary.getMinMaxDictionary(unit);
                configuration.max(minMaxDictionary.max);
            }
        }
    }

    private List<List<Float>> getValuesForParameter(com.jht.workoutcontrollerlib.model.WorkoutModel sprint8Model, String parameter, float conversionFactor) {
        List<List<Float>> parameterInfo = new ArrayList<>();
        int currentLevel;
        for (StageModel stage : sprint8Model.getStages()) {

            float warmupValue = -1;
            for (DataModel data : stage.getDefaultValues()) {
                if (data.getParameter().equals(parameter)) {
                    warmupValue = data.getValue();
                }
            }

            currentLevel = 0;
            for (LevelModel level : stage.getLevels()) {
                if (parameterInfo.size() <= currentLevel) {
                    parameterInfo.add(new ArrayList<>());
                }

                List<Float> levelParamValues = parameterInfo.get(currentLevel);
                if (warmupValue >= 0) levelParamValues.add(warmupValue * conversionFactor);

                for (DataModel data : level.getDefaultValues()) {
                    if (data.getParameter().equals(parameter)) {
                        levelParamValues.add(data.getValue() * conversionFactor);
                    }
                }

                for (IntervalModel interval : level.getIntervals()) {
                    for (DataModel data : interval.getDefaultValues()) {
                        if (data.getParameter().equals(parameter)) {
                            levelParamValues.add(data.getValue() * conversionFactor);
                        }
                    }
                }
                currentLevel++;
            }
        }
        return parameterInfo;
    }

    private List<List<Float>> capValuesToMax(List<List<Float>> modelValues, double max) {
        List<List<Float>> updateModelValues = new ArrayList<>();
        for (List<Float> levelValues : modelValues) {
            List<Float> updatedLevelValues = new ArrayList<>();
            for (int i = 0; i < levelValues.size(); i++) {
                Float value = levelValues.get(i);
                if (value >= max) value = (float) max;
                updatedLevelValues.add(value);
            }

            updateModelValues.add(updatedLevelValues);
        }

        return updateModelValues;
    }

    /**
     * Converts units to their metric equivalents to pass to the workout controller
     *
     * @param input The input to convert
     * @return the value for the current value to send as an initial condition
     */
    private float getConversionFactor(MinMaxParam input) {
        boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
        float conversionFactor;
        if (!isMetric) {
            if (input == MinMaxParam.Speed || input == MinMaxParam.MaxSpeed ||  input == MinMaxParam.TargetSpeed
                    || input == MinMaxParam.MaxSpeedPair || input == MinMaxParam.MinSpeedPair) {
                conversionFactor = SpeedConvertUtil.convertKphToMph(1);
            } else if (input == MinMaxParam.Distance) {
                conversionFactor = (DistanceConvertUtil.convertKilometersToMiles(1));
            } else if (input == MinMaxParam.Weight) {
                conversionFactor = (WeightConvertUtil.convertKilogramsToPounds(1));
            } else if (input == MinMaxParam.Height) {
                conversionFactor = (DistanceConvertUtil.convert(1, DistanceConvertUtil.CENTIMETERS_ID, DistanceConvertUtil.INCHES_ID));
            } else {
                conversionFactor = 1f;
            }
        } else {
            conversionFactor = 1f;
        }
        return conversionFactor;
    }

     /* DEPRECATED code for the Legacy UI */
    /**
     * Gets a list of the landmarks workouts available in Chimera
     *
     * @param context The context to get the correct strings
     * @return The list of available landmarks workouts
     */
    @Deprecated
    public List<GoScreenWorkoutDisplayItem> setLandmarksWorkouts(Context context) {
        LandmarkLoader.getInstance().reset();
        List<GoScreenWorkoutDisplayItem> landmarksWorkouts = new ArrayList<>();
        List<LandmarkWorkoutItem> mLandmarksWorkoutList = LandmarkDataKt.getLandmarkWorkoutItemList();
        int numFloorsCalculated = 0;
        for (int i = 0; i < mLandmarksWorkoutList.size(); i++) {
            numFloorsCalculated = LandmarkDataKt.getFloorsFromLandmarkItem(mLandmarksWorkoutList.get(i));

            GoScreenWorkoutDisplayItem temp = new GoScreenWorkoutDisplayItem(
                    StringLookup.getString(mLandmarksWorkoutList.get(i).getTitleId(), context),
                    StringLookup.getString(mLandmarksWorkoutList.get(i).getLocationTitle(), context),
                    numFloorsCalculated + " " + StringLookup.getString(R.string.floors_uc, context),
                    StringLookup.getString(mLandmarksWorkoutList.get(i).getDescriptionId(), context),
                    mLandmarksWorkoutList.get(i).getBackground().get(0), // background = first in list of backgrounds
                    mLandmarksWorkoutList.get(i).getSilhouetteUri(), // silhouette for thumbnail? this is what was before...
                    // more of that identifier stuff that is actually LANDMARKS_PREFIX:actual_landmark_title that I made the optionalId for Landmarks
                    mLandmarksWorkoutList.get(i).getOptionalId()// optionalId
            );

            landmarksWorkouts.add(temp);

        }
        return landmarksWorkouts;
    }

    //Categories for the Go Screen
    private final int[] trainingCategoryIds = {WorkoutIds.MANUAL, WorkoutIds.CUSTOM_WORKOUT, WorkoutIds.TARGET_TRAINING, WorkoutIds.ROLLING_HILLS, WorkoutIds.FAT_BURN,
            WorkoutIds.INTERVALS, WorkoutIds.INTERVALS_SPEED, WorkoutIds.PACE_TIME_GOAL, WorkoutIds.PACE_DISTANCE_GOAL, WorkoutIds.CONSTANT_WATTS,
            WorkoutIds.GLUTES, WorkoutIds.SPIN_BIKE_INTERVALS_DISTANCE, WorkoutIds.SPIN_BIKE_INTERVALS_TIME};

    private final int[] goalCategoryIds = {WorkoutIds.TIME_GOAL, WorkoutIds.DISTANCE_GOAL, WorkoutIds.FLOORS_GOAL, WorkoutIds.CALORIES_GOAL};

    private final int[] targetHeartRateCategoryIds = {WorkoutIds.THR_INCLINE, WorkoutIds.THR_SPEED, WorkoutIds.THR_RESISTANCE};

    private final int[] fitnessTestCategoryIds = {WorkoutIds.ARMY_FITNESS_TEST, WorkoutIds.MARINES_FITNESS_TEST, WorkoutIds.AIR_FORCE_FITNESS_TEST, WorkoutIds.NAVY_FITNESS_TEST,
            WorkoutIds.PEB_FITNESS_TEST, WorkoutIds.GERKIN_FITNESS_TEST, WorkoutIds.CPAT_FITNESS_TEST, WorkoutIds.SUBMAX_FITNESS_TEST, WorkoutIds.WFI_FITNESS_TEST,
            WorkoutIds.STEPPER_FITNESS_TEST,  WorkoutIds.FTP_TEST_20, WorkoutIds.FTP_TEST_8,  WorkoutIds.COOPER_FITNESS_TEST, WorkoutIds.MAX_HR_TEST,};

    /**
     * Gets the displayed workouts from the database and stores the available workouts
     *
     * @param context The context
     * @return The list of workout items to display
     */
    @Deprecated
    public List<GoScreenWorkoutDisplayItem> getDisplayedWorkouts(Context context) {
        List<GoScreenWorkoutDisplayItem> workoutItems = new ArrayList<>();

        // Gets the displayed workout categories
        GoScreenWorkoutDisplayItem sectionTitle = new GoScreenWorkoutDisplayItem(context.getResources().getString(R.string.exercises_in_uc), GoScreenWorkoutDisplayItem.CATEGORY_TITLE_TYPE);
        workoutItems.add(sectionTitle);

        // Add training
        workoutItems.add(new GoScreenWorkoutDisplayItem(context.getString((ProductConfig.isLightCommercial() ? R.string.training_tc : R.string.training_workouts_tc)),
                GoScreenWorkoutDisplayItem.CATEGORY_SUBTITLE_TITLE));
        for(int trainingId: trainingCategoryIds){
            WorkoutItem workout = mWorkoutMap.getWorkout(trainingId);
            if(workout != null){
                workoutItems.add(new GoScreenWorkoutDisplayItem(context, workout));
            }
        }

        //Add Goals
        workoutItems.add(new GoScreenWorkoutDisplayItem(context.getString(R.string.goals_tc), GoScreenWorkoutDisplayItem.CATEGORY_SUBTITLE_TITLE));
        for(int goalId: goalCategoryIds){
            WorkoutItem workout = mWorkoutMap.getWorkout(goalId);
            if(workout != null){
                workoutItems.add(new GoScreenWorkoutDisplayItem(context, workout));
            }
        }
        //Add Marathon if enabled
        if (DataMatrix.getBoolean(SettingKeys.Workout.DATA_SHOW_MARATHON_MODE_BOOL)) {
            workoutItems.add(new GoScreenWorkoutDisplayItem(context.getString(R.string.marathon_tc), GoScreenWorkoutDisplayItem.CATEGORY_SUBTITLE_TITLE));
            WorkoutItem workout = mWorkoutMap.getWorkout(WorkoutIds.MARATHON);
            if(workout != null){
                workoutItems.add(new GoScreenWorkoutDisplayItem(context, workout));
            }
        }

        //Add Target Heart Rate
        workoutItems.add(new GoScreenWorkoutDisplayItem(context.getString(R.string.target_heart_rate_tc), GoScreenWorkoutDisplayItem.CATEGORY_SUBTITLE_TITLE));
        for(int targetHeartRateId: targetHeartRateCategoryIds){
            WorkoutItem workout = mWorkoutMap.getWorkout(targetHeartRateId);
            if(workout != null){
                workoutItems.add(new GoScreenWorkoutDisplayItem(context, workout));
            }
        }

        //Add Fitness Tests
        List<GoScreenWorkoutDisplayItem> fitnessTestItems = new ArrayList<>();
        for(int fitnessId: fitnessTestCategoryIds){
            WorkoutItem workout = mWorkoutMap.getWorkout(fitnessId);
            if(workout != null){
                fitnessTestItems.add(new GoScreenWorkoutDisplayItem(context, workout));
            }
        }

        if (!fitnessTestItems.isEmpty()) {
            workoutItems.add(new GoScreenWorkoutDisplayItem(context.getString(R.string.fitness_test_tc), GoScreenWorkoutDisplayItem.CATEGORY_SUBTITLE_TITLE));
            workoutItems.addAll(fitnessTestItems);
        }

        return workoutItems;
    }

    /**
     * Gets the current VA workouts from the DataMatrix and sends them back in a format usable
     * by the go screen module.
     *
     * @param context The context to get the strings and background correctly
     * @return The list of available VA workouts
     */
    @Deprecated
    public List<GoScreenWorkoutDisplayItem> setVaWorkouts(Context context) {
        mVaWorkoutList = mWorkoutManager.getVaWorkouts(context);
        List<GoScreenWorkoutDisplayItem> vaGoScreenItems = new ArrayList<>();

        for (final WorkoutItem vaItem : mVaWorkoutList) {
            GoScreenWorkoutDisplayItem item = new GoScreenWorkoutDisplayItem(context, vaItem);
            item.identifier = VA_PREFIX + vaItem.getOptionalId();
            vaGoScreenItems.add(item);
        }
        return vaGoScreenItems;
    }

    /**
     * Deprecated:
     * Replaced with {@link #getCarouselWorkouts()} for the carousel workout ids
     * Replaced with {@link #getFeaturedWorkouts()} for the featured workout ids
     * <p>
     * Gets the featured workouts for the go screen modules. Populates both lists with the correct
     * workouts and items depending on frame and which workouts are available.
     * <p>
     * The main featured item will be the prominent item which featured items will be on the front
     * page below.
     *
     * @param mainFeaturedItems the main items for the go screen
     * @param featuredItems     the featured items for the go screen
     */
    @Deprecated
    public void setMainAndFeaturedWorkouts(List<FeaturedItem> mainFeaturedItems, List<FeaturedItem> featuredItems) {
        FeaturedContentHelper featuredContentHelper = new FeaturedContentHelper(mWorkoutMap);
        mainFeaturedItems.clear();
        featuredItems.clear();
        for(int carouselId : featuredContentHelper.getCarouselWorkoutIds()){
            mainFeaturedItems.add(featuredContentHelper.getFeaturedItem(carouselId, context, mWorkoutMap));
        }
        for(int featureId : featuredContentHelper.getFeaturedWorkoutIds()){
            if(featureId == WorkoutIds.VIRTUAL_ACTIVE){
                boolean hasVa = mVaWorkoutList != null && !mVaWorkoutList.isEmpty();
                if(hasVa){
                    featuredItems.add(featuredContentHelper.getFeaturedItem(featureId, context, mWorkoutMap));
                }
            }else{
                featuredItems.add(featuredContentHelper.getFeaturedItem(featureId, context, mWorkoutMap));
            }
        }
    }

    /**
     * Clears the cache of inputs so we will get the default input values again
     */
    @Deprecated
    public void clearInputCache() {
        mInputMap.clear();
    }

}
