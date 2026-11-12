package com.jht.chimera.ui.mainapp;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.jht.chimera.lib.widget.activity.RestringActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.SystemBrightness;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.ClubComKeys;
import com.jht.chimera.lib.common.datamatrixkeys.ConsoleConfigKeys;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.widget.dialog.ProductDashboardHost;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.common.i18n.LocaleUtil;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.lib.common.testing.TestingUtils;
import com.jht.chimera.lib.common.timer.HomeScreenIdleTimer;
import com.jht.chimera.lib.login.RFIDLogin;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.lib.widget.button.HomeButton;
import com.jht.chimera.lib.widget.dialog.MessageBoxDialog;
import com.jht.chimera.lib.widget.listview.DefaultItemAdapter;
import com.jht.chimera.lib.widget.menu.RightMenuBaseLayout;
import com.jht.chimera.lib.widget.selectron.SelectronButton;
import com.jht.chimera.lib.integrations.xarefit.XareFitLib;
import com.jht.chimera.service.ui.panels.DigitalSafetyPinDialog;
import com.jht.chimera.ui.mainapp.createaccount.fragment.LoginFragment;
import com.jht.chimera.ui.mainapp.createaccount.fragment.RegisterFragment;
import com.jht.chimera.ui.mainapp.util.BackgroundManager;
import com.jht.clubcom.interfaces.ClubComOptions;
import com.jht.clubcom.interfaces.ClubComUrl;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.rfid.Legic.RFIDChip;
import com.jhtp.dapiservice.an.types.ButtonPressType;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

public class WelcomeActivity extends RestringActivity {
    @NonNls
    private static final String TAG = "WelcomeActivity";

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private DataMatrix mDataMatrix;

    private ConstraintLayout mWelcomeOverlay;
    private Group guestGroup;
    private Group loginGroup;
    private Group registerGroup;
    private Group touchToContinueGroup;

    private View welcomeBackgroundBar;

    private HomeButton mHomeButton;
    private ConstraintLayout mLanguageBackground;
    private SelectronButton mLanguageButton;
    private BackgroundManager backgroundManager;

    private Button loginTopButton;
    private TextView loginTopText;

    private ConstraintLayout mFragmentContainerLayout;
    private View mOverlayView;

    private LoginFragment mLoginFragment;
    private RegisterFragment mRegisterFragment;
    private Fragment currentFragment;

    private Animation fadeInAnimation;
    private Animation fadeOutAnimation;

    private MessageBoxDialog mMessageBoxDialog;
    private JHTTimer welcomeOverlayTimer;
    private JHTTimer welcomeVideoTimer;
    private final Handler mHandler = new Handler();
    private HomeScreenIdleTimer homeScreenIdleTimer;

    //Custom Video
    private File[] videoFiles;
    private int videoIndex;
    private SurfaceView mVideoSurfaceView;
    private MediaPlayer mediaPlayer;

    private ImageView touchIcon;
    private AppCompatImageView loginImage;
    private AppCompatImageView xareFitLoginImage;

    private boolean disableLaunchWorkout = false;
    private boolean goButtonAlreadyClicked = false;

    private WebView clubComWebView;

    private RightMenuBaseLayout mSettingsMenu; // WelcomeScreenSettingsMenu not in all flavors, try to use common base class?

    private boolean xarefitEnabled = false;
    private boolean qrCodeShown = false;

    // Lib to handle XareFit functionality
    private XareFitLib mXareFitLib = XareFitLib.getInstance();

    private RFIDLogin rfidLogin;

    //security/safety pin
    DigitalSafetyPinDialog safetyPrompt;

