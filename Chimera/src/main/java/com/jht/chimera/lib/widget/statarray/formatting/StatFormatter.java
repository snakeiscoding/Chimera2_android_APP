package com.jht.chimera.lib.widget.statarray.formatting;

import com.jht.chimera.lib.common.formatting.BasicNumberFormat;
import com.jht.chimera.lib.widget.statarray.StatValueUpdater;

/**
 * Formats stats to a given precision.
 * <p>
 * Created by chris.foyer on 8/23/2017.
 */
public class StatFormatter implements IStatFormattingLogic
{
    private int mPrecision;
    private String mAppendedValue = "";

    public StatFormatter(int precision)
    {
        mPrecision = precision;
    }

    public StatFormatter(int precision, String appendedValue)
    {
        mAppendedValue = appendedValue;
    }

    @Override
    public String format(String value)
    {
        // Not implemented for string values
        return "";
    }

    @Override
    public String format(Integer value)
    {
        // Precision not needed here
        return value + mAppendedValue;
    }

    @Override
    public String format(Double value)
    {
        // No double stat values currently exist
        return "";
    }

    @Override
    public String format(Float value)
    {
        return BasicNumberFormat.format(value, mPrecision) + mAppendedValue;
    }

    @Override
    public void setStatUpdaterCallback(StatValueUpdater.IStatValueUpdaterCallback statUpdaterCallback)
    {
    }
}
