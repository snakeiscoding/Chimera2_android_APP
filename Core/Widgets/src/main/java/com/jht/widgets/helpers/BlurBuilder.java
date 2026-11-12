package com.jht.widgets.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

import java.io.InputStream;

public class BlurBuilder {
    private static final String TAG = "BlurBuilder";
    private static final float BLUR_RADIUS = 25f;

    @SuppressWarnings("deprecation")

    public static Bitmap blur(Context context, Uri uri, Rect rect, int width, int height) {
        Bitmap blurredBitmap = null;
        Bitmap source = getScaledSourceImage(context, uri, width, height);
        if(source != null) {
            // Create another bitmap that will hold the results of the filter.
            blurredBitmap = Bitmap.createBitmap(source, rect.left, rect.top, rect.right, rect.bottom);

            // Allocate memory for Renderscript to work with
            RenderScript rs = RenderScript.create(context);
            Allocation input = Allocation.createFromBitmap(rs, blurredBitmap, Allocation.MipmapControl.MIPMAP_FULL, Allocation.USAGE_SHARED);
            Allocation output = Allocation.createTyped(rs, input.getType());

            // Load up an instance of the specific script that we want to use.
            ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

            script.setInput(input);

            // Set the blur radius
            script.setRadius(BLUR_RADIUS);

            // Start the ScriptIntrinisicBlur
            script.forEach(output);

            // Copy the output to the blurred bitmap
            output.copyTo(blurredBitmap);

            //Recycle source bitmap
            source.recycle();
        }
        return blurredBitmap;
    }

    private static Bitmap getScaledSourceImage(Context context, Uri uri, int width, int height){
        Bitmap bitmap = null;
        try {
            //Set bitmap options to decoding just the bounds for memory allocation
            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
            }

            //Calculate the downsampling of the image based on the original and desired resolution
            int imageHeight = onlyBoundsOptions.outHeight;
            int imageWidth = onlyBoundsOptions.outWidth;
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = 1;
            while (imageHeight > height && imageWidth > width) {
                bitmapOptions.inSampleSize *= 2;
                imageHeight /= 2;
                imageWidth /= 2;
            }

            //Load the image at the downsampled resolution
            bitmapOptions.inMutable = true;

            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                Bitmap tempBitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);

                //Create scaled bitmap in case resolution does not exactly match screen size
                bitmap = Bitmap.createScaledBitmap(tempBitmap, width, height, false);

                //Recycle temp bitmap
                tempBitmap.recycle();
            }

        } catch (Exception e) {
            Log.w(TAG, "Failed to create scaled source image", e);
        }
        return bitmap;
    }
}
