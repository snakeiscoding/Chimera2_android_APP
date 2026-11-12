package com.jht.chimera.lib.widget.streamingimageview;

import android.util.Log;

class Gatso 
{
	private static final String TAG = "Gatso";
	private String mName;
	private long mLastTime;
	private float mFrameCount;
	private boolean mShowFPS;
	private boolean mShowMemoryStats;

	public Gatso() {
		mName = "default"; //NON-NLS
		mShowFPS = false;
		mShowMemoryStats = false;
		mLastTime = System.currentTimeMillis();
		mFrameCount = 0.0f;				
	}

	public void setName(String name) {
		mName = new String(name);
	}
	
	public void enableFPS(boolean showFPS) {
		mShowFPS = showFPS;	
	}
	
	public void enableMemoryStats(boolean showMemoryStats) {
		mShowMemoryStats = showMemoryStats;	
	}
	
	public void update() {
        mFrameCount++;
        final long currentTime = System.currentTimeMillis();
        final long elapsedTime = currentTime - mLastTime;
        if(elapsedTime >= 1000) {
        	if(mShowFPS || mShowMemoryStats) {
        		Log.d(TAG, "Gatso : " + mName);
        	
	        	if(mShowFPS) {
	        		float fps = mFrameCount / (elapsedTime / 1000.0f);
	            	Log.d(TAG, "FPS = " + String.format("%.1f", fps));
	        	}
	        	
	        	if(mShowMemoryStats) {
		            Log.d(TAG, "Heap used (MB): " + Long.toString(Runtime.getRuntime().totalMemory() / (1024 * 1024)));
		            Log.d(TAG, "Heap free (MB): " + Long.toString(Runtime.getRuntime().freeMemory() / (1024 * 1024)));
		            Log.d(TAG, "Heap max allowed (MB): " + Long.toString(Runtime.getRuntime().maxMemory() / (1024 * 1024)));
	        	}
        	}
        	mFrameCount = 0.0f;
            mLastTime = currentTime;
        }
    }
}