package com.jht.chimera.lib.common.timer;

import android.os.Handler;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.testing.TestingUtils;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for handling the idle timers. This does nothing if automation is
 * enabled.
 */
public class IdleTimerManagement {
    private final List<JHTTimer> timers;
    private final DataMatrix dataMatrix = new DataMatrix();
    private final Handler handler = new Handler();
    private boolean monitoring = false;
    public Runnable wakeCallback = null;

    public IdleTimerManagement() {
        timers = new ArrayList<>();

        // Create a custom adapter this allows for toggling off the managed timers if needed.
        dataMatrix.createBoolAdapter(TestingUtils.KEY, value -> {
            if (value) stopMonitoring();
            else resetTimers();
        }, false);
    }

    /**
     * Reset of the managed timers.
     */
    public void resetTimers() {
        handler.post(() -> {
            if(monitoring) {
                for (JHTTimer timer : timers) {
                    timer.reset();
                }
                if (wakeCallback != null) wakeCallback.run();
            }
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    public JHTTimer addTimer(JHTTimer timer) {
        // Prevent the adding of any timers, so that none will be started or reset (and reset doesn't do this check constantly)
        if (TestingUtils.isAutomationEnabled()) {
            Log.d(TestingUtils.TAG, "Aborting IdleTimerManagement#addTimer() because automation is enabled.");
            return timer;
        }

        handler.post(() -> {
            timers.add(timer);
        });
        return timer;
    }

    public void monitorTimers() {
        handler.post(() -> {
            if (!monitoring) { // either we should check if we are already monitoring or call stop before starting, otherwise we could add duplicate listeners
                monitoring = true;
                for (JHTTimer timer : timers) {
                    timer.start();
                }

                dataMatrix.createEventAdapter(HALKeys.EVENT_SCREEN_TOUCH, s -> resetTimers());
                dataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, s -> resetTimers());
                // CGC-21937 - consider watch connecting as a wake condition
                dataMatrix.createIntAdapter(HALKeys.DATA_GYM_KIT_STATUS_INT, s -> resetTimers());
                dataMatrix.createIntAdapter(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, s -> resetTimers());
            }
        });
    }

    public void stopMonitoring() {
        handler.post(() -> stopMonitoring(false));
    }

    public void clearTimers() {
        handler.post(() -> stopMonitoring(true));
    }

    private void stopMonitoring(boolean clearTimers) {
        monitoring = false;
        dataMatrix.finish();
        for (JHTTimer timer : timers) {
            timer.stop();
        }
        if (clearTimers) timers.clear();
    }

}
