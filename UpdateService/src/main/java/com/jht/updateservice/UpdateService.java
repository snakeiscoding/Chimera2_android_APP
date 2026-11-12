package com.jht.updateservice;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UpdateEngineCallback;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.events.JHTDataNotification;
import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.androidcommonalgorithms.util.ExecuteAsRoot;
import com.jht.androidcommonalgorithms.util.SystemCommandResult;
import com.jht.androidcommonalgorithms.util.SystemCommands;
import com.jht.androidcommonalgorithms.util.SystemDetection;
import com.jht.common.CmdProcessHelper;
import com.jht.interfaces.IDefaults;
import com.jht.interfaces.IInstallEvent;
import com.jht.interfaces.IInstallListener;
import com.jht.interfaces.IPackageVersions;
import com.jht.oauthlib.auth.BksCredentialProvider;
import com.jht.updatemanagerservicelib.UpdateManager;
import com.jht.updatemanagerservicelib.UpdatePaths;
import com.jht.updateservice.ui.UpdateUi;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import updatepackages.UpdatePackage;
import updatepackages.Version;

public class UpdateService extends Service implements IDefaults, IInstallEvent, IPackageVersions, IInstallListener {
    private static final String TAG = "UpdateService";
    public static boolean recoveryCalled = false;
    @NonNls private static final String UPDATE_STARTED_INTENT = "com.jht.update_started_intent";
    private static final int SALUTRON_UPDATE_START_ID = 100;
    private static final int SALUTRON_UPDATE_PERCENT_ID = 101;
    private static final int SALUTRON_UPDATE_COMPLETE_ID = 102;

    /** How long past this service starting a missing hardware version still means "HAL has not published yet" */
    private static final long HARDWARE_VERSION_SETTLE_MS = 10_000;
    /** Uptime this service started at, which HAL is already past since chimera starts as the home activity */
    private final long serviceStartUptimeMs = SystemClock.elapsedRealtime();


    static String getStoragePath() {
        // Vortex (Android 13) doesn't provide permission to /mnt/media_rw but /storage works
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return "/storage";
        // Older OS have use this for mounting
        return "/mnt/media_rw";
    }

    // The path of the primary USB
    @NonNls public static final String mUsbPath = getStoragePath() + "/udisk";
    @NonNls public static final String mUsbPath2 = getStoragePath() + "/udisk2";
    // The location of the SD card
    @SuppressLint("SdCardPath")
    @NonNls public static final String SD_CARD_PATH_DEFAULT = getStoragePath() + "/sdcard";

    @NonNls private static final String UPDATE_IO_BROADCAST = "com.jht.hal.update.io";
    @NonNls private static final String UPDATE_TUNER_BROADCAST = "com.jht.hal.update.tuner";
    @NonNls private static final String UPDATE_LCB_BROADCAST = "com.jht.hal.update.lcb";
    @NonNls private static final String UPDATE_LCD_TOUCH_FIRMWARE_BROADCAST = "p1_lcd_touch_firmware_update";

    @NonNls private static final String ACTION_BT_UPDATE_START = "bt_sink_update_start";
    @NonNls private static final String ACTION_RFID2_UPDATE = "bt_rfid2_update";
    @NonNls private static final String ACTION_RFID3_UPDATE = "bt_rfid3_update";
    @NonNls private static final String ACTION_RFID4_UPDATE = "bt_rfid4_update";

    //CAN updates
    @NonNls private static final String ACTION_CAN_UPDATE = "can_firmware_update_single"; //update firmware for dumb plastics treadmill/ascent version
    @NonNls private static final String ACTION_CAN_UPDATE_LEFT = "can_firmware_update_left"; //update firmware for dumb plastics climb mill/bike version (left side)
    @NonNls private static final String ACTION_CAN_UPDATE_RIGHT = "can_firmware_update_right"; //update firmware for dumb plastics climb mill/bike version (right side)

    @NonNls private static final String EXTRA_START_PATH = "path";
    @NonNls private static final String ACTION_UPDATE_COMPLETE = "firmware_update_complete";
    @NonNls private static final String ACTION_UPDATE_PROGRESS = "firmware_update_progress";
    @NonNls private static final String EXTRA_PROGRESS = "progress";
    @NonNls private static final String ACTION_BS_UPDATE = "bump_sensor_update";

    private static final String MCU_IO_TARGET = "IO";
    private static final String MCU_IR_TARGET = "IR";

    @SuppressLint("SdCardPath")
    @NonNls private static final String SYSTEM_RSCU_UPDATE_DIR = "/sdcard/Download";
    private File updatePackagePath = null;

    private static final int IO_UPDATE_TIMEOUT = 300; // 5 minute timeout
    private static final int LCB_UPDATE_TIMEOUT = 300; // 5 minute timeout
    private static final int TUNER_UPDATE_TIMEOUT = 600000 * 2; // 20 minute timeout
    private static final int OS_UPDATE_TIMEOUT = 600000; // 10 minute timeout, updates generally don't take this long, but want to be safe to avoid issues
    private static final int TOUCH_PANEL_UPDATE_TIMEOUT_MS = 600000; // 10 minutes ms

    //Set a 10 minute timer for the the LCD Touch Panel update. 5 minutes per side.
    private static final long LCD_TOUCH_PANEL_UPDATE_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(10L);

    private final UpdateEngineCallback updateOSReceiverCallback = new UpdateEngineCallback() {

        @Override
        public void onStatusUpdate(int status, float percent) {
            Log.v(TAG, "status: " + status + " | percent: " + percent);
            switch (status) {
                case 0:
                    Log.d(TAG, "OS Update Idle");
                    break;
                case 1:
                    Log.d(TAG, "OS Update Checking For Update");
                    break;
                case 2:
                    Log.d(TAG, "OS Update Update Available");
                    break;
                case 3:
                    Log.d(TAG, String.format(Locale.ENGLISH, "OS Update Downloading %.0f%s", percent * 100.0f, "%"));
                    break;
                case 4:
                    Log.d(TAG, "OS Update Verifying");
                    break;
                case 5:
                    Log.d(TAG, "OS Update Finalizing");
                    break;
                case 6:
                    Log.d(TAG, "OS Update Completed");
                    break;
                case 7:
                    Log.d(TAG, "OS Update OS Update Error");
                    break;
                case 8:
                    Log.d(TAG, "OS Update Attempting Rollback");
                    break;
                case 9:
                    Log.d(TAG, "OS Update Disabled");
                    break;
                default:
                    Log.d(TAG, "Status: Unknown status " + status);
                    break;
            }
        }

        @Override
        public void onPayloadApplicationComplete(int errorCode) {
            Log.v(TAG, "errorCode: " + errorCode);
            switch (errorCode) {
                case 0:

                    PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
                    powerManager.reboot("");
                    break;
                case 2:
                    Log.d(TAG, "OS Update Failed: Payload Properties File Not Found Error");
                    break;
                case 3:
                    Log.d(TAG, "OS Update Failed: Payload File Not Found Error");
                    break;
                case 1:
                    Log.d(TAG, "OS Update Failed: Error");
                    break;
                case 4:
                    Log.d(TAG, "OS Update Failed: Filesystem Copier Error");
                    break;
                case 5:
                    Log.d(TAG, "OS Update Failed: Post Install Runner Error");
                    break;
                case 6:
                    Log.d(TAG, "OS Update Failed: Payload Mismatched Type Error");
                    break;
                case 7:
                    Log.d(TAG, "OS Update Failed: Install Device Open Error");
                    break;
                case 8:
                    Log.d(TAG, "OS Update Failed: Kernel Device Open Error");
                    break;
                case 9:
                    Log.d(TAG, "OS Update Failed: Download Transfer Error");
                    break;
                case 10:
                    Log.d(TAG, "OS Update Failed: Payload Hash Mismatch");
                    break;
                case 11:
                    Log.d(TAG, "OS Update Failed: Payload Size Mismatch");
                    break;
                case 12:
                    Log.d(TAG, "OS Update Failed: Payload Verification Error");
                    break;
                case 13:
                    Log.d(TAG, "OS Update Failed: Download New Partition Error");
                    break;
                case 14:
                    Log.d(TAG, "OS Update Failed: Payload Download Write Error");
                    break;
                case 15:
                    Log.d(TAG, "OS Update Failed: New Rootfs Verification Error");
                    break;
                case 16:
                    Log.d(TAG, "OS Update Failed: New Kernel Verification Error");
                    break;
                case 17:
                    Log.d(TAG, "OS Update Failed: Delta Payload Expected Error");
                    break;
                case 18:
                    Log.d(TAG, "OS Update Failed: Download Payload Pub Key Verification Error");
                    break;
                case 19:
                    Log.d(TAG, "OS Update Failed: Post Install Booted From Firware B Error");
                    break;
                case 20:
                    Log.d(TAG, "OS Update Failed: Download State Initialization Error");
                    break;
                case 21:
                    Log.d(TAG, "OS Update Failed: Download Invalid Metadata Magic String Error");
                    break;
                case 22:
                    Log.d(TAG, "OS Update Failed: Signature Missing In Manifest Error");
                    break;
                case 23:
                    Log.d(TAG, "OS Update Failed: Manifest Parse Error");
                    break;
                case 24:
                    Log.d(TAG, "OS Update Failed: Metadata Signature Error");
                    break;
                case 25:
                    Log.d(TAG, "OS Update Failed: Metadata Signature Verification Error");
                    break;
                case 26:
                    Log.d(TAG, "OS Update Failed: Metadata Signature Mismatch");
                    break;
                case 27:
                    Log.d(TAG, "OS Update Failed: Operation Hash Verification Error");
                    break;
                case 28:
                    Log.d(TAG, "OS Update Failed: Operation Execution Error");
                    break;
                case 29:
                    Log.d(TAG, "OS Update Failed: Operation Hash Mismatch");
                    break;
                case 30:
                    Log.d(TAG, "OS Update Failed: Omaha Request Empty Response Error");
                    break;
                case 31:
                    Log.d(TAG, "OS Update Failed: Omaha Request XML Parse Error");
                    break;
                case 32:
                    Log.d(TAG, "OS Update Failed: Download Invalid Metadata Size");
                    break;
                case 33:
                    Log.d(TAG, "OS Update Failed: Download Invalid Metadata Signature");
                    break;
                case 34:
                    Log.d(TAG, "OS Update Failed: Omaha Response Invalid");
                    break;
                case 35:
                    Log.d(TAG, "OS Update Failed: Omaha Update Ignored Per Policy");
                    break;
                case 36:
                    Log.d(TAG, "OS Update Failed: Omaha Update Deferred Per Policy");
                    break;
                case 37:
                    Log.d(TAG, "OS Update Failed: Omaha Error In HTTP Response");
                    break;
                case 38:
                    Log.d(TAG, "OS Update Failed: Download Operation Hash Missing Error");
                    break;
                case 39:
                    Log.d(TAG, "OS Update Failed: Download Metadata Signature Missing Error");
                    break;
                case 40:
                    Log.d(TAG, "OS Update Failed: Omaha Update Deferred For Backoff");
                    break;
                case 41:
                    Log.d(TAG, "OS Update Failed: Post Install Power Wash Error");
                    break;
                case 42:
                    Log.d(TAG, "OS Update Failed: Update Canceled By Channel Change");
                    break;
                case 43:
                    Log.d(TAG, "OS Update Failed: Post Install Firmware RO Not Updatable");
                    break;
                case 44:
                    Log.d(TAG, "OS Update Failed: Unsupported Major Payload Version");
                    break;
                case 45:
                    Log.d(TAG, "OS Update Failed: Unsupported Minor Payload Version");
                    break;
                case 46:
                    Log.d(TAG, "OS Update Failed: Omaha Request XML Has Entity Decl");
                    break;
                case 47:
                    Log.d(TAG, "OS Update Failed: File System Verifier Error");
                    break;
                case 48:
                    Log.d(TAG, "OS Update Failed: User Canceled");
                    break;
                case 49:
                    Log.d(TAG, "OS Update Failed: Non Critical Update In OOBE");
                    break;
                case 51:
                    Log.d(TAG, "OS Update Failed: Payload Timestamp Error");
                    break;
                case 52:
                    Log.d(TAG, "OS Update Failed: Updated But Not Active");
                    break;
                default:
                    Log.d(TAG, "OS Update Failed: Unknown error " + errorCode);
                    break;
            }
        }
    };
    private final BroadcastReceiver updateOSReceiver = new BroadcastReceiver() {
        @SuppressLint("SdCardPath")
        @Override
        public void onReceive(Context context, Intent intent) {

            new Thread(() -> {
                UpdateOS updateOS = new UpdateOS(getApplicationContext());
                String payload = intent.getStringExtra("payload");
                if (payload == null || payload.isEmpty()) {
                    payload = "/sdcard/update/payload.bin"; //NON-NLS
                }
                String payloadProperties = intent.getStringExtra("payload_properties");
                if (payloadProperties == null || payloadProperties.isEmpty()) {
                    payloadProperties = "/sdcard/update/payload_properties.txt"; //NON-NLS
                }
                updateOS.update(updateOSReceiverCallback, new File(payload), new File(payloadProperties));

            }).start();
        }
    };

