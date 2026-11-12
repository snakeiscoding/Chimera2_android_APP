package com.jht.chimera.lib.tv.hdmi;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.util.List;

abstract public class HdmiCameraBase extends RestringActivity {

    public interface IOnOpen {
        void onOpen();
    }

    private static final String TAG = "HdmiCameraBase";
    private ImageView splashView;
    private HandlerThread mThread;
    private Handler handler;
    private final IOnOpen onOpen;
    private final ResolutionManager resolutionManager;
    private boolean released = false;
    private final JHTNotification notification = new JHTNotification();
    private DataMatrix dataMatrix;

    private final Object lock = new Object();

    HdmiCameraBase(@Nullable ImageView splash, @Nullable List<HDMIResolution> supportedResolutions, IOnOpen onOpen) {
        if (supportedResolutions == null) {
            resolutionManager = new ResolutionManager();
        } else {
            resolutionManager = new ResolutionManager(supportedResolutions);
        }
        this.splashView = splash;
        this.onOpen = onOpen;

        dataMatrix = new DataMatrix();

        ensureThread();

        dataMatrix.createBoolAdapter(TVKeys.DATA_CAMERA_IS_OPEN_BOOL, open -> {
            Log.d(TAG, "check to see if camera is open " + open);
            synchronized (lock) {
                if (released) {
                    Log.w(TAG, "Camera already released, ignore callback");
                    return;
                }
                if (!open) {
                    if (dataMatrix != null) {
                        dataMatrix.finish();
                    }

                    if (mThread != null && mThread.isAlive()) {
                        handler.post(() -> {
                            Log.d(TAG, "call open camera");
                            openCamera();
                            handler.post(() -> DataMatrix.putBoolean(TVKeys.DATA_CAMERA_IS_OPEN_BOOL, true));
                        });
                    } else {
                        Log.w(TAG, "mThread is null or dead, skip openCamera");
                    }
                }
            }
        }, true);
    }

    private void ensureThread() {
        synchronized (lock) {
            if (mThread == null || !mThread.isAlive()) {
                mThread = new HandlerThread("HDMI Input Render");
                mThread.start();
                handler = new Handler(mThread.getLooper());
                Log.d(TAG, "Handler initialized: true");
            }
        }
    }

    public void openCamera() {
        ensureThread();
    }

    public void closeCamera() {
        synchronized (lock) {
            if (mThread != null) {
                mThread.quitSafely();
                mThread = null;
                handler = null;
            }
        }
    }

    boolean isResolutionSupported() {
        return resolutionManager.isResolutionValid();
    }

    void onOpen() {
        if (splashView != null) {
            splashView.post(() -> splashView.setVisibility(View.GONE));
        }
        if (onOpen != null) {
            onOpen.onOpen();
        }
    }

    boolean released() { return released; }

    Handler getHandler() {
        return handler;
    }

    public void release() {
        new Thread(() -> {
            Log.d(TAG, "1 release camera");
            synchronized (lock) {
                if (!released) {
                    released = true;

                    if (dataMatrix != null) {
                        dataMatrix.finish();
                        dataMatrix = null;
                    }

                    Log.d(TAG, "2 release camera");
                    new Thread(() -> {
                        DataMatrix.putBoolean(TVKeys.DATA_CAMERA_IS_OPEN_BOOL, false);
                        Log.d(TAG, "3 release camera");

                        if (handler != null) {
                            handler.post(() -> {
                                closeCamera();
                                splashView = null;
                                notification.setNotification();
                            });
                        } else {
                            closeCamera();
                            splashView = null;
                            notification.setNotification();
                        }

                        notification.waitForNotification(1000);
                        Log.d(TAG, "4 release camera");
                    }).start();
                }
            }
        }).start();
    }

    public abstract void reopen();
}
