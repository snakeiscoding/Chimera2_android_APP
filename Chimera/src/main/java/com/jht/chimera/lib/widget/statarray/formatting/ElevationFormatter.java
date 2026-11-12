package com.jht.chimera.lib.widget.statarray.formatting;

import android.content.Context;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.formatting.DistanceFormat;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.statarray.StatValueUpdater;
import com.jht.datamatrixlib.DataMatrix;

public class ElevationFormatter implements IStatFormattingLogic, IStatTitles {
    private boolean mDisplayMetric = true;
    private StatValueUpdater.IStatValueUpdaterCallback mCallback = null;
    private Context mContext;
    private boolean mDisplaySecondary = false;

    public ElevationFormatter(Context context, boolean displaySecondary) {
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
        throw new RuntimeException("Goal remaining cannot be formatted from string");
    }

    @Override
    public String format(Integer value) {
        throw new RuntimeException("Goal remaining cannot be formatted from int");
    }

    @Override
    public String format(Double value) {
        throw new RuntimeException("Goal remaining cannot be formatted from double");
    }

    @Override
    public String format(Float value) {
        if (mDisplayMetric) {
            return DistanceFormat.formatElevation(value, true);
        } else {
            return DistanceFormat.formatElevation(value, false);
        }
    }

    @Override
    public void setStatUpdaterCallback(StatValueUpdater.IStatValueUpdaterCallback statUpdaterCallback) {
        mCallback = statUpdaterCallback;
    }

    @Override
    public String getStatTitle() {
        if (mDisplayMetric) {
            return StringLookup.getString(R.string.elevation_uc, mContext);
        } else {
            return StringLookup.getString(R.string.elevation_uc, mContext);
        }
    }

    @Override
    public String getStatDefaultValue() {
        return "0";
    }
}
