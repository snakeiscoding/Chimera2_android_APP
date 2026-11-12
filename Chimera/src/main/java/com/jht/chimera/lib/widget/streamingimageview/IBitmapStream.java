package com.jht.chimera.lib.widget.streamingimageview;

import android.graphics.Bitmap;

interface IBitmapStream
{	
	// Get the current frame (Bitmap). 
	Bitmap getBitmap();
	
	// Request that the next frame be loaded.
	void nextFrame();

	// Called to load the next frame (requested via nextFrame()) .
	// Absolute frame will load the specific frameStep requested
	void update(float frameStep, boolean absoluteFrame);
	
	// Enable debug logging.
	void enableDebugInfo(boolean onOff);

	//Clears loaded bitmaps
	void clear();
}
