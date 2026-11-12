
package com.jht.chimera.service.dapi;

import static android.app.AlarmManager.ELAPSED_REALTIME;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.dapi.DapiGetSoftware;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.update.UpdateValues;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.updatemanager.IUpdateCallback;
import com.jht.updatemanager.UpdateManagerClient;
import com.jht.updatemanager.UpdateManagerTypes;
import com.jhtp.dapiservice.DapiResult;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import dapi.IOtaUpdateInterface;

/**
 * Facilitates Auto Update features such as checking the update at a certain time, downloading in
 * the background, and starting the update.
 */
public class AutoUpdateManager {
    @NonNls public static final String TAG = "AutoUpdateManager";
    private static final String ALARM_INTENT = "com.jht.update_alarm"; //NON-NLS
    public static final String SYSTEM_RSCU_PATH = "/sdcard/Download"; //NON-NLS

    private UpdateManagerClient mUpdateClient;
    private static final String AUTO_UPDATE_ID = "auto_update_manager_id"; //NON-NLS

    protected DataMatrix mDataMatrix = new DataMatrix();
    private BroadcastReceiver mUpdateBroadcastReceiver;
    private PendingIntent mPendingIntent;
    private AlarmManager mAlarmManager;

    private int mClubId = -1;
    private int mDapiEnvironment = -1;

    public static AutoUpdateManager getInstance() {
        return AutoUpdateManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final AutoUpdateManager INSTANCE = new AutoUpdateManager();
    }

    private AutoUpdateManager() {
        Context context = ContextUtil.getApplicationContext();
        mUpdateClient = new UpdateManagerClient(context, new IUpdateCallback() {
            @Override
            public void applyUpdatesCallback(int returnCode, String id, Intent intent) {
                // Not needed to be handled here
            }

            @Override
            public void checkUpdateCallback(int returnCode, String id, Intent intent) {
                if (!id.equals(AUTO_UPDATE_ID)) return;
                Log.d(TAG, "Auto update checkUpdate callback " + id);
                boolean isAutoUpdateReady = false;
                long currentTime = Calendar.getInstance().getTime().getTime();
                DataMatrix.putLong(SettingKeys.Update.DATA_LAST_CHECK_TIME_LONG, currentTime);

                switch (returnCode) {
                    case UpdateManagerTypes.CHECK_UPDATE_NEW_UPDATE:
                        Log.i(TAG, "starting background download");
                        mUpdateClient.startBackgroundDownload(AUTO_UPDATE_ID);
                        break;
                    case UpdateManagerTypes.CHECK_UPDATE_NO_UPDATES:
                        Log.i(TAG, "No new updates available");
                        break;
                    case UpdateManagerTypes.CHECK_UPDATE_NO_UPDATE_FOUND:
                        Log.d(TAG, "No update found when checking updates!");
                        break;
                    case UpdateManagerTypes.CHECK_UPDATE_NOT_ENOUGH_STORAGE:
                        Log.d(TAG, "Not enough storage!");
                        break;
                    case UpdateManagerTypes.CHECK_UPDATE_ALREADY_UPDATING:
                        Log.d(TAG, "Already updating!");
                        break;
                    case UpdateManagerTypes.CHECK_UPDATE_ERROR:
                        Log.e(TAG, "Error occurred when updating!");
                        // No software found means it failed as well since there should be a software defined for the package
                        // return in case internet wasn't working but auto update is ready
                        return;
                    case UpdateManagerTypes.CHECK_UPDATE_DOWNLOADING_UPDATE:
                        Log.i(TAG, "Downloading software, waiting to install until software download has completed.");
                        break;
                    case UpdateManagerTypes.CHECK_UPDATE_READY_TO_INSTALL:
                        Log.i(TAG, "Ready to install");
                        isAutoUpdateReady = true;
                        break;
                    default:
                        Log.e(TAG, "Could not get auto update, return code " + returnCode);
                        break;
                }

                DataMatrix.putBoolean(DAPIKeys.DATA_IS_AUTO_UPDATE_READY_BOOL, isAutoUpdateReady);

                // reset the boolean indicating the update dialog has been shown
                if (!isAutoUpdateReady) DataMatrix.putBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_DLG_SHOWN_BOOL, false);
            }

            @Override
            public void downloadedUpdateFilesCallback(int returnCode, String id, Intent intent) {
                if (!id.equals(AUTO_UPDATE_ID)) return;
                switch (returnCode) {
                    case UpdateManagerTypes.DOWNLOAD_SUCCESSFUL:
                        Log.d(TAG, "Files have been downloaded successfully");
                        DataMatrix.putBoolean(DAPIKeys.DATA_IS_AUTO_UPDATE_READY_BOOL, true);
                        DataMatrix.putBoolean(TopBarKeys.DATA_IS_UPDATE_DOWNLOADING_BOOL, false);
                        break;
                    case UpdateManagerTypes.DOWNLOAD_STARTED:
                        Log.d(TAG, "Files have started downloading");
                        DataMatrix.putBoolean(TopBarKeys.DATA_IS_UPDATE_DOWNLOADING_BOOL, true);
                        break;
                    case UpdateManagerTypes.DOWNLOAD_FAILED:
                        Log.d(TAG, "Files have failed downloading");
                        DataMatrix.putBoolean(TopBarKeys.DATA_IS_UPDATE_DOWNLOADING_BOOL, false);
                        break;
                }
            }

            @Override
            public void checkStatusCallback(int returnCode, String id) {
                if (!id.equals(AUTO_UPDATE_ID)) return;
                switch (returnCode) {
                    case UpdateManagerTypes.CHECK_STATUS_DOWNLOADING:
                        DataMatrix.putBoolean(TopBarKeys.DATA_IS_UPDATE_DOWNLOADING_BOOL, true);
                        break;
                    case UpdateManagerTypes.CHECK_STATUS_READY_TO_INSTALL:
                        DataMatrix.putBoolean(DAPIKeys.DATA_IS_AUTO_UPDATE_READY_BOOL, true);
                        break;
                    case UpdateManagerTypes.CHECK_STATUS_ERROR:
                        Log.e(TAG, "Error in checking the update status");
                        break;
                    case UpdateManagerTypes.CHECK_STATUS_NO_ACTION:
                    case UpdateManagerTypes.CHECK_STATUS_UPDATING:
                    default:
                        // No action needed
                        break;
                }
            }
        });

