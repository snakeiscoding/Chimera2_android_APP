package com.jht.splashapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.VersionHelper;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.bluetooth.BTSinkCommand;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.dapi.DapiLogin;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.BTSinkKeys;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TiersKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.widget.dialog.ProductDashboardHost;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.common.i18n.LocaleUtil;
import com.jht.chimera.lib.common.log.ConsoleEvent;
import com.jht.chimera.lib.common.process.KillProcessHelper;
import com.jht.chimera.lib.common.stats.WorkoutSummaryList;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.widget.sound.SystemVolume;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workoutapps.AppInfo;
import com.jht.chimera.lib.workoutapps.AppManager;
import com.jht.chimera.service.ConsoleServicesHelper;
import com.jht.chimera.service.dapi.UserProfileManager;
import com.jht.chimera.service.hal.lib.hardware.AudioSelectManager;
import com.jht.chimera.service.hal.lib.io.iocommlib.PowerType;
import com.jht.chimera.service.sso.SingleSignOnService;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gattserver2.BtBondUtilKt;
import com.jht.gymkitmodule.comm.GsCommands;
import com.jht.interfaces.models.MatrixProgressionModelKt;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * As a first step to cleaning up SplashScreen, move code not directly related to the UI into a separate class.
 * There should be NO UI code in here.
 * Could some of this logic be moved back into services done in response to ACTION_SPLASH_RESET ?
 */
public class SplashScreenLogic {
    private static final String TAG = "SplashScreenLogic";

    private final Handler mHandler = new Handler();
    private final Context context;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public SplashScreenLogic() { this.context = ContextUtil.getApplicationContext(); } // using static instance so need app context, hope to replace with a service context

    /*
        See MatrixApplication for application start up code
     */

    public void showSettingsDialog() {
        mHandler.post(ProductDashboardHost::show);
    }

    public void onSplashPause() {
        ProductDashboardHost.dismiss();
    }

