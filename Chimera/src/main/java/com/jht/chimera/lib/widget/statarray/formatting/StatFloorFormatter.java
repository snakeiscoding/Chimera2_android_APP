package com.jht.chimera.lib.widget.statarray.formatting;

import com.jht.chimera.lib.common.formatting.FloorFormat;
import com.jht.chimera.lib.widget.statarray.StatValueUpdater;

/**
 * Formats stats to a given precision.
 * <p>
 * Created by chris.foyer on 8/23/2017.
 */
public class StatFloorFormatter implements IStatFormattingLogic
{
    private int mPrecision;

    public StatFloorFormatter(int precision)
    {
        mPrecision = precision;
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
        return String.valueOf(value);
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
        return FloorFormat.format(value, mPrecision);
    }

    @Override
    public void setStatUpdaterCallback(StatValueUpdater.IStatValueUpdaterCallback statUpdaterCallback)
    {
    }
}
