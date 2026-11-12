package com.jht.chimera.lib.widget.statarray.formatting;

import android.content.Context;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.formatting.SpeedFormat;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.statarray.StatValueUpdater;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Formats distances in kilometers (float) to be passed to the stat bar for display
 * <p>
 * Created by chris.foyer on 8/23/2017.
 */
public class SpeedFormatter implements IStatFormattingLogic, IStatTitles {
    private boolean mDisplayMetric = true;
    private Context mContext;
    private StatValueUpdater.IStatValueUpdaterCallback mCallback;
    private boolean mDisplaySecondary = false;

    public SpeedFormatter(Context context, boolean displaySecondary) {
        mContext = context;
        mDisplaySecondary = displaySecondary;

        DataMatrix factory = new DataMatrix();
        factory.createBoolAdapter(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, value -> {
            if (!mDisplaySecondary) mDisplayMetric = value;
            else mDisplayMetric = !value;

            if (mCallback != null) mCallback.forceStatUpdate();
        }, true);
    }

    @Override
    public String format(String value) {
        throw new RuntimeException("Speed cannot be formatted from string");
    }

    @Override
    public String format(Integer value) {
        throw new RuntimeException("Speed cannot be formatted from string");
    }

    @Override
    public String format(Double value) {
        throw new RuntimeException("Speed cannot be formatted from double");
    }

    @Override
    public String format(Float value) {
        return SpeedFormat.formatSpeed(value, mDisplayMetric);
    }

    @Override
    public void setStatUpdaterCallback(StatValueUpdater.IStatValueUpdaterCallback statUpdaterCallback) {
        mCallback = statUpdaterCallback;
    }

    @Override
    public String getStatTitle() {
        if (mDisplayMetric) {
            return StringLookup.getString(R.string.kph_uc, mContext);
        } else {
            return StringLookup.getString(R.string.mph_uc, mContext);
        }
    }

    @Override
    public String getStatDefaultValue() {
        return "0.0";
    }
}