    public void runLogic(int minimumWaitTimeMS) {
        if (isRunning.getAndSet(true)) {
            Log.d(TAG, "splash logic is already running, ignoring runLogic request");
            return;
        }
        Log.d(TAG, "splash logic started");
        // NOTE: Most things cannot be launched until the UI service has started!

        if (DataMatrix.getBoolean(MainAppKeys.DATA_RUNNING_INSTRUMENTATION_TEST_BOOL)) {
            LaunchAppUtil.launchAndroidLauncher();
            return;
        }

        // support disabling software if machine hasn't been setup and special file exists
        if (new File(StorageUtils.getUSBPath(), "jht_disable_chimera.txt").exists()) {
            LaunchAppUtil.launchAndroidLauncher();
            return;
        }

        // Turn on Qi wireless.  It may have been turned off during the workout due to not enough
        // power.
        // TODO: We need to log an analytics event.
        if (DataMatrix.getBoolean(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL)) {
            DataMatrix.putInt(HALKeys.ACTION_IO_POWER_ON_DEVICE_ENUM_INT, PowerType.qiPower.getID());
        }

        ConsoleServicesHelper servicesHelper = new ConsoleServicesHelper(); // best to create DM holder on UI thread
        new Thread(() -> {
            try {
                DataMatrix.sendEvent(HALKeys.ACTION_SPLASH_RESET);
                DataMatrix.putBoolean(TVKeys.DATA_CAMERA_IS_OPEN_BOOL, false);
                DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, "");
                DataMatrix.putParcelable(WorkoutKeys.DATA_WORKOUT_APP_SETUP_PARCELABLE, Bundle.EMPTY);
                DataMatrix.putString(WorkoutKeys.DATA_CUSTOM_WORKOUT_UUID_STRING, "");
                DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.SPLASH_SCREEN.name());

                dropPreviousSession();
                servicesHelper.waitForServices(minimumWaitTimeMS);
                resetDefaults();

                launchApp();

                // We have been lazy using root to copy files to the package.  This causes the
                // files to have the wrong security which can prove problematic.  This one line
                // covers our copy sins.
                //
                // Runs AFTER the home screen launches to speed up boot time: three recursive
                // walks of /data/data take hundreds of ms to seconds and nothing before first
                // user interaction depends on them.
                ExecuteAsRoot.fixFilesInPackage("com.jht.chimera");

            } catch (Exception e) {
                Log.e(TAG, "Failed to setup Matrix app", e); // do not let thread die quietly
            } finally {
                servicesHelper.close();
                isRunning.set(false);
                Log.d(TAG, "splash logic finished");
            }
        }, "splashReset").start();
    }

    void launchApp() {
        boolean isMachineSetup = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL);
        Log.d(TAG, "--> Boot launchApp called uptime: " + (SystemClock.uptimeMillis() / 1000.0) + " seconds");

        // Make sure the frame is set correctly.
        boolean frameFound = false;
        String frame = DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING);
        for (FrameConfigEnum f : FrameConfigEnum.getEntries()) {
            if (f.name().equals(frame)) {
                frameFound = true;
                break;
            }
        }

        boolean systemBooted = DataMatrix.getBoolean(MainAppKeys.DATA_SYSTEM_BOOTED_BOOL);
        DataMatrix.updateBoolean(MainAppKeys.DATA_SYSTEM_BOOTED_BOOL, true);
        //First time the system has booted, show console version logs
        if(!systemBooted){
            VersionHelper.getFirmwareVersionsAsync("", "\n").thenAcceptAsync(ConsoleEvent::logEvent);
        }

        // (Setup Wizard check-box changes SHOW_MACHINE_SETUP_BOOL value which flips the IS_MACHINE_SETUP on first boot)
        // (FTS should only show on reboot according to the Spec)
        // https://jhtnaee.atlassian.net/browse/CHIM-4976 for more info.
        // Regardless, we should do FTS if frameFound is false.
        if (isMachineSetup && frameFound) {
            String appUuid = DataMatrix.getString(SettingKeys.Experimental.DATA_OVERRIDE_MAIN_APP_ID_STRING);
            AppInfo appInfo = null; //Initialize appInfo as null; it will only be looked up if an override UUID is registered
            AppManager.getInstance().reset();
            if (appUuid != null && !appUuid.isEmpty()) { //Guard: Only look up log if the override UUID is not empty
                appInfo = AppManager.getInstance().getApp(appUuid);
                Log.d(TAG, "appInfo: app package:" + appInfo.appPackage);

                PackageManager pm = context.getPackageManager();
                List<PackageInfo> packages = pm.getInstalledPackages(0);
                boolean foundOverrideApp = false;
                for (PackageInfo packageInfo : packages) {
                    if(appInfo != null && Objects.equals(appInfo.appPackage, packageInfo.packageName)){
                        Log.d(TAG, "Found Override app " + appInfo.appPackage + " '" + appUuid + "'");
                        foundOverrideApp = true;
                    }
                }
                if (!foundOverrideApp) {
                    Log.e(TAG, "cannot find alternative start app appPackage " + appInfo.appPackage);
                    Toast.makeText(context, "Cannot find alternative start app" + appInfo.appPackage, Toast.LENGTH_LONG).show();
                    showSettingsDialog();
                }
            }

            if (appInfo != null) {
                Log.d(TAG, "launching alternative start app " + appInfo.appPackage);
                LaunchAppUtil.initialLaunchApp(context, appUuid, null);
            } else if (DataMatrix.getBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL)) {
                LaunchAppUtil.launchEGym();
            } else if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL)) {
                Log.d(TAG, "launching welcome screen");
                LaunchAppUtil.launchWelcomeScreen();
            } else {
                LaunchAppUtil.launchGoScreen();
            }
            if (DataMatrix.getBoolean(SettingKeys.General.DATA_SHOW_OUT_OF_ORDER_BOOL)) {
                LaunchAppUtil.launchOutOfOrderScreen();
            }
            DataMatrix.putBoolean(HALKeys.ACTION_START_RFID_MANAGER_BOOL, true);
        } else {
            LaunchAppUtil.launchMachineSetup();
        }
    }

    /**
     * Note most of the killing is now done in the single sign on service.
     * That is because we need to time it after we have uploaded the user data.
     * No need to kill third party apps here.
     */
    void dropPreviousSession() {
        KillProcessHelper killProcessHelper = new KillProcessHelper();
        killProcessHelper.addPackageToKill("com.jht.mainapp", true);
        killProcessHelper.addPackageToKill("com.jht.machinesettings", true);
        killProcessHelper.addPackageToKill("com.jht.machinesetup", true);
        killProcessHelper.addPackageToKill("com.jht.egym", true);
        if (ProductConfig.isCommercial()) {
            killProcessHelper.addPackagesToClear("com.ifit.standalone", false);
            // Note this is done because we are running out of GATT connections.  That
            // happens if you don't close it properly.  Looks like there is a bug in
            // the myzone service for this.  Adding this line fixes that problem.
            //killProcessHelper.addPackagesToClear("com.android.bluetooth", false);
        }
        killProcessHelper.kill();

        if (ProductConfig.needToPurgeUserData()) {
            // We need to always clear this in the splash screen if is commercial.
            SingleSignOnService.killUserWebData();

            if (ProductConfig.hasBTSink()) {
                byte[] bytes = DataMatrix.getBytes(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_BYTES);
                if (bytes != null) {
                    BTSinkCommand.sendCommand(GsCommands.BtDeleteBond.Value, bytes);
                }
                DataMatrix.sendEvent(BTSinkKeys.EVENT_BT_SINK_RESET_SEQUENCE);
                //Disconnect BT Sink
                DataMatrix.putBoolean(HALKeys.DATA_IS_BT_SINK_CONNECTED_BOOL, false);
            }
            DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, true);
        } else {
            // For non-guest have SSO purge it when changing users?
            if (DapiUser.isGuestUser()) {
                // This used to go with the killing of apps in the single sign on service.  We place it here
                // to avoid the race condition where we pull the app_web* directory out from under the workout summary.
                SingleSignOnService.killUserWebData();
            }
        }

        // BT Devices will be disconnected
        DataMatrix.putBoolean(TopBarKeys.DATA_BT_HEADPHONE_CONNECTED_BOOL, false); // RGC-3524 cover devices that might not raise a disconnection event?
        toggleBluetoothAdapter();
    }

    void resetDefaults() {
        DataMatrix.sendEvent(HALKeys.ACTION_STOP_MACHINE);
        Log.d(TAG, "####CS Stop machine trigger by splash");
        //DataMatrix.putBoolean(UiServiceKeys.ACTION_DISABLE_SAFETY_SCREENS_BOOL, false);
        DataMatrix.putBoolean(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL, true);
        if (!ProductConfig.hasUserProfiles()) {
            LanguageProperty lp = LanguageProperty.getLanguagePropertyFromIndex(DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT));
            LocaleUtil.changeLanguage(context, lp, false);
            DapiLogin.logout();
        }
        DataMatrix.putStringList(MainAppKeys.DATA_LAUNCHED_SSO_APP_PACKAGE_STRING_LIST, new LinkedList<>()); // does this belong in SSO killApps ??? what if upload is not done?
        if (ProductConfig.needToPurgeUserData()) {  // for clubs, mute the home screen
            SystemVolume.getInstance().setVolume(0); // AudioSelectManager will set the volume to default when workout starts
        } else {
            SystemVolume.getInstance().setVolume(AudioSelectManager.getMasterVolume());
        }
        DataMatrix.putInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT, 0);

        DataMatrix.sendEvent(WorkoutKeys.ACTION_RESET_STATS);
        DataMatrix.putBoolean(HALKeys.DATA_HAVE_MYZONE_TIME_IN_ZONES_BOOL, false);

        // Reset clock and date format. Units reset by logout of user
        DataMatrix.putInt(ImportExportKeys.DATA_CURRENT_DATE_FORMAT_INT, DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT));
        DataMatrix.putBoolean(ImportExportKeys.DATA_CURRENT_CLOCK_FORMAT_IS_24_HOURS_BOOL, DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL));

        DataMatrix.putString(WorkoutKeys.DATA_SELECTED_VA_OR_LANDMARK_TITLE_STRING, "");
        DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_ORIGINATOR_STRING, "");

        DataMatrix.putBoolean(TiersKeys.DATA_UPDATE_TIERS_BOOL, false);

        DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_APP_PAGE_INT, 0);

        DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_IS_AVRCP_READY_BOOL, false);

        if (DataMatrix.getBoolean(MainAppKeys.DATA_SPLASH_ANALYTICS_BOOL)) {
            DataMatrix.putBoolean(MainAppKeys.DATA_SPLASH_ANALYTICS_BOOL, false);
            DataMatrix.putString(DAPIKeys.ACTION_DAPI_ANALYTICS_STRING, DapiUtil.getMachineId());
        }

        DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT, WorkoutKeys.Constants.WORKOUT_STAGE_WARMUP);
        DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_GENDER_INT, ConsoleWorkoutVariables.GENDER_MALE_CONSTANT);
        DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_NUMBER_OF_INTERVALS_INT, 0);
        DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_INTERVAL_DISTANCE_GOAL_KM_FLOAT, 0f);
        DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_REST_TIME_GOAL_FLOAT, 0f);
        DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_INTERVAL_TIME_GOAL_FLOAT, 0f);

        DataMatrix.putParcelable(WorkoutKeys.DATA_INTERVAL_LIST_PARCELABLE, new WorkoutSummaryList()); // Clear this at the splash screen

        DataMatrix.putString(DAPIKeys.DATA_USER_BLE_HR_MAC_ID_STRING, "");
        DataMatrix.putString(DAPIKeys.DATA_USER_BLE_AUDIO_MAC_ID_STRING, "");
        DataMatrix.putString(DAPIKeys.DATA_USER_ANTPLUS_HR_DEVICE_ID_STRING, "");

        DataMatrix.putString(MainAppKeys.DATA_WEB_BROWSER_CURRENT_PAGE_STRING, DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_WEBPAGE_STRING));

        if (ProductConfig.hasUserProfiles()) {
            DataMatrix.sendEvent(UserProfileManager.Keys.EVENT_REFRESH_CURRENT_SLOT); // reload the last profile
        }

        // reset progression keys and data
        DataMatrix.putString(DAPIKeys.DATA_PROGRESSION_PROGRAM_STRING, "");
        MatrixProgressionModelKt.resetGuestUser();
    }

    @SuppressLint("MissingPermission")
    private void toggleBluetoothAdapter() {
        // on a cold boot, don't toggle the bluetooth adapter
        if (!DataMatrix.getBoolean(MainAppKeys.DATA_SYSTEM_BOOTED_BOOL)) return;

        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            BluetoothAdapter adapter = manager.getAdapter();
            if (adapter != null) {
                // purge all bonds first
                for (BluetoothDevice device : adapter.getBondedDevices()) {
                    boolean success = BtBondUtilKt.removeBond(device);
                    Log.d(TAG, "removeBond " + device.getName() + " " + device.getAddress() + " " + success);
                }
                SystemClock.sleep(1000); // BT operations tend to be async, so maybe give this a little time
                // then toggle adapter
                adapter.disable();
                SystemClock.sleep(500); // we could pass the delay as a parameter if we wanted
                adapter.enable();
            }
        }
    }

}
