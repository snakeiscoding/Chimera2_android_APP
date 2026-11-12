package com.jht.chimera.lib.widget.activity;

import android.app.Activity;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.testing.TestingUtils;
import com.jht.chimera.lib.common.timer.AbsoluteTimer;
import com.jht.chimera.lib.common.timer.AbsoluteTimerTask;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Custom activity that adds support for timing out the activities.
 * <p></p>
 *
 * <h3>Note</h3>
 * This has been modified slightly so the timeouts are disabled if the custom automation key is set
 * to enabled.
 */
public class TimeoutActivity extends RestringActivity {
    public interface TimeoutListener {
        void onTimeout();
    }

    private final DataMatrix mLocalDataMatrix = new DataMatrix();
    static volatile AbsoluteTimer mTimeoutTimer;
    static volatile AbsoluteTimer mTimeoutExtraTimer;
    TimeoutListener mTimeoutListener;
    TimeoutListener mTimeoutExtraListener;
    int mTimeoutTimeSec = 0;
    int mTimeoutExtraTimeSec = 0;
    boolean mPauseTimer = false;

    @Override
    protected void onPause() {
        super.onPause();
        stopScreenTimer();
        mLocalDataMatrix.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Timers are not allowed if automation is enabled. So set the pause flag and leave it paused.
        if (TestingUtils.isAutomationEnabled()) {
            Log.d(TestingUtils.TAG, "Pausing timers inside TimeoutActivity#onResume() because automation is enabled.");
            pauseScreenTimer(); // set the pause flag (we will also prevent clearing it by blocking inside unpauseScreenTimer)
        } else {
            resumeScreenTimer();
        }
        mLocalDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, value -> resumeScreenTimer());
        mLocalDataMatrix.createEventAdapter(HALKeys.EVENT_SCREEN_TOUCH, key -> resumeScreenTimer());
        mLocalDataMatrix.createBoolAdapter(TestingUtils.KEY, value -> {
            if (value) stopScreenTimer();
            else resumeScreenTimer();
        });
    }

    public void setDefaultScreenTimeout(TimeoutListener timeoutListener) {
        mTimeoutListener = timeoutListener;
        mTimeoutTimeSec = DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT);
    }

    public void setExtraTimeout(TimeoutListener timeoutListener, int timeoutTimeSec) {
        mTimeoutExtraListener = timeoutListener;
        mTimeoutExtraTimeSec = timeoutTimeSec;
    }

    public synchronized void resumeScreenTimer() {
        // TestingUtils.isAutomationEnabled() will prevent the clearing of mPauseTimer. This method is called with every touch so keep it quick.
        stopScreenTimer();
        if (mTimeoutTimeSec > 0 && !mPauseTimer) {
            mTimeoutTimer = new AbsoluteTimer();
            mTimeoutTimer.schedule(new AbsoluteTimerTask() {
                @Override
                public void run() {
                    stopScreenTimer();
                    if (mTimeoutListener != null) mTimeoutListener.onTimeout();
                }
            }, mTimeoutTimeSec * 1000);
        }

    }

    public synchronized void resumeExtraTimer() {
        if (TestingUtils.isAutomationEnabled()) {
            Log.d(TestingUtils.TAG, "Aborting TimeoutActivity#resumeExtraTimer(). Not allowed when automation is enabled.");
            return;
        }
        stopExtraTimer();

        if (mTimeoutExtraTimeSec > 0) {
            mTimeoutExtraTimer = new AbsoluteTimer();
            mTimeoutExtraTimer.schedule(new AbsoluteTimerTask() {
                @Override
                public void run() {
                    stopScreenTimer();
                    if (mTimeoutExtraListener != null) mTimeoutExtraListener.onTimeout();
                }
            }, mTimeoutExtraTimeSec * 1000);
        }
    }

    public synchronized void stopScreenTimer() {
        if (mTimeoutTimer != null) {
            mTimeoutTimer.cancel();
            mTimeoutTimer.purge();
            mTimeoutTimer = null;
        }
    }

    public synchronized void pauseScreenTimer() {
        mPauseTimer = true;
        stopScreenTimer();
    }

    public synchronized void unpauseScreenTimer() {
        if (TestingUtils.isAutomationEnabled()) {
            Log.d(TestingUtils.TAG, "Aborting TimeoutActivity#unpauseScreenTimer() because automation is enabled.");
            return;
        }

        mPauseTimer = false;
        resumeScreenTimer();
    }

    public synchronized void stopExtraTimer() {
        if (mTimeoutExtraTimer != null) {
            mTimeoutExtraTimer.cancel();
            mTimeoutExtraTimer.purge();
            mTimeoutExtraTimer = null;
        }
    }

    public static void resumeScreenTimer(Activity activity) {
        if (activity instanceof TimeoutActivity) {
            ((TimeoutActivity) activity).resumeScreenTimer();
        }
    }

    public static void stopScreenTimer(Activity activity) {
        if (activity instanceof TimeoutActivity) {
            ((TimeoutActivity) activity).stopScreenTimer();
        }
    }

    public static void pauseScreenTimer(Activity activity) {
        if (activity instanceof TimeoutActivity) {
            ((TimeoutActivity) activity).pauseScreenTimer();
        }
    }

    public static void unpauseScreenTimer(Activity activity) {
        if (activity instanceof TimeoutActivity) {
            ((TimeoutActivity) activity).unpauseScreenTimer();
        }
    }

    public static void reloadDefaultScreenTimer(Activity activity) {
        if (activity instanceof TimeoutActivity) {
            ((TimeoutActivity) activity).mTimeoutTimeSec = DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT);
            ((TimeoutActivity) activity).resumeScreenTimer();
        }
    }
}
