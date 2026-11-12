package com.jht.chimera.lib.widget.dial;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.jht.chimera.R;

import java.util.ArrayList;

public class MultiZoneCircleGauge extends View {
    private static final String TAG = "MultiZoneCircleGauge";
    final private int MINIMUM_SIZE = 100;
    int m_X;
    int m_Y;
    int m_Width;
    int m_Height;
    int m_SmallerDimension;  // either height or width
    int m_StrokeWidth = 8;
    int gaugeBandStrokeWidth = 3;
    int strokeOffset;
    int blurLineWidth = 3;
    private RectF mBackgroundOvalRectF;
    private RectF mInnerArcRectF;
    private RectF mOuterArcRectF;
    private RectF mOuterLineBlurRectF;
    private RectF mInnerLineBlurRectF;
    private ObjectAnimator objAnimator;
    private float maxGaugeValue = 100;
    private float minGaugeValue = 0;
    private float currentGaugeValue = 60.0f;
    //private int currentGaugeValue = 60;
    private String currentMiddleValue = "";
    private String currentMiddleUnit = "";
    private Paint backgroundCirclePaint;
    private Paint backgroundCircleDetailPaint;
    private Paint edgeNeedlePaint;
    private Paint hardLinePaint;
    private Paint mTextPaint;
    private Paint mTextPaintMid;
    private Paint mTextPaintBigValue;
    private Paint mTextPaintBigUnits;
    private Paint circleBackgroundPaint;
    private float radius;
    private float fontSize = 16;
    private float fontSizeLargeValue = getResources().getDimension(R.dimen.constant_watts_font_size_large_value);
    private float fontSizeLargeUnits = getResources().getDimension(R.dimen.constant_watts_font_size_large_units);
    private float outsidePadding;
    private boolean showLargeText = false;
    private boolean showBottomText = false;
    private boolean currentValueSameAsBottomValue = true;
    private String rangeUnitsString = "";
    private int visibleZoneCount = 0;
    private float bottomGaugeValue = 0;

    class ZoneColorConfig {
        private Paint hardLinePaint;
        private Paint lineBlurPaint;
        private Paint middleBlurPaint;
        private float startValue;
        private float endValue;
        private String name;
        private boolean isVisible = false;

        private ZoneColorConfig (String name, float startValue, float endValue, String hardlineColor, String lineBlurColor, String middleBlurColor, boolean isVisible) {
            this.name = name;
            this.startValue = startValue;
            this.endValue = endValue;
            this.isVisible = isVisible;
            hardLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            hardLinePaint.setColor(Color.parseColor(hardlineColor));
            hardLinePaint.setStyle(Paint.Style.STROKE);
            hardLinePaint.setStrokeWidth(3);
            //hardLinePaint.setMaskFilter(new BlurMaskFilter(2, BlurMaskFilter.Blur.NORMAL));
            hardLinePaint.setDither(true);
            // Note:  blurring may or may not work depending on OS/Hardware integration
            lineBlurPaint = new Paint();
            lineBlurPaint.setColor(Color.parseColor(lineBlurColor));
            //lineBlurPaint.setColor(Color.parseColor("#ffffff"));//White
            lineBlurPaint.setStyle(Paint.Style.STROKE);
            lineBlurPaint.setStrokeWidth(blurLineWidth+7);
            lineBlurPaint.setDither(true);
            // Note:  blurring may or may not work depending on OS/Hardware integration
            lineBlurPaint.setMaskFilter(new BlurMaskFilter(blurLineWidth+7, BlurMaskFilter.Blur.OUTER));
            middleBlurPaint = new Paint ();
            middleBlurPaint.setColor (Color.parseColor(middleBlurColor));
            middleBlurPaint.setStyle(Paint.Style.STROKE);
            //middleBlurPaint.setMaskFilter(new BlurMaskFilter(gaugeBandStrokeWidth*2, BlurMaskFilter.Blur.NORMAL));
            middleBlurPaint.setMaskFilter(new BlurMaskFilter(gaugeBandStrokeWidth, BlurMaskFilter.Blur.OUTER));
            middleBlurPaint.setStrokeWidth (gaugeBandStrokeWidth);
        }
    }

