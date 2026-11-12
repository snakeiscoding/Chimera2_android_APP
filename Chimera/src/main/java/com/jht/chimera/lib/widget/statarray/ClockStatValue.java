package com.jht.chimera.lib.widget.statarray;

import android.content.Context;

import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * StatValue specifically for showing the clock time because we do not need to get clock time
 * from the DataMatrix.
 *
 * Created by chris.foyer on 8/23/2017.
 */
public class ClockStatValue extends StatValueUpdater
{
    private boolean mIs24Hour = false;
    private DataMatrix mDataMatrix;
    private Timer mTimer;

    private boolean mIsStarted = false;

    public ClockStatValue(Context context, int defaultTitleId, String defaultValue)
    {
        // Stat type and key are not used for the clock stat value
        super(context, StatValueUpdaterCreator.CLOCK, "", -1, defaultTitleId, defaultValue);
        mDataMatrix = new DataMatrix();
        mIs24Hour = DataMatrix.getBoolean(ImportExportKeys.DATA_CURRENT_CLOCK_FORMAT_IS_24_HOURS_BOOL);
    }

    /**
     * Connects the DataMatrix and begins sending value updates to connected interfaces
     */
    @Override
    public void startCallbacks()
    {
        if (!mIsStarted)
        {
            mIsStarted = true;
            setupDataMatrix();
            stopTimer();
            mTimer = new Timer();
            mTimer.schedule(new ClockTimerTask(), 0, 1000);
        }
    }

    private void setupDataMatrix()
    {
        mDataMatrix.createBoolAdapter(ImportExportKeys.DATA_CURRENT_CLOCK_FORMAT_IS_24_HOURS_BOOL, value -> mIs24Hour = value, true);
    }

    @Override
    public void stopCallbacks()
    {
        stopTimer();
        mDataMatrix.finish();
        mIsStarted = false;
    }

    private void stopTimer()
    {
        if (mTimer != null)
        {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }

    private class ClockTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            if (!mIs24Hour)
            {
                if (hour == 0) hour = 12;
                else if (hour > 12) hour = hour - 12;
            }

            String time = hour + ":" + String.format(Locale.ENGLISH, "%02d", minute);
            for (IStatValueUpdate callback : mCallbacks)
            {
                callback.statValueChanged(time);
            }
        }
    }
}
