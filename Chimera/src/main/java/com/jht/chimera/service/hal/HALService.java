package com.jht.chimera.service.hal;

import static com.jht.chimera.service.hal.lib.SntpClient.NTP_PORT;
import static com.jht.utility.UtilityKt.unregisterReceiverSafe;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androidcommonalgorithms.util.Strings;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.SystemBrightness;
import com.jht.chimera.lib.common.SystemPropertyUtil;
import com.jht.chimera.lib.common.VersionHelper;
import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.config.ConsoleType;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.lib.common.log.ConsoleEvent;
import com.jht.chimera.lib.widget.network.WifiConfigWrapper;
import com.jht.chimera.lib.widget.network.WifiManagerHelper;
import com.jht.chimera.service.hal.lib.SntpClient;
import com.jht.chimera.service.hal.lib.hardware.AudioBeeper;
import com.jht.chimera.service.hal.lib.hardware.AudioSelectManager;
import com.jht.chimera.service.hal.lib.hardware.BluetoothHeadphoneManager;
import com.jht.chimera.service.hal.lib.hardware.BluetoothManager;
import com.jht.chimera.service.hal.lib.hardware.BumpSensorManager;
import com.jht.chimera.service.hal.lib.hardware.CSafeManager;
import com.jht.chimera.service.hal.lib.hardware.DisplayAdjustment;
import com.jht.chimera.service.hal.lib.hardware.GymKitManager;
import com.jht.chimera.service.hal.lib.hardware.HeadphoneManager;
import com.jht.chimera.service.hal.lib.hardware.KeypadManager;
import com.jht.chimera.service.hal.lib.hardware.LineInManager;
import com.jht.chimera.service.hal.lib.hardware.OverCurrentAutoManager;
import com.jht.chimera.service.hal.lib.hardware.OverCurrentDCbusManager;
import com.jht.chimera.service.hal.lib.hardware.OverCurrentErrorCodeManager;
import com.jht.chimera.service.hal.lib.hardware.OverCurrentTM548DCbusManager;
import com.jht.chimera.service.hal.lib.hardware.OverCurrentType;
import com.jht.chimera.service.hal.lib.hardware.PowerLimiter;
import com.jht.chimera.service.hal.lib.hardware.RfidReaderManager;
import com.jht.chimera.service.hal.lib.hardware.SafetyKeyManager;
import com.jht.chimera.service.hal.lib.hardware.TouchEventManager;
import com.jht.chimera.service.hal.lib.hardware.USBMountManager;
import com.jht.chimera.service.hal.lib.hardware.VirtualKeyboardManager;
import com.jht.chimera.service.hal.lib.hardware.WifiSignalManager;
import com.jht.chimera.service.hal.lib.hardware.heartrate.HeartRateManager;
import com.jht.chimera.service.workout.SafeToStartFlags;
import com.jht.chimera.service.hal.lib.util.NtpServerUtils;
import com.jht.chimera.ui.bluetooth.HeadphoneDeviceManager;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gymkitmodule.comm.Crc8;
import com.jht.rfid.Legic.RFIDChip;
import com.jht.rfid.LegicEnums.RfStandard;
import com.jht.serialport.SerialPort;
import com.jht.serialport.SerialPortConfiguration;
import com.jht.services.ChildConsoleService;
import com.jht.services.ConsoleServiceState;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

public class HALService extends ChildConsoleService {

    private final String TAG = "HALService";
    private final DataMatrix mDataMatrix = new DataMatrix();
    private final int MS_IN_DAY = 1000 * 60 * 60 * 24;
    private Handler handler;
    private OverCurrentType mOCtype = OverCurrentType.OC_UNKNOWN;
    private OverCurrentAutoManager mOverCurrentAutoManager;
    private OverCurrentErrorCodeManager mOverCurrentErrorCodeManager;
    private OverCurrentDCbusManager mOverCurrentDCbusManager;
    private OverCurrentTM548DCbusManager mOverCurrentTM548DCbusManager;
    private TvTunerManager tvTunerManager;
    private RfidReaderManager rfidReaderManager;

    FrameConfig mFrameConfig;
    private PowerManager.WakeLock wl = null;
    private ConnectivityManager connectivityManager;

    private NetworkTimeUpdateCallback mNetworkTimeUpdateCallback;
    private long mCachedNtpTime;
    private long mCachedNtpElapsedRealtime;
    // Array of Network Time Protocol (NTP) servers used for time synchronization, cleaned up due to weird time from one of the servers 2/28/2025
    private final String[] ntpServerHost = new String[]{
            "time.apple.com", "time.windows.com", "time.google.com", "time.android.com", "time.aws.com"
    };