    public void addZone (float startValue, float endValue, String hardlineColor, String lineBlurColor, String middleBlurColor) {
        ZoneColorConfig zoneConfig = new ZoneColorConfig ("",startValue, endValue, hardlineColor, lineBlurColor, middleBlurColor, true);
        zoneConfigs.add (zoneConfig);
    }

    public void createZone(String name, float startValue, float endValue, String hardlineColor, String lineBlurColor, String middleBlurColor, boolean isVisible) {
        Log.d (TAG, "DIAL createZone=" + name + " new =" + startValue + " " + endValue);
        ZoneColorConfig zoneConfig = new ZoneColorConfig (name, startValue, endValue, hardlineColor, lineBlurColor, middleBlurColor, isVisible);
        zoneConfigs.add (zoneConfig);
    }

    public void setBackgroundCircleColor(String color) {
        if (circleBackgroundPaint == null) {
            circleBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
        circleBackgroundPaint.setColor(Color.parseColor(color));
        circleBackgroundPaint.setStyle(Paint.Style.FILL);
    }

    private ArrayList<ZoneColorConfig> zoneConfigs = new ArrayList<>();

    public MultiZoneCircleGauge(Context context) {
        super(context);
        init();
    }

    public MultiZoneCircleGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        //initTypedArray(context, attrs);
        init();
    }

    //    private void initTypedArray(Context context, AttributeSet attrs) { }

    public MultiZoneCircleGauge(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mBackgroundOvalRectF = new RectF();
        mInnerArcRectF = new RectF();
        mOuterArcRectF = new RectF();
        mInnerLineBlurRectF = new RectF();
        mOuterLineBlurRectF = new RectF();
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        initPaints();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        m_Height = Math.max (MINIMUM_SIZE, heightSize);
        m_Width = Math.max (MINIMUM_SIZE, widthSize);
        m_SmallerDimension = Math.min(m_Width, m_Height);
        //m_StrokeWidth = m_SmallerDimension / 12;
        gaugeBandStrokeWidth = m_SmallerDimension / 12;
        backgroundCirclePaint.setStrokeWidth(gaugeBandStrokeWidth);
        backgroundCircleDetailPaint.setStrokeWidth(gaugeBandStrokeWidth/4);
        strokeOffset = gaugeBandStrokeWidth / 2;
        blurLineWidth = gaugeBandStrokeWidth / 18;
        // Create a 5% padding around the circle
        outsidePadding = m_SmallerDimension / 13.0f;
        setMeasuredDimension (m_SmallerDimension, m_SmallerDimension);
        m_X = 0 + m_StrokeWidth / 2;
        m_Y = 0 + m_StrokeWidth / 2;
        int right = m_SmallerDimension - m_StrokeWidth / 2;
        int bottom = m_SmallerDimension - m_StrokeWidth / 2;

        mBackgroundOvalRectF.set(m_X + outsidePadding, m_Y + outsidePadding, right - outsidePadding, bottom - outsidePadding);

        mOuterArcRectF.set(mBackgroundOvalRectF.left - strokeOffset, mBackgroundOvalRectF.top - strokeOffset,
                mBackgroundOvalRectF.right + strokeOffset, mBackgroundOvalRectF.bottom + strokeOffset);

        mOuterLineBlurRectF.set(mBackgroundOvalRectF.left - strokeOffset , mBackgroundOvalRectF.top - strokeOffset,
                mBackgroundOvalRectF.right + strokeOffset, mBackgroundOvalRectF.bottom + strokeOffset );

        mInnerArcRectF.set(mBackgroundOvalRectF.left + strokeOffset, mBackgroundOvalRectF.top + strokeOffset,
                mBackgroundOvalRectF.right - strokeOffset, mBackgroundOvalRectF.bottom - strokeOffset);

        mInnerLineBlurRectF.set(mBackgroundOvalRectF.left + strokeOffset , mBackgroundOvalRectF.top + strokeOffset ,
                mBackgroundOvalRectF.right - strokeOffset , mBackgroundOvalRectF.bottom - strokeOffset );

        radius = (m_SmallerDimension - outsidePadding)/2f;
        // Recalculate the Pen stroke widths
        for (ZoneColorConfig zcc : zoneConfigs) {
            zcc.lineBlurPaint.setStrokeWidth(blurLineWidth);
            zcc.middleBlurPaint.setStrokeWidth(gaugeBandStrokeWidth);
        }
        fontSize = m_SmallerDimension/20;
        mTextPaint.setTextSize(fontSize);
        recalcVisibleZoneCount();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        //drawFilledArc(canvas);
        //canvas.drawCircle (m_SmallerDimension/2, m_SmallerDimension/2, m_SmallerDimension/2, radialPaint1);
        if (visibleZoneCount > 1) {
            for (ZoneColorConfig zcc : zoneConfigs) {
                drawFilledArc(canvas, zcc);
            }
        }
        else if (visibleZoneCount == 1 || zoneConfigs.size() == 1) {
            try {
                ZoneColorConfig zcc = zoneConfigs.get(0);
                zcc.endValue = getGaugeValue();
                zcc.startValue = minGaugeValue;
                drawFilledArc (canvas, zcc);
            } catch (Exception e){Log.e(TAG,"Error", e);}
        }
        float angle = calculateArcAngleFromValue (getGaugeValue());
        drawNeedle (canvas, angle, hardLinePaint);
        drawNeedle (canvas, 120 - 1, edgeNeedlePaint);
        drawNeedle (canvas, 60 + 1, edgeNeedlePaint);
        drawRangeNumbers (canvas, minGaugeValue, maxGaugeValue);
    }

