package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatTextView;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.widget.dialog.ProductDashboardHost;
import com.jht.datamatrixlib.DataMatrix;


public class TopBarTextView extends AppCompatTextView {

    GestureDetector mGestureDetector;
    private JHTTimer logoPressTimer;
    private Handler mHandler = new Handler();

    public TopBarTextView(Context context) {
        super(context);
        init();
    }

    public TopBarTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public TopBarTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    void init() {

        logoPressTimer = new JHTTimer(2000, () -> mHandler.post(ProductDashboardHost::show));

        mGestureDetector = new GestureDetector(getContext(), new MyGestureListener());
        setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                new Thread(() -> {
                    logoPressTimer.start();
                }).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                logoPressTimer.stop();
            }
            return mGestureDetector.onTouchEvent(event);
        });
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
            DataMatrix.sendEvent(LoggerKeys.ACTION_SHOW_HAL_DLG);
            return true;
        }
    }
}
