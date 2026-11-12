package com.jht.chimera.ui.mainapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import com.jht.chimera.lib.widget.activity.RestringActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.jht.androidcommonalgorithms.events.JHTDataNotification;
import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.SystemBrightness;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.datamatrixkeys.ConsoleConfigKeys;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.chimera.lib.common.timer.HomeScreenIdleTimer;
import com.jht.chimera.lib.login.WalletPass;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.ui.mainapp.egym.EGymWebAppInterface;
import com.jht.chimera.ui.mainapp.util.BackgroundManager;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.oauthlib.auth.BksCredentialProvider;
import com.jht.rfid.Legic.RFIDChip;
import com.jht.rfidlib.WalletType;

import org.jetbrains.annotations.NonNls;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

public class EGymActivity extends RestringActivity {
    private static final String TAG = "EGymActivity";
    @NonNls
    private static final String EGYM_REFRESH_INTENT = "com.jht.egym.refresh";

    private final String eGymAppleIssuerId = "pass.com.vollgas.checkIn";
    private final String eGymGoogleIssuerId = "83045681";

    private LinearLayout mWebViewLayout;
    private WebView mWebView;
    private final DataMatrix mDataMatrix = new DataMatrix();
    private Handler mHandler;
    private boolean mFinishedLoading;
    private PendingIntent mRefreshPendingIntent = null;
    private HomeScreenIdleTimer mHomeScreenIdleTimer;
    private final EGymWebAppInterface mInterface = new EGymWebAppInterface(this);
    private boolean networkConnected;
    private BackgroundManager backgroundManager;
    private JHTTimer webViewFadeTimer;
    private Animation fadeInAnimation;
    private Animation fadeOutAnimation;
    private ImageView touchIcon;
    private boolean touchIconIsShowing = false;
    private EGymWebAppInterface.EGymState previousState;

    // Manages the state of the webview panel that is displayed when the EGYM app is paused
    boolean webViewOverlayShown = false;

