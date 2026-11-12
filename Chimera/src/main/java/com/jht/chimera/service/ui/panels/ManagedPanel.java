package com.jht.chimera.service.ui.panels;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.jht.chimera.service.ui.PanelManager;

import java.util.UUID;

/**
 * A generic managed panel that is used by the message center.  Can be used by others.
 * This just makes it easy to show and hide from the panel manager passed.
 */
public abstract class ManagedPanel extends RelativeLayout implements IPanel {
    private final PanelManager panelManager;
    private final UUID panelId;
    private boolean isShown = false;

    public ManagedPanel(Context context, UUID id) {
        this(context, id, null);
    }
    public ManagedPanel(Context context, UUID id, @Nullable PanelManager panelManager) {
        super(context);
        this.panelManager = panelManager;
        panelId = panelManager == null ? null : panelManager.addPanel(id, this);
    }

    @Override
    public void panelAdded() {}

    @Override
    public void panelUpdated() {}

    @Override
    public void panelRemoved() {}

    @Override
    public void dispose() {}

    @Override
    public View getView() {
        return this;
    }

    @Override
    public boolean setPanelMode(WorkoutPanelMode mode) { return false; }

    public void show() {
        if(panelManager != null && !isShown) {
            panelManager.showPanel(panelId);
        }
        isShown = true;
    }

    public void hide() {
        if(panelManager != null && isShown) {
            panelManager.hidePanel(panelId);
        }
        isShown = false;
    }
}
