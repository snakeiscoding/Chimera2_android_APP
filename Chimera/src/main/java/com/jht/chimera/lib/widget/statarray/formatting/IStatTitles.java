package com.jht.chimera.lib.widget.statarray.formatting;

import com.jht.chimera.lib.widget.statarray.StatValueUpdater;

/**
 * Implement to be able to get the stat title in a {@link StatValueUpdater}.
 * Created by chris.foyer on 8/30/2017.
 */
public interface IStatTitles
{
    String getStatTitle();
    String getStatDefaultValue();
}
