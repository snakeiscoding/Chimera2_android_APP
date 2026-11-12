package com.jht.chimera.ui.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.util.Collections;
import java.util.List;

@SuppressLint("MissingPermission")
public class HeartRateScanner implements IBluetoothScanner {

    private BluetoothLeScanner scanner;
    private boolean isScanning = false;
    private final Context context;
    private final BluetoothManager manager;
    private IBluetoothScanResult scanResult;
    private final ParcelUuid hrService = ParcelUuid.fromString("0000180d-0000-1000-8000-00805f9b34fb");

    public HeartRateScanner(Context context) {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        manager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.context = context;

    }

    @Override
    public BluetoothScannerType scannerType() {
        return BluetoothScannerType.HeartRateScanner;
    }

    /**
     *
     * @return  The name as a resource id that we will show for this scanner list.
     */
    @Override
    public int name() {
        return R.string.heart_rate_monitors_tc;
    }

    /**
     *
     * @return  The icon as a resource id that we will show for this scanner list.
     */
    @Override
    public int icon() {
        return R.drawable.bt_pairing_heart_rate;
    }

    /**
     * This operation must be asynchronous.  Use the scan result to pass back device notifications.
     *
     * @param scanResult  Callback to let the UI know the results.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void scan(final IBluetoothScanResult scanResult) {
        if(!isScanning && scanner != null) {
            isScanning = true;

            // add previously connected ble heart rate device to list
            String connectedDeviceAddress = DataMatrix.getString(HALKeys.DATA_BLE_HEART_RATE_ADDRESS_STRING);
            if(manager != null && !connectedDeviceAddress.isEmpty()) {
                for(BluetoothDevice d : manager.getConnectedDevices(BluetoothProfile.GATT)) {
                    if (connectedDeviceAddress.equalsIgnoreCase(d.getAddress())) {
                        HeartRateDevice heartRateDevice = new HeartRateDevice(context, d, true);
                        heartRateDevice.registerCallbacks(); // already connected device so register for connection events
                        scanResult.deviceFound(heartRateDevice, null);
                    }
                }
            }

            this.scanResult = scanResult;

            // Note: tweaking the scan setting appears to help find devices (set the filters to null to find all devices)
            ScanFilter hrFilter = new ScanFilter.Builder()
                    .setServiceUuid(hrService)
                    .build();
            List<ScanFilter> filters = Collections.singletonList(hrFilter);
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .build();
            scanner.startScan(filters, settings, scanCallback);

        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        private void parseResult(ScanResult result) {
            if(result.getDevice().getName() == null){
                return;
            }
            List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();
            if(uuids != null) {
                for (ParcelUuid uuid : uuids) {
                    if (uuid.getUuid().equals(hrService.getUuid())) {
                        scanResult.deviceFound(new HeartRateDevice(context, result.getDevice(), isConnectedHeartRate(result.getDevice())), null);
                        return;

                    }
                }
            }
        }
        @Override
        public void onBatchScanResults(List<ScanResult> result) {
            super.onBatchScanResults(result);
            for(ScanResult r : result) {
                parseResult(r);
            }
        }
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            parseResult(result);
        }
    };

    /**
     * Stop any current scan.  Do nothing if a scan is not happening.
     */
    @Override
    public void stopScan() {
        if(isScanning) {
            isScanning = false;
            scanner.stopScan(scanCallback);
        }
    }

    private boolean isConnectedHeartRate(BluetoothDevice device) {
        // if the device is connected to GATT still but we are ignoring it, do not count as connected
        String connectedDeviceAddress = DataMatrix.getString(HALKeys.DATA_BLE_HEART_RATE_ADDRESS_STRING);
        if (!device.getAddress().equalsIgnoreCase(connectedDeviceAddress)) return false;
        if(manager != null) {
            for(BluetoothDevice d : manager.getConnectedDevices(BluetoothProfile.GATT)) {
                if(d.getAddress().equals(device.getAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void clearDevices() {
        // Would this disconnect all BLE devices, not just HR devices?
        // Should we track the paired HR devices or perhaps leverage a DM key like DATA_BLE_HEART_RATE_ADDRESS_STRING ?
        try {
            for (BluetoothDevice d : manager.getConnectedDevices(BluetoothProfile.GATT)) {
                try (HeartRateDevice device = new HeartRateDevice(context, d, true)) {
                    device.disconnect(false);
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void close() {
        try {
            stopScan();
        } catch (Exception ignored) {}
    }
}