    private void drawBackground (Canvas canvas) {
        if (circleBackgroundPaint != null) {
            canvas.drawOval(mBackgroundOvalRectF, circleBackgroundPaint);
        }
        canvas.drawOval(mBackgroundOvalRectF, backgroundCirclePaint);
        canvas.drawArc(mBackgroundOvalRectF, 60F, 60F, false, backgroundCircleDetailPaint);
        //canvas.drawOval(mBackgroundOvalRectF, backgroundCircleDetailPaint);
    }

    private float calculateArcAngleFromValue (float value) {
        value = Math.min (value, maxGaugeValue);
        value = Math.max (value, minGaugeValue);
        float percentArc = (value - minGaugeValue) / (maxGaugeValue - minGaugeValue);
        float angle = (percentArc) * 300 + 120;
        return angle;
    }

    private void drawFilledArc (Canvas canvas, ZoneColorConfig zcc) {
        // We span 300 degrees on the gauge, starting angle value is at 120 degrees
        float minStart = Math.max (zcc.startValue, minGaugeValue);
        float maxEnd = Math.min (zcc.endValue, maxGaugeValue);
        if (maxEnd < minGaugeValue) maxEnd = minGaugeValue;
        if (minStart > maxGaugeValue) minStart = maxGaugeValue;
        float percentArcEnd = (maxEnd - minGaugeValue) / (maxGaugeValue - minGaugeValue);
        float percentArcBegin = (minStart - minGaugeValue) / (maxGaugeValue - minGaugeValue);
        float startAngle = percentArcBegin * 300 + 120;
        float endAngle = (percentArcEnd-percentArcBegin) * 300;
//        Log.d (TAG, "GaugeRange = " + minGaugeValue + "-" + maxGaugeValue);
//        Log.d (TAG, "Values = " + zcc.startValue + "-" + zcc.endValue);
//        Log.d (TAG, "Angles = " + startAngle + "-" + endAngle);
        canvas.drawArc (mInnerArcRectF, startAngle, endAngle, false, zcc.hardLinePaint);
        canvas.drawArc (mInnerLineBlurRectF, startAngle, endAngle, false, zcc.lineBlurPaint);
        // Draw the outer line and then the blur line to the inside
        canvas.drawArc (mOuterArcRectF, startAngle, endAngle, false, zcc.hardLinePaint);
        canvas.drawArc (mOuterLineBlurRectF, startAngle, endAngle, false, zcc.lineBlurPaint);
        // Draw the blurry middle of the arc
        canvas.drawArc (mBackgroundOvalRectF, startAngle, endAngle, false, zcc.middleBlurPaint);
        drawZoneEdge (canvas, startAngle, zcc);
        drawZoneEdge (canvas, endAngle + startAngle, zcc);
    }

