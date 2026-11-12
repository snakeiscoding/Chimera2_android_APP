package com.jht.chimera.service.ui.panels;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.jht.interfaces.ui.BasePanelData;
import com.jht.interfaces.ui.InnerPanelHolder;

import javax.annotation.Nullable;

public interface IWorkoutPanelManager {
    /**
     * Initializes and adds the workout panels to the panel manager
     *
     * @param context the context used to crated the panels
     */
    void initializeWorkoutPanelManager(Context context);

    /**
     * Gets whether a panel is currently added to the window manager and can be visible.
     *
     * @param workoutPanelKey The enum equivalent of the panel to check if shown
     * @return Whether it is added to the window manager or not
     */
    boolean isPanelShown(PanelIds workoutPanelKey);

    /**
     * Shows or hides workout panels based on state. Note the manager will not update any associated DataMatrix keys.
     *
     * @param state What state of panels should be shown (i.e. idle/regular/fullscreen) See UiServiceKeys.WorkoutSet values.
     */
    void setWorkoutPanelState(int state);

    /**
     * Updates the cached panel state for the last app
     */
    void updateCachedAppWorkoutPanelBehavior(PanelBehavior panelBehavior);

    /**
     * Sets the current panel behavior to the cached panel behavior
     */
    void useCachedAppWorkoutPanelBehavior();

    /** call when panels need resetting for next workout */
    void resetForWorkout();

    /**
     * Describes what panels should be set based on current workout panel set state.
     *
     * @param behavior Instance of class describing panel behaviors to be used at various states.
     */
    void setWorkoutPanelBehavior(@Nullable PanelBehavior behavior);

    /** Apply given panel override options if applicable */
    void setPanelOverrides(@NonNull Bundle panelOverrides);

    /** Remove given panel override options if applicable */
    void clearPanelOverrides(@NonNull Bundle panelOverrides);

    /**
     * Shows/Hides the panel based on the passed in panel key
     *
     * @param workoutPanelKey the panel key that needs to be shown/hidden
     * @param isVisible shown if true and hidden if false
     */
    void showPanel(PanelIds workoutPanelKey, boolean isVisible);

    /**
     * Set overlay delay and shows overlay panel
     *
     * @param delayMs delay in milliseconds
     */
    void showOverlay(int delayMs);

    /**
     * Updates the panel with the specified ID
     *
     * @param workoutPanelKey The enum equivalent of the panel to update
     */
    void updatePanel(PanelIds workoutPanelKey);

    /**
     * Show prompt for digital safety PIN
     *
     * @param show if true show, otherwise hide
     */
    void showDigitalSafetyPrompt(boolean show);

    /**
     * Adds a panel from a module
     * @param context The context from the service
     * @param panelHolder The panel to add
     */
    void addInnerPanel(Context context, InnerPanelHolder panelHolder);

    /**
     * Sends data into a module panel, if the panel is added.
     * @param panelData The data to send
     */
    void sendInnerPanelData(BasePanelData panelData);

    /**
     * Removes all inner panels added from modules
     */
    void removeInnerPanels();

    // auto hide panel functions
    void resetAutoHidePanels();
    void setAutoHideBehavior(WorkoutPanelManager.AutoHideBehavior mode);
    void onTouch();
    // compose panel theming
    void updateTheme(String themeUri);

    void showDebugPanel();
}