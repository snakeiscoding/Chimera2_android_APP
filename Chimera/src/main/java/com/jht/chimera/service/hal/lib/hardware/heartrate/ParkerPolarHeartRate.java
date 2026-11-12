package com.jht.chimera.service.hal.lib.hardware.heartrate;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.service.hal.lib.hardware.IDriver;
import com.jht.enums.HeartRateSource;

class ParkerPolarHeartRate implements IHeartRateReader {
    private static final String TAG = "ParkerPolarHeartRate";
    private IDriver driver;
    private int fd;
    private volatile int heartRate;
    private IHeartRateUpdated heartRateUpdated;
    private final PeriodicTimer hrTimer = new PeriodicTimer(1000, 1000, () -> {
        int ret = driver.waitForRead(fd, 100);
        byte[] data = new byte[2];
        if (ret > 0) {
            ret = driver.read(fd, data);
            if (ret == 2) {
                heartRate = data[0]  & 0xff;
                if (heartRate >= 0) {
                    heartRateUpdated.heartRateUpdated();
                }
            }
        }

    });

    ParkerPolarHeartRate(IDriver driver, IHeartRateUpdated heartRateUpdated) {
        this.heartRateUpdated = heartRateUpdated;
        this.driver = driver;
        fd = driver.safeOpen("/dev/wl_heart"); //NON-NLS
        if (fd < 0) {
            Log.e(TAG, "Failed to open driver");
            return;
        }
        hrTimer.start();
    }

    @Override
    public void close() {
        hrTimer.stop();
        driver.safeClose(fd);
    }

    @Override
    public int heartRate() {
        return heartRate;
    }
    @Override
    public synchronized boolean blankState() { return false; }
    @Override
    public HeartRateSource getSource() {
        return HeartRateSource.FIVE_KHZ;
    }

    @Override
    public boolean hasConnection() {
        return heartRate > 0;
    }

}
