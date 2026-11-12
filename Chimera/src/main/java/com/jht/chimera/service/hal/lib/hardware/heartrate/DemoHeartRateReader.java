package com.jht.chimera.service.hal.lib.hardware.heartrate;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.HeartRateSource;

class DemoHeartRateReader implements IHeartRateReader {
    private volatile int heartRate;
    private boolean inDemoMode;
    private final DataMatrix dataMatrix = new DataMatrix();

    DemoHeartRateReader(IHeartRateUpdated heartRateUpdated) {
        dataMatrix.createBoolAdapter(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, demo -> {
            synchronized (this) {
                inDemoMode = demo;
            }
            heartRateUpdated.heartRateUpdated();
        });
        dataMatrix.createIntAdapter(HALKeys.DATA_DEMO_HEART_RATE_INT, value -> {
            synchronized (this) {
                heartRate = value;
            }
            heartRateUpdated.heartRateUpdated();
        });
    }

    @Override
    public void close() {
        dataMatrix.finish();
    }

    @Override
    public synchronized int heartRate() {
        return inDemoMode ? heartRate : 0;
    }
    @Override
    public synchronized boolean blankState() { return false; }
    @Override
    public HeartRateSource getSource() {
        return HeartRateSource.Simulated;
    }

    @Override
    public boolean hasConnection() {
        return inDemoMode && heartRate > 0;
    }

}
