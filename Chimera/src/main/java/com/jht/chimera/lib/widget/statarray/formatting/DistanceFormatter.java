package com.jht.chimera.lib.widget.statarray.formatting;

import android.content.Context;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.formatting.DistanceFormat;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.statarray.StatValueUpdater;

/**
 * Formats distances in kilometers (float) to be passed to the stat bar for display
 * <p>
 * Created by chris.foyer on 8/23/2017.
 */
public class DistanceFormatter implements IStatFormattingLogic, IStatTitles {
    private boolean mShowMetric;
    private Context mContext;

    public DistanceFormatter(Context context, boolean showMetric) {
        mContext = context;
        mShowMetric = showMetric;
    }

    @Override
    public String format(String value) {
        throw new RuntimeException("Goal remaining cannot be formatted from string");
    }

    @Override
    public String format(Integer value) {
        throw new RuntimeException("Goal remaining cannot be formatted from string");
    }

    @Override
    public String format(Double value) {
        throw new RuntimeException("Goal remaining cannot be formatted from double");
    }

    @Override
    public String format(Float value) {
        return DistanceFormat.formatWorkoutDistance(value, mShowMetric);
    }

    @Override
    public void setStatUpdaterCallback(StatValueUpdater.IStatValueUpdaterCallback statUpdaterCallback) {
    }

    @Override
    public String getStatTitle() {
        return mShowMetric ? StringLookup.getString(R.string.kilometers_uc, mContext) : StringLookup.getString(R.string.miles_uc, mContext);
    }

    @Override
    public String getStatDefaultValue() {
        // Generate the correct default for the intended precision (just gets 0, so does not matter units)
        return DistanceFormat.formatWorkoutDistance(0, true);
    }
}
