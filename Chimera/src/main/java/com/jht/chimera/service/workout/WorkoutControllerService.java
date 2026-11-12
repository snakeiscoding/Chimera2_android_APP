package com.jht.chimera.service.workout;

import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_CLIMBMILL_CZ;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_CLIMBMILL_IR;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.GOAL_TYPE_CALORIES;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.GOAL_TYPE_CALORIES_STAGE;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.GOAL_TYPE_DISTANCE;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.GOAL_TYPE_DISTANCE_STAGE;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.GOAL_TYPE_TIME;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.GOAL_TYPE_TIME_STAGE;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.WORKOUT_STAGE_WORKOUT;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.math.MathUtils;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ProfileValue;
import com.jht.chimera.lib.common.WorkoutFuture;
import com.jht.chimera.lib.common.WorkoutFutureProfile;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.data.RangeInfo;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.Sprint8Keys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.lib.common.log.ConsoleEvent;
import com.jht.chimera.lib.common.stats.WorkoutSummary;
import com.jht.chimera.lib.integrations.xarefit.XareFitLib;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workout.StringInputList;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.lib.workout.XmlWorkoutEntry;
import com.jht.chimera.lib.workout.generator.BaseSprint8Config;
import com.jht.chimera.lib.workout.generator.Sprint8Generator;
import com.jht.chimera.lib.workout.workoutlist.WorkoutEntry;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.lib.workoutapps.AppInfo;
import com.jht.chimera.lib.workoutapps.AppManager;
import com.jht.chimera.service.workout.SafeToStartFlags;
import com.jht.chimera.service.ui.panels.PanelIds;
import com.jht.chimera.service.workout.stat.WorkoutDataContainer;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.seca2.SecaActivity;
import com.jht.services.ChildConsoleService;
import com.jht.services.ConsoleServiceState;
import com.jht.workoutcontrollerlib.WorkoutController;
import com.jht.workoutcontrollerlib.future.FutureConfig;
import com.jht.workoutcontrollerlib.future.GoalDependentValue;
import com.jht.workoutcontrollerlib.future.WorkoutProfile;
import com.jht.workoutcontrollerlib.legacyconversion.XmlToScript;
import com.jht.workoutcontrollerlib.model.DataModel;
import com.jht.workoutcontrollerlib.model.WorkoutModel;
import com.jht.workoutcontrollerlib.parser.ScriptException;
import com.jht.workoutcontrollerlib.workoutcardio.CardioDefinitionInfo;
import com.jht.workoutcontrollerlib.workoutcardio.WorkoutControllerCardio;
import com.jht.workoutcontrollerlib.workoutcardio.WorkoutScriptVariables;
import com.jht.workoutcontrollerlib.workoutcardio.WorkoutSettings;
import com.jht.workoutcontrollerlib.workoutdefinition.statements.SettingValue;
import com.jhtp.dapiservice.an.events.WorkoutEvent;
import com.jhtp.dapiservice.an.types.ButtonPressType;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import kotlin.Unit;


/**
 * Controls the workout and sends changes from the workout controller to the system. Also keeps
 * track of workout stats for each workout stage.
 */
public class WorkoutControllerService extends ChildConsoleService {
    private final String TAG = "WorkoutControllerSer...";

    private static final float TOLERANCE = 0.0005f;
    private static final int REFRESH_TIME_MS = 1000;
    @NonNls public static final String CAN_MODIFY_GOAL_SETTING = "allow_user_goal_changes"; // similar to WorkoutSettings constant, but currently only used outside of WorkoutController

    private CardioDefinitionInfo mWorkoutDefinition;
    private WorkoutEntry mWorkoutEntry;
    private WorkoutControllerCardio mController;
    private WorkoutLaunchServiceHelper workoutLaunchHelper;
    private StatUpdating mStatUpdate;

    private int mGoalType = -1;
    private int mLevel = 0;
    private boolean mAllowInclineChanges = true; // Used for Navy test
    private boolean mIsManualPause;
    private boolean mAllowAutoPause = true;
    private int resumeResistance = 0;
    private int resumeWatts = 0;
    private int mRpm0Counter = 0;
    private int mSpm0Counter = 0;
    private int mLastElapsedTimeSec = 0;
    private boolean mIsStopped = true;
    private boolean canShowWorkoutComplete = true;
    private boolean mBeltStop = false; // need to track if belt/CM was explicitly stopped so we don't start moving on resume
    private boolean mTreadSenseEnabled;
    private Timer mTreadSenseTimer = null;
    private boolean mTreadSenseStarted = false;
    private int mSprint8State = 0;
    private int mCooperWarningValue = 0;

    private final Object workoutTransitionLock = new Object();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final DataMatrix mDataMatrix = new DataMatrix();
    private final FrameConfig mFrameConfig = FrameConfigManager.getInstance().getFrameConfig(); // manager maintains single instance and copies into it when changed
    private final FrameControlDebounce frameControlDebounce = new FrameControlDebounce();

    @Override
    public void onCreate() {
        Log.d("ServiceStarter", "Starting " + this);
        super.onCreate();
        workoutLaunchHelper = new WorkoutLaunchServiceHelper(getApplicationContext());

        mDataMatrix.createBoolAdapter(HALKeys.DATA_HAL_SERVICE_READY_BOOL, ready -> {
            if (ready) {
                Log.d("ServiceStarter", "Initializing WorkoutControllerService");
                mDataMatrix.finish(); // remove this start up listener before setting up the regular listeners
                init();
                updateMyServiceState(ConsoleServiceState.READY);
                Log.d("ServiceStarter", "Done Initializing WorkoutControllerService");
            }
        });
        Log.d("ServiceStarter", "Ending " + this);
    }

    @Override
    public void onDestroy() {
        mDataMatrix.finish();
        mStatUpdate.finishDM();
        if (workoutLaunchHelper != null) workoutLaunchHelper.close();
        XareFitLib.getInstance().destroy();
        frameControlDebounce.close();
        super.onDestroy();
    }

    private void init() {
        mStatUpdate = new StatUpdating();
        resetValues();
        mController = new WorkoutControllerCardio(new WorkoutControllerCardio.ICardioWorkoutController() {
            @Override
            public void valueChangedEvent(String parameter, Float value) {
                workoutControllerOutput(parameter, value);
            }

            @Override
            public void allowUserGoalChangesUpdated(boolean allowUserGoalChanges) {
                DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_USER_GOAL_CHANGES_BOOL, allowUserGoalChanges);
            }

            @Override
            public void allowUserChangesUpdated(boolean allowUserChanges) {
                DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_USER_CHANGES_BOOL, allowUserChanges);
            }

            @Override
            public void allowPauseUpdated(boolean allowPause) {
                DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_USER_PAUSE_BOOL, allowPause);
            }

            @Override
            public void scriptExceptionOccurred() {
                stopWorkoutService();
            }

            @Override
            public void updateComplete() {
                DataMatrix.sendEvent(WorkoutKeys.ACTION_WORKOUT_STATS_UPDATED);
            }

            @Override
            public void goalProgressUpdated(float goal, float goalProgress, float goalRemaining) {
                updateGoalRemaining(goal, goalProgress, goalRemaining);
            }

