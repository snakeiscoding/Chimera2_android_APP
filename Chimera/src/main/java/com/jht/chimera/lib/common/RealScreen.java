package com.jht.chimera.lib.common;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.jht.chimera.lib.common.context.ContextUtil;

public class RealScreen {
    // screen real physical size should not change, lets find it once
    // (does that add to global memory consumption and power up static processing? should we just use the static function locally?)
    public static final int heightPixels;
    public static final int widthPixels;
    public static final int densityDpi;
    static {
        /*Point screen = getRealSize();
        heightPixels = screen.y;
        widthPixels = screen.x;*/
        DisplayMetrics metrics = new DisplayMetrics();
        getRealMetrics(metrics);
        // when screen orientation is portrait these flip, we want them always as landscape dimensions
        heightPixels = Math.min(metrics.heightPixels, metrics.widthPixels);
        widthPixels = Math.max(metrics.heightPixels, metrics.widthPixels);
        densityDpi = metrics.densityDpi;
    }

    /*public static Point getRealSize() {
        Context context = ContextUtil.getApplicationContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return size;
    }*/

    public static void getRealMetrics(DisplayMetrics metrics) {
        Context context = ContextUtil.getApplicationContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getRealMetrics(metrics);
    }

}
