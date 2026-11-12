package com.jht.chimera.service.hal.lib.hardware;

import android.util.Log;

public class SafetyKeyManager {

    public final String TAG = "SafetyKeyManager";

    public interface SafetyKeyListener {
        void onSafetyKey(boolean isPulled);
    }

    public static SafetyKeyManager getInstance() {
        return SafetyKeyManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final SafetyKeyManager INSTANCE = new SafetyKeyManager();
    }

    private String mDeviceName;
    private SafetyKeyListener mSafetyKeyListener;
    private int mFd = -1;
    private int mSafetyKeyState = -1;
    private int mLastSafetyKeyState = -1;
    private Thread mThread;

    public void start(String deviceName, SafetyKeyListener safetyKeyListener) {
        mDeviceName = deviceName;
        mSafetyKeyListener = safetyKeyListener;
        try {
            Log.i(TAG, "==> starting: " + mDeviceName);
            mFd = Driver.safeOpen(deviceName);
            if (mFd < 0) {
                Log.e(TAG, "Could not start " + mDeviceName + " (open failed " + mFd + ")");
                return;
            }
            if (mThread == null) {
                mThread = new Thread(mRunnable, "SafetyKeyManager");
                mThread.start();
            }
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Could not start> ", ex);
        }
    }

    public void close() {
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        Driver.safeClose(mFd);
        mFd = -1;
    }

    public boolean isPulled() {
        return mSafetyKeyState == 0;
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            //mSafetyKeyState = Driver.readInt(mFd);
            //Log.i(TAG, "==> init mSafetyKeyState: " + mSafetyKeyState);

            while (!Thread.currentThread().isInterrupted()) {
                mSafetyKeyState = Driver.readInt(mFd);
                if (mLastSafetyKeyState != mSafetyKeyState) {
                    mSafetyKeyListener.onSafetyKey(isPulled());
                    mLastSafetyKeyState = mSafetyKeyState;
                    Log.i(TAG, "mSafetyKeyState: " + mSafetyKeyState);
                }
            }
        }
    };

}
