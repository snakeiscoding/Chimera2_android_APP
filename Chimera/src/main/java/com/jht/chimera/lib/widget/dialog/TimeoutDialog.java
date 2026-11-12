package com.jht.chimera.lib.widget.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.timer.AbsoluteTimer;
import com.jht.chimera.lib.common.timer.AbsoluteTimerTask;

import org.jetbrains.annotations.NonNls;

public abstract class TimeoutDialog extends Dialog {
    @NonNls protected final String TAG = "TimeoutDialog";

    protected int mDefaultTimeoutSec = 15;
    private volatile AbsoluteTimer mTimeoutTimer;

    public TimeoutDialog(@NonNull Context context) {
        super(context);
    }

    public synchronized void resumeTimeout() {
        stopTimeout();
        mTimeoutTimer = new AbsoluteTimer();
        mTimeoutTimer.schedule(new AbsoluteTimerTask() {
            @Override
            public void run() {
                try {
                    stopTimeout();
                    dismiss();
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            }
        }, mDefaultTimeoutSec * 1000);

    }

    public synchronized void stopTimeout() {
        if (mTimeoutTimer != null) {
            mTimeoutTimer.cancel();
            mTimeoutTimer.purge();
            mTimeoutTimer = null;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            resumeTimeout();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }


    @Override
    public void onStart() {
        super.onStart();
        resumeTimeout();

    }

    @Override
    protected void onStop() {
        super.onStop();
        stopTimeout();
    }
}
