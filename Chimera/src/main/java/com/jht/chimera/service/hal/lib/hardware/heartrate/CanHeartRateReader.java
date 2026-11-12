package com.jht.chimera.service.hal.lib.hardware.heartrate;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.HeartRateSource;

class CanHeartRateReader implements IHeartRateReader {
    private volatile int heartRate;

    private final DataMatrix dataMatrix = new DataMatrix();

    CanHeartRateReader(IHeartRateUpdated heartRateUpdated) {
        dataMatrix.createIntAdapter(HALKeys.DATA_CAN_HEART_RATE_INT, value -> {
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
        return heartRate;
    }

    @Override
    public HeartRateSource getSource() {
        return HeartRateSource.Grips;
    }

    @Override
    public boolean hasConnection() {
        return heartRate > 0;
    }

    @Override
    public synchronized boolean blankState(){ return false; }
}