    private final BroadcastReceiver updateIOReceiver = new BroadcastReceiver() {
        @SuppressLint("SdCardPath")
        @Override
        public void onReceive(Context context, Intent intent) {

            String target = intent.getStringExtra("target");
            final String finalTarget = (target == null || target.isEmpty() ? MCU_IO_TARGET : target);
            new Thread(() -> updateIO(new File("/sdcard/mcu_firmware.BIN"), finalTarget)).start();
        }
    };


    public enum ChimeraPackage {
        ChimeraApps("chimera_mainapp", "63a74e6c-8380-4416-963a-2ee9d2357cc6"),
        ChimeraUpdater("chimera_updater", "d9a76997-7a2d-4f47-b429-6273de4bcfeb"),
        ChimeraThirdApps("chimera_tpa", "fd9193ca-3ab3-43e9-829f-b41e83fc915f"),
        ChimeraAssets("chimera_assets", "f5a6a28a-dacc-44a5-9949-697fc379873d"),
        ChimeraOS("chimera_os", "2ec6a147-375f-4a61-ad53-6050c17c27bb"),
        ChimeraOS1("chimera_os", "27a13436-e938-4115-9538-22d47bf0bc4c"),
        ChimeraOS2("chimera_os", "2ddca859-61f9-4151-92a0-c65f27fed095"),
        Chimera2OS("chimera2_os", "517bcd4c-4049-4c3e-b20b-1a3eb97c3616"),
        Chimera3OS("chimera3_os", "63f7fa02-a12c-11fc-edeb-29e2973f7716"),
        UltraThinOS("ut_os", "4d95cc64-dbef-4173-9deb-bee7ae9cb3be"),
        Parker3OS("parker3_os", "3a57fa29-173c-4644-a33c-4aaf9867b23a"),
        VortexOS("vortex_os", "4e15686c-89a7-4f4c-829a-34aaaf737fd8"),
        VortexLiteOS("vortex_lite_os", "ace5c5f9-73d8-4efc-b0bc-4950b617186e"),
        VortexIO("vortex_io", "26d2f55d-7286-40d1-afcd-a5d6dfe006c2"),
        ChimeraIO("chimera_io", "fb50aaa6-64bc-4217-a2aa-fdeda79684b0"),
        ChimeraIO2("chimera_io_2", "348ffaf2-804c-47ac-9250-8b011a7bf9fc"),
        ChimeraIO3("chimera_io_3","e4640af0-abf9-11ec-b909-0242ac120002"),
        ChimeraIO5("chimera_io_5","6b90c9a5-2b75-5a65-66de-12a0a06717f5"),
        Parker3IO("parker3_io", "6b3d43a2-46a4-48e9-9c0d-bdae7af28f2b"),
        ParkerIR("parker_ir", "0cccd5fd-cc77-4467-9aff-d20de5f4cae0"),
        Icr70LCB("icr70_lcb", "004e7300-42d7-4e88-8c9b-d41fddd68a21"),
        ChimeraLCB1("chimera_lcb1", "100ec386-af52-4ee5-ab62-326388ea36e1"),
        ChimeraLCB2("chimera_lcb2", "94f764e0-b90e-4c4a-ba13-0f0be210ecbb"),
        ChimeraLCBClimbmill("chimera_lcb_climbmill", "41e6d75f-ec8a-4481-8e4a-91885b429fd0"),
        RetailLCBClimbmill("retail_lcb_climbmill", "dd256a8f-99b2-4ff6-9f8b-adeb82cd66b3"),
        ChimeraLCBA("chimera_lcba", "c3136682-5a04-4795-88d2-61044f3a14ef"),
        ChimeraLCB1xBike("chimera_lcb_1x_bike", "18bb5557-e9d2-4256-a99a-44bfa2e51168"),
        ChimeraLCBAthena("chimera_lcb_athena", "eb7359d0-68e8-4293-ada3-2c42392b62c8"),
        ChimeraBluetooth("chimera_bluetooth", "f253ef1f-960a-4400-aca8-a4c84ff18df3"),
        ChimeraRfid2("chimera_rfid2", "56ecf39c-dee0-11ed-b5ea-0242ac120002"),
        ChimeraRfid3("chimera_rfid3", "caa013cd-b9b5-4df0-9269-46623b32a1a4"),
        ChimeraRfid4("chimera_rfid4", "19cbd702-2fb8-425c-8197-a7699ecf77dc"),
        ChimeraCanSingle("chimera_can_single", "4d5890bb-3cd8-480b-99de-41c568db17aa"),
        ChimeraCanRight("chimera_can_right", "65664e6d-0128-4ba7-b6c0-c9dd01a38ed3"),
        ChimeraCanLeft("chimera_can_left", "86f5f0cd-dc6e-4f6c-b5ba-3d7c9df02f86"),
        ChimeraSalutron("chimera_salutron", "acd5713e-3e57-4b15-98f9-37716a3440a6"),
        ChimeraTouchPanel("chimera_touch_panel", "bed535d8-6386-4af4-a42f-249103f2edd3"),
        Chimera2TouchPanel("chimera2_touch_panel", "fe0ce7df-cef6-45e8-8189-3c922e7f03bb"),
        ChimeraUBoot("chimera_uboot", "d3ff5cbb-cdba-4c7d-a8c8-2f5bcf977ce7"),
        ChimeraADboard("chimera_ADboard", "251db1fe-cbbc-4c9f-b396-7505c1643111"),
        ChimeraUnknown("chimera_unknown", "4c4245bf-9c88-4d6c-963b-8e663244e674"),
        ChimeraUS5("chimera_us5", "6349cde6-09ae-4e67-9f42-bcc7cd94c7e7"),
        ChimeraUS6("chimera_us6", "3de6ce69-b8b5-4be9-ad25-400c168a7ffe"),
        ChimeraUS7("chimera_us7", "d6d20f13-5a88-4314-a3ec-183bdefe7cb9"),
        ChimeraKM5("chimera_km5", "1d739cc3-0875-4ad8-9556-1d7fff102a33"),
        P1BumpSensor("p1_bump_sensor", "0aa9a8ea-990a-4b07-91f5-4e7777dedf8b"),
        P1LCDTouchFirmware("p1_lcd_touch_firmware", "7916ce3e-3223-11ee-be56-0242ac120002"),

