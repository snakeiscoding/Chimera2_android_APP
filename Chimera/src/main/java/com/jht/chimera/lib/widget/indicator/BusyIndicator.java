package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.content.res.AppCompatResources;

import com.jht.chimera.R;


public class BusyIndicator extends ImageView {

    Animation spinClockwise;

    boolean animationStarted = false;

    public BusyIndicator(Context context) {
        super(context);
        init();
    }

    public BusyIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.ic_spinner));

        spinClockwise = AnimationUtils.loadAnimation(getContext(),
                R.anim.spinning_clockwise);

        this.setVisibility(View.GONE);
    }

    public void startProgress() {
        this.startAnimation(spinClockwise);
        show();
        animationStarted = true;
    }

    public void stopProgress() {
        if (animationStarted) {
            spinClockwise.cancel();
            spinClockwise.reset();
            this.setAnimation(null);
        }

        hide();
    }

    public void show() {
        this.setVisibility(View.VISIBLE);
    }

    public void hide() {
        this.setVisibility(View.GONE);
    }
}