    private void drawZoneEdge (Canvas canvas, float angle, ZoneColorConfig zcc) {
        double tdeg = angle * (Math.PI/180d);
        float radiusBegin = mInnerArcRectF.width() - mInnerArcRectF.centerX() + mInnerArcRectF.top;
        float radiusEnd = mOuterArcRectF.width() - mOuterArcRectF.centerX() + mOuterArcRectF.top;
        //radiusBegin -= tickExtension;
        //radiusEnd += tickExtension;
        double beginx = radiusBegin * Math.cos (tdeg) + mInnerArcRectF.centerX();
        double endx = radiusEnd * Math.cos (tdeg) + mOuterArcRectF.centerX();
        double beginy = radiusBegin * Math.sin (tdeg) + mInnerArcRectF.centerY();
        double endy = radiusEnd * Math.sin (tdeg) + mOuterArcRectF.centerY();
        //Log.d ("wattsGauge", "Angle=" + angle + " Beginx=" + beginx + " Beginy=" + beginy + " Endx=" + endx + " Endy=" + endy);
        canvas.drawLine ((float)beginx, (float)beginy, (float)endx, (float)endy, zcc.hardLinePaint);
        canvas.drawLine ((float)beginx, (float)beginy, (float)endx, (float)endy, zcc.lineBlurPaint);
    }

    private void drawNeedle(Canvas canvas, float angle, Paint paint) {
        double tdeg = (angle) * (Math.PI/180d);
        double tdeg1 = (angle - .5) * (Math.PI/180d);
        double tdeg2 = (angle + .5) * (Math.PI/180d);
        float radiusEnd = mInnerArcRectF.width() - mInnerArcRectF.centerX() + mInnerArcRectF.top - gaugeBandStrokeWidth*.25f;
        float radiusBegin = mOuterArcRectF.width() - mOuterArcRectF.centerX()  + gaugeBandStrokeWidth* 1.0f;
        double beginx = radiusBegin * Math.cos (tdeg) + mOuterArcRectF.centerX();
        double endx = radiusEnd * Math.cos (tdeg1) + mOuterArcRectF.centerX();
        double beginy = radiusBegin * Math.sin (tdeg) + mOuterArcRectF.centerY();
        double endy = radiusEnd * Math.sin (tdeg1) + mOuterArcRectF.centerY();
        double endx1 = radiusEnd * Math.cos (tdeg2) + mOuterArcRectF.centerX();
        double endy1 = radiusEnd * Math.sin (tdeg2) + mOuterArcRectF.centerY();
        Path trianglePath = new Path();
        trianglePath.setFillType(Path.FillType.EVEN_ODD);
        trianglePath.moveTo((float)beginx, (float)beginy);
        trianglePath.lineTo((float)endx, (float)endy);
        trianglePath.lineTo((float)endx1, (float)endy1);
        trianglePath.close();
        canvas.drawPath (trianglePath, paint);
    }

    public void setVisibility (String name, boolean isVisible) {
        for (ZoneColorConfig zcc : zoneConfigs) {
            if (zcc.name.compareTo(name) == 0) {
                Log.d (TAG, "DIAL showZone=" + name + " " + isVisible);
                zcc.isVisible = isVisible;
                recalcVisibleZoneCount();
                return;
            }
        }
    }

    private void recalcVisibleZoneCount () {
        visibleZoneCount = 0;
        for (ZoneColorConfig zcc : zoneConfigs) {
            if (zcc.isVisible) visibleZoneCount++;
        }
    }