    @SuppressLint({"ClickableViewAccessibility", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        CommonRoutines.initWebActivity(this, R.layout.welcome_activity, TAG);

        homeScreenIdleTimer = new HomeScreenIdleTimer(this, true);
        touchIcon = findViewById(R.id.welcome_touch_icon);
        mWelcomeOverlay = findViewById(R.id.welcome_overlay);
        ImageView guestButton = findViewById(R.id.welcome_guest_button);
        ImageView loginButton = findViewById(R.id.welcome_login_button);
        ImageView registerButton = findViewById(R.id.welcome_register_button);
        loginImage = findViewById(R.id.welcome_login_icon);
        xareFitLoginImage = findViewById(R.id.welcome_login_xarefit_icon);

        ImageView guestInfoIcon = findViewById(R.id.welcome_guest_icon);
        guestInfoIcon.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        guestInfoIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_guest));
        ImageView guestInfoButton = findViewById(R.id.welcome_guest_info_button);
        ImageView loginInfoButton = findViewById(R.id.welcome_login_info_button);
        ImageView registerInfoButton = findViewById(R.id.welcome_register_info_button);
        ImageView touchToContinueButton = findViewById(R.id.welcome_touch_to_continue_button);
        mLanguageButton = findViewById(R.id.buttonSelectLanguage);
        mLanguageButton.setOnClickListener(v -> {
            mLanguageButton.setIconHeight(40);
            mLanguageButton.setIconDrawable(VectorDrawableCompat.create(getResources(), R.drawable.button_tv_close_white, null));
            toggleLanguageListVisibility();
        });

