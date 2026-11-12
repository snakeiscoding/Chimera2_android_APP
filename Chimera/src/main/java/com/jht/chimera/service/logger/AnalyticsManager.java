package com.jht.chimera.service.logger;

import static com.jht.chimera.lib.common.analytic.AnalyticsUtil.STATS_FILE;
import static com.jht.chimera.lib.common.analytic.AnalyticsUtil.STATS_FOLDER;
import static com.jht.chimera.lib.common.analytic.AnalyticsUtil.WORKOUT_FOLDER;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_PAUSE;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_RUNNING;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_STOP;

import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.gson.Gson;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.analytic.AnalyticStats;
import com.jht.chimera.lib.common.analytic.AnalyticWorkoutSession;
import com.jht.chimera.lib.common.analytic.AnalyticsUtil;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.dapi.DapiCreateUser;
import com.jht.chimera.lib.common.dapi.DapiLogin;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.PTPKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.lib.workoutapps.AppIconManager;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.DapiResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;

@SuppressWarnings("HardCodedStringLiteral")
public class AnalyticsManager {

    final String TAG = "AnalyticsManager";
    Context mContext = ContextUtil.getApplicationContext();
    DataMatrix mDataMatrix = new DataMatrix();
    FrameConfig mFrameConfig = FrameConfigManager.getInstance().getFrameConfig();
    boolean mWorkoutStarted = false;
    PeriodicTimer mPeriodicTimer = new PeriodicTimer(1000, 1000, () -> monitorProcess());
    String mCurrentAcivityInfo = "Unknown";
    boolean mIsRunning = false;
    boolean mHasSetupFinished = false;
    String mLoginType = "";
    File mStatsFile = new File(STATS_FILE);

    private long ptpSystemTime;
    long ptpInitTime;

    AnalyticWorkoutSession mWorkoutSession = new AnalyticWorkoutSession();
    AnalyticStats mStats = new AnalyticStats();
    private Map<String, AppIcon> appList;

    // Used to locally store times for workouts
    private long ptpTimingLoginTime = 0;
    private long ptpTimingUserInfoTime = 0;
    private long ptpTimingUserConnectionsTime = 0;
    private long ptpTimingShowUiTime = 0;
    private long ptpTimingApiCookieTime = 0;
    private long ptpTimingApiWorkoutsTime = 0;

    public static AnalyticsManager getInstance() {
        return AnalyticsManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final AnalyticsManager INSTANCE = new AnalyticsManager();
    }

