package com.jht.chimera.service.ui.panels.sidepanel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.RealScreen;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.service.ui.panels.IPanel;
import com.jht.chimera.service.ui.panels.WorkoutPanelMode;

/**
 * The left bar in a workout. Adjusts based on the current workout and frame type.
 * <p>
 * Created by chris.foyer on 8/21/2017.
 */
public class BorderLeftPanel extends RelativeLayout implements IPanel {
    private static final String TAG = "BorderLeftPanel";

    public BorderLeftPanel(Context context) {
        super(context);
        initialize(context);
    }

    public BorderLeftPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public BorderLeftPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialize(final Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void panelAdded() {
        // Not needed at this point
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

    @SuppressLint("RtlHardcoded")
    @Override
    public WindowManager.LayoutParams getPanelParams() {
        //int width = (int) getResources().getDimension(R.dimen.left_bar_ifit_width);
        int width = (RealScreen.widthPixels - (int) getResources().getDimension(R.dimen.app_container_width)) >> 1; // split remaining width between left/right panels
        int height = RealScreen.heightPixels;

        WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(width, height,
                ChimeraEnums.getTypeSystemAlert(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);

        viewParams.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
        viewParams.x = 0;
        viewParams.y = 0;
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