        mHomeButton = findViewById(R.id.buttonHome);
        mHomeButton.setOnClickListener(v -> {
            //Stores home button press events for Dapi Analytics
            DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.HOME_BTN_PRESS.name);
            showWelcome();
        });

        welcomeBackgroundBar = findViewById(R.id.welcome_background_bar);

        guestGroup = findViewById(R.id.welcome_guest_group);
        loginGroup = findViewById(R.id.welcome_login_group);
        registerGroup = findViewById(R.id.welcome_register_group);
        touchToContinueGroup = findViewById(R.id.welcome_touch_to_continue_group);

        mFragmentContainerLayout = findViewById(R.id.fragment_container_layout);
        mOverlayView = findViewById(R.id.login_overlay_view);
        mVideoSurfaceView = findViewById(R.id.welcome_video_surface_view);


        mVideoSurfaceView.setOnTouchListener((v, event) -> {
            Log.e(TAG, "in my touch listener");
            return true;
        });

        loginTopButton = findViewById(R.id.buttonLogin);
        loginTopButton.setOnClickListener((e) -> showLoginFragment());
        loginTopText = findViewById(R.id.login_name_text);

        // Initialize language buttons
        handleLanguage();

        guestButton.setOnClickListener(v -> {
            Log.d(TAG, "about to launch go screen. Thread: " + Thread.currentThread().getName());
            //Launch guest activity
            if (!disableLaunchWorkout) {
                disableLaunchWorkout = true;
                LaunchAppUtil.launchGoScreen();
            }
        });

        touchToContinueButton.setOnClickListener(v -> {
            //Launch guest activity
            DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, true);
            LaunchAppUtil.launchGoScreen();
        });

        loginButton.setOnClickListener(v -> {
            if(xarefitEnabled) {
                showQRCode();
            }
            else {
                showLoginFragment();
            }
        });
        registerButton.setOnClickListener(v -> showRegisterFragment());

        guestInfoButton.setOnClickListener(v -> showInfoDialog(getResources().getString(R.string.guest_tc), getResources().getString(R.string.guest_help_pg), true));
        // These are handy hacks to test out code.  I put them in the guest info button to simulate the events.
        // DataMatrix.putParcelable(HALKeys.EVENT_PF_BARCODE_DETECTED_PARCELABLE, new PFBarcodeData(new RFIDChip(RfStandard.Iso14443A, new byte[] {0x04, (byte)0xbb, (byte)0xf8, (byte)0xb2, (byte)0xba, (byte)0x5d, (byte)0x80}), "TEST0001112333"));
        // DataMatrix.putInt(HALKeys.DATA_LOG_ERROR_CODE_INT, 0x01A8);

        loginInfoButton.setOnClickListener(v -> showInfoDialog(getResources().getString(R.string.login_tc), getResources().getString(R.string.user_login_help_pg), true));
        registerInfoButton.setOnClickListener(v -> showInfoDialog(getResources().getString(R.string.register_tc), getResources().getString(R.string.register_help_pg), true));

        fadeInAnimation = new AlphaAnimation(0, 1);
        fadeOutAnimation = new AlphaAnimation(1, 0);
        mMessageBoxDialog = new MessageBoxDialog(this);
        welcomeOverlayTimer = new JHTTimer(() -> mHandler.post(this::fadeOutOverlay));
        welcomeVideoTimer = new JHTTimer(() -> mHandler.post(this::startVideo));

        setTopBarButton(true);
        setIconPositions();

        clubComWebView = findViewById(R.id.clubComWebView);
        clubComWebView.getSettings().setJavaScriptEnabled(true);
        clubComWebView.setBackgroundColor(Color.BLACK);
        clubComWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                view.loadUrl("about:blank");
            }
        });

        if (ProductConfig.isLightCommercial()) {
            mSettingsMenu = findViewById(R.id.settings_menu);
            findViewById(R.id.buttonSettings).setOnClickListener(v -> mSettingsMenu.setVisibility(View.VISIBLE));
        }
    }

    private void showInfoDialog(String titleText, String messageText, Boolean closeOnTouch) {
        mMessageBoxDialog.setTitle(titleText);
        mMessageBoxDialog.setMessage(messageText);
        mMessageBoxDialog.setCanceledOnTouchOutside(closeOnTouch);
        mMessageBoxDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        mDataMatrix = new DataMatrix();

        DataMatrix.putBoolean(UiServiceKeys.ACTION_DISABLE_SAFETY_SCREENS_BOOL, false);

        DataMatrix.putBoolean(HALKeys.DATA_HOMEKEY_ENABLE_BOOL, true);

        DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Default.ordinal());
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.WELCOME_SCREEN.name());

        disableLaunchWorkout = false;
        checkCustomVideo();

        homeScreenIdleTimer.start();
        backgroundManager = new BackgroundManager(findViewById(R.id.welcome_background), findViewById(R.id.welcome_background2));
        mDataMatrix.createEventAdapter(ConsoleConfigKeys.EVENT_CONSOLE_THEME_READY, s -> {
            if (backgroundManager != null)
                backgroundManager.loadCustomImages();
        });

        Log.d(TAG, "io version " + DataMatrix.getString(HALKeys.DATA_VERSION_IO_STRING));

        mDataMatrix.createEventAdapter(HALKeys.EVENT_SCREEN_TOUCH, event -> resetTimers());

        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, value -> {
            Log.e(TAG, "keypress listener. Thread: " + Thread.currentThread().getName());
            resetTimers();
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                stopVideo();
                return;
            }

            KeyPress.KeyCode keyCode = KeyPress.getKeyCode(value);
            KeyPress.KeyState keyState = KeyPress.getKeyState(value);

            if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHYSICAL_GO_BTN_BOOL)) {
                if (keyCode == KeyPress.KeyCode.KEY_GO && keyState == KeyPress.KeyState.KEY_DOWN) {
                    if (!disableLaunchWorkout) {
                        if (!goButtonAlreadyClicked) {
                            checkIfSafetyPinNeeded(()-> goButtonAlreadyClicked = GoScreenActivity.startGoButtonWorkout());
                        } else {
                            Log.d(TAG, "cannot start go workout from welcome activity");
                        }
                    } else {
                        Log.d(TAG, "onResume, disableLaunchWorkout true, can't start");
                    }
                }
            }
        });

        mLoginFragment = new LoginFragment();
        mRegisterFragment = new RegisterFragment();

        if (!DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL)) {
            mDataMatrix.createBoolAdapter(DAPIKeys.DATA_DAPI_CONNECTION_LOST_BOOL, (connectionLost) -> setIconPositions(), false);
        }

        Log.d(TAG, "Welcome Activity Loaded");

        mDataMatrix.createParcelableAdapter(ClubComKeys.DATA_CLUB_COM_OPTIONS_PARCELABLE, ClubComOptions.class, this::configureClubCom);

        mDataMatrix.createEventAdapter(MainAppKeys.ACTION_XARE_FIT_HIDE_QR_CODE, event -> hideQRCode(), false);

        mDataMatrix.createIntAdapter(HALKeys.DATA_SAFETY_KEY_STATUS_INT, safetyKeyStatus ->{
            //Prevent workout starting edge case where splash screen is launching while workout is technically safe to
            // start
            if(safetyKeyStatus > 0) {
                mDataMatrix.finish();
            }
        });

        initRFIDLogin();

        DataMatrix.sendEvent(LoggerKeys.ACTION_RECORD_BOOT_TIME);

        goButtonAlreadyClicked = false;
    }

    private void initRFIDLogin() {
        rfidLogin = new RFIDLogin(mDataMatrix, new RFIDLogin.ILoginListener() {
            @Override
            public void onLoginAttemptStart() {
                showInfoDialog(null, getString(R.string.rid_login_msg_pg), false);
                setGoButtonEnabled(false);
            }

            @Override
            public void onLoginAttemptEnd(RFIDLogin.Result result, RFIDChip chip) {
                switch (result) {
                    case SUCCESS:
                        mMessageBoxDialog.closeMessageBox();
                        LaunchAppUtil.launchGoScreen();
                        break;
                    case INVALID_CREDENTIALS:
                        showInfoDialog(null, getResources().getString(R.string.login_failed_message_pg), true);
                        mMessageBoxDialog.setOnDismissListener(dialog -> rfidLogin.start());
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
                            showLoginFragment();
                            rfidLogin.start();
                        });
                        break;
                    case CONNECTION_ERROR:
                        showInfoDialog(getResources().getString(R.string.rfid_pairing_failed_tc), getResources().getString(R.string.rfid_pairing_failed_msg_pg), true);
                        mMessageBoxDialog.setOnCloseCallback(() -> rfidLogin.start());
                        mMessageBoxDialog.setOnDismissListener((e) -> rfidLogin.start());
                        break;
                    default:
                        mMessageBoxDialog.dismiss();
                        break;
                }
                setGoButtonEnabled(true);
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

    // Enables & disables the physical go button (quick workout) on frames that have it.
    private void setGoButtonEnabled(boolean enabled){
        if (enabled){
            if (currentFragment == mRegisterFragment) {
                mRegisterFragment.enableGoButton(true);
            }
            else if (currentFragment == mLoginFragment) {
                mLoginFragment.enableGoButton(true);
            }
            else disableLaunchWorkout = false;
        }
        else {
            mRegisterFragment.enableGoButton(false);
            mLoginFragment.enableGoButton(false);
            disableLaunchWorkout = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        disableLaunchWorkout = true;
        homeScreenIdleTimer.stop();

        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mDataMatrix.finish();
        stopTimers();
        if (safetyPrompt != null) safetyPrompt.dismiss();
        if (backgroundManager != null) {
            backgroundManager.stopTimer();
            backgroundManager = null;
        }

        // Tell ClubCom we are hiding it if feature is enabled
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CLUB_COM_BOOL)) {
            clubComWebView.loadUrl("javascript:onHidden()");
        }

        // The fragment must be removed or it'll save the UI state and show the fragment on top, even though the splash screen kills the app
        if (currentFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .remove(currentFragment).commit();
            currentFragment = null;
        }

        mXareFitLib = null;

        if(mMessageBoxDialog != null && mMessageBoxDialog.isShowing()){
            mMessageBoxDialog.dismiss();
        }

        if (rfidLogin != null) {
            rfidLogin.stop();
        }
        goButtonAlreadyClicked = true;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT) == SettingKeys.AssetManagementConstants.WELCOME_ICON_POSITION_CENTER ||
                    DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT) == SettingKeys.AssetManagementConstants.WELCOME_ICON_POSITION_TOP ||
                    DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT) == SettingKeys.AssetManagementConstants.WELCOME_ICON_POSITION_GONE
            ) {
                resetTimers();
            }

            // stop the timer in case the user just doesn't lift up finger and doesn't move
            stopTimers();
        }


        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (mVideoSurfaceView.getVisibility() == View.VISIBLE) {
            return true;
        }

        // this is needed for the top bar
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            if (DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT) == SettingKeys.AssetManagementConstants.WELCOME_ICON_POSITION_CENTER ||
                    DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT) == SettingKeys.AssetManagementConstants.WELCOME_ICON_POSITION_TOP ||
                    isClubComVisible() || DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT) == SettingKeys.AssetManagementConstants.WELCOME_ICON_POSITION_GONE) {
                resetTimers();
                // add a small delay so that the button are not clickable right away
                mHandler.postDelayed(this::fadeInOverlay, 250);
            }

            // stop the timer in case the user just doesn't lift up finger and doesn't move
            stopTimers();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            resetTimers();
        }
        return super.dispatchTouchEvent(event);
    }

    private void handleLanguage() {
        RecyclerView mLanguageRecyclerView = findViewById(R.id.language_recycler_view);
        mLanguageBackground = findViewById(R.id.language_background);

        int languageId = DataMatrix.getInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT);

        DefaultItemAdapter defaultItemAdapter = new DefaultItemAdapter(LanguageProperty.getSpinnerList(), languageId, position -> {
            LanguageProperty lp = LanguageProperty.getLanguagePropertyFromIndex(position);
            LocaleUtil.changeLanguage(WelcomeActivity.this, lp, false);
            toggleLanguageListVisibility();
        });

        mLanguageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mLanguageRecyclerView.setAdapter(defaultItemAdapter);

        mLanguageBackground.setOnClickListener(v -> toggleLanguageListVisibility());
    }

    private void toggleLanguageListVisibility() {
        if (mLanguageBackground.getVisibility() != View.VISIBLE) {
            fadeInAnimation.setDuration(150);
            mLanguageBackground.startAnimation(fadeInAnimation);
            mLanguageBackground.setVisibility(View.VISIBLE);
            stopTimers();
        } else {
            mLanguageButton.setIconDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_icon_globe, null));
            fadeOutAnimation.setDuration(150);
            mLanguageBackground.startAnimation(fadeOutAnimation);
            mLanguageBackground.setVisibility(View.GONE);
            resetTimers();
        }
    }

    private final Runnable screenDimRunnable = () -> SystemBrightness.setSystemBrightness(getContentResolver(), DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_PERCENT_AFTER_TIMEOUT_INT));

    private final Runnable hideOrShowHandIcon = new Runnable() {
        @Override
        public void run() {
            if (touchIcon.getVisibility() == View.VISIBLE) {
                fadeOutAnimation.setDuration(1000);
                touchIcon.startAnimation(fadeOutAnimation);
                touchIcon.setVisibility(View.GONE);
                mHandler.postDelayed(this, 60000);
            } else {
                fadeInAnimation.setDuration(1000);
                touchIcon.startAnimation(fadeInAnimation);
                touchIcon.setVisibility(View.VISIBLE);
                mHandler.postDelayed(this, 10000);
            }

        }
    };

    private void fadeOutOverlay() {
        // Show Screensaver UI
        if (mWelcomeOverlay.getVisibility() == View.VISIBLE) {
//            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 50);
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
            DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.SCREENSAVER_SCREEN.name());

            mHandler.removeCallbacks(screenDimRunnable);
            mHandler.postDelayed(screenDimRunnable, timeUntilScreenDim * 60000);
            if (isClubComVisible()) {
                // start club com session
                DataMatrix.sendEvent(ClubComKeys.ACTION_CLUB_COM_START_SESSION);
            } else {
                fadeInAnimation.setDuration(1000);
                touchIcon.startAnimation(fadeInAnimation);
                touchIcon.setVisibility(View.VISIBLE);
                mHandler.removeCallbacks(hideOrShowHandIcon);
                mHandler.postDelayed(hideOrShowHandIcon, 10000);
            }
            if (backgroundManager != null) {
                backgroundManager.enableDefaultWallpapers(true);
            }
            fadeOutAnimation.setDuration(500);
            mWelcomeOverlay.startAnimation(fadeOutAnimation);
            mWelcomeOverlay.setVisibility(View.GONE);
            mHandler.postDelayed(this::hideQRCode, 550);
        }
    }

    private void startVideo() {
        Log.d(TAG, "startVideo");
        if (videoFiles != null && videoFiles.length > 0) {
            startCustomVideo();
        }
    }

    private void fadeInOverlay() {
        // Hide Screensaver UI
        mHandler.post(() -> {
            if (mWelcomeOverlay.getVisibility() != View.VISIBLE) {
                mHandler.removeCallbacks(hideOrShowHandIcon);
                mHandler.removeCallbacks(screenDimRunnable);
                DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.WELCOME_SCREEN.name());
                SystemBrightness.setSystemBrightness(getContentResolver(), DataMatrix.getInt(SettingKeys.Hardware.DATA_BACKLIGHT_PROGRESS_INT));
                touchIcon.setVisibility(View.GONE);
                fadeInAnimation.setDuration(500);
                mWelcomeOverlay.startAnimation(fadeInAnimation);
                mWelcomeOverlay.setVisibility(View.VISIBLE);
                if (isClubComVisible()) {
                    // end club com session
                    DataMatrix.sendEvent(ClubComKeys.ACTION_CLUB_COM_END_SESSION);
                }
                if (backgroundManager != null) {
                    backgroundManager.enableDefaultWallpapers(false);
                }
            }
        });
    }

    private void stopTimers() {
        welcomeOverlayTimer.stop();
        welcomeVideoTimer.stop();
        stopVideo();
    }

    private void resetTimers() {

        if (currentFragment instanceof LoginFragment || currentFragment instanceof RegisterFragment) {
            return;
        }
        stopTimers();

        if (DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT) == SettingKeys.AssetManagementConstants.WELCOME_ICON_POSITION_CENTER ||
                DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT) == SettingKeys.AssetManagementConstants.WELCOME_ICON_POSITION_TOP ||
                isClubComVisible() || DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT) == SettingKeys.AssetManagementConstants.WELCOME_ICON_POSITION_GONE) {
            fadeInOverlay();
            int timeout = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT) * 1000;
            if (isClubComVisible()) timeout = 5000; // force 5 second fade if clubcom is visible
            if (timeout > 0) {
                if (TestingUtils.isAutomationEnabled(getApplicationContext())) {
                    Log.d(TestingUtils.TAG, "Preventing the welcomeOverlayTimer because automation mode is enabled");
                } else {
                    welcomeOverlayTimer.start(timeout);
                }
            }
        }

        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_WELCOME_SCREEN_BOOL)) {
            int timeout = DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT) * 1000;
            if (timeout > 0) {
                if (TestingUtils.isAutomationEnabled(getApplicationContext())) {
                    Log.d(TestingUtils.TAG, "Preventing the welcomeVideoTimer because automation mode is enabled");
                } else {
                    welcomeVideoTimer.start(timeout);
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setIconPositions() {
        int position = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT);
        if (ProductConfig.isLightCommercial() && position == 1) {
            position++;
        }

        if (!DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL) && DataMatrix.getBoolean(DAPIKeys.DATA_DAPI_CONNECTION_LOST_BOOL)) {
            showTouchToContinue();
            return;
        }

        // If XareFit is enabled, show the XareFit login button, else show normal login button
        xarefitEnabled = DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XAREFIT_LOGIN_BOOL);
        if (xarefitEnabled) {
            qrCodeShown = false;
            touchToContinueGroup.setVisibility(View.GONE);
            loginGroup.setVisibility(View.VISIBLE);
            loginImage.setVisibility(View.GONE);
            xareFitLoginImage.setVisibility(View.VISIBLE);
            registerGroup.setVisibility(View.GONE);
            if (!DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL)) {
                guestGroup.setVisibility(View.GONE);
            } else {
                guestGroup.setVisibility(View.VISIBLE);
            }
            loginTopButton.setVisibility(View.GONE);
            loginTopText.setVisibility(View.GONE);
            welcomeBackgroundBar.setVisibility(View.VISIBLE);
            return;
        }
        loginImage.setVisibility(View.GONE);
        xareFitLoginImage.setVisibility(View.GONE);

        switch (position) {
            case SettingKeys.AssetManagementConstants.WELCOME_ICON_POSITION_GONE:
                touchToContinueGroup.setVisibility(View.GONE);
                guestGroup.setVisibility(View.GONE);
                loginGroup.setVisibility(View.GONE);
                registerGroup.setVisibility(View.GONE);
                welcomeBackgroundBar.setVisibility(View.GONE);
                loginTopButton.setVisibility(View.GONE);
                loginTopText.setVisibility(View.GONE);
                resetTimers();
                break;
            case SettingKeys.AssetManagementConstants.WELCOME_ICON_POSITION_TOP:

                mFragmentContainerLayout.setOnTouchListener((v, event) -> {

                    if (mLanguageBackground.getVisibility() != View.VISIBLE) {
                        resetTimers();
                        stopTimers();
                        showLoginFragment();
                    }

                    return false;
                });

                if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL) || DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL)) {
                    touchToContinueGroup.setVisibility(View.GONE);
                    guestGroup.setVisibility(View.GONE);
                    loginGroup.setVisibility(View.GONE);
                    registerGroup.setVisibility(View.GONE);
                    welcomeBackgroundBar.setVisibility(View.GONE);
                    //Show icon login topbar
                    loginTopButton.setVisibility(View.VISIBLE);
                    loginTopText.setVisibility(View.VISIBLE);
                    resetTimers();
                } else {
                    showTouchToContinue();
                }
                break;
            case SettingKeys.AssetManagementConstants.WELCOME_ICON_POSITION_CENTER:
                if (!ProductConfig.isLightCommercial() && (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL) || DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL))) {
                    touchToContinueGroup.setVisibility(View.GONE);
                    if (!DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL)) {
                        guestGroup.setVisibility(View.GONE);
                    } else {
                        guestGroup.setVisibility(View.VISIBLE);
                    }
                    loginGroup.setVisibility(View.VISIBLE);
                    loginImage.setVisibility(View.VISIBLE);
                    registerGroup.setVisibility(View.VISIBLE);
                    welcomeBackgroundBar.setVisibility(View.VISIBLE);
                    loginTopButton.setVisibility(View.GONE);
                    loginTopText.setVisibility(View.GONE);
                    resetTimers();
                } else {
                    showTouchToContinue();
                    if (ProductConfig.isLightCommercial()) {
                        resetTimers();
                    }
                }
                break;
        }

        if (!DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL) || !DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_REGISTRATION_BOOL)) {
            registerGroup.setVisibility(View.GONE);
        }

        if (!ProductConfig.isLightCommercial() && SettingKeys.AssetManagement.isAllLoginTypeDisabled()) {
            showTouchToContinue();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showTouchToContinue(){
        touchToContinueGroup.setVisibility(View.VISIBLE);
        welcomeBackgroundBar.setVisibility(View.VISIBLE);
        guestGroup.setVisibility(View.GONE);
        loginGroup.setVisibility(View.GONE);
        loginImage.setVisibility(View.GONE);
        registerGroup.setVisibility(View.GONE);
        loginTopText.setVisibility(View.GONE);
        loginTopButton.setVisibility(View.GONE);
        mFragmentContainerLayout.setOnTouchListener((v, event)-> false);
    }


    public void showLoginFragment() {
//        loginTopButton.animate().alpha(0f).setDuration(1000).setListener(new AnimatorListenerAdapter() {
//
//            @Override
//            public void onAnimationEnd(Animator animator) {
//                loginTopButton.setVisibility(View.GONE);
//            }
//        });
        loginTopButton.setVisibility(View.GONE);
        loginTopText.setVisibility(View.GONE);
        homeScreenIdleTimer.stop();
        replaceFragment(mLoginFragment);
        stopTimers();
        currentFragment = mLoginFragment;

        disableLaunchWorkout = true;
        setTopBarButton(false);
        if (mLanguageBackground.getVisibility() == View.VISIBLE) toggleLanguageListVisibility();
    }

    public void showQRCode() {
        if (mXareFitLib != null && !qrCodeShown) {
            qrCodeShown = true;
            Bitmap bm = mXareFitLib.getLoginQRCode(this);
            xareFitLoginImage.setImageBitmap(bm);

            // Start the user checkin loop
            mXareFitLib.handleUserCheckIn();
        }
    }

    private void hideQRCode() {
        mHandler.post(() -> {
            if(qrCodeShown) {
                qrCodeShown = false;
                xareFitLoginImage.setImageResource(R.drawable.xarefit_icon);
            }
        });
    }

    public void showRegisterFragment() {
        homeScreenIdleTimer.stop();
        disableLaunchWorkout = true;
        replaceFragment(mRegisterFragment);
        stopTimers();
        currentFragment = mRegisterFragment;
        setTopBarButton(false);
    }


    /**
     * Sets whether the overlay is visible and blocking touch inputs
     *
     * @param visible whether it should be visible or not
     */
    public void setOverlayVisibility(final boolean visible) {
        mHandler.post(() -> {
            if (visible) {
                mOverlayView.setVisibility(View.VISIBLE);
                DataMatrix.sendEvent(TopBarKeys.ACTION_SHOW_BUSY_INDICATOR);
            } else {
                mOverlayView.setVisibility(View.GONE);
                DataMatrix.sendEvent(TopBarKeys.ACTION_HIDE_BUSY_INDICATOR);
            }
        });
    }

    public void showWelcome() {
        disableLaunchWorkout = false;
        runOnUiThread(() -> {
            setIconPositions();

            if (currentFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                        .remove(currentFragment).commit();
                currentFragment = null;
                setTopBarButton(true);
                homeScreenIdleTimer.resetLanguage();
                homeScreenIdleTimer.start();
                resetTimers();
            }
        });

        //Test if this is not needed
        //DataMatrix.sendEvent(UiServiceKeys.EVENT_HIDE_KEYBOARD);
    }

    private void setTopBarButton(boolean language) {
        if (language) {
            mLanguageButton.setVisibility(View.VISIBLE);
            mHomeButton.setVisibility(View.GONE);
        } else {
            mHomeButton.setVisibility(View.VISIBLE);
            mLanguageButton.setVisibility(View.GONE);
        }
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(mFragmentContainerLayout.getId(), fragment).commit();
    }

    private void checkCustomVideo() {
        File videoFolder = new File(getFilesDir(), "/settings/theme/welcome_video");
        if (videoFolder.exists()) {
            Log.d(TAG, "video folder exists");
            videoFiles = videoFolder.listFiles();

            if (videoFiles == null) {
                Log.d(TAG, "video files are null");
            } else {
                Log.d(TAG, "video files " + videoFiles.length);
            }

            videoFiles = videoFolder.listFiles(new FileFilter() {
                private boolean found = false;

                @Override
                public boolean accept(File pathname) {

                    Log.d(TAG, "video file " + pathname);
                    if (!found) {
                        found = pathname.getName().matches(".*\\.(mp4|mov)"); //NON-NLS
                    }
                    return found;
                }
            });
            if (videoFiles == null) {
                Log.d(TAG, "video files are null");
            } else {
                Log.d(TAG, "video files " + videoFiles.length);
            }

            mediaPlayer = new MediaPlayer();
            mVideoSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mediaPlayer.setSurface(holder.getSurface());
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    mediaPlayer.setSurface(holder.getSurface());
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mediaPlayer.setSurface(null);
                }
            });
        }
    }

    private void startCustomVideo() {
        // don't show if the Settings dialog is visible
        if (ProductDashboardHost.isShowing() || goButtonAlreadyClicked) {
            return;
        }

        mHandler.post(() -> {
            videoIndex = 0;
            playVideo();
            mediaPlayer.setOnCompletionListener(mp -> {
                if (mediaPlayer != null && mVideoSurfaceView.getVisibility() == View.VISIBLE) {
                    videoIndex++;
                    if (videoIndex >= videoFiles.length) {
                        videoIndex = 0;
                    }
                    playVideo();
                }
            });
        });
    }

    private void playVideo() {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(videoFiles[videoIndex].getPath());
            mediaPlayer.setOnPreparedListener(mp -> {
                mVideoSurfaceView.setVisibility(View.VISIBLE);
                DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Foreground.ordinal());
                mediaPlayer.start();
            });
            mediaPlayer.prepareAsync();

            mMessageBoxDialog.closeMessageBox();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    private void stopVideo() {
        if (mVideoSurfaceView.getVisibility() == View.VISIBLE) {
            mVideoSurfaceView.setVisibility(View.GONE);
            DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Default.ordinal());
            mediaPlayer.stop();
        }
    }

    private boolean isClubComVisible() {
        return clubComWebView != null && clubComWebView.getVisibility() == View.VISIBLE;
    }

    private void configureClubCom(ClubComOptions options) {
        if (!DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CLUB_COM_BOOL) || options == null) return;

        ClubComUrl clubComWelcomeEntry = options.getWelcomeUrl();
        String clubComWelcomeUrl = clubComWelcomeEntry == null ? "" : clubComWelcomeEntry.getUrl();
        if (clubComWelcomeUrl.isEmpty()) return;

        clubComWebView.setVisibility(View.VISIBLE);
        findViewById(R.id.welcome_background).setVisibility(View.INVISIBLE);
        findViewById(R.id.welcome_background2).setVisibility(View.INVISIBLE);
        clubComWebView.loadUrl(clubComWelcomeUrl);
        resetTimers();
    }

    // safety pin stuff!
    private void checkIfSafetyPinNeeded(DigitalSafetyPinDialog.Action action) {
        if (DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL)) {
            if (safetyPrompt == null) {
                safetyPrompt = new DigitalSafetyPinDialog(getApplicationContext());
            }
            safetyPrompt.checkIfNeeded(action);
        } else {
            action.doAction();
        }
    }

}