        mUpdateClient.initOtaUpdates(new IOtaUpdateInterface() {
            @Override
            public String getSystemSoftwareType() {
                return UpdateValues.getOtaSwType();
            }

            @Override
            public void getLatestSoftware(String softwareType) {
                DapiGetSoftware.getLatestSoftware(softwareType, response -> {
                    int artifactResult;
                    DapiResult result = DapiResult.fromInt(response.dapiResponse);
                    switch (result) {
                        case Success:
                            artifactResult = UpdateManagerClient.DAPI_RESPONSE_SUCCESS;
                            break;
                        case NoSoftwareFound:
                            artifactResult = UpdateManagerClient.DAPI_RESPONSE_NO_SOFTWARE_FOUND;
                            break;
                        case GeneralError:
                        default:
                            artifactResult = UpdateManagerClient.DAPI_RESPONSE_FAILURE;
                            break;
                    }
                    mUpdateClient.getLatestSoftwareVersionResponse(artifactResult, response.softwareType,
                            response.version, response.url, response.md5);
                });
            }

            @Override
            public File getOtaUpdateXmlFileDir() {
                return new File(SYSTEM_RSCU_PATH);
            }
        });

        // Create data matrix callbacks
        mDataMatrix.createBoolAdapter(MainAppKeys.DATA_IS_SAFE_TO_UPDATE_BOOL, aBoolean -> installIfPossible());
        mDataMatrix.createBoolAdapter(DAPIKeys.DATA_IS_AUTO_UPDATE_READY_BOOL, aBoolean -> installIfPossible());
        mDataMatrix.createIntAdapter(SettingKeys.Update.DATA_AUTO_UPDATE_HOUR_INT, hour -> {
            mAlarmManager.cancel(mPendingIntent);
            DataMatrix.putBoolean(DAPIKeys.DATA_AUTO_UPDATE_INTENT_SET_BOOL, false);
            setAutoUpdateIntent();
        });

        mDataMatrix.createIntAdapter(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, clubId -> {
            if (mClubId != -1 && mClubId != clubId) {
                cancelUpdateDownload(context);
            }

            mClubId = clubId;
        });

        mDataMatrix.createIntAdapter(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, dapiEnvironment -> {
            if (mDapiEnvironment != -1 && mDapiEnvironment != dapiEnvironment) {
                cancelUpdateDownload(context);
            }

            mDapiEnvironment = dapiEnvironment;
        });

        // check first boot update
        DataMatrix.putBoolean(TopBarKeys.DATA_IS_UPDATE_DOWNLOADING_BOOL, false);

