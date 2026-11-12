package com.jht.chimera.ui.bluetooth;

import android.bluetooth.le.ScanRecord;

public interface IBluetoothScanResult {
    void deviceFound(IBluetoothDevice device, ScanRecord scanRecord);
}
