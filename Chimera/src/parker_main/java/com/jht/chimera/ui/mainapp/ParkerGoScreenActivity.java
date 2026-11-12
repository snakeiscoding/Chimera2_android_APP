package com.jht.chimera.ui.mainapp;

import static com.jht.chimera.lib.widget.CommonRoutines.doesAssetExist;
import static com.jht.chimera.lib.widget.CommonRoutines.getAssetUri;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.ObjectUtil;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.FeaturedWorkoutKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.va.VAWorkoutList;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workout.WorkoutInput;
import com.jht.chimera.lib.workout.WorkoutManager;
import com.jht.chimera.lib.workout.WorkoutStartupParams;
import com.jht.chimera.lib.workout.platform.ChimeraWorkoutManager;
import com.jht.chimera.lib.workout.workoutlist.workoutmap.ParkerWorkoutMap;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.lib.workoutapps.AppIconManager;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.service.dapi.UserProfileManager;
import com.jht.chimera.service.matrixcomm.messages.events.EStopEventMonitor;
import com.jht.chimera.service.ui.panels.DigitalSafetyPinDialog;
import com.jht.chimera.ui.machinesettings.EngineeringScreen;
import com.jht.chimera.ui.mainapp.dialog.UpdateDialog;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenSettingsMenu;
import com.jht.chimera.ui.mainapp.goscreen.ParkerGoScreenModule;
import com.jht.chimera.ui.mainapp.goscreen.ParkerHomeScreenIdleTimer;
import com.jht.chimera.ui.mainapp.goscreen.SecurityPinSetupScreen;
import com.jht.chimera.ui.mainapp.userprofiles.UserProfileDialog;
import com.jht.chimera.ui.mainapp.workouttracking.WorkoutTrackingService;
import com.jht.data.WorkoutItem;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenBaseFragment;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItemJSONParser;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter;
import com.jhtp.dapiservice.an.types.ButtonPressType;
import com.jhtp.dapiservice.wt.DapiProgramType;

import java.util.ArrayList;
import java.util.List;

@dagger.hilt.android.AndroidEntryPoint
public class ParkerGoScreenActivity extends RestringActivity {

    public static final String TAG = "ParkerGoScreenActivity";

    private ParkerGoScreenModule mGoScreenModule;
    private Button mLoginButton;
    private TextView mLoginText;
    private TextView mTitleBar;

    private ChimeraWorkoutManager mGoScreenWorkoutManager;
    private DataMatrix mDataMatrix = new DataMatrix();
    private ParkerHomeScreenIdleTimer mHomeScreenIdleTimer;
    private GoScreenSettingsMenu mSettingsMenu;

    private UserProfileDialog userProfileDialog;
    private boolean isServiceModeKey1Down = false;
    private boolean isServiceModeKey2Down = false;
    private JHTTimer serviceModeTimer;
    private UpdateDialog updateDialog;

    private List<CustomWorkoutItem> customWorkoutItems;
    private boolean isPaused = false;
    private DigitalSafetyPinDialog safetyPrompt; // for pre-flight checks
    private SecurityPinSetupScreen securityPinSetupScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Lock orientation to landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mHomeScreenIdleTimer = new ParkerHomeScreenIdleTimer(this);

