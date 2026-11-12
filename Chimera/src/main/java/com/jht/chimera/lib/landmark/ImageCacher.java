package com.jht.chimera.lib.landmark;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.util.List;

public class ImageCacher {
    private static final String TAG = "LandmarksImageCacher";
    private List<Uri> _sources;
    private Bitmap[] _images = new Bitmap[3];
    private Bitmap _image;
    private int _position = 0;

    public void buildImages(final List<Uri> sources, int startIndex) throws IndexOutOfBoundsException {
        _sources = sources;

        loadImages(startIndex == 0 ? _sources.size() - 1 : startIndex - 1);
        _position = startIndex;
    }

    public void buildImages(final List<Uri> sources) {
        buildImages(sources, 0);
    }

    private void loadImages(int position) {
        position = position % _sources.size();
        for (int i = 0; i < 3; i++) {
            Log.e(TAG, "Loading image cache " + i + ", " + _sources.get(position));
            _images[i] = BitmapFactory.decodeFile(_sources.get(position++).getPath());
            position = position % _sources.size();
        }
        _image = _images[1];

    }


    public Bitmap getImage() {
        return _image;
    }

    // Discarded interface since this will block the UI thread.
    // Comment by Leaf.Chen 08/21 2015.
//	public Bitmap getImage(int position) {
//		loadImages(position == 0 ? _sources.size() - 1 : position - 1);
//		_position = position;
//		return _image;
//	}

    public int position() {
        return _position;
    }

    public Bitmap getNextImage() {
        _position = (_position + 1) % _sources.size();
        _images[0] = _images[1];
        _images[1] = _images[2];
        _image = _images[1];

        new Thread(() -> {
            Log.e(TAG, "Loading next image " + _sources.get((_position + 1) % _sources.size()));
            _images[2] = BitmapFactory.decodeFile(_sources.get((_position + 1) % _sources.size()).getPath());
        }).start();

        return _image;
    }

    public Bitmap getPreviousImage() {
        _position = _position == 0 ? _sources.size() - 1 : (_position - 1);

        _images[2] = _images[1];
        _images[1] = _images[0];
        _image = _images[1];

        Log.e(TAG, "Loading previous image " + _sources.get(_position));
        // Uncomment this code as we need it be a circle.
//		if (_position >= 1) { // make sure is not out of index
        new Thread(() -> {
            Log.e(TAG, "Loading previous image " + _sources.get(_position == 0 ? _sources.size() - 1 : (_position - 1)));
            _images[0] = BitmapFactory.decodeFile(_sources.get(_position == 0 ? _sources.size() - 1 : (_position - 1)).getPath());
        }).start();
//		}

        return _image;
    }
	
	/*
	public Bitmap getNextImage(final int index) {
		Bitmap bm = _image;
		
		new Thread(new Runnable(){

			@Override
			public void run()
			{
				if (index < _sources.size() - 1) {
					_image = BitmapFactory.decodeFile(_sources.get(index + 1));
				} else { // reset to HEAD when reach the end
					_image = BitmapFactory.decodeFile(_sources.get(0));
				}
			}
			
		}).start();
			

		return bm;
	}
	*/
}
