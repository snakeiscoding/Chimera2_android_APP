package com.jht.chimera.lib.widget.streamingimageview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

class BitmapLoaderUncached implements IBitmapLoader
{
	private static final String TAG = "BitmapLoaderUncached";
	boolean mDebug;
	
	@Override
	public Bitmap loadBitmap(String fileName, BitmapFactory.Options options) {
		long startTime = System.currentTimeMillis();
		Bitmap bm = BitmapFactory.decodeFile(fileName, options);
		if(mDebug) {
			Log.d(TAG, "decodeFile : " + fileName + ". Time (MS) = " + Long.toString(System.currentTimeMillis() - startTime));
		}
		return bm;
	}
	
	@Override
	public void clear() {}

	@Override
	public void enableDebugInfo(boolean onOff) {
		mDebug = onOff;
	}	
}
