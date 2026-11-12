package com.jht.chimera.service.ui.panels;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.datamatrixlib.DataMatrix;

/**
 * A floating minimize button to be shown on top of third party full screen apps
 */
public class FloatingMinimizePanel extends LinearLayout implements IPanel {
    boolean hasPendingChange = false;

    public FloatingMinimizePanel(Context context) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.floating_minimize_button, this, true);
        findViewById(R.id.minimize_button).setOnClickListener(view -> {
            if (!hasPendingChange) {
                DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR);
            }
        });
    }

    public void setHasPendingChange(boolean hasPendingChange) {
        this.hasPendingChange = hasPendingChange;
    }

    @Override
    public void panelAdded() {
    }

    @Override
    public void panelUpdated() {}

    @Override
    public void panelRemoved() {
    }

    @Override
    public void dispose() {}

    @Override
    public boolean setPanelMode(WorkoutPanelMode mode) { return false; }

    @Override
    public WindowManager.LayoutParams getPanelParams() {
        int width = (int) getResources().getDimension(R.dimen.floating_minimize_panel_width);
        int height = (int) getResources().getDimension(R.dimen.floating_minimize_panel_height);
        WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(width, height,
                ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
        viewParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
        viewParams.x = 30;
        viewParams.y = 30;
        return viewParams;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public String getDebugName() {
        return getClass().getName();
    }
}
