package com.jht.chimera.ui.mainapp.goscreen;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * Created by AlexN on 2/12/2019.
 */

public class BitmapAsyncLoadTask extends AsyncTask<Uri, Void, Bitmap> {
    private final WeakReference<ImageView> imageViewReference;
    private int width;
    private int height;

    public BitmapAsyncLoadTask(ImageView imageView, int width, int height) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<ImageView>(imageView);
        this.width = width;
        this.height = height;
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(Uri... params) {
        final ImageView imageView = imageViewReference.get();
        if (imageView == null) return null;
        return BitmapLoader.decodeSampledBitmap(imageView.getContext(), params[0], width, height);
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()){
            bitmap = null;
        }

        if (bitmap != null) {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}
