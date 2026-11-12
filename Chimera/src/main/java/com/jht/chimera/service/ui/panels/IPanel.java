package com.jht.chimera.service.ui.panels;

import android.view.View;
import android.view.WindowManager;

/**
 * A base panel class that contains the methods needed to display the panel in the panel manager.
 *
 * Created by chris.foyer on 8/2/2017.
 */
public interface IPanel
{
    // Methods to help manage the panels without needing specific call from the managing logic
    void panelAdded();
    void panelUpdated();
    void panelRemoved();

    /** Perform any final clean up for a panel (panel may be added/removed multiple times, but disposal usually happens when we are releasing the panel */
    void dispose();

    String getDebugName();

    // Methods for showing the panel
    WindowManager.LayoutParams getPanelParams();
    View getView();

    // some panels may toggle modes (return true if panel changed)
    boolean setPanelMode(WorkoutPanelMode mode);
}
