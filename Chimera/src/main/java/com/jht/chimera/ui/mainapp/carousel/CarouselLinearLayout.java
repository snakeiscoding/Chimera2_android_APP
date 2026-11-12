package com.jht.chimera.ui.mainapp.carousel;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class CarouselLinearLayout extends LinearLayout {
    private float scale = 1.0f;

    public CarouselLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    public CarouselLinearLayout(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public void setScaleBoth(float scale) {
        this.scale = scale;
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = this.getWidth();
        int h = this.getHeight();
        canvas.scale(scale, scale, w / 2, h / 2);
    }

}