        // Specific TPAs
        TpaBsjLiveTV("BSJLiveTV", "00f9be8c-deba-4fa2-9eea-7e20058c6eab"),
        TpaTeamViewer("TeamViewer", "6f94e2a1-711b-4080-9cc1-49c3ebc59bea"),
        TpaTeamViewerAddon("TeamViewerAddon", "5427b75e-706a-42bd-91da-4a0134e36465"),
        ; // END ENUM LIST

        private final String name;
        private final String uuid;

        ChimeraPackage(@NonNls String name, @NonNls String uuid) {
            this.name = name;
            this.uuid = uuid;
        }

        @SuppressWarnings({"unused", "RedundantSuppression"})
        public String packageName() {
            return name;
        }

        public String uuid() {
            return uuid;
        }

        @SuppressWarnings({"unused", "RedundantSuppression"})
        public static ChimeraPackage fromString(String p) {
            for (ChimeraPackage cp : values()) {
                if (cp.name().equals(p)) {
                    return cp;
                }
            }
            return ChimeraUnknown;
        }

        public static ChimeraPackage fromUUID(String uuid) {
            for (ChimeraPackage cp : values()) {
                if (cp.uuid().equals(uuid)) {
                    return cp;
                }
            }
            return ChimeraUnknown;
        }

        public boolean isOS() { // is it work adding an isOS field?
            switch (this) {
                case ChimeraOS:
                case ChimeraOS1:
                case ChimeraOS2:
                case Chimera2OS:
                case Chimera3OS:
                case Parker3OS:
                case UltraThinOS:
                case VortexOS:
                case VortexLiteOS:
                    return true;
                default:
                    return false;
            }
        }
    }

    private CountDownLatch mSalutronLatch;
    private UpdateUi updateUi;
    private AutomatedUpdate automatedUpdate;
    private CountDownLatch mUpdateLatch;
    private CountDownLatch mLcbLatch;
    private CountDownLatch mIoLatch;
    protected UpdateManager updateManager = null;
    private boolean osUpdateSuccessful = false;
    private boolean mIoUpdateSuccessful;
    private boolean isUpdateSuccessful;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // This should be refactor in artifact to be a static function since getApplyDir is currently a hardcoded path
        // but the better option is to refactor the library to allow the apply path to be configurable by the IDefault interface
        updatePackagePath = new UpdatePaths("").getApplyDir();

        updateUi = new UpdateUi(getApplicationContext(), () -> updateManager.cancelUpdate());

        // for newer android versions we could just check foregroundServiceType != FOREGROUND_SERVICE_TYPE_NONE
        if (ServiceStarter.shouldStartUpdateServiceAsForeground()) {
            startForegroundService(this);
        }

        Log.d(this.getClass().getName(), "UpdateService starting");
        if (this.updateManager == null && !recoveryCalled) {
            this.updateManager = new UpdateManager(this, this, this, this, this);
        }
        passCurrentBundleVersionToMainApp();

        // check if a USB is plugged in
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
//        registerReceiver(new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                StorageUtils.checkDevice(path -> mUsbPath = path);
//            }
//        }, filter);
//
//        // check if there is a USB already plugged in
//        StorageUtils.checkDevice(path -> mUsbPath = path);

