package com.jht.chimera.lib.widget.statarray.formatting;

import android.content.Context;

import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.statarray.StatValueUpdater;

/**
 * Formats float to int (flooring) to the stat bar for display
 */
public class FloatToIntFormatter implements IStatFormattingLogic, IStatTitles {

    private int mTitleId;
    private Context mContext;

    public FloatToIntFormatter(Context context, int titleId) {
        mTitleId = titleId;
        mContext = context;
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
        return String.valueOf((int) value.floatValue());
    }

    @Override
    public void setStatUpdaterCallback(StatValueUpdater.IStatValueUpdaterCallback statUpdaterCallback) {
        // Forcing updates not needed
    }

    @Override
    public String getStatTitle() {
        return StringLookup.getString(mTitleId, mContext);
    }

    @Override
    public String getStatDefaultValue() {
        return "0";
    }
}
