package com.jht.chimera.lib.download;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.jetbrains.annotations.NonNls;

import java.io.File;


public class MiscUtilHelper {
    @NonNls private static final String TAG = "MiscUtilHelper";
    private static final char[] hexDigits = "0123456789abcdef".toCharArray(); // NON-NLS

    private MiscUtilHelper() {
        // prevent instantiation
    }



    public static Bitmap decodeBackgroundImage(File file) {
        // Make sure the file exists.
        if (!file.exists()) {
            return null;
        }

        // Get the image dimensions.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;

        // Find the appropriate scale.
        options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        int height = Resources.getSystem().getDisplayMetrics().heightPixels;
        while (imageHeight > height && imageWidth > width) {
            options.inSampleSize *= 2;
            imageHeight /= 2;
            imageWidth /= 2;
        }

        // Decode the file.
        return BitmapFactory.decodeFile(file.getPath(), options);
    }
}