        registerReceiver(updateOSReceiver, new IntentFilter("com.jht.updateos"));
        registerReceiver(updateIOReceiver, new IntentFilter("com.jht.updateio"));
        automatedUpdate = new AutomatedUpdate(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateOSReceiver);
        unregisterReceiver(updateIOReceiver);
        if(automatedUpdate != null) {
            automatedUpdate.close();
            unregisterReceiver(automatedUpdate);
        }
    }

    /** inform main app of the current top level bundle version so it can display it */
    private void passCurrentBundleVersionToMainApp() {
        if (updateManager != null) {
            String currentBundleVersion = updateManager.getCurrentTopLevelBundleVersion().toString();
            Settings.Secure.putString(getApplicationContext().getContentResolver(), "com.jht.updateservice.bundleVersion", currentBundleVersion);
        }
    }

    @Override
    public String getUsb0Path() {
        return automatedUpdate != null && automatedUpdate.inAutomatedUpdate() ? automatedUpdate.getAutomatedUpdateLocation().getAbsolutePath() : mUsbPath;
    }

    @Override
    public String getUsb1Path() {
        return mUsbPath2;
    }

    @Override
    public String getSdCardPathDefault() {
        return SD_CARD_PATH_DEFAULT;
    }

    @Override
    public String getPackageToCheck() {
        return "com.jht.chimera"; // NON-NLS
    }

    @Override
    public boolean installAutoUpdatesOnBoot() {
        return false;
    }

    @Override
    public Set<String> getConsoleType() {
        HashSet<String> consoleTypes = new HashSet<>();

        // add console type from Android Secure setting if main app saved the console type
        String console_type = Settings.Secure.getString(getContentResolver(), "console_type"); // NON-NLS
        if (console_type != null) consoleTypes.add(console_type);

        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        // add Build.PRODUCT (ie jht_8mq, rk3399, rk3588_jht, rk3588_jht_gen2, rk3588_jht_l)
        consoleTypes.add(Build.PRODUCT.toLowerCase());
        // add Build.PRODUCT + width (ie jht_8mq_1920)
        consoleTypes.add(Build.PRODUCT.toLowerCase() + "_" + width);
        // TODO any other useful console types? BuildConfig.FLAVOR_platform and BuildConfig.FLAVOR_product could be added but doesn't seem useful

        if (BuildConfig.DEBUG) Log.i(TAG, "--> ConsoleType list: " + consoleTypes);
        return consoleTypes;
    }

    @Override
    public String getOtaUpdateXmlDirectory() {
        return SYSTEM_RSCU_UPDATE_DIR;
    }

    @Override
    public String getSystemSoftwareType() {
        return BuildConfig.UPDATE_EQUIPMENT; // this must match equipment tag in update xml
    }

    /** Block installation of packages that are deprecated even if the given manifest has them */
    private boolean blockedByDeprecation(ChimeraPackage chimeraPackage) {
        // Removing a package or even adding to TPA clean up does not necessarily prevent from getting installed from old deployment
        switch (chimeraPackage) {
            case TpaBsjLiveTV:
            case TpaTeamViewer:
            case TpaTeamViewerAddon:
                Log.w(TAG, "Blocking deprecated package " + chimeraPackage.packageName());
                return true;
        }
        return false;
    }

    private boolean blockedByEmulator(ChimeraPackage chimeraPackage) {
        // for emulator, block everything except our apks and assets
        if (SystemDetection.isEmulator()) {
            switch (chimeraPackage) {
                case ChimeraApps:
                case ChimeraUpdater:
                case ChimeraAssets:
                    break;
                default:
                    Log.w(TAG, "EMULATOR DETECTED - skipping update package: " + chimeraPackage.packageName());
                    return true;
            }
        }
        return false;
    }

    private boolean blockedByOsVersion(Version version) {
        if (ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Parker)) {
            // block parker OS updates before 1.0.0.18
            Version minVersion = new Version(1,0,0,18);
            if (version.isNewer(minVersion)) {
                Log.w(TAG, "This parker OS must be manually updated to at least " + minVersion + ", current: " + version);
                return true;
            }
        }
        return false;
    }
    private boolean blockedByOsVersion() {
        Version osVersion = getCurrentOsVersion();
        return blockedByOsVersion(osVersion);
    }

    // TODO - this get+regex for OS version is also duplicated in the app, can we get a common function for it?
    private Version getCurrentOsVersion() {
        String osVersion = SystemCommands.getSystemProperty("ro.product.os"); // return cannot be null, but can be empty
        if (osVersion.isEmpty()) {
            osVersion = SystemCommands.getSystemProperty("ro.jht.product.version"); // try alternative property
        }
        if (osVersion.isEmpty()) osVersion = "0.0.0.1";
        // regex will remove the front non-decimal
        Version version = Version.parseVersion(osVersion.replaceAll("^[^0-9]*", ""));
        version = (version == null ? new Version(0, 0, 0, 0) : version);
        return version;
    }

    /**
     * Short hand function for common OS check in checkVersionForPackage()
     * @param check
     * @return
     */
    private Version checkThenGetOsVersion(boolean check) {
        Version osVersion = getCurrentOsVersion();
        if (check) {
            if (blockedByOsVersion(osVersion)) {
                return Version.getDoNotInstallVersion();
            }
            Log.i(TAG, "checkVersionForPackage: " + osVersion);
            return osVersion;
        }
        return Version.getDoNotInstallVersion();
    }

    private Version checkThenGetIoVersion(boolean check) {
        if (check) {
            return getIoVersion();
        }
        return Version.getDoNotInstallVersion();
    }
    private Version checkThenGetIoVersion(ConsoleType.ConsoleHardware hw) {
        return checkThenGetIoVersion(ConsoleType.hardware.equals(hw));
    }
    private Version checkThenGetIrVersion(boolean check) {
        if (check) {
            return getIrVersion();
        }
        return Version.getDoNotInstallVersion();
    }
    private Version checkThenGetIrVersion(ConsoleType.ConsoleHardware hw) {
        return checkThenGetIrVersion(ConsoleType.hardware.equals(hw));
    }

    /** Determine if version is likely a local date test build */
    private static boolean isLocalDatedBuild(Version version) {
        if (version == null || !version.hasBranch()) return false; // in theory this should be enough, formal release would not have a 5th field
        if (version.getMajor() < 2025) return false; // we started the local format in year 2025
        if (version.getMinor() < 1 || version.getMinor() > 12) return false; // month
        if (version.getPatch() < 1 || version.getPatch() > 31) return false; // day
        return version.getBuild() <= 1440; // minute of the day
    }
    /** Checks the version from our built packages. If none can be found or version is a test build, reports the minimum valid version of zero. */
    private Version processBuildVersion(Version version) {
        if (version != null) {
            if (isLocalDatedBuild(version)) {
                // NOTE this would always update if current version is local, but what if the incoming version is also a local build? just always replace a local?
                Log.d(TAG, "Local test version detected: " + version);
            } else {
                return version;
            }
        }
        return new Version(0, 0, 0, 0);
    }

    @Override
    public Version checkVersionForPackage(String uuid) {
        ChimeraPackage chimeraPackage = ChimeraPackage.fromUUID(uuid);

        // stopping it here will prevent downloading it, however if old update service started it, it might still resume
        if (blockedByEmulator(chimeraPackage) || blockedByDeprecation(chimeraPackage)) {
            return Version.getDoNotInstallVersion();
        }

        switch (chimeraPackage) {
            case ChimeraApps:
                Version mainAppVersion = VersionChecker.getPackageVersion(getApplicationContext(), "com.jht.chimera");
                mainAppVersion = processBuildVersion(mainAppVersion);
                Log.i(TAG, "ChimeraApps checkVersionForPackage: " + mainAppVersion);
                return mainAppVersion;
            case ChimeraUpdater:
                Version updaterVersion = VersionChecker.getPackageVersion(getApplicationContext(), getPackageName());
                updaterVersion = processBuildVersion(updaterVersion);
                Log.i(TAG, "ChimeraUpdater checkVersionForPackage: " + updaterVersion);
                return updaterVersion;
            case ChimeraOS: // parker actually re-used the UUID from ChimeraOS (which was probably the wrong thing to do)
            case ChimeraOS1:
            case ChimeraOS2:
                return checkThenGetOsVersion(ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Chimera) || ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Parker));
            case Chimera2OS:
                return checkThenGetOsVersion(ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Chimera_2));
            case Chimera3OS:
                return checkThenGetOsVersion(ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Chimera_3));
            case UltraThinOS:
                return checkThenGetOsVersion(ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.UltraThin));
            case Parker3OS:
                return checkThenGetOsVersion(ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Parker_3));
            case VortexOS:
                return checkThenGetOsVersion(ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Vortex));
            case VortexIO:
                return checkThenGetIoVersion(ConsoleType.hardware.isVortexBase());
            case VortexLiteOS:
                return checkThenGetOsVersion(ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.VortexLite));
            case ChimeraIO:
                if (ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Chimera)) {
                    // Only run this if we are on the carrier board D or E
                    if (CarrierBoard.isRevEOrOlder(getApplicationContext())) {
                        return getIoVersion();
                    } else {
                        return Version.getDoNotInstallVersion();
                    }
                } else {
                    return checkThenGetIoVersion(ConsoleType.ConsoleHardware.Parker);  // parker used same UUID
                }

            case ChimeraIO2:
                // Only run this if we are on carrier board F or later.
                if (ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Chimera) && CarrierBoard.isRevFOrLater(getApplicationContext())) {
                    return getIoVersion();
                } else {
                    return Version.getDoNotInstallVersion();
                }
            case ChimeraIO3:
                if(ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Chimera_2)) {
                    return getIoVersion();
                }
                return Version.getDoNotInstallVersion();
            case ChimeraIO5:
                return checkThenGetIoVersion(ConsoleType.ConsoleHardware.Chimera_3);
            case Parker3IO:
                return checkThenGetIoVersion(ConsoleType.ConsoleHardware.Parker_3);
            case ParkerIR:
                return checkThenGetIrVersion(ConsoleType.ConsoleHardware.Parker);
            case ChimeraLCB1:
            case ChimeraLCB2:
            case ChimeraLCBClimbmill:
            case RetailLCBClimbmill:
            case ChimeraLCBA:
            case ChimeraLCB1xBike:
            case ChimeraADboard:
            case ChimeraLCBAthena:
            case Icr70LCB:
                return getLcbVersion();
            case ChimeraSalutron:
                return getSalutronVersion();
            case ChimeraBluetooth:
                return getBluetoothVersion();
            case ChimeraRfid2:
                return getRfid2Version();
            case ChimeraRfid3:
                return getRfid3Version();
            case ChimeraRfid4:
                return getRfid4Version();
            case ChimeraCanSingle:
                return getCanVersion("single");
            case ChimeraCanRight:
                return getCanVersion("right");
            case ChimeraCanLeft:
                return getCanVersion("left");
            // These packages use the current_update.xml stored version
            case ChimeraTouchPanel:
            case ChimeraThirdApps:
            case ChimeraAssets:
            case ChimeraUnknown:
                break;
            case ChimeraUBoot:
                if (ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Chimera)) {
                    return Version.parseVersion(SystemCommands.getSystemProperty("ro.boot.uboot_ver"));
                } else {
                    return Version.getDoNotInstallVersion();
                }
            case ChimeraUS5:
                return getTunerVersion("US5");
            case ChimeraUS6:
                return getTunerVersion("US6");
            case ChimeraUS7:
                return getTunerVersion("US7");
            case ChimeraKM5:
                return getTunerVersion("KM5");
            case P1BumpSensor:
                return getBumpSensorVersion();
            case P1LCDTouchFirmware:
                return getP1ToggleVersion();
        }
        return null;
    }

    /**
     * Read a hardware version that HAL publishes for optional hardware. HAL blanks these at boot and
     * republishes each one as its module is detected, so a check early in boot can read "N/A" and wrongly
     * decide the hardware is not fitted, silently dropping its package. Until HARDWARE_VERSION_SETTLE_MS
     * past this service starting, keep re-reading until a version appears; after that, read it once and
     * take "N/A" at face value.
     *
     * @param key Settings.Secure key holding the version
     * @return the published version, or null if the hardware is not fitted
     */
    @Nullable
    private String readHardwareVersion(@NonNls String key) {
        long deadline = serviceStartUptimeMs + HARDWARE_VERSION_SETTLE_MS;
        String version = readVersionOrNull(key);
        while (version == null && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(500);
            version = readVersionOrNull(key);
        }
        if (version == null) Log.e(TAG, "No version published for '" + key + "', treating the hardware as not installed");
        return version;
    }

    /** Read a version out of Settings.Secure, treating the "N/A" placeholder HAL writes as no version */
    @Nullable
    private String readVersionOrNull(@NonNls String key) {
        String version = Settings.Secure.getString(getApplicationContext().getContentResolver(), key);
        return (version == null || version.equals("N/A")) ? null : version;
    }

    /** Generic method for getting a version out of Settings.Secure.getString() or null */
    private Version getVersionFromSecureSettings(@NonNls String key) {
        return getVersionFromSecureSettings(key, false);
    }
    private Version getVersionFromSecureSettings(@NonNls String key, boolean blockIfMissing) {
        String versionString = Settings.Secure.getString(getApplicationContext().getContentResolver(), key);
        if (versionString != null && !versionString.equals("N/A")) {
            return Version.parseVersion(versionString);
        } else {
            Log.d(TAG, "Setting key '" + key + "' = '" + versionString + "', blockIfMissing=" + blockIfMissing);
            return blockIfMissing ? Version.getDoNotInstallVersion() : null;
        }
    }

    private Version getTunerVersion(String expectedTuner) {
        if(!SystemCommands.getSystemProperty("persist.jht.tuner.detect").equals(expectedTuner)) {
            return Version.getDoNotInstallVersion();
        }
        return Version.parseVersion(SystemCommands.getSystemProperty("persist.jht.tuner.version"));
    }

    private Version getLcbVersion() {
        return getVersionFromSecureSettings("chimera_lcb_version");
    }

    private Version getIoVersion() {
        return getVersionFromSecureSettings("chimera_io_version");
    }

    private Version getIrVersion() {
        return getVersionFromSecureSettings("parker_ir_version");
    }

    private Version getP1ToggleVersion() {
        try {
            // This string is a list of toggle versions in format of "key: value, key: value"
            // If mastboard is gone, we just get "N/A" as whole string. If a toggle is disconnected, we get "?" for its value in the key/value pair.
            String versionString = readHardwareVersion("p1_lcd_toggle_version"); // NON-NLS
            if (versionString != null) {
                // Find the oldest version and return it
                Map<String, String> versionMap = parseKeyValueFromString(versionString);
                Version minimumVersion = Version.getDoNotInstallVersion(); // Do not install is MAX longs, so if nothing found, will just be dont install
                for (Map.Entry<String, String> entry : versionMap.entrySet()) {
                    Version entryVersion = Version.parseVersion(entry.getValue());
                    if (entryVersion != null) {
                        if (minimumVersion.isOlder(entryVersion)) minimumVersion = entryVersion;
                    }
                }
                return minimumVersion;
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not determine P1 toggle version", e);
        }
        return Version.getDoNotInstallVersion();
    }

    /** Parse format of "key: value, key: value" into a map of key value pairs, ignores incomplete pairs */
    private Map<String, String> parseKeyValueFromString(String KeyValueCsv) {
        Map<String, String> map = new HashMap<>();
        String[] pairs = KeyValueCsv.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.trim().split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                map.put(key, value);
            }
        }
        return map;
    }

    private Version getSalutronVersion() {
        String version = readHardwareVersion("chimera_salutron_version"); // NON-NLS
        if (version != null) {
            return parseSalutronVersion(version);
        } else {
            Log.e(TAG, "Could not find salutron version, returning large version to avoid install");
            return Version.getDoNotInstallVersion();
        }
    }

    // 4504C2230 is an example version, 4504 is the firmware version, 2230 is bootloader version
    private Version parseSalutronVersion(String version) {
        String[] versionParts = version.split("[a-zA-Z]");
        if (versionParts.length == 2) {
            return new Version(Long.parseLong(versionParts[0]), Long.parseLong(versionParts[1]), 0, 0);
        } else {
            Log.e(TAG, "Unexpected version format for salutron: " + version);
            return Version.getDoNotInstallVersion();
        }
    }

    private Version getBluetoothVersion() {
        String versionString = readHardwareVersion("chimera_bluetooth_version"); // NON-NLS

        // Note 10.5.21.0 did not fix a bug and we need to downgrade.  This hack ensures that we downgrade.
        if (versionString != null && versionString.startsWith("10.5.21")) {
            Log.d(TAG, "We have 10.5.21.0 which is bad so making sure we downgrade.");
            return Version.parseVersion("0.0.0.0");
        }
        else if (versionString != null) {
            Log.d(TAG, "Bluetooth version is " + versionString);
            return Version.parseVersion(versionString);
        } else {
            Log.e(TAG, "No bluetooth version was found, assuming BT module is not installed. Do not install if not forced.");
            return Version.getDoNotInstallVersion();
        }
    }

    private Version getRfidXVersion(@NonNls String moduleName) {
        String versionString = readHardwareVersion("chimera_" + moduleName + "_version");
        if (versionString != null) {
            return Version.parseVersion(versionString);
        } else {
            Log.e(TAG, "No " + moduleName + " version was found, assuming BT module is not installed. Do not install if not forced.");
            return Version.getDoNotInstallVersion();
        }
    }
    private Version getRfid2Version() {
        return getRfidXVersion("rfid2");
    }
    private Version getRfid3Version() {
        return getRfidXVersion("rfid3");
    }
    private Version getRfid4Version() {
        return getRfidXVersion("rfid4");
    }

    private Version getCanVersion(@NonNls String moduleName) {
        String versionString = readHardwareVersion("can_" + moduleName + "_version"); // NON-NLS
        if (versionString != null) {
            Log.d(TAG, moduleName + " version is " + versionString);
            return Version.parseVersion(versionString);
        } else {
            Log.e(TAG, "No " + moduleName + " version was found.");
            return Version.getDoNotInstallVersion();
        }
    }

    private Version getBumpSensorVersion(){
        String versionString = readHardwareVersion("chimera_bumpsensor_version"); // NON-NLS
        if (versionString != null) {
            Log.d(TAG, "Bump sensor version is " + versionString);
            return Version.parseVersion(versionString);
        } else {
            Log.e(TAG, "No bump sensor version was found, assuming bump sensor module is not installed. Do not install if not forced.");
            return Version.getDoNotInstallVersion();
        }
    }

    @Override
    public void setupCompleted(boolean inUpdate) {
        updateUi.setupCompleted(inUpdate);
    }

    @Override
    public void showUpdateScreen(boolean firstTimeInstaller) {
        Log.d(TAG, "Starting update");
        Intent updateStartedIntent = new Intent();
        updateStartedIntent.setAction(UPDATE_STARTED_INTENT);

        // Only send stick broadcast if the main app is installed. It appears Android throws this sometimes but we don't know why
        // java.lang.SecurityException: Permission Denial: broadcastIntent() requesting a sticky broadcast from pid=3239, uid=10069 requires android.permission.BROADCAST_STICKY
        // Appears that Android can get into a weird state where even adding the window to the window manager will crash
        Version mainAppVersion = VersionChecker.getPackageVersion(getApplicationContext(), "com.jht.chimera");
        if (mainAppVersion != null) {
            Context context = getApplicationContext();
            context.sendStickyBroadcast(updateStartedIntent);
            updateUi.showUpdateScreen(firstTimeInstaller);
        } else {
            Log.d(TAG, "main app is not installed!");
            updateUi.showUpdateScreen(firstTimeInstaller);
        }

        // disable the Android launcher in case the install is interrupted during the main app install which
        // causes Android to show the Android launcher
        new Thread(() -> {
            try {
                CmdProcessHelper cmdProcessHelper = new CmdProcessHelper();
                //noinspection HardCodedStringLiteral
                cmdProcessHelper.addCommand("pm disable com.android.launcher3");
                cmdProcessHelper.execute();
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }).start();
    }

    @Override
    public void hideUpdateScreen() {
        updateUi.hideUpdateScreen();
    }

    @Override
    public void updateManagerVersionSet(String s) {
        updateUi.updateManagerVersionSet(s);
    }

    @Override
    public void downloadInformation(List<UpdatePackage> list, boolean b) {
        updateUi.downloadInformation(list, b);
    }

    @Override
    public void downloadProgressUpdate(int progress, String sizeDownloaded) {
        updateUi.downloadProgressUpdate(progress, sizeDownloaded);
    }

    @Override
    public void installingUpdate(String packageName, String packageVersion) {
        updateUi.installingUpdate(packageName, packageVersion);
    }

    @Override
    public boolean updateComplete() {
        passCurrentBundleVersionToMainApp();
        return updateUi.updateComplete();
    }

    @Override
    public void errorOccurred(String errorMessage, boolean canReboot) {
        updateUi.errorOccurred(errorMessage, canReboot);
    }

    @Override
    public void displayManualReboot() {
        updateUi.displayManualReboot();
    }

    // short hand function for common OS check/update in deferInstall()
    private boolean checkThenUpdateOS(ConsoleType.ConsoleHardware hw) {
        if (blockedByOsVersion()) return true;
        if (ConsoleType.hardware.equals(hw)) {
            return updateOS() == 0;
        }
        return true;
    }
    private boolean checkThenUpdateIO(ConsoleType.ConsoleHardware hw) {
        return checkThenUpdateIO(ConsoleType.hardware.equals(hw));
    }
    private boolean checkThenUpdateIO(boolean check) {
        if (check) {
            return updateIO();
        }
        return true;
    }

    private boolean checkThenUpdateIR(ConsoleType.ConsoleHardware hw) {
        if (ConsoleType.hardware.equals(hw)) {
            Version irVersion = getIrVersion(); // only attempt to update if there is indication that IR fw is present
            if (null != irVersion) {
                return updateIO(MCU_IR_TARGET);
            } else {
                Log.w(TAG, "Unknown IR version installed, skipping IR update!");
            }
        }
        return true;
    }

    @Override
    public boolean deferInstall(String s) {
        ChimeraPackage chimeraPackage = ChimeraPackage.fromUUID(s);
        Log.d(TAG, "deferInstall " + s);

        // if old updater queued it anyway, but it is deferred and should be blocked, block it
        if (blockedByEmulator(chimeraPackage) || blockedByDeprecation(chimeraPackage)) {
            return true;
        }

        switch (chimeraPackage) {
            case ChimeraOS: // parker actually re-used the UUID from ChimeraOS BUT it does not use this updateOS() method
            case ChimeraOS1:
            case ChimeraOS2:
                return checkThenUpdateOS(ConsoleType.ConsoleHardware.Chimera);
            case Chimera2OS:
                return checkThenUpdateOS(ConsoleType.ConsoleHardware.Chimera_2);
            case Chimera3OS:
                return checkThenUpdateOS(ConsoleType.ConsoleHardware.Chimera_3);
            case UltraThinOS: // UT runs parker, but OS uses A/B updating
                return checkThenUpdateOS(ConsoleType.ConsoleHardware.UltraThin);
            case Parker3OS:
                return checkThenUpdateOS(ConsoleType.ConsoleHardware.Parker_3);
            case VortexOS:
                return checkThenUpdateOS(ConsoleType.ConsoleHardware.Vortex);
            case VortexIO:
                return checkThenUpdateIO(ConsoleType.hardware.isVortexBase());
            case VortexLiteOS:
                return checkThenUpdateOS(ConsoleType.ConsoleHardware.VortexLite);
            case ChimeraIO:
                // Only run this if we are on the carrier board D or E
                if (ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Chimera) && CarrierBoard.isRevEOrOlder(getApplicationContext())) {
                    return updateIO();
                }
                if (ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Parker)) {
                    return updateIO(); // parker re-use uuid, probably shouldn't have
                }
                break;
            case ChimeraIO2:
                // Only run this if we are on carrier board F or later.
                if (ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Chimera) && CarrierBoard.isRevFOrLater(getApplicationContext())) {
                    return updateIO();
                }
                break;
            case ChimeraIO3:
                if (ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Chimera_2)) {
                    return updateIO();
                }
                break;
            case ChimeraIO5:
                return checkThenUpdateIO(ConsoleType.ConsoleHardware.Chimera_3);
            case ChimeraUS5:
                return updateTunerFirmware("US5");
            case ChimeraUS6:
                return updateTunerFirmware("US6");
            case ChimeraUS7:
                return updateTunerFirmware("US7");
            case ChimeraKM5:
                return updateTunerFirmware("KM5");
            case Parker3IO:
                return checkThenUpdateIO(ConsoleType.ConsoleHardware.Parker_3);
            case ParkerIR:
                return checkThenUpdateIR(ConsoleType.ConsoleHardware.Parker);
            case ChimeraLCB1:
                return updateLCB(0x07);
            case ChimeraLCB2:
                return updateLCB(0x0A);
            case ChimeraLCBClimbmill:
                return updateLCB(0x17);
            case RetailLCBClimbmill:
                return updateLCB(0x19);
            case ChimeraLCBA:
                return updateLCB(0x13);
            case ChimeraLCB1xBike:
                return updateLCB(0x14);
            case ChimeraLCBAthena:
                return updateLCB(0x1B);
            case Icr70LCB:
                return updateLCB(0x1D);
            case ChimeraBluetooth:
                return updateBluetooth();
            case ChimeraRfid2:
                return updateRfid2();
            case ChimeraRfid3:
                return updateRfid3();
            case ChimeraRfid4:
                return updateRfid4();
            case ChimeraCanSingle:
                return updateCanSingle();
            case ChimeraCanRight:
                return updateCanRight();
            case ChimeraCanLeft:
                return updateCanLeft();
            case ChimeraSalutron:
                return updateSalutron();
            case ChimeraTouchPanel:
                if (ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Chimera)) {
                    return updateTouchPanel();
                }
            case Chimera2TouchPanel:
                if (ConsoleType.hardware.equals(ConsoleType.ConsoleHardware.Chimera_2)) {
                    return updateTouchPanel2();
                }
            case ChimeraADboard:
                return updateLCB(0x1C);
            case P1BumpSensor:
                return updateBumpSensor();
            case P1LCDTouchFirmware:
                return updateLCDTouchFirmware();
        }
        return true;
    }

    /**
     * Return the paths commonly used during the update process.
     *
     * @return The paths commonly used during the update process.
     */
    @NonNull
    private File getUpdatePath() {
        return updatePackagePath;
    }

    private boolean updateTunerFirmware(String tunerType) {
        if(SystemCommands.getSystemProperty("persist.jht.tuner.detect").equals(tunerType)) {
            File[] updateFiles = getUpdatePath().listFiles();
            if (updateFiles != null) {
                for (File f : updateFiles) {
                    if (f.getName().endsWith(".bin")) {
                        // NOTE: tuner update passes in folder name, not file name; there's hardcoded logic that
                        // hardcode the firmware filename in the serial port artifact
                        return updateTunerFirmware(f.getParentFile(), "TV Tuner");
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void resetUpdates() {
        // This will get called when cleaning up updates, we don't need to do anything here unless
        // we create files in that need to be removed after a deferred install
    }

    @Override
    public boolean runShellScript(String s) {
        ChimeraPackage chimeraPackage = ChimeraPackage.fromUUID(s);

        // if old updater queued it anyway, but it is deferred and should be blocked, block it
        if (blockedByEmulator(chimeraPackage) || blockedByDeprecation(chimeraPackage)) {
            return false;
        }
        if (chimeraPackage.isOS()) {
            if (blockedByOsVersion()) return false;
        }

        return true;
    }

    @Override
    public String zipPassword() {
        BksCredentialProvider credentialProvider = new BksCredentialProvider(getApplicationContext());
        return credentialProvider.getString("UPDATE_ZIP_PASSWORD");
    }

    @Override
    public boolean use7Zip() {
        return false;
    }

    public static void startForegroundService(Service service) {

        NotificationManager mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        String id = "id"; // NON-NLS
        CharSequence name = "name"; // NON-NLS
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        mNotificationManager.createNotificationChannel(channel);

        Notification notification = new Notification.Builder(service, id)
                .setContentTitle("JHT Service") // NON-NLS
                .setContentText("JHT Service") // NON-NLS
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .build();
        service.startForeground(1, notification);
    }

    private boolean updateIO() { return updateIO(MCU_IO_TARGET); } // default target is console IO
    private boolean updateIO(String target) {

        Log.d(TAG, "Deferred install of IO starting");
        File[] updateFiles = getUpdatePath().listFiles();
        if (updateFiles != null) {
            for (File f : updateFiles) {
                if (f.getName().toUpperCase(Locale.ENGLISH).endsWith(".BIN")) {
                    return updateIO(f, target);
                }
            }
        }
        return false;
    }
    private boolean updateIO(File firmware, String target) {

        mIoLatch = new CountDownLatch(1);
        mIoUpdateSuccessful = false;

        if (firmware != null && firmware.exists()) {
            BroadcastReceiver ioResult = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    float percent = intent.getFloatExtra("percent", 0.0f);
                    int status = intent.getIntExtra("status", 0);
                    switch (status) {
                        case 1028: //start update
                            updateUi.updateInstallMessage(target + " Update Started");
                            break;
                        case 1026: //update processing
                            updateUi.updateInstallMessage(String.format(Locale.ENGLISH, "%s Update %.0f%s", target, percent * 1.0f, "%"));
                            break;
                        case 1032: //end update
                            updateUi.updateInstallMessage(target + " Update Completed");
                            Log.d(TAG, target + " update successful");
                            mIoUpdateSuccessful = true;
                            mIoLatch.countDown();
                            break;
                        case 1:
                            updateUi.updateInstallMessage(target + " Update Failed: MCU no response");
                            Log.e(TAG, target + " update failed, MCU no response");
                            mIoLatch.countDown();
                            mIoUpdateSuccessful = false;
                            break;
                    }
                }
            };
            registerReceiver(ioResult, new IntentFilter("com.jht.updateio.result"));

            // Note due to https://jhtnaee.atlassian.net/browse/CHIM-2917, we have seen a case
            // where the broadcast is called before HAL has fully started.  We are too fast
            // to get to this point.  This led to an infinite loop of the update always failing.
            // Lets give HAL a few seconds to wake up.
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }

            Log.d(TAG, "IOUpdate: start update for " + target);
            Intent ioUpdateBroadcast = new Intent(UPDATE_IO_BROADCAST);
            ioUpdateBroadcast.putExtra("path", firmware.getAbsolutePath());
            ioUpdateBroadcast.putExtra("target", target);
            sendBroadcast(ioUpdateBroadcast);

            boolean hasFinished;
            // Wait for the IO update to complete
            try {
                hasFinished = mIoLatch.await(IO_UPDATE_TIMEOUT, TimeUnit.SECONDS);
                if (hasFinished) Log.d(TAG, target + " update complete");
                else Log.e(TAG, target + " update timed out!");
            } catch (InterruptedException e) {
                Log.e(TAG, target + " update interrupted!", e);
                hasFinished = false;
            }

            return hasFinished && mIoUpdateSuccessful;

        }
        return false;
    }

    private boolean updateLCB(int lcbType) {

        // Check if LCB type of system matches
        try {
            int systemLcbType = Settings.Secure.getInt(getApplicationContext().getContentResolver(), "chimera_lcb_type"); // NON-NLS
            if (lcbType != systemLcbType) {
                return true;
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "LCB Type not found!", e);
            return false;
        }

        mLcbLatch = new CountDownLatch(1);

        File firmware = null;
        File[] fileList = getUpdatePath().listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile() && file.getName().endsWith(".bin")) {
                    firmware = file;
                    break;
                }
            }
        }

        if (firmware != null) {
            BroadcastReceiver lcbResult = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    float percent = intent.getFloatExtra("percent", 0.0f);
                    int status = intent.getIntExtra("status", 0);
                    switch (status) {
                        case 100: //start update
                            updateUi.updateInstallMessage("LCB Update Started");
                            break;
                        case 101: //update EraseFlash processing
                            updateUi.updateInstallMessage(String.format(Locale.ENGLISH, "LCB EraseFlash Update %.0f%s", percent * 1.0f, "%"));
                            break;
                        case 102: //update WriteFlash processing
                            updateUi.updateInstallMessage(String.format(Locale.ENGLISH, "LCB WriteFlash Update %.0f%s", percent * 1.0f, "%"));
                            break;
                        case 103: //update ReadFlash processing
                            updateUi.updateInstallMessage(String.format(Locale.ENGLISH, "LCB ReadFlash Update %.0f%s", percent * 1.0f, "%"));
                            break;
                        case 104: //end update
                            updateUi.updateInstallMessage("LCB Update Completed");
                            mLcbLatch.countDown();
                            break;
                    }
                }
            };
            registerReceiver(lcbResult, new IntentFilter("com.jht.updatelcb.result"));

            Log.d(TAG, "start update");
            Intent ioUpdateBroadcast = new Intent(UPDATE_LCB_BROADCAST);
            ioUpdateBroadcast.putExtra("path", firmware.getAbsolutePath());
            sendBroadcast(ioUpdateBroadcast);

            // Wait for the LCB update to complete
            boolean successful;
            try {
                successful = mLcbLatch.await(LCB_UPDATE_TIMEOUT, TimeUnit.SECONDS);
                if (successful) Log.d(TAG, "LCB update success");
                else Log.e(TAG, "LCB update timed out!");
            } catch (InterruptedException e) {
                Log.e(TAG, "LCB update error!", e);
                successful = false;
            }

            return successful;

        } else {
            Log.e(TAG, "Could not find a bin file for the LCB firmware");
        }
        return false;
    }

    private boolean updateBluetooth() {
        return updateFirmware("BT", "chimera_bluetooth_version", "bt_update.bin", ACTION_BT_UPDATE_START, 600);
    }

    private boolean updateRfid2() {
        return updateFirmware("rfid2", "chimera_rfid2_version", "update.bin", ACTION_RFID2_UPDATE, 600);
    }

    private boolean updateRfid3() {
        return updateFirmware("rfid3", "chimera_rfid3_version", "update.bin", ACTION_RFID3_UPDATE, 600);
    }

    private boolean updateRfid4() {
        return updateFirmware("rfid4", "chimera_rfid4_version", "update.bin", ACTION_RFID4_UPDATE, 600);
    }

    private boolean updateCanSingle()  {
        return updateFirmware("CAN_SINGLE", "can_single_version", "update.bin", ACTION_CAN_UPDATE, 3600);
    }

    private boolean updateCanRight() {
        return updateFirmware("CAN_RIGHT", "can_right_version", "update.bin", ACTION_CAN_UPDATE_RIGHT, 3600);
    }

    private boolean updateCanLeft() {
        return updateFirmware("CAN_LEFT", "can_left_version", "update.bin", ACTION_CAN_UPDATE_LEFT,3600);
    }

    private boolean updateBumpSensor() {
        try {
            return updateFirmware("BS", "chimera_bumpsensor_version", getFirmwareFile(), ACTION_BS_UPDATE, 600);
        } catch (FileNotFoundException e){
            Log.e(TAG, "Firmware file not found", e);
            return false;
        }
    }

    /**
     * Retrieves the firmware file from the update directory.
     *
     * @return The name of the firmware file.
     * @throws "FileNotFoundException" if no firmware file is found.
     */
    private @NonNull String getFirmwareFile() throws FileNotFoundException {
        // List all files in the update directory
        final File[] children = getUpdatePath().listFiles();
        if (children != null) {
            for (final File file : children) {
                // Check if the file is a firmware file (ends with .bin)
                if (file.isFile() && file.getName().toLowerCase().endsWith(".bin")) {
                    return file.getName();
                }
            }
        }
        throw new FileNotFoundException("No firmware file found in the update directory.");
    }

    /**
     * Update firmware logic
     *
     * @return true if successful; otherwise return false
     */
    @SuppressLint("SdCardPath")
    private boolean updateFirmware(String tagName, String secureSettingVersion, String filename, String actionIntent, int timeoutMs) {

        String versionString = Settings.Secure.getString(getApplicationContext().getContentResolver(), secureSettingVersion); // NON-NLS

        if (versionString == null || versionString.equals("N/A")) {
            // No firmware version found so skip the update
            return true;
        }

        mUpdateLatch = new CountDownLatch(1);
        File updateDir = getUpdatePath();
        File firmware = new File(updateDir, filename);
        isUpdateSuccessful = false;

        BroadcastReceiver updateProgressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.getAction() != null) {
                    if (intent.getAction().equals(ACTION_UPDATE_PROGRESS)) {
                        int progress = intent.getIntExtra(EXTRA_PROGRESS, 0);
                        updateUi.updateInstallMessage("Progress: " + progress + "%");
                    } else if (intent.getAction().equals(ACTION_UPDATE_COMPLETE)) {
                        // Handle the update completion
                        isUpdateSuccessful = intent.getBooleanExtra("success", true);
                        if (isUpdateSuccessful) {
                            Log.d(TAG, tagName + " update completed successfully");
                        } else {
                            Log.e(TAG, tagName + " update failed");
                        }
                        updateUi.updateInstallMessage("");
                        mUpdateLatch.countDown();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_PROGRESS);
        filter.addAction(ACTION_UPDATE_COMPLETE);

        registerReceiver(updateProgressReceiver, filter);

        // Send start update command with file path
        Intent startIntent = new Intent(actionIntent);
        startIntent.putExtra(EXTRA_START_PATH, firmware.getPath());

        Log.d(TAG, "broadcasting : "+startIntent.getAction() + ", "+ startIntent.getStringExtra(EXTRA_START_PATH));

        sendBroadcast(startIntent);

        try {
            mUpdateLatch.await(timeoutMs, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, tagName + " update interrupted!", e);
            return false;
        }finally {
            unregisterReceiver(updateProgressReceiver);
        }

        // Return true if success, false if otherwise
        return isUpdateSuccessful;
    }

    /**
     * Updates the touch panel firmware
     */
    private boolean updateTouchPanel() {
        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point point = new Point();
        display.getRealSize(point);
        boolean success;
        // 22in is 1920x1080, 16in is 1400x768
        if (point.x > 1500) {
            // This is a 22in console
            Log.d(TAG, "Updating 22 inch console touch panel");
            File folder = new File(getUpdatePath(), "22");
            CmdProcessHelper cmdProcessHelper = new CmdProcessHelper();
            cmdProcessHelper.addCommand("cd " + folder.getPath()); // NON-NLS
            cmdProcessHelper.addCommand("chmod 777 eUpdate2"); // NON-NLS
            cmdProcessHelper.addCommand("./eUpdate2 -i SensorTestDefault.ini"); // NON-NLS
            cmdProcessHelper.addCommand("./eUpdate2 -f tp_firmware_22.3189UR"); // NON-NLS
            success = cmdProcessHelper.executeSafe(TOUCH_PANEL_UPDATE_TIMEOUT_MS);
        } else {
            // This is a 16in console
            Log.e(TAG, "Updating 16 inch console touch panel");
            File folder = new File(getUpdatePath(), "16");
            CmdProcessHelper cmdProcessHelper = new CmdProcessHelper();
            cmdProcessHelper.addCommand("cd " + folder.getPath()); // NON-NLS
            cmdProcessHelper.addCommand("chmod 777 eUpdate2"); // NON-NLS
            cmdProcessHelper.addCommand("./eUpdate2 -i SensorTestDefault.ini"); // NON-NLS
            cmdProcessHelper.addCommand("./eUpdate2 -f tp_firmware_16.3188UI"); // NON-NLS
            success = cmdProcessHelper.executeSafe(TOUCH_PANEL_UPDATE_TIMEOUT_MS);
        }

        // If not successful, return failure
        return success;
    }

    private boolean updateTouchPanel2() {

        File updateDir = getUpdatePath();
        File hexPath = null;
        String panelId = SystemCommands.getSystemProperty("ro.boot.panel_id");
        switch (panelId) {
            case "9":
                Log.d(TAG, "Updating 22 inch chimera2.0 console touch panel");
                for (File file : updateDir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".hex") && file.getName().contains("215F00")) {
                        hexPath = file;
                        break;
                    }
                }
                break;
            case "7":
                Log.d(TAG, "Updating 16 inch chimera2.0 console touch panel");
                for (File file : updateDir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".hex") && file.getName().contains("156F00")) {
                        hexPath = file;
                        break;
                    }
                }
                break;
        }
        if(hexPath != null && hexPath.exists()) {
            SystemCommandResult result_command = ExecuteAsRoot.executeCommand("ls /vendor/bin | grep Linux_Daemon_ITS");
            String command = result_command.output().trim();
            if (command.equals("")){
                Log.e(TAG,"touch firmware tool Linux_Daemon_ITS not found!!");
            }
            Log.d(TAG, "updateTouchPanel2: hex Path " + hexPath.getAbsolutePath());
            SystemCommandResult result = ExecuteAsRoot.executeCommand(command + " FWUpgrade USB V3 null null " + hexPath.getAbsolutePath());
            String[] output = result.output().split("\n");
            if (output != null) {
                for (String line : output) {
//                    Log.d(TAG, "updateTouchPanel2: " + line);
                    if(line.contains("Success"))
                        return true;
                }
            }
        }
        else
            Log.d(TAG, "updateTouchPanel2: hexPath doesn't exit");

        return false;
    }

    private boolean updateLCDTouchFirmware() {
        // Attempt to locate the LCD Touch firmware files.
        final File[] children = getUpdatePath().listFiles();
        File binary = null;

        // only check children if not null.
        if (children != null) {
            for (final File child : children) {
                // Only check files.
                if (child.isFile() && child.getName().toLowerCase().endsWith(".bin")) {
                    binary = child;
                    CmdProcessHelper.chmod("777", binary.getPath());
                    break;
                }
            }
        }

        // If the binary firmware file was found, attempt the update.
        if (binary != null) {
            final String target = "LCD Touch Firmware";
            final JHTDataNotification<Boolean> updateResult = new JHTDataNotification<>();
            final BroadcastReceiver touchFirmwareResult = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int percent = intent.getIntExtra("lcd_touch_firmware_progress", 0);
                    boolean finished = intent.getBooleanExtra("finished", false);
                    String result = intent.getStringExtra("result");
                    if (finished) {
                        if (result == null || result.isEmpty()) {
                            updateUi.updateInstallMessage(target + " Update Completed");
                        } else {
                            updateUi.updateInstallMessage(target + " Update Failed: " + result);
                        }
                        updateResult.setData(result == null || result.isEmpty());
                    } else {
                        updateUi.updateInstallMessage(String.format(Locale.ENGLISH, "%s Update %d%s", target, percent, "%"));
                    }
                }
            };
            registerReceiver(touchFirmwareResult, new IntentFilter("com.jht.updatelcdtouchfirmware.result"));

            Intent touchFirmwareUpdateBroadcast = new Intent(UPDATE_LCD_TOUCH_FIRMWARE_BROADCAST);
            touchFirmwareUpdateBroadcast.putExtra("path", binary.getAbsolutePath());
            sendBroadcast(touchFirmwareUpdateBroadcast);

            Boolean success = updateResult.waitForData(LCD_TOUCH_PANEL_UPDATE_TIMEOUT_MS);
            if (success == null) {
                Log.e(TAG, target + "LCD Touch Panel update timed out!");
            }
            return success != null && success;
        }

        Log.d(TAG, "LCD Touch Firmware File Not Found!");
        return false;
    }

    private boolean updateTunerFirmware(File firmware, String target) {

        JHTDataNotification<Boolean> updateResult = new JHTDataNotification<>();

        if (firmware != null && firmware.exists()) {
            BroadcastReceiver tunerResult = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int percent = intent.getIntExtra("progress", 0);
                    boolean finished = intent.getBooleanExtra("finished", false);
                    String result = intent.getStringExtra("result");
                    if (finished) {
                        if (result == null || result.isEmpty()) {
                            updateUi.updateInstallMessage(target + " Update Completed");
                        } else {
                            updateUi.updateInstallMessage(target + " Update Failed: " + result);
                        }
                        updateResult.setData(result == null || result.isEmpty());
                    } else {
                        updateUi.updateInstallMessage(String.format(Locale.ENGLISH, "%s Update %d%s", target, percent, "%"));
                    }
                }
            };
            registerReceiver(tunerResult, new IntentFilter("com.jht.updatetuner.result"));

            // Note due to https://jhtnaee.atlassian.net/browse/CHIM-2917, we have seen a case
            // where the broadcast is called before HAL has fully started.  We are too fast
            // to get to this point.  This led to an infinite loop of the update always failing.
            // Lets give HAL a few seconds to wake up.
            try {
                Thread.sleep(15000);
            } catch (InterruptedException ignored) {
            }

            Log.d("TunerUpdate", "start update for " + target);
            Intent tunerUpdateBroadcast = new Intent(UPDATE_TUNER_BROADCAST);
            tunerUpdateBroadcast.putExtra("path", firmware.getAbsolutePath());
            sendBroadcast(tunerUpdateBroadcast);

            int timeoutTime = TUNER_UPDATE_TIMEOUT;
            if (BuildConfig.UPDATE_EQUIPMENT.equals("projectone")) timeoutTime *= 10; // x10 for P1 slow serial communication
            Boolean success = updateResult.waitForData(timeoutTime);
            if (success == null) {
                Log.e(TAG, target + " update timed out!");
            }
            return success != null && success;


        }
        return false;
    }

    private boolean updateSalutron() {
        mSalutronLatch = new CountDownLatch(1);
        BroadcastReceiver salutronReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int status = intent.getIntExtra("status", -1);
                switch (status) {
                    case SALUTRON_UPDATE_PERCENT_ID:
                        float percent = intent.getFloatExtra("percent", 0);
                        Log.d(TAG, "Salutron progress: " + percent);
                        break;
                    case SALUTRON_UPDATE_START_ID:
                        Log.d(TAG, "Salutron update started");
                        break;
                    case SALUTRON_UPDATE_COMPLETE_ID:
                        Log.d(TAG, "Salutron update completed");
                        mSalutronLatch.countDown();
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.jht.update8700.result");

        Context context = getApplicationContext();
        context.registerReceiver(salutronReceiver, filter);

        File manifestJson = new File(getUpdatePath(), "manifest.json");
        Intent startUpdateIntent = new Intent("com.jht.hal.update.salutron");
        startUpdateIntent.putExtra("path", manifestJson.getPath());
        context.sendBroadcast(startUpdateIntent);
        boolean successful;
        try {
            // 7 minute timeout, looked like the update took approximately 3 minutes locally
            successful = mSalutronLatch.await(420, TimeUnit.SECONDS);
            if (successful) Log.d(TAG, "Salutron updated successfully");
            else Log.e(TAG, "Salutron update timed out!");
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted when updating salutron!", e);
            successful = false;
        }

        Log.d(TAG, "Completed update for salutron");
        return successful;
    }


    private int updateOS() {

        final JHTNotification osCompleteNotification = new JHTNotification();
        osUpdateSuccessful = false;
        UpdateEngineCallback callback = new UpdateEngineCallback() {

            @Override
            public void onStatusUpdate(int status, float percent) {
                Log.v(TAG, "status: " + status + " | percent: " + percent);
                switch (status) {
                    case 0:
                        updateUi.updateInstallMessage("OS Update Idle");
                        break;
                    case 1:
                        updateUi.updateInstallMessage("OS Update Checking For Update");
                        break;
                    case 2:
                        updateUi.updateInstallMessage("OS Update Update Available");
                        break;
                    case 3:
                        updateUi.updateInstallMessage(String.format(Locale.ENGLISH, "OS Update Downloading %.0f%s", percent * 100.0f, "%"));
                        break;
                    case 4:
                        updateUi.updateInstallMessage("OS Update Verifying");
                        break;
                    case 5:
                        updateUi.updateInstallMessage("OS Update Finalizing");
                        break;
                    case 6:
                        updateUi.updateInstallMessage("OS Update Completed");
                        osUpdateSuccessful = true;
                        osCompleteNotification.setNotification();
                        break;
                    case 7:
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        updateUi.updateInstallMessage("OS Update OS Update Error");
                        break;
                    case 8:
                        updateUi.updateInstallMessage("OS Update Attempting Rollback");
                        break;
                    case 9:
                        updateUi.updateInstallMessage("OS Update Disabled");
                        break;
                    default:
                        updateUi.updateInstallMessage("Status: Unknown status " + status);
                        break;
                }
            }

            @Override
            public void onPayloadApplicationComplete(int errorCode) {
                Log.v(TAG, "errorCode: " + errorCode);
                switch (errorCode) {
                    case 0:
                        break;
                    case 2:
                        updateUi.updateInstallMessage("OS Update Failed: Payload Properties File Not Found Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 3:
                        updateUi.updateInstallMessage("OS Update Failed: Payload File Not Found Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 1:
                        updateUi.updateInstallMessage("OS Update Failed: Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 4:
                        updateUi.updateInstallMessage("OS Update Failed: Filesystem Copier Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 5:
                        updateUi.updateInstallMessage("OS Update Failed: Post Install Runner Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 6:
                        updateUi.updateInstallMessage("OS Update Failed: Payload Mismatched Type Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 7:
                        updateUi.updateInstallMessage("OS Update Failed: Install Device Open Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 8:
                        updateUi.updateInstallMessage("OS Update Failed: Kernel Device Open Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 9:
                        updateUi.updateInstallMessage("OS Update Failed: Download Transfer Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 10:
                        updateUi.updateInstallMessage("OS Update Failed: Payload Hash Mismatch");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 11:
                        updateUi.updateInstallMessage("OS Update Failed: Payload Size Mismatch");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 12:
                        updateUi.updateInstallMessage("OS Update Failed: Payload Verification Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;

                    case 13:
                        updateUi.updateInstallMessage("OS Update Failed: Download New Partition Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 14:
                        updateUi.updateInstallMessage("OS Update Failed: Payload Download Write Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 15:
                        updateUi.updateInstallMessage("OS Update Failed: New Rootfs Verification Error");
                        Log.e(TAG, "Update error, likely the same version of the OS being installed. Advancing as successful!");
                        osUpdateSuccessful = true;
                        osCompleteNotification.setNotification();
                        break;

                    case 16:
                        updateUi.updateInstallMessage("OS Update Failed: New Kernel Verification Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 17:
                        updateUi.updateInstallMessage("OS Update Failed: Delta Payload Expected Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 18:
                        updateUi.updateInstallMessage("OS Update Failed: Download Payload Pub Key Verification Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 19:
                        updateUi.updateInstallMessage("OS Update Failed: Post Install Booted From Firware B Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 20:
                        updateUi.updateInstallMessage("OS Update Failed: Download State Initialization Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 21:
                        updateUi.updateInstallMessage("OS Update Failed: Download Invalid Metadata Magic String Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 22:
                        updateUi.updateInstallMessage("OS Update Failed: Signature Missing In Manifest Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 23:
                        updateUi.updateInstallMessage("OS Update Failed: Manifest Parse Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 24:
                        updateUi.updateInstallMessage("OS Update Failed: Metadata Signature Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 25:
                        updateUi.updateInstallMessage("OS Update Failed: Metadata Signature Verification Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 26:
                        updateUi.updateInstallMessage("OS Update Failed: Metadata Signature Mismatch");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 27:
                        updateUi.updateInstallMessage("OS Update Failed: Operation Hash Verification Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 28:
                        updateUi.updateInstallMessage("OS Update Failed: Operation Execution Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 29:
                        updateUi.updateInstallMessage("OS Update Failed: Operation Hash Mismatch");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 30:
                        updateUi.updateInstallMessage("OS Update Failed: Omaha Request Empty Response Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 31:
                        updateUi.updateInstallMessage("OS Update Failed: Omaha Request XML Parse Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 32:
                        updateUi.updateInstallMessage("OS Update Failed: Download Invalid Metadata Size");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 33:
                        updateUi.updateInstallMessage("OS Update Failed: Download Invalid Metadata Signature");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 34:
                        updateUi.updateInstallMessage("OS Update Failed: Omaha Response Invalid");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 35:
                        updateUi.updateInstallMessage("OS Update Failed: Omaha Update Ignored Per Policy");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 36:
                        updateUi.updateInstallMessage("OS Update Failed: Omaha Update Deferred Per Policy");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 37:
                        updateUi.updateInstallMessage("OS Update Failed: Omaha Error In HTTP Response");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 38:
                        updateUi.updateInstallMessage("OS Update Failed: Download Operation Hash Missing Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 39:
                        updateUi.updateInstallMessage("OS Update Failed: Download Metadata Signature Missing Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 40:
                        updateUi.updateInstallMessage("OS Update Failed: Omaha Update Deferred For Backoff");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 41:
                        updateUi.updateInstallMessage("OS Update Failed: Post Install Power Wash Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 42:
                        updateUi.updateInstallMessage("OS Update Failed: Update Canceled By Channel Change");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 43:
                        updateUi.updateInstallMessage("OS Update Failed: Post Install Firmware RO Not Updatable");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 44:
                        updateUi.updateInstallMessage("OS Update Failed: Unsupported Major Payload Version");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 45:
                        updateUi.updateInstallMessage("OS Update Failed: Unsupported Minor Payload Version");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 46:
                        updateUi.updateInstallMessage("OS Update Failed: Omaha Request XML Has Entity Decl");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 47:
                        updateUi.updateInstallMessage("OS Update Failed: File System Verifier Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 48:
                        updateUi.updateInstallMessage("OS Update Failed: User Canceled");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 49:
                        updateUi.updateInstallMessage("OS Update Failed: Non Critical Update In OOBE");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 51:
                        updateUi.updateInstallMessage("OS Update Failed: Payload Timestamp Error");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    case 52:
                        updateUi.updateInstallMessage("OS Update Failed: Updated But Not Active");
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                    default:
                        updateUi.updateInstallMessage("OS Update Failed: Unknown error " + errorCode);
                        osUpdateSuccessful = false;
                        osCompleteNotification.setNotification();
                        break;
                }
            }
        };

        File payload = new File(getUpdatePath(), "/payload.bin");
        File payloadProperties = new File(getUpdatePath(), "/payload_properties.txt");

        if (payload.exists() && payloadProperties.exists()) {

            UpdateOS updateOS = new UpdateOS(getApplicationContext());
            if (!updateOS.update(callback, payload, payloadProperties)) {
                // return if an exception occurred try to perform update
                return -3;
            }
            osCompleteNotification.waitForNotification(OS_UPDATE_TIMEOUT);
            return osUpdateSuccessful ? 0 : -1;
        }
        return -2;
    }
}
