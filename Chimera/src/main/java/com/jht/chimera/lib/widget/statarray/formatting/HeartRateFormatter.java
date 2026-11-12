package com.jht.chimera.lib.widget.statarray.formatting;

import android.content.Context;

import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.statarray.StatValueUpdater;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Formats the heart rate to be passed to the stat bar for display
 * <p>
 * Created by chris.foyer on 8/23/2017.
 */
public class HeartRateFormatter implements IStatFormattingLogic, IStatTitles {
    private int mTitleId;
    private Context mContext;
    private StatValueUpdater.IStatValueUpdaterCallback mCallback = null;

    public HeartRateFormatter(Context context, int titleId) {
        mTitleId = titleId;
        mContext = context;

        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, runState -> {
            if (runState == WorkoutKeys.Constants.RUN_STATE_RUNNING && mCallback != null) {
                mCallback.forceStatUpdate();
            }
        }, false);
    }

    @Override
    public String format(String value) {
        throw new RuntimeException("Incline cannot be formatted from string");
    }

    @Override
    public String format(Integer value) {
        return StringUtil.formatHeartRate(value);
    }

    @Override
    public String format(Double value) {
        throw new RuntimeException("Incline cannot be formatted from double");
    }

    @Override
    public String format(Float value) {
        throw new RuntimeException("Incline cannot be formatted from float");
    }

    @Override
    public void setStatUpdaterCallback(StatValueUpdater.IStatValueUpdaterCallback statUpdaterCallback) {
        mCallback = statUpdaterCallback;
    }

    @Override
    public String getStatTitle() {
        return StringLookup.getString(mTitleId, mContext);
    }

    @Override
    public String getStatDefaultValue() {
        return "--";
    }
}
