package com.jht.chimera.service.ui.panels;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;

/**
 * A black overlay meant to hide poor animations from screen size switches and so on.
 * <p>
 * Created by chris.foyer on 3/28/2018.
 */
public class BlackOverlayPanel extends LinearLayout implements IPanel {
    private View mOverlayView;

    public BlackOverlayPanel(Context context) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.black_overlay_screen, this, true);
        mOverlayView = findViewById(R.id.black_overlay_full_view);
    }

    @Override
    public void panelAdded() {
        AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(200);
        fadeIn.setFillAfter(true);
        mOverlayView.startAnimation(fadeIn);
    }

    /**
     * Removes the workout panel after fading out the overlay
     */
    public void removePanelWithAnimation() {
        AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(200);
        fadeOut.setFillAfter(true);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                WorkoutPanelManager.getInstance().showPanel(PanelIds.OVERLAY_PANEL, false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        mOverlayView.startAnimation(fadeOut);
    }

    @Override
    public void panelUpdated() {}

    @Override
    public void panelRemoved() {
    }

    @Override
    public void dispose() {}

    @Override
    public String getDebugName() {
        return getClass().getName();
    }

    @Override
    public boolean setPanelMode(WorkoutPanelMode mode) { return false; }

    @Override
    public WindowManager.LayoutParams getPanelParams() {
        WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
        viewParams.gravity = Gravity.NO_GRAVITY;
        viewParams.x = 0;
        viewParams.y = 0;
        return viewParams;
    }

    @Override
    public View getView() {
        return this;
    }
}
