package com.jht.chimera.ui.bluetooth;

import android.util.Log;

import com.jht.chimera.R;

import org.jetbrains.annotations.NonNls;


public class TestScanner implements IBluetoothScanner {
    @NonNls private final String TAG = "TestScanner";

    @Override
    public BluetoothScannerType scannerType() {
        return BluetoothScannerType.TestScanner;
    }

    /**
     *
     * @return  The name as a resource id that we will show for this scanner list.
     */
    @Override
    public int name() {
        return R.string.tests_tc;
    }

    /**
     *
     * @return  The icon as a resource id that we will show for this scanner list.
     */
    @Override
    public int icon() {
        return R.drawable.bt_pairing_media_player;
    }

    private boolean isScanning = false;
    /**
     * This operation must be asynchronous.  Use the scan result to pass back device notifications.
     *
     * @param scanResult  Callback to let the UI know the results.
     */
    @Override
    public void scan(final IBluetoothScanResult scanResult) {
        Log.d(TAG, "Test scanner scan");
        if(!isScanning) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        Log.w(TAG, e);
                    }

                    if(!isScanning) { return; }

                    Log.d(TAG, "Create test bluetooth devices.");
                    scanResult.deviceFound(new TestBluetoothDevice("Success", "1", TestBluetoothDevice.TEST_MODES.CONNECT_SUCCESS, TestBluetoothDevice.DEVICE_MODE.DISCONNECTED, false), null);
                    scanResult.deviceFound(new TestBluetoothDevice("Success Use Pin", "2", TestBluetoothDevice.TEST_MODES.CONNECT_SUCCESS, TestBluetoothDevice.DEVICE_MODE.CONNECTED, true), null);
                    scanResult.deviceFound(new TestBluetoothDevice("Pair Fail", "3", TestBluetoothDevice.TEST_MODES.PAIRING_FAIL, TestBluetoothDevice.DEVICE_MODE.DISCONNECTED, false), null);
                    scanResult.deviceFound(new TestBluetoothDevice("Connect Fail", "4", TestBluetoothDevice.TEST_MODES.CONNECTION_FAIL, TestBluetoothDevice.DEVICE_MODE.DISCONNECTED, false), null);
                    scanResult.deviceFound(new TestBluetoothDevice("HANG", "5", TestBluetoothDevice.TEST_MODES.HANG, TestBluetoothDevice.DEVICE_MODE.DISCONNECTED, false), null);
                }
            }).start();
            isScanning = true;
        }
    }

    /**
     * Stop any current scan.  Do nothing if a scan is not happening.
     */
    @Override
    public void stopScan() {
        Log.d(TAG, "Test scanner stop scan");
        isScanning = false;
    }

    @Override
    public void clearDevices() {

    }

    @Override
    public void close() {

    }
}
