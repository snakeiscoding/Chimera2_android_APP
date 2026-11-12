package com.jht.chimera.lib.widget.streamingimageview;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class BitmapLoaderCached implements IBitmapLoader {
	@NonNls private final String TAG = "BitmapLoaderCached";
	Map<String, byte[]> mLookup;
	boolean mDebug;
	Context mContext;

	private synchronized byte[] getAssetsFile(String fileName) {
		try {
			if(mLookup.get(fileName) == null) {

				AssetManager am = mContext.getAssets();

				AssetFileDescriptor fd = am.openFd(fileName);
				int len = (int)fd.getLength();
				fd.close();

				if(len > 0) {
					byte[] buffer = new byte[len];
					InputStream is = am.open(fileName);
					is.read(buffer);
					is.close();
					mLookup.put(fileName, buffer);
				}
			}
			return mLookup.get(fileName);
		}
		catch(Exception e) {
			Log.w(TAG, e);
			return null;
		}
	}

	private synchronized byte[] getFile(String fileName) {
		try {
			if(mLookup.get(fileName) == null) {
				File file = new File(fileName);
				int len = (int)file.length();
				if(len > 0) {
					byte[] buffer = new byte[len];
					InputStream is = new FileInputStream(fileName);
					is.read(buffer);
					is.close();
					mLookup.put(fileName, buffer);
				}
			}
			return mLookup.get(fileName);
		}
		catch(Exception e) {
			Log.w(TAG, e);
			return null;
		}
	}
	
	BitmapLoaderCached() {
		mLookup = new HashMap<String, byte[]>();

		try {
			final Class<?> activityThreadClass = Class.forName("android.app.ActivityThread"); //NON-NLS
			final Method method = activityThreadClass.getMethod("currentApplication"); //NON-NLS
			Application application = (Application) method.invoke(null, (Object[]) null);
			mContext = application.getApplicationContext();
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}
		
	@Override
	public Bitmap loadBitmap(String fileName, BitmapFactory.Options options) {
		try {
			//byte[] buffer = getAssetsFile(fileName);
			byte[] buffer = getFile(fileName);
			if(buffer != null) {
				long startTime = System.currentTimeMillis();
				Bitmap bm = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);
				if(mDebug) {	
					Log.d(TAG, "decodeByteArray : " + fileName + ". Time (MS) = " + Long.toString(System.currentTimeMillis() - startTime));
				}
				return bm;
			}
			return null;
		}
		catch(Exception e) {
			Log.w(TAG, e);
			return null;			
		}
	}
	
	@Override
	public synchronized void clear() {
		mLookup = new HashMap<String, byte[]>();
	}
	
	@Override
	public void enableDebugInfo(boolean onOff) {
		mDebug = onOff;
	}	
}
