package com.jht.chimera.lib.widget.selectron;/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

/* This class was masterfully pilfered, Carmen-Sandiego style, from our awesome sample app
 * ApiDemos.  To see how it looks as a dialog, check out:
 * http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/graphics/ColorPickerDialog.html
 *
 */
public class ColorWheel extends View {
    public interface OnColorChangedListener {
        void colorChanged(int color);
    }
    /*
    */
    private OnColorChangedListener mListener;
    private int mInitialColor;
    private Paint mPaint;
    private Paint mPaint2;
    private Paint mCenterPaint;

    private final int[] mColors;
    private final int[] mColors2;

    private float alpha = 0xff;

    public void setOnColorChangedListener(OnColorChangedListener l) {
        mListener = l;
    }
    public ColorWheel(Context c) {
        super(c);
        mColors = new int[] {
                0xFFFFFFFF, 0xFFFFFF00, 0xFFFF0000, 0xFF000000,
                0xFF00FF00, 0xFF00FFFF, 0xFFFFFFFF, 0xFFFF00FF,
                0xFF0000FF, 0xFF000000, 0xFFFFFFFF
        };
        mColors2 = new int[] {
                0x00FFFFFF, 0xFFFFFFFF
        };
        Shader s = new SweepGradient(0, 0, mColors, null);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setShader(s);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(100);
        mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCenterPaint.setColor(0);
        mCenterPaint.setStrokeWidth(5);
        Shader s2 = new LinearGradient(0, -CENTER_X, 0, CENTER_X, mColors2, null, Shader.TileMode.MIRROR);
        mPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint2.setShader(s2);
        mPaint2.setStyle(Paint.Style.FILL);

        ProgressBar progressBar = new ProgressBar(c);
        progressBar.setMax(255);
        progressBar.setVisibility(VISIBLE);

    }
    private boolean mTrackingCenter;
    private boolean mHighlightCenter;
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float r = CENTER_X - mPaint.getStrokeWidth()*0.5f;
        canvas.translate(100, 100);
        canvas.drawOval(new RectF(-r, -r, r, r), mPaint);

        canvas.drawRect(CENTER_X + 5, -CENTER_Y, CENTER_X * 1.5f + 5,
                CENTER_Y,
                mPaint2);

        if (mTrackingCenter) {
            int c = mCenterPaint.getColor();
            mCenterPaint.setStyle(Paint.Style.STROKE);
            if (mHighlightCenter) {
                mCenterPaint.setAlpha(0xFF);
            } else {
                mCenterPaint.setAlpha(0x80);
            }

            mCenterPaint.setStyle(Paint.Style.FILL);
            mCenterPaint.setColor(c);
        }
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(CENTER_X*4, CENTER_Y*2);
    }
    private static final int CENTER_X = 100;
    private static final int CENTER_Y = 100;
    private static final int CENTER_RADIUS = 0;

    private int floatToByte(float x) {
        int n = Math.round(x);
        return n;
    }

    private int pinToByte(int n) {
        if (n < 0) {
            n = 0;
        } else if (n > 255) {
            n = 255;
        }
        return n;
    }

    private int ave(int s, int d, float p) {
        return s + Math.round(p * (d - s));
    }

    private int interpColor(int colors[], float unit) {
        if (unit <= 0) {
            return colors[0];
        }
        if (unit >= 1) {
            return colors[colors.length - 1];
        }
        float p = unit * (colors.length - 1);
        int i = (int)p;
        p -= i;
        // now p is just the fractional part [0...1) and i is the index
        int c0 = colors[i];
        int c1 = colors[i+1];
        int a = (int)alpha;
        int r = ave(Color.red(c0), Color.red(c1), p);
        int g = ave(Color.green(c0), Color.green(c1), p);
        int b = ave(Color.blue(c0), Color.blue(c1), p);
        return Color.argb(a, r, g, b);
    }

    private int rotateColor(int color, float rad) {
        float deg = rad * 180 / 3.1415927f;
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        ColorMatrix cm = new ColorMatrix();
        ColorMatrix tmp = new ColorMatrix();
        cm.setRGB2YUV();
        tmp.setRotate(0, deg);
        cm.postConcat(tmp);
        tmp.setYUV2RGB();
        cm.postConcat(tmp);
        final float[] a = cm.getArray();
        int ir = floatToByte(a[0] * r +  a[1] * g +  a[2] * b);
        int ig = floatToByte(a[5] * r +  a[6] * g +  a[7] * b);
        int ib = floatToByte(a[10] * r + a[11] * g + a[12] * b);
        return Color.argb(Color.alpha(color), pinToByte(ir),
                pinToByte(ig), pinToByte(ib));
    }


    private static final float PI = 3.1415926f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() - CENTER_X;
        float y = event.getY() - CENTER_Y;
        boolean inCenter = Math.sqrt(x*x + y*y) <= CENTER_RADIUS;
        boolean inCircle = Math.sqrt(x*x + y*y) <= CENTER_X;
        boolean inAlpha = (x >= CENTER_X*1.25);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTrackingCenter = inCenter;
                if (inCenter) {
                    mHighlightCenter = true;
                    invalidate();
                    break;
                }
            case MotionEvent.ACTION_MOVE:
                if (mTrackingCenter) {
                    if (mHighlightCenter != inCenter) {
                        mHighlightCenter = inCenter;
                        invalidate();
                    }
                }
                else if (inCircle){
                    float angle = (float) Math.atan2(y, x);
                    // need to turn angle [-PI ... PI] into unit [0....1]
                    float unit = angle/(2*PI);
                    if (unit < 0) {
                        unit += 1;
                    }
                    mCenterPaint.setColor(interpColor(mColors, unit));
                    invalidate();
                    mListener.colorChanged(mCenterPaint.getColor());
                }
                else if (inAlpha){
                    alpha = 255 * (y+100) / (CENTER_X*2);
                    mCenterPaint.setAlpha((int) alpha);
                    invalidate();
                    mListener.colorChanged(mCenterPaint.getColor());
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mTrackingCenter) {
                    if (inCenter) {
                        mListener.colorChanged(mCenterPaint.getColor());
                    }
                    mListener.colorChanged(0);
                    // mListener.colorChanged(mCenterPaint.getColor());
                    mTrackingCenter = false;    // so we draw w/o halo
                    invalidate();
                }
                break;
        }
        return true;
    }
}