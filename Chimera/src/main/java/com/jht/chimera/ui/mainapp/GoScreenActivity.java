package com.jht.chimera.ui.mainapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.StateSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.SystemBrightness;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.datamatrixkeys.ConsoleConfigKeys;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TiersKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.data.intent.IntentConstants;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.common.i18n.LocaleUtil;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.lib.common.process.KillProcessHelper;
import com.jht.chimera.lib.common.timer.HomeScreenIdleTimer;
import com.jht.chimera.lib.download.MiscUtilHelper;
import com.jht.chimera.lib.login.RFIDLogin;
import com.jht.chimera.lib.va.VAWorkoutList;
import com.jht.chimera.lib.widget.button.HomeButton;
import com.jht.chimera.lib.widget.dialog.MessageBoxDialog;
import com.jht.chimera.lib.widget.listview.DefaultItemAdapter;
import com.jht.chimera.lib.widget.selectron.SelectronButton;
import com.jht.chimera.lib.widget.sound.SystemVolume;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workout.WorkoutInput;
import com.jht.chimera.lib.workout.WorkoutManager;
import com.jht.chimera.lib.workout.platform.ChimeraWorkoutManager;
import com.jht.chimera.lib.workout.workoutlist.workoutmap.ChimeraWorkoutMap;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.lib.workoutapps.AppIconManager;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.service.hal.lib.hardware.AudioSelectManager;
import com.jht.chimera.service.tiers.FeatureUpgrade;
import com.jht.chimera.service.tiers.PremiumFeature;
import com.jht.chimera.service.tiers.PremiumFeatureSet;
import com.jht.chimera.service.tiers.UpgradeSkin;
import com.jht.chimera.service.tiers.dialogs.BusyStatusDialog;
import com.jht.chimera.service.tiers.dialogs.MultiChoiceDialog;
import com.jht.chimera.service.ui.panels.DigitalSafetyPinDialog;
import com.jht.chimera.ui.machinesettings.JHTTypes;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenModule;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItemJSONParser;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter;
import com.jht.chimera.ui.mainapp.userprofiles.UserProfileDialog;
import com.jht.chimera.ui.mainapp.util.BackgroundManager;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.rfid.Legic.RFIDChip;
import com.jhtp.dapiservice.an.types.ButtonPressType;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@dagger.hilt.android.AndroidEntryPoint
public class GoScreenActivity extends RestringActivity {

    public static final String TAG = "GoScreenActivity";

    private GoScreenModule mGoScreenModule;
    private final List<FeaturedItem> mMainFeaturedItems = new ArrayList<>();
    private final List<FeaturedItem> mFeaturedItems = new ArrayList<>();
    private Button mLoginButton;
    private TextView mLoginText;

    private ChimeraWorkoutManager mWorkoutManager;

    //welcome screen slideshow stuff
    private Animation fadeInAnimation;
    private Animation fadeOutAnimation;
    private ImageView background;
    private ImageView background2;
    private ImageView touchIcon;
    private ImageView goBusyImage;
    private final Handler mHandler = new Handler();
    private JHTTimer welcomeDisabledBackgroundTimer;
    private BackgroundManager backgroundManager;

    private final DataMatrix mDataMatrix = new DataMatrix();
    private HomeScreenIdleTimer mHomeScreenIdleTimer;
    private BusyStatusDialog mBusyStatusDialog;
    private ConstraintLayout mLanguageBackground;
    private RecyclerView mLanguageRecyclerView;
    private RelativeLayout mGoScreenOverlay;

    private boolean mScreenIsHome = false;
    private boolean slideShowActive = false;
    private boolean init = false;