    public void setZoneRange (String name, float minValue, float maxValue) {
        for (ZoneColorConfig zcc : zoneConfigs) {
            if (zcc.name.compareTo(name) == 0) {
                Log.d (TAG, "DIAL setZoneRange=" + name + " (" + minValue + "-" + maxValue + ")");
                zcc.startValue = minValue;
                zcc.endValue = maxValue;
                return;
            }
        }
    }

    private void drawRangeNumbers (Canvas canvas, float beginValue, float endValue) {
        final float mStartingAngle = 120f;
        final float mEndingAngle = 420;
        float angleMin = (mEndingAngle-mStartingAngle) * (beginValue - minGaugeValue) / (maxGaugeValue - minGaugeValue);
        float angleMax = (mEndingAngle-mStartingAngle) * (endValue - minGaugeValue) / (maxGaugeValue - minGaugeValue);
        if (angleMin < 0 || angleMax < 0) return;
        double degRadMin = (angleMin + mStartingAngle) * (Math.PI / 180d);
        double degRadMax = (angleMax + mStartingAngle) * (Math.PI / 180d);
        String valueStr = "" + (int) beginValue;
        Rect fontRect = new Rect();
        mTextPaint.getTextBounds(valueStr, 0, valueStr.length(), fontRect);
        double endx = radius * Math.cos (degRadMin)+ canvas.getWidth() / 2 - fontRect.width();
        double endy = radius * Math.sin (degRadMin) + canvas.getHeight() / 2f + fontRect.height() * 1.5;
        double centerBottomPositionY = radius * Math.sin (degRadMin) + canvas.getHeight() / 2f + (fontRect.height()/4);
        canvas.drawText(valueStr, (float) endx , (float) endy, mTextPaint);
        valueStr = "" + (int) endValue;
        mTextPaint.getTextBounds(valueStr, 0, valueStr.length(), fontRect);
        endx = radius * Math.cos (degRadMax) + canvas.getWidth() / 1.85 + fontRect.width();
        endy = radius * Math.sin (degRadMax) + canvas.getHeight() / 2f + fontRect.height() * 1.5;
        canvas.drawText((int) endValue + "", (float) endx, (float) endy, mTextPaint);
        if ( showLargeText ){
            if (currentMiddleValue.length() >= 7){
                mTextPaintBigValue.setTextSize(fontSizeLargeValue*9/10);
            } else {
                mTextPaintBigValue.setTextSize(fontSizeLargeValue);
            }
            canvas.drawText(currentMiddleValue + "", canvas.getWidth() / 2, canvas.getHeight() / 2 + outsidePadding/2, mTextPaintBigValue);
            canvas.drawText(currentMiddleUnit + "", canvas.getWidth() / 2, canvas.getHeight() / 2 + outsidePadding/2 + mTextPaintBigUnits.getTextSize() + 10,  mTextPaintBigUnits);
        }
        if ( showBottomText ){
            int bottomValue = 0;
            if (currentValueSameAsBottomValue) {
                bottomValue = (int) currentGaugeValue;
            } else {
                bottomValue = (int) bottomGaugeValue;
            }
            canvas.drawText((int) bottomValue + "", canvas.getWidth() / 2, (float)centerBottomPositionY, mTextPaintMid);
            canvas.drawText(getResources().getString(R.string.rpm_uc), canvas.getWidth() / 2, (float)centerBottomPositionY + mTextPaintMid.getTextSize(), mTextPaintMid);
        }
    }

    public float getGaugeValue() {
        return currentGaugeValue;
    }

