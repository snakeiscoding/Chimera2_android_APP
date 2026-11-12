package com.jht.chimera.lib.widget.streamingimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.jht.chimera.R;


public class StreamingImageView extends ImageView {
    private static final String TAG = "StreamingImageView";
    private Handler mHandler;
    private long mDesiredFrameTimeMs;
    private float mSpeed;
    private float mFrame;

    private IBitmapLoader mBitmapLoader;
    private IBitmapStream mBitmapStream;
    private volatile boolean mThreadExit;
    private volatile boolean mThreadRun;
    private Thread mBitmapStreamerThread;
    private String fileExtension = ".jpg"; //NON-NLS
    private Gatso mGatso;
    private Runnable bitmapSetter;
    private int mNumFrames;
    private Object mLock = new Object();

    private IStreamingDuplicateImage iStreamingDuplicateImage;

    public StreamingImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Extract XML layout attributes.
        String basePath = "", baseFilename = "";
        int startIndex = 0, endIndex = 0;
        int frameIdFieldWidth = 0;
        int fps = 24;
        float speed = 1.0f;
        boolean useFileCache = false;
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.StreamingImageView, 0, 0);
        for (int i = 0; i < ta.getIndexCount(); ++i) {
            int attr = ta.getIndex(i);
            if (attr == R.styleable.StreamingImageView_basePath) {
                basePath = ta.getString(attr);
            } else if (attr == R.styleable.StreamingImageView_baseFilename) {
                baseFilename = ta.getString(attr);
            } else if (attr == R.styleable.StreamingImageView_frameIdFieldWidth) {
                frameIdFieldWidth = ta.getInteger(attr, 0);
            } else if (attr == R.styleable.StreamingImageView_startIndex) {
                startIndex = ta.getInteger(attr, 0);
            } else if (attr == R.styleable.StreamingImageView_endIndex) {
                endIndex = ta.getInteger(attr, 0);
            } else if (attr == R.styleable.StreamingImageView_fps) {
                fps = ta.getInteger(attr, 0);
            } else if (attr == R.styleable.StreamingImageView_speed) {
                speed = ta.getFloat(attr, 0);
            } else if (attr == R.styleable.StreamingImageView_useFileCache) {
                useFileCache = ta.getBoolean(attr, useFileCache);
            } else if (attr == R.styleable.StreamingImageView_fileExtension) {
                fileExtension = ta.getString(attr);
            }
        }
        ta.recycle();

        // Handler to update the ImageView on the UI thread.
        mHandler = new Handler();

        // Frame time based of the playback FPS.
        mDesiredFrameTimeMs = (long) (1000.0f / fps);

        // Speed of playback (i.e. 1.0, 2.5 etc.... not FPS).
        mSpeed = speed;

        // Create the Bitmap stream containing the sequence of images to be displayed.
        if (useFileCache) {
            mBitmapLoader = new BitmapLoaderCached();
        } else {
            mBitmapLoader = new BitmapLoaderUncached();
        }
        //mBitmapLoader.enableDebugInfo(true);
        mBitmapStream = new BitmapFileStreamWithRecycling(basePath, baseFilename, fileExtension, frameIdFieldWidth, startIndex, endIndex, mBitmapLoader, 4);
        mNumFrames = endIndex - startIndex + 1;
        //mStream.enableDebugInfo(true);

        // Create the Thread that does the streaming.
        mThreadExit = false;
        mThreadRun = false;

        bitmapSetter = () -> {
            setImageBitmap(mBitmapStream.getBitmap());
            if (iStreamingDuplicateImage != null) {
                iStreamingDuplicateImage.duplicateImage(mBitmapStream.getBitmap());
            }
//				mGatso.update();
        };

        if (fps > 0) {
            mBitmapStreamerThread = new Thread(() -> {
                Log.d(TAG, "BitmapStreamerThread starting");
                while (!mThreadExit) {
                    synchronized (mLock) {
                        if (mThreadRun) {
                            long startTime = System.nanoTime();
                            mHandler.post(bitmapSetter);
                            mBitmapStream.nextFrame();
                            if (mSpeed != 0) {
                                mBitmapStream.update(mSpeed, false);
                            } else {
                                mBitmapStream.update(mFrame, true);
                            }
                            long actualFrameTimeMs = (System.nanoTime() - startTime) / 1000000;
                            //Log.d("DEBUG","actualFrameTimeMs = " + actualFrameTimeMs);
                            if (actualFrameTimeMs < mDesiredFrameTimeMs) {
                                // Sleep until it's time for the next frame.
                                try {
                                    Thread.sleep(mDesiredFrameTimeMs - actualFrameTimeMs);
                                } catch (InterruptedException ignore) {
                                }
                            }
                        } else {
                            try {
                                mLock.wait();
                            } catch (InterruptedException ignore) {
                            }
                        }
                    }
                }
                Log.d(TAG, "BitmapStreamerThread exiting");
            });
            mBitmapStreamerThread.start();
        }

        // Debug stuff.
        mGatso = new Gatso();
        mGatso.setName(baseFilename);
    }

    public void setSpeed(float speed) {
        mSpeed = speed;
    }

    public void setFrame(float frame) {
        mFrame = frame;
    }

    public void setManualFrame(float frame) {
        mFrame = frame;
        mBitmapStream.nextFrame();
        mBitmapStream.update(mFrame, true);
        mHandler.post(bitmapSetter);
    }

    public void setTintColor(int color) {
        setColorFilter(color);
    }

    public synchronized void resume() {
        mThreadRun = true;
        synchronized (mLock) {
            mLock.notify();
        }
    }

    public synchronized void pause() {
        mThreadRun = false;
        mHandler.removeCallbacks(bitmapSetter);
    }

    public synchronized void shutdown() {
        pause();
        mThreadExit = true;
        synchronized (mLock) {
            mLock.notify();
        }
        if (mBitmapStreamerThread != null) {
            boolean retry = true;
            while (retry) {
                try {
                    mBitmapStreamerThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    Log.d(TAG, "$$$$$ StreamingImageView#shutdown() InterruptedException caught: $$$$$", e);
                }
            }
            mBitmapStreamerThread = null;
        }

        // Release any cached PNG files.
        mBitmapLoader.clear();
        mBitmapStream.clear();
        Log.d(TAG, "StreamingImageView#shutdown() complete!");
    }

    public void enableDebugInfo(boolean onOff) {
        mGatso.enableFPS(onOff);
        mGatso.enableMemoryStats(onOff);
    }

    public interface IStreamingDuplicateImage {
        void duplicateImage(Bitmap bitmap);
    }

    public void setDuplicateImageInterface(IStreamingDuplicateImage iDuplicateStreamingImage) {
        this.iStreamingDuplicateImage = iDuplicateStreamingImage;
    }

    public int getNumFrames() {
        return mNumFrames;
    }
}
