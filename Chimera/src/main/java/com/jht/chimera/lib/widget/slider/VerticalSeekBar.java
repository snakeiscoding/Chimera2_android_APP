package com.jht.chimera.lib.widget.slider;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

/**
 * <p>Manages implementation details for the Vertical version of the SeekBar.</p>
 * <p>Created 04/28/2019 by Kong Yang.</p>
 */
public class VerticalSeekBar extends SeekBar {
    public interface OnReleaseListener {
        void onRelease();
    }

    OnReleaseListener mOnReleaseListener;

    public void setOnReleaseListener(OnReleaseListener listener) {
        mOnReleaseListener = listener;
    }

    public VerticalSeekBar(Context context) {
        super(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    /**
     * @param widthMeasureSpec horizontal space requirements as imposed by the parent.
     *                         The requirements are encoded with
     *                         {@link android.view.View.MeasureSpec}.
     * @param heightMeasureSpec vertical space requirements as imposed by the parent.
     *                         The requirements are encoded with
     *                         {@link android.view.View.MeasureSpec}.
     *
     */
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    /**
     * @param progress the new progress, between {@link #getMin()} and {@link #getMax()}
     */
    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }

    /**
     * @param c the canvas on which the background will be drawn
     */
    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);
        super.onDraw(c);
    }

    /**
     * Handles when seek bar is adjusted.
     * @param event The motion event.
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                int i = getMax() - (int) (getMax() * event.getY() / getHeight());
                setProgress(i);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (mOnReleaseListener != null) mOnReleaseListener.onRelease();
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }

}