    private UserProfileDialog userProfileDialog;
    private boolean lockOverlay = false;
    private Animation mSpinner;
    private boolean enableStartWorkout;
    private boolean goButtonAlreadyClicked = false;
    private RFIDLogin rfidLogin;
    private MessageBoxDialog mMessageBoxDialog;
    private DigitalSafetyPinDialog safetyPrompt; // for pre-flight checks

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.go_screen_new_activity);

        // Detects whether the go screen should be the home screen as opposed to the welcome screen
        Bundle extras = getIntent().getExtras();
        mScreenIsHome = extras != null && extras.getBoolean(IntentConstants.GoScreenIsHomeBool, false);

        mHomeScreenIdleTimer = new HomeScreenIdleTimer(this, mScreenIsHome);

        mLoginButton = findViewById(R.id.buttonLogin);
        mLoginText = findViewById(R.id.login_name_text);

        mLanguageBackground = findViewById(R.id.language_background);
        goBusyImage = findViewById(R.id.goBusy);
        mGoScreenOverlay = findViewById(R.id.go_screen_overlay);
        mGoScreenOverlay.setVisibility(View.VISIBLE);

        Handler handler = new Handler();
        mSpinner = AnimationUtils.loadAnimation(this, R.anim.spinning_clockwise);
        new JHTTimer(500, () -> handler .post(() -> {
            if(mGoScreenOverlay.getVisibility() == View.VISIBLE) {
                lockOverlay = true;
                goBusyImage.setVisibility(View.VISIBLE);
                goBusyImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.spinning_clockwise));
            }
        })).start();

        new JHTTimer(2500, () -> handler.post(() -> {
            if(lockOverlay) {
                lockOverlay = false;
                checkUserInfo();
            }
        })).start();

        init = false;
        mDataMatrix.createBoolAdapter(DAPIKeys.DATA_GETTING_USER_TIERS_BOOL, value -> {
            checkUserInfo();
        }, false);

        mDataMatrix.createBoolAdapter(DAPIKeys.DATA_GETTING_USER_CUSTOM_WORKOUT_BOOL, value -> {
            checkUserInfo();
        }, false);

        mDataMatrix.createBoolAdapter(DAPIKeys.DATA_GETTING_USER_INFO_BOOL, value -> {
            if (!value)
                initRFIDLogin(); // Begin RFID login scanning after DAPI completes getting user info
            checkUserInfo();
        }, true);

        // begin fade in/out background stuff if welcome screen disabled

        int timeout = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT) * 1000;
        slideShowActive = timeout > 0;
        welcomeDisabledBackgroundTimer = new JHTTimer(timeout, () -> mHandler.post(() -> {
            if (background != null && background.getVisibility() == View.INVISIBLE) {
                fadeInSlideshow();
            }
        }));

        // Required for some apps audio to function (YMCA360, Sprint8, iFIT, etc.)
        requestAudioFocus();
        SystemVolume.getInstance().setVolume(AudioSelectManager.getMasterVolume());
        DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Foreground.ordinal());

        if(mScreenIsHome) { // go screen is the home screen
            fadeInAnimation = new AlphaAnimation(0, 1);
            fadeOutAnimation = new AlphaAnimation(1, 0);
            background = findViewById(R.id.welcome_disabled_background);
            background2 = findViewById(R.id.welcome_disabled_background2);
            touchIcon = findViewById(R.id.welcome_disabled_hand);
        }

        if (!DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL)){
            DataMatrix.sendEvent(LoggerKeys.ACTION_RECORD_BOOT_TIME);
        }
    }

    private boolean mPtpInitialized = false;

    private void checkUserInfo() {
        if (!init && !DataMatrix.getBoolean(DAPIKeys.DATA_GETTING_USER_INFO_BOOL) && !DataMatrix.getBoolean(DAPIKeys.DATA_GETTING_USER_TIERS_BOOL)) {
            init = true;
            initGoScreen();
            init();
        }

        if (!DataMatrix.getBoolean(DAPIKeys.DATA_GETTING_USER_TIERS_BOOL) &&
                !DataMatrix.getBoolean(DAPIKeys.DATA_GETTING_USER_INFO_BOOL) &&
                !DataMatrix.getBoolean(DAPIKeys.DATA_GETTING_USER_CUSTOM_WORKOUT_BOOL) &&
                !lockOverlay) {

            if (goBusyImage.getVisibility() == View.VISIBLE) {
                mSpinner.cancel();
                goBusyImage.setVisibility(View.GONE);
            }
            mGoScreenOverlay.setVisibility(View.GONE);
            Log.d(TAG, "ptp_timing: Ready to show go screen");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mDataMatrix.createBoolAdapter(DAPIKeys.DATA_GETTING_USER_INFO_BOOL, value -> {
            initRFIDLogin();
        });

        //slideshow stuff
        backgroundManager = new BackgroundManager(findViewById(R.id.welcome_disabled_background), findViewById(R.id.welcome_disabled_background2));
        mDataMatrix.createEventAdapter(ConsoleConfigKeys.EVENT_CONSOLE_THEME_READY, s -> {
            if (backgroundManager != null)
                backgroundManager.loadCustomImages();
        });

        DataMatrix.putBoolean(UiServiceKeys.ACTION_DISABLE_SAFETY_SCREENS_BOOL, false);
        mHomeScreenIdleTimer.start();

        if (slideShowActive && mScreenIsHome) {
            welcomeDisabledBackgroundTimer.start();
        }


        enableStartWorkout = true;
        goButtonAlreadyClicked = false;
    }

    private void initRFIDLogin() {
        rfidLogin = new RFIDLogin(mDataMatrix, new RFIDLogin.ILoginListener() {

            @Override
            public void onLoginAttemptStart() {
                showInfoDialog(null, getString(R.string.rid_login_msg_pg), false);
                enableStartWorkout = false;
            }

            @Override
            public void onLoginAttemptEnd(RFIDLogin.Result result, RFIDChip chip) {
                switch (result) {
                    case SUCCESS:
                        mMessageBoxDialog.closeMessageBox();
                        clearSSOCache();
                        LaunchAppUtil.launchGoScreen();
                        break;
                    case INVALID_CREDENTIALS:
                        showInfoDialog(null, getResources().getString(R.string.login_failed_message_pg), true);
                        mMessageBoxDialog.setOnDismissListener(dialog -> rfidLogin.start());
                        enableStartWorkout = true;
                        break;
                    case NO_ASSOCIATED_XID:
                        mMessageBoxDialog.setMessage(getResources().getString(R.string.rfid_associate_wtih_xid_msg_pg));
                        mMessageBoxDialog.setButtonsEnabled(true);
                        mMessageBoxDialog.setButton1OnClickListener(v -> {
                            mMessageBoxDialog.closeMessageBox();
                            mMessageBoxDialog.setButtonsEnabled(false);
                            DataMatrix.putParcelable(MainAppKeys.DATA_ASSOCIATE_RFID_CHIP_PARCELABLE, null);
                            rfidLogin.start();
                        });
                        mMessageBoxDialog.setButton0OnClickListener(v -> {
                            mMessageBoxDialog.closeMessageBox();
                            mMessageBoxDialog.setButtonsEnabled(false);
                            DataMatrix.putParcelable(MainAppKeys.DATA_ASSOCIATE_RFID_CHIP_PARCELABLE, chip);
                            launchLoginScreen();
                            rfidLogin.start();
                        });
                        enableStartWorkout = true;
                        break;
                    case CONNECTION_ERROR:
                        showInfoDialog(getResources().getString(R.string.rfid_pairing_failed_tc), getResources().getString(R.string.rfid_pairing_failed_msg_pg), true);
                        mMessageBoxDialog.setOnCloseCallback(() -> rfidLogin.start());
                        mMessageBoxDialog.setOnDismissListener((e) -> rfidLogin.start());
                        enableStartWorkout = true;
                        break;
                    default:
                        mMessageBoxDialog.dismiss();
                        enableStartWorkout = true;
                        break;
                }
            }

            @Override
            public void onWalletLoginAttemptStart() {
                // Not supported in old UI
            }

            @Override
            public void onWalletLoginAttemptEnd(RFIDLogin.Result result) {
                // Not supported in old UI
            }
        });
        rfidLogin.start(); // Starts RFID scanning
    }

    private void showInfoDialog(String titleText, String messageText, Boolean closeOnTouch) {
        if (mMessageBoxDialog == null) {
            mMessageBoxDialog = new MessageBoxDialog(this);
        }
        mMessageBoxDialog.setTitle(titleText);
        mMessageBoxDialog.setMessage(messageText);
        mMessageBoxDialog.setCanceledOnTouchOutside(closeOnTouch);
        mMessageBoxDialog.show();
    }

    void requestAudioFocus() {
        AudioManager mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes mAudioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
        AudioFocusRequest mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(mAudioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChange -> {

                })
                .build();
        mAudioManager.requestAudioFocus(mAudioFocusRequest);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mLoginText != null) {
            mLoginText.setOnClickListener(null);
        }

        if (mWorkoutManager != null) mWorkoutManager.clearInputCache();

        mHomeScreenIdleTimer.stop();
        mDataMatrix.finish();
        if (safetyPrompt != null) safetyPrompt.dismiss();

        // slideshow stuff
        if (backgroundManager != null) {
            backgroundManager.stopTimer();
            backgroundManager = null;
        }

        if (slideShowActive && mScreenIsHome) {
            welcomeDisabledBackgroundTimer.stop();
        }

        if (mMessageBoxDialog != null && mMessageBoxDialog.isShowing()) mMessageBoxDialog.dismiss();

        enableStartWorkout = false;
        if (rfidLogin != null) rfidLogin.stop();
        goButtonAlreadyClicked = true;
    }

    protected void loadCustomWorkoutsFromJson(String customWorkoutsJson) {
        final List<CustomWorkoutItem> customWorkoutItems;
        try {
            customWorkoutItems = CustomWorkoutItemJSONParser.fromJson(customWorkoutsJson, DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL));
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load custom workouts from JSON: ", e);
            return;
        }
        mGoScreenModule.setCustomWorkoutItems(this, mWorkoutManager.getCustomWorkoutParameters(), customWorkoutItems);
    }

    private void initGoScreen() {
        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, value -> {
            KeyPress.KeyCode keyCode = KeyPress.getKeyCode(value);
            KeyPress.KeyState keyState = KeyPress.getKeyState(value);
            if (keyCode == KeyPress.KeyCode.KEY_GO && keyState == KeyPress.KeyState.KEY_DOWN) {
                if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHYSICAL_GO_BTN_BOOL)) {
                    if (enableStartWorkout) {
                        if (!goButtonAlreadyClicked){
                            checkIfSafetyPinNeeded(() -> goButtonAlreadyClicked = GoScreenActivity.startGoButtonWorkout());
                        } else {
                            Log.d(TAG, "initGoScreen, goButtonAlreadyClicked true, can't start");
                        }
                    } else {
                        Log.d(TAG, "initGoScreen, enableStartWorkout false, can't start");
                    }
                }
            }
        });

        mDataMatrix.createStringAdapter(DAPIKeys.DATA_USER_FIRST_NAME_STRING, v -> setLoginButton(), true);

        mWorkoutManager = new ChimeraWorkoutManager(this);

        mGoScreenModule = new GoScreenModule(this, new GoScreenModule.IGoScreenInterface() {
            @Override
            public void onWorkoutSelected(String identifier) {
                Log.d(TAG, "a workout has been selected: " + identifier);

                //Stores workout selection button press event for Dapi Analytics
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, identifier.replace(":","-") + "-workoutSelect"); //NON-NLS

                // Load the selected workout and pass parameters into the go screen
                mWorkoutManager.loadWorkout(identifier, parameters -> mGoScreenModule.setWorkoutParameters(parameters));

            }

            @Override
            public void onWorkoutStart(String workoutId, List<WorkoutParameter> list) {
                checkIfSafetyPinNeeded(()-> {
                    //Stores workout start button press event for Dapi Analytics
                    DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.GENERAL_START_WORKOUT_BTN_PRESS.name);
                    Log.d(TAG, "onWorkoutStart: " + workoutId);
                    mWorkoutManager.startWorkout(workoutId, list);
                });
            }

            @Override
            public void onQuickStart() {
                checkIfSafetyPinNeeded(()-> {
                    Log.d(TAG, "Clicked quickstart workout");

                    //Stores Go button press event for Dapi Analytics
                    DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.GO_QUICKSTART_CONSOLE_BTN_PRESS.name);

                    startGoButtonWorkout();
                });
            }

            @Override
            public void saveCustomWorkouts(List<CustomWorkoutItem> list) {
                String json;
                try {
                    json = CustomWorkoutItemJSONParser.toJsonObject(list, DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)).toString();
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to save custom workout: ", e);
                    return;
                }
                DataMatrix.putString(DAPIKeys.ACTION_UPDATE_CUSTOM_WORKOUT_JSON_STRING, json);
            }

            @Override
            public void onCustomWorkoutStart(CustomWorkoutItem customWorkoutItem) {
                checkIfSafetyPinNeeded(()-> {
                    //Stores custom workout button press event for Dapi Analytics
                    DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.CUSTOM_WORKOUT_START_WORKOUT_BTN_PRESS.name);
                    mWorkoutManager.startCustomWorkout(customWorkoutItem);
                });
            }

            @Override
            public void onDisabledWorkoutSelected(String s) {
                Log.d(TAG, "id clicked on: " + s);
                if (DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL)) {
                    PremiumFeatureSet set = DataMatrix.getParcelable(TiersKeys.DATA_GUEST_FEATURE_SET_PARCELABLE, PremiumFeatureSet.class);
                    if (set != null) {
                        initiateUpgradeProcess(set, s);
                    }
                } else {
                    PremiumFeatureSet set = DataMatrix.getParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, PremiumFeatureSet.class);
                    if (set != null) {
                        initiateUpgradeProcess(set, s);
                    }
                }
            }

            @Override
            public boolean isChangeWorkout() {
                return false;
            }

            @Override
            public void getWorkoutInfo() {
                mWorkoutManager.retrieveModelInfo(workoutModel -> mGoScreenModule.setWorkoutModelInfo(workoutModel));
            }
        });

        new Thread(() -> {
            mGoScreenModule.setVAWorkoutItems(mWorkoutManager.setVaWorkouts(GoScreenActivity.this));
            mGoScreenModule.setWorkoutItems(mWorkoutManager.getDisplayedWorkouts(GoScreenActivity.this));
            mWorkoutManager.setMainAndFeaturedWorkouts(mMainFeaturedItems, mFeaturedItems);

            JHTCommonTypes.MachineClassificationType machineClassType = FrameConfigManager.getInstance().getFrameConfig().machineClassType;
            if (machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill ||
                    machineClassType == JHTCommonTypes.MachineClassificationType.Stepper) {
                mGoScreenModule.setLandmarksWorkoutItems(mWorkoutManager.setLandmarksWorkouts(GoScreenActivity.this));
            }

            mHandler.post(() -> {
                //Only create the data matrix adapter if the current va list is empty. Otherwise it is not needed as we can ignore
                //dynamic VA content being added as a use case (USB)
                if(mWorkoutManager.mVaWorkoutList.isEmpty()) {
                    mDataMatrix.createParcelableAdapter(VaKeys.DATA_VA_ITEMS_PARCELABLE, VAWorkoutList.class, vaItems -> {
                        mMainFeaturedItems.clear();
                        mFeaturedItems.clear();
                        mGoScreenModule.setVAWorkoutItems(mWorkoutManager.setVaWorkouts(GoScreenActivity.this));
                        mWorkoutManager.setMainAndFeaturedWorkouts(mMainFeaturedItems, mFeaturedItems);
                        mGoScreenModule.setMainFeaturedItems(mMainFeaturedItems);
                        mGoScreenModule.setFeaturedItems(mFeaturedItems);
                        mGoScreenModule.applyTiersMap(getWorkoutStateMap());
                    });
                }

                mGoScreenModule.setMainFeaturedItems(mMainFeaturedItems);
                mGoScreenModule.setFeaturedItems(mFeaturedItems);

                //check if all featured workouts are the same, if so, make it fullscreen on the toggle
                boolean fullscreen = true;
                for (FeaturedItem featuredItem : mMainFeaturedItems) {
                    if (featuredItem.workoutId != mMainFeaturedItems.get(0).workoutId) {
                        fullscreen = false;
                        break;
                    }
                }
                DataMatrix.putBoolean(UiServiceKeys.DATA_FEATURED_WORKOUT_FULLSCREEN_BOOL, fullscreen);
                if (mMainFeaturedItems.size() > 0)
                    DataMatrix.putInt(UiServiceKeys.DATA_FEATURED_WORKOUT_1_INT, mMainFeaturedItems.get(0).workoutId);
                if (mMainFeaturedItems.size() > 1)
                    DataMatrix.putInt(UiServiceKeys.DATA_FEATURED_WORKOUT_2_INT, mMainFeaturedItems.get(1).workoutId);
                if (mMainFeaturedItems.size() > 2)
                    DataMatrix.putInt(UiServiceKeys.DATA_FEATURED_WORKOUT_3_INT, mMainFeaturedItems.get(2).workoutId);

                boolean isGuest = DapiUser.isGuestUser();
                mGoScreenModule.setCustomWorkoutIsGuest(isGuest);

                if (isGuest || DataMatrix.getBoolean(DAPIKeys.DATA_EXTENDED_INFO_RECEIVED_BOOL)) {
                    loadCustomWorkoutsFromJson(DataMatrix.getString(DAPIKeys.DATA_USER_CUSTOM_WORKOUT_JSON_STRING));
                } else {
                    mGoScreenModule.setCustomWorkoutLoading(GoScreenActivity.this);
                }

                mGoScreenModule.applyTiersMap(getWorkoutStateMap());

                // Notify that the featured workouts has been force updated.
                // fixme: remove this band-aide
                DataMatrix.sendEvent(SettingKeys.ProjectOne.EVENT_FEATURED_WORKOUTS_UPDATED);
            });
        }).start();
    }

    public void init() {
        setLoginButton();
        findViewById(R.id.buttonSettings).setOnClickListener(v -> LaunchAppUtil.launchBluetoothPairing());
        View root = findViewById(R.id.go_screen_root);
        SelectronButton mSettingsButton = findViewById(R.id.buttonSettings);
        mSettingsButton.setOnClickListener(v -> LaunchAppUtil.launchBluetoothPairing("", root));

        // Setup the home button
        HomeButton homeButton = findViewById(R.id.buttonHome);
        if(!DataMatrix.getBoolean(SettingKeys.General.DATA_IS_PTP_USER_BOOL)) {
            homeButton.setOnClickListener(v ->
            {
                //Stores home button press events for Dapi Analytics
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.HOME_BTN_PRESS.name);

                if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_SEND_WORKOUT_DATA_TO_ALTERNATE_ENDPOINT_BOOL)) {
                    // Reset DATA_NON_DAPI_USER_CHIP_UUID_STRING to empty string if home button is pressed
                    DataMatrix.putString(DAPIKeys.DATA_NON_DAPI_USER_CHIP_UUID_STRING, "");
                }

                LaunchAppUtil.launchSplashScreen();
            });
        }

        // Setup the language button
        SelectronButton languageButton = findViewById(R.id.buttonLanguage);
        languageButton.setOnClickListener(v -> toggleLanguageListVisibility());

        if (ProductConfig.hasUserProfiles()) {
            homeButton.setVisibility(View.GONE);
            languageButton.setVisibility(View.GONE);
        } else if (!mScreenIsHome || !DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL) || DataMatrix.getBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL)) {
            homeButton.setVisibility(View.VISIBLE);
            languageButton.setVisibility(View.GONE);
        } else {
            homeButton.setVisibility(View.INVISIBLE);
            languageButton.setVisibility(View.VISIBLE);
        }


        mDataMatrix.createParcelableAdapter(TiersKeys.ACTION_UPGRADE_RESULT_PARCELABLE, Bundle.class, bundle -> {
            if (bundle != null) {
                boolean result = bundle.getBoolean("result");
                String message = bundle.getString("message");
                if (result) {
                    mGoScreenModule.applyTiersMap(getWorkoutStateMap());
                }

                if (mBusyStatusDialog != null) {
                    mBusyStatusDialog.setStatusMessage(message);
                    mBusyStatusDialog.changeButtonStateToOkay();
                }
            }
        });

        handleLanguage();

        mDataMatrix.createStringAdapter(DAPIKeys.DATA_USER_DAPI_UUID_STRING, uuid ->
                mGoScreenModule.setCustomWorkoutIsGuest(uuid.equals(DapiUser.GUEST_USER_UUID)), false);
        mDataMatrix.createBoolAdapter(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL, networkConnected ->
                mGoScreenModule.setCustomWorkoutHasNetwork(networkConnected));


        mDataMatrix.createBoolAdapter(DAPIKeys.DATA_EXTENDED_INFO_RECEIVED_BOOL, received -> {
            if (DapiUser.isGuestUser() || received) {
                loadCustomWorkoutsFromJson(DataMatrix.getString(DAPIKeys.DATA_USER_CUSTOM_WORKOUT_JSON_STRING));
            } else {
                mGoScreenModule.setCustomWorkoutLoading(GoScreenActivity.this);
            }
        });

        // Need this callback so we can get custom workout information if it's sent late
        mDataMatrix.createStringAdapter(DAPIKeys.DATA_USER_CUSTOM_WORKOUT_JSON_STRING, this::loadCustomWorkoutsFromJson, false);
    }

    private void showDefaultUpgradeDialog() {
        MessageBoxDialog messageBoxDialog =new MessageBoxDialog(this);
        messageBoxDialog.setTitle(getString(R.string.upgrade_unavailable_tc));
        messageBoxDialog.setMessage(getString(R.string.upgrade_unavailable_pg));
        messageBoxDialog.setCanceledOnTouchOutside(true);
        messageBoxDialog.show();
    }

    private void initiateUpgradeProcess(PremiumFeatureSet set, String workoutID) {
        int languageId = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT);
        LanguageProperty languageProperty = LanguageProperty.getLanguagePropertyFromIndex(languageId);
        String region = languageProperty.locale;
        View root = findViewById(R.id.go_screen_root);

        if (set == null || set.isEmpty()) {
            showDefaultUpgradeDialog();
            return;
        }

        PremiumFeature feature = set.getFeature(workoutID, JHTTypes.FEATURE_TYPE.FEATURE_TYPE_WORKOUT);
        if (feature == null) {
            showDefaultUpgradeDialog();
            return;
        }

        List<FeatureUpgrade> featureUpgrades = set.getUpgrades(feature);
        if (featureUpgrades.size() <= 0) {
            showDefaultUpgradeDialog();
            return;
        }

        if (featureUpgrades.size() == 1 && featureUpgrades.get(0).launchWebView()) {
            String url = featureUpgrades.get(0).url();
            ProjectDialogFactory.webViewDialogBox(root, url);
            return;
        }

        MultiChoiceDialog multiChoiceDialog = new MultiChoiceDialog(root.getContext());
        multiChoiceDialog.addUpgrades(featureUpgrades);
        multiChoiceDialog.setTitle(getResources().getString(R.string.upgrade_tc));


        UpgradeSkin skin = set.getUpgradeSkin();
        if (skin == null) {
            multiChoiceDialog.setCustomBackgroundColor(Color.BLACK);
            multiChoiceDialog.setButton0Text(getResources().getString(R.string.upgrade_tc));
            multiChoiceDialog.setButton1Text(getResources().getString(R.string.cancel_tc));

        } else {
            String title = skin.getTitle(region);
            if (title != null && !title.equals("")) {
                multiChoiceDialog.setTitle(title);
            }
            Bitmap backgroundBitmap = MiscUtilHelper.decodeBackgroundImage(new File(skin.getBackgroundPath()));
            if (backgroundBitmap != null) {
                BitmapDrawable backgroundDrawable = new BitmapDrawable(getResources(), backgroundBitmap);
                multiChoiceDialog.setCustomBackgroundDrawable(backgroundDrawable);
            } else {
                multiChoiceDialog.setCustomBackgroundColor(skin.backgroundColor());
            }

            StateListDrawable stateListDrawableOk = getButtonDrawable(
                    MiscUtilHelper.decodeBackgroundImage(new File(skin.getOkButtonPath())),
                    MiscUtilHelper.decodeBackgroundImage(new File(skin.getOkButtonPressedPath())));
            if (stateListDrawableOk != null) {
                multiChoiceDialog.setButton0BackgroundDrawable(stateListDrawableOk);
                multiChoiceDialog.setButton0Text("");
            } else {
                multiChoiceDialog.setButton0Text(getResources().getString(R.string.upgrade_tc));
            }

            StateListDrawable stateListDrawableCancel = getButtonDrawable(
                    MiscUtilHelper.decodeBackgroundImage(new File(skin.getCancelButtonPath())),
                    MiscUtilHelper.decodeBackgroundImage(new File(skin.getCancelButtonPressedPath())));
            if (stateListDrawableCancel != null) {
                multiChoiceDialog.setButton1BackgroundDrawable(stateListDrawableCancel);
                multiChoiceDialog.setButton1Text("");
            } else {
                multiChoiceDialog.setButton1Text(getResources().getString(R.string.cancel_tc));
            }

        }

        multiChoiceDialog.setButton0OnClickListener(v -> {
            if (multiChoiceDialog.getSelectedChoice() == -1) {
                Log.d(TAG, "trying to upgrade nothing selected");
            } else {
                FeatureUpgrade upgrade = featureUpgrades.get(multiChoiceDialog.getSelectedChoice());

                if (upgrade.launchWebView()) {
                    multiChoiceDialog.hide();
                    ProjectDialogFactory.webViewDialogBox(root, upgrade.url());
                } else {
                    ProjectDialogFactory.confirm(root, getResources().getString(R.string.confirm_upgrade_pg), () -> {
                        multiChoiceDialog.hide();
                        mBusyStatusDialog = new BusyStatusDialog(root.getContext());
                        mBusyStatusDialog.setStatusMessage(getResources().getString(R.string.upgrade_in_progress_pg));
                        mBusyStatusDialog.show(findViewById(R.id.go_screen_root));


                        DataMatrix.putParcelable(TiersKeys.DATA_UPGRADE_PARCELABLE, upgrade);
                    }, null);
                }
            }
        });

        multiChoiceDialog.setButton1OnClickListener(v -> multiChoiceDialog.hide());

        multiChoiceDialog.show(root);
    }

    private StateListDrawable getButtonDrawable(Bitmap unpressedBitmap, Bitmap pressedBitmap) {
        if (unpressedBitmap == null || pressedBitmap == null) {
            return null;
        }
        StateListDrawable stateListDrawable = new StateListDrawable();
        BitmapDrawable unpressedDrawable = new BitmapDrawable(getResources(), unpressedBitmap);
        BitmapDrawable pressedDrawable = new BitmapDrawable(getResources(), pressedBitmap);
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
        stateListDrawable.addState(StateSet.WILD_CARD, unpressedDrawable);
        return stateListDrawable;
    }

    private HashMap<String, Integer> getWorkoutStateMap() {

        @SuppressLint("UseSparseArrays") HashMap<String, Integer> map = new HashMap<>();
        if (TiersKeys.usingTiers()) {
            if (DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL)) {
                PremiumFeatureSet set = DataMatrix.getParcelable(TiersKeys.DATA_GUEST_FEATURE_SET_PARCELABLE, PremiumFeatureSet.class);
                if (set != null) {
                    for (PremiumFeature feature : set.getFeatures()) {
                        logcatFeatureSet (feature);

                        if (feature.getFeatureType() == JHTTypes.FEATURE_TYPE.FEATURE_TYPE_WORKOUT) {
                            try {
                                //TODO: should we handle case where we want to hide a workout in the go screen?
                                if (feature.getFeatureState() == JHTTypes.FEATURE_STATE.FEATURE_DISABLED) {
                                    map.put(feature.id(), 1);
                                } else {
                                    map.put(feature.id(), 0);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "NumberFormatEX", e);
                            }

                        }
                    }
                }
            } else {
                PremiumFeatureSet set = DataMatrix.getParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, PremiumFeatureSet.class);
                if (set != null) {
                    for (PremiumFeature feature : set.getFeatures()) {
                        logcatFeatureSet (feature);

                        if (feature.getFeatureType() == JHTTypes.FEATURE_TYPE.FEATURE_TYPE_WORKOUT) {
                            try {
                                //TODO: should we handle case where we want to hide a workout in the go screen?
                                if (feature.getFeatureState() == JHTTypes.FEATURE_STATE.FEATURE_DISABLED) {
                                    map.put(feature.id(), 1);
                                } else {
                                    map.put(feature.id(), 0);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "NumberFormatEX", e);
                            }

                        }
                    }
                }
            }
        }


        return map;
    }

    private void logcatFeatureSet (PremiumFeature feature) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Feature 2=" + feature.name());
            Log.d(TAG, "Feature 2=" + feature.id());
            Log.d(TAG, "Feature 2=" + feature.getFeatureType());
            Log.d(TAG, "Feature 2=" + feature.getFeatureState());
        }
    }

    private void showUserProfiles() {
        if (userProfileDialog==null) userProfileDialog = new UserProfileDialog(GoScreenActivity.this);
        userProfileDialog.show(mLoginButton);
    }

    private void launchLoginScreen() {
        if(mGoScreenOverlay.getVisibility() == View.GONE) {
            clearSSOCache();
            LaunchAppUtil.launchLoginScreen();
        }
    }

    private void clearSSOCache(){
        //We need to clear the iFit SSO cache and kill the app in case a user logged in as Guest for Matrix
        //but entered credentials for iFit. That info has persisted in the past and caused issues. At this time,
        //there are no other apps that allow the MatrixGuest/3rd Party log in combo without going the Splash Screen
        AppIcon app = AppIconManager.getInstance().getAppIcon(AppIds.IFIT_UUID);
        if (app != null) {
            KillProcessHelper killProcessHelper = new KillProcessHelper();
            killProcessHelper.addPackagesToClear(app.getAppPackage(), true);
            killProcessHelper.kill();
        }
    }

    private void setLoginButton() {
        String userFirstName = mDataMatrix.getString(DAPIKeys.DATA_USER_FIRST_NAME_STRING);
        if (mLoginButton == null) {
            return;
        }
        if (ProductConfig.hasUserProfiles()) {
            // TODO - Need to create profile screen, also need to load last selected user
            mLoginButton.setVisibility(View.VISIBLE);
            mLoginButton.setOnClickListener(v -> showUserProfiles());
            mLoginText.setOnClickListener(v -> showUserProfiles());
            mLoginText.setText(userFirstName);
        }
        else if ((!DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL) && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL))) {
            mLoginButton.setVisibility(View.GONE);
            mLoginText.setVisibility(View.GONE);
        } else {
            if (DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL)) { //NON-NLS
                mLoginButton.setVisibility(View.VISIBLE);
                mLoginText.setVisibility(View.VISIBLE);
                mLoginButton.setOnClickListener(v -> launchLoginScreen());
            } else {
                mLoginButton.setVisibility(View.GONE);
                mLoginText.setText(userFirstName);
                mLoginText.setVisibility(View.VISIBLE);
                mLoginText.setOnClickListener(null);
            }
        }

        if (isAllLoginTypeDisabled()) {
            mLoginButton.setVisibility(View.GONE);
            mLoginText.setVisibility(View.GONE);
        }
    }

    public static boolean startGoButtonWorkout() {
        Log.d(TAG, "Starting quick start workout");
        // We know the initial conditions for the go button workout, so we don't need to worry about the callback here
        WorkoutManager workoutManager = new WorkoutManager(new ChimeraWorkoutMap());
        workoutManager.loadQuickStartWorkout(null);
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


    private void handleLanguage() {
        // TODO: This recycler view should follow the other containers.
        RecyclerView mLanguageRecyclerView = findViewById(R.id.language_recycler_view);

        int languageId = DataMatrix.getInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT);

        DefaultItemAdapter defaultItemAdapter = new DefaultItemAdapter(LanguageProperty.getSpinnerList(), languageId, position -> {
            LanguageProperty lp = LanguageProperty.getLanguagePropertyFromIndex(position);
            mGoScreenModule.cleanGoScreenModule(this);
            LocaleUtil.changeLanguage(GoScreenActivity.this, lp, false);
            toggleLanguageListVisibility();
        });

        mLanguageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mLanguageRecyclerView.setAdapter(defaultItemAdapter);

        mLanguageBackground.setOnClickListener(v -> toggleLanguageListVisibility());
    }


    private void toggleLanguageListVisibility() {
        if (mLanguageBackground.getVisibility() != View.VISIBLE) {
            mLanguageBackground.animate().alpha(1.0f).setDuration(150);
            mLanguageBackground.setVisibility(View.VISIBLE);
        } else {
            mLanguageBackground.animate().alpha(0.0f).setDuration(150);
            mLanguageBackground.setVisibility(View.GONE);
        }
    }

    public boolean isAllLoginTypeDisabled() {
        return !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL)
                && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_BARCODE_LOGIN_BOOL)
                && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL)
                && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_EMAIL_LOGIN_BOOL)
                && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_FITLINXX_LOGIN_BOOL)
                && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CUSTOM_LOGIN_BOOL)
                && !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_CUSTOM_RFID_ONLY_BOOL);
    }

    // begin show welcome screen slideshow if welcome screen is disabled stuff
    // there is a timer in the onCreate method too!
    // (unashamedly poached parts from WelcomeActivity thanks Alex)

    // resets the timer on user activity
    @Override
    public void onUserInteraction(){
        if(slideShowActive && mScreenIsHome) {
            welcomeDisabledBackgroundTimer.reset();
        }
        super.onUserInteraction();
    }

    // runnable to show the hand touching the screen icon and dim brightness
    private boolean touchIconIsShowing = false;
    private final Runnable hideOrShowHandIcon = new Runnable() {
        @Override
        public void run() {
            if(touchIcon.getVisibility() == View.VISIBLE){
                fadeOutAnimation.setDuration(1000);
                touchIcon.startAnimation(fadeOutAnimation);
                touchIcon.setVisibility(View.GONE);
                touchIconIsShowing = false;
                mHandler.postDelayed(this, 60000);// hidden for 60 seconds
            }else {
                fadeInAnimation.setDuration(1000);
                touchIcon.startAnimation(fadeInAnimation);
                touchIcon.setVisibility(View.VISIBLE);
                touchIconIsShowing = true;
                mHandler.postDelayed(this, 10000);// shown for 10 seconds
            }
        }
    };

    // runnable to dim the screen
    private final Runnable screenDimRunnable = () -> SystemBrightness.setSystemBrightness(getContentResolver(),
            DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_PERCENT_AFTER_TIMEOUT_INT));

    private void fadeInSlideshow() {
        if(backgroundManager != null){
            backgroundManager.enableDefaultWallpapers(true);
        }
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.SCREENSAVER_SCREEN.name());
        // fade in background
        fadeInAnimation.setDuration(500);
        background.startAnimation(fadeInAnimation);
        background2.startAnimation(fadeInAnimation);
        background.setVisibility(View.VISIBLE);
        background2.setVisibility(View.VISIBLE);
        // fade in hand icon to prompt user to touch the screen
        fadeInAnimation.setDuration(1000);
        touchIcon.startAnimation(fadeInAnimation);
        touchIcon.setVisibility(View.VISIBLE);
        touchIconIsShowing = true;
        // remove previous runnable to show/hide hand icon and start a new one
        mHandler.removeCallbacks(hideOrShowHandIcon);
        mHandler.postDelayed(hideOrShowHandIcon, 10000);

        // stuff to get the screen dim timeout correct
        int position = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_TIMEOUT_ENUM_INT);
        int timeUntilScreenDim = 30;
        if (position == SettingKeys.AssetManagementConstants.SCREEN_BRIGHTNESS_AFTER_TIMEOUT_60_MIN) {
            timeUntilScreenDim = 60;
        } else if (position == SettingKeys.AssetManagementConstants.SCREEN_BRIGHTNESS_AFTER_TIMEOUT_90_MIN) {
            timeUntilScreenDim = 90;
        } else if (position == SettingKeys.AssetManagementConstants.SCREEN_BRIGHTNESS_AFTER_TIMEOUT_120_MIN) {
            timeUntilScreenDim = 120;
        } else if (position == SettingKeys.AssetManagementConstants.SCREEN_BRIGHTNESS_AFTER_TIMEOUT_0_MIN) {
            timeUntilScreenDim = 0;
        }
        // remove previous runnable to dim the screen and start a new one
        mHandler.removeCallbacks(screenDimRunnable);
        mHandler.postDelayed(screenDimRunnable, timeUntilScreenDim * 60000);
        // set a listener for clicking the newly faded in background to trigger a fade out
        background.setOnClickListener( v -> fadeOutSlideshow());
        background2.setOnClickListener( v -> fadeOutSlideshow());
    }

    private void fadeOutSlideshow() {
        if (background.getVisibility() == View.VISIBLE) {
            // Notify transitioning out of the screensaver to the go screen.
            DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.GO_SCREEN.name());
            mHandler.removeCallbacks(hideOrShowHandIcon);
            touchIcon.setVisibility(View.GONE);
            touchIconIsShowing = false;
            fadeOutAnimation.setDuration(500);
            background.startAnimation(fadeOutAnimation);
            background2.startAnimation(fadeOutAnimation);
            background.setVisibility(View.INVISIBLE);
            background2.setVisibility(View.INVISIBLE);
            // set brightness back to original value
            mHandler.removeCallbacks(screenDimRunnable);
            SystemBrightness.setSystemBrightness(getContentResolver(), DataMatrix.getInt(SettingKeys.Hardware.DATA_BACKLIGHT_PROGRESS_INT));
            // reset the timer for fading in the welcome screen timeout background again
            welcomeDisabledBackgroundTimer.reset();
            if(backgroundManager != null){
                backgroundManager.enableDefaultWallpapers(false);
            }
        }
    }
    // end show welcome screen slideshow if welcome screen is disabled stuff

    // safety pin stuff!
    private void checkIfSafetyPinNeeded(DigitalSafetyPinDialog.Action action) {
        if (DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL)) {
            if (safetyPrompt == null) safetyPrompt = new DigitalSafetyPinDialog(getApplicationContext());
            safetyPrompt.checkIfNeeded(action);
        } else {
            action.doAction();
        }
    }

}
