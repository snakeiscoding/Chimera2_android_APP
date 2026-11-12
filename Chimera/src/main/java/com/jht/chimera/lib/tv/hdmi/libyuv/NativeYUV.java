package com.jht.chimera.lib.tv.hdmi.libyuv;

import android.view.Surface;

public class NativeYUV {

    static {
        System.loadLibrary("nativeyuv");
    }

    public native void yuv2rgb(byte[] nv21, int width, int height, Surface surface);
}