    public void setGaugeValue(float newGaugeValue) {
        ValueAnimator animator = ValueAnimator.ofFloat(currentGaugeValue, newGaugeValue);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            currentGaugeValue = value;
            invalidate();
        });
        animator.start();
        this.currentGaugeValue = newGaugeValue;
    }

    public void setRangeUnits (String str) { rangeUnitsString = str; }

    public void setLargeTextValue(String value){ currentMiddleValue = value; }

    public void setLargeTextUnits(String value){
        currentMiddleUnit = value;
    }

    public void setGaugeRange (float minValue, float maxValue) {
        minGaugeValue = minValue;
        maxGaugeValue = maxValue;
    }

    public void setShowLargeText (boolean canShow){ showLargeText = canShow; }

    public void setShowBottomText (boolean canShow){ showBottomText = canShow; }

    public void setCurrentValueSameAsBottomValue (boolean currentValueSameAsBottomValue){ this.currentValueSameAsBottomValue = currentValueSameAsBottomValue; }

    public void setBottomGaugeValue (float value) { bottomGaugeValue = value; }


    //   Color, paint, font section
    protected void initPaints() {
        //The paint for the cursor
        hardLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hardLinePaint.setColor(Color.parseColor("#77ccff"));
        hardLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        hardLinePaint.setStrokeWidth(3);
        //Large background circle
        backgroundCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundCirclePaint.setColor(Color.parseColor("#012A36"));//Dropper color 1
        backgroundCirclePaint.setStyle(Paint.Style.STROKE);
        backgroundCirclePaint.setStrokeWidth(gaugeBandStrokeWidth * 4);
        //The smaller inner circle part at the bottom of the gauge
        backgroundCircleDetailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundCircleDetailPaint.setColor(Color.parseColor("#003B43"));
        backgroundCircleDetailPaint.setStyle(Paint.Style.STROKE);
        backgroundCircleDetailPaint.setStrokeWidth(gaugeBandStrokeWidth * .5f);
        edgeNeedlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgeNeedlePaint.setColor(Color.parseColor("#00404E"));
        edgeNeedlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        edgeNeedlePaint.setStrokeWidth(3);
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.parseColor("#03A6CB"));
        mTextPaint.setStrokeWidth(1);
        mTextPaint.setTextSize(fontSize);
        mTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mTextPaint.setTextAlign(Paint.Align.RIGHT);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaintMid = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaintMid.setColor(Color.parseColor("#03A6CB"));
        mTextPaintMid.setStrokeWidth(1);
        mTextPaintMid.setTextSize(fontSize);
        mTextPaintMid.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mTextPaintMid.setTextAlign(Paint.Align.CENTER);
        mTextPaintMid.setStyle(Paint.Style.FILL);
        mTextPaintBigValue = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaintBigValue.setColor(Color.parseColor("#ffffff"));
        mTextPaintBigValue.setStrokeWidth(1);
        mTextPaintBigValue.setTextSize(fontSizeLargeValue);
        mTextPaintBigValue.setTextAlign(Paint.Align.CENTER);
        mTextPaintBigValue.setStyle(Paint.Style.FILL);
        mTextPaintBigUnits = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaintBigUnits.setColor(Color.parseColor("#999999"));
        mTextPaintBigUnits.setStrokeWidth(1);
        mTextPaintBigUnits.setTextSize(fontSizeLargeUnits);
        mTextPaintBigUnits.setTextAlign(Paint.Align.CENTER);
        mTextPaintBigUnits.setStyle(Paint.Style.FILL);
    }


//Experimental drawing logo with canvas
//    public boolean drawIcon() {
//        try {
//            Paint p=new Paint();
//            Bitmap b= BitmapFactory.decodeResource(getResources(), R.drawable.ic_watts_white);
//            p.setColor(Color.WHITE);
//            canvas.drawBitmap(b, 0, 0, p);
//            return true;
//        }
//        catch (Exception e){
//            Log.e(TAG, "error: "+e);
//            return false;
//        }
//    }
//
// Experimental code using circular gradient (OnMeasure)
//        radialPaint1 = new Paint();
//        radialPaint1.setColor(Color.BLACK);
//        radialPaint1.setStyle(Paint.Style.FILL_AND_STROKE);
//        radialPaint1.setStrokeWidth(1);
//        radialPaint1.setShader(new RadialGradient(m_SmallerDimension / 2, m_SmallerDimension / 2f,
//        m_SmallerDimension / 2, Color.parseColor ("#0088aa"), Color.parseColor ("#115599"), Shader.TileMode.MIRROR));
}

