package com.jht.chimera.service.ui;

import static android.content.Context.WINDOW_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.callbacks.ICallback;
import com.jht.chimera.lib.common.UiThreadHandler;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.service.ui.panels.IPanel;
import com.jht.interfaces.ui.UiPanelOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Handles low level adding and removing panels from the WindowManager. Tracks which panels
 * have been added.
 * <p>
 * To add a tracked panel call {@link #addPanel(UUID, IPanel)} and save the integer ID that is returned.
 * The ID can be used for the other calls to manage the visibility of the panel.
 * <p>
 * Created by chris.foyer on 8/14/2017.
 */
public class PanelManager implements UiPanelOperations {
    private static final String TAG = "PanelManager";

    @SuppressLint("UseSparseArrays")
    private final Map<UUID, PanelInternal> mPanelMap = new HashMap<>();
    private final WindowManager mWindowManager;
    private final UiThreadHandler mHandler = new UiThreadHandler();
    private static final boolean logPanelActions = false;

    public PanelManager(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
    }

    /** Marshall the action onto the UI thread if not already */
    public void runOnUiThread(Runnable action) {
        mHandler.runOnUiThread(action);
    }

    /**
     * Adds a panel to the panel manager. All panels will be added expecting to be not
     * yet added to the WindowManager. This method will return a unique ID for the panel that
     * should be saved and managed by the calling class. The returned ID can be used
     * for subsequent calls controlling the added panel.
     *
     * @param id The ID to track the panel
     * @param panel The panel to be added to the manager
     * @return The ID to track the panel
     */
    public UUID addPanel(UUID id, IPanel panel) {
        mPanelMap.put(id, new PanelInternal(panel));
        return id;
    }
    public IPanel getPanel(UUID id) {
        PanelInternal wrapped = mPanelMap.get(id);
        return wrapped == null ? null : wrapped.panel;
    }

    public void removePanel(UUID id) {
        mPanelMap.remove(id);
    }

    /** Hide and dispose all tracked panels then clear list */
    public void disposeAll() {
        mHandler.runOnUiThread(()-> {
            mPanelMap.forEach((id, panelInternal) -> {
                hidePanelSync(id, null);
                panelInternal.panel.dispose();
            });
            mPanelMap.clear();
        });
    }

    public void forEachPanel(BiConsumer<UUID, IPanel> callback) {
        mPanelMap.forEach((uid, panelInternal) -> callback.accept(uid, panelInternal.panel));
    }

    private static class PanelInternal {
        boolean isVisible = false;
        int lastWindowType = -1;
        IPanel panel;

        PanelInternal(IPanel basePanel) {
            panel = basePanel;
        }
    }

    public void showPanel(UUID id) {
        showPanel(id, null);
    }

    private void logPanelAction(UUID id, String action) {
        if(logPanelActions) {
            PanelInternal panel = mPanelMap.get(id);
            if(panel != null) {
                Log.d(PanelManager.class.getName(), panel.panel.getDebugName() + " " + action);
            }
        }
    }
    /**
     * Adds a panel's view to the WindowManager.
     *
     * @param id The id of the panel to add
     */
    public void showPanel(UUID id, @Nullable ICallback<Void> onPanelShown) {
        logPanelAction(id, "Show Panel");
        mHandler.runOnUiThread(() -> {
            PanelInternal panel = mPanelMap.get(id);
            setPanelVisibility(panel, true);
            if (onPanelShown != null) {
                onPanelShown.onEvent(null);
            }
        });
    }

    /**
     * Removes a panel's view from the WindowManager.
     *
     * @param id The id of the panel to remove
     */
    public void hidePanel(UUID id) {
        hidePanel(id, null);
    }
    public void hidePanel(UUID id, @Nullable ICallback<Void> onHidePanel) {
        mHandler.runOnUiThread(() -> hidePanelSync(id, onHidePanel));
    }
    private void hidePanelSync(UUID id, @Nullable ICallback<Void> onHidePanel) {
        logPanelAction(id, "Hide Panel");
        PanelInternal panel = mPanelMap.get(id);
        setPanelVisibility(panel, false);
        if (onHidePanel != null) {
            onHidePanel.onEvent(null);
        }
    }

    /**
     * Sets the visibility of a panel to a the state in toVisibility.
     *
     * @param panelItem The panel to set
     * @param visible   Whether the panel should be visible or not
     */
    private void setPanelVisibility(final PanelInternal panelItem, final boolean visible) {
        if (panelItem != null) {
            if (visible) {
                if (!panelItem.isVisible) {
                    panelItem.isVisible = true;
                    try {
                        panelItem.panel.panelAdded();
                        WindowManager.LayoutParams params = panelItem.panel.getPanelParams();
                        mWindowManager.addView(panelItem.panel.getView(), params);
                        panelItem.lastWindowType = params.type;
                    } catch (Exception e) {
                        Log.w(TAG, e);
                    }
                } else {
                    Log.v(TAG, "Panel is already visible");
                }
            } else {
                if (panelItem.isVisible) {
                    panelItem.isVisible = false;
                    try {
                        mWindowManager.removeView(panelItem.panel.getView());
                        panelItem.panel.panelRemoved();
                    } catch (Exception e) {
                        Log.w(TAG, e);
                    }
                } else {
                    Log.v(TAG, "Panel is already invisible");
                }
            }
        } else {
            Log.e(TAG, "Panel was null, could not change visibility!");
        }
    }

    /**
     * Gets whether a panel is currently added to the window manager
     *
     * @param id The ID of the panel
     * @return Whether it is currently added
     */
    public boolean isAdded(UUID id) {
        PanelInternal panel = mPanelMap.get(id);
        if (panel != null) {
            return panel.isVisible;
        }
        else {
            return false;
        }
    }

    public void updatePanels() {
        mHandler.runOnUiThread(() -> {
            for (PanelInternal p : mPanelMap.values()) {
                if (p.isVisible) updatePanelSync(p);
            }
        });
    }

    /**
     * Updates a panel with the panel params that are currently returned by the IPanel interface.
     *
     * @param id The id of the panel to update
     */
    public void updatePanel(UUID id) {
        mHandler.runOnUiThread(() -> updatePanelSync(mPanelMap.get(id)));
    }

    // if show/hide need a post, update probably does?
    private void updatePanelSync(PanelInternal internalPanel) {
        if (internalPanel != null && internalPanel.isVisible) {
            try {
                WindowManager.LayoutParams params = internalPanel.panel.getPanelParams();
                if (internalPanel.lastWindowType != params.type) { // cannot change window type after it is added, will need to toggle it quickly
                    mWindowManager.removeView(internalPanel.panel.getView());
                    mWindowManager.addView(internalPanel.panel.getView(), params);
                    internalPanel.lastWindowType = params.type;
                } else {
                    mWindowManager.updateViewLayout(internalPanel.panel.getView(), params);
                }
                internalPanel.panel.panelUpdated();
            } catch (Exception e) {
                Log.e(TAG, "updatePanel error", e);
            }
        }
    }

    /** Remove and re-add visible panels (essentially force overlay back to top) */
    public void reAddPanels() {
        mHandler.runOnUiThread(() -> {
            for (PanelInternal p : mPanelMap.values()) {
                if (p.isVisible) reAddPanelSync(p);
            }
        });
    }
    private void reAddPanelSync(PanelInternal internalPanel) {
        if (internalPanel != null && internalPanel.isVisible) {
            try {
                WindowManager.LayoutParams params = internalPanel.panel.getPanelParams();
                mWindowManager.removeView(internalPanel.panel.getView());
                mWindowManager.addView(internalPanel.panel.getView(), params);
                //internalPanel.panel.panelUpdated();
            } catch (Exception e) {
                Log.e(TAG, "reAddPanel error", e);
            }
        }
    }

    public void useTypeErrorForSystemAlert(boolean use) {
        mHandler.runOnUiThread(() -> ChimeraEnums.useErrorForSystemAlert = use); // not real sure why this was in a handler post?
    }
}
