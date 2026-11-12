package com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.jht.chimera.R;

/**
 * Created by AlexN on 12/30/2018.
 */

public class FeaturedDiagonal extends ImageView {

    private final static int DIAGONAL_STROKE_WIDTH = 4;

    private int width;
    private int height;
    private Path path;
    private Path borderPath;
    private Path shieldPath;

    private Paint paint;
    private Paint shieldPaint;
    private Paint backgroundPaint;
    private Paint borderPaint;
    int degree;

    Bitmap bitmap;
    Rect src;
    Rect dest;

    public FeaturedDiagonal(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.FeaturedDiagonal,
                0, 0);

        degree = a.getInt(R.styleable.FeaturedDiagonal_degree, 0);
        degree = Math.max(0, degree);
        degree = Math.min(180, degree);

        //Porter Duff filters don't work with hardware acceleration
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        shieldPaint = new Paint();
        shieldPaint.setAntiAlias(true);


        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setStrokeWidth(DIAGONAL_STROKE_WIDTH);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(0xBFFFFFFF);
        borderPaint.setAntiAlias(true);

        path = new Path();
        borderPath = new Path();
        shieldPath = new Path();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        bitmap = bm;
        setScale();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        setScale();
        shieldPaint.setShader(new LinearGradient(0, 0, 0, height, 0x91000000, 0xE6000000, Shader.TileMode.MIRROR));
        setPaths();
    }

    private void setScale(){
        if(width > 0 && height > 0 && bitmap != null) {
            float scale = Math.max(width * 1f / bitmap.getWidth(), height * 1f / bitmap.getHeight());
            src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            dest = new Rect(0, 0, (int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale));
        }
    }

    public void setPaths() {
        //Background path
        path.reset();
        path.moveTo(width, 0);
        path.lineTo(width, height - (float) (Math.atan(Math.toRadians(degree)) * width));
        path.lineTo(0, height - DIAGONAL_STROKE_WIDTH / 2);
        path.lineTo(0, 0);
        path.lineTo(width, 0);
        path.close();

        //Border line path
        borderPath.reset();
        borderPath.moveTo(0, height - DIAGONAL_STROKE_WIDTH / 2);
        borderPath.lineTo(width, height - (float) (Math.atan(Math.toRadians(degree)) * width));
        borderPath.lineTo(0, height - DIAGONAL_STROKE_WIDTH / 2);
        borderPath.close();

        //Shield Path
        shieldPath.reset();
        shieldPath.moveTo(width, 0);
        shieldPath.lineTo(width, height - (float) (Math.atan(Math.toRadians(degree)) * width));
        shieldPath.lineTo(width * 0.476f, height - ((float) (Math.atan(Math.toRadians(degree)) * width * 0.476f)));
        shieldPath.lineTo(width * 0.608f, 0);
        shieldPath.lineTo(width, 0);
        shieldPath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(bitmap != null) {
            canvas.drawPath(path, backgroundPaint);
            canvas.drawBitmap(bitmap, src, dest, paint);
            canvas.drawPath(shieldPath, shieldPaint);
            canvas.drawPath(borderPath, borderPaint);
        }
    }

}
