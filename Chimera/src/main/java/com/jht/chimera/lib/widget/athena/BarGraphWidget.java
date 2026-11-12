package com.jht.chimera.lib.widget.athena;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class BarGraphWidget extends View
{
    public final String TAG = "BarGraphWidget";
    private Paint timeGapNumericPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint timeGapTextPaint = new Paint (Paint.ANTI_ALIAS_FLAG);
    private Paint barGraphGraidentPaint = new Paint (Paint.ANTI_ALIAS_FLAG);
    private Paint verticleBarLinePaint = new Paint (Paint.ANTI_ALIAS_FLAG);
    private Paint barGraphTopLinePaint = new Paint (Paint.ANTI_ALIAS_FLAG);
    private Paint timeGapBoxPaint = new Paint (Paint.ANTI_ALIAS_FLAG);
    private Paint timeGapTrianglePaint = new Paint (Paint.ANTI_ALIAS_FLAG);
    private Paint backgroundPaint = new Paint();
    private Path  trianglePath = new Path();

    private Paint greenZoneLinePaint = new Paint (Paint.ANTI_ALIAS_FLAG);
    private Paint greenZonePaint = new Paint();

    private float canvasWidth;
    private float canvasHeight;
    private RectF bargraphOutlineRectF = new RectF();
    private RectF filledBarGradientRectF = new RectF();
    private RectF timeOffsetRectF = new RectF();
    private RectF greenZoneRectF = new RectF();
    private Rect  textBounds = new Rect();
    private boolean bShowLabel = true;
    private float barBorderStrokeWidth = 3;
    private boolean bSolidBar = false;
    private boolean showZone = true;

    public enum  TTCOLOR  {RED, GREEN, BLUE, OFF};
    private TTCOLOR colorChange = TTCOLOR.OFF;

    private String flagString = "     ";
    private float fillLevel = 0;
    private int TOP_BOTTOM_BAR_MARGIN = 25;
    private float goalLineOffsetFromTopInPercent = 0;
    private float goalLineOffsetInPixels = 0;
    private float lineMarkerOffset = 0;
    private float scale;
    private float targetZoneHeight = 75;
    private LinearGradient barGradient;

    public BarGraphWidget(Context context)
    {
        super(context);
        init();
    }

    public BarGraphWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initTypedArray(context, attrs);
        init();
    }

    public BarGraphWidget(Context context, AttributeSet attrs, int defStyle)
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
    public void onSizeChanged (int w, int h, int oldw, int oldh)
    {
        canvasWidth = w;
        canvasHeight = h;

        scale = w/100;
        targetZoneHeight = h * .05f;
        bargraphOutlineRectF.set (canvasWidth*.6f, TOP_BOTTOM_BAR_MARGIN, canvasWidth, canvasHeight - TOP_BOTTOM_BAR_MARGIN);
        greenZoneRectF.set (bargraphOutlineRectF.left, bargraphOutlineRectF.centerY()-targetZoneHeight, bargraphOutlineRectF.right, bargraphOutlineRectF.centerY()+targetZoneHeight);

        initPaints();
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        canvas.drawRect(0, 0, canvasWidth, canvasHeight, backgroundPaint);

        goalLineOffsetInPixels = (canvasHeight-TOP_BOTTOM_BAR_MARGIN) * goalLineOffsetFromTopInPercent;
        float workingHeight = bargraphOutlineRectF.height() - goalLineOffsetInPixels;
        //float y = (bargraphOutlineRectF.bottom+bargraphOutlineRectF.top) * lineMarkerOffset;
        float fillPoint = workingHeight * fillLevel;

        //Log.d (TAG, "Fillpoint=" + fillPoint + " height=" + filledBarGradientRectF.height());

        filledBarGradientRectF.set (bargraphOutlineRectF.left, bargraphOutlineRectF.bottom - fillPoint, bargraphOutlineRectF.right, bargraphOutlineRectF.bottom);

        // interior box gradient fill
        canvas.drawRect (filledBarGradientRectF.left, filledBarGradientRectF.top, filledBarGradientRectF.right, filledBarGradientRectF.bottom, barGraphGraidentPaint);

        if (showZone) {
            canvas.drawRect(greenZoneRectF, greenZonePaint);
            canvas.drawLine(greenZoneRectF.left, greenZoneRectF.top + 1, greenZoneRectF.right, greenZoneRectF.top + 1, greenZoneLinePaint);
            canvas.drawLine(greenZoneRectF.left, greenZoneRectF.bottom, greenZoneRectF.right, greenZoneRectF.bottom, greenZoneLinePaint);

            drawTarget (canvas);
        }

        // white bar graph side lines
        if (bSolidBar) {
            canvas.drawRect(bargraphOutlineRectF, verticleBarLinePaint);
        }
        else {
            // white bar graph fading side lines
            canvas.drawLine(bargraphOutlineRectF.left, bargraphOutlineRectF.top, bargraphOutlineRectF.left, bargraphOutlineRectF.bottom, verticleBarLinePaint);
            canvas.drawLine(bargraphOutlineRectF.right, bargraphOutlineRectF.top, bargraphOutlineRectF.right, bargraphOutlineRectF.bottom, verticleBarLinePaint);
        }

        // Top line in filled bar graph (lines up with numeric box)
        if (fillLevel > 0 && fillLevel < 100) {
            canvas.drawLine(filledBarGradientRectF.left, filledBarGradientRectF.top, filledBarGradientRectF.right, filledBarGradientRectF.top, barGraphTopLinePaint);
        }

        //  Triangle of numeric box
        float triangleWidth = canvasWidth/6;
        trianglePath.reset();
        trianglePath.moveTo (filledBarGradientRectF.left, filledBarGradientRectF.top);
        trianglePath.lineTo (filledBarGradientRectF.left - triangleWidth, filledBarGradientRectF.top - triangleWidth);
        trianglePath.lineTo (filledBarGradientRectF.left - triangleWidth, filledBarGradientRectF.top + triangleWidth);
        trianglePath.close();

        canvas.drawPath(trianglePath, timeGapTrianglePaint);

        // The top of the bar graph is not always the end goal value.   Sometimes the goal value is set lower in the bar graph with a solid line connecting
        // to a label
        if (goalLineOffsetFromTopInPercent > 0)
        {
            canvas.drawLine(bargraphOutlineRectF.right, goalLineOffsetInPixels + bargraphOutlineRectF.top, bargraphOutlineRectF.left - 25*scale, goalLineOffsetInPixels +bargraphOutlineRectF.top, timeGapTextPaint);
        }
        else if (lineMarkerOffset > 0)   // just a marker line
        {
            float y = (bargraphOutlineRectF.bottom + bargraphOutlineRectF.top) * lineMarkerOffset;
            canvas.drawLine(bargraphOutlineRectF.left, y, bargraphOutlineRectF.left - 20, y, timeGapTextPaint);
        }

        if (bShowLabel)
        {
            timeOffsetRectF.set(0, filledBarGradientRectF.top - triangleWidth, filledBarGradientRectF.left - triangleWidth, filledBarGradientRectF.top + triangleWidth);
            canvas.drawRect(timeOffsetRectF, timeGapBoxPaint);

            timeGapTextPaint.getTextBounds(flagString, 0, flagString.length(), textBounds);
            float x = timeOffsetRectF.centerX() - textBounds.width() / 2;
            float y = timeOffsetRectF.centerY() - textBounds.centerY();

            canvas.drawText(flagString, x >= 0 ? x : 0, y >= 0 ? y : 0, timeGapTextPaint);
        }
    }

    private void drawTarget (Canvas canvas)
    {
        float circleRadius = bargraphOutlineRectF.width() / 6;
        float circleCenterX = greenZoneRectF.right - bargraphOutlineRectF.width() / 2;
        float circleCenterY = greenZoneRectF.centerY();

        canvas.drawCircle(circleCenterX, circleCenterY, circleRadius, timeGapNumericPaint);
        canvas.drawLine(circleCenterX - circleRadius * 1.5f, circleCenterY, circleCenterX - circleRadius * .5f, circleCenterY, timeGapNumericPaint);
        canvas.drawLine(circleCenterX + circleRadius * .5f, circleCenterY, circleCenterX + circleRadius * 1.5f, circleCenterY, timeGapNumericPaint);

        canvas.drawLine(circleCenterX, circleCenterY - circleRadius * 1.5f, circleCenterX, circleCenterY - circleRadius * .5f, timeGapNumericPaint);
        canvas.drawLine(circleCenterX, circleCenterY + circleRadius * 1.5f, circleCenterX, circleCenterY + circleRadius * .5f, timeGapNumericPaint);
    }

    /////////////////////////////////////////////////////////////////////
    //
    //   Color, paint, font section
    //
    /////////////////////////////////////////////////////////////////////

    private void initPaints()
    {
        timeGapNumericPaint.setColor(Color.parseColor("#cceeff"));
        timeGapNumericPaint.setStyle(Paint.Style.STROKE);
        timeGapNumericPaint.setStrokeWidth(1);
        timeGapNumericPaint.setTextSize(20f * scale);
        timeGapNumericPaint.setStrokeCap(Paint.Cap.ROUND);

        verticleBarLinePaint.setStyle(Paint.Style.STROKE);
        verticleBarLinePaint.setStrokeWidth(barBorderStrokeWidth);
        verticleBarLinePaint.setStrokeCap(Paint.Cap.ROUND);

        // linear-gradient(180deg, #00A5A5A5 0%, #A5A5A5 19.43%, #A5A5A5 77.2%, #00A5A5A5 100%)
        float barHeight = bargraphOutlineRectF.bottom - bargraphOutlineRectF.top;
        if (bSolidBar)
            verticleBarLinePaint.setColor(Color.parseColor("#a5a5a5"));
        else
            verticleBarLinePaint.setShader(new LinearGradient(0, 0, 0, barHeight/2,  Color.parseColor("#00A5A5A5"), Color.parseColor ("#A5A5A5"), Shader.TileMode.MIRROR));

        barGraphGraidentPaint.setStyle(Paint.Style.FILL);
        barGraphGraidentPaint.setStrokeWidth(1);
        barGraphGraidentPaint.setStrokeCap(Paint.Cap.ROUND);
        barGradient = new LinearGradient(0, barHeight, 0, 0, Color.parseColor("#0029ABE2"), Color.parseColor ("#CC29ABE2"), Shader.TileMode.CLAMP);
        barGraphGraidentPaint.setShader (barGradient);

        barGraphTopLinePaint.setStyle (Paint.Style.FILL_AND_STROKE);
        barGraphTopLinePaint.setStrokeWidth(5* scale);
        barGraphTopLinePaint.setStrokeCap(Paint.Cap.ROUND);
        barGraphTopLinePaint.setColor(Color.parseColor("#13536D"));

        timeGapTrianglePaint.setStyle (Paint.Style.FILL_AND_STROKE);
        timeGapTrianglePaint.setStrokeWidth(1);
        timeGapTrianglePaint.setStrokeCap(Paint.Cap.ROUND);
        timeGapTrianglePaint.setColor(Color.parseColor("#1C779D"));

        timeGapBoxPaint.setStyle (Paint.Style.FILL_AND_STROKE);
        timeGapBoxPaint.setStrokeWidth(1);
        timeGapBoxPaint.setStrokeCap(Paint.Cap.ROUND);
        timeGapBoxPaint.setColor(Color.parseColor("#29ABE2"));

        timeGapTextPaint.setColor(Color.parseColor("#ffffff"));
        timeGapTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        timeGapTextPaint.setStrokeWidth(1);
        timeGapTextPaint.setTextSize(20f * scale);
        timeGapTextPaint.setStrokeCap(Paint.Cap.ROUND);

        backgroundPaint.setColor(Color.parseColor("#00000000"));
        backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        backgroundPaint.setStrokeWidth(1);

        greenZonePaint.setStyle(Paint.Style.FILL);
        greenZonePaint.setStrokeWidth(1);
        greenZonePaint.setStrokeCap(Paint.Cap.ROUND);
        //greenZonePaint.setColor(Color.parseColor("#44444444"));
        greenZonePaint.setShader(new LinearGradient(0, 0, canvasWidth, 122,  Color.parseColor("#003F9C47"), Color.parseColor ("#663F9C47"),Shader.TileMode.MIRROR));

        // android:endColor="#003F9C47"    android:startColor="#3F9C47"
        greenZoneLinePaint.setStyle(Paint.Style.FILL);
        greenZoneLinePaint.setStrokeWidth(2);
        greenZoneLinePaint.setStrokeCap(Paint.Cap.ROUND);
        //greenZoneLinePaint.setColor(Color.GREEN);
        greenZoneLinePaint.setShader(new LinearGradient(0, 0, canvasWidth, 1,  Color.parseColor("#223F9C47"), Color.parseColor ("#cc3F9C47"),Shader.TileMode.MIRROR));

        changeBarColor(colorChange);
    }

    public void changeBarColor (TTCOLOR changeToColor)
    {
        if (changeToColor == TTCOLOR.RED)   // To Fast -- RED
        {
            barGraphTopLinePaint.setColor(Color.parseColor("#C60F10"));
            timeGapTrianglePaint.setColor(Color.parseColor("#960B0C"));
            timeGapBoxPaint.setColor(Color.parseColor("#C60F10"));
            //android:endColor="#00C60F10"  android:startColor="#CCC60F10"
            barGradient = new LinearGradient(0, bargraphOutlineRectF.height(), 0, 0, Color.parseColor("#00C60F10"), Color.parseColor ("#CCC60F10"), Shader.TileMode.CLAMP);
            barGraphGraidentPaint.setShader (barGradient);
        }
        else if (changeToColor == TTCOLOR.GREEN)   // -- GREEN
        {
            barGraphTopLinePaint.setColor(Color.parseColor("#3F9C47"));
            timeGapTrianglePaint.setColor(Color.parseColor("#663F9C47"));
            timeGapBoxPaint.setColor(Color.parseColor("#3F9C47"));
            barGradient = new LinearGradient(0, bargraphOutlineRectF.height(), 0, 0, Color.parseColor("#003F9C47"), Color.parseColor ("#CC3F9C47"), Shader.TileMode.CLAMP);
            barGraphGraidentPaint.setShader (barGradient);
        }
        else  // On target -- BLUE
        {
            barGraphTopLinePaint.setColor(Color.parseColor("#13536D"));
            timeGapBoxPaint.setColor(Color.parseColor("#29ABE2"));
            timeGapTrianglePaint.setColor(Color.parseColor("#1C779D"));
            barGradient = new LinearGradient(0, bargraphOutlineRectF.height(), 0, 0, Color.parseColor("#0029ABE2"), Color.parseColor ("#CC29ABE2"), Shader.TileMode.CLAMP);
            barGraphGraidentPaint.setShader (barGradient);
        }

        colorChange = changeToColor;
    }

    public void updateLabel (String flagLabel)
    {
        flagString = flagLabel;
        invalidate();
    }

    public void showLabel (boolean hideShowState)
    {
        bShowLabel = hideShowState;
    }
    public void showTargetZone (boolean hideShowState)
    {
        showZone = hideShowState;
    }

    public void setGoalLineOffsetAsDecimalPercent(float offsetFromTopInPercent) { goalLineOffsetFromTopInPercent = offsetFromTopInPercent; }
    public void setMarkerLineAsDecimalPercent(float offsetFromTopInPixels) { lineMarkerOffset = offsetFromTopInPixels; }

    public void setBorderAsSolidBar (boolean wantSolidBar) { bSolidBar = wantSolidBar; }

    public void updatePercentageFilled (float percentFilledAsDecimal)   //  .0 - .9999999
    {
        if (percentFilledAsDecimal < 0) percentFilledAsDecimal = 0;
        if (percentFilledAsDecimal > 100) percentFilledAsDecimal = 1;

        ValueAnimator animator = ValueAnimator.ofFloat(fillLevel, percentFilledAsDecimal);

        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(1000);

        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();

            fillLevel = Math.min (value, 1f);
            invalidate();
        });

        animator.start();
    }
}
