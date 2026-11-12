package com.jht.mastboard.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import java.io.InputStream;

public class LCDImageViewer extends androidx.appcompat.widget.AppCompatImageView {

    // variables for animated gif
    Thread mGifThread;
    Movie mAnimagedGif;
    long mGifStartMs;
    int mGifDuration, mGifWeight, mGifHeight;
    Bitmap mGifBitmap;
    Canvas mGifCanvas;

    public LCDImageViewer(Context context) {
        super(context);
    }

    public LCDImageViewer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LCDImageViewer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void setAnimatedGifResource(int resourceID) {

        InputStream inputStream = getContext().getResources().openRawResource(resourceID);
        mAnimagedGif = Movie.decodeStream(inputStream);

        mGifDuration = mAnimagedGif.duration();
        if (mGifDuration == 0) {
            mGifDuration = 1000;
        }
        mGifStartMs = SystemClock.uptimeMillis();
        mGifWeight = mAnimagedGif.width();
        mGifHeight = mAnimagedGif.height();
        mGifBitmap = Bitmap.createBitmap(mGifWeight, mGifHeight, Bitmap.Config.ARGB_8888);
        mGifCanvas = new Canvas(mGifBitmap);
        startAnimation();
    }

    @Override
    public void draw(Canvas canvas) {
        if (mAnimagedGif != null) {
            super.draw(canvas);
            int relTime = (int) ((SystemClock.uptimeMillis() - mGifStartMs) % mGifDuration);
            mAnimagedGif.setTime(relTime);
            mAnimagedGif.draw(mGifCanvas, 0, 0);
            canvas.drawBitmap(Bitmap.createScaledBitmap(mGifBitmap, LCDPresentation.WIDTH, LCDPresentation.HEIGHT, true), 0, 0, new Paint());
        } else {
            super.draw(canvas);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    void startAnimation() {

        stopAnimation();
        mGifThread = new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    SystemClock.sleep(50);
                    // hack not sure why invalidate doesn't work
                    // invalidate();
                    if (getParent() != null) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        getParent().onDescendantInvalidated(this, this);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        mGifThread.start();
    }

    void stopAnimation() {
        if (mGifThread != null) {
            mGifThread.interrupt();
            try {
                mGifThread.join(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mGifThread = null;
        }
    }
}
