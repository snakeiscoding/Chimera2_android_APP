package com.jht.chimera.lib.widget.athena;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewWorkoutGraph extends View
{
    public final String TAG = "NewWorkoutGraph";
    private Paint backgroundPaint;
    private Paint majorLinePaint;
    private Paint minorLinePaint;
    private Paint verticleBarPaint;
    private Paint timeTextPaint;
    private Paint distanceMarkerPaint;
   
    private RectF graphOutline = new RectF();
    private Path drawPath = new Path();
   
    private Rect  textBounds = new Rect();

    private List <LineGroup> lineGroups = Collections.synchronizedList(new ArrayList<>());

    private String timeText = "--:--";
    private float maxScaleTimeInSeconds = 60*30f;   // seconds
    private boolean isMetric = true;
    private boolean showDistanceMarkers = true;
    private final float KMtoMILES_CONVERT_FACTOR = .6213f;
    private int lastKM = 0;
    private int lsatMile = 0;
    private List <DataPoint> kmMarkers = Collections.synchronizedList(new ArrayList<>());
    private List <DataPoint> mileMarkers = Collections.synchronizedList(new ArrayList<>());
    private String kmString = "Km"; //NON-NLS
    private String milesString = "Miles"; //NON-NLS

    class DataPoint
    {
        public float value;
        public float time;

        public DataPoint(float value, float time)
        {
            this.value = value;
            this.time = time;
        }
    }

    public  class LineGroup
    {
        private Paint linePaint;
        private Paint fillPaint;
        private Paint futurePaint;
        private Paint futureFillPaint;
        private List <DataPoint> linePoints = Collections.synchronizedList(new ArrayList<>());
        private List <DataPoint> futurePoints = Collections.synchronizedList(new ArrayList<>());
        private float minValue;
        private float maxValue;
        private float yScaleFactor;
        private boolean trackChanges = false;

        public void addData (float data, float timeInMillisecs)
        {
            DataPoint dp = new DataPoint(data-minValue, timeInMillisecs/1000);
            linePoints.add (dp);

            if (timeInMillisecs/1000 > maxScaleTimeInSeconds)
            {
                maxScaleTimeInSeconds += 300;
                setWorkoutTime ((int) maxScaleTimeInSeconds);
            }
        }

        public void addFutureData (float data, float timeInMillisecs)
        {
            DataPoint dp = new DataPoint(data-minValue, timeInMillisecs/1000);
            futurePoints.add (dp);
        }

        public void addFutureColor (int futureColor)
        {
            futurePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            futurePaint.setStrokeWidth(2);
            futurePaint.setColor(futureColor);
            futurePaint.setStyle (Paint.Style.STROKE);
            trackChanges = true;
        }

        public void addFutureColor (int futureColor, int futureFillColor)
        {
            addFutureColor(futureColor);
            futureFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            futureFillPaint.setStrokeWidth(1);
            futureFillPaint.setColor(futureFillColor);
            futureFillPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        }

        public void clearData() { linePoints.clear(); futurePoints.clear();}
        public void clearFutureData() { futurePoints.clear();}
    }

    public NewWorkoutGraph(Context context)
    {
        super(context);
        init();
    }

    public NewWorkoutGraph(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initTypedArray(context, attrs);
        init();
    }

    public NewWorkoutGraph(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    private void init()
    {
    }

    private void initTypedArray(Context context, AttributeSet attrs)
    {
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        initPaints(h);
        graphOutline.set (0, timeTextPaint.getTextSize()*1.3f, w, h);

        for (LineGroup lg : lineGroups)
        {
            lg.yScaleFactor = graphOutline.height() / (lg.maxValue-lg.minValue);
        }

        Log.d (TAG, "W,H " +  w + " " + h);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        canvas.drawPaint (backgroundPaint);

        float halfHeight = (graphOutline.bottom - graphOutline.top) / 2;
        float quarterHeight = (graphOutline.bottom - graphOutline.top) / 4;

        canvas.drawLine (graphOutline.left, graphOutline.top, graphOutline.right, graphOutline.top, majorLinePaint);
        canvas.drawLine (graphOutline.left, graphOutline.top + halfHeight, graphOutline.right, graphOutline.top + halfHeight, majorLinePaint);
        canvas.drawLine (graphOutline.left, graphOutline.bottom, graphOutline.right, graphOutline.bottom, majorLinePaint);

        canvas.drawLine (graphOutline.left, graphOutline.top + quarterHeight, graphOutline.right, graphOutline.top + quarterHeight, minorLinePaint);
        canvas.drawLine (graphOutline.left, graphOutline.bottom - quarterHeight, graphOutline.right, graphOutline.bottom - quarterHeight, minorLinePaint);

        canvas.drawText(timeText, getWidth() - textBounds.width(),  timeTextPaint.getTextSize(), timeTextPaint);

        int currentX = 0;

        // Draw each line or line set, where a set is a future line plus current data
        synchronized(lineGroups) {
            for (LineGroup lg : lineGroups) {
                if (lg.trackChanges) {
                    int size = lg.linePoints.size();
                    if (size > 3 && lg.linePoints.get(size - 1) != lg.linePoints.get(size - 2)) {
                        adjustFutureProfileToReflectUserChanges(lg);
                    }
                }
                if (lg.futurePaint != null) {
                    drawLine(canvas, lg, true);
                }
                currentX = Math.max(drawLine(canvas, lg, false), currentX);
            }
        }

        // distance markers
        if (showDistanceMarkers) {
            // How many markers do we show, it gets crowded, so start skipping the display of markers every 15 or so
            int skipMarkers = mileMarkers.size() / 15 + 1;
            if (isMetric) skipMarkers = kmMarkers.size() / 15 + 1;

            synchronized(isMetric ? kmMarkers : mileMarkers) {
                int markerCounter = 1;
                for (DataPoint dp : isMetric ? kmMarkers : mileMarkers) {
                    if (markerCounter % skipMarkers == 0) {
                        int markerX = (int) ((dp.time / maxScaleTimeInSeconds) * getWidth());
                        canvas.drawLine(markerX, graphOutline.bottom, markerX, graphOutline.top + quarterHeight, distanceMarkerPaint);
                        Log.d(TAG, "Data distance marker " + markerX);
                        float textSize = distanceMarkerPaint.getTextSize() * 1.1f;
                        canvas.drawText((int) dp.value + " " + (isMetric ? kmString : milesString), markerX, graphOutline.top + textSize, distanceMarkerPaint);
                        int hours = (int) (dp.time / 3600);
                        int minutes = (int) ((dp.time - hours * 3600) / 60);
                        int seconds = (int) (dp.time) % 60;
                        String timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                        canvas.drawText(timeText, markerX, graphOutline.top + textSize * 2, distanceMarkerPaint);
                    }
                    markerCounter++;
                }
            }
        }

        if (currentX > 0 && currentX < graphOutline.right) {
            canvas.drawLine(currentX, graphOutline.bottom, currentX, graphOutline.top, verticleBarPaint);
        }
    }

    private int drawLine (Canvas canvas, LineGroup lg, boolean isFuture)
    {
        drawPath.reset();
        boolean needToMove = true;

        float currentY;
        int currentX = 0;

        // Fill case line or future
        if ((isFuture ? lg.futureFillPaint : lg.fillPaint) != null)
        {
            drawPath.moveTo(0, getHeight());
            //Log.d (TAG, "Data moveTo " + 0 + "  y=" + getHeight());
            needToMove = false;
        }

        synchronized (isFuture ? lg.futurePoints : lg.linePoints) {
            for (DataPoint point : isFuture ? lg.futurePoints : lg.linePoints) {
                currentY = getHeight() - (point.value * lg.yScaleFactor - lg.minValue);
                currentX = (int) ((point.time / maxScaleTimeInSeconds) * getWidth());

                //Log.d (TAG, "parts " + point.value + " fac " + lg.yScaleFactor + "-" + lg.minValue);

                if (needToMove) {
                    drawPath.moveTo(currentX, getHeight());
                    //Log.d (TAG, "Data moveTo " + currentX + "  y=" + getHeight());
                    drawPath.lineTo(currentX, currentY);
                    //Log.d (TAG, "Data lineTo " + currentX + "  y=" + currentY);
                    needToMove = false;
                } else {
                    drawPath.lineTo(currentX, currentY);
                }
            }
        }

        canvas.drawPath(drawPath, isFuture ? lg.futurePaint : lg.linePaint);

        // Fill case
        if (lg.fillPaint != null)
        {
            drawPath.lineTo(currentX, getHeight());
            drawPath.close();
            canvas.drawPath(drawPath, isFuture ? lg.futureFillPaint : lg.fillPaint);
        }
        return currentX;
    }

    private void adjustFutureProfileToReflectUserChanges(LineGroup lg) {
        DataPoint p = lg.linePoints.get(lg.linePoints.size()-1);

        int size = lg.futurePoints.size();

        synchronized (lg) {
            for (int i = 0; i < size - 2; i++) {
                DataPoint fp1 = lg.futurePoints.get(i);
                DataPoint fp2 = lg.futurePoints.get(i + 1);
                if (p.time >= fp1.time && p.time <= fp2.time) {
                    if (p.value != fp1.value) {
                        lg.futurePoints.set(i, new DataPoint(p.value, p.time));
                        fp2.value = p.value;

                        Log.d(TAG, "Data Resetting future to " + fp1.value);
                        break;
                    }
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////////////
    //
    //   Color, paint, font section
    //
    /////////////////////////////////////////////////////////////////////

    private void initPaints(int canvasHeight)
    {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.parseColor("#00000000"));
        backgroundPaint.setStyle(Paint.Style.FILL);

        majorLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        majorLinePaint.setStyle(Paint.Style.STROKE);
        majorLinePaint.setStrokeWidth(2);
        majorLinePaint.setColor(Color.parseColor("#aaaaaa"));

        minorLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minorLinePaint.setStyle(Paint.Style.STROKE);
        minorLinePaint.setStrokeWidth(1);
        minorLinePaint.setColor(Color.parseColor("#aaaaaa"));

        verticleBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        verticleBarPaint.setStyle(Paint.Style.STROKE);
        verticleBarPaint.setStrokeWidth(2);
        verticleBarPaint.setColor(Color.parseColor("#ffffff"));

        timeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timeTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        timeTextPaint.setStrokeWidth(1);
        timeTextPaint.setColor(Color.parseColor("#aaaaaa"));
        timeTextPaint.setTextSize(canvasHeight/8);
        timeTextPaint.setTextAlign(Paint.Align.RIGHT);

        distanceMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        distanceMarkerPaint.setStyle(Paint.Style.STROKE);
        distanceMarkerPaint.setStrokeWidth(1);
        distanceMarkerPaint.setColor(Color.parseColor("#aaaaaa"));
        distanceMarkerPaint.setTextSize(canvasHeight/10);
        distanceMarkerPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void updateLabel (String timeLabel)
    {
        timeText = timeLabel;
        invalidate();
    }

    private float computeTextWidth (String str, Paint painter)
    {
        painter.getTextBounds (str, 0, str.length(), textBounds);
        return textBounds.width();
    }

    public void addDistanceData (float distanceInKm, float timeInSeconds)
    {
        int distanceKm = (int) distanceInKm;
        int distanceM   = (int) (distanceInKm * KMtoMILES_CONVERT_FACTOR);

        if (distanceKm > lastKM) {
            kmMarkers.add(new DataPoint(distanceInKm, timeInSeconds/1000));
            lastKM = distanceKm;
            Log.d (TAG, "Data new KM marker " + lastKM);
        }

        if (distanceM > lsatMile) {
            mileMarkers.add (new DataPoint(distanceInKm* KMtoMILES_CONVERT_FACTOR, timeInSeconds/1000));
            lsatMile = distanceM;
            Log.d (TAG, "Data new MI marker " + lsatMile);
        }
    }

    public LineGroup  addLineType (float scaleMin, float scaleMax, int lineColor, int fillColor)
    {
        LineGroup lg = new LineGroup();

        lg.linePaint = new Paint (Paint.ANTI_ALIAS_FLAG);
        lg.linePaint.setStrokeWidth(2);
        lg.linePaint.setColor(lineColor);
        lg.linePaint.setStyle (Paint.Style.STROKE);

        lg.fillPaint = new Paint (Paint.ANTI_ALIAS_FLAG);
        lg.fillPaint.setStrokeWidth(1);
        lg.fillPaint.setColor(fillColor);
        lg.fillPaint.setStyle (Paint.Style.FILL_AND_STROKE);

        lg.maxValue = scaleMax;
        lg.minValue = scaleMin;

        lg.yScaleFactor = graphOutline.height() / (scaleMax-scaleMin);

        synchronized(lineGroups) {
            lineGroups.add(lg);
        }
        return lg;
    }

    public LineGroup addLineType (float scaleMin, float scaleMax, int lineColor)
    {
        LineGroup lg = new LineGroup();

        lg.linePaint = new Paint (Paint.ANTI_ALIAS_FLAG);
        lg.linePaint.setStrokeWidth(2);
        lg.linePaint.setColor(lineColor);
        lg.linePaint.setStyle (Paint.Style.STROKE);

        lg.maxValue = scaleMax;
        lg.minValue = scaleMin;

        lg.yScaleFactor = graphOutline.height() / (scaleMax-scaleMin);
        synchronized(lineGroups) {
            lineGroups.add(lg);
        }
        return lg;
    }

    public void setWorkoutTime (int seconds)
    {
        maxScaleTimeInSeconds = seconds;
        timeText = String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    public void updateScreen()
    {
       invalidate();
    }

    public void setAsMetric (boolean metricFlag)
    {
        isMetric = metricFlag;
    }

    public void setDistanceMarkerOn (boolean distanceMarkerFlag)
    {
        this.showDistanceMarkers = distanceMarkerFlag;
    }

    public void setDistanceStrings (String kmstring, String milesString)
    {
        this.kmString = kmstring;
        this.milesString = milesString;
    }
}