    public void onAccessibilityClickEvent(String buttonId) {
        try {
            if (buttonId.contains("right_fan_button")) {
                mStats.fanClickCount++;
            } else if (buttonId.contains("appIconImage")) {
                String appName = buttonId.substring(buttonId.indexOf("[") + 1, buttonId.indexOf("]"));
                incrementCountValue(mStats.workoutAppClickCountMap, appName);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    private AnalyticsManager() {
        appList = AppIconManager.getInstance().getAllApps();
        AppIconManager.getInstance().finishAppManager();
    }

    public void onAccessibilityWindowEvent(AccessibilityEvent event) {
        try {
            String className = event.getClassName().toString();
            String description = "";
            AccessibilityNodeInfo source = event.getSource();
            if (source != null && source.getContentDescription() != null) {
                description = event.getContentDescription().toString();
            }

            if (className.contains("com.jht")) {

                // trim classname
                className = className.substring(className.lastIndexOf(".") + 1);

                // return classname and description for jht classes
                if (description != null && !description.isEmpty())
                    setCurrentActivityInfo(className + "|" + description);
                else
                    setCurrentActivityInfo(className);
            } else {
                // just return class package for 3rd party apps
                setCurrentActivityInfo(event.getPackageName().toString());
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public void onUSBAttachedEvent() {
        mStats.usbAttachedCount++;
    }

    void saveJsonFile(File file, String json) {

        try {
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(json);
            myOutWriter.close();
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            Log.e(TAG, "File write failed: " + e.toString());
        }
    }

    synchronized void updateSession() {
        // only update the session stats if we finished a workout
        if (mHasSetupFinished) {
            Gson gson = new Gson();
            // only save local session if experimental setting is enabled
            if (mDataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_ANALYTICS_BOOL)) {
                String jsonSessionStats = gson.toJson(mWorkoutSession);
                Log.d(TAG, "jsonSessionStats ==> " + jsonSessionStats);
                saveJsonFile(new File(WORKOUT_FOLDER, String.format("workout_%04d.json", mStats.totalWorkouts)), jsonSessionStats);
            }

            updateStats();
            String jsonTotalStats = gson.toJson(mStats);
            saveJsonFile(mStatsFile, jsonTotalStats);
            Log.d(TAG, "jsonTotalStats ==> " + jsonTotalStats);
        }

        mWorkoutSession = new AnalyticWorkoutSession();
        mHasSetupFinished = false;
    }

    void setCurrentActivityInfo(String activityInfo) {
        mCurrentAcivityInfo = AnalyticsUtil.getAppStringId(activityInfo, appList);
        if (!mIsRunning) return;

        boolean showWelcomeScreen = mDataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL);
        if (showWelcomeScreen && mCurrentAcivityInfo.contains("WelcomeActivity")) {
            updateSession();
        } else if (!showWelcomeScreen && mCurrentAcivityInfo.contains("GoScreenNewActivity")) {
            updateSession();
        }

        if (ProductConfig.isParker() &&mCurrentAcivityInfo.contains("ParkerGoScreenActivity")) updateSession();
    }

    synchronized void loadStats() {
        try {
            if (!mStatsFile.exists()) return;
            Gson gson = new Gson();
            AnalyticStats stats = gson.fromJson(new BufferedReader(new InputStreamReader(new FileInputStream(mStatsFile))), AnalyticStats.class);
            Log.d(TAG, "load ==> " + gson.toJson(stats));
            if (stats != null) mStats = stats;
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public void start() {
        mIsRunning = true;
        mPeriodicTimer.start();

        // make sure folder exists
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("mkdir -p " + WORKOUT_FOLDER);
        executeAsRoot.execute();

        new Thread(() -> {
            loadStats();
        }).start();

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, runState -> {

            switch (runState) {
                case RUN_STATE_STOP:
                    if (mWorkoutStarted) {
                        mWorkoutStarted = false;
                    }
                    break;
                case RUN_STATE_RUNNING:
                    DataMatrix.putBoolean(MainAppKeys.DATA_SPLASH_ANALYTICS_BOOL, true);
                    if (!mWorkoutStarted) {
                        mWorkoutStarted = true;
                        mHasSetupFinished = true;
                        mWorkoutSession.workoutStartTimeSec = System.currentTimeMillis() / 1000L;
                        mWorkoutSession.workoutId = mDataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
                    }
                    break;
                case RUN_STATE_PAUSE:
                    break;
            }
        });

        mDataMatrix.createParcelableAdapter(DapiLogin.Keys.ACTION_LOGIN_PARCEL, DapiLogin.DapiLoginParcelable.class, loginParcelable -> {
            // listen to login call and cache login type
            mLoginType = loginParcelable.loginType;
            ptpSystemTime = System.currentTimeMillis();
            ptpInitTime = System.currentTimeMillis();
        });
        mDataMatrix.createIntAdapter(DapiLogin.Keys.EVENT_LOGIN_RESPONSE_INT, dapiStatus -> {
            if (dapiStatus == DapiResult.Success.Value) {
                mWorkoutSession.loginType = mLoginType;
                long time = System.currentTimeMillis() - ptpSystemTime;
                ptpSystemTime = System.currentTimeMillis();
                ptpTimingLoginTime = time;
                Log.i(TAG, "PTP Timing: Login @" + time / 1000f);
            }
        });
        mDataMatrix.createBoolAdapter(DAPIKeys.DATA_GETTING_USER_INFO_BOOL, value -> {
            if (!value) {
                long time = System.currentTimeMillis() - ptpSystemTime;
                ptpSystemTime = System.currentTimeMillis();
                ptpTimingUserInfoTime = time;
                Log.i(TAG, "PTP Timing: Get User Info @" + time / 1000f);
            }
        });
        // actually have to add the above time and this to get the full timing for CBOR 3 as a ptp user.
        // this is because umDapiUserInfoHandler will put DATA_IS_PTP_USER_BOOL=true for a ptp user
        // while stashing all the user info from the CBOR 3 call, and then hit the
        // DATA_GETTING_USER_INFO_BOOL=false checkpoint the timing above is looking for.
        mDataMatrix.createBoolAdapter(SettingKeys.General.DATA_IS_PTP_USER_BOOL, value -> {
            if (value) {
                long time = System.currentTimeMillis() - ptpSystemTime;
                ptpSystemTime = System.currentTimeMillis();
                ptpTimingUserConnectionsTime = time;
                Log.i(TAG, "PTP Timing: Get User Connections @" + time / 1000f);
            }
        });
        // no longer in use by chameleon login flow
        mDataMatrix.createEventAdapter(PTPKeys.ACTION_PTP_ACTIVITY_LOADED, value -> {
            long time = System.currentTimeMillis() - ptpSystemTime;
            ptpSystemTime = System.currentTimeMillis();
            ptpTimingShowUiTime = time;
            Log.i(TAG, "PTP Timing: Activity Loaded @" + time / 1000f);
        });
        // no longer in use by chameleon login flow
        mDataMatrix.createStringAdapter(PTPKeys.DATA_PTP_USER_COOKIE_STRING, value -> {
            if (value != null && !value.isEmpty()) {
                long time = System.currentTimeMillis() - ptpSystemTime;
                ptpSystemTime = System.currentTimeMillis();
                ptpTimingApiCookieTime = time;
                mStats.ptpLoginCount++;
                Log.i(TAG, "PTP Timing: API Get Cookie @" + time / 1000f);
            }
        });
        // no longer in use by chameleon login flow
        mDataMatrix.createIntAdapter(PTPKeys.ACTION_PTP_WORKOUTS_LOADED_INT, value -> {
            long time = System.currentTimeMillis() - ptpSystemTime;
            ptpSystemTime = System.currentTimeMillis();
            ptpTimingApiWorkoutsTime = time;
            mStats.ptpWorkoutCount += value;
            Log.i(TAG, "PTP Timing: API Workouts Loaded @" + time / 1000f + " Workouts: " + value);
        });
        // no longer in use by chameleon login flow
        mDataMatrix.createEventAdapter(PTPKeys.ACTION_PTP_DISPLAY_LOADED, value -> {
            long displayTime = System.currentTimeMillis() - ptpSystemTime;
            long totalTime = System.currentTimeMillis() - ptpInitTime;

            // Add all times at this point since we know it's a PTP workout here
            mStats.ptpTimingLogin += ptpTimingLoginTime;
            mStats.ptpTimingUserInfo += ptpTimingUserInfoTime;
            mStats.ptpTimingUserConnections += ptpTimingUserConnectionsTime;
            mStats.ptpTimingShowUI += ptpTimingShowUiTime;
            mStats.ptpTimingAPICookie += ptpTimingApiCookieTime;
            mStats.ptpTimingAPIWorkouts += ptpTimingApiWorkoutsTime;
            mStats.ptpTimingDisplay += displayTime;
            mStats.ptpTimingComplete += totalTime;

            saveStats();
            Log.i(TAG, "PTP Timing: Displayed @" + displayTime / 1000f);
            Log.i(TAG, "PTP Timing: Total Time @" + totalTime / 1000f);
        });

        mDataMatrix.createParcelableAdapter(DapiCreateUser.Keys.ACTION_REGISTER_USER_PARCEL, DapiCreateUser.DapiRegisterParcelable.class, parcel -> mLoginType = "register_xid");
        mDataMatrix.createIntAdapter(DapiCreateUser.Keys.EVENT_REGISTER_RESPONSE_INT, dapiStatus -> {
            if (dapiStatus == DapiResult.Success.Value) {
                mWorkoutSession.loginType = mLoginType;
                mStats.userRegistrationCount++;
            }
        });


        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, value -> {
            KeyPress.KeyCode keyCode = KeyPress.getKeyCode(value);
            KeyPress.KeyState keyState = KeyPress.getKeyState(value);
            if (keyState == KeyPress.KeyState.KEY_DOWN) {
                incrementCountValue(mStats.keyPressCountMap, keyCode.name());
            }
        });
        mDataMatrix.createIntAdapter(HALKeys.DATA_SAFETY_KEY_STATUS_INT, status -> {
            if (ChimeraEnums.isTreadmillSafetyKeyPulled(status)) mStats.treadmillSafetyKeyCount++;
            if (ChimeraEnums.isClimbmillHandrailPulled(status)) mStats.climbmillHandrailCount++;
            if (ChimeraEnums.isClimbmillUCBPulled(status)) mStats.climbmillUCBCount++;
            if (ChimeraEnums.isClimbmillControlZoneTriggered(status))
                mStats.climbmillControlZoneCount++;
            if (ChimeraEnums.isClimbmillIRSensorTriggered(status)) mStats.climbmillIRSensorCount++;
        });
    }

    public void stop() {
        mIsRunning = false;
        mDataMatrix.finish();
        mPeriodicTimer.stop();
    }

    void accumulateCountValue(Map<String, Long> map, String key, long incrementValue) {
        Long value = map.get(key);
        if (value == null) value = new Long(0);
        value += incrementValue;
        map.put(key, value);
    }

    void incrementCountValue(Map<String, Long> map, String key) {
        accumulateCountValue(map, key, 1);
    }

    void updateStats() {
        mStats.totalWorkouts++;

        incrementCountValue(mStats.workoutIdMap, String.valueOf(mWorkoutSession.workoutId));
        incrementCountValue(mStats.loginTypeCountMap, mWorkoutSession.loginType);

        // using for-each loop for iteration over Map.entrySet()
        for (Map.Entry<String, Long> entry : mWorkoutSession.workoutActivityInfoTimeSecMap.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();

            accumulateCountValue(mStats.workoutActivityInfoTimeSecMap, key, value);
            incrementCountValue(mStats.workoutActivityInfoCountMap, key);
        }

    }

    void monitorProcess() {
        if (mWorkoutStarted) {
            incrementCountValue(mWorkoutSession.workoutActivityInfoTimeSecMap, mCurrentAcivityInfo);
        }
    }

    private void saveStats() {
        Gson gson = new Gson();
        String jsonTotalStats = gson.toJson(mStats);
        saveJsonFile(mStatsFile, jsonTotalStats);
    }

    public void resetAllData() {
        // make sure folder exists
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("rm -rf " + STATS_FOLDER);
        executeAsRoot.addCommand("mkdir -p " + WORKOUT_FOLDER);
        executeAsRoot.execute();

        mStats = new AnalyticStats();
    }

    public void resetPTPData() {
        mStats.ptpLoginCount = 0;
        mStats.ptpWorkoutCount = 0;
        mStats.ptpTimingLogin = 0;
        mStats.ptpTimingUserInfo = 0;
        mStats.ptpTimingUserConnections = 0;
        mStats.ptpTimingShowUI = 0;
        mStats.ptpTimingAPICookie = 0;
        mStats.ptpTimingAPIUserInfo = 0;
        mStats.ptpTimingAPIWorkouts = 0;
        mStats.ptpTimingDisplay = 0;
        mStats.ptpTimingComplete = 0;
        saveStats();
    }

    public void export() {

        String usbPath = StorageUtils.getUSBPath().getAbsolutePath();
        File exportPath = new File(usbPath, "analytics/" + DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING));
        exportPath.mkdirs();

        ExecuteAsRoot root = new ExecuteAsRoot();
        root.addCommand("cp -rf " + STATS_FOLDER + " " + exportPath.getAbsolutePath());
        root.execute();
    }

    AnalyticsDialog dialog = new AnalyticsDialog(mContext);

    public void viewStats() {
        dialog.show();
    }
}
