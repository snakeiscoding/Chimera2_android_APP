package com.jht.chimera.service.hal.lib;

import static android.content.Context.WINDOW_SERVICE;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;

public class FloatingStatPanel extends LinearLayout {

    private int mXStart;
    private int mYStart;
    private WindowManager mWindowManager;
    private boolean mStatsPanelVisible = false;
    private TextView mTextView;
    private Handler mHandler = new Handler();

    public FloatingStatPanel(Context context) {
        super(context);
        mWindowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        initialize(context);
    }

    private void initialize(final Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.floating_stat_panel, this, true);

        findViewById(R.id.stat_panel).setOnTouchListener(mOnTouchListener);
        mTextView = findViewById(R.id.stat_text);
    }

    public void setText(final String text) {
        if (mStatsPanelVisible) {
            mHandler.post(() -> mTextView.setText(text));
        }
    }

    public void show() {
        if (!mStatsPanelVisible) {
            mStatsPanelVisible = true;
            mHandler.post(() -> mWindowManager.addView(FloatingStatPanel.this, getPanelParams()));
        }
    }

    public void hide() {
        if (mStatsPanelVisible) {
            mStatsPanelVisible = false;
            mHandler.post(() -> mWindowManager.removeView(FloatingStatPanel.this));
        }
    }

    WindowManager.LayoutParams getPanelParams() {
        WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ChimeraEnums.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);

        viewParams.gravity = Gravity.TOP | Gravity.CENTER;
        viewParams.x = 0;
        viewParams.y = 100;
        return viewParams;
    }


    OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int x = (int) event.getRawX();
            final int y = (int) event.getRawY();

            switch (event.getAction() & MotionEvent.ACTION_MASK) {

                case MotionEvent.ACTION_DOWN:
                    mXStart = x;
                    mYStart = y;
                    break;

                case MotionEvent.ACTION_UP:
                    break;

                case MotionEvent.ACTION_MOVE:
                    WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) getLayoutParams();
                    layoutParams.x += x - mXStart;
                    layoutParams.y += y - mYStart;
                    mXStart = x;
                    mYStart = y;
                    mWindowManager.updateViewLayout(FloatingStatPanel.this, layoutParams);
            }

            return true;
        }
    };
}