        if (DataMatrix.getBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_BOOL) && hasBeen24HoursSinceCheck() &&
                DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL)) {
            // Make sure we got the correct UUID before checking RSCU.
            mDataMatrix.createBoolAdapter(DAPIKeys.DATA_DAPI_MACHINE_UUID_VALIDATED_BOOL, value -> {
                if(value) {
                    Log.i(TAG, "Has been 24 hours, checking auto update");
                    safeCheckRscu();
                }
            });
        } else {
            Log.d(TAG, "Has not been 24 hours, checking status of updater");
            mUpdateClient.checkUpdateStatus(AUTO_UPDATE_ID);
        }


        mUpdateBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received auto update check intent, starting check");
                DataMatrix.putBoolean(DAPIKeys.DATA_AUTO_UPDATE_INTENT_SET_BOOL, false);
                DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.UPDATE.name());
                safeCheckRscu();
                setAutoUpdateIntent();
            }
        };

        context.registerReceiver(mUpdateBroadcastReceiver, new IntentFilter(ALARM_INTENT));
        mPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ALARM_INTENT), PendingIntent.FLAG_IMMUTABLE);
        mAlarmManager = (AlarmManager) (context.getSystemService(Context.ALARM_SERVICE));

        setAutoUpdateIntent();
    }

    private void cancelUpdateDownload(Context context) {
        if (context != null) {
            UpdateManagerClient.cancelDownload(context);
            DataMatrix.putBoolean(DAPIKeys.DATA_IS_AUTO_UPDATE_READY_BOOL, false);
            DataMatrix.putBoolean(TopBarKeys.DATA_IS_UPDATE_DOWNLOADING_BOOL, false);
        }
    }

    public void checkRscuOnBootIfRequired() {

        if (!DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL)) return; // don't send if the machine has not been setup yet

        if (ProductConfig.isRetail()) {
            // always check on boot on parker if the setting is enabled
            if (DataMatrix.getBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_BOOL)) {
                safeCheckRscu();
            }
        } else {
            if (DataMatrix.getBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_BOOL) && hasBeen24HoursSinceCheck()) {
                safeCheckRscu();
            }
        }
    }

    private boolean safeCheckRscu() {
        try {
            mUpdateClient.checkUpdateOta(AUTO_UPDATE_ID);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "checkRscuForUpdate exception", e);
            return false;
        }
    }

    private boolean hasBeen24HoursSinceCheck() {
        long lastChecked = DataMatrix.getLong(SettingKeys.Update.DATA_LAST_CHECK_TIME_LONG);
        long currentTime = System.currentTimeMillis();
        return currentTime - lastChecked >= TimeUnit.DAYS.toMillis(1);
    }

    private void setAutoUpdateIntent() {

        if (!DataMatrix.getBoolean(DAPIKeys.DATA_AUTO_UPDATE_INTENT_SET_BOOL) &&
                DataMatrix.getBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_BOOL)) {
            Calendar calendar = Calendar.getInstance();
            int hour = DataMatrix.getInt(SettingKeys.Update.DATA_AUTO_UPDATE_HOUR_INT);
            int minute = (int) (Math.random() * 59); // minute should be randomized between 0 and 59
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            long timeToUpdate = calendar.getTime().getTime();
            if (timeToUpdate < System.currentTimeMillis()) {
                // If time is in the past send the next day
                timeToUpdate += TimeUnit.DAYS.toMillis(1);
            }

            // Find elapsed realtime from now where the system should trigger intent
            long triggerTimeElapsed = timeToUpdate - System.currentTimeMillis() + SystemClock.elapsedRealtime();
            Log.d(TAG, "Setting auto update for " + hour + ":" + minute);
            mAlarmManager.set(ELAPSED_REALTIME, triggerTimeElapsed, mPendingIntent);
            DataMatrix.putBoolean(DAPIKeys.DATA_AUTO_UPDATE_INTENT_SET_BOOL, true);
        }
    }

    /**
     * Gets whether the update can be installed.
     *
     * @return Whether the update can be installed
     */
    private boolean canInstallSoftware() {
        boolean updateReady = DataMatrix.getBoolean(DAPIKeys.DATA_IS_AUTO_UPDATE_READY_BOOL);
        boolean safeToUpdate = DataMatrix.getBoolean(MainAppKeys.DATA_IS_SAFE_TO_UPDATE_BOOL);
        boolean allowUpdate = DataMatrix.getBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_BOOL);
        boolean canInstall = updateReady && safeToUpdate && allowUpdate;
        Log.d(TAG, "Can install (all true)? " + canInstall + ": auto update is ready =" + updateReady
                + ", safe to update =" + safeToUpdate
                + ", auto update enabled =" + allowUpdate);
        return canInstall;
    }

    /**
     * Starts an update if all conditions are met
     */
    private void installIfPossible() {
        if (canInstallSoftware()) {
            mUpdateClient.applyUpdate(AUTO_UPDATE_ID);
        }
    }
}
