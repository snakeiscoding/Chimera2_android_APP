package com.jht.chimera.lib.widget.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import com.jht.chimera.R;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.util.Timer;
import java.util.TimerTask;

public class DialogSpinner {
    @NonNls private static final String TAG = "DialogSpinner";

    public interface OnTimeoutListener {
        void onTimeout();
    }

    static class LayoutSpinner extends RelativeLayout {
        public Context mContext;
        private View mSpinner;

        private LayoutSpinner(Context context) {
            super(context);
            mContext = context;

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.dialog_spinner, this, true);
            mSpinner = findViewById(R.id.imageSpinner);
        }

        private void startAnimation() {
            mSpinner.post(() -> {
                Animation rotateAnimation = AnimationUtils.loadAnimation(mContext, R.anim.spinning_clockwise);
                mSpinner.startAnimation(rotateAnimation);
            });
        }

        private void stopAnimation() {
            mSpinner.post(() -> mSpinner.clearAnimation());
        }
    }

    private static Dialog mDialog;
    private static LayoutSpinner mLayoutSpinner;
    private static Timer mHeartBeatTimer;
    private static DataMatrix mDataMatrix = new DataMatrix();
    private static int mTimeout = 0;
    private static int mTimeoutCounter = 0;
    private static OnTimeoutListener mOnTimeoutListener = null;


    public static void show(Context context) {
        show(context, 0, null);
    }

    public static void show(Context context, int timeout, OnTimeoutListener onTimeoutListener) {

        try {
            dismiss();
            mTimeout = timeout;
            mTimeoutCounter = 0;
            mOnTimeoutListener = onTimeoutListener;
            mDialog = new Dialog(context);
            mLayoutSpinner = new LayoutSpinner(context);
            mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mDialog.setContentView(mLayoutSpinner);
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.argb(0, 255, 255, 255)));
            mLayoutSpinner.startAnimation();
            startResetTouchTimer();
            mDialog.show();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static void dismiss() {
        mTimeout = 0;
        mTimeoutCounter = 0;
        mOnTimeoutListener = null;
        if (mDialog != null) {
            stopResetTouchTimer();
            mLayoutSpinner.stopAnimation();
            mDialog.dismiss();
        }
    }

    private static void startResetTouchTimer() {
        stopResetTouchTimer();
        mHeartBeatTimer = new Timer();
        mHeartBeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // reset the screen the timeout if spinner is visible
                //mDataMatrix.sendEvent(HALKeys.EVENT_SCREEN_TOUCH);
                mTimeoutCounter++;
                if (mTimeout > 0) {
                    if (mTimeoutCounter > mTimeout) {
                        if (mOnTimeoutListener != null) mOnTimeoutListener.onTimeout();
                        dismiss();
                    }
                }
            }
        }, 1000, 1000);
    }

    private static void stopResetTouchTimer() {
        if (mHeartBeatTimer != null) {
            mHeartBeatTimer.cancel();
            mHeartBeatTimer.purge();
            mHeartBeatTimer = null;
        }
    }
}
