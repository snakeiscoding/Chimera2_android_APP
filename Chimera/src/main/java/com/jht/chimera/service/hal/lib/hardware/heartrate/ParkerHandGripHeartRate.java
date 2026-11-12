package com.jht.chimera.service.hal.lib.hardware.heartrate;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.service.hal.lib.hardware.IDriver;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.HeartRateSource;
import com.jhtp.dapiservice.an.types.ButtonPressType;

class ParkerHandGripHeartRate implements IHeartRateReader {
    private static final String TAG = "ParkerHandGripHeartRate";
    private IDriver driver;
    private int fd;
    private volatile int heartRate;
    private IHeartRateUpdated heartRateUpdated;
    private boolean sendAnalytics = false;
    private final DataMatrix mDataMatrix = new DataMatrix();
    private final PeriodicTimer hrTimer = new PeriodicTimer(1000, 1000, () -> {
        int ret = driver.waitForRead(fd, 100);
        if (ret > 0) {
            heartRate = driver.readInt(fd);
            if (heartRate >= 0) {
                if(heartRate > 0 && sendAnalytics) {
                    DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.HEARTRATE_HAND_GRIPS_USED.name);
                    sendAnalytics = false;
                }
                heartRateUpdated.heartRateUpdated();
            }
        }

    });
    ParkerHandGripHeartRate(IDriver driver, IHeartRateUpdated heartRateUpdated) {
        this.heartRateUpdated = heartRateUpdated;
        this.driver = driver;
        fd = driver.safeOpen("/dev/hr_heart"); //NON-NLS
        if (fd < 0) {
            Log.e(TAG, "Failed to open driver");
            return;
        }
        hrTimer.start();
        mDataMatrix.createEventAdapter(WorkoutKeys.EVENT_WORKOUT_START, key -> sendAnalytics = true);
    }

    @Override
    public void close() {
        hrTimer.stop();
        driver.safeClose(fd);
        mDataMatrix.finish();
    }

    @Override
    public int heartRate() {
        return heartRate;
    }
    @Override
    public synchronized boolean blankState() { return false; }
    @Override
    public HeartRateSource getSource() {
        return HeartRateSource.Grips;
    }

    @Override
    public boolean hasConnection() {
        return heartRate > 0;
    }

}
