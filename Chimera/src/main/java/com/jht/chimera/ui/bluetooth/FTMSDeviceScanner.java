package com.jht.chimera.ui.bluetooth;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

public class FTMSDeviceScanner implements IBluetoothScanner {
    @Override
    public BluetoothScannerType scannerType() {
        return BluetoothScannerType.FtmsScanner;
    }

    @Override
    public int name() {
        return R.string.ftms_tc;
    }

    @Override
    public int icon() {
        return R.drawable.ic_ftms;
    }

    @Override
    public void scan(IBluetoothScanResult scanResult) {
        DataMatrix.putBoolean(SettingKeys.Hardware.DATA_FTMS_SHOULD_ADVERTISE_BOOL, true);
    }

    @Override
    public void stopScan() {
        DataMatrix.putBoolean(SettingKeys.Hardware.DATA_FTMS_SHOULD_ADVERTISE_BOOL, false);
    }

    @Override
    public void clearDevices() {

    }

    @Override
    public void close() {

    }
}
