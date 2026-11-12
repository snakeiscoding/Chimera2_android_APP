package com.jht.chimera.lib.widget.statbar;

/**
 * Notifies about changes in a stat column.
 *
 * Created by chris.foyer on 8/14/2017.
 */
public interface IStatColumnNotifications
{
    /**
     * Notifies that the stat bar was opened
     */
    void columnOpened();

    /**
     * Notifies that the stat bar was closed
     */
    void columnClosed();

    /**
     * Notifies that the primary item was changed to the one in the parameter
     * @param newPrimaryItem The new primary item (the one that will always be visible)
     */
    void primaryViewSwitched(StatItem newPrimaryItem);
}
