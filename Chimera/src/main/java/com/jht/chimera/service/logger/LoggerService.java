package com.jht.chimera.service.logger;

import android.accessibilityservice.AccessibilityService;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.SystemPropertyUtil;
import com.jht.chimera.lib.common.SystemUtils;
import com.jht.chimera.lib.common.alarm.Alarm;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.log.ServiceLog;
import com.jht.chimera.lib.common.log.ServiceLogList;
import com.jht.chimera.lib.common.performance.SystemStats;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.chimera.lib.common.testing.TestingUtils;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.widget.dialog.BandwidthTestDlg;
import com.jht.chimera.lib.widget.dialog.DebugHalDlg;
import com.jht.chimera.lib.widget.dialog.ProductDashboardHost;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.liberror.ErrorLogServiceExtender;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public final class LoggerService extends AccessibilityService {
    public static final String TAG = "LoggerService";
    @NonNls public static final String ACTION_UPDATE_DATA_MATRIX_KEY = "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY";
    @NonNls public static final String ACTION_LOG_DATA_MATRIX_KEY = "com.jht.loggerservice.ACTION_LOG_DATA_MATRIX_KEY";
    @NonNls public static final String ACTION_SHOW_DEBUG_DLG = "com.jht.loggerservice.ACTION_SHOW_DEBUG_DLG";
    @NonNls public static final String ACTION_UPDATE_TEST_AUTOMATION_MODE = "com.jht.loggerservice.ACTION_UPDATE_TEST_AUTOMATION_MODE";
    @NonNls public static final String ACTION_CONSOLE_REBOOT = "com.jht.loggerservice.ACTION_CONSOLE_REBOOT";
    @NonNls public static final String DATA_MATRIX_KEY = "key";
    @NonNls public static final String DATA_MATRIX_TYPE = "type";
    @NonNls public static final String DATA_MATRIX_VALUE = "value";
    @NonNls public static final String DATA_MATRIX_CLASS = "class";
    @NonNls public static final String EVENT_LOG_FILENAME = "event_log";

    @NonNls private static final String TYPE_STRING = "string";
    @NonNls private static final String TYPE_BOOLEAN = "boolean";
    @NonNls private static final String TYPE_DOUBLE = "double";
    @NonNls private static final String TYPE_LONG = "long";
    @NonNls private static final String TYPE_INTEGER = "integer";
    @NonNls private static final String TYPE_FLOAT = "float";
    @NonNls private static final String TYPE_STRING_LIST = "string_list";
    @NonNls private static final String TYPE_EVENT = "event";
    @NonNls private static final String TYPE_PARCELABLE = "parcelable";

    private final DataMatrix mDataMatrix = new DataMatrix();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    /**
     * The extender that adds error log support to this service.
     */
    private final ErrorLogServiceExtender mErrorLogExtender = new ErrorLogServiceExtender();
    private boolean mIsInitialized = false;
    private LoggerDataSource mDataSource;
    private boolean mIsPersistLogProcessRunning = false;
    private static java.util.logging.Handler mEventLog;
    private LogMonitor mLogMonitor;
    private boolean isAdbWifiEnabled = false;
    private JHTTimer  crashReportTimer;
    private Alarm persistentLogAlarm = null;
    private SystemStats systemStats = new SystemStats(this);

    private final BroadcastReceiver dumpLogsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dumpLogToCloud();
        }
    };

    @Override
    public void onCreate() {
        Log.d("ServiceStarter", "Starting " + this);
        super.onCreate();

        // Initialize the error log extender. This automatically publishes the current state of the
        // database, as required.
        mErrorLogExtender.onCreate(this);

        init();

        // Register all the receivers.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_UPDATE_DATA_MATRIX_KEY);
        intentFilter.addAction(ACTION_LOG_DATA_MATRIX_KEY);
        intentFilter.addAction(ACTION_SHOW_DEBUG_DLG);
        intentFilter.addAction(ACTION_UPDATE_TEST_AUTOMATION_MODE);
        intentFilter.addAction(ACTION_CONSOLE_REBOOT);
        registerReceiver(mUpdateKeyReceiver, intentFilter);
        registerReceiver(dumpLogsReceiver, new IntentFilter("com.jht.logs.dump_to_cloud"));

        Log.d("ServiceStarter", "Ending " + this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDataMatrix.finish();
        unregisterReceiver(usbMountedReceiver);
        unregisterReceiver(mUpdateKeyReceiver);
        unregisterReceiver(dumpLogsReceiver);
        unregisterReceiver(mUpdateKeyReceiver);
        mErrorLogExtender.onDestroy(this);
        crashReportTimer.stop();
        PerformanceMetricsManager.getInstance().stop();
        mDataMatrix.finish();
    }

    private void init() {
        if (!mIsInitialized) {
            mIsInitialized = true;
            Log.i(TAG, "init!");

            BeltLubeManager.getInstance().start();
            createEventLogFile();
            AccessibilityServiceList.enableAccessibilityServices();

            // Initialize and start the log monitor
            Log.i(TAG, "Starting LogMonitor");
            mLogMonitor = new LogMonitor(this);



            IntentFilter filter = new IntentFilter();
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            registerReceiver(usbMountedReceiver, filter);

            // Initialize the logger database and publish the current state.
            mDataSource = new LoggerDataSource(this);
            updateDataMatrixServiceLog();

            mDataMatrix.createBoolAdapter(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL, value -> checkPersistentLogs());
            mDataMatrix.createIntAdapter(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, value -> checkPersistentLogs());

            // handle ACTION_ADD_SERVICE_PARCELABLE
            mDataMatrix.createParcelableAdapter(LoggerKeys.ACTION_ADD_SERVICE_PARCELABLE, ServiceLog.class, serviceLog -> {
                addServiceLog(serviceLog.serviceType, serviceLog.message);
                updateDataMatrixServiceLog();
            });
            // handle ACTION_DELETE_SERVICE_LOG_PARCELABLE
            mDataMatrix.createParcelableAdapter(LoggerKeys.ACTION_DELETE_SERVICE_LOG_PARCELABLE, ServiceLog.class, serviceLog -> {
                deleteServiceLog(serviceLog);
                updateDataMatrixServiceLog();
            });

            // handle ACTION_ENABLE_ADB_WIFI
            mDataMatrix.createEventAdapter(LoggerKeys.ACTION_ENABLE_ADB_WIFI, keyReturnedBack -> enableAdbWifi());

            // handle ACTION_SHOW_HAL_DLG
            mDataMatrix.createEventAdapter(LoggerKeys.ACTION_SHOW_HAL_DLG, keyReturnedBack -> {
                if (BuildConfig.DEBUG) DebugHalDlg.showDlg();
            });

            // handle ACTION_SHOW_BANDWIDTH_TEST
            mDataMatrix.createEventAdapter(LoggerKeys.ACTION_SHOW_BANDWIDTH_TEST, keyReturnedBack -> BandwidthTestDlg.showDlg());

            // handle ACTION_SAVE_LOGS
            mDataMatrix.createEventAdapter(LoggerKeys.ACTION_SAVE_LOGS, keyReturnedBack -> {
                dumpLogToUsb();
                dumpLogToCloud();
                SystemUtils.playBeep();
                DataMatrix.sendEvent(LoggerKeys.EVENT_LOGS_SAVED);
                Log.d(TAG, "ACTION_SAVE_LOGS");
            });

            mDataMatrix.createEventAdapter(SettingKeys.EVENT_RESET_FACTORY_DEFAULTS, keyReturnedBack -> {
                mErrorLogExtender.deleteAll();
                clearServiceLog();
                AnalyticsManager.getInstance().resetAllData();
                PerformanceMetricsManager.getInstance().resetAllData();
            });

            AnalyticsManager.getInstance().start();
            PerformanceMetricsManager.getInstance().start();

            mDataMatrix.createEventAdapter(LoggerKeys.ACTION_RESET_ANALYTICS, v -> {
                AnalyticsManager.getInstance().resetAllData();
                PerformanceMetricsManager.getInstance().resetAllData();
            });
            mDataMatrix.createEventAdapter(LoggerKeys.ACTION_RESET_PTP_ANALYTICS, v -> AnalyticsManager.getInstance().resetPTPData());
            mDataMatrix.createEventAdapter(LoggerKeys.ACTION_VIEW_ANALYTICS, v -> AnalyticsManager.getInstance().viewStats());
            mDataMatrix.createEventAdapter(LoggerKeys.ACTION_EXPORT_ANALYTICS, v -> AnalyticsManager.getInstance().export());

            if (DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT) == DAPIKeys.Constants.DAPI_DEPLOYMENT_PROD) {
                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_ADB_VIA_NETWORK_BOOL, false);
            }
            mDataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_ADB_VIA_NETWORK_BOOL, v -> {
                if (v) {
                    enableAdbWifi();
                } else {
                    // don't call this on first launch if we haven't enabled adb wifi
                    // so we don't kill the adb connection if we don't need to
                    if (!isAdbWifiEnabled) return;
                    disableAdbWifi();
                }
            });

            // monitor system stats per workout
            // polling the stats seems fast enough so it probably doesn't need to run on a thread
            mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, value -> {
                switch (value) {
                    case WorkoutKeys.Constants.RUN_STATE_RUNNING:
                        // startRecording will not restart a recording if it is already running so we don't have to worry about the pause/resume case
                        systemStats.startRecording(10000); // update every 10 seconds
                        break;
                    case WorkoutKeys.Constants.RUN_STATE_STOP:
                        systemStats.stopRecording();
                        Log.d(TAG, systemStats.analyzeCurrentData());
                        break;
                }
            }, false);
        }
    }

    private void checkPersistentLogs() {
        if(DataMatrix.getBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL)) {
            if(persistentLogAlarm != null) {
                persistentLogAlarm.destroy();
            }
            long persistentLogStartingTime = DataMatrix.getLong(SettingKeys.Debug.DATA_SETTING_PERSIST_LOGGING_START_TIME_LONG);
            if (SystemUtils.isProductionEnvironment()) {
                Calendar persistentStopTime = Calendar.getInstance();
                persistentStopTime.setTimeInMillis(persistentLogStartingTime);
                persistentStopTime.add(Calendar.DAY_OF_MONTH, 14);
                if(persistentStopTime.before(Calendar.getInstance())) {
                    stopPersistentLogs();
                }
                else {
                    persistentLogAlarm = new Alarm(this, "jhtpersistentlogs", persistentStopTime, this::stopPersistentLogs); //NON-NLS
                    persistLog();
                }
            }
            else {
                persistLog();
            }
        }
    }

    private void stopPersistentLogs() {
        DataMatrix.putBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL, false);
    }


    private void scheduleCrashReport(long persistentLogStartingTime) {
        Calendar computeCrashReportCal= Calendar.getInstance();
        Calendar calForExtraction = Calendar.getInstance();
        calForExtraction.setTimeInMillis(persistentLogStartingTime);
        // Use the persistent logging "turn on" time for setting the HH:MM:SS of the alarm
        computeCrashReportCal.set(Calendar.HOUR_OF_DAY, calForExtraction.get(Calendar.HOUR_OF_DAY));
        computeCrashReportCal.set(Calendar.MINUTE, calForExtraction.get(Calendar.MINUTE));
        computeCrashReportCal.set(Calendar.SECOND, calForExtraction.get(Calendar.SECOND));

        long timeout = computeCrashReportCal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        if (timeout < 0) timeout += AlarmManager.INTERVAL_DAY;

        long seconds = timeout/1000;
        long minutes = seconds/60;
        long hours = minutes/60;
        Log.d(TAG, "Scheduling persistent daily crash report alarm in  H" + hours + ":M" + minutes % 60);

        //long developerTimerToMakeTestingEasier = 60 * 10;    // 10 minute timer
        // crashReportTimer = new JHTTimer((int) (developerTimerToMakeTestingEasier), () -> {
        crashReportTimer = new JHTTimer((int) (timeout), () -> {
            Log.d (TAG, "Daily alarm triggered to create persistent crash report ...");
            mLogMonitor.createManualCrashEntry();
        });
        crashReportTimer.start();
    }

    /*
    private void deletePersistentLogs() {
        try {
            // delete the persistent logs
            CmdProcessHelper cmdProcessHelper = new CmdProcessHelper();
            cmdProcessHelper.addCommand("rm -f /sdcard/log/log.tx*");
            cmdProcessHelper.execute();
        }
        catch (Exception ignored) { }
    }
     */

    /**
     * Resolves a Parcelable class by name for the debug broadcasts.
     * Restricted to JHT types so a broadcast cannot load arbitrary classes off the classpath.
     *
     * @return null when the name is missing, unknown, or not a Parcelable
     */
    private static Class<? extends Parcelable> parcelableClass(String className) {
        if (className == null || !className.startsWith("com.jht.")) {
            Log.w(TAG, "Parcelable broadcasts need a com.jht class name, got: " + className);
            return null;
        }
        try {
            return Class.forName(className).asSubclass(Parcelable.class);
        } catch (ClassNotFoundException | ClassCastException e) {
            Log.w(TAG, "Not a Parcelable: " + className, e);
            return null;
        }
    }

    /**
     * Debug Broadcast that can be used to update the DataMatrix primitive key.
     */
    BroadcastReceiver mUpdateKeyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            final String key = intent.getStringExtra(DATA_MATRIX_KEY);
            final String type = intent.getStringExtra(DATA_MATRIX_TYPE);
            final String value = intent.getStringExtra(DATA_MATRIX_VALUE);

            if (ACTION_CONSOLE_REBOOT.equals(intent.getAction())) { // could we clean this up with a switch block and break these things into private sub functions?
                String reason = intent.getStringExtra("reason");
                DataMatrix.putString(HALKeys.ACTION_CONSOLE_REBOOT_STRING, reason);
                return;
            }

            if (ACTION_UPDATE_TEST_AUTOMATION_MODE.equals(intent.getAction())) {
                Log.d(TestingUtils.TAG, "Trying to set the value to " + value);
                if (value == null || value.isEmpty()) return;
                TestingUtils.setAutomationEnabled(context, Boolean.parseBoolean(value));
                return;
            }

            if (key == null || key.isEmpty()) return;
            if (type == null || type.isEmpty()) return;

            if (ACTION_UPDATE_DATA_MATRIX_KEY.equals(intent.getAction())) {
                Log.i(TAG, "Update key=" + key + ", type=" + type + ", value=" + StringUtil.quote(value));
                switch (type) {
                    case TYPE_STRING:
                        if (value == null) return;
                        DataMatrix.putString(key, value);
                        break;

                    case TYPE_BOOLEAN:
                        if (value == null || value.isEmpty()) return;
                        DataMatrix.putBoolean(key, value.compareToIgnoreCase("true") == 0);
                        break;

                    case TYPE_DOUBLE:
                        if (value == null || value.isEmpty()) return;
                        DataMatrix.putDouble(key, Double.parseDouble(value));
                        break;

                    case TYPE_LONG:
                        if (value == null || value.isEmpty()) return;
                        DataMatrix.putLong(key, Long.parseLong(value));
                        break;

                    case TYPE_INTEGER:
                        if (value == null || value.isEmpty()) return;
                        DataMatrix.putInt(key, Integer.parseInt(value));
                        break;

                    case TYPE_FLOAT:
                        if (value == null || value.isEmpty()) return;
                        DataMatrix.putFloat(key, Float.parseFloat(value));
                        break;

                    case TYPE_PARCELABLE: {
                        if (value == null) return;
                        Class<? extends Parcelable> cls = parcelableClass(intent.getStringExtra(DATA_MATRIX_CLASS));
                        if (cls == null) return;
                        try {
                            DataMatrix.putParcelable(key, new Gson().fromJson(value, cls));
                        } catch (JsonSyntaxException e) {
                            Log.w(TAG, "Bad JSON for " + cls.getName(), e);
                        }
                        break;
                    }

                    case TYPE_EVENT:
                        DataMatrix.sendEvent(key);
                        break;
                }
            }

            // we suppress log.i and up in release mode, so need a log.w for dqa
            // these are only ever triggered manually from a script call so a higher level log
            // is perfectly acceptable to me.
            if (ACTION_LOG_DATA_MATRIX_KEY.equals(intent.getAction())) {
                switch (type) {
                    case TYPE_STRING:
                        Log.w(TAG, key + "=" + DataMatrix.getString(key, ""));
                        break;

                    case TYPE_BOOLEAN:
                        Log.w(TAG, key + "=" + DataMatrix.getBoolean(key, false));
                        break;

                    case TYPE_DOUBLE:
                        Log.w(TAG, key + "=" + DataMatrix.getDouble(key, -1));
                        break;

                    case TYPE_LONG:
                        Log.w(TAG, key + "=" + DataMatrix.getLong(key, -1));
                        break;

                    case TYPE_INTEGER:
                        Log.w(TAG, key + "=" + DataMatrix.getInt(key, -1));
                        break;

                    case TYPE_FLOAT:
                        Log.w(TAG, key + "=" + DataMatrix.getFloat(key, -1));
                        break;

                    case TYPE_STRING_LIST:
                        Log.w(TAG, key + "=" + DataMatrix.getStringList(key));
                        break;

                    case TYPE_PARCELABLE: {
                        Class<? extends Parcelable> cls = parcelableClass(intent.getStringExtra(DATA_MATRIX_CLASS));
                        if (cls == null) return;
                        Log.w(TAG, key + "=" + new Gson().toJson(DataMatrix.getParcelable(key, cls)));
                        break;
                    }
                }
            }
        }
    };

    protected void dumpLogToUsb() {
        final File logPath = SystemUtils.getLogUSBPath(Calendar.getInstance());
        if (logPath == null) {
            Log.w(TAG, "Aborting dumpLogToUsb.");
            return;
        }

        mLogMonitor.updateExtraLogFiles();
        mLogMonitor.copyLogFilesToDir(Paths.get(logPath.getAbsolutePath()));

        ExecuteAsRoot root = new ExecuteAsRoot();
        root.addCommand("logcat -d -v threadtime -v year > " + logPath.getAbsolutePath() + "/logcat.txt");
        root.addCommand("dmesg > " + logPath.getAbsolutePath() + "/dmesg.txt");
        root.addCommand("cp -rf /sdcard/log " + logPath.getAbsolutePath() + "/");
        //root.addCommand("dmesg > " + logPath.getAbsolutePath() + "/dmesg.txt");
        root.addCommand("cp -rf /data/anr/ " + logPath.getAbsolutePath() + "/anr");
        root.addCommand("cp -rf /data/tombstones/ " + logPath.getAbsolutePath() + "/tombstones");
        //root.addCommand("screencap -p " + logPath.getAbsolutePath() + "/screenshot.png"); // updateExtraLogFiles already adds this?
        root.addCommand("sync");
        root.execute();
    }

    protected void dumpLogToCloud() {
        mLogMonitor.createManualCrashEntry();
    }

    private final ExecuteAsRoot logExe = new ExecuteAsRoot();

    protected void persistLog() {
        if (mIsPersistLogProcessRunning) return;
        mIsPersistLogProcessRunning = true;

        logExe.addCommand("mkdir -p /sdcard/log");
        logExe.addCommand("kill $(ps -Af | grep f/sdcard/log/log.txt | awk '{print $2}')");
        logExe.addCommand("kill $(ps -Af | grep f/sdcard/log/dmesg.txt | awk '{print $2}')");
        logExe.addCommand("rm /sdcard/log/dmesg2.txt");
        logExe.addCommand("mv /sdcard/log/dmesg.txt /sdcard/log/dmesg2.txt");
        logExe.addCommand("logcat -n10 -vthreadtime -r8192 -f/sdcard/log/log.txt &");
        logExe.addCommand("dmesg -w > /sdcard/log/dmesg.txt &");
        logExe.execute(false, true, null);

        scheduleCrashReport (DataMatrix.getLong(SettingKeys.Debug.DATA_SETTING_PERSIST_LOGGING_START_TIME_LONG));
    }

    protected void dumpScreenCaptureToUsb() {
        final Calendar now = Calendar.getInstance();
        final File logPath = SystemUtils.getLogUSBPath(now);
        if (logPath == null) {
            Log.w(TAG, "Aborting dumpScreenCaptureToUsb.");
            return;
        }

        final String fileName = "screen_" + SystemUtils.calendarToFilename(now) + ".png";

        ExecuteAsRoot root = new ExecuteAsRoot();
        root.addCommand("screencap -p " + logPath.getAbsolutePath() + "/" + fileName);
        root.execute();

    }

    protected void dumpVideoCaptureToUsb(int seconds) {
        final Calendar now = Calendar.getInstance();
        final File logPath = SystemUtils.getLogUSBPath(now);
        if (logPath == null) {
            Log.w(TAG, "Aborting dumpVideoCaptureToUsb.");
            return;
        }
        final String fileName = "video_" + SystemUtils.calendarToFilename(now) + ".mp4";

        // Screen recording adb util has a max of three minutes
        seconds = Math.min (seconds, 180);
        ExecuteAsRoot root = new ExecuteAsRoot();
        root.addCommand("screenrecord --size 1920x1080 --time-limit " + seconds + " " + logPath.getAbsolutePath() + "/" + fileName);

        Log.d (TAG, "Screen Recording " + fileName);
        mHandler.post(() -> {
            Toast.makeText(getApplicationContext(), "Starting 3 Minute Video Recording into " + fileName, Toast.LENGTH_LONG).show(); //NON-NLS
        });

        // Approximate the 3 minute end time
        mHandler.postDelayed(() -> {
            SystemUtils.playBeep();
            Log.d (TAG, "Screen Recording Done " + fileName);
            Toast.makeText(getApplicationContext(), "Ending 3 Minute Video Recording into " + fileName, Toast.LENGTH_LONG).show(); //NON-NLS
            }, seconds * 1000L + 10L);
        SystemUtils.playBeep();
        root.execute();
    }

    private final BroadcastReceiver usbMountedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                DataMatrix.putBoolean(LoggerKeys.DATA_USB_STICK_ATTACHED_BOOL, true);
                // increment ACTION_USB_DEVICE_ATTACHED count
                int count = DataMatrix.getInt(SettingKeys.Hardware.DATA_USB_ATTACH_COUNT_INT);
                DataMatrix.putInt(SettingKeys.Hardware.DATA_USB_ATTACH_COUNT_INT, ++count);

                AnalyticsManager.getInstance().onUSBAttachedEvent();
                File usbPath = StorageUtils.getUSBPath();

                Thread scanForDebugTxt = new Thread(() -> {

                    File[] files = usbPath.listFiles();
                    int tries = 0;
                    while (null == files && tries < 20) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception ignored) {
                        }

                        files = usbPath.listFiles();
                        tries++;
                    }

                    if (null != files) {
                        boolean beepNeeded = false;
                        for (File inFile : files) {

                            if (inFile.getName().equalsIgnoreCase("jht_screen_capture.txt")) {
                                dumpScreenCaptureToUsb();
                                beepNeeded = true;
                                continue;
                            }

                            if (inFile.getName().equalsIgnoreCase("jht_video_capture.txt")) {
                                dumpVideoCaptureToUsb(180);
                                beepNeeded = true;
                                continue;
                            }

                            if (inFile.getName().equalsIgnoreCase("jht_debug.txt")) {
                                dumpLogToUsb();
                                dumpLogToCloud();
                                beepNeeded = true;
                                continue;
                            }

                            // This is for the retail machines from Costco
                            if (inFile.getName().equalsIgnoreCase("jht_block_ifit.txt")) {
                                if (ProductConfig.supportsIfitBlocking()) {
                                    if (!ProductConfig.isIfitBlocked()) {
                                        Log.w(TAG, "BLOCKING IFIT via marker");
                                        ExecuteAsRoot root = new ExecuteAsRoot();
                                        root.addCommand("touch " + ProductConfig.getMarkerFilePathIfitBlocked());
                                        root.addCommand("sync");
                                        root.execute();
                                        LaunchAppUtil.launchSplashScreen(); // go back to splash so ifit can be hidden
                                        beepNeeded = true;
                                    }
                                }

                                continue;
                            }

                            // This is for the retail machines outside North America
                            if (inFile.getName().equalsIgnoreCase("jht_block_ifit_outside_north_america.txt")) {
                                if (ProductConfig.supportsIfitBlocking()) {
                                    if (!ProductConfig.isIfitBlockedOutsideNorthAmerica()) {
                                        Log.w(TAG, "BLOCKING IFIT via marker outside NA");
                                        ExecuteAsRoot root = new ExecuteAsRoot();
                                        root.addCommand("touch " + ProductConfig.getMarkerFilePathIfitBlockedOutsideNA());
                                        root.addCommand("sync");
                                        root.execute();
                                        LaunchAppUtil.launchSplashScreen(); // go back to splash so ifit can be hidden
                                        beepNeeded = true;
                                    }
                                }
                                continue;
                            }

                            if (inFile.getName().equalsIgnoreCase("jht_dashboard.txt")) {
                                mHandler.post(ProductDashboardHost::show);
                                beepNeeded = true;
                                continue;
                            }

                            if (inFile.getName().equalsIgnoreCase("jht_run_launcher.txt")) {
                                Log.d(TAG, "run launcher");
                                LaunchAppUtil.launchAndroidLauncher();
                                continue;
                            }

                            if (inFile.getName().equalsIgnoreCase("jht_reset_passcode.txt")) {
                                Log.d(TAG, "reset passcode");
                                DataMatrix.putString(SettingKeys.Experimental.DATA_SETTINGS_LOCKOUT_PASSCODE_STRING, "");
                                DataMatrix.putLong(SettingKeys.Experimental.DATA_SETTINGS_LOCKOUT_TIMESTAMP_LONG, 0);
                                beepNeeded = true;
                            }
                        }
                        if (beepNeeded) SystemUtils.playBeep();
                    }
                });
                scanForDebugTxt.start();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                DataMatrix.putBoolean(LoggerKeys.DATA_USB_STICK_ATTACHED_BOOL, false);
                        //ACTION_SCREEN_RECORD_INT,);
            }
        }
    };

    private void enableAdbWifi() {
        Log.d(TAG, "enableAdbWifi");
        if(!SystemPropertyUtil.get("service.adb.tcp.port").equals("5555")) { //NON-NLS
            SystemPropertyUtil.set("service.adb.tcp.port", "5555");
            SystemPropertyUtil.set("ctl.restart", "adbd");
        }

        isAdbWifiEnabled = true;
    }

    private void disableAdbWifi() {
        Log.d(TAG, "disableAdbWifi");

        if(!SystemPropertyUtil.get("service.adb.tcp.port").equals("")) {
            SystemPropertyUtil.set("service.adb.tcp.port", "");
            SystemPropertyUtil.set("ctl.restart", "adbd");
        }
        isAdbWifiEnabled = false;
    }

    public void addServiceLog(int serviceType, String message) {
        ServiceLog log = new ServiceLog();
        log.accumulatedDistanceKm = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT);
        log.accumulatedTimeSec = DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT);
        log.accumulatedFloor = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT);
        log.serviceType = serviceType;
        log.message = message;

        mDataSource.addServiceLog(log);
    }

    public void clearServiceLog() {
        mDataSource.clearServiceLog();
    }

    public void deleteServiceLog(ServiceLog log) {
        mDataSource.deleteServiceLog(log);
    }

    public ServiceLog[] getServiceLog() {
        return mDataSource.getServiceLog();
    }

    public void updateDataMatrixServiceLog() {
        ServiceLogList serviceLogList = new ServiceLogList(getServiceLog());
        DataMatrix.putParcelable(LoggerKeys.DATA_SERVICE_LOG_LIST_PARCELABLE, serviceLogList);
    }

    void createEventLogFile() {
        try {
            mEventLog = new FileHandler(getFilesDir().getAbsolutePath() + "/" + EVENT_LOG_FILENAME, 1024 * 1024, 5, true);
            mEventLog.setFormatter(new java.util.logging.Formatter() {
                @Override
                public String format(LogRecord r) {
                    return r.getMessage();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Log the click event to the rotating log.
     *
     * @param viewId the view that was clicked.
     */
    void saveClickEvent(final String viewId) {
        String stringBuffer = "<event time=\"" + // NON-NLS
                Calendar.getInstance().getTime().getTime() / 1000 + // NON-NLS
                "\" type=\"click\" id=\"" + // NON-NLS
                viewId + // NON-NLS
                "\" />\n"; // NON-NLS
        LogRecord record = new LogRecord(Level.INFO, stringBuffer);
        mEventLog.publish(record);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //Log.e(TAG, "==> onAccessibilityEvent: " + event);
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String className = event.getClassName() != null ? event.getClassName().toString() : null;
            if (className != null && !className.contains("android.widget")) {
                AnalyticsManager.getInstance().onAccessibilityWindowEvent(event);
            }
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            try {
                AccessibilityNodeInfo source = event.getSource();
                String buttonId;
                if (source == null) {
                    buttonId = event.getPackageName().toString() + "|" + event.getText();
                } else {
                    String viewIdResourceName = source.getViewIdResourceName();
                    if (viewIdResourceName != null) {
                        buttonId = viewIdResourceName + "|" + event.getText();
                    } else {
                        buttonId = event.getPackageName().toString() + "|" + event.getText();
                    }
                }

                //Log.e(TAG, "==> CLICK: " + buttonId);
                saveClickEvent(buttonId);
                AnalyticsManager.getInstance().onAccessibilityClickEvent(buttonId);
            } catch (IllegalStateException ignored) {
                // We can get a already in pool exception.  Just ignore and move on with life.
            }
        }
    }

    @Override
    public void onInterrupt() {}
}
