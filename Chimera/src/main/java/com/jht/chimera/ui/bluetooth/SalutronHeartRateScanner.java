package com.jht.chimera.ui.bluetooth;

import android.content.Context;
import android.util.Log;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.service.hal.lib.salutron.SalutronDevice;
import com.jht.chimera.service.hal.lib.salutron.salutronCommands;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;

public class SalutronHeartRateScanner extends HeartRateScanner {
    private static final String TAG = "SalutronHeartRateSca...";
    DataMatrix mDataMatrix = new DataMatrix();
    IBluetoothScanResult scanResult;
    ArrayList<SalutronHeartRateDevice> deviceList = new ArrayList<>();

    public SalutronHeartRateScanner(Context context){
        super(context);
        mDataMatrix.createParcelableAdapter(HALKeys.EVENT_SALUTRON_DEVICE_FOUND_PARCELABLE, SalutronDevice.SalutronDeviceParcelable.class, deviceParcelable -> {
            if(deviceParcelable.name != null && !deviceParcelable.name.isEmpty()) {
                if(scanResult != null && deviceParcelable.type != salutronCommands.DeviceType.BLE) {
                    SalutronHeartRateDevice device = new SalutronHeartRateDevice(deviceParcelable.id, deviceParcelable.name, deviceParcelable.type);
                    if (deviceList.contains(device)) {
                        device.close();
                    } else {
                        scanResult.deviceFound(device, null);
                        deviceList.add(device);
                        Log.d(TAG, "Device Found: " + device.name + "( " + device.connected() + " )");
                    }
                }

            }
        });
    }

    @Override
    public BluetoothScannerType scannerType() {
        return BluetoothScannerType.SalutronHeartRateScanner;
    }

    @Override
    public void scan(IBluetoothScanResult scanResult) {
        super.scan(scanResult);
        DataMatrix.sendEvent(HALKeys.ACTION_START_SALUTRON_SCAN);
        deviceList.clear();
        this.scanResult = scanResult;
    }

    @Override
    public void close() {
        mDataMatrix.finish();
        super.close();
    }
}
