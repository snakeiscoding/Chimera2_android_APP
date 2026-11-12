package com.jht.chimera.lib.widget.statarray.formatting;

import com.jht.chimera.lib.widget.statarray.StatValueUpdater;

/**
 * Used for formatting stat values into a string. Useful for converting values to correct units.
 *
 * Created by chris.foyer on 8/23/2017.
 */
public interface IStatFormattingLogic
{
    String format(String value);
    String format(Integer value);
    String format(Double value);
    String format(Float value);
    void setStatUpdaterCallback(StatValueUpdater.IStatValueUpdaterCallback statUpdaterCallback);
}
