package com.jht.chimera.lib.widget.statarray.formatting;

import com.jht.chimera.lib.common.formatting.TimeFormat;
import com.jht.chimera.lib.widget.statarray.StatValueUpdater;

/**
 * Formats integer times that are passed into this class into correct time formats for the stat bar
 *
 * Created by chris.foyer on 8/23/2017.
 */
public class TimeFormatter implements IStatFormattingLogic
{
    @Override
    public String format(String value)
    {
        throw new RuntimeException("Date cannot be formatted from string");
    }

    @Override
    public String format(Integer value)
    {
        return TimeFormat.formatTime(value);
    }

    @Override
    public String format(Double value)
    {
        throw new RuntimeException("Date cannot be formatted from double");
    }

    @Override
    public String format(Float value)
    {
        throw new RuntimeException("Date cannot be formatted from float");
    }

    @Override
    public void setStatUpdaterCallback(StatValueUpdater.IStatValueUpdaterCallback statUpdaterCallback)
    {
        //Does not need to be implemented
    }
}
