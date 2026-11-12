package com.jht.chimera.ui.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * Generic ble scanner that facilitates scanning for bluetooth devices
 */
public class BleScanner implements IBluetoothScanner {

    private static final String TAG = BleScanner.class.getName();

    private BluetoothLeScanner scanner;
    private boolean isScanning = false;
    private final Context context;
    private ScanCallback scanCallback;

    public BleScanner(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            Log.d(TAG, "scanner null? " + (scanner == null));
        }

        this.context = context;
    }

    @Override
    public BluetoothScannerType scannerType() {
        return BluetoothScannerType.BleScanner;
    }

    /**
     * @return The name as a resource id that we will show for this scanner list.
     */
    @Override
    public int name() {
        return 0;
    }

    /**
     * @return The icon as a resource id that we will show for this scanner list.
     */
    @Override
    public int icon() {
        return 0;
    }

    /**
     * This operation must be asynchronous.  Use the scan result to pass back device notifications.
     *
     * @param scanResult Callback to let the UI know the results.
     */
    @Override
    public void scan(final IBluetoothScanResult scanResult) {
        Log.d(TAG, "trying to scan, isScanning: " + isScanning + " scanner null " + (scanner == null));
        if (!isScanning && scanner != null) {

            isScanning = true;
            scanCallback = new ScanCallback() {

                private void parseResult(ScanResult result) {
                    scanResult.deviceFound(new BleDevice(context, result.getDevice()), result.getScanRecord());
                }

                @Override
                public void onBatchScanResults(List<ScanResult> result) {
                    super.onBatchScanResults(result);
                    for (ScanResult r : result) {
                        parseResult(r);
                    }
                }

                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    parseResult(result);
                }
            };

            scanner.startScan(scanCallback);
        }
    }

    /**
     * Stop any current scan.  Do nothing if a scan is not happening.
     */
    @Override
    public void stopScan() {
        Log.d(TAG, "stopping scan in ble scanner isScanning: " + isScanning);
        if (isScanning) {
            isScanning = false;
            scanner.stopScan(scanCallback);
        }
    }

    @Override
    public void clearDevices() {
    }

    @Override
    public void close() {

    }
}
