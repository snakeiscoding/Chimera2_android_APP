package com.jht.chimera.lib.widget.streamingimageview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

interface IBitmapLoader {
	// Load Bitmap.
	Bitmap loadBitmap(String fileName, BitmapFactory.Options options);
	
	// Enable debug logging.
	void enableDebugInfo(boolean onOff);
	
	// Call this to empty any caches (if implemented in sub-class).
	void clear();
}

