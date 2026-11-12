package com.jht.chimera.service.hal.lib.hardware.heartrate;

import com.jht.enums.HeartRateSource;

import java.io.Closeable;

interface IHeartRateReader extends Closeable {
    interface IHeartRateUpdated {
        void heartRateUpdated();
    }
    boolean blankState();
    int heartRate();
    HeartRateSource getSource();
    boolean hasConnection();
}
