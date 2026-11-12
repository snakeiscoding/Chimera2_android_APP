package com.jht.chimera.service.hal.lib.hardware;

import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.datamatrixlib.DataMatrix;

import java.io.File;

public class USBMountManager {

    public final String TAG = "USBMountManager";
    static volatile PeriodicTimer mTimer = new PeriodicTimer();
    public boolean mIsUSBConnected = false;

    public static USBMountManager getInstance() {
        return USBMountManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final USBMountManager INSTANCE = new USBMountManager();
    }

    private USBMountManager() {
        mTimer.setOnTimer(() -> {
            boolean isUSBConnected = new File(StorageUtils.getUdiskPath()).exists();
            if (isUSBConnected != mIsUSBConnected) {
                mIsUSBConnected = isUSBConnected;
                DataMatrix.putBoolean(HALKeys.DATA_IS_USB_CONNECTED_BOOL, mIsUSBConnected);
            }
        });
    }

    private String mDeviceName = "/dev/input/event1"; //NON-NLS

    public void start() {
        mTimer.start(1000, 1000);
    }

    public void close() {
        mTimer.stop();
    }

}
