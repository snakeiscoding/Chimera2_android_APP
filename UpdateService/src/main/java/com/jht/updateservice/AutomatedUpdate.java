package com.jht.updateservice;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.updatemanager.IUpdateCallback;
import com.jht.updatemanager.UpdateManagerClient;
import com.jht.updatemanager.UpdateManagerTypes;

import java.io.File;

import dapi.IOtaUpdateInterface;

class AutomatedUpdate extends BroadcastReceiver implements IUpdateCallback, IOtaUpdateInterface {
    private static final String TAG = "AutomatedUpdate";
    final String UPDATE_USB_ID = "automated_update"; //NON-NLS
    final String UPDATE_RSCU_ID = "automated_rscu_update"; //NON-NLS
    private File automatedUpdateLocation;
    private final UpdateManagerClient updateManagerClient;
    private String rscuVersion;
    private String rscuLocation;
    private String rscuMd5;
    private boolean automatedUpdate = false;

    // From UpdateValues, copied into updater project
    private static final String VISION_EQUIPMENT_TYPE = "vision600e";
    private static final String VISION_SW_TYPE = "v600e";
    private static final String LCGUI_EQUIPMENT_TYPE = "lowcostgui";
    private static final String LCGUI_SW_TYPE = "lcgui";

    AutomatedUpdate(Context context) {
        context.registerReceiver(this, new IntentFilter("com.jht.automatedupdate"));
        updateManagerClient = new UpdateManagerClient(context, this);
        updateManagerClient.initOtaUpdates(this);
    }
    public void close() {
        if (updateManagerClient != null) updateManagerClient.stopUpdateManagerClient();
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getBooleanExtra("rscu", false)) {
            rscuVersion = intent.getStringExtra("rscu_version");
            rscuLocation = intent.getStringExtra("rscu_location");
            rscuMd5 = intent.getStringExtra("rscu_md5");
            Log.d(TAG, "ADB RSCU Request " + rscuLocation + " " + rscuVersion + " " + rscuMd5);
            if(
                rscuVersion != null && !rscuVersion.isEmpty() &&
                rscuLocation != null && !rscuLocation.isEmpty() &&
                rscuMd5 != null && !rscuMd5.isEmpty()
            ) {
                runRSCUUpdate(intent.getBooleanExtra("force_update", false));
            }
        }
        else {
            runManualUpdate(intent.getStringExtra("manifest"), intent.getBooleanExtra("force_update", false));
        }
    }

    private void runRSCUUpdate(boolean force) {
        new Thread(() -> {
            automatedUpdate = true;
            Log.d(TAG, "Check rscu for update " + rscuLocation + " " + rscuVersion + " " + rscuMd5);
            updateManagerClient.checkUpdateOta(UPDATE_RSCU_ID, force);
        }).start();

    }

    private void runManualUpdate(String manifestFile, boolean force) {

        if(manifestFile != null && !manifestFile.isEmpty()) {
            new Thread(() -> {
                automatedUpdate = true;
                automatedUpdateLocation = new File(manifestFile).getParentFile();
                updateManagerClient.checkUpdateOta(UPDATE_USB_ID, force);
            }).start();
        }
    }

    @Override
    public void applyUpdatesCallback(int returnCode, String id, Intent intent) {

        Log.d(TAG, "Apply updates callback " + returnCode);
        if(automatedUpdate && returnCode == 0) {
            if (id.equals(UPDATE_USB_ID)) {
                FileUtils.delete(automatedUpdateLocation);
            }
            automatedUpdate = false;
        }

    }

    @Override
    public void checkUpdateCallback(int returnCode, String id, Intent intent) {
        Log.d(TAG, "Check updates callback " + returnCode);
        if(automatedUpdate) {
            if (returnCode == UpdateManagerTypes.CHECK_UPDATE_NEW_UPDATE) {
                updateManagerClient.applyUpdate(id);
            } else {
                if (id.equals(UPDATE_USB_ID)) {
                    FileUtils.delete(automatedUpdateLocation);
                }
                automatedUpdate = false;
            }
        }
    }

    @Override
    public void downloadedUpdateFilesCallback(int returnCode, String id, Intent intent) {
    }

    @Override
    public void checkStatusCallback(int returnCode, String id) {
    }

    @Override
    public String getSystemSoftwareType() {
        String systemSwType = BuildConfig.UPDATE_EQUIPMENT;
        if (systemSwType.equalsIgnoreCase(VISION_EQUIPMENT_TYPE))  {
            return VISION_SW_TYPE; //NON-NLS
        } else if (systemSwType.equalsIgnoreCase(LCGUI_EQUIPMENT_TYPE)) {
            return LCGUI_SW_TYPE;
        }

        return systemSwType;
    }

    @Override
    public void getLatestSoftware(String softwareType) {
        Log.d(TAG, "get latest software in automated " + rscuLocation);
        updateManagerClient.getLatestSoftwareVersionResponse(
                UpdateManagerClient.DAPI_RESPONSE_SUCCESS,
                softwareType,
                rscuVersion,
                rscuLocation,
                rscuMd5);
    }

    @SuppressLint("SdCardPath")
    @Override
    public File getOtaUpdateXmlFileDir() {
        return new File("/sdcard/Download");
    }

    boolean inAutomatedUpdate() {
        return automatedUpdate;
    }

    File getAutomatedUpdateLocation() {
        return automatedUpdateLocation;
    }
}
