package com.jht.chimera.lib.widget.track;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class TrackWidget extends View
{
    static final float PAD_PERCENT = 0.1f;
    private Paint paint;
    private Paint dotPaint;
    private Paint linePaint;
    private int w;
    private int h;
    private float angle = 0;
    private int lapIndex = 0;
    private Path path;
    private float markerX;
    private float markerY1;
    private float markerY2;

    public TrackWidget(Context context)
    {
        super(context);
        init();
    }

    public TrackWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    private void init(){
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0x66FFFFFF);
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(0xFFFFDB00);
        dotPaint.setTextAlign(Paint.Align.CENTER);
        dotPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3);
        linePaint.setColor(0xFFFFDB00);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        this.w = w;
        this.h = h;

        path = new Path();
        RectF rightRect = new RectF((int)(w/2f + (w*(1-2* PAD_PERCENT)) * 0.0f), (int)(h* PAD_PERCENT), (int)(w*(1- PAD_PERCENT)), (int)(h*(1- PAD_PERCENT)));
        RectF leftRect = new RectF((int)(w*(PAD_PERCENT)), (int)(h* PAD_PERCENT), (int)(w/2f - (w*(1-2* PAD_PERCENT)) * 0.0f), (int)(h*(1- PAD_PERCENT)));

        path.arcTo(rightRect, 90, -180 );
        path.arcTo(leftRect, 270, -180 );
        path.close();
        paint.setStrokeWidth(h/10f);
        dotPaint.setTextSize(h/3f);
        markerX = w/2f;
        markerY1 = h;
        markerY2 = markerY1 - 2*h/10f;
    }

    float x;
    float y;
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        setCircleCoords();
        canvas.drawPath(path, paint);
        canvas.drawCircle(x, y, h/10f, dotPaint);
        canvas.drawLine(markerX, markerY1, markerX, markerY2, linePaint);
         if(lapIndex >=0) {
            canvas.drawText((lapIndex + ""), w / 2f, h * 3 / 5f, dotPaint);
        }
    }

    private void setCircleCoords(){
        //Straight edge bottom of the track
        if(angle < 45){
            x = w/2f + angle/45f * w*(1-2* PAD_PERCENT)/4f;
            y = h*(1- PAD_PERCENT);
        }
        //Round the first curve
        else if (angle>=45 && angle < 135){
            x = (float) (w/2f + w*(1-2* PAD_PERCENT)/4f+ w*(1-2* PAD_PERCENT)/4f*Math.cos(Math.toRadians((angle-45)*2+270)));
            y = (float) (h*(1-2* PAD_PERCENT)/2f * Math.sin(Math.toRadians((angle-45)*2+90))) + h/2f;
        }
        //Straight edge top of the track
        else if (angle >= 135 && angle <225){
            x = w/2f + w*(1-2* PAD_PERCENT)/4 - (angle-135)/90f * w*(1-2* PAD_PERCENT)/2;
            y = h* PAD_PERCENT;
        }
        //Round the second curve
        else if (angle >= 225 && angle <315){
            x = (float) (w/2f - w*(1-2* PAD_PERCENT)/4 + w*(1-2* PAD_PERCENT)/4f*Math.cos(Math.toRadians((angle-225)*2+90)));
            y = (float) (h*(1-2* PAD_PERCENT)/2f * Math.sin(Math.toRadians((angle-225)*2+270))) + h/2f;
        }
        //Back to start
        else if (angle >= 315 && angle <360){
            x = w/2f - w*(1-2* PAD_PERCENT)/4 + (angle-315)/45f * w*(1-2* PAD_PERCENT)/4f;
            y = h*(1- PAD_PERCENT);
        }
    }

    public void setProgress(float progress){
        angle = progress/100 * 360;
        invalidate();
    }
    public void setLapIndex(int index){
        lapIndex = index;
        invalidate();
    }

    public void setCoolDownColor(){
        dotPaint.setColor(0xff0071bc);
        linePaint.setColor(0xff0071bc);
    }

    public void setNormalColor() {
        dotPaint.setColor(0xFFFFDB00);
        linePaint.setColor(0xFFFFDB00);
    }
  }

