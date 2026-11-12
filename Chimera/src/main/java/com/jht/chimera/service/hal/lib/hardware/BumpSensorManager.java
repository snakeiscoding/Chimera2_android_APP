package com.jht.chimera.service.hal.lib.hardware;

import static com.jht.libbumpsensor.BumpSensorController.MAX_TRIGGER_THRESHOLD;
import static com.jht.libbumpsensor.BumpSensorController.MIN_TRIGGER_THRESHOLD;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.log.ConsoleEvent;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.libbumpsensor.BumpSensorController;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class BumpSensorManager {

    private final String TAG = "BumpSensorManager";
    private final AtomicBoolean isUpdatingFirmware = new AtomicBoolean(false);
    private final Context applicationContext;
    private final DataMatrix dataMatrix = new DataMatrix();

    @NonNls
    private static final String EXTRA_BS_START_PATH = "path";
    @NonNls
    private static final String ACTION_BS_UPDATE_COMPLETE = "firmware_update_complete";
    @NonNls
    private static final String ACTION_BS_UPDATE = "bump_sensor_update";

    // Create a single-threaded executor service
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(
            r -> new Thread(r, "BumpSensorManager-worker")
    );

    private static class SingletonHolder {
        private static final BumpSensorManager INSTANCE = new BumpSensorManager();
    }

    public static BumpSensorManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private BumpSensorManager() {
        applicationContext = ContextUtil.getApplicationContext();// Initialize application context
    }

    /** sleeps but does not eat the interruption exception */
    private static boolean sleepUnlessInterrupted(long ms) {
        try { Thread.sleep(ms); return true; }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Starts the bump sensor manager and attempting to detect the bump sensor.
     */
    public void start() {
        stop(); // Clean up any existing connections

        // Submit the bump sensor detection task to the executor service
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Bump Sensor Initial Wait For Power");
                if (!sleepUnlessInterrupted(1000)) return; //Initial 1-second wait for power stabilization

                int tries = 0;
                while (!Thread.currentThread().isInterrupted()) {

                    Log.d(TAG, "Bump Sensor Search attempt " + tries);

                    // Check if bump sensor is connected
                    if (BumpSensorController.isConnected()) {
                        updateVersion(); //Updates version information
                        setupDataMatrix();

                        // Register broadcast receiver for firmware updates
                        if (applicationContext != null) {
                            applicationContext.registerReceiver(updateReceiver, new IntentFilter(ACTION_BS_UPDATE));
                            Log.d(TAG, "The update receiver is registered.");
                        }
                        break;
                    } else if (++tries > 30) {
                        break;
                    }

                    // Wait before retrying
                    if (!sleepUnlessInterrupted(1000)) return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Bump Sensor Search thread crashed", e);
            }
        });
    }


    /**
     * Unregistering the broadcast receiver if the broadcast is registered.
     */
    public void stop() {
        dataMatrix.finish();
        try {
            if (applicationContext != null) {
                applicationContext.unregisterReceiver(updateReceiver);
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver wasn't registered, ignore", e);
        }
    }

    /** assume caller has established there is a bump sensor, so take care of thresholds */
    private void setupDataMatrix() {
        dataMatrix.createIntAdapter(SettingKeys.ProjectOne.DATA_BUMP_SENSOR_THRESHOLD_INT, threshold -> {
            if (threshold == -1) {
                Log.d(TAG, "Skipping bump sensor threshold update because given threshold is -1");
                return;
            }

            if (threshold < BumpSensorController.MIN_TRIGGER_THRESHOLD) {
                Log.d(TAG, "The given threshold (" + threshold + ") is less than the minimum (" + MIN_TRIGGER_THRESHOLD + "). Forcing the value to the minimum.");
                DataMatrix.putInt(SettingKeys.ProjectOne.DATA_BUMP_SENSOR_THRESHOLD_INT, MIN_TRIGGER_THRESHOLD);
                return;
            }

            if (threshold > MAX_TRIGGER_THRESHOLD) {
                Log.d(TAG, "The given threshold (" + threshold + ") is greater than the maximum (" + MAX_TRIGGER_THRESHOLD + "). Forcing the value to the maximum.");
                DataMatrix.putInt(SettingKeys.ProjectOne.DATA_BUMP_SENSOR_THRESHOLD_INT, MAX_TRIGGER_THRESHOLD);
                return;
            }

            // Don't do IO on main thread
            executorService.execute(() -> {
                // Ensure we set the trigger threshold if different.
                try {
                    if (BumpSensorController.getTriggerThreshold() != threshold) {
                        Log.d(TAG, "Setting bump sensor threshold to " + threshold + " mG");
                        BumpSensorController.setTriggerThreshold(threshold);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Caught exception caught while trying to set bump sensor threshold", e);
                }
            });
        }, true);
    }

    /**
     * Updates the firmware version of the bump sensor and stores it in Android settings.
     */
    private void updateVersion() {
        if (!BumpSensorController.isConnected()) {
            Log.d(TAG, "No bump sensor is connected. Do not install.");
            return;
        }

        try {
            String versionString = BumpSensorController.getFirmwareVersion();
            if (versionString == null) {
                Log.d(TAG, "Issues arose attempting to communicate with the bump sensor that prevented acquisition of the firmware version. Do not install.");
                return;
            }
            // Store the version in Android Settings Secure
            AndroidSetting.putString(applicationContext.getContentResolver(), "chimera_bumpsensor_version", versionString);
            // Bump sensor might not be ready during console boot, so just log it here
            ConsoleEvent.logEvent("Bump sensor firmware: " + versionString);
        } catch (Exception e) {
            Log.e(TAG, "Caught exception", e);
        }
    }


    /**
     * Helper function to send a broadcast indicating the result of the firmware update.
     *
     * @param context The context used to send the broadcast.
     * @param success Whether the update was successful (true) or failed (false).
     */
    private void sendUpdateCompleteBroadcast(Context context, boolean success) {
        Intent completeIntent = new Intent(ACTION_BS_UPDATE_COMPLETE);
        completeIntent.putExtra("success", success); // Indicate success or failure
        context.sendBroadcast(completeIntent);
    }

    // Broadcast receiver for handling firmware update intents
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Ignore if the intent is null or the action does not match
            if (intent == null || !ACTION_BS_UPDATE.equals(intent.getAction())) {
                return;
            }

            // Prevent multiple simultaneous updates
            if (isUpdatingFirmware.getAndSet(true)) {
                Log.w(TAG, "Firmware update already in progress");
                return;
            }

            //Posts update task to the background thread, do not do IO operations on main thread
            executorService.execute(() -> {

                // Get the firmware file path from the intent
                String firmwarePath = intent.getStringExtra(EXTRA_BS_START_PATH);
                if (firmwarePath == null || firmwarePath.isEmpty()) {
                    Log.e(TAG, "No firmware path provided in update intent");
                    isUpdatingFirmware.set(false);
                    sendUpdateCompleteBroadcast(context, false);
                    return;
                }

                // Check if the firmware file exists and is readable
                File firmwareFile = new File(firmwarePath);
                if (!firmwareFile.exists() || !firmwareFile.canRead()) {
                    Log.e(TAG, "Firmware file does not exist or is not readable: " + firmwarePath);
                    isUpdatingFirmware.set(false);
                    sendUpdateCompleteBroadcast(context, false);
                    return;
                }

                try {
                    // Start the firmware update process
                    BumpSensorController.updateFirmware(firmwareFile, result -> {
                        // Broadcast update complete
                        sendUpdateCompleteBroadcast(context, result);

                        // Update version after a successful firmware update
                        if (result) {
                            executorService.execute(() -> {
                                if (!sleepUnlessInterrupted(1000)) return;
                                updateVersion();
                            });
                            Log.d(TAG, "Updating the firmware version.");
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error during firmware update", e);
                    // Broadcast failure due to exception
                    sendUpdateCompleteBroadcast(context, false);
                } finally {
                    isUpdatingFirmware.set(false);
                }
            });
        }
    };
}
