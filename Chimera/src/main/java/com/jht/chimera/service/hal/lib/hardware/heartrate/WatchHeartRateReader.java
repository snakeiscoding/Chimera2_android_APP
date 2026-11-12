package com.jht.chimera.service.hal.lib.hardware.heartrate;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.DeviceState;
import com.jht.enums.HeartRateSource;

class WatchHeartRateReader implements IHeartRateReader {
    private volatile int heartRate;
    private final DataMatrix dataMatrix = new DataMatrix();
    private final IHeartRateUpdated heartRateUpdated;

    WatchHeartRateReader(IHeartRateUpdated heartRateUpdated) {
        this.heartRateUpdated = heartRateUpdated;
        dataMatrix.createIntAdapter(HALKeys.ACTION_UPDATE_GYM_KIT_HEART_RATE_INT, this::updateHeartRate, false);
        dataMatrix.createIntAdapter(HALKeys.ACTION_UPDATE_SAMSUNG_HEART_RATE_INT, this::updateHeartRate, false);
    }

    private void updateHeartRate(int heartRate) {
        this.heartRate = heartRate;
        heartRateUpdated.heartRateUpdated();
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
    public synchronized boolean blankState() { return false; }
    @Override
    public HeartRateSource getSource() {
        return HeartRateSource.Watch;
    }

    @Override
    public boolean hasConnection() {
        return DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT) != DeviceState.Disconnected.ordinal()
                || DataMatrix.getInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT) != DeviceState.Disconnected.ordinal();
    }

}