            @Override
            public void intervalGoalProgressUpdated(float goal, float goalProgress, float goalRemaining) {
                DataMatrix.putFloat(WorkoutKeys.DATA_INTERVAL_GOAL_REMAINING_FLOAT, goalRemaining);
            }
        }, REFRESH_TIME_MS);

        mController.setDebugMode(false);

        FutureConfig futureConfig = new FutureConfig();
        futureConfig.setPeriod(new GoalDependentValue(REFRESH_TIME_MS, 0.1f, 1f, 1f))
                .setStopAtStageChange(false);

        mController.setFutureConfiguration(futureConfig);

        mController.addFutureCallback(profileList -> {
            WorkoutFuture futureProfile = convertProfileList(profileList);
            updateFutureProfile(futureProfile);
        });

        mStatUpdate.initializeStatUpdating(mController);

        // publish init values
        DataMatrix.putInt(WorkoutKeys.DATA_GOAL_TYPE_INT, -1);
        DataMatrix.putInt(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_TYPE_INT, -1);
        DataMatrix.putFloat(WorkoutKeys.DATA_GOAL_VALUE_FLOAT, 0);
        DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT, WORKOUT_STAGE_WORKOUT);
        DataMatrix.putFloat(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT, 0);

        initDataMatrix();
        initTreadSense();

        // Get an instance of XareFitLib
        XareFitLib.getInstance();


        int workoutState = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT);
        if (workoutState != WorkoutKeys.Constants.RUN_STATE_STOP) {
            Log.e(TAG, "Workout controller crashed in a workout. Stopping the workout and returning to main screen to prevent workout issues!");
            mIsStopped = false; // we are not actually stopped, so this flag should be false to allow stop routine will run
            stopWorkoutService(true, true); // full abort running workout
        }
    }

    private void updateGoalRemaining(float goal, float goalProgress, float goalRemaining) {
        int goalType = Math.round(mController.getVariableValue(ConsoleWorkoutVariables.GOAL_TYPE));

        // Distance goal needs special logic so it changes at the same time as the accumulating distance
        switch (goalType) {
            case WorkoutKeys.Constants.GOAL_TYPE_DISTANCE:
            case GOAL_TYPE_DISTANCE_STAGE:
                // Goal remaining is equal to the visible value for the goal minus the visible for goal progress
                boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
                if (isMetric) {
                    long distanceGoal = (long) (goal * 100 + 0.5);
                    double km = Math.floor(goalProgress * 1000) / 1000;
                    long distanceRun = (long) (km * 100);

                    goalRemaining = (float) ((distanceGoal - distanceRun) / 100.0);
                } else {
                    long distanceGoal = (long) (DistanceConvertUtil.convertKilometersToMiles(goal) * 100 + 0.5);
                    double miles = DistanceConvertUtil.convertKilometersToMiles((float) (Math.floor(goalProgress * 1000) / 1000));
                    long distanceRun = (long) (miles * 100);

                    goalRemaining = (float) ((distanceGoal - distanceRun) / 100.0);
                    goalRemaining = DistanceConvertUtil.convertMilesToKilometers(goalRemaining);
                }
                break;
        }

        DataMatrix.putFloat(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT, goalRemaining);

        int stageInt = mController.getStageIntFromName();

        if ((goalType == GOAL_TYPE_CALORIES || goalType == GOAL_TYPE_CALORIES_STAGE) && goalRemaining == 1.0) {
            //Log.d("GRAPH", "goalRemaining + "+goalRemaining+", reached full workout = true.");
            DataMatrix.putBoolean(WorkoutKeys.DATA_REACHED_FULL_WORKOUT_BOOL, true);
        } else if ((goalType == GOAL_TYPE_TIME || goalType == GOAL_TYPE_TIME_STAGE) && goalRemaining == 1000) {
            if (stageInt == WorkoutController.WORKOUT_STAGE_WORKOUT) {
                //Log.d("GRAPH", "goalRemaining + "+goalRemaining+", reached full workout = true..");
                DataMatrix.putBoolean(WorkoutKeys.DATA_REACHED_FULL_WORKOUT_BOOL, true);
            }
        } else if ((goalType == GOAL_TYPE_DISTANCE || goalType == GOAL_TYPE_DISTANCE_STAGE) && goalRemaining <= 0.11265408) {
            //Log.d("GRAPH", "goalRemaining + "+goalRemaining+", reached full workout = true...");
            DataMatrix.putBoolean(WorkoutKeys.DATA_REACHED_FULL_WORKOUT_BOOL, true);
        }

        if (stageInt == WorkoutController.WORKOUT_STAGE_WORKOUT) {
            DataMatrix.putFloat(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_REMAINING_FLOAT, goalRemaining);
        }
    }

    private void initDataMatrix() {
        //***** Keys that the service handles *****
        mDataMatrix.createParcelableAdapter(WorkoutKeys.ACTION_LOAD_WORKOUT_XML_PARCELABLE, XmlWorkoutEntry.class, this::loadWorkoutXml);
        mDataMatrix.createParcelableAdapter(WorkoutKeys.ACTION_LOAD_WORKOUT_ENTRY_PARCELABLE, WorkoutEntry.class, this::loadWorkoutEntry);
        mDataMatrix.createParcelableAdapter(WorkoutKeys.ACTION_SET_SPRINT8_WORKOUT_INFO_PARCELABLE, BaseSprint8Config.class, this::loadSprint8Config);
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_GET_MODEL, key -> getCurrentWorkoutModel());

        mDataMatrix.createParcelableAdapter(WorkoutKeys.ACTION_LAUNCH_WORKOUT_PARCELABLE, WorkoutLaunchData.class, this::launchWorkout);

        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_PAUSE_WORKOUT, key -> manualPauseWorkoutService());
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_RESUME_WORKOUT, key -> resumeWorkoutEvent());
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_STOP_WORKOUT, key -> stopWorkoutService());
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_ABORT_WORKOUT, key -> stopWorkoutService(true, false)); // don't force UI unless a workout was actually aborted
        mDataMatrix.createEventAdapter(HALKeys.ACTION_SPLASH_RESET, key -> stopWorkoutService(true, false)); // if we splash reset, abort any running workouts (do regular abort or use additional flags?)
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_INCREASE_WORKOUT_GOAL, key -> changeGoal(true)); // could these 2 events be 1 bool ?
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_DECREASE_WORKOUT_GOAL, key -> changeGoal(false));
        mDataMatrix.createIntAdapter(WorkoutKeys.ACTION_SET_TIME_GOAL_SECONDS_INT, this::changeTimeGoal);
        mDataMatrix.createFloatAdapter(WorkoutKeys.ACTION_UPDATE_WORKOUT_GOAL_FLOAT, this::updateGoal);
        mDataMatrix.createFloatAdapter(WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT, this::setSpeedUser);
        mDataMatrix.createFloatAdapter(WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT, this::setInclineUser);
        mDataMatrix.createIntAdapter(WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT, this::setResistanceUser);
        mDataMatrix.createBoolAdapter(HALKeys.ACTION_TM_REVERSE_BELT_BOOL, this::reverseTreadmillBelt);
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_SKIP_INTERVAL, key -> skipIntervalEvent());
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_SKIP_TO_COOLDOWN, key -> skipToCooldown());
        mDataMatrix.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING, frameType -> {
            FrameConfigManager.getInstance().refreshFrame();
            updateTreadSenseEnabled();
        });
        mDataMatrix.createFloatAdapter(WorkoutKeys.ACTION_SET_WORKOUT_SPEED_MULTIPLIER_FLOAT, multiplier -> mController.changeExecutionSpeedMultiplier(multiplier));
        mDataMatrix.createIntAdapter(HALKeys.DATA_SAFETY_KEY_STATUS_INT, value -> checkSafetyStatus());
        mDataMatrix.createBoolAdapter(HALKeys.DATA_SIMULATED_ESTOP_BOOL, value -> checkSafetyStatus());

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, value -> {
            int workoutElapsedTimeSec = value / 1000;
            if (workoutElapsedTimeSec != mLastElapsedTimeSec) {
                mLastElapsedTimeSec = workoutElapsedTimeSec;

                boolean isClimbmill = mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill;

                if (mFrameConfig.hasResistance() && !isClimbmill && mAllowAutoPause) {
                    // handle auto pause/resume logic
                    if (DataMatrix.getInt(HALKeys.DATA_RPM_INT) == 0) {
                        mRpm0Counter++;
                        if (mRpm0Counter >= 5 && mController.isRunning()) {
                            autoPauseWorkoutService();
                        }
                    }
                } else if (isClimbmill) {
                    checkSafetyStatus();

                    // handle auto pause logic to detect if the user has jumped off
                    if (DataMatrix.getInt(HALKeys.DATA_SPM_INT) == 0) {
                        mSpm0Counter++;
                        if (mSpm0Counter >= 5 && mController.isRunning()) {
                            // pause workout
                            DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT);
                            MessageCenterMessage message = new MessageCenterMessage("", StringLookup.get(R.string.workout_paused_no_spm_tc), 30);
                            DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message);
                        }
                    } else {
                        mSpm0Counter = 0;
                    }
                }
            }
        });

        mDataMatrix.createIntAdapter(HALKeys.DATA_RPM_INT, rpm -> {
            if (rpm > 0) {
                mRpm0Counter = 0;
                if (mController.isPaused()) {
                    autoResumeWorkoutService();
                }
            }
        });

        // Handle skipping the warmup
        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, value -> {
            //Reset treadsense if needed
            resetTreadSenseTimer();

            KeyPress.KeyCode keyCode = KeyPress.getKeyCode(value);
            KeyPress.KeyState keyState = KeyPress.getKeyState(value);
            ConsoleEvent.logEvent("Physical Keypress: " + keyCode + " State: " + keyState);
            boolean keyStateIsDown = keyState == KeyPress.KeyState.KEY_DOWN;
            if (keyStateIsDown && (keyCode == KeyPress.KeyCode.KEY_GO)) {
                // NOTE: Everything in here is meant for during a workout, the calls below all have checks to see if one is running
                if (mController.isPaused()) {
                    //Stores Go physical button press event for Dapi Analytics
                    DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.GO_QUICKSTART_PHYSICAL_BTN_PRESS.name);
                    resumeWorkoutService();
                } else {
                    if (ProductConfig.hasPausePlayButton()) {  // parker has a pause/play button
                        manualPauseWorkoutService();
                    } else {
                        skipWarmup();
                    }
                }
            } else if (keyStateIsDown && (keyCode == KeyPress.KeyCode.KEY_COOLDOWN)) {
                skipToCooldown();
            } else if (keyStateIsDown && (keyCode == KeyPress.KeyCode.KEY_STOP)) {
                //Stores stop physical workout button press event for Dapi Analytics
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.STOP_WORKOUT_PHYSICAL_BTN_PRESS.name);
                stopWorkoutService();
                Log.d(TAG, "####CS Stop workout trigger by physical stop");
            } else if (keyStateIsDown && (keyCode == KeyPress.KeyCode.KEY_PAUSE)) {
                //Stores pause physical workout button press event for Dapi Analytics
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAUSE_WORKOUT_PHYSICAL_BTN_PRESS.name);
                manualPauseWorkoutService();
            }
        });

        // Resets the goal type when we reach the splash screen so that the next workout show goal +- buttons correctly (goal type none does not show them and will disable those buttons)
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_RESET_STATS, value -> {
            DataMatrix.putInt(WorkoutKeys.DATA_GOAL_TYPE_INT, WorkoutKeys.Constants.GOAL_TYPE_TIME_STAGE); // reset this for UI purposes
            DataMatrix.putInt(Sprint8Keys.DATA_SPRINT8_NUM_SPRINTS_INT, 8);
            DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_SCRIPT_NEXT_SPEED_FLOAT, 0);
            DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_SCRIPT_NEXT_INCLINE_FLOAT, 0);
            DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_SCRIPT_NEXT_RESISTANCE_INT, 0);
        });

        // Listen for the XareFit login event
        mDataMatrix.createBoolAdapter(MainAppKeys.EVENT_XARE_FIT_LOGIN_SUCCESSFUL, value -> {
            if (value) {
                LaunchAppUtil.launchGoScreen();
            }
            // Hide the QR Code
            DataMatrix.sendEvent(MainAppKeys.ACTION_XARE_FIT_HIDE_QR_CODE);
        });

        mDataMatrix.createIntAdapter(HALKeys.DATA_GYM_KIT_CALORIES_TOTAL_INT, this::gymKitUpdate);
        mDataMatrix.createIntAdapter(HALKeys.DATA_GYM_KIT_CALORIES_ACTIVE_INT, this::gymKitUpdate);
        mDataMatrix.createIntAdapter(HALKeys.DATA_GYM_KIT_AVE_HEART_RATE_INT, this::gymKitUpdate);
        mDataMatrix.createIntAdapter(HALKeys.ACTION_UPDATE_GYM_KIT_HEART_RATE_INT, this::gymKitUpdate);

        mDataMatrix.createIntAdapter(WorkoutKeys.ACTION_HOT_SWAP_LEVEL_INT, level -> {
            try {
                mController.hotSwapLevel(level);
            } catch (ScriptException e) {
                Log.e(TAG, "script exception found " + e.getMessage());
            }
        });
    }

    private void gymKitUpdate(Integer value) {
        // force an update for the edge case where gym kit sends an update but the workout is paused
        if (mController.isPaused()) {
            mStatUpdate.forceGymKitUpdate();
        }

        // force an update for the edge case where gym kit sends an update but the workout has finished
        if (!mController.isRunning()) {
            mStatUpdate.forceGymKitSummaryUpdate();
            mStatUpdate.sendStatsToDataMatrix();
        }
    }

    private void checkSafetyStatus() {
        if(DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL)) {
            return;
        }
        
        int statusBitmap = DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT);
        if (isSafetyScreenShown(statusBitmap)) {
            ConsoleEvent.logEvent("Safety Key Triggered: " + statusBitmap);
            stopWorkoutService();
            return;
        }

        // pause workout for climbmill if control zone or ir sensor is triggered
        boolean isInWorkout = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP;
        if (isInWorkout) {
            boolean controlZoneTriggered = (statusBitmap & SAFETY_KEY_CLIMBMILL_CZ) > 0;
            boolean irSensorTriggered = (statusBitmap & SAFETY_KEY_CLIMBMILL_IR) > 0;
            if (controlZoneTriggered || irSensorTriggered) {
                String text;
                if (controlZoneTriggered) {
                    text = StringLookup.get(R.string.pause_control_zone_tc);
                } else {
                    text = StringLookup.get(R.string.pause_ir_sensor_tc);
                }

                // pause workout
                DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT);
                MessageCenterMessage message = new MessageCenterMessage("", text, 30);
                DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message);
            }
        }
    }

    /**
     * Gets the external dirs for the system
     * @return The external dirs
     */
    private List<File> getExternalDirs() {
        return null;
    }

    private boolean isLoadAllowed() {
        // Load requests alter the workout definition/entry fields which are used during launch process.
        // For now we block them, an alternative might be to have launch use local copies and pass them as parameters to needed functions.
        if (workoutLaunchHelper.isBusy()) {
            Log.w(TAG, "Workout load request ignored due to launch in progress");
            return false;
        }
        return true;
    }

    /** These DM keys are used by UI in response to a load workout request */
    private void sendLoadResponse(boolean success) {
        // On failure just send the last loaded info, caller can check if ID matches to see if its request was taken
        if (success) {
            // Levels key is used to set min/max on workout input for level in UI
            if (AppIds.VA_UUID.equals(mWorkoutEntry.appId)) {
                DataMatrix.putInt(WorkoutKeys.DATA_NUM_LEVELS_INT, 20);
            } else {
                DataMatrix.putInt(WorkoutKeys.DATA_NUM_LEVELS_INT, mWorkoutDefinition.getNumberOfLevels());
            }
        }
        // Workout inputs key is used to relay the needed inputs up to UI via optional loaded callback
        StringInputList inputList = new StringInputList(mWorkoutEntry.callerId, mWorkoutDefinition.getInputs());
        DataMatrix.putParcelable(WorkoutKeys.EVENT_LOADED_WORKOUT_INPUTS_PARCELABLE, inputList);
    }

    private void loadWorkoutXml(XmlWorkoutEntry xmlEntry) {
        boolean success = false;
        if (isLoadAllowed()) {
            try {
                XmlToScript xmlConverter = new XmlToScript();
                String script = xmlConverter.convertXmlToScript(xmlEntry.xmlString).toString();
                mWorkoutEntry = new WorkoutEntry(0, AppIds.DEFAULT_APP_UUID, "xml_entry"); //NON-NLS
                mWorkoutEntry.callerId = xmlEntry.id;
                mWorkoutDefinition = WorkoutControllerCardio.loadScript(script);
                success = true;
            } catch (ScriptException scriptException) {
                Log.e(TAG, "There was an error parsing the xml workout", scriptException);
            }
        }
        sendLoadResponse(success);
    }

    private void loadWorkoutEntry(WorkoutEntry entry) {
        boolean success = false;
        if (isLoadAllowed()) {
            try {
                mWorkoutEntry = entry;
                mWorkoutDefinition = WorkoutControllerCardio.loadScript(entry.scriptString, getExternalDirs());
                success = true;
            } catch (ScriptException scriptException) {
                Log.e(TAG, "There was an error parsing the workout", scriptException);
            }
        }
        sendLoadResponse(success);
    }

    private void loadSprint8Config(BaseSprint8Config config) {
        boolean success = false;
        if (isLoadAllowed()) {
            // Create entry for custom sprint8
            mWorkoutEntry = new WorkoutEntry(0, AppIds.SPRINT8_UUID, "custom_sprint8_entry"); //NON-NLS
            mWorkoutDefinition = Sprint8Generator.createDefinition(config);
            success = true;
        }
        sendLoadResponse(success); // Sprint8 doesn't really use a callback currently? at the moment caller ID would be zero here per WorkoutEntry default value
    }

    private void getCurrentWorkoutModel() {
        WorkoutModel model = mController.getWorkoutModel(mWorkoutDefinition);
        DataMatrix.putParcelable(WorkoutKeys.EVENT_MODEL_INFORMATION_RESPONSE_PARCELABLE, model);
    }

    private void initialValuesSet(InitialConditionsList conditionsList) {
        mAllowAutoPause = true;
        for (InitialCondition initialCondition : conditionsList.initialConditions) {
            if (initialCondition != null && initialCondition.conditionName != null) {
                if (initialCondition.conditionName.equals(ConsoleWorkoutVariables.LEVEL)) {
                    mLevel = Math.round(initialCondition.conditionValue);
                    mController.updateAndValidateValue(initialCondition.conditionName, initialCondition.conditionValue);
                    addToDataMatrixIfNeeded(ConsoleWorkoutVariables.LEVEL, mLevel);
                } else if (initialCondition.conditionName.equals(ConsoleWorkoutVariables.TIME_GOAL_START)
                        || initialCondition.conditionName.equals(ConsoleWorkoutVariables.INTERVAL_TIME_GOAL)
                        || initialCondition.conditionName.equals(ConsoleWorkoutVariables.REST_TIME_GOAL)) {
                    // These are stored in minutes (see MinMaxDictionary) but compared against INTERVAL_TIME_MS in workout scripts, so convert to millis
                    mController.updateAndValidateValue(initialCondition.conditionName, initialCondition.conditionValue * 60 * 1000);
                    addToDataMatrixIfNeeded(initialCondition.conditionName, initialCondition.conditionValue * 60 * 1000);
                } else if (initialCondition.conditionName.equals(ConsoleWorkoutVariables.ALLOW_AUTO_PAUSE)) {
                    mAllowAutoPause = (initialCondition.conditionValue != 0);
                } else {
                    mController.updateAndValidateValue(initialCondition.conditionName, initialCondition.conditionValue);
                    addToDataMatrixIfNeeded(initialCondition.conditionName, initialCondition.conditionValue);
                }

                // This allows the watts app to be correct during the countdown.  Probably need to do this for more variables.
                if(initialCondition.conditionName.equals(ConsoleWorkoutVariables.TARGET_WATTS_START)) {
                    DataMatrix.putInt(WorkoutKeys.DATA_TARGET_WATTS_INT, (int)initialCondition.conditionValue);
                }
            }
        }
    }

    private void startWorkoutEvent() {
        // we need to NOT allow a workout to start for a climbmill if the control zone or ir is triggered
        boolean isSafeToStart = DataMatrix.getBoolean(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL);
        if (isSafeToStart) {
            WorkoutFuture future = new WorkoutFuture(new ArrayList<>());
            DataMatrix.putParcelable(WorkoutKeys.DATA_WORKOUT_FUTURE_PARCELABLE, future);

            if (mController.isStarted()) {
                DataMatrix.sendEvent(WorkoutKeys.EVENT_WORKOUT_SWITCHED);
                switchWorkoutService();
            } else {
                DataMatrix.sendEvent(WorkoutKeys.EVENT_WORKOUT_START);
                startWorkoutService();
            }
        } else {
            Log.e(TAG, "==> Error: not safe to start!");
                int isSafeToStartShow = DataMatrix.getInt(HALKeys.DATA_TM_IS_SAFE_TO_START_BITMASK_INT);


                //Display a message due to the SafeToStart restriction
                if (!SafeToStartFlags.checkSafeToStartWithNotification(isSafeToStartShow)) {
					Log.d(TAG, "Not safe to start - Special SafeToStartFlags");
                }

            stopWorkoutService(true, true);
        }
    }

    private void resumeWorkoutEvent() {
        manualResumeWorkoutService();
    }

    /**
     * Updates the value of a goal if it is time based
     * @param newGoalSeconds The new time goal in seconds.
     */
    private void changeTimeGoal(int newGoalSeconds) {
        if (!mController.getAllowUserGoalChanges()) return;

        RangeInfo rangeInfo;
        switch (mGoalType) {
            case WorkoutKeys.Constants.GOAL_TYPE_TIME:
            case WorkoutKeys.Constants.GOAL_TYPE_TIME_STAGE:
                String stage = mController.getCurrentStageName();
                switch (stage) {
                    case WorkoutController.COOLDOWN:
                    case WorkoutController.WARMUP:
                        putGoal((float)MathUtils.clamp(newGoalSeconds * 1000, 60 * 1000, DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_COOLDOWN_TIME_INT) * 60 * 1000));
                        break;
                    default:
                        rangeInfo = mFrameConfig.workoutTime;
                        putGoal(MathUtils.clamp(newGoalSeconds * 1000, rangeInfo.getMin() * 60 * 1000, rangeInfo.getMin() * 60 * 1000));
                        break;
                }
                break;
            default:
                Log.e(TAG, "The current goal type is not supported! Cannot change goal!");
                break;
        }

    }

    /**
     * Updates the value of a goal type according to the current frame.
     * @param increment whether to increment (true) or decrement (false)
     */
    private void changeGoal(boolean increment) {
        if (!mController.getAllowUserGoalChanges()) return;

        float currentGoal;
        float max;
        float min;
        float stepSize;
        RangeInfo rangeInfo;
        switch (mGoalType) {
            case WorkoutKeys.Constants.GOAL_TYPE_TIME:
            case WorkoutKeys.Constants.GOAL_TYPE_TIME_STAGE:
                String stage = mController.getCurrentStageName();
                switch (stage) {
                    case WorkoutController.COOLDOWN:
                    case WorkoutController.WARMUP:
                        stepSize = 60 * 1000; // One minute
                        max = DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_COOLDOWN_TIME_INT) * 60 * 1000;
                        min = 60 * 1000;
                        break;
                    default:
                        rangeInfo = mFrameConfig.workoutTime;
                        stepSize = rangeInfo.stepSize * 60 * 1000;
                        max = rangeInfo.getMax() * 60 * 1000;
                        min = rangeInfo.getMin() * 60 * 1000;
                        break;
                }
                break;
            case WorkoutKeys.Constants.GOAL_TYPE_DISTANCE:
            case GOAL_TYPE_DISTANCE_STAGE:
                boolean preferMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
                if (preferMetric) {
                    stepSize = mFrameConfig.workoutDistanceKm.stepSize;
                    max = mFrameConfig.workoutDistanceKm.getMax();
                    min = mFrameConfig.workoutDistanceKm.getMin();
                } else {
                    stepSize = DistanceConvertUtil.convertMilesToKilometers(mFrameConfig.workoutDistanceMi.stepSize);
                    max = DistanceConvertUtil.convertMilesToKilometers(mFrameConfig.workoutDistanceMi.getMax());
                    min = DistanceConvertUtil.convertMilesToKilometers(mFrameConfig.workoutDistanceMi.getMin());
                }
                break;
            case WorkoutKeys.Constants.GOAL_TYPE_CALORIES:
            case WorkoutKeys.Constants.GOAL_TYPE_CALORIES_STAGE:
                rangeInfo = mFrameConfig.workoutCalories;
                stepSize = 10; // This is 10 because the change should be different than the step size
                max = rangeInfo.getMax();
                min = rangeInfo.getMin();
                break;
            case WorkoutKeys.Constants.GOAL_TYPE_FLOORS:
            case WorkoutKeys.Constants.GOAL_TYPE_FLOORS_STAGE:
                rangeInfo = mFrameConfig.workoutFloors;
                stepSize = rangeInfo.stepSize;
                max = rangeInfo.getMax();
                min = rangeInfo.getMin();
                break;
            default:
                Log.e(TAG, "The current goal type is not supported! Cannot change goal!");
                return;
        }

        currentGoal = DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_VALUE_FLOAT);
        float goalRemaining = DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT);
        if (increment || goalRemaining >= stepSize) {
            float updatedValue = currentGoal;
            if (increment) {
                updatedValue += stepSize;
            } else {
                updatedValue -= stepSize;
            }

            if (updatedValue < currentGoal && updatedValue < (min - TOLERANCE)) {
                updatedValue = min;
            } else if (updatedValue > currentGoal && updatedValue > (max + TOLERANCE)) {
                updatedValue = max;
            }

            if (updatedValue != currentGoal) {
                mController.updateAndValidateValue(WorkoutScriptVariables.GOAL, updatedValue);
                putGoal(updatedValue);
            }
        }
    }

    private void updateGoal(float updatedValue) {
        try {
            float currentGoal = DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_VALUE_FLOAT);
            if (updatedValue != currentGoal) {
                mController.updateAndValidateValue(WorkoutScriptVariables.GOAL, updatedValue);
                putGoal(updatedValue);
            }
        } catch (Exception e) {
            // Possible issue with MatrixComm update goal being used before workout is started? Seems to happen after changing frames?
            // Can we prevent this in MatrixComm by blocking it if run state is stopped ???
            // "mController.getStageIntFromName()" can throw
            // java.lang.NullPointerException: Attempt to invoke virtual method 'java.util.List com.jht.workoutcontrollerlib.workoutdefinition.WorkoutDefinition.getStages()' on a null object reference
            Log.w(TAG, "Error updating workout goal to " + updatedValue, e);
        }
    }

    private void putGoal(Float goal) {//
        DataMatrix.putFloat(WorkoutKeys.DATA_GOAL_VALUE_FLOAT, goal);
        if (mController.getStageIntFromName() == WorkoutController.WORKOUT_STAGE_WORKOUT) {
            DataMatrix.putFloat(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_FLOAT, goal);
        }
    }

    private boolean isUserChangeAllowed() {
        boolean isTransition = workoutLaunchHelper.isBusy();
        boolean isRunning = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_RUNNING;
        return !isTransition && mController.getAllowUserChanges() && isRunning;
    }

    private void setSpeedUser(float speed) {
        if (isUserChangeAllowed()) {
            setSpeed(speed);
        }
    }

    private void setInclineUser(float incline) {
        if (isUserChangeAllowed() && mAllowInclineChanges) {
            setIncline(incline);
        }
    }

    private void setResistanceUser(int resistance) {
        // return if disable pedal changes is enabled
        if (mFrameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Treadmill &&
                mFrameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Climbmill) {
            if (DataMatrix.getBoolean(WorkoutKeys.DATA_DISABLE_PEDAL_CHANGES_BOOL)) return;
        }
        if (isUserChangeAllowed()) {
            setResistance(resistance);
        }
    }

    private void setSpeed(float speed) {
        ConsoleEvent.logEvent("Set Speed: " + speed);
        float clampedSpeed;
        if (speed == 0) { // allow for stopping belt via speed zero
            mBeltStop = true;
            clampedSpeed = 0;
        } else {
            mBeltStop = false;
            boolean useMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
            RangeInfo range = useMetric ? mFrameConfig.speedKph : mFrameConfig.speedMph;
            if (useMetric) {
                clampedSpeed = MathUtils.clamp(speed, range.getMin(), range.getMax());
            } else {
                clampedSpeed = MathUtils.clamp(speed,
                        SpeedConvertUtil.convertMphToKph(range.getMin()), SpeedConvertUtil.convertMphToKph(range.getMax()));
            }
        }
        frameControlDebounce.updateSpeedKph(clampedSpeed);
    }

    private void setIncline(float incline) {
        ConsoleEvent.logEvent("Set Incline: " + incline);
        float clampedIncline = MathUtils.clamp(incline, mFrameConfig.incline.getMin(), mFrameConfig.incline.getMax());
        frameControlDebounce.updateIncline(clampedIncline);
    }

    private void setResistance(int resistance) {
        ConsoleEvent.logEvent("Set Resistance: " + resistance);
        int clampedResistance;
        if (resistance == 0) {
            clampedResistance = 0;
            mBeltStop = true; // CM stop
        } else {
            clampedResistance = MathUtils.clamp(resistance, Math.round(mFrameConfig.resistance.getMin()), Math.round(mFrameConfig.resistance.getMax()));
            mBeltStop = false; // CM stop
        }
        DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, clampedResistance);
    }

    private void skipIntervalEvent() {
        if (!mController.isStarted()) return;
        mController.skipInterval();
    }

    private void skipToCooldown() {
        boolean isTransition = workoutLaunchHelper.isBusy();
        boolean canSkipWorkout = DataMatrix.getBoolean(WorkoutKeys.DATA_ALLOW_SKIP_WORKOUT_BOOL);
        if (mController.isStarted() && !isTransition && canSkipWorkout) {
            DataMatrix.putBoolean(WorkoutKeys.DATA_REACHED_FULL_WORKOUT_BOOL, false);
            mController.startCooldown();
        }
    }

    private void skipWarmup() {
        boolean isTransition = workoutLaunchHelper.isBusy();
        boolean canSkipWarmup = DataMatrix.getBoolean(WorkoutKeys.DATA_ALLOW_SKIP_WARMUP_BOOL);
        if (mController.isStarted() && !isTransition && canSkipWarmup) {
            mController.skipWarmup();
        }
    }

    /**
     * Hides the workout panels and goes to the go screen
     */
    private void showMainScreen(boolean workoutCompleted) {
        // don't show summary if safety screen is on, go back to
        Log.d(TAG, "Leaving workout with workoutCompleted = " + workoutCompleted);

        if (workoutCompleted && !isSafetyScreenShown()) {
            String appId = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING);
            AppManager.getInstance().reset(); // in case app info has changed (more for UI flow changes that probably need a restart anyway)
            AppInfo appInfo = AppManager.getInstance().getApp(appId);
            if (appInfo != null) {
                if (appInfo.workoutEndResetSafetyPin) DataMatrix.putBoolean(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL, true);
                switch (appInfo.workoutEndAction) {
                    case NoWorkoutSummary:
                        DataMatrix.putString(DAPIKeys.ACTION_DAPI_ANALYTICS_STRING, DapiUtil.getMachineId()); // kick analytics, normally splash screen does this
                        LaunchAppUtil.attemptLaunchNonWorkoutSummary(appInfo.workoutEndActivity);
                        // if no summary and special app LCD grips, go back to loading grips
                        if (appInfo.uiEventName != null && !appInfo.uiEventName.isEmpty()) DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, appInfo.uiEventName);
                        break;
                    case WorkoutSummary:
                        LaunchAppUtil.launchWorkoutSummary(appInfo.workoutEndActivity);
                        break;
                }
            } else if(mWorkoutEntry.workoutId == WorkoutIds.SECA_PACE){
                LaunchAppUtil.launchWorkoutSummary(SecaActivity.class.getCanonicalName());
            } else {
                DataMatrix.putBoolean(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL, true);
                LaunchAppUtil.launchWorkoutSummary();
            }
        } else {
            Log.e(TAG, "Going to splash app, either workout was not completed or safety screen was shown");
            LaunchAppUtil.launchSplashScreen();
        }
    }

    /**
     * Changes the goal type
     * @param value The new goal type to set to
     */
    private void goalTypeChanged(float value) {
        mGoalType = Math.round(value);
        DataMatrix.putInt(WorkoutKeys.DATA_GOAL_TYPE_INT, mGoalType);
        if (mController.getStageIntFromName() == WorkoutController.WORKOUT_STAGE_WORKOUT) {
            DataMatrix.putInt(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_TYPE_INT, mGoalType);
        }
    }

    private void handleStageChangeEvent() {
        String stageName = mController.getCurrentStageName();

        int currentStage;
        float activeCaloriesOffset;
        float caloriesOffset;
        switch (stageName) {
            case "warmup": //NON-NLS
                currentStage = WorkoutKeys.Constants.WORKOUT_STAGE_WARMUP;
                break;
            case "cooldown": //NON-NLS
                currentStage = WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN;
                activeCaloriesOffset = Math.max(WorkoutDataContainer.getInstance().workoutSummaryWarmup.statsAccumulated.activeCalories,
                        WorkoutDataContainer.getInstance().workoutSummaryWorkout.statsAccumulated.activeCalories);
                if (activeCaloriesOffset > 0) {
                    WorkoutDataContainer.getInstance().workoutSummaryCooldown.statsAccumulated.activeCaloriesOffset = activeCaloriesOffset;
                    caloriesOffset = Math.max(WorkoutDataContainer.getInstance().workoutSummaryWarmup.statsAccumulated.calories,
                            WorkoutDataContainer.getInstance().workoutSummaryWorkout.statsAccumulated.calories);
                    WorkoutDataContainer.getInstance().workoutSummaryCooldown.statsAccumulated.caloriesOffset = caloriesOffset;
                }

                // Logic for the workout graph
                float goalValue;
                if (mGoalType == WorkoutKeys.Constants.GOAL_TYPE_CALORIES || mGoalType == WorkoutKeys.Constants.GOAL_TYPE_CALORIES_STAGE) {
                    goalValue = DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT);
                    DataMatrix.putFloat(WorkoutKeys.DATA_COOL_DOWN_VARIABLE_PROGRESS_FLOAT, goalValue);
                } else if (mGoalType == WorkoutKeys.Constants.GOAL_TYPE_DISTANCE || mGoalType == GOAL_TYPE_DISTANCE_STAGE) {
                    goalValue = DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT);
                    DataMatrix.putFloat(WorkoutKeys.DATA_COOL_DOWN_VARIABLE_PROGRESS_FLOAT, goalValue);
                } else if (mGoalType == WorkoutKeys.Constants.GOAL_TYPE_FLOORS || mGoalType == WorkoutKeys.Constants.GOAL_TYPE_FLOORS_STAGE) {
                    goalValue = DataMatrix.getFloat(WorkoutKeys.DATA_FLOORS_FLOAT);
                    DataMatrix.putFloat(WorkoutKeys.DATA_COOL_DOWN_VARIABLE_PROGRESS_FLOAT, goalValue);
                } else if (mGoalType == WorkoutKeys.Constants.GOAL_TYPE_TIME || mGoalType == WorkoutKeys.Constants.GOAL_TYPE_TIME_STAGE) {
                    goalValue = DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT);
                    DataMatrix.putFloat(WorkoutKeys.DATA_COOL_DOWN_VARIABLE_PROGRESS_FLOAT, goalValue);
                }

                break;
            case "": //NON-NLS
            case "workout": //NON-NLS
            default:
                currentStage = WORKOUT_STAGE_WORKOUT;
                activeCaloriesOffset = WorkoutDataContainer.getInstance().workoutSummaryWarmup.statsAccumulated.activeCalories;
                if (activeCaloriesOffset > 0) {
                    WorkoutDataContainer.getInstance().workoutSummaryWorkout.statsAccumulated.activeCaloriesOffset = activeCaloriesOffset;
                    caloriesOffset = WorkoutDataContainer.getInstance().workoutSummaryWarmup.statsAccumulated.calories;
                    WorkoutDataContainer.getInstance().workoutSummaryWorkout.statsAccumulated.caloriesOffset = caloriesOffset;
                }
                break;
        }

        Log.d("WorkoutControllerService.java", "####CS Publishing currentState " + currentStage);
        DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT, currentStage);
    }

    private void resetValues() {
        mSprint8State = 0;
        mCooperWarningValue = 0;
        resumeWatts = 0;
        mAllowInclineChanges = true;
        mStatUpdate.resetStats();
        DataMatrix.putInt(WorkoutKeys.DATA_SPRINT8_CURRENT_STATE_INT, 0);
        DataMatrix.putFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FLOAT, 0);
        DataMatrix.putFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_MALE_FLOAT, 0);
        DataMatrix.putFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FEMALE_FLOAT, 0);
        DataMatrix.putFloat(WorkoutKeys.DATA_VO2_FLOAT, 0);
        DataMatrix.putInt(WorkoutKeys.DATA_SUBMAX_OUTPUT_INT, 0);
        DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_LEVEL_INT, mLevel);
        DataMatrix.putInt(WorkoutKeys.DATA_TARGET_WATTS_INT, 0);

        // Workout settings reset
        DataMatrix.putBoolean(WorkoutKeys.DATA_DISABLE_PEDAL_CHANGES_BOOL, false);

        // Reset the gymkit keys to -1 so they will not be used until valid values are received by the watch
        DataMatrix.updateInt(HALKeys.DATA_GYM_KIT_CALORIES_TOTAL_INT, -1);
        DataMatrix.updateInt(HALKeys.DATA_GYM_KIT_CALORIES_ACTIVE_INT, -1);
        DataMatrix.updateInt(HALKeys.DATA_GYM_KIT_AVE_HEART_RATE_INT, -1);
    }


    /**
     * Resets some keys that should be defaulted when workouts are switched
     */
    private void resetValuesForSwitchWorkout() {
        mCooperWarningValue = 0;
        mAllowInclineChanges = true;
        resumeWatts = 0;
        DataMatrix.putInt(WorkoutKeys.DATA_TARGET_WATTS_INT, 0);
        mStatUpdate.resetForSwitchWorkout();
    }

    /**
     * Begins the workout and starts the workout timer
     */
    private void startWorkoutService() {
        synchronized (workoutTransitionLock) {
            if (mWorkoutDefinition == null) {
                Log.e(TAG, "Workout definition is not set!");
                return;
            }
            ConsoleEvent.logEvent("Starting workout: id " + mWorkoutDefinition.getWorkoutId());
            mIsStopped = false;
            canShowWorkoutComplete = true;
            try {
                resetValues();
                setDefaultInitialConditions();
                DataMatrix.sendEvent(HALKeys.ACTION_START_MACHINE);
                DataMatrix.putInt(WorkoutKeys.DATA_RUN_STATE_INT, WorkoutKeys.Constants.RUN_STATE_RUNNING);
                mController.startWorkout(mWorkoutDefinition, mLevel);
                startTreadSenseIfNeeded(); // Starts tread sense if enabled
            } catch (ScriptException e) {
                Log.e(TAG, "Error with script caught, not starting workout!", e);
                stopWorkoutService(true, true);
            }
            mRpm0Counter = 0;
        }
    }

    /**
     * Updates the workout that is currently being run to run a different script
     */
    private void switchWorkoutService() {
        synchronized (workoutTransitionLock) {
            ConsoleEvent.logEvent("Switching workout: id " + mWorkoutDefinition.getWorkoutId());
            setDefaultInitialConditions();
            try {
                updateSettingsIfNeeded();
                resetValuesForSwitchWorkout();
                mController.switchWorkout(mWorkoutDefinition, mLevel);
                DataMatrix.sendEvent(HALKeys.ACTION_START_MACHINE);
                startTreadSenseIfNeeded(); // Starts tread sense if enabled
                DataMatrix.putInt(WorkoutKeys.DATA_RUN_STATE_INT, WorkoutKeys.Constants.RUN_STATE_RUNNING);
            } catch (ScriptException e) {
                Log.e(TAG, "Error with script caught, error switching workout!", e);
                stopWorkoutService(true, true);
            }
        }
    }

    private void updateSettings(WorkoutSettingOverrides settingOverrides) {
        int workoutId = 0;
        boolean allowSkipWorkout = true;
        boolean allowSkipWarmup = true;
        boolean allowUserGoalChanges = true;

        if (mWorkoutDefinition != null) {
            workoutId = mWorkoutDefinition.getWorkoutId();
            allowSkipWorkout = mWorkoutDefinition.getAllowSkipWorkout();
            allowSkipWarmup = mWorkoutDefinition.getAllowSkipWarmup();
            for (SettingValue setting : mWorkoutDefinition.getWorkoutSettings()) {
                if (CAN_MODIFY_GOAL_SETTING.equals(setting.setting)) {
                    allowUserGoalChanges = Math.round(setting.value) > 0;
                }
            }
            for (SettingValue setting : settingOverrides.settings) {
                switch (setting.setting) {
                    case CAN_MODIFY_GOAL_SETTING:
                        allowUserGoalChanges = Math.round(setting.value) > 0;
                        break;
                    case WorkoutSettings.WORKOUT_ID:
                        workoutId = Math.round(setting.value);
                        break;
                    case WorkoutSettings.ALLOW_SKIP_WORKOUT:
                        allowSkipWorkout = Math.round(setting.value) > 0;
                        break;
                    case WorkoutSettings.ALLOW_SKIP_WARMUP:
                        allowSkipWarmup = Math.round(setting.value) > 0;
                        break;

                }
            }
        }

        DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_ID_INT, workoutId);
        DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, mWorkoutEntry.appId);
        DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_SKIP_WORKOUT_BOOL, allowSkipWorkout);
        DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_SKIP_WARMUP_BOOL, allowSkipWarmup);
        DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_USER_GOAL_CHANGES_BOOL, allowUserGoalChanges);
    }

    private void updateSettingsIfNeeded() {
        if (mWorkoutDefinition != null) {
            int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
            if (mWorkoutDefinition.getWorkoutId() != workoutId) {
                DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_ID_INT, mWorkoutDefinition.getWorkoutId());
            }

            String appId = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING);
            if (!appId.equals(mWorkoutEntry.appId)) {
                DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, mWorkoutEntry.appId);
            }

            // These can be updated without issue
            DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_SKIP_WORKOUT_BOOL, mWorkoutDefinition.getAllowSkipWorkout());
            DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_SKIP_WARMUP_BOOL, mWorkoutDefinition.getAllowSkipWarmup());
        }
    }

    private void setDefaultInitialConditions() {
        List<String> inputs = mWorkoutDefinition.getInputs();
        for (String input : inputs) {
            if (mController.getVariableValue(input) == null) {
                Float defaultValue = getDefaultValue(input);
                if (defaultValue != null) {
                    mController.updateAndValidateValue(input, defaultValue);
                    addToDataMatrixIfNeeded(input, defaultValue);
                } else {
                    Log.e(TAG, "No default value available for missing input " + input + ". The input MUST be provided!!");
                }
            }
        }

        // Set the constant values
        for (DataModel constant : ConsoleWorkoutVariables.getConstantValues()) {
            mController.updateAndValidateValue(constant.getParameter(), constant.getValue());
        }
    }

    private Float getDefaultValue(String input) {
        switch (input) {
            case ConsoleWorkoutVariables.RPM:
                return (float) DataMatrix.getInt(HALKeys.DATA_RPM_INT); // use last known RPM in case it does not update later
            case ConsoleWorkoutVariables.START_SPEED:
                return DataMatrix.getFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT);
            case ConsoleWorkoutVariables.START_RESISTANCE:
                return (float) DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_LEVEL_INT);
            case ConsoleWorkoutVariables.COOLDOWN_TIME_START:
                String appId = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING);
                if (appId.equals(AppIds.IFIT_UUID)) {
                    return (float)10 * 60 * 1000;
                } else if (AppIds.YMCA_360_FEATURED_UUID.equals(appId)) {
                    return 0f;
                }
                return (float) DataMatrix.getInt(SettingKeys.Workout.DATA_COOLDOWN_TIME_INT) * 60 * 1000;
            case ConsoleWorkoutVariables.WARMUP_TIME_START:
                return (float) 2 * 60 * 1000; // 2 minutes is the standard warmup time
            case ConsoleWorkoutVariables.GOAL:
            case ConsoleWorkoutVariables.DISTANCE:
            case ConsoleWorkoutVariables.CALORIES:
            case ConsoleWorkoutVariables.WATTS:
            case ConsoleWorkoutVariables.TARGET_WATTS:
            case ConsoleWorkoutVariables.ELEVATION:
            case ConsoleWorkoutVariables.FLOORS:
            case ConsoleWorkoutVariables.HEART_RATE:
            case ConsoleWorkoutVariables.METS:
            case ConsoleWorkoutVariables.SPEED_IN:
            case ConsoleWorkoutVariables.RESISTANCE_IN:
            case ConsoleWorkoutVariables.START_INCLINE:
            case ConsoleWorkoutVariables.INCLINE_IN:
            case ConsoleWorkoutVariables.TARGET_AVG_SPEED:
            case ConsoleWorkoutVariables.NUM_INTERVALS:
            case ConsoleWorkoutVariables.INTERVAL_DISTANCE_GOAL:
            case ConsoleWorkoutVariables.INTERVAL_TIME_GOAL:
            case ConsoleWorkoutVariables.REST_TIME_GOAL:
                return 0f;
            case ConsoleWorkoutVariables.RAMP_RATE_SPEED:
                return mFrameConfig.RATE_KPH_PER_SEC;
            case ConsoleWorkoutVariables.RAMP_RATE_INCLINE:
                return mFrameConfig.RATE_INCLINE_PERCENT_PER_SEC;
            case ConsoleWorkoutVariables.RAMP_RATE_RESISTANCE:
                return mFrameConfig.RATE_RESISTANCE_PER_SEC;
            case ConsoleWorkoutVariables.GENDER:
                return (float) ConsoleWorkoutVariables.GENDER_MALE_CONSTANT;
            case "FTP_WATTS": //NON-NLS
                return (float) DataMatrix.getInt(DAPIKeys.DATA_USER_FTP_INT);
            case ConsoleWorkoutVariables.FRAME_ELEVATION:
                boolean above5000Feet = DataMatrix.getBoolean(SettingKeys.Workout.DATA_ELEVATION_ABOVE_5000_FT_BOOL);
                return (float) (above5000Feet ? ConsoleWorkoutVariables.FRAME_ELEVATION_VAL_HIGH : ConsoleWorkoutVariables.FRAME_ELEVATION_VAL_LOW);
            case "CONTROL_MODE": //NON-NLS
                // Control mode for constant watts.
                return 0f;
            case "LCB_CONTROLS_WATTS": //NON-NLS
                // Control mode for constant watts.
                return mFrameConfig.LCB_CAN_CONTROL_WATTS ? 1f : 0f;
            default:
                return null;
        }
    }

    private void addToDataMatrixIfNeeded(String name, float value) {
        switch (name) {
            case ConsoleWorkoutVariables.TARGET_HEART_RATE:
                DataMatrix.putInt(WorkoutKeys.DATA_TARGET_HEART_RATE_INT, Math.round(value));
                break;
            case ConsoleWorkoutVariables.PERCENT_MAX_HEART_RATE:
                DataMatrix.putFloat(WorkoutKeys.DATA_TARGET_HEART_RATE_PERCENT_FLOAT, value);
                break;
            case ConsoleWorkoutVariables.WEIGHT:
                DataMatrix.putFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT, value);
                break;
            case ConsoleWorkoutVariables.LEVEL:
                DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_LEVEL_INT, Math.round(value));
                break;
            case ConsoleWorkoutVariables.AGE:
                DataMatrix.putInt(DAPIKeys.DATA_USER_AGE_INT, Math.round(value));
                break;
            case "FTP_WATTS": //NON-NLS
                DataMatrix.putFloat(WorkoutKeys.DATA_TARGET_FTP_FLOAT, value);
                break;
            case ConsoleWorkoutVariables.GENDER:
                DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_GENDER_INT, Math.round(value));
                break;
            case ConsoleWorkoutVariables.NUM_INTERVALS:
                DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_NUMBER_OF_INTERVALS_INT, Math.round(value));
                break;
            case ConsoleWorkoutVariables.INTERVAL_DISTANCE_GOAL:
                DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_INTERVAL_DISTANCE_GOAL_KM_FLOAT, value);
                break;
            case ConsoleWorkoutVariables.INTERVAL_TIME_GOAL:
                DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_INTERVAL_TIME_GOAL_FLOAT, value);
                break;
            case ConsoleWorkoutVariables.REST_TIME_GOAL:
                DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_REST_TIME_GOAL_FLOAT, value);
                break;
            default:
                // do nothing
                break;
        }
    }

    /** Stops the workout as part of a normal user flow */
    private void stopWorkoutService() { stopWorkoutService(false, false); }
    /** Stops the workout, if [abort] is true then treats it as abnormal flow, if [forceUiOnAbort] is true then abort forces UI to known state */
    private void stopWorkoutService(boolean abort, boolean forceUiOnAbort) {
        // if we have to abort first, we need to make sure things are reset afterwards as launch may have been in the middle of something
        if (!workoutLaunchHelper.abortLaunch(()->{
            Log.d(TAG, "Aborting launch");
            mHandler.post(()-> _haltWorkoutAndReset(true, true)); // post to ensure it's after any launch UI that might have fired
            return Unit.INSTANCE;
        })) {
            _haltWorkoutAndReset(abort, forceUiOnAbort);
        }
    }
    /** [stopWorkoutService] internal method to execute the actual stop; [abort] indicates an abnormal stop, [forceUiOnAbort] indicates abort should force UI to known state */
    private void _haltWorkoutAndReset(boolean abort, boolean forceUiOnAbort) { // ONLY CALL THIS FROM stopWorkoutService() which includes launch/switch checking (could use lambda variable or anon class to hide inside function?)
        synchronized (workoutTransitionLock) {
            Log.d(TAG, "####CS Stop workout trigger abort=" + abort);
            if (!mIsStopped) {
                canShowWorkoutComplete = !abort;
                if (mWorkoutDefinition != null) {
                    ConsoleEvent.logEvent("Stopping workout: id " + mWorkoutDefinition.getWorkoutId());
                } else {
                    ConsoleEvent.logEvent("Stopping workout: null workout definition");
                }

                mIsStopped = true;
                if (!abort) sendWorkoutEndAnalytics(); // assuming we do not want to send analytics if we had to abort

                mLevel = 0;
                accumulateStats();
                stopTreadSenseTimer();
                mStatUpdate.sendStatsToDataMatrix();

                // this needs to happen after sendStatsToDataMatrix until the logic that depends on workout summary data is refactored
                DataMatrix.putInt(WorkoutKeys.DATA_RUN_STATE_INT, WorkoutKeys.Constants.RUN_STATE_STOP);

                DataMatrix.sendEvent(HALKeys.ACTION_STOP_MACHINE);
                Log.d(TAG, "####CS Stop machine trigger by stop workout");
                mController.stopWorkout(); // triggers UI flow via the controller state change callback in workoutControllerOutput
                resetKeysAtWorkoutEnd();
                mRpm0Counter = 0;
                mSpm0Counter = 0;
                mBeltStop = false; // is it best to do this at stop in case we are starting with belt stop?
            }
        }
        if (abort && forceUiOnAbort) showMainScreen(false); // controller stop event handles the main screen for normal completion
    }

    /**
     * Sends analytics when the workout is completed
     */
    private void sendWorkoutEndAnalytics() {
        Log.d(TAG, "sending end of workout analytics data");
        WorkoutEvent workoutEvent = new WorkoutEvent();
        workoutEvent.setProgramId(DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT));
        workoutEvent.setSubProgramId(0); // This is not used
        workoutEvent.setLevel(mLevel + 1); // This is a 1 indexed value on the DAPI side so we need to add one
        workoutEvent.setTotalDistanceKm(DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT));
        workoutEvent.setTotalDistanceSteps(DataMatrix.getInt(WorkoutKeys.DATA_STEPS_INT));
        int timeSeconds = DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000;
        workoutEvent.setTotalTimeSeconds(timeSeconds);
        DataMatrix.putParcelable(DAPIKeys.EVENT_DAPI_WORKOUT_COMPLETE_ANALYTICS_PARCELABLE, workoutEvent);
    }

    private void resetKeysAtWorkoutEnd() {
        frameControlDebounce.cancel();
        DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, 0f);
        DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, 0);
        DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, 0f);
        DataMatrix.putFloat(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_FLOAT, 0f);//
        DataMatrix.putFloat(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_REMAINING_FLOAT, 0f);//
        DataMatrix.putInt(WorkoutKeys.DATA_SPRINT8_CURRENT_STATE_INT, 0);
        DataMatrix.putBoolean(WorkoutKeys.DATA_WORKOUT_SCRIPT_IS_SPRINTING_BOOL, false);
        DataMatrix.putBoolean(WorkoutKeys.DATA_WORKOUT_SCRIPT_IS_RAMPING_BOOL, false);
        DataMatrix.putBoolean(WorkoutKeys.DATA_REACHED_FULL_WORKOUT_BOOL, false);
        DataMatrix.putInt(WorkoutKeys.DATA_TARGET_WATTS_INT, 0);
        DataMatrix.putInt(WorkoutKeys.DATA_LCB_CONTROL_MODE_INT, 0);
        mCooperWarningValue = 0;
        // Added delay to avoid race condition where data is reset before sending apple watch data
        mHandler.postDelayed(() -> {
            // should this be in ACTION_RESET_STATS listener instead?
            DataMatrix.putInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, 0);
        }, 1500);
        DataMatrix.putInt(WorkoutKeys.DATA_CS_THR_ZONE_INT, 0);
        DataMatrix.putParcelable(WorkoutKeys.DATA_CS_THR_ZONES_PARCELABLE, null);
        DataMatrix.putInt(WorkoutKeys.DATA_INTERVAL_NUM_INT, 0);
        DataMatrix.putFloat(WorkoutKeys.DATA_INTERVAL_GOAL_REMAINING_FLOAT, 0);
        DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_USER_PAUSE_BOOL, true);
        DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_USER_CHANGES_BOOL, true);
        DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_COUNTDOWN_BOOL, true);

        // Make sure the belt direction is not in reverse for the hybrid treadmill to make sure the any
        // future workout is in the normal direction (unsure the best place to put this logic)
        DataMatrix.updateBoolean(HALKeys.DATA_TM_IS_REVERSE_BELT_BOOL, false);
    }

    /**
     * Pauses workout execution
     */
    private void manualPauseWorkoutService() {
        if (pauseWorkoutService()) {
            mIsManualPause = true;
        }
    }

    private void autoPauseWorkoutService() {
        if (pauseWorkoutService()) {
            mIsManualPause = false;
        }
    }

    private boolean pauseWorkoutService() {
        synchronized (workoutTransitionLock) {
            if (!mController.isRunning()) return false;
            ConsoleEvent.logEvent("Pausing workout: id " + mWorkoutDefinition.getWorkoutId());
            if (mController.getAllowPause()) {
                mController.pauseWorkout();
                stopTreadSenseTimer();
                DataMatrix.putInt(WorkoutKeys.DATA_RUN_STATE_INT, WorkoutKeys.Constants.RUN_STATE_PAUSE);
                if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill ||
                        mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Stepper) {
                    int minResistance = mBeltStop ? 0 : Math.round(mFrameConfig.resistance.getMin());
                    DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, minResistance);
                }
                frameControlDebounce.cancel();
                DataMatrix.sendEvent(HALKeys.ACTION_STOP_MACHINE);
                DataMatrix.sendEvent(HALKeys.ACTION_STOP_INCLINE);
                DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, 0f);
                if (mFrameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Climbmill &&
                        mFrameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Treadmill &&
                        mFrameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Stepper) {
                    resumeResistance = DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT);
                    resumeWatts = DataMatrix.getInt(WorkoutKeys.DATA_TARGET_WATTS_INT);
                    DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, 1);
                }
                mSpm0Counter = 0;
            } else {
                stopWorkoutService();
                Log.d(TAG, "####CS Stop workout trigger by pauseWorkoutService");
            }
            return true;
        }
    }

    private void manualResumeWorkoutService() {
        resumeWorkoutService();
    }

    private void autoResumeWorkoutService() {
        if (mIsManualPause) return;
        resumeWorkoutService();
    }

    private void resumeWorkoutService() {
        synchronized (workoutTransitionLock) {
            if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
                if (DataMatrix.getBoolean(HALKeys.DATA_CLIMB_START_CONTROL_STATUS_BOOL)) {
                    return;
                }
            }

            if (!mController.isPaused()) return;
            if (workoutLaunchHelper.isBusy()) { // I guess controller does not internally use switching state, so we may get past the isPaused check?
                Log.d(TAG, "Cannot resume: launch in progress");
                return;
            }
            if ((!mController.isRunning()) && (!DataMatrix.getBoolean(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL)))
            {
                int isSafeToStart = DataMatrix.getInt(HALKeys.DATA_TM_IS_SAFE_TO_START_BITMASK_INT);
                if (!SafeToStartFlags.checkSafeToStartWithNotification(isSafeToStart)) {
                    Log.d(TAG, "Not safe to start - Special SafeToStartFlags");
                }
                Log.d(TAG, "Cannot resume: not safe to start");
                return;
            }
            ConsoleEvent.logEvent("Resuming workout: id " + mWorkoutDefinition.getWorkoutId());
            // float minSpeed = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? mFrameConfig.speedKph.min : SpeedConvertUtil.convertMphToKph(mFrameConfig.speedMph.min);
            DataMatrix.sendEvent(HALKeys.ACTION_START_MACHINE);
            // if the belt was stopped, keep it stopped; otherwise use the defined starting speed
            float startingSpeed = (mBeltStop ? 0 : DataMatrix.getFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT));
            DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, startingSpeed);
            DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT));

            if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill ||
                    mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Stepper) {
                int minResistance = (mBeltStop ? 0 : Math.round(mFrameConfig.resistance.getMin()));
                DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, minResistance);
            } else if (mFrameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.SpinBike) || mFrameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Bike)) {
                if (resumeWatts != 0) {
                    DataMatrix.putInt(WorkoutKeys.DATA_TARGET_WATTS_INT, resumeWatts);
                } else {
                    DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, resumeResistance);
                }
            } else if (mFrameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Treadmill) {
                DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, resumeResistance);
            }
            DataMatrix.putInt(WorkoutKeys.DATA_RUN_STATE_INT, WorkoutKeys.Constants.RUN_STATE_RUNNING);

            startTreadSenseIfNeeded();
            mController.resumeWorkout();
            mRpm0Counter = 0;
            mSpm0Counter = 0;
            mLastElapsedTimeSec = 0;
        }
    }

    private void updateFutureProfile(WorkoutFuture future) {
        if (WorkoutFuture.isValid(future)) {
            DataMatrix.putParcelable(WorkoutKeys.DATA_WORKOUT_FUTURE_PARCELABLE, future);
        }
    }

    private WorkoutFuture convertProfileList(List<WorkoutProfile> profiles) {
        if (profiles == null) return null;
        List<WorkoutFutureProfile> parcelList = new ArrayList<>();

        for (WorkoutProfile profile : profiles) {
            String parameter = profile.parameter;
            List<ProfileValue> valuesParcelList = new ArrayList<>();
            for (com.jht.workoutcontrollerlib.future.ProfileValue value : profile.dataList) {
                valuesParcelList.add(new ProfileValue(value.goalValue, value.value));
            }
            WorkoutFutureProfile parcel = new WorkoutFutureProfile(parameter, valuesParcelList);
            parcelList.add(parcel);
        }

        return new WorkoutFuture(parcelList);
    }

    public void workoutControllerOutput(String parameter, float value) {
        switch (parameter) {
            case WorkoutController.WORKOUT_TIME_MS:
                DataMatrix.putInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, (int) value);

                if (mController.getStageIntFromName() != WorkoutController.WORKOUT_STAGE_COOLDOWN) {
                    DataMatrix.putInt(WorkoutKeys.DATA_PRE_COOL_DOWN_TIME_ELAPSED_MS_INT, (int) value);
                }

                mStatUpdate.timeUpdated((int) value);
                break;
            case ConsoleWorkoutVariables.SPEED:
                if (mBeltStop) {
                    // if belt stop, block any automatic speed setting (we could always add a script keyword to clear belt stop? though user should really be required to interact to clear?)
                    Log.d(TAG, "script set SPEED blocked by belt stop");
                } else {
                    setSpeed(value);
                }
                break;
            case ConsoleWorkoutVariables.INCLINE:
                setIncline(value);
                break;
            case ConsoleWorkoutVariables.RESISTANCE:
                setResistance(Math.round(value));
                break;
            case WorkoutController.WORKOUT_STATE:
                // if workoutLaunchHelper.isBusy() then we are either launching/switching a workout, so we are not stopping as part of normal workout (any need for dedicated block flag?)
                if (value == WorkoutController.WORKOUT_STATE_STOPPED && !workoutLaunchHelper.isBusy() && canShowWorkoutComplete) {
                    Log.i(TAG, "Workout Stopped");
                    stopWorkoutService();
                    Log.d(TAG, "####CS Stop workout trigger by workoutControllerOutput");
                    showMainScreen(true);
                }
                break;
            case WorkoutController.STAGE:
                handleStageChangeEvent();
                break;
            case ConsoleWorkoutVariables.GOAL_TYPE:
                goalTypeChanged(value);
                break;
            case ConsoleWorkoutVariables.GOAL:
                putGoal(value);
                break;
            case ConsoleWorkoutVariables.INTERVAL_GOAL:
                Log.e(TAG, "Updating interval goal");
                DataMatrix.putFloat(WorkoutKeys.DATA_INTERVAL_GOAL_VALUE_FLOAT, value);
                break;
            case ConsoleWorkoutVariables.IS_RAMPING:
                DataMatrix.putBoolean(WorkoutKeys.DATA_WORKOUT_SCRIPT_IS_RAMPING_BOOL, value > 0);
                break;
            case ConsoleWorkoutVariables.NEXT_SPEED: {
                if (value >= 0f) {
                    DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_SCRIPT_NEXT_SPEED_FLOAT, value);
                }
                break;
            }
            case ConsoleWorkoutVariables.NEXT_INCLINE: {
                if (value >= 0f) {
                    DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_SCRIPT_NEXT_INCLINE_FLOAT, value);
                }
                break;
            }
            case ConsoleWorkoutVariables.NEXT_RESISTANCE: {
                if (value >= 0f) {
                    DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_SCRIPT_NEXT_RESISTANCE_INT, Math.round(value));
                }
                break;
            }
            case ConsoleWorkoutVariables.IS_SPRINTING:
                boolean isSprinting = value > 0;
                DataMatrix.putBoolean(WorkoutKeys.DATA_WORKOUT_SCRIPT_IS_SPRINTING_BOOL, isSprinting);
                mStatUpdate.startStopSweatScore(isSprinting);

                // Handles Sprint8 specific behavior
                String appId = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING);
                if (appId.equals(AppIds.SPRINT8_UUID)) {
                    int stage = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT);
                    if (stage == WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN) {
                        int numSprints = DataMatrix.getInt(Sprint8Keys.DATA_SPRINT8_NUM_SPRINTS_INT);
                        mSprint8State = numSprints * 2;
                    } else if (stage == WorkoutKeys.Constants.WORKOUT_STAGE_WARMUP) {
                        mSprint8State = 0;
                    } else {
                        mSprint8State++;
                    }
                    DataMatrix.putInt(WorkoutKeys.DATA_SPRINT8_CURRENT_STATE_INT, mSprint8State);
                }
                break;
            case ConsoleWorkoutVariables.TEST_SCORE:
                DataMatrix.putFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FLOAT, value);
                break;
            case ConsoleWorkoutVariables.TEST_SCORE_MALE:
                DataMatrix.putFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_MALE_FLOAT, value);
                break;
            case ConsoleWorkoutVariables.TEST_SCORE_FEMALE:
                DataMatrix.putFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FEMALE_FLOAT, value);
                break;
            case ConsoleWorkoutVariables.VO2:
                DataMatrix.putFloat(WorkoutKeys.DATA_VO2_FLOAT, value);
                break;
            case ConsoleWorkoutVariables.COOPER_RPM_WARNING:
                sendCooperRpmWarning(Math.round(value));
                break;
            case ConsoleWorkoutVariables.HEART_RATE_WARNING:
                sendHeartRateWarning(value > 0);
                break;
            case ConsoleWorkoutVariables.SUBMAX_OUTPUT:
                DataMatrix.putInt(WorkoutKeys.DATA_SUBMAX_OUTPUT_INT, Math.round(value));
                break;
            case ConsoleWorkoutVariables.PREVENT_INCLINE_CHANGES:
                mAllowInclineChanges = value <= 0;
                break;
            case ConsoleWorkoutVariables.MAX_HEART_RATE:
                DataMatrix.putFloat(WorkoutKeys.DATA_MAX_HEART_RATE_FLOAT, value);
                break;
            case "MAX_STAGE_HEART_RATE": //NON-NLS
                DataMatrix.putFloat(WorkoutKeys.DATA_SUBMAX_MAX_HEART_RATE_FLOAT, value);
                break;
            case "MIN_STAGE_HEART_RATE": //NON-NLS
                DataMatrix.putFloat(WorkoutKeys.DATA_SUBMAX_MIN_HEART_RATE_FLOAT, value);
                break;
            case ConsoleWorkoutVariables.TARGET_WATTS:
                DataMatrix.putInt(WorkoutKeys.DATA_TARGET_WATTS_INT, Math.round(value));
                break;
            case ConsoleWorkoutVariables.LEVEL:
                // Update the workout level (1-based index for ui) when it changes in sprint 8 (hot swap in assessment)
                int newLevel = Math.round(value);
                mLevel = newLevel;
                DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_LEVEL_INT, newLevel+1);
                break;
            case WorkoutController.INTERVAL: // This is the interval number
                DataMatrix.putInt(WorkoutKeys.DATA_STAGE_INTERVAL_NUM_INT, Math.round(value));
                break;
            case ConsoleWorkoutVariables.INTERVAL_NUMBER:
                DataMatrix.putInt(WorkoutKeys.DATA_INTERVAL_NUM_INT, Math.round(value));
                break;
            case ConsoleWorkoutVariables.CURRENT_AVG_TARGET_SPEED:
                DataMatrix.putFloat(WorkoutKeys.DATA_CURRENT_AVG_TARGET_SPEED_KPH_FLOAT, value);
                break;
            case "TARGET_METS": //NON-NLS
                DataMatrix.putFloat(HALKeys.ACTION_SET_TARGET_METS_FLOAT, value);
                break;
            case "FTP_WATTS": //NON-NLS
                DataMatrix.putFloat(WorkoutKeys.DATA_TARGET_FTP_FLOAT, value);
                break;
            case "CONTROL_MODE": //NON-NLS
                DataMatrix.putInt(WorkoutKeys.DATA_LCB_CONTROL_MODE_INT, (int)value);
                break;
            case "HEART_RATE_ZONE":
                int currentTHRZone = DataMatrix.getInt(WorkoutKeys.DATA_CS_THR_ZONE_INT);
                int nextTHRZone = (int) value;
                if (nextTHRZone != currentTHRZone) {
                    DataMatrix.putInt(WorkoutKeys.DATA_CS_THR_ZONE_INT, nextTHRZone);
                }
                break;
        }
    }

    private void sendCooperRpmWarning(int messageValue) {
        if (mCooperWarningValue != messageValue) {
            mCooperWarningValue = messageValue;
            String messageText = messageValue == ConsoleWorkoutVariables.COOPER_WARNING_TOO_SLOW ? getString(R.string.rpm_too_low_pg) : getString(R.string.rpm_too_high_pg);
            MessageCenterMessage message = new MessageCenterMessage()
                    .setMessageHeader("")
                    .setMessageText(messageText)
                    .setPriority(10)
                    .setHideTime(30)
                    .setId(JHTTypes.MessageIds.RPM_Too_High.getId())
                    .setBackgroundColor(0xFFFE0000)
                    .setHeaderColor(0xFFA01E20)
                    .setTextColor(Color.WHITE)
                    .setHidden(messageValue == ConsoleWorkoutVariables.COOPER_WARNING_NONE);

            DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message);
        }
    }

    private void sendHeartRateWarning(boolean show) {
        String messageText = getString(R.string.no_hr_detected_pg).toUpperCase();
        MessageCenterMessage message = new MessageCenterMessage("", messageText, 10, 30)
                .setId(JHTTypes.MessageIds.No_HR_Detected.getId())
                .setBackgroundColor(0xFFFE0000)
                .setHeaderColor(0xFFA01E20)
                .setTextColor(Color.WHITE)
                .setHidden(!show);
        DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message);
    }

    boolean isSafetyScreenShown() { return isSafetyScreenShown(DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT)); }
    boolean isSafetyScreenShown(int safeKeyBitmap) {
        boolean simulatedSafetyKey = DataMatrix.getBoolean(HALKeys.DATA_SIMULATED_ESTOP_BOOL);
        boolean demoMode = DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL);
        if (demoMode) return simulatedSafetyKey; // If we are in demo mode, do not trigger normal safety errors
        else return ChimeraEnums.isAnySafetyErrorTriggered(safeKeyBitmap) || simulatedSafetyKey;
    }

    private void accumulateStats() {
        Log.d(TAG, "==> accumulateStats");

        int accumulatedTimeSec = DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT);
        float accumulatedDistanceKm = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT);
        float accumulatedFloors = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT);
        if (accumulatedTimeSec < 0 || accumulatedDistanceKm < 0 || accumulatedFloors < 0) {
            Log.e(TAG, "==> Should never be here! accumulatedTimeSec = " + accumulatedTimeSec
                    + ", accumulatedDistanceKm = " + accumulatedDistanceKm
                    + ", accumulatedFloors = " + accumulatedFloors);
            return;
        }

        WorkoutSummary workoutSummaryTotal = WorkoutDataContainer.getInstance().workoutSummaryTotal;
        accumulatedTimeSec += workoutSummaryTotal.statsAccumulated.secondsStateRun;
        if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill || mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Stepper) {
            accumulatedFloors += workoutSummaryTotal.statsAccumulated.floors;
            DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT, accumulatedFloors);
        } else {
            accumulatedDistanceKm += workoutSummaryTotal.statsAccumulated.distanceKm;
            DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT, accumulatedDistanceKm);
        }
        DataMatrix.putInt(SettingKeys.General.DATA_ACCUMULATED_WORKOUTS_INT, DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_WORKOUTS_INT) + 1);
        DataMatrix.putInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT, accumulatedTimeSec);
    }

    private void initTreadSense() {
        mDataMatrix.createBoolAdapter(SettingKeys.Workout.DATA_TREAD_SENSE_ENABLED_BOOL, enabled -> updateTreadSenseEnabled(), false);
        updateTreadSenseEnabled();

        /*
        This is not something we have all agreed on yet.  It is in the flow chart for some reason.
        No one knows where the request came from.
        mDataMatrix.createIntAdapter(HALKeys.DATA_HEART_RATE_INT, value -> {
            if (value > 40) {
				resetTreadSenseTimer();
	    	}
        });
         */
        mDataMatrix.createEventAdapter(HALKeys.EVENT_SCREEN_TOUCH, key -> resetTreadSenseTimer());
        mDataMatrix.createBoolAdapter(HALKeys.DATA_PERSON_IN_PLACE_BOOL, this::processPersonInPlace);
    }

    private void processPersonInPlace(boolean hasPerson) {
        if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
            if (mTreadSenseEnabled && mController.isStarted()) {
                if (hasPerson && mTreadSenseStarted) {
                    stopTreadSenseTimer();
                } else if (!hasPerson && !mTreadSenseStarted && mController.isRunning()) {
                    startTreadSenseTimer();
                }
            }
        } else if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
            if (!hasPerson && DataMatrix.getBoolean(SettingKeys.Workout.DATA_CM_DETECT_PERSON_BOOL)) {
                if (mController.isRunning()) {
                    // pause workout
                    DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT);
                    MessageCenterMessage message = new MessageCenterMessage("", StringLookup.get(R.string.workout_paused_no_person_tc), 30);
                    DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message);
                    mSpm0Counter = 0;
                }
            }
        }
    }

    private void updateTreadSenseEnabled() {
        boolean treadSenseEnabled = DataMatrix.getBoolean(SettingKeys.Workout.DATA_TREAD_SENSE_ENABLED_BOOL);
        mTreadSenseEnabled = treadSenseEnabled && mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill;
    }

    private void startTreadSenseIfNeeded() {
        boolean hasPerson = DataMatrix.getBoolean(HALKeys.DATA_PERSON_IN_PLACE_BOOL);
        processPersonInPlace(hasPerson);
    }

    /**
     * Starts the timer for TreadSense, stopping the workout if the timer expires
     */
    private void startTreadSenseTimer() {
        if (mFrameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Treadmill)
            return;
        if (!mTreadSenseEnabled) return;
        int timeoutSeconds = DataMatrix.getInt(SettingKeys.Workout.DATA_TREAD_SENSE_TIMEOUT_SEC_INT);
	/*in non used condition over 6 sec
	The LCB shall response 0x55 to UCB*/
        timeoutSeconds = timeoutSeconds - 6;
        if (timeoutSeconds < 0) timeoutSeconds = 0;

        stopTreadSenseTimer();
        mTreadSenseStarted = true;
        mTreadSenseTimer = new Timer();
        mTreadSenseTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                DataMatrix.sendEvent(UiServiceKeys.ACTION_SHOW_TREAD_SENSE_EVENT);
            }
        }, timeoutSeconds * 1000L);
    }

    /**
     * Stops the tread sense timer
     */
    private void stopTreadSenseTimer() {
        mTreadSenseStarted = false;

        DataMatrix.sendEvent(UiServiceKeys.ACTION_HIDE_TREAD_SENSE_EVENT);
        if (mTreadSenseTimer != null) {
            mTreadSenseTimer.cancel();
            mTreadSenseTimer.purge();
            mTreadSenseTimer = null;
        }
    }

    /**
     * Resets the tread sense timer to the full time if needed
     */
    private void resetTreadSenseTimer() {
        if (mTreadSenseEnabled && mTreadSenseStarted) {
            startTreadSenseTimer();
        }
    }

    /**
     * Single method for launching (or switching) a workout
     * @param data contains all the launch data and options required
     */
    private void launchWorkout(WorkoutLaunchData data) {
        synchronized (workoutTransitionLock) { // debating if this should be part of state lock or not?
            // if already in a workout, ignore (unless you are switching)
            int state = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT);

            // helper will track and check if launch is already in progress
            if (state != WorkoutKeys.Constants.RUN_STATE_STOP) return; // do not interrupt running workout
            workoutLaunchHelper.launchWorkout(
                    () -> {
                        // setup parameters
                        initialValuesSet(data.getInitialConditionsList());
                        updateSettings(data.getWorkoutSettingOverrides());
                        return null; // Unit
                    },
                    mWorkoutEntry.appId, // UI to launch
                    //Could be an inital condition/setting, but there is a race condition with the
                    //values being parsed asynchronously in the function above
                    DataMatrix.getBoolean(WorkoutKeys.DATA_ALLOW_COUNTDOWN_BOOL),
                    () -> {
                        // then start workout
                        startWorkoutEvent();
                        return null; // Unit
                    }
            );
        }
    }

    /**
     * Logic to reverse the treadmill belt on Hybrid Treadmill machines
     * This should stop the belt, start a countdown, and then resume with the belt at 0.1 mph
     */
    private void reverseTreadmillBelt(boolean isReverse){
        workoutLaunchHelper.launchPanel(
                () -> {
                    // clean up
                    reverseTreadmillBeltSetup(isReverse);
                    return null; // Unit
                },
                () -> {
                    //No new parameters needed
                    return null; // Unit
                },
                PanelIds.REVERSE_COUNTDOWN_PANEL.id.toString(),
                () -> {
                    // then resume workout
                    reverseTreadmillBeltResume();
                    return null; // Unit
                }
        );
    }

    private void reverseTreadmillBeltSetup(Boolean isReverse){
        if (!mController.isRunning() && !mController.isPaused()) return; // whether running or paused, if something was started it must be stopped
        mController.pauseWorkout();

        stopTreadSenseTimer();
        DataMatrix.sendEvent(HALKeys.ACTION_STOP_MACHINE);
        Log.d(TAG, "####CS Stop machine trigger by reverseTreadmillBelt");

        frameControlDebounce.cancel();
        DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, 0f);
        DataMatrix.updateBoolean(HALKeys.DATA_TM_IS_REVERSE_BELT_BOOL, isReverse);
    }

    private void reverseTreadmillBeltResume(){
        DataMatrix.sendEvent(HALKeys.ACTION_START_MACHINE);
        startTreadSenseIfNeeded(); // Starts tread sense if enabled
        mController.resumeWorkout();
        DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, SpeedConvertUtil.convertMphToKph(0.1f));
        DataMatrix.putInt(WorkoutKeys.DATA_RUN_STATE_INT, WorkoutKeys.Constants.RUN_STATE_RUNNING);
    }
}
