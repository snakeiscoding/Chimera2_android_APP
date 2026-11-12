package com.jht.chimera.lib.widget.streamingimageview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.Locale;

class BitmapFileStreamWithRecycling implements IBitmapStream {
    private final String mFileExtension;
    private IBitmapLoader mLoader;
    private final String mBasePath;
    private final String mBaseFilename;
    private final int mResourceCount;
    private Bitmap[] mBitmaps;
    private final String mFieldWidthString;
    private final int mBufferCount;
    private float mFrameNumber;
    private BitmapFactory.Options mOptions;
    private volatile boolean mDecodeNextFrame;
    private int mDisplayedFrameIdx;
    private Bitmap mCurrentBitmap;
    private String[] mFileNames;

    @SuppressWarnings("deprecation")
    BitmapFileStreamWithRecycling(String basePath, String baseFilename, String fileExtension, int frameIdFieldWidth, int startResourceIdx, int endResourceIdx, IBitmapLoader loader, int bufferCount) {
        mLoader = loader;
        mBasePath = basePath;
        mBaseFilename = baseFilename;
        mFileExtension = fileExtension;
        mResourceCount = (endResourceIdx - startResourceIdx) + 1;
        mBufferCount = bufferCount;
        mBitmaps = new Bitmap[mResourceCount];
        mFieldWidthString = "%0" + frameIdFieldWidth + "d"; //NON-NLS
        mFrameNumber = 0.0f;
        mFileNames = new String[mResourceCount];
        mOptions = new BitmapFactory.Options();
        mOptions.inPreferQualityOverSpeed = false;
        mOptions.inMutable = true;
        mOptions.inPurgeable = false;
        mOptions.inSampleSize = 1;
        mOptions.inBitmap = mCurrentBitmap;


        for (int i = 0; i < mResourceCount; i++) {
            mFileNames[i] = mBasePath + "/" + mBaseFilename + String.format(Locale.US, mFieldWidthString, i + startResourceIdx) + mFileExtension;
        }

        for (int i = 0; i < mBufferCount; i++) {
            mBitmaps[i] = mLoader.loadBitmap(mFileNames[i], mOptions);
        }
    }

    @Override
    public Bitmap getBitmap() {
        int displayedFrameIdx;
        synchronized (this) {
            displayedFrameIdx = mDisplayedFrameIdx;
            mCurrentBitmap = mBitmaps[displayedFrameIdx];
        }
        return mCurrentBitmap;
    }

    @Override
    public void nextFrame() {
        mDecodeNextFrame = true;
    }

    @Override
    public void update(float frameStep, boolean absoluteFrame) {
        if (!mDecodeNextFrame) {
            return;
        }
        mDecodeNextFrame = false;
        if (absoluteFrame) {
            mFrameNumber = frameStep;
        } else {
            mFrameNumber += frameStep;
        }
        mFrameNumber = mFrameNumber % mResourceCount;
        if (mBitmaps[(int) mFrameNumber] == null) {
            mBitmaps[(int) mFrameNumber] = mLoader.loadBitmap(mFileNames[(int) mFrameNumber], mOptions);
        }
        synchronized (this) {
            mDisplayedFrameIdx = (int) mFrameNumber;
        }
    }

    @Override
    public void enableDebugInfo(boolean onOff) {
    }

    @Override
    public void clear() {
        if(mBitmaps != null) {
            for(Bitmap b : mBitmaps) {
                if(b != null) {
                    b.recycle();
                }
            }
        }
        mFileNames = null;
        mBitmaps = null;
    }
}