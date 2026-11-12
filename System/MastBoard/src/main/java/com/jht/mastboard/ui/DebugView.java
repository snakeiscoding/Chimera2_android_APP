package com.jht.mastboard.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DebugView extends View {
    Bitmap mBitmap;

    public DebugView(Context context) {
        super(context);
    }

    public DebugView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DebugView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap == null) return;
        canvas.drawBitmap(mBitmap, 0, 0, new Paint());
    }
}