    private final BroadcastReceiver findDevices = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            findUSBDevices();
        }
    };

    private final BroadcastReceiver osErpEvent = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DataMatrix.sendEvent(HALKeys.ACTION_ENTER_ERP);
        }
    };

    private boolean showingSoftwareUpdate = false;
    private final BroadcastReceiver softwareUpdateEvent = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                showingSoftwareUpdate = true;
                updateSafeToStartWorkout();
                Log.w(TAG, "RECEIVED SOFTWARE UPDATE EVENT! Workouts will be locked out!");
            }
        }
    };

    private final BroadcastReceiver rfidSimulator = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("rfid_scan")){
                try {
                    String[] rfid = intent.getStringExtra("rfid_scan").split(",");
                    if (rfid.length < 2) return;
                    RfStandard rfStandard = RfStandard.fromByte(Byte.parseByte(rfid[0]));
                    byte[] bytes = Strings.hexStringToByteArray(rfid[1]);
                    RFIDChip rfidChip = new RFIDChip(rfStandard, bytes);
                    Log.d(TAG, "Simulating RFID Scan: " + rfidChip);
                    DataMatrix.putParcelable(HALKeys.EVENT_RFID_TAG_DETECTED_PARCELABLE, rfidChip);
                } catch (Exception e) {
                    Log.e(TAG, "Error simulating RFID tag: " + e);
                }
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        Log.d("ServiceStarter", "Starting " + this);
        mFrameConfig = FrameConfigManager.getInstance().getFrameConfig();

        if(BuildConfig.HAS_TV) {
            tvTunerManager = new TvTunerManager(getApplicationContext());
            tvTunerManager.start();
        }

        try {
            init();
        }
        catch (Exception ex)
        {
            Log.e (TAG, "", ex);
        }
        updateMyServiceState(ConsoleServiceState.READY);
        Log.d("ServiceStarter", "Ending " + this);

        // Note we need to take this out when we get a corrected OS from Nexcom.  The following
        // keeps the screen from dimming after playing a video.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //noinspection deprecation
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Chimera:"); //NON-NLS
        wl.acquire(10*60*1000L /*10 minutes*/);

        // Turning on Wifi.  Sometimes after an OS update the Wifi gets turned off.
        if(!DataMatrix.getBoolean(SettingKeys.Experimental.DATA_DISABLE_WIFI_BOOL)) {
            WifiManagerHelper.TurnWifiOn(this);
        }

        registerReceiver(findDevices, new IntentFilter("com.jht.find.devices"));
        registerReceiver(osErpEvent, new IntentFilter("android.intent.action.ACTION_ENTER_ERP"));
        registerReceiver(rfidSimulator, new IntentFilter("com.jht.rfid_sim"));
        Intent updateIntent = registerReceiver(softwareUpdateEvent, new IntentFilter("com.jht.update_started_intent"));
        if (updateIntent != null) {
            Log.d(TAG, "Found sticky software update event intent"); // sticky being deprecated, should we look at other mechanisms?
            softwareUpdateEvent.onReceive(this, updateIntent);
        }

        mDataMatrix.createEventAdapter(HALKeys.EVENT_HAL_HEARTBEAT_CHECK, event -> DataMatrix.sendEvent(HALKeys.EVENT_HAL_HEARTBEAT_RESPONSE));
    }

    @Override
    public void onDestroy() {
        mDataMatrix.finish();
        if (wl != null) wl.release();
        if (connectivityManager != null) connectivityManager.unregisterNetworkCallback(mNetworkTimeUpdateCallback);
        if (rfidReaderManager != null) rfidReaderManager.stop();
        if (ProductConfig.hasGymkit()) GymKitManager.getInstance().stop();
        if (ProductConfig.hasBumpSensor()) BumpSensorManager.getInstance().stop();
        if (tvTunerManager != null) tvTunerManager.stop();
        unregisterReceiverSafe(this, softwareUpdateEvent);
        unregisterReceiverSafe(this, findDevices);
        unregisterReceiverSafe(this, osErpEvent);
        unregisterReceiverSafe(this, rfidSimulator);
        super.onDestroy();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void init() {
        // make sure to reset the version strings first which will be updated later if the hardware is found which is used by the software updater
        AndroidSetting.putString(getContentResolver(), "chimera_bluetooth_version", "N/A"); //NON-NLS
        AndroidSetting.putString(getContentResolver(), "chimera_rfid2_version", "N/A"); //NON-NLS
        AndroidSetting.putString(getContentResolver(), "chimera_rfid3_version", "N/A"); //NON-NLS
        AndroidSetting.putString(getContentResolver(), "chimera_rfid4_version", "N/A"); //NON-NLS
        AndroidSetting.putString(getContentResolver(), "chimera_salutron_version", "N/A"); //NON-NLS
        AndroidSetting.putString(getContentResolver(), "chimera_bumpsensor_version", "N/A"); //NON-NLS
        AndroidSetting.putString(getContentResolver(), "can_single_version", "N/A"); //NON-NLS
        AndroidSetting.putString(getContentResolver(), "can_right_version", "N/A"); //NON-NLS
        AndroidSetting.putString(getContentResolver(), "can_left_version", "N/A"); //NON-NLS

        setupDataMatrixCallbacks();

        // update gui and os version
        String versionTextMainApp = "";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            versionTextMainApp += pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e);
        }
        DataMatrix.putString(HALKeys.DATA_VERSION_CHIMERA_STRING, versionTextMainApp);

        String osVersion = VersionHelper.getOsVersion();
        if (!osVersion.isEmpty()) {
            DataMatrix.putString(HALKeys.DATA_VERSION_OS_STRING, osVersion);
        }

        WifiSignalManager.getInstance().start();
        USBMountManager.getInstance().start();
        HeadphoneManager.getInstance().start();
        HeartRateManager.getInstance().start(this);
        VirtualKeyboardManager.getInstance().start();

        if (ProductConfig.isRealDevice()) AudioSelectManager.getInstance().start();

        TouchEventManager.getInstance().start(() -> DataMatrix.sendEvent(HALKeys.EVENT_SCREEN_TOUCH));

        if (ProductConfig.hasRFID()) {
            rfidReaderManager = new RfidReaderManager(this);
            rfidReaderManager.start(ProductConfig.portMappings.rfidPath());
        }

        if (ProductConfig.hasGymkit()) GymKitManager.getInstance().start(ProductConfig.portMappings.bluetoothSinkPath());
        if (ProductConfig.hasBumpSensor()) BumpSensorManager.getInstance().start();

        if (ProductConfig.hasCSAFE()) CSafeManager.getInstance().start();
        if (ProductConfig.hasBluetoothManager()) BluetoothManager.getInstance().start();
        BluetoothHeadphoneManager.getInstance().start();
        if(ProductConfig.platform.equals(ProductConfig.Platform.LCGUI)) {
            findUSBDevices();
        }
        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker) || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
            LineInManager.getInstance().start();

            // originally from parker, should fail gracefully if "driver" cannot be loaded or "/dev/safe_lock" cannot be found
            SafetyKeyManager.getInstance().start("/dev/safe_lock", isPulled -> { //NON-NLS
                DataMatrix.putBoolean(HALKeys.DATA_IO_IS_SAFETY_PULLED_BOOL, isPulled);
            });

            // handle case where machine is not setup and DATA_IO_IS_SAFETY_PULLED_BOOL is sent but LCB is not configured as TM so it ignores event
            mDataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, value -> {
                if (value) {
                    DataMatrix.putBoolean(HALKeys.DATA_IO_IS_SAFETY_PULLED_BOOL, SafetyKeyManager.getInstance().isPulled());
                }
            });

            KeypadManager.getInstance().start((physicalKey, value) -> {
                FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
                KeyPress.KeyCode keyCode = KeypadManager.getKeyCode(frameConfig.getKeyMap(), physicalKey);
                KeyPress.KeyState keyState = (value == 0 ? KeyPress.KeyState.KEY_UP : KeyPress.KeyState.KEY_DOWN);
                int bitmapKey = KeyPress.createBitmapKey(keyCode, keyState);
                DataMatrix.putInt(HALKeys.EVENT_KEYPRESS_BITMAP_INT, bitmapKey);

                if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_KEY_BEEP_BOOL) && keyState == KeyPress.KeyState.KEY_DOWN) {
                    DataMatrix.sendEvent(HALKeys.ACTION_IO_PLAY_BEEP);
                }
            }, false);

            if (ProductConfig.isCommercial()) { // This is for LCGUI
                ExecuteAsRoot.run("tinymix \"HP Playback Volume\" 33 33");
                ExecuteAsRoot.run("tinymix \"IN Capture Volume\" 30 30");
            }
        }

        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, value -> wakeConsoleIfAsleep());

        // EVENT_SCREEN_TOUCH is sent by Onyx toggles
        mDataMatrix.createEventAdapter(HALKeys.EVENT_SCREEN_TOUCH, key -> wakeConsoleIfAsleep());

        mDataMatrix.createIntAdapter(SettingKeys.Hardware.DATA_DISPLAY_COLOR_TEMPERATURE_INT, value -> {
            try {
                DisplayAdjustment displayAdjustment = DisplayAdjustment.create();
                displayAdjustment.setColorTemperature(value);
            } catch (Exception e) {
                Log.e(TAG, "DisplayAdjustment failure", e);
            }
        }, true);

        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.UltraThin)) {
            SystemPropertyUtil.set("persist.CVT_EN_POWER_KEY_TO_APP", "1"); // 0 = ERP directly, 1 = uses "android.intent.action.ACTION_ENTER_ERP"
        }

        DataMatrix.putBoolean(HALKeys.DATA_HAL_SERVICE_READY_BOOL, true);

        // Turn off swap.  If swap runs our performance dies.
        ExecuteAsRoot.run("swapoff /dev/block/zram0"); //NON-NLS

        if (connectivityManager == null) {
            connectivityManager = getApplicationContext().getSystemService(ConnectivityManager.class);
        }
        updateTime();

        mNetworkTimeUpdateCallback = new NetworkTimeUpdateCallback();
        connectivityManager.registerDefaultNetworkCallback(mNetworkTimeUpdateCallback);

        // Only start the HeadphoneDeviceManager for commercial that will disconnect unwanted device
        if (ProductConfig.isCommercial()) HeadphoneDeviceManager.INSTANCE.start();

    }

    private void wakeConsoleIfAsleep() {
        if (ProductConfig.hasSleepMode() && !isInteractive()) {
            ExecuteAsRoot.simulatePowerButton(); // if console asleep, wake it
        }
    }

    private void setTime(SntpClient client, AlarmManager alarmManager) {
        try {
            mCachedNtpTime = client.getNtpTime();
            mCachedNtpElapsedRealtime = client.getNtpTimeReference();

            long now = mCachedNtpTime + SystemClock.elapsedRealtime() - mCachedNtpElapsedRealtime;

            Date current = new Date(now);
            Log.d(TAG, "updateTime: from ntp " + current.toString());

            alarmManager.setTime(now);
            Log.d(TAG, "updateTime: the time after update: " + Calendar.getInstance().getTime());
        } catch (Exception e) {
            Log.e(TAG, "setTime Exception", e);
        }
    }

    private void setInternetAvailable() {
        // WifiSignalManager has a 2-second delay before updating the status on boot.
        // Update the network connectivity to true after NTP has succeeded which proves that internet works.
        DataMatrix.updateBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL, true);
    }

    // Update time when HALService initial, when auto-update is enabled, and when the network change
    boolean isUpdateTimeRunning = false;
    long lastUpdateTimeMs = 0;
    private void updateTime(){
        // don't sync time if the auto date/time is false
        if(!DataMatrix.getBoolean(SettingKeys.General.DATA_AUTO_DATE_TIME_BOOL, true)) return;
        // return if thread is already running
        if (isUpdateTimeRunning) return;
        AlarmManager am = (AlarmManager) ContextUtil.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        // 1735689600000L Jan 1 2025 2997993600000L Jan 1 2065  This is a hard fix for dates that are past 2100 that NTP can't handle OC-259
        if (System.currentTimeMillis() > 2997993600000L ) am.setTime(1735689600001L);
        // return if it hasn't been more than 5 seconds since the last check
        if (SystemClock.uptimeMillis() - lastUpdateTimeMs < 5000) return;
        new Thread(() -> {
            isUpdateTimeRunning = true;

            // TODO: currentTime appears to be used as a timestamp for logs. Is this necessary given logs already contain a timestamp?
            Date currentTime = Calendar.getInstance().getTime();
            Log.d(TAG, "updateTime: the time before update: " + currentTime);

            if (connectivityManager == null) {
                connectivityManager = getApplicationContext().getSystemService(ConnectivityManager.class);
            }

            Network network = connectivityManager == null ? null : connectivityManager.getActiveNetwork();
            boolean networkConnected = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                networkConnected = connectivityManager.getNetworkCapabilities(network) == null ? false : true;
                Log.d(TAG, "updateTime: NetworkCapabilities " + networkConnected);
            }
            else{
                NetworkInfo ni = connectivityManager == null ? null : connectivityManager.getNetworkInfo(network);
                networkConnected = ni != null && ni.isConnected();
                Log.d(TAG, "updateTime: NetworkInfo " + networkConnected);
            }

            SntpClient client = new SntpClient();

            if (!networkConnected) {
                Log.d(TAG, "updateTime: no connectivity");
            }
            else {

                lastUpdateTimeMs = SystemClock.uptimeMillis();

                int timeout = 5000;

                List<InetAddress> ntpServersOverDHCP = NtpServerUtils.INSTANCE.getNtpServersOverDHCP();
                if (!ntpServersOverDHCP.isEmpty()) {
                    for (InetAddress address : ntpServersOverDHCP) {
                        if (client.requestTime(address, NTP_PORT, timeout, network)) {
                            setTime(client, am);
                            setInternetAvailable();
                            break;
                        }
                    }
                } else {

                    for (String hostname : ntpServerHost) {
                        if (client.requestTime(hostname, timeout, network)) {
                            setTime(client, am);
                            setInternetAvailable();
                            break;
                        }
                    }
                }
            }
            isUpdateTimeRunning = false;

        }, "HalNtp").start();

    }

    private class NetworkTimeUpdateCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            Log.d(TAG, String.format("New default network %s; checking time.", network));
            updateTime();
        }
        @Override
        public void onUnavailable() {
            Log.d(TAG, "Network unavailable");
        }
        @Override
        public void onLost(@NonNull Network network) {
            Log.d(TAG, "Network lost " + network);
        }
    }

    private void setupDataMatrixCallbacks() {
        mDataMatrix.createEventAdapter(SettingKeys.EVENT_RESET_FACTORY_DEFAULTS, keyReturnedBack -> resetFactoryDefault());
        mDataMatrix.createBoolAdapter(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL, aBoolean -> updateSafeToStartWorkout(), false);
        mDataMatrix.createBoolAdapter(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, demoModeEnabled -> {
            updateSafeToStartWorkout();
            if (ProductConfig.isCommercial() && !demoModeEnabled) {
                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL, true);
            }
        });
        mDataMatrix.createIntAdapter(HALKeys.DATA_RPM_INT, integer -> updateSafeToStartWorkout(), false);
        mDataMatrix.createIntAdapter(HALKeys.DATA_SPM_INT, integer -> updateSafeToStartWorkout(), false);
        mDataMatrix.createBoolAdapter(HALKeys.DATA_MAIN_MOTOR_STATUS_BOOL, aBoolean -> updateSafeToStartWorkout(), false);
        mDataMatrix.createBoolAdapter(HALKeys.DATA_RPM_ENCODER_STATUS_BOOL, aBoolean -> updateSafeToStartWorkout(), false);
        mDataMatrix.createBoolAdapter(HALKeys.DATA_INCLINE_MOVING_BOOL, aBoolean -> updateSafeToStartWorkout(), false);
        mDataMatrix.createBoolAdapter(UiServiceKeys.DATA_SHOW_ERROR_PANEL_BOOL, aBoolean -> updateSafeToStartWorkout(), false);
        mDataMatrix.createIntAdapter(HALKeys.DATA_SAFETY_KEY_STATUS_INT, statusBitmap -> updateSafeToStartWorkout(), false);
        mDataMatrix.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING, frameString -> updateConsoleTypeForUpdater());
        mDataMatrix.createIntAdapter(SettingKeys.Hardware.DATA_BACKLIGHT_PROGRESS_INT, this::setBrightness);
        mDataMatrix.createBoolAdapter(WorkoutKeys.DATA_SETTINGS_DIALOG_OPEN_BOOL, aBoolean -> updateSafeToStartWorkout(), false);
        mDataMatrix.createEventAdapter(HALKeys.ACTION_SPLASH_RESET, key -> {
           if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_DAILY_FORCE_REBOOT_ENABLE_BOOL)
                   && SystemClock.uptimeMillis() >= MS_IN_DAY
           ) {
               this.rebootConsole("Daily Force Reboot Enabled");
           }
        });
        mDataMatrix.createStringAdapter(HALKeys.ACTION_CONSOLE_REBOOT_STRING, this::rebootConsole, false);

        mDataMatrix.createBoolAdapter(SettingKeys.General.DATA_AUTO_DATE_TIME_BOOL, enable -> {
            if (enable)
                updateTime();
        }, false);

        mDataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL, value -> {
            if (DataMatrix.getBoolean(SettingKeys.Experimental.DATA_POWER_LIMITING_BOOL)) {
                PowerLimiter.getInstance().checkApplyLimits(value);
            } else {
                Log.d(TAG, "Power limiting is disabled. Removing all power limits.");
                PowerLimiter.getInstance().removeAllLimits();
            }
        });

        if (!ProductConfig.hasIO()) {
            try {
                AudioBeeper beeper = new AudioBeeper(); // since no hw beep, need sw beep
                mDataMatrix.createEventAdapter(HALKeys.ACTION_IO_PLAY_BEEP, key -> {
                    try { beeper.playBeep(); }
                    catch (Exception e) { Log.w(TAG, "Failed to play audio beep", e); }
                });
            } catch (Exception e) {
                Log.e(TAG, "Cannot setup audio beep", e);
            }

            mDataMatrix.createIntAdapter(SettingKeys.Hardware.DATA_ENERGY_SAVER_TYPE_ENUM_INT, type -> SystemPropertyUtil.set("persist.CVT_EN_FAKE_SLEEP", "" + (type == SettingKeys.ERPType.Screen.ordinal()? 1 : 0)));
        }

        mDataMatrix.createIntAdapter(SettingKeys.Experimental.DATA_HDMI_ERROR_THRESHOLD_V2_INT, value -> ExecuteAsRoot.run("echo e " + value + " > /proc/mx6s_v4l2/entry")); //NON-NLS

        mDataMatrix.createIntAdapter(HALKeys.DATA_OC_CONTROL_TYPE_INT, this::setOverCurrentClass);

        updateSafeToStartWorkout();
    }

    private void rebootConsole(String reason) {
        ConsoleEvent.logEvent("Rebooting from " + reason);
        if (tvTunerManager != null) {
            tvTunerManager.onRebooting();
        } else {
            SystemClock.sleep(1000); // Some spots had a one second delay? if we have to reboot tuners that will generate some delay anyway
        }

        // Sleep/reboot must run off the main thread. Otherwise it blocks DataMatrix callback
        // dispatch, and onRebooting()'s tuner management (e.g. clear data, soft AP reset,
        // tuner reboot) never completes.
        new Thread(() -> {
            SystemClock.sleep(3000); // make sure to provide enough time for other logic that handles EVENT_RESET_FACTORY_DEFAULTS

            ExecuteAsRoot.runSyncCommand(); // some spots ran a file system sync when rebooting
            PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            powerManager.reboot("");
            // parker had this additional safe guard in case we do not reboot within a time window
            new JHTTimer(10000, () -> {
                Log.w(TAG, "Triggering secondary reboot");
                ExecuteAsRoot.run("reboot");
            }).start();
        }, "ConsoleRebootThread").start();
    }


    private void setBrightness(int brightness) {
        try {
            SystemBrightness.setSystemBrightness(getContentResolver(), brightness);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }


    private void updateSafeToStartWorkout() {
        boolean isSafeToStartWorkout = true;
        // starting new set of flags
        EnumSet<SafeToStartFlags> newFlags = EnumSet.noneOf(SafeToStartFlags.class);

        // disable starting a workout for treadmill if rpm > 0 or if safety key is pulled
        boolean isDemoMode = DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL);
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        if (!isDemoMode && config.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {

            // Check RPM or speed for all treadmills (threshold 24 to avoid noise)
            if ((DataMatrix.getInt(HALKeys.DATA_RPM_INT) >= 24 || DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT) > 0.1f)) {
                newFlags.add(SafeToStartFlags.TM_RPM_VALUE_STATUS);
                isSafeToStartWorkout = false;
            }

            //Only C6 check status byte B3 RPM encoder to Safe workout
            if (DataMatrix.getBoolean(HALKeys.DATA_RPM_ENCODER_STATUS_BOOL)) {
                newFlags.add(SafeToStartFlags.TM_RPM_ENCODER_STATUS);
                isSafeToStartWorkout = false;
            }
            if (DataMatrix.getBoolean(HALKeys.DATA_MAIN_MOTOR_STATUS_BOOL)) {
                newFlags.add(SafeToStartFlags.TM_MAIN_MOTOR_STATUS);
                isSafeToStartWorkout = false;
            }
            if (DataMatrix.getBoolean(HALKeys.DATA_INCLINE_MOVING_BOOL)) {
                newFlags.add(SafeToStartFlags.TM_INCLINE_MOVING);
                isSafeToStartWorkout = false;
            }
            if (ChimeraEnums.isTreadmillSafetyKeyPulled(DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT))) {
                isSafeToStartWorkout = false;
            }
        }

        if (!isDemoMode && (config.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill )) {
            if (DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT) != ChimeraEnums.SAFETY_KEY_NOT_PULLED) {
                isSafeToStartWorkout = false;
            }
            if (DataMatrix.getInt(HALKeys.DATA_SPM_INT) > 0) {
                isSafeToStartWorkout = false;
            }
        }

        if (!isDemoMode && config.machineClassType == JHTCommonTypes.MachineClassificationType.Ascent) {
            if (DataMatrix.getBoolean(HALKeys.DATA_INCLINE_MOVING_BOOL)) {
                isSafeToStartWorkout = false;
            }
        }

        // Error panel includes out of order, safety keys, and error codes.  We should never start
        // a workout while this is shown.
        if(DataMatrix.getBoolean(UiServiceKeys.DATA_SHOW_ERROR_PANEL_BOOL)) {
            isSafeToStartWorkout = false;
        }

        if (DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL) && DataMatrix.getBoolean(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL)) {
            isSafeToStartWorkout = false; // if digital safety PIN is enabled, do not allow anything to start!
        }

        if (showingSoftwareUpdate) {
            isSafeToStartWorkout = false;
        }

        if (SafeToStartFlags.toInt(newFlags) != DataMatrix.getInt(HALKeys.DATA_TM_IS_SAFE_TO_START_BITMASK_INT)) {
            DataMatrix.updateInt(HALKeys.DATA_TM_IS_SAFE_TO_START_BITMASK_INT, SafeToStartFlags.toInt(newFlags));
        }

        DataMatrix.updateBoolean(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL, isSafeToStartWorkout);
		
		
    }

    @SuppressWarnings({"MissingPermission"})
    public void resetFactoryDefault() {
        // remove all wifi
        WifiConfigWrapper wifiConfigWrapper = new WifiConfigWrapper(getApplicationContext());
        wifiConfigWrapper.removeAllWireless();
    }

    public int getKey() {
        return 23;
    }

    private void updateConsoleTypeForUpdater() {
        Context context = getApplicationContext();
        ConsoleType consoleType = ProductConfig.getConsoleType();
        AndroidSetting.putString(context.getContentResolver(), "console_type", consoleType.name()); //NON-NLS
        setTmOCtypeControl();

    }

    private int foundRFID = -1;
    private int foundBTSink = -1;
    private void findUSBDevices() {
        new Thread(() -> {
            JHTNotification btSinkNotification = new JHTNotification();

            for (int i = 4; i < 6; i++) {

                String device = "/dev/ttyXRUSB" + i; //NON-NLS

                if (foundBTSink == -1 && foundRFID != i) {
                    SerialPortConfiguration serialPortConfiguration = new SerialPortConfiguration();
                    serialPortConfiguration.setVFlags = false;
                    serialPortConfiguration.VMIN = 1;
                    serialPortConfiguration.VTIME = 0;
                    serialPortConfiguration.makeRaw = true;
                    serialPortConfiguration.baudRate = SerialPortConfiguration.BaudRate.B115200;
                    serialPortConfiguration.csize = SerialPortConfiguration.CSIZE.CS8;
                    serialPortConfiguration.CLOCAL = true;
                    serialPortConfiguration.CREAD = true;
                    serialPortConfiguration.TRACE_DEBUG = DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_RFID_BOOL);

                    SerialPort port = new SerialPort(device, serialPortConfiguration); //NON-NLS
                    try {
                        port.open();
                        new Thread(() -> {
                            try {
                                byte[] buffer = new byte[1024];
                                int bytesRead = port.getInputStream().read(buffer);
                                // NFC Firmware Version command 0x02ff will respond with 9 bytes invalid message for BTSink (aa55ff02010000007b)
                                // RFID2 will respond with bytes > 9
                                Log.d(TAG, "Find BTSink bytesRead: " +  bytesRead);
                                if (bytesRead == 9) {
                                    btSinkNotification.setNotification();
                                }
                            } catch (IOException ignored) {
                            }
                        }, "findUSBDevicesPort").start();

                        int PACKET_HEADER_SIZE = 8;
                        int PACKET_CRC_SIZE = 1;
                        int PACKET_MIN_SIZE = PACKET_HEADER_SIZE + PACKET_CRC_SIZE;
                        byte[] result = new byte[PACKET_MIN_SIZE];
                        ByteBuffer byteBuffer = ByteBuffer.wrap(result);
                        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                        // sync bytes
                        byteBuffer.put((byte) 0xAA);
                        byteBuffer.put((byte) 0x55);
                        // command
                        byteBuffer.putShort((short) 0x02ff); // NFC Firmware Version
                        // parameter/result
                        byteBuffer.putShort((short) 0);
                        // data length
                        byteBuffer.putShort((short) 0);
                        // checksum
                        byteBuffer.put(Crc8.doCrc8(result, result.length - 2));
                        port.getOutputStream().write(result);

                        if (btSinkNotification.waitForNotification(2000)) {
                            foundBTSink = i;
                            continue;
                        }
                    } catch (IOException ignored) {
                    } finally {
                        port.close();
                        if (foundBTSink == i) {
                            handler.post(() -> GymKitManager.getInstance().start(device));
                            DataMatrix.putString(HALKeys.DATA_BT_SINK_PORT_STRING, device);
                            Log.d(TAG, "Found BTSink: " +  device);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                if (foundRFID == -1 && foundBTSink != i) {
                    if (rfidReaderManager == null) rfidReaderManager = new RfidReaderManager(this);
                    rfidReaderManager.start(device);
                    SystemClock.sleep(3500); // wait to see if rfid reader is found
                    if (rfidReaderManager.isFound()) {
                        foundRFID = i;
                        DataMatrix.putString(HALKeys.DATA_RFID_MANAGER_SERIAL_PORT_STRING, device);
                    }
                }
            }
        }, "findUSBDevices").start();

    }

    private void setTmOCtypeControl() {
        int OCtype = OverCurrentType.OC_UNKNOWN.ordinal();

        if(BuildConfig.NEW_OC_PROTECTION) {//600E Parker use DC Bus current
            OCtype = OverCurrentType.OC_DC_BUS_CURRENT_LCB.getValue();
        }else {

            FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
            if(config.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS){	//C6 use auto OC
                OCtype = OverCurrentType.OC_AUTO_LCB.getValue();
            }else if(config.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02){	//F1 use DC bus OC
                OCtype = OverCurrentType.OC_DC_BUS_CURRENT_LCB.getValue();
            }else if(mFrameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill)){//error code OC
                OCtype = OverCurrentType.OC_ERROR_CODE_LCB.getValue();
            }
        }

        DataMatrix.putInt(HALKeys.DATA_OC_CONTROL_TYPE_INT, OCtype);

    }

    private void setOverCurrentClass(int ocType)
    {
        finishOverCurrentClass();
        if (ocType == 0x01) {
            mOCtype = OverCurrentType.OC_AUTO_LCB;
        } else if (ocType == 0x02) {
            mOCtype = OverCurrentType.OC_DC_BUS_CURRENT_LCB;
        } else if (ocType == 0x03) {
            mOCtype = OverCurrentType.OC_ERROR_CODE_LCB;
        }else{
            mOCtype = OverCurrentType.OC_UNKNOWN;
        }

        switch (mOCtype) {
            case OC_AUTO_LCB:
                mOverCurrentAutoManager = new OverCurrentAutoManager();
                break;
            case OC_DC_BUS_CURRENT_LCB:
                FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();

                if(config.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02){	//F1 use DC bus OC
                    mOverCurrentTM548DCbusManager = OverCurrentTM548DCbusManager.getInstance();
                }else{
                    mOverCurrentDCbusManager = new OverCurrentDCbusManager();
                }
                break;
            case OC_ERROR_CODE_LCB:
                mOverCurrentErrorCodeManager = new OverCurrentErrorCodeManager();
                break;
            case OC_UNKNOWN:
                break;
        }
    }

    private void finishOverCurrentClass()
    {
        switch (mOCtype) {
            case OC_AUTO_LCB:
                if(mOverCurrentAutoManager != null){
                    mOverCurrentAutoManager.closeOverCurrentAuto();
                }
                break;
            case OC_DC_BUS_CURRENT_LCB:
                FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();

                if(config.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
                    if(mOverCurrentTM548DCbusManager != null){
                        mOverCurrentTM548DCbusManager.closeOverCurrentDCbus();
                    }
                }
                else {
                    if(mOverCurrentDCbusManager != null){
                        mOverCurrentDCbusManager.closeOverCurrentDCbus();
                    }
                }
                break;
            case OC_ERROR_CODE_LCB:
                if(mOverCurrentErrorCodeManager != null){
                    mOverCurrentErrorCodeManager.closeOverCurrentErrorCode();
                }
                break;
            case OC_UNKNOWN:
                break;
        }
    }

    private boolean isInteractive() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isInteractive();
    }
}
