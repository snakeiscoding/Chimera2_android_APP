package com.jht.chimera.service.hal.lib.io;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;

public class KeyPressAndHoldRepeatHandler {
    private static final String TAG = "KeyPressAndHoldRepea...";
    private JHTTimer holdTimer = new JHTTimer(new JHTTimer.IOnTimer() {
        @Override
        public void onTimer() {
            handler.post(new Runnable() {
                @Override
                public void run() {

                    if (slowTimer != null) {
                        Log.d(TAG, "keyEvent: slowTimer start");
                        slowTimer.start(slowSpeedMS, slowSpeedMS);
                    }
                    numPresses++;
                    sendKeyEvent();
                }
            });

        }
    });
    private PeriodicTimer slowTimer = new PeriodicTimer(new JHTTimer.IOnTimer() {
        @Override
        public void onTimer() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (numPresses == numSlowPresses + 1) {
                        slowTimer.stop();
                        Log.d(TAG, "keyEvent: slowTimer stop and fastTimer start");
                        if (fastTimer != null)
                            fastTimer.start(fastSpeedMS, fastSpeedMS);
                    }
                    numPresses++;
                    sendKeyEvent();
                }
            });
        }
    });
    private PeriodicTimer fastTimer = new PeriodicTimer(new JHTTimer.IOnTimer() {
        @Override
        public void onTimer() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    numPresses++;
                    sendKeyEvent();
                    if (numPresses >= maxPresses) {
                        Log.d(TAG, "keyEvent: fastTimer stop");
                        fastTimer.stop();
                    }
                }
            });

        }
    });

    private int holdTimeoutMS = 1000;
    private int slowSpeedMS = 500;
    private int numSlowPresses = 2;
    private int fastSpeedMS = 100;
    private int maxPresses = -1;
    private int numPresses = 0;
    private Handler handler;
    private Context context;
    private JHTCommonTypes.JHT_KEY key;
//    private boolean timerStop = false;

    public KeyPressAndHoldRepeatHandler(Context context, Handler handler, JHTCommonTypes.JHT_KEY key, int maxPresses) {
        init(context, handler, key, holdTimeoutMS, slowSpeedMS, numSlowPresses, fastSpeedMS, maxPresses);
    }


    public void setMaxPresses(int maxPresses) {
        this.maxPresses = maxPresses;
    }

    private void init(Context context, Handler handler, JHTCommonTypes.JHT_KEY key, int holdTimeoutMS, int slowSpeedMS, int numSlowPresses, int fastSpeedMS, int maxPresses) {
        Log.d(TAG, "keyEvent: init maxPresses: " + maxPresses);
//        handler = new Handler();
        this.context = context;
        this.handler = handler;
        this.key = key;
        this.holdTimeoutMS = holdTimeoutMS;
        this.slowSpeedMS = slowSpeedMS;
        this.numSlowPresses = numSlowPresses;
        this.fastSpeedMS = fastSpeedMS;
        this.maxPresses = maxPresses;

    }

    private void sendKeyEvent() {
        Intent keyIntent = new Intent("com.jht.keypress");
        keyIntent.putExtra("event", 1);
        keyIntent.putExtra("key", key);
        context.sendBroadcast(keyIntent);
        Log.d(TAG, "keyEvent: sendBroadcast key: " + key.toString() + "  repeat: " + numPresses);
    }

    public void startRepeatTimer() {
        numPresses = 1;
        sendKeyEvent();
        if (holdTimer != null) {
            holdTimer.start(holdTimeoutMS);
        }
    }

    public void stopRepeatTimer() {
        Log.d(TAG, "keyEvent: stop all repeat timer");
        holdTimer.stop();
//        holdTimer = null;
        slowTimer.stop();
//        slowTimer = null;
        fastTimer.stop();
//        fastTimer = null;
    }
}
