package com.jht.chimera.ui.mainapp.goscreen;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.jht.chimera.R;

/**
 * Created by AlexN on 12/30/2018.
 */

public class DiagonalView extends View {

    private final static int DIAGONAL_STROKE_WIDTH = 4;

    private int width;
    private int height;
    private Path path;
    private Path borderPath;

    private Paint paint;
    private Paint solidBackgroundPaint;
    private Paint backgroundPaint;
    private Paint borderPaint;
    int degree;

    public DiagonalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.FeaturedDiagonal,
                0, 0);

        degree = a.getInt(R.styleable.FeaturedDiagonal_degree, 0);
        degree = Math.max(0, degree);
        degree = Math.min(180, degree);
        init();
    }

    private void init(){
        paint = new Paint();

        backgroundPaint = new Paint();
        backgroundPaint.setColor(0x4D000000);
        backgroundPaint.setAntiAlias(true);

        solidBackgroundPaint = new Paint();
        solidBackgroundPaint.setColor(Color.BLACK);
        solidBackgroundPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setStrokeWidth(DIAGONAL_STROKE_WIDTH);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(0x33FFFFFF);
        borderPaint.setAntiAlias(true);

        path = new Path();
        borderPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        Bitmap greyBackground = BitmapLoader.decodeSampledBitmapResource(getResources(), R.drawable.background_gray, w, h);
        BitmapShader backgroundPattern = new BitmapShader(greyBackground, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        paint.setAntiAlias(true);
        paint.setShader(backgroundPattern);
        setPath();
    }

    public void setPath() {
        path.reset();
        path.moveTo(0, height);
        if(degree <90){
            path.lineTo(0, (float) (Math.atan(Math.toRadians(degree))*width));
            path.lineTo( width, DIAGONAL_STROKE_WIDTH/2);
        }
        else{
            path.lineTo(0, 0);
            path.lineTo( width, (float) (Math.atan(Math.toRadians(180-degree))*width));
        }
        path.lineTo(width, height);
        path.lineTo(0, height);
        path.close(); 

        borderPath.reset();
        if(degree < 90){
            borderPath.moveTo(width, DIAGONAL_STROKE_WIDTH/2);
            borderPath.lineTo(0, (float) (Math.atan(Math.toRadians(degree))*width));
            borderPath.lineTo(width, DIAGONAL_STROKE_WIDTH/2);
        }
        else{
            borderPath.moveTo(0, DIAGONAL_STROKE_WIDTH/2);
            borderPath.lineTo(width, (float) (Math.atan(Math.toRadians(180-degree))*width));
            borderPath.lineTo(0, DIAGONAL_STROKE_WIDTH/2);
        }
        borderPath.close();
}

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, solidBackgroundPaint);
        canvas.drawPath(path, backgroundPaint);
        canvas.drawPath(path, paint);
        canvas.drawPath(borderPath, borderPaint);
    }

}