        // start the graph workout tracking service
        try {
            Intent serviceIntent = new Intent();
            serviceIntent.setClassName(getPackageName(), WorkoutTrackingService.class.getCanonicalName());
            startService(serviceIntent);
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        setContentView(R.layout.parker_go_screen_activity);
        DataMatrix.sendEvent(HALKeys.ACTION_PWM_CAL_TABLE);
        initGoScreen(0);

        //Running the security pin setup dialog for parker screen
        if (!SecurityPinSetupScreen.shouldShowPinSetupDialog()) {
            if (securityPinSetupScreen == null) {
                securityPinSetupScreen = new SecurityPinSetupScreen(getApplicationContext());
                securityPinSetupScreen.showSecurityPinSetupDialog();
            }
        }

        // the ifit app is visible based on whether it's blocked or intentionally hidden
        if(ProductConfig.isIfitBlocked() || ProductConfig.isIfitBlockedOutsideNorthAmerica()){
            DataMatrix.putBoolean(FeaturedWorkoutKeys.DATA_IFIT_APP_VISIBLE_BOOL, false);
        }else {
            boolean isVisible = !DataMatrix.getBoolean(SettingKeys.Experimental.DATA_FORCE_HIDE_IFIT_APP_BOOL);
            DataMatrix.putBoolean(FeaturedWorkoutKeys.DATA_IFIT_APP_VISIBLE_BOOL, isVisible);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;

        int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
        String appUuid = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING);
        if (!ObjectUtil.isNullOrEmpty(appUuid)) { // this needs to match whatever splash screen resets it
            // we somehow got to go screen bypassing the splash screen
            Log.e(TAG, "Activity is resuming but state has not been reset, workoutId=" + workoutId + ", appUuid=" + appUuid);
            LaunchAppUtil.launchSplashScreen(); // make sure everything gets reset
        }

        init();
        setupDataMatrixCallbacks();

        DataMatrix.putBoolean(UiServiceKeys.ACTION_DISABLE_SAFETY_SCREENS_BOOL, false);
        DataMatrix.sendEvent(LoggerKeys.ACTION_CHECK_BELT_LUBE);

        // Initialize audio input if it hasn't been already, defaulting to 'Foreground',
        // which seems to mean audio from the aux port plays.
        // DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM may be defaulted to 0 by ConsoleOverrides#loadConsoleSettings,
        // Take that to mean that ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT has never been broadcast,
        // and go ahead and do it:
        int currentInputIndex = DataMatrix.getInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT, -1);
        Log.d(TAG, "Current audio source (DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM) = "+currentInputIndex);
        if (currentInputIndex == 0 || currentInputIndex == -1) {
            if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
                DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.LineIn.ordinal());
            } else {
                Log.d(TAG, "Sending " + AudioSourceInput.Foreground.ordinal() + " (" + AudioSourceInput.Foreground + ") to ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT");
                DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Foreground.ordinal());
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isPaused = true;
        mDataMatrix.finish();
        mGoScreenWorkoutManager.clearInputCache();
        mHomeScreenIdleTimer.stop();
        updateDialog.dismiss();
        if (safetyPrompt != null) safetyPrompt.dismiss();
    }

    private void initGoScreen(int defaultIndex) {
        mGoScreenWorkoutManager = new ChimeraWorkoutManager(this);
        mGoScreenModule = new ParkerGoScreenModule(this, defaultIndex, new ParkerGoScreenModule.IGoScreenInterface() {
            @Override
            public void onWorkoutSelected(String identifier) {
                Log.d(TAG, "a workout has been selected: " + identifier);
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, identifier.replace(":","-") + "-workoutSelect"); //NON-NLS
                mGoScreenWorkoutManager.loadWorkout(identifier, parameters -> mGoScreenModule.setWorkoutParameters(parameters));
            }

            @Override
            public void onWorkoutStart(String workoutId, List<WorkoutParameter> list) {
                checkIfSafetyPinNeeded(()->{
                    DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.GENERAL_START_WORKOUT_BTN_PRESS.name);
                    Log.d(TAG, "onWorkoutStart: " + workoutId);
                    mGoScreenWorkoutManager.startWorkout(workoutId, list);
                });
            }

            @Override
            public void onQuickStart() {
                checkIfSafetyPinNeeded(()-> {
                    Log.d(TAG, "Clicked quickstart workout");
                    DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.GO_QUICKSTART_CONSOLE_BTN_PRESS.name);
                    startGoButtonWorkout();
                });
            }

            @Override
            public void saveCustomWorkouts(List<CustomWorkoutItem> list) {
                String json;
                try {
                    json = CustomWorkoutItemJSONParser.toJsonObject(list, DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)).toString();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to save custom workout: ", e);
                    return;
                }
                // should we check if the json actually changed before triggering any updates? would this help avoid overwriting new workouts with older ones?
                if (DataMatrix.getString(DAPIKeys.DATA_USER_CUSTOM_WORKOUT_JSON_STRING).equals(json)) return;
                DataMatrix.putString(DAPIKeys.ACTION_UPDATE_CUSTOM_WORKOUT_JSON_STRING, json);
            }

            @Override
            public void onCustomWorkoutStart(CustomWorkoutItem customWorkoutItem) {
                checkIfSafetyPinNeeded(()-> {
                    DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.CUSTOM_WORKOUT_START_WORKOUT_BTN_PRESS.name);
                    DataMatrix.putString(WorkoutKeys.DATA_CUSTOM_WORKOUT_UUID_STRING, ObjectUtil.nullableToString(customWorkoutItem.uuid));
                    mGoScreenWorkoutManager.startCustomWorkout(customWorkoutItem);
                });
            }

            @Override
            public void launchThirdPartyInterface(String appUuid) {
                if (AppIds.IFIT_UUID.equals(appUuid)) {
                    // since ifit does not yet have a safety PIN function we can only pre-check at launch
                    // for ifit workouts after the first we'll have to fail the start, prompt for PIN, and have them re-start
                    checkIfSafetyPinNeeded(()-> ParkerGoScreenActivity.this.preLaunchApp(appUuid));
                } else {
                    ParkerGoScreenActivity.this.preLaunchApp(appUuid);
                }
            }

            @Override
            public void onDisabledWorkoutSelected(String s) {
            }

            @Override
            public void setCurrentFragment(GoScreenBaseFragment fragment) {
                mGoScreenModule.setCurrentFragment(fragment);
            }

            @Override
            public void selectCategory(int index) {
                mGoScreenModule.selectCategory(index);
            }

            @Override
            public void selectCustomWorkout(String uuid) {
                mGoScreenModule.selectCustomWorkout(uuid);
            }
        });

        mGoScreenModule.setVAWorkoutItems(mGoScreenWorkoutManager.setVaWorkouts(this));
        mGoScreenModule.setWorkoutItems(mGoScreenWorkoutManager.getDisplayedWorkouts(this));
        JHTCommonTypes.MachineClassificationType machineClassType = FrameConfigManager.getInstance().getFrameConfig().machineClassType;
        if (machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill ||
                machineClassType == JHTCommonTypes.MachineClassificationType.Stepper) {
            mGoScreenModule.setLandmarksWorkoutItems(mGoScreenWorkoutManager.setLandmarksWorkouts(this));
        }

        // load custom workouts for user
        loadCustomWorkoutsFromJson(DataMatrix.getString(DAPIKeys.DATA_USER_CUSTOM_WORKOUT_JSON_STRING));

        mGoScreenModule.setCustomWorkoutIsGuest(DapiUser.isGuestUser());
        mGoScreenModule.setCustomWorkoutIsOfflineUser(DapiUser.isOfflineUser());

        serviceModeTimer = new JHTTimer(5000, () -> {
            DataMatrix.putBoolean(UiServiceKeys.ACTION_DISABLE_SAFETY_SCREENS_BOOL, true);
            LaunchAppUtil.launchInternalScreen(EngineeringScreen.class.getCanonicalName(), "service");
        });
    }

    protected void loadCustomWorkoutsFromJson(String customWorkoutsJson) {
        try {
            customWorkoutItems = CustomWorkoutItemJSONParser.fromJson(customWorkoutsJson, DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL));
        } catch (Exception e) {
            Log.d(TAG, "Failed to load custom workouts from JSON: ", e);
        }
        mGoScreenModule.setCustomWorkoutItems(this, mGoScreenWorkoutManager.getCustomWorkoutParameters(), customWorkoutItems);
        mGoScreenModule.setWorkoutLabelProvider(myWorkoutLabelProvider); // custom names may have changed
    }

    public void init() {
        updateDialog = new UpdateDialog(getApplicationContext());

        mSettingsMenu = findViewById(R.id.settings_menu);
        findViewById(R.id.buttonSettings).setOnClickListener(v -> mSettingsMenu.setVisibility(View.VISIBLE));

        mTitleBar = findViewById(R.id.topbar_logo);

        // Setup the home button
        mLoginButton = findViewById(R.id.buttonLogin);
        mLoginText = findViewById(R.id.login_name_text);

        mLoginButton.setOnClickListener(v -> showUserProfiles());
        mLoginText.setOnClickListener(v -> showUserProfiles());
        
        // Need this callback so we can get custom workout information if it's sent late
        mDataMatrix.createStringAdapter(DAPIKeys.DATA_USER_CUSTOM_WORKOUT_JSON_STRING, this::loadCustomWorkoutsFromJson, false);

        // since user change will reload this screen, this might be redundant
        mDataMatrix.createStringAdapter(DAPIKeys.DATA_USER_DAPI_UUID_STRING, uuid -> {
            mGoScreenModule.setCustomWorkoutIsGuest(uuid.equals(DapiUser.GUEST_USER_UUID)); // does parker allow guest to save since we have offline user storage? assuming no
            mGoScreenModule.setCustomWorkoutIsOfflineUser(DapiUser.isOfflineUser());
        }, false);

    }

    void setupDataMatrixCallbacks() {
        mDataMatrix.createBoolAdapter(UiServiceKeys.DATA_SHOW_ERROR_PANEL_BOOL, shown -> {
            if (shown) { mHomeScreenIdleTimer.stop(); }
            else if (!isPaused) { mHomeScreenIdleTimer.start(); } // there was a race condition between this start and the stop in onPause
        }, true);

        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, value -> {
            KeyPress.KeyCode keyCode = KeyPress.getKeyCode(value);
            KeyPress.KeyState keyState = KeyPress.getKeyState(value);

            if (!DataMatrix.getBoolean(SettingKeys.General.DATA_SHOW_OUT_OF_ORDER_BOOL) && keyCode == KeyPress.KeyCode.KEY_GO && keyState == KeyPress.KeyState.KEY_DOWN) {
                if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHYSICAL_GO_BTN_BOOL)) {
                    checkIfSafetyPinNeeded(()-> startGoButtonWorkout());
                }
            }

            checkServiceModeLogic(keyCode, keyState);
        });

        mDataMatrix.createStringAdapter(DAPIKeys.DATA_USER_FIRST_NAME_STRING, value -> {
            setLoginButton();
        }, true);

        mDataMatrix.createParcelableAdapter(VaKeys.DATA_VA_ITEMS_PARCELABLE, VAWorkoutList.class, value -> {
            // update VA list if USB is mounted/unmounted
            mGoScreenModule.setVAWorkoutItems(mGoScreenWorkoutManager.setVaWorkouts(ParkerGoScreenActivity.this));
        });

        mDataMatrix.createBoolAdapter(DAPIKeys.DATA_IS_AUTO_UPDATE_READY_BOOL, value -> {
            // check if we need to show the software update dialog
            if (DataMatrix.getBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_BOOL) && value &&  !DataMatrix.getBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_DLG_SHOWN_BOOL)) {
                updateDialog.show();
                DataMatrix.putBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_DLG_SHOWN_BOOL, true);
            } else {
                updateDialog.dismiss();
            }
        });

        mDataMatrix.createEventAdapter(HALKeys.ACTION_SETUP_FRAME, keyReturnedBack -> {
            LaunchAppUtil.launchSplashScreen(); // launch splash screen to reload screens that may have frame specific content
        }, false);

        mDataMatrix.createEventAdapter(UserProfileManager.Keys.EVENT_PROFILE_UPDATED, e -> {
            UserProfileManager.getInstance().reloadData(); // Dapi service and main app may each have a profile singleton, make sure we grab latest data from backing store
            if (userProfileDialog != null) {
                userProfileDialog.profileUpdated();
            }
            setLoginButton(); // since initials come from profile manager, DM first name event might fire before profile data gets saved/loaded here
        }, false);

        mDataMatrix.createBoolAdapter(HALKeys.DATA_FRAME_CONSOLE_COMBO_INVALID_BOOL, isInvalid -> {
            if (isInvalid) {
                runOnUiThread(()->{
                    MessageCenterMessage msgInvalidCombo = new MessageCenterMessage()
                            .setMessageText(getString(R.string.frame_not_compatible_pg))
                            .setHideTime(999999)
                            .setTextColor(Color.RED)
                            .setBackgroundColor(Color.BLACK);
                    DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msgInvalidCombo);

                    /* dialog box messes with automation testing, hold off for now unless we really want it
                    SimpleMessageDialog dialog = DialogFactory.message(mTitleBar, getString(R.string.frame_not_compatible_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                    dialog.show(mTitleBar); // message function already contains show, but for some reason conditional bypasses it */
                });
            }
        }, true);
    }

    private void checkForPassBounce() {
        UserProfileManager.ProfileUser user = UserProfileManager.getInstance().getCurrentProfile();
        if (user.isPassBounceThresholdHit()) {
            Log.w(TAG, "Passcode bounced for " + user.xid);
            if (userProfileDialog == null) userProfileDialog = new UserProfileDialog(ParkerGoScreenActivity.this);
            if (!userProfileDialog.isShown()) userProfileDialog.showUpdatePasscode(mLoginButton, user);
        }
    }

    public static boolean startGoButtonWorkout() { return startGoButtonWorkout(null); }
    public static boolean startGoButtonWorkout(String appId) {
        Log.d(TAG, "Starting quick start workout");
        WorkoutManager workoutManager = new WorkoutManager(new ParkerWorkoutMap());
        if (appId == null) workoutManager.loadQuickStartWorkout(null);
        else workoutManager.loadQuickStartWorkout(appId, null);
        return workoutManager.startLoadedWorkout(getQuickStartInitialConditions(), false);
    }

    public static List<WorkoutInput> getQuickStartInitialConditions() {
        List<WorkoutInput> initialConditions = new ArrayList<>();
        initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.TIME_GOAL_START, DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_TIME_MIN_INT)));
        initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.START_SPEED, DataMatrix.getFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT)));
        initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.START_INCLINE, FrameConfigManager.getInstance().getFrameConfig().incline.getMin()));
        initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.START_RESISTANCE, DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_LEVEL_INT)));
        return initialConditions;
    }

    private void showUserProfiles() {
        if (userProfileDialog == null)
            userProfileDialog = new UserProfileDialog(ParkerGoScreenActivity.this);
        userProfileDialog.show(mLoginButton);
    }

    private void setLoginButton() {
        runOnUiThread(()->{
            if (mLoginText == null) {
                return;
            }
            mLoginText.setText(UserProfileManager.getInstance().getCurrentProfile().getInitials());
            checkForPassBounce(); // if login button is being updated, user may have changed
        });
    }

    @Override
    public void setTitle(int id) {
        super.setTitle(id);
        if (mTitleBar != null) mTitleBar.setText(id);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (mTitleBar != null) mTitleBar.setText(title);
    }

    void checkServiceModeLogic(KeyPress.KeyCode keyCode, KeyPress.KeyState keyState) {
        // Access service mode by holding the physical buttons Incline Up + Speed Down on Treadmills,
        // Speed Up + Speed Down on Climbmills, and Resistance Up + Resistance Down

        KeyPress.KeyCode keyCode1 = KeyPress.KeyCode.KEY_UNKNOWN;
        KeyPress.KeyCode keyCode2 = KeyPress.KeyCode.KEY_UNKNOWN;
        switch (FrameConfigManager.getInstance().getFrameConfig().machineClassType) {
            case Treadmill:
                keyCode1 = KeyPress.KeyCode.KEY_INCLINE_UP;
                keyCode2 = KeyPress.KeyCode.KEY_SPEED_DOWN;
                break;

            case Climbmill:
                keyCode1 = KeyPress.KeyCode.KEY_SPEED_UP;
                keyCode2 = KeyPress.KeyCode.KEY_SPEED_DOWN;
                break;

            default: // bikes, ep, at
                keyCode1 = KeyPress.KeyCode.KEY_RESISTANCE_UP;
                keyCode2 = KeyPress.KeyCode.KEY_RESISTANCE_DOWN;
                break;
        }


        if (keyCode == keyCode1) {
            if (keyState == KeyPress.KeyState.KEY_DOWN) {
                isServiceModeKey1Down = true;
            } else {
                isServiceModeKey1Down = false;
            }
        }
        if (keyCode == keyCode2) {
            if (keyState == KeyPress.KeyState.KEY_DOWN) {
                isServiceModeKey2Down = true;
            } else {
                isServiceModeKey2Down = false;
            }
        }

        // if both key1 and key2 are down, start the timer for service mode
        if (isServiceModeKey1Down && isServiceModeKey2Down) {
            serviceModeTimer.start();
        } else {
            serviceModeTimer.stop();
        }
    }


    // Is this the best place for these? getWorkoutLabels does need access to custom workout items list?
    public static class WorkoutLabels { // return both workout and app name so ui can decide how to reconcile them
        public String workoutName;
        public Uri workoutBackground;
        public AppIcon appIcon;
    }
    public interface WorkoutLabelProvider {
        WorkoutLabels getWorkoutLabels(WorkoutStartupParams startupParams);
        boolean customWorkoutExists(String customWorkoutUuid);  // abuse fact that this provider has access to the custom workout list
    }
    private WorkoutLabelProvider myWorkoutLabelProvider = new WorkoutLabelProvider() {

        public WorkoutLabels getWorkoutLabels(WorkoutStartupParams startupParams) {
            WorkoutLabels labels = new WorkoutLabels();
            if (startupParams != null && startupParams.programTypeId != null) {
                ParkerWorkoutMap workoutMap = new ParkerWorkoutMap();
                WorkoutItem workoutEntry = workoutMap.getWorkout(startupParams.programTypeId);
                // PRKS-1303: We want the title to read "Manual Workout", not just "Manual",
                // especially when the workout actually includes running some app.
                // The meaning of "Manual" by itself under the Instagram icon is less than obvious!
                if (startupParams.programTypeId == DapiProgramType.Manual.Value) {
                    labels.workoutName = StringLookup.get(R.string.manual_workout_tc);
                    labels.appIcon = AppIconManager.getInstance().getAppIcon(startupParams.appUuid);
                } else if (startupParams.programTypeId == DapiProgramType.Custom.Value) {
                    // looks like custom is not in the database (but we shouldn't use that one anyway)
                    labels.workoutName = StringLookup.get(R.string.custom_no_ellipes_tc);
                } else if (workoutEntry == null) {
                    labels.workoutName = StringLookup.get(R.string.program_tc) + " " + startupParams.programTypeId;
                } else {
                    labels.workoutName = StringLookup.get(workoutEntry.getTitleId());
                }
                if (startupParams.programTypeId == DapiProgramType.CardioCustomWorkout.Value && ParkerGoScreenActivity.this.customWorkoutItems != null) {
                    for (CustomWorkoutItem item : ParkerGoScreenActivity.this.customWorkoutItems) {
                        if (item.uuid != null && item.uuid.toString().equals(startupParams.customWorkoutUuid)) {
                            if (item.title != null && !item.title.isEmpty()) labels.workoutName = item.title;
                            break;
                        }
                    }
                }
                // workout background image
                Context context = ContextUtil.getApplicationContext();
                assert context != null;
                if (workoutEntry != null) {
                    // use the thumbnail defined in the database
                    labels.workoutBackground = workoutEntry.getThumbnail();
                }
                // looks like placeholder icon exists in workout database so use the correct images
                DapiProgramType dapiProgramType = DapiProgramType.fromInt(startupParams.programTypeId);
                if (dapiProgramType != null) {
                    switch (dapiProgramType) {
                        case Sprint8:
                            labels.workoutBackground = getAssetUri("samples/sprint-8-feature.jpg");
                            break;
                        case VirtualActive:
                            labels.workoutBackground = getAssetUri("samples/feature_va.jpg");
                            break;
                        case Landmark:
                            labels.workoutBackground = getAssetUri("samples/landmarks-feature.jpg");
                            break;
                        case Custom:
                            labels.workoutBackground = getAssetUri("thumbnails/bg_custom.jpg");
                            break;
                    }
                }
            }
            return labels;
        }

        public boolean customWorkoutExists(String customWorkoutUuid) {
            if (ParkerGoScreenActivity.this.customWorkoutItems != null) {
                for (CustomWorkoutItem item : ParkerGoScreenActivity.this.customWorkoutItems) {
                    if (item.uuid != null && item.uuid.toString().equals(customWorkoutUuid)) return true;
                }
            }
            return false;
        }
    };

    private void preLaunchApp(String uuid) {
        LaunchAppUtil.initialLaunchApp(getApplicationContext(), uuid, getWindow().getDecorView().findViewById(android.R.id.content));
    }

    public void checkIfSafetyPinNeeded(DigitalSafetyPinDialog.Action action) {

        if (EStopEventMonitor.isAnySafetyKeyTriggered()){
            Log.d(TAG, "The safety pin dialog never pops up on the E-stop screen.");
            return;
        }

        if (DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL)) {
            if (safetyPrompt == null) safetyPrompt = new DigitalSafetyPinDialog(getApplicationContext());
            safetyPrompt.checkIfNeeded(action);
        } else {
            action.doAction();
        }
    }
}
