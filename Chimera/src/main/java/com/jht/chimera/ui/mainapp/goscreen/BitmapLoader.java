package com.jht.chimera.ui.mainapp.goscreen;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class BitmapLoader {
    @NonNls private static final String TAG = "BitmapLoader";

    //Returns false if needed to do post load
    public static boolean loadSampledBitmap(final Context context, final ImageView view, final Uri uri){
        if(uri == null || uri.toString().isEmpty()){
            return true;
        }
        if (view.getMeasuredWidth() == 0 || view.getMeasuredHeight() == 0) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    view.setImageBitmap(BitmapLoader.decodeSampledBitmap(context, uri,
                            view.getMeasuredWidth(), view.getMeasuredHeight()));
                }
            });
            return false;
        }
        else{
            view.setImageBitmap(BitmapLoader.decodeSampledBitmap(context, uri,
                    view.getMeasuredWidth(), view.getMeasuredHeight()));
        }
        return true;
    }

    private static InputStream getStream(Context context, Uri uri) {
        try {
            if (uri.getScheme() == null) {
                // Assume a file.
                return new FileInputStream(new File(uri.toString()));
            }
            switch (uri.getScheme()) {
                case "assets": //NON-NLS
                    String path = uri.toString().substring("assets://".length()); //NON-NLS
                    File file = new File(context.getFilesDir(), path);
                    if(file.exists()) {
                        return new FileInputStream(file);
                    }
                    return context.getAssets().open(path);

                case ContentResolver.SCHEME_ANDROID_RESOURCE:
                case ContentResolver.SCHEME_CONTENT:
                case ContentResolver.SCHEME_FILE:
                    return context.getContentResolver().openInputStream(uri);
                default:
                    // Assume this is web.
                    return (new URL(uri.toString())).openStream();
            }
        } catch(Exception ex) {
            return null;
        }
    }

    public static Bitmap decodeSampledBitmap(Context context, Uri uri, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        InputStream stream = null;
        options.inJustDecodeBounds = true;

        //noinspection TryFinallyCanBeTryWithResources
        try {
            stream = getStream(context, uri);
            BitmapFactory.decodeStream(stream, null, options);
            if(stream != null) { stream.close(); }
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            stream = getStream(context, uri);
            return BitmapFactory.decodeStream(stream, null, options);
        }
        catch (Exception ex) {
            Log.w(TAG, ex);
        }
        finally {
            if(stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {}
            }
        }
        return null;
    }

    public static Bitmap decodeSampledBitmapResource(Resources resources, int id, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, id, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return  BitmapFactory.decodeResource(resources, id, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if ((height > reqHeight || width > reqWidth) && reqHeight !=0 && reqWidth != 0) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
