package com.jht.mastboard.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.mastboard.device.MastboardManager;
import com.jht.mastboard.lib.PanelSide;

import java.util.concurrent.Semaphore;

public class LCDPanelRootView extends ConstraintLayout {

    private final long TIME_BETWEEN_FRAMES_MIN = 50;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private final Handler mHandler = new Handler();
    private long mLastUpdateTimestampMs = 0;
    private final Semaphore mIsDirty = new Semaphore(0);
    private Bitmap cachedBitmap;
    private final Object cacheLock = new Object();

    DebugView mDebugView;

    public LCDPanelRootView(Context context) {
        super(context);
    }

    public LCDPanelRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LCDPanelRootView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private int currBaseViewId = -1;
    @Override
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        currBaseViewId = view.getId();
    }

    @Nullable
    View getCurrentView() {
        return findViewById(currBaseViewId);
    }

    private boolean isRunning = false;
    private Thread refreshBitmapThread;
    private final Object objectEndThread = new Object();

    /**
     * Init should only be called once ready to draw. Not sure why it wouldn't work.
     *
     * @param panelSide
     */
    void init(@NonNull final PanelSide panelSide) {
        mBitmap = Bitmap.createBitmap(LCDPresentation.WIDTH, LCDPresentation.HEIGHT, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        if (refreshBitmapThread == null) {
            refreshBitmapThread = new Thread(() -> {
                isRunning = true;
                while (isRunning) {
                    try {
                        mIsDirty.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        isRunning = false;
                    }

                    if (cachedBitmap != null && isRunning) {
                        Bitmap tmp;
                        synchronized (cacheLock) {
                            tmp = Bitmap.createBitmap(cachedBitmap);
                        }
                        // TODO: this should be consistently 50 or more but I am getting less. Figure out why
                        mLastUpdateTimestampMs = SystemClock.uptimeMillis();
                        MastboardManager.drawBitmap(tmp, panelSide);
                    }
                }
                synchronized (objectEndThread) {
                    objectEndThread.notify();
                }
            });
            refreshBitmapThread.start();
        }

    }

    public void closeRootView() {
        isRunning = false;
        if (refreshBitmapThread != null) {
            refreshBitmapThread.interrupt();
            refreshBitmapThread = null;
        }
        synchronized (objectEndThread) {
            try {
                objectEndThread.wait(5000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        cachedBitmap = null;
        mBitmap = null;
        mCanvas = null;
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(mCanvas);
        updateScreen();
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        return super.drawChild(canvas, child, drawingTime);
    }

    void updateScreen() {
        synchronized (cacheLock) {
            cachedBitmap = Bitmap.createBitmap(mBitmap);
        }
        // update debug float panel if necessary
        if (mDebugView != null) {
            mDebugView.setBitmap(mBitmap);
        }
        mIsDirty.release();
    }

    private final Runnable dispatchDrawRunnable = new Runnable() {
        @Override
        public void run() {
            dispatchDraw(mCanvas);
        }
    };

    @SuppressLint("MissingSuperCall")
    @Override
    public void onDescendantInvalidated(@NonNull View child, @NonNull View target) {
        // TODO: why is this getting called so much and is there a better way to dispatch draws.
        // Note: We are taking over the Android drawing logic.
        // onDescendantInvalidated will be used to notify us that we need to update the canvas
        // dispatchDraw will update the canvas but we don't want to draw too fast since the
        // handgrip LCD is might only support 20 fps
        long deltaTimestampMs = SystemClock.uptimeMillis() - mLastUpdateTimestampMs;
        mHandler.removeCallbacks(dispatchDrawRunnable);
        if (deltaTimestampMs > TIME_BETWEEN_FRAMES_MIN) {
            mHandler.post(dispatchDrawRunnable);
        }
        else {
            mHandler.postDelayed(dispatchDrawRunnable, TIME_BETWEEN_FRAMES_MIN - deltaTimestampMs);
        }
    }

    public void setDebugView(DebugView view) {
        mDebugView = view;
        if (mDebugView != null) mDebugView.setBitmap(mBitmap);
    }

}
