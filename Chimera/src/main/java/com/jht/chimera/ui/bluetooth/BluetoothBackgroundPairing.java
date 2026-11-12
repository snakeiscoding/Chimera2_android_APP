package com.jht.chimera.ui.bluetooth;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class BluetoothBackgroundPairing {
    private static final String TAG = "BluetoothBackgroundP...";
    private final Context context;
    private List<IBluetoothScanner> scanners;

    private boolean scanning = false;
    private final Queue<IBluetoothDevice> connectingDevices = new ArrayDeque<>();
    private IBluetoothDevice connectingDevice = null;
    private final List<IBluetoothDevice> devicesToClose = new LinkedList<>();

    private final HandlerThread pairingThread = new HandlerThread("BTPairingBackground");
    private final Handler pairingThreadHandler;
    private int numberOfDevicesToSearchFor;

    private final JHTTimer stopTimer = new JHTTimer(120000, this::stopAutoPairing);

    public BluetoothBackgroundPairing(Context context) {
        this.context = context;

        pairingThread.start();
        pairingThreadHandler = new Handler(pairingThread.getLooper());
    }

    public void destroy() {
        pairingThread.quit();
    }


    public void startAutoPairing(int messageTimeoutSeconds, int scanTimeoutSeconds) {

        displayAutoPairingMessage (messageTimeoutSeconds);

        String hrDevice = DataMatrix.getString(DAPIKeys.DATA_USER_BLE_HR_MAC_ID_STRING);
        String audioDevice = DataMatrix.getString(DAPIKeys.DATA_USER_BLE_AUDIO_MAC_ID_STRING);
        if (hrDevice.length() > 4) numberOfDevicesToSearchFor++;
        if (audioDevice.length() > 4) numberOfDevicesToSearchFor++;

        //Log.d("systemSettingsApp", "numberOfDevicesToSearchFor " + numberOfDevicesToSearchFor);

        pairingThreadHandler.post( () -> {
            if (scanning) return;
            scanning = true;
            connectingDevices.clear();
            scanners = BluetoothScanners.getInstance().getBackgroundScanners(context);
            startScanners();
            stopTimer.start(scanTimeoutSeconds * 1000);
        });
    }

    public void displayAutoPairingMessage (int timeSeconds) {
        displayAutoPairingMessage(timeSeconds, context.getString(R.string.bluetooth_auto_pair_notification_pg));
    }

    public void displayAutoPairingMessage (int timeSeconds, String textMsg) {
        MessageCenterMessage msg = new MessageCenterMessage("", textMsg, 30);
        msg.setHideTime(timeSeconds);
        msg.setId(JHTTypes.MessageIds.BLUETOOTH_AUTOPAIR_NOTIFICATION.getId());
        DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msg);
    }

    private void hideAutoPairingMessage () {
        Log.i(TAG, "systemSettingsApp: hiding ble message");
        MessageCenterMessage msg = new MessageCenterMessage("", context.getString(R.string.bluetooth_auto_pair_notification_pg), 30);
        msg.setHidden(true);
        msg.setId(JHTTypes.MessageIds.BLUETOOTH_AUTOPAIR_NOTIFICATION.getId());
        DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msg);
    }

    private void stopScanners(boolean destroy) {
        if(scanners != null) {
            for (IBluetoothScanner scanner : scanners) {
                scanner.stopScan();
                if (destroy) {
                    scanner.close();
                }
            }
        }
    }

    private void startScanners() {
        if(scanners != null) {
            for (IBluetoothScanner scanner : scanners) {
                scanner.scan((device, scanRecord) -> onDeviceFound(scanner.scannerType(), device));
            }
        }
    }

    /**
     * Called when we find a bluetooth device during background scanning
     *
     * @param scannerType The type of the scanner
     * @param device      The bluetooth device that was found
     */
    private void onDeviceFound(BluetoothScannerType scannerType, IBluetoothDevice device) {
        if (device != null) pairingThreadHandler.post(() -> {
            if (!connectingDevices.contains(device) && !device.connected() && isAutoConnect(scannerType, device.macAddress())) {
                Log.i(TAG, "systemSettingsApp: add searching for device " + device.displayName());
                connectingDevices.add(device);

                if(connectingDevice == null) {
                    stopScanners(false);
                    connectNextDevice();
                }
            } else {
                device.close(); // make sure the device is closed in case it registered for callbacks
            }
        });
    }

    private void connectNextDevice() {
        if(connectingDevices.isEmpty()) {
            connectingDevice = null;
            if(scanning) {
                startScanners();
            }
        }
        else {
            connectingDevice = connectingDevices.remove();
            devicesToClose.add(connectingDevice);
            Log.i(TAG, "systemSettingsApp: connecting device " + connectingDevice.displayName());
            pairingThreadHandler.post(() -> {
                if (connectingDevice != null) {
                    // for some reason, disconnect is needed if the HeadphoneDeviceManager prevents the device from unauthorized connection
                    connectingDevice.disconnect(false);
                    connectingDevice.connect(this::handleConnectionResult);
                }
            });
        }
    }

    /**
     * Gets the mac address of the device to connect to based on the scanner type that found the device
     *
     * @param scannerType The type of scanner that found the device
     * @return The mac address to connect to
     */
    private boolean isAutoConnect(BluetoothScannerType scannerType, String macAddress) {
        switch (scannerType) {
            case SalutronHeartRateScanner:
                // This next return will force-favor BLE connections over ANT+ connections.   This is
                // Important for MyZone handling - where BLE is fully MyZone handled and ANT+ MyZone is not
                return macAddress.equals(DataMatrix.getString(DAPIKeys.DATA_USER_BLE_HR_MAC_ID_STRING)) ||
                        macAddress.equals(DataMatrix.getString(DAPIKeys.DATA_USER_ANTPLUS_HR_DEVICE_ID_STRING));
            case HeartRateScanner:
                return macAddress.equals(DataMatrix.getString(DAPIKeys.DATA_USER_BLE_HR_MAC_ID_STRING));
            case HeadphoneScanner:
                return macAddress.equals(DataMatrix.getString(DAPIKeys.DATA_USER_BLE_AUDIO_MAC_ID_STRING));
            default:
                // Not available to connect
                return false;
        }
    }

    private void handleConnectionResult(IBluetoothConnectionResult.ConnectionResult result, IBluetoothDevice device) {
        if (result == IBluetoothConnectionResult.ConnectionResult.Success && --numberOfDevicesToSearchFor < 1) hideAutoPairingMessage();
        pairingThreadHandler.post(this::connectNextDevice);
    }

    public void stopAutoPairing() {
        if (!scanning) return; // skip if scanning has not been started
        pairingThreadHandler.post( () -> {
            scanning = false;
            stopTimer.stop();

            // Make sure to call close on all the devices to properly unregister any callbacks
            for (IBluetoothDevice device : devicesToClose) device.close();
            devicesToClose.clear();

            stopScanners(true);
            scanners = null;
            connectingDevices.clear();
            hideAutoPairingMessage();
        });
    }
}