    private final BroadcastReceiver testAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mInterface.gainAudioFocus();
        }
    };

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }


    LinearLayout basicLayout;

    private void showWebView() {
        if (webViewFadeTimer != null) {
            webViewFadeTimer.stop();
        }
        //In case we have hidden the view using auto hide option
        if (mWebView.getVisibility() != View.VISIBLE) {
            mWebView.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CommonRoutines.initWebActivity(this, R.layout.egym_activity, TAG);

        basicLayout = new LinearLayout(this);
        getLayoutInflater().inflate(R.layout.blank_layout, basicLayout);

        Log.d(TAG, "onCreate");
        mHandler = new Handler();

        findViewById(R.id.btn_refesh).setOnClickListener(v -> finish());
        findViewById(R.id.btn_matrix).setOnClickListener(v -> LaunchAppUtil.launchGoScreen());

        mWebViewLayout = findViewById(R.id.egym_container);

        mHomeScreenIdleTimer = new HomeScreenIdleTimer(this, true);
        mWebView = findViewById(R.id.webview);
        // CGC-17607 Some webview versions such as 114 do not like software layers. Our other webviews are kept hardware when SDK_INT >= 19
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        //mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // Added this based on https://stackoverflow.com/questions/19739269/gl-error-from-openglrenderer-0x502
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setBuiltInZoomControls(false);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        mWebView.addJavascriptInterface(mInterface, "external");
        webViewFadeTimer = new JHTTimer(() -> mHandler.post(this::fadeOutWebView));
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mFinishedLoading = true;
                DataMatrix.sendEvent(LoggerKeys.ACTION_RECORD_BOOT_TIME);
                Log.d(TAG, "onPageFinished: " + url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Log.w(TAG, "onReceivedError: "  + error.getErrorCode() + " Internet: " + DataMatrix.getBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL));
                //We want to handle errors received on the EGYM webview in a similar fashion as no internet
                //Otherwise, we may accidentally softlock the console if the webpage fails to load.
                //This at least leaves an out for the matrix experience.
                handleEgymError(false);
            }
        });

        mDataMatrix.createIntAdapter(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, value -> finish(), false);
        mDataMatrix.createIntAdapter(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, value -> finish(), false);
        mDataMatrix.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING, value -> finish(), false);
        mDataMatrix.createStringAdapter(SettingKeys.Experimental.DATA_EGYM_ENVIRONMENT_2_STRING, value -> finish(), false);

        mDataMatrix.createBoolAdapter(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, value -> {
            if (!mFinishedLoading) return;
            mHandler.post(() -> mWebView.loadUrl("javascript:setUnit(" + "'" + (value ? "METRIC" : "IMPERIAL") + "'" + ");"));
        }, false);

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, value -> {
            if (!mFinishedLoading) return;
            mHandler.post(() -> mWebView.loadUrl("javascript:onMetricsUpdated(" + mInterface.onMetricsUpdated() + ");"));
        }, false);
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, value -> {
            if (!mFinishedLoading) return;
            mHandler.post(() -> {
                switch (value) {
                    case WorkoutKeys.Constants.RUN_STATE_PAUSE: //value 2
                        mInterface.setEgymState("workout");
                        mInterface.setWorkoutState("pause");
                        mInterface.setLoginInProgress(true);
                        mWebView.loadUrl("javascript:onWorkoutStateChanged();");
                        break;
                    case WorkoutKeys.Constants.RUN_STATE_RUNNING: //value 1
                        mInterface.setEgymState("workout");
                        mInterface.setWorkoutState("run");
                        mInterface.setLoginInProgress(true);
                        mWebView.loadUrl("javascript:onWorkoutStateChanged();");
                        break;
                    case WorkoutKeys.Constants.RUN_STATE_STOP: // value 0
                        mInterface.setEgymState("summary");
                        mInterface.setWorkoutState("stop");
                        mInterface.setLoginInProgress(false);
                        mWebView.loadUrl("javascript:onWorkoutStateChanged();");
                        mWebView.loadUrl("javascript:onMetricsUpdated();");
                        break;
                }
            });
        }, false);


        mDataMatrix.createParcelableAdapter(HALKeys.EVENT_RFID_TAG_DETECTED_PARCELABLE, RFIDChip.class, chip -> {
            if (!mFinishedLoading) return;
            if (!mInterface.getIsLoginInProgress()) {
                mHandler.post(() -> {
                    String tagInfo = getTagJson(chip.uid(), chip.rfStandard().toByte());
                    Log.d(TAG, "Sending RFID info to web: " + tagInfo);
                    mInterface.setLoginInProgress(true);
                    mWebView.loadUrl("javascript: onRFIDTokenReceived(" + tagInfo + ")");
                    showWebView();
                });
            }

        }, false);

        mDataMatrix.createParcelableAdapter(HALKeys.EVENT_WALLET_PASS_DETECTED_PARCELABLE, WalletPass.class, walletPass -> {
            if (!mFinishedLoading) return;

            // NOTE: if eGym changes the passes issuerId, we need to update it as well!
            // making sure to only allow eGym wallet passes so we don't accidentally pass our pass
            if (walletPass.passType == WalletType.ApplePay && !walletPass.issuerId.equalsIgnoreCase(eGymAppleIssuerId)) {
                Log.d(TAG, "Ignoring pass with issuerId: " + walletPass.issuerId);
                return;
            }
            if (walletPass.passType == WalletType.AndroidPay && !walletPass.issuerId.equalsIgnoreCase(eGymGoogleIssuerId)) {
                Log.d(TAG, "Ignoring pass with issuerId: " + walletPass.issuerId);
                return;
            }

            if (!mInterface.getIsLoginInProgress()) {
                mHandler.post(() -> {
                    String passInfo = getWalletPassJson(walletPass);
                    Log.i(TAG, "Sending wallet pass info to web: " + passInfo);
                    mInterface.setLoginInProgress(true);
                    mWebView.loadUrl("javascript: onNFCRead(" + passInfo + ")");
                    showWebView();
                });
            }

        }, false);

        previousState = mInterface.getEgymStateEnum();
        mInterface.setOnEGymSateChange(state -> mHandler.post(() -> {
            if (state != previousState) {
                Log.d(TAG, "previousState=" + previousState + " state=" + state);
                if (shown) {
                    handleLCD(state);
                    if (state == EGymWebAppInterface.EGymState.HOME) {
                        // quick hack to force splash leaving summary, might be better for egym summary to signal a splash reset and have services reset as needed?
                        if (previousState == EGymWebAppInterface.EGymState.SUMMARY) {
                            Log.d(TAG, "transition from SUMMARY to HOME, trigger splash");
                            LaunchAppUtil.launchSplashScreen();
                            finish();
                        } else {
                            restartCustomWallpaperTimer();
                            mHomeScreenIdleTimer.start();
                        }
                    } else {
                        mHomeScreenIdleTimer.stop();
                        webViewFadeTimer.stop();
                    }
                }
                previousState = state;
            }
        }));

        // NOTE: this handles the case on first boot to wait for internet connection
        // try to reload the page once there's internet connection
        networkConnected = DataMatrix.getBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL);
        findViewById(R.id.layout_error).setVisibility(networkConnected ? View.GONE : View.VISIBLE);
        mDataMatrix.createBoolAdapter(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL, value -> {
            if (networkConnected != value) {
                networkConnected = value;
                handleEgymError(value);
            }
        }, false);

        // NOTE: this handles the case on first boot to wait for internet connection but there is no internet
        reload();
        fadeInAnimation = new AlphaAnimation(0, 1);
        fadeOutAnimation = new AlphaAnimation(1, 0);
        touchIcon = findViewById(R.id.egym_touch_icon);
    }

    private boolean shown = false;

    public boolean resizeApp() {
        JHTDataNotification<Boolean> resizeNotification = new JHTDataNotification<>();
        mHandler.post(() -> {
            if (!shown || !AppIds.EGYM_UUID.equals(DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING))) {
                resizeNotification.setData(false);
                return;
            }
            if (DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT) != UiServiceKeys.WorkoutPanelState.REGULAR) {
                DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR);
            } else {
                DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.FULLSCREEN);
            }
            resizeNotification.setData(true);

        });

        Boolean result = resizeNotification.waitForData(500);
        if (result != null) {
            Log.d(TAG, "returning result " + result);
            return result;
        } else {
            return false;
        }
    }

    private void handleLCD(EGymWebAppInterface.EGymState state) {
        switch (state) {
            case SUMMARY:
                DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.WORKOUT_SUMMARY.name());
                break;
            case WORKOUT:
                DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.WORKOUT.name());
                break;
            default:
                DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.EGYM_LOADING.name());
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        shown = true;

        // Remove the hidden panel and add the WebView back to the root in onResume if needed
        removeWebViewHiddenPanel();

        DataMatrix.putBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL, true);
        DataMatrix.putBoolean(UiServiceKeys.ACTION_DISABLE_SAFETY_SCREENS_BOOL, false);
        setRefreshAlarm();
        registerReceiver(mReceiver, new IntentFilter(EGYM_REFRESH_INTENT));
        EGymWebAppInterface.EGymState state = mInterface.getEgymStateEnum();
        handleLCD(state);
        if (state == EGymWebAppInterface.EGymState.HOME) {
            mHomeScreenIdleTimer.start();
        }
        registerReceiver(testAudio, new IntentFilter("com.jht.test.egym.audio"));
        backgroundManager = new BackgroundManager(findViewById(R.id.welcome_background), findViewById(R.id.welcome_background2));
        mDataMatrix.createEventAdapter(ConsoleConfigKeys.EVENT_CONSOLE_THEME_READY, s -> {
            if (backgroundManager != null)
                backgroundManager.loadCustomImages();
        });
        restartCustomWallpaperTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shown = false;
        // Add the hidden panel in onPause, this will be removed in onResume
        addWebViewHiddenPanel();
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception ignored) {
        }
        try {
            unregisterReceiver(testAudio);
        } catch (Exception ignored) {
        }
        mHomeScreenIdleTimer.stop();
        if (backgroundManager != null) {
            backgroundManager.stopTimer();
            backgroundManager = null;
        }

        if (webViewFadeTimer != null) {
            webViewFadeTimer.stop();
        }
    }

    /**
     * We need to add the WebView to the WindowManager to keep it active. Otherwise the WebView
     * will throttle javascript after 5 minutes of being in the background.
     * Check CGC-14379 for more info on this issue.
     */
    private void addWebViewHiddenPanel() {
        if (!webViewOverlayShown) {
            // Remove view from the view hierarchy so that the UI service can add it there
            mWebViewLayout.removeView(mWebView);
            basicLayout.addView(mWebView);

            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(
                    0, 0,
                    ChimeraEnums.TYPE_SYSTEM_ERROR,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSPARENT
            );
            viewParams.gravity = Gravity.NO_GRAVITY;
            viewParams.x = 0;
            viewParams.y = 0;
            windowManager.addView(basicLayout, viewParams);
            webViewOverlayShown = true;
        }
    }

    /**
     * Removes the WebView panel if it's displayed, and adds the WebView back into the activity
     * root view. We need the WebView panel to prevent JavaScript from throttling after 5 minutes.
     * Check CGC-14379 for more info on this issue.
     */
    private void removeWebViewHiddenPanel() {
        // Hide the webview panel if it is displayed and add the weview back to the activity layout
        if (webViewOverlayShown) {
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.removeView(basicLayout);

            if (mWebView.getParent() != null) {
                ViewGroup parentLayout = (ViewGroup) mWebView.getParent();
                parentLayout.removeView(mWebView);
            }

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mWebViewLayout.addView(mWebView, layoutParams);
            webViewOverlayShown = false;
        }
    }

    @Override
    public void finish() {
        Log.d(TAG, "finish");
        mDataMatrix.finish();
        if (mRefreshPendingIntent != null) {
            AlarmManager manager = (AlarmManager) (getSystemService(Context.ALARM_SERVICE));
            if (manager != null) {
                manager.cancel(mRefreshPendingIntent);
            }
        }
        super.finish();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            restartCustomWallpaperTimer();
        }
        return super.dispatchTouchEvent(ev);
    }

    private void restartCustomWallpaperTimer() {
        if (mInterface.getEgymState().equals(EGymWebAppInterface.EGymState.HOME.name().toLowerCase(Locale.ENGLISH))) {
            webViewFadeTimer.stop();
            fadeInWebView();
            int timeout = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT) * 1000;
            if (timeout > 0) {
                webViewFadeTimer.start(timeout);
            }
        }
    }


    private final Runnable hideOrShowHandIcon = new Runnable() {
        @Override
        public void run() {
            if (touchIconIsShowing) {
                fadeOutAnimation.setDuration(1000);
                touchIcon.startAnimation(fadeOutAnimation);
                touchIcon.setVisibility(View.GONE);
                touchIconIsShowing = false;
                mHandler.postDelayed(this, 60000);
            } else {
                fadeInAnimation.setDuration(1000);
                touchIcon.startAnimation(fadeInAnimation);
                touchIcon.setVisibility(View.VISIBLE);
                touchIconIsShowing = true;
                mHandler.postDelayed(this, 10000);
            }

        }
    };

    private void fadeOutWebView() {
        try {
            if (findViewById(R.id.webview).getVisibility() == View.VISIBLE) {
//            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 50);
                int position = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_TIMEOUT_ENUM_INT);
                int timeUntilScreenDim;
                switch (position) {
                    case SettingKeys.AssetManagementConstants.SCREEN_BRIGHTNESS_AFTER_TIMEOUT_60_MIN:
                        timeUntilScreenDim = 60;
                        break;
                    case SettingKeys.AssetManagementConstants.SCREEN_BRIGHTNESS_AFTER_TIMEOUT_90_MIN:
                        timeUntilScreenDim = 90;
                        break;
                    case SettingKeys.AssetManagementConstants.SCREEN_BRIGHTNESS_AFTER_TIMEOUT_120_MIN:
                        timeUntilScreenDim = 120;
                        break;
                    case SettingKeys.AssetManagementConstants.SCREEN_BRIGHTNESS_AFTER_TIMEOUT_0_MIN:
                        timeUntilScreenDim = 0;
                        break;
                    case SettingKeys.AssetManagementConstants.SCREEN_BRIGHTNESS_AFTER_TIMEOUT_30_MIN:
                    default:
                        timeUntilScreenDim = 30;
                        break;
                }

                mHandler.removeCallbacks(screenDimRunnable);
                mHandler.postDelayed(screenDimRunnable, timeUntilScreenDim * 60000);

                fadeInAnimation.setDuration(1000);
                touchIcon.startAnimation(fadeInAnimation);
                touchIcon.setVisibility(View.VISIBLE);
                touchIconIsShowing = true;
                mHandler.removeCallbacks(hideOrShowHandIcon);
                mHandler.postDelayed(hideOrShowHandIcon, 10000);

                if (backgroundManager != null) {
                    backgroundManager.enableDefaultWallpapers(true);
                }
                fadeOutAnimation.setDuration(500);
                findViewById(R.id.webview).startAnimation(fadeOutAnimation);
                findViewById(R.id.webview).setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.w(TAG, "fadeOutWebView error", e);
        }
    }

    public void fadeInWebView() {
        mHandler.post(() -> {
            try {
                if (findViewById(R.id.webview).getVisibility() != View.VISIBLE) {
                    mHandler.removeCallbacks(hideOrShowHandIcon);
                    mHandler.removeCallbacks(screenDimRunnable);
                    SystemBrightness.setSystemBrightness(getContentResolver(), DataMatrix.getInt(SettingKeys.Hardware.DATA_BACKLIGHT_PROGRESS_INT));
                    touchIcon.setVisibility(View.GONE);
                    touchIconIsShowing = false;
                    fadeInAnimation.setDuration(500);
                    findViewById(R.id.webview).startAnimation(fadeInAnimation);
                    findViewById(R.id.webview).setVisibility(View.VISIBLE);
                    if (backgroundManager != null) {
                        backgroundManager.enableDefaultWallpapers(false);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "fadeInWebView error", e);
            }
        });
    }

    private final Runnable screenDimRunnable = () -> SystemBrightness.setSystemBrightness(getContentResolver(), DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_PERCENT_AFTER_TIMEOUT_INT));

    public void reload() {
        if (mInterface.getEgymStateEnum() == EGymWebAppInterface.EGymState.WORKOUT) {
            Log.e(TAG, "Not reloading: we should not reload EGYM in a workout.");
            return;
        }


        ExecuteAsRoot clearCache = new ExecuteAsRoot();
        clearCache.addCommand("rm -rf /data/data/com.jht.chimera/app_webview_EGymActivity/*");
        clearCache.addCommand("rm -rf /data/data/com.jht.chimera/cache/webview_EGymActivity/*");
        clearCache.execute();

        mFinishedLoading = false;
        pullClientSecret();

        String egymEnvironment = DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_ENVIRONMENT_2_STRING).toLowerCase();
        if (egymEnvironment.equalsIgnoreCase("development")) {
            Log.d(TAG, "loading egym dev url: " + DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_DEV_WEBSITE_2_STRING));
            mWebView.loadUrl(DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_DEV_WEBSITE_2_STRING));
        } else if (egymEnvironment.equalsIgnoreCase("production")) {
            Log.d(TAG, "loading egym prod url:" + DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_PROD_WEBSITE_2_STRING));
            mWebView.loadUrl(DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_PROD_WEBSITE_2_STRING));
        } else {
            Log.d(TAG, "loading egym dev url: " + DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_TESTING_WEBSITE_2_STRING));
            mWebView.loadUrl(DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_TESTING_WEBSITE_2_STRING));
        }

    }

    private void handleEgymError(boolean hasInternet){
        EGymWebAppInterface.EGymState eGymState = mInterface.getEgymStateEnum();
        if (hasInternet) {
            if (eGymState != EGymWebAppInterface.EGymState.WORKOUT && eGymState != EGymWebAppInterface.EGymState.SUMMARY) {
                // Reloads the page and removes the no internet overlay shortly after. We can do this whenever we're not in a workout.
                mHandler.postDelayed(() -> findViewById(R.id.layout_error).setVisibility(View.GONE), 6000);
                mHandler.postDelayed(this::reload, 3000);
            } else {
                // If we're in a workout or summary, show a message center message that we regained internet. Don't reload
                String messageText = String.format((getApplicationContext().getString(R.string.reconnection_tc)));
                MessageCenterMessage message = new MessageCenterMessage("", messageText, 10, 30);
                DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message);
                findViewById(R.id.layout_error).setVisibility(View.GONE);
            }
        } else {
            // Send message center message that we've lost internet connection if we're in selection, summary, or workout, otherwise show the no internet screen
            if (eGymState == EGymWebAppInterface.EGymState.SELECTION || eGymState == EGymWebAppInterface.EGymState.WORKOUT || eGymState == EGymWebAppInterface.EGymState.SUMMARY) {
                Log.e(TAG, "EGYM lost internet connection in workout");
                String messageText = String.format((getApplicationContext().getString(R.string.connection_error_tc )));
                MessageCenterMessage message = new MessageCenterMessage("", messageText, 10, 30);
                DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message);
            } else {
                findViewById(R.id.layout_error).setVisibility(View.VISIBLE);
                DataMatrix.sendEvent(UiServiceKeys.EVENT_HIDE_KEYBOARD);
            }
        }
    }


    private void setClientSecretValues(StringBuffer result) {
        if (result != null) {
            try {
                JSONObject jsonObject = new JSONObject(result.toString());
                String egymResult = jsonObject.getString("egym");
                jsonObject = new JSONObject(egymResult);
                String clientSecret = jsonObject.getString("partner_client_secret");
                String clientID = jsonObject.getString("partner_client_id");

                // Send client secret and id to control point for storage
                mInterface.setClientSecret(clientSecret);
                mInterface.setClientID(clientID);
            } catch (JSONException e) {
                Log.w(TAG, e);
            }
        }
    }

    private void pullClientSecret() {
        new Thread(() -> {
            // String totp = generateAuthorization();
            try {
                //Stub for now
                @NonNls String env;

                switch (DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)) {
                    case DAPIKeys.Constants.DAPI_DEPLOYMENT_DEV:
                        env = "dev/"; // NON-NLS
                        break;
                    case DAPIKeys.Constants.DAPI_DEPLOYMENT_QA:
                        env = "qa/"; // NON-NLS
                        break;
                    case DAPIKeys.Constants.DAPI_DEPLOYMENT_STAGING:
                        env = "staging/"; // NON-NLS
                        break;
                    case DAPIKeys.Constants.DAPI_DEPLOYMENT_PROD:
                    default:
                        env = "prod/"; // NON-NLS
                        break;
                }
                URL url = new URL("https://dapi.jfit.co/" + env + "console/config/club/" + DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT)); //NON-NLS
                Log.d(TAG, "ClientSecretURL: " + url);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(100000);
                conn.setConnectTimeout(15000);
                conn.setDoInput(true);
                conn.setDoOutput(false);
                BksCredentialProvider credentialProvider = new BksCredentialProvider(getApplicationContext());
                String apiKey = credentialProvider.getString("EGYM_API_KEY");
                conn.setRequestProperty("x-api-key", apiKey);
//                conn.setRequestProperty("Authorization", totp);
                conn.setRequestMethod("GET");
                conn.connect();
                Log.d(TAG, "Client Secret Connection Response Code: " + conn.getResponseCode());
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String inputLine;
                    StringBuffer result = new StringBuffer();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((inputLine = reader.readLine()) != null) {
                        result.append(inputLine);
                    }
                    reader.close();
                    conn.disconnect();
                    setClientSecretValues(result);

                }
            } catch (Exception e) {
                Log.e(TAG, "exception caught ", e);
            }


        }).start();

    }

    public String getWalletPassJson(WalletPass walletPass) {
        try {
            JSONObject json = new JSONObject();
            json.put("payload", walletPass.pass);
            json.put("transponderType", walletPass.passType == WalletType.ApplePay ? "APPLE" : "GOOGLE");
            json.put("issuerId", walletPass.issuerId);
            return json.toString();
        } catch (Exception e) {
            Log.e(TAG, "getWalletPassJson error", e);
            return "";
        }
    }

    private String getTagJson(byte[] uid, byte carrierType) {
        String tagUidArray = Arrays.toString(uid);
        String tagUidHex = StringUtil.byteArray2Hex(uid).toUpperCase();
        Log.d(TAG, "uid: " + Arrays.toString(uid));
        String tagType = getTagTypeAsString(carrierType);
        @NonNls String result = "{" +
                "\"uidHex\":" + "\"" + tagUidHex + "\"" + "," // NON-NLS
                + "\"uidArray\":" + "\"" + tagUidArray + "\"" + "," // NON-NLS
                + "\"tagType\":\"" + tagType + "\"" // NON-NLS
                + "}"; // NON-NLS
        Log.d(TAG, "returning tagJSON:" + result);
        return result;
    }

    private String getTagTypeAsString(byte carrier) {
        @NonNls String type;
        switch (carrier) {
            case 0x00:
                type = "LegicRf"; // NON-NLS
                break;
            case 0x01:
                type = "Iso15693"; // NON-NLS
                break;
            case 0x03:
                type = "Iso14443B"; // NON-NLS
                break;
            case 0x04:
                type = "InsideSecure"; // NON-NLS
                break;
            case 0x05:
                type = "SonyFelica"; // NON-NLS
                break;
            case 0x02:
            default:
                type = "Iso14443A"; // NON-NLS
                break;
        }
        Log.d(TAG, "found tag type of carrier id: " + carrier + " tag type: " + type);
        return type;
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    private void setRefreshAlarm() {
        int triggerTimeHour = DataMatrix.getInt(SettingKeys.Experimental.DATA_EGYM_REFRESH_HOUR_INT);

        AlarmManager manager = (AlarmManager) (getSystemService(Context.ALARM_SERVICE));
        PendingIntent checkIntent = PendingIntent.getBroadcast(this, 0, new Intent(EGYM_REFRESH_INTENT), PendingIntent.FLAG_IMMUTABLE);
        if (manager != null) {
            manager.cancel(checkIntent);
        }

        long currentTime = System.currentTimeMillis();
        Calendar currentCalendarTime = Calendar.getInstance();
        currentCalendarTime.setTimeInMillis(currentTime);
        Calendar updateTime = Calendar.getInstance();
        updateTime.setTimeInMillis(currentTime);
        updateTime.set(Calendar.HOUR_OF_DAY, triggerTimeHour);
        updateTime.set(Calendar.MINUTE, 0);
        updateTime.set(Calendar.SECOND, 0);
        updateTime.set(Calendar.MILLISECOND, 0);
        if (updateTime.before(currentCalendarTime)) {
            updateTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        long nextCheckTime = updateTime.getTimeInMillis();
        long delayCheck = nextCheckTime - currentTime;

        Log.d(TAG, "Sending refresh update check in " + delayCheck + " ms");
        if (mRefreshPendingIntent == null) {
            mRefreshPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(EGYM_REFRESH_INTENT), PendingIntent.FLAG_IMMUTABLE);
        }
        if (manager != null) {
            manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayCheck, checkIntent);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "on refresh alarm");
            if (mInterface.getEgymState().equals(EGymWebAppInterface.EGymState.HOME.name())) {
                finish();
            }
        }
    };

    public boolean shown() {
        return shown;
    }
}
