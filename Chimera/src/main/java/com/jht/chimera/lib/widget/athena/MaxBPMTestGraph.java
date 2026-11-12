package com.jht.chimera.lib.widget.athena;

import static com.jht.chimera.lib.common.context.ContextUtil.getString;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.jht.chimera.R;
import com.jht.workoutcontrollerlib.workoutcardio.WorkoutControllerCardio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MaxBPMTestGraph extends View
{
    public final String TAG = "MaxBPMTestGraph";

    private float _graphVirtualLengthInSeconds = 180;
    private float _currentSecond = 0;
    private float _percentageOfGraphToShow = 50;
    private float _virtualGraphWidth = 0;
    private float _maxWattsShowingOnGraph = 100f;
    private float _minWattsShowingOnGraph = 50f;
    private float _graphMostLeftPoint = 0;
    private float _currentVirtualDistance;
    private boolean mMainZoneBandOn = true;
    private boolean mStagesOn = false;
    private boolean ftpTestLogic = false;
    private boolean maxHRTestLogic = false;
    private boolean isInWorkoutStage = false;

    private float mWattsMainZoneLowerBoundry = 100;
    private float mWattsMainZoneHigherBoundry = 125;
    private float mWattsLowerZoneLowerBoundry = 25;
    private float mWattsLowerZoneHigherBoundry = 0;

    List<FloatPairs> mWattsElapsedTimeList = new ArrayList<>();
    private WorkoutControllerCardio mController;

    private Paint mMainZoneFillPaint;
    private Paint mLowerZoneFillPaint;
    private Paint mWarmupFTPPaint;
    private Paint mStage1ZoneFillPaint;
    private Paint mStage2ZoneFillPaint;
    private Paint mStage3ZoneFillPaint;
    private Paint mMainZoneLinePaint;
    private Paint mLowerZoneLinePaint;
    private Paint alpahOverWattsNumericsAreaPaint;
    private Paint graphBorderLinePaint;
    private Paint wattsTickPaint;
    private Paint numericWattsPaint;
    private Paint timeTextPaint;
    private Paint verticleTimeLinePaint;
    private Paint wattsLinePaint;
    private Paint mWattsFillPaint;
    private Paint triangleLeftPaint;
    private Paint triangleRightPaint;
    private Paint triangleSplitLineLeft;
    private Paint triangleSplitLineRight;
    private Paint mDebugPaint;
    private Paint wattsLineScalePaint;
    private Paint darkAlphaPaint;
    private Paint numericWattsWPaint;

    private Bitmap graphBackground;
    private Path _wattsLinePath = new Path();
    private Path _trianglePathLeft;
    private Path _trianglePathRight;
    private Path _needle;
    private Rect _wattsStringBounds = new Rect();
    private float mGraphBottomY = 75;
    private float mGraphTopY = 10;
    private int _fixedVerticleBarXOffset;
    final private int TOP_MARGIN = 26;
    final private int BOTTOM_MARGIN = 75;
    private float warmUpEndDuration = 0;
    private String wString = "W"; //NON-NLS

    class FloatPairs
    {
        private float watts;
        private float elapsedTime;

        public FloatPairs(float watts, float elapsedTime)
        {
            this.watts = watts;
            this.elapsedTime = elapsedTime;
            if (watts > _maxWattsShowingOnGraph) {
                _maxWattsShowingOnGraph = roundUp((int)watts);
            }
        }

        // rescale the graph by adding another 25 watts
        int roundUp(int n)
        {
            return (n + 24) / 25 * 25;
        }

        public float getWatts()
        {
            return watts;
        }

        public void setWatts(float watts)
        {
            this.watts = watts;
        }

        public float getElapsedTime()
        {
            return elapsedTime;
        }

        public void setElapsedTime(float elapsedTime)
        {
            this.elapsedTime = elapsedTime;
        }
    }

    public MaxBPMTestGraph(Context context)
    {
        super(context);
        init();
    }

    public MaxBPMTestGraph(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initTypedArray(context, attrs);
        init();
    }

    public MaxBPMTestGraph(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    private void init()
    {
        initPaints();
        graphBackground = BitmapFactory.decodeResource(getResources(), R.drawable.background_gray);
        wString = getString (R.string.watts_symbol_uc);
    }

    private void initTypedArray(Context context, AttributeSet attrs)
    {
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        int width = canvas.getWidth();
        int height = canvas.getHeight();
        //Log.d (TAG, "Height  =" + height);

        _fixedVerticleBarXOffset = width * 3/4;

        float percentIntoWorkout = _currentSecond / _graphVirtualLengthInSeconds;

        _virtualGraphWidth = width / (_percentageOfGraphToShow /100f);
        _currentVirtualDistance = percentIntoWorkout * _virtualGraphWidth;  // pixels

        _graphMostLeftPoint = _fixedVerticleBarXOffset - _currentVirtualDistance;
        _graphMostLeftPoint = Math.max (_graphMostLeftPoint, 0);

        mGraphTopY = TOP_MARGIN;
        mGraphBottomY = height - BOTTOM_MARGIN;

        // Keep the target zone bands on the screen by checking to see if
        // the zone high mark is bigger than the user max watts on screen
        if (mWattsMainZoneHigherBoundry > _maxWattsShowingOnGraph)
        {
            _maxWattsShowingOnGraph = mWattsMainZoneHigherBoundry;
        }

        canvas.drawRect(0, mGraphBottomY, width, height, darkAlphaPaint);
        canvas.drawRect(0, 0, width, TOP_MARGIN, darkAlphaPaint);

        Rect frameToDraw = new Rect(0, TOP_MARGIN, width, ((int) mGraphBottomY));
        RectF whereToDraw = new RectF(0, TOP_MARGIN, width, ((int) mGraphBottomY));
        canvas.drawBitmap(graphBackground, frameToDraw, whereToDraw, null);

        drawWattsNumericsGridLines (canvas);
        float lastY = computeWattsGraphLine(canvas);

        if (mMainZoneBandOn) {
            if (!mStagesOn) {
                drawZoneBands(canvas);
            }
        }

        if (ftpTestLogic) {
            float warmupEnd = (warmUpEndDuration / _graphVirtualLengthInSeconds) * _virtualGraphWidth - _currentVirtualDistance + _fixedVerticleBarXOffset;
            if (warmupEnd > _graphMostLeftPoint) {
                canvas.drawRect(_graphMostLeftPoint, mGraphBottomY, warmupEnd, mGraphTopY, mWarmupFTPPaint);
            }
        }

        canvas.drawRect(_fixedVerticleBarXOffset, TOP_MARGIN, width, mGraphBottomY, alpahOverWattsNumericsAreaPaint);
        // Graph vertical split section line
        canvas.drawLine (_fixedVerticleBarXOffset, mGraphBottomY, _fixedVerticleBarXOffset, mGraphTopY, wattsTickPaint);
        // Graph top line
        canvas.drawLine (0, mGraphTopY, canvas.getWidth(), mGraphTopY, graphBorderLinePaint);
        // Graph bottom line
        canvas.drawLine (0, mGraphBottomY, canvas.getWidth(), mGraphBottomY, graphBorderLinePaint);

        drawWattsNumerics (canvas);
        if (!mWattsElapsedTimeList.isEmpty()) drawWattsGraphLine (canvas);

        if (_trianglePathLeft == null)
        {
          createTriangles();
        }

        canvas.drawPath (_trianglePathLeft, triangleLeftPaint);
        canvas.drawPath (_trianglePathRight, triangleRightPaint);
        canvas.drawLine (_fixedVerticleBarXOffset-23, mGraphTopY, _fixedVerticleBarXOffset, mGraphTopY+10, triangleSplitLineLeft);
        canvas.drawLine (_fixedVerticleBarXOffset+23, mGraphTopY, _fixedVerticleBarXOffset, mGraphTopY+10, triangleSplitLineRight);

        drawNeedle (canvas, lastY);
    }

    private void createTriangles ()
    {
        _trianglePathLeft = new Path();
        _trianglePathLeft.moveTo (_fixedVerticleBarXOffset, mGraphTopY - 26);
        _trianglePathLeft.lineTo (_fixedVerticleBarXOffset, mGraphTopY + 30);
        _trianglePathLeft.lineTo (_fixedVerticleBarXOffset-23, mGraphTopY );
        _trianglePathLeft.lineTo (_fixedVerticleBarXOffset-18, mGraphTopY );
        _trianglePathLeft.close();

        _trianglePathRight = new Path();
        _trianglePathRight.moveTo (_fixedVerticleBarXOffset, mGraphTopY - 26);
        _trianglePathRight.lineTo (_fixedVerticleBarXOffset, mGraphTopY + 30);
        _trianglePathRight.lineTo (_fixedVerticleBarXOffset+23, mGraphTopY );
        _trianglePathRight.lineTo (_fixedVerticleBarXOffset+18, mGraphTopY );
        _trianglePathRight.close();

        _needle = new Path();
    }

    private void drawNeedle (Canvas canvas, float yValue)
    {

        _needle.reset();
        _needle.moveTo (_fixedVerticleBarXOffset, yValue-5);
        _needle.lineTo (_fixedVerticleBarXOffset+40, yValue);
        _needle.lineTo (_fixedVerticleBarXOffset, yValue+5);
        _needle.lineTo (_fixedVerticleBarXOffset-10, yValue );
        _needle.close();

        canvas.drawPath (_needle, triangleLeftPaint);
    }

    private float computeWattsGraphLine (Canvas canvas)
    {
        float nextY = 0;
        int lastElapsedTime = -1;

        boolean init = true;
        _wattsLinePath.reset();

        float minWattShowing = roundDownToNearest25th (mWattsMainZoneLowerBoundry);
        float maxWattShowing = mWattsMainZoneHigherBoundry;

        for (FloatPairs pair : mWattsElapsedTimeList)
        {
            float nextX = (pair.getElapsedTime() / _graphVirtualLengthInSeconds) * _virtualGraphWidth - _currentVirtualDistance + _fixedVerticleBarXOffset;

            nextY =  calculateYPosition(canvas, pair.getWatts());

            // see if this point is still on the screen
            if (nextX >= _graphMostLeftPoint && nextX >= -50 && nextX <= canvas.getWidth() + 50)
            {
                if (pair.getWatts() < minWattShowing)
                {
                    minWattShowing = pair.getWatts();
                }
                if (pair.getWatts() > maxWattShowing)
                {
                    maxWattShowing = pair.getWatts();
                }

                // if first data, then init the path
                if (init)
                {
                    _wattsLinePath.reset();
                    _wattsLinePath.moveTo (Math.max(0, _graphMostLeftPoint)-1, mGraphBottomY);
                    _wattsLinePath.lineTo (Math.max(0, _graphMostLeftPoint)-1, nextY);
                    init = false;
                }
                _wattsLinePath.lineTo(nextX, nextY);
            }

            int elapsedTime = (int)pair.getElapsedTime();
            if ((elapsedTime % 30 == 0) && lastElapsedTime != elapsedTime)
            {
                lastElapsedTime = elapsedTime;

                if (maxHRTestLogic) {
                    if (lastElapsedTime == 0) {
                        verticleTimeLinePaint.setStrokeWidth(2);
                        verticleTimeLinePaint.setColor(Color.parseColor("#39ABE2"));
                        canvas.drawLine(nextX, mGraphTopY, nextX, mGraphBottomY, verticleTimeLinePaint);
                    } else if (lastElapsedTime == 120) {
                        verticleTimeLinePaint.setStrokeWidth(2);
                        verticleTimeLinePaint.setColor(Color.parseColor("#2C9F19"));
                        canvas.drawLine(nextX, mGraphTopY, nextX, mGraphBottomY, verticleTimeLinePaint);
                    } else if (lastElapsedTime == 240) {
                        verticleTimeLinePaint.setStrokeWidth(2);
                        verticleTimeLinePaint.setColor(Color.parseColor("#F50B05"));
                        canvas.drawLine(nextX, mGraphTopY, nextX, mGraphBottomY, verticleTimeLinePaint);
                    } else {
                        verticleTimeLinePaint.setStrokeWidth(1);
                        verticleTimeLinePaint.setColor(Color.LTGRAY);
                        canvas.drawLine (nextX, mGraphTopY, nextX, mGraphBottomY, verticleTimeLinePaint);
                    }
                } else {
                    verticleTimeLinePaint.setStrokeWidth(1);
                    verticleTimeLinePaint.setColor(Color.LTGRAY);
                    canvas.drawLine (nextX, mGraphTopY, nextX, mGraphBottomY, verticleTimeLinePaint);
                }
                String timeString = String.format (Locale.ENGLISH, "%02d:%02d", elapsedTime / 60, elapsedTime % 60);
                canvas.drawText (timeString, nextX - 27, mGraphBottomY + 30, timeTextPaint);
            }

        }
        float stage1Start = (0 / _graphVirtualLengthInSeconds) * _virtualGraphWidth - _currentVirtualDistance + _fixedVerticleBarXOffset;
        // get start value for stage 2 (after 2 minutes)
        float stage2Start = (120 / _graphVirtualLengthInSeconds) * _virtualGraphWidth - _currentVirtualDistance + _fixedVerticleBarXOffset;
        // get start value for stage 3 (after 4 minutes)
        float stage3Start = (240 / _graphVirtualLengthInSeconds) * _virtualGraphWidth - _currentVirtualDistance + _fixedVerticleBarXOffset;

        // get end value for stage 1 (after 2 minutes)
        float stage1End = (120 / _graphVirtualLengthInSeconds) * _virtualGraphWidth - _currentVirtualDistance + _fixedVerticleBarXOffset;
        // get end value for stage 2 (after 4 minutes)
        float stage2End = (240 / _graphVirtualLengthInSeconds) * _virtualGraphWidth - _currentVirtualDistance + _fixedVerticleBarXOffset;
        // get end value for stage 3 (after 6 minutes)
        float stage3End = (360 / _graphVirtualLengthInSeconds) * _virtualGraphWidth - _currentVirtualDistance + _fixedVerticleBarXOffset;


        if (maxHRTestLogic) {
            if (lastElapsedTime >= stage1End || lastElapsedTime >= stage2End || lastElapsedTime >= stage3End) {
                canvas.drawRect(stage1Start, mGraphBottomY, stage1End, mGraphTopY, mStage1ZoneFillPaint);
                canvas.drawRect(stage2Start, mGraphBottomY, stage2End, mGraphTopY, mStage2ZoneFillPaint);
                canvas.drawRect(stage3Start, mGraphBottomY, stage3End, mGraphTopY, mStage3ZoneFillPaint);
            }
            else {
                canvas.drawRect(stage1Start, mGraphBottomY, _fixedVerticleBarXOffset, mGraphTopY, mStage1ZoneFillPaint);
                canvas.drawRect(stage2Start, mGraphBottomY, _fixedVerticleBarXOffset, mGraphTopY, mStage2ZoneFillPaint);
                canvas.drawRect(stage3Start, mGraphBottomY, _fixedVerticleBarXOffset, mGraphTopY, mStage3ZoneFillPaint);
            }
        }

        _minWattsShowingOnGraph = Math.max (minWattShowing-25, 0);
        _maxWattsShowingOnGraph = maxWattShowing + 25;
        return nextY;
    }

    private void drawWattsGraphLine (Canvas canvas)
    {
        if (!mMainZoneBandOn && !(isInWorkoutStage)) {
            _wattsLinePath.lineTo (_fixedVerticleBarXOffset, mGraphBottomY);
            _wattsLinePath.close();
            canvas.drawPath (_wattsLinePath, mWattsFillPaint);
            canvas.drawPath (_wattsLinePath, mLowerZoneLinePaint);
        } else if (isInWorkoutStage) {
            canvas.drawPath (_wattsLinePath, wattsLinePaint);
        } else if (maxHRTestLogic) {
            canvas.drawPath (_wattsLinePath, wattsLinePaint);
        } else {
            canvas.drawPath (_wattsLinePath, wattsLinePaint);
        }
    }

    private void drawZoneBands(Canvas canvas) {
        float zoneHighY = calculateYPosition(canvas, mWattsMainZoneHigherBoundry);
        float zoneLowY = calculateYPosition(canvas, mWattsMainZoneLowerBoundry);
        canvas.drawRect(_graphMostLeftPoint, zoneHighY, canvas.getWidth(), zoneLowY, mMainZoneFillPaint);
        canvas.drawLine(_graphMostLeftPoint, zoneHighY, canvas.getWidth(), zoneHighY, mMainZoneLinePaint);
        canvas.drawLine(_graphMostLeftPoint, zoneLowY, canvas.getWidth(), zoneLowY, mMainZoneLinePaint);
    }

    private String workingString;

    private void drawWattsNumerics (Canvas canvas)
    {
        for (float f = 0; f <= _maxWattsShowingOnGraph +25; f += 25) {
            float Y = calculateYPosition(canvas, f);
            if (Y > mGraphTopY && Y < mGraphBottomY) {
                int yAsInt = (int) f;

                if (yAsInt % 50 == 0) {
                    canvas.drawLine(_fixedVerticleBarXOffset, Y, _fixedVerticleBarXOffset + 50, Y, wattsTickPaint);
                    workingString = Integer.toString(yAsInt);
                    canvas.drawText(workingString, _fixedVerticleBarXOffset + 65, Y+13, numericWattsPaint);

                    numericWattsPaint.getTextBounds (workingString, 0, workingString.length(), _wattsStringBounds);
                    canvas.drawText(wString, _fixedVerticleBarXOffset + 75 + _wattsStringBounds.width(), Y+13, numericWattsWPaint);
                }
                else if (yAsInt % 25 == 0) {
                    canvas.drawLine(_fixedVerticleBarXOffset, Y, _fixedVerticleBarXOffset + 25, Y, wattsTickPaint);
                    //canvas.drawText(yAsInt + "w", _fixedVerticleBarXOffset + 55, Y+13, numericWattsPaint);
                }
            }
        }
    }

    private void drawWattsNumericsGridLines (Canvas canvas)
    {
        for (float f = 0; f <= _maxWattsShowingOnGraph +25; f += 25) {
            float Y = calculateYPosition(canvas, f);
            if (Y > mGraphTopY && Y < mGraphBottomY) {
                if(((int)f % 50) == 0) {
                    wattsLineScalePaint.setColor(Color.WHITE);
                }
                else {
                    // horizontal watts scale grid line
                    wattsLineScalePaint.setColor(Color.BLACK);
                }
                canvas.drawLine(0, Y, _virtualGraphWidth, Y, wattsLineScalePaint);
            }
        }
    }

    private int roundDownToNearest25th (float value)
    {
        int rd = ((int) value / 25) * 25;
        return rd;
    }

    private int roundUpToNearest25th (float value)
    {
        int rd = ((int) value / 25) * 25;
        return rd + 25;
    }

    // Given a value in watts, return float Y coordinate position
    private float calculateYPosition (Canvas canvas, float watts)
    {
        float wattsDisplayRange = _maxWattsShowingOnGraph - _minWattsShowingOnGraph;
        float graphHeight = mGraphBottomY - mGraphTopY;
        float pixelsPerWatt = graphHeight/wattsDisplayRange;

        float nextY =  mGraphBottomY - ((watts- _minWattsShowingOnGraph) * pixelsPerWatt);

        return nextY;
    }

    public void addWattSample (float watts, float elapsedTime)
    {
        _currentSecond = elapsedTime;
        mWattsElapsedTimeList.add(new FloatPairs(watts, elapsedTime));
        invalidate();
    }

    public void setZoneBandOn (boolean onState)
    {
        mMainZoneBandOn = onState;
    }

    public void setStagesOn (boolean onState)
    {
        mStagesOn = onState;
    }

    public void setIsInWorkoutStage(boolean b) {
        isInWorkoutStage = true;
        mMainZoneBandOn = true;
    }

    public void setZoneRange (float lowBoundry, float highBoundry)
    {
        mWattsMainZoneHigherBoundry = roundDownToNearest25th (lowBoundry);
        mWattsLowerZoneHigherBoundry = mWattsMainZoneLowerBoundry;
        mWattsMainZoneHigherBoundry = roundUpToNearest25th (highBoundry);
        mWattsLowerZoneLowerBoundry = mWattsLowerZoneHigherBoundry;
    }

    public void setZoneRangeNoRounding (float lowBoundry, float highBoundry)
    {
        mWattsMainZoneLowerBoundry = lowBoundry;
        mWattsMainZoneHigherBoundry = highBoundry;
        mWattsLowerZoneHigherBoundry = mWattsMainZoneLowerBoundry;
        mWattsLowerZoneLowerBoundry = mWattsLowerZoneHigherBoundry - 25;
    }

    public void setWarmUpDuration (float duration) { warmUpEndDuration = duration; }

    public void setMaxHRTestLogic (boolean logicFlag) {
        maxHRTestLogic = logicFlag;
        wattsLinePaint.setColor(Color.WHITE);}
    public void setFTPTestLogic (boolean logicFlag) { ftpTestLogic = logicFlag;}

    /////////////////////////////////////////////////////////////////////
    //
    //   Color, paint, font section
    //
    /////////////////////////////////////////////////////////////////////

    protected void initPaints()
    {
        verticleTimeLinePaint = new Paint();
        verticleTimeLinePaint.setColor(Color.LTGRAY);
        verticleTimeLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        verticleTimeLinePaint.setStrokeWidth(1);

        numericWattsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numericWattsPaint.setColor(Color.WHITE);
        numericWattsPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        numericWattsPaint.setTextSize(50);

        numericWattsWPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numericWattsWPaint.setColor(Color.parseColor("#9B9B9B"));
        numericWattsWPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        numericWattsWPaint.setTextSize(30);

        timeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timeTextPaint.setColor(Color.parseColor ("#9B9B9B"));
        timeTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        timeTextPaint.setStrokeWidth(1);
        timeTextPaint.setTextSize(24);

        graphBorderLinePaint = new Paint();
        graphBorderLinePaint.setColor(Color.parseColor("#D8D8D8"));
        graphBorderLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        graphBorderLinePaint.setStrokeWidth(2);

        triangleLeftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        triangleLeftPaint.setColor(Color.WHITE);
        triangleLeftPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        triangleLeftPaint.setStrokeWidth(1);

        triangleRightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        triangleRightPaint.setColor(Color.parseColor ("#E4E4E4"));
        triangleRightPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        triangleRightPaint.setStrokeWidth(1);

        triangleSplitLineLeft = new Paint(Paint.ANTI_ALIAS_FLAG);
        triangleSplitLineLeft.setColor(Color.BLUE);
        triangleSplitLineLeft.setStyle(Paint.Style.FILL_AND_STROKE);
        triangleSplitLineLeft.setStrokeWidth(2);

        triangleSplitLineRight = new Paint(Paint.ANTI_ALIAS_FLAG);
        triangleSplitLineRight.setColor(Color.BLACK);
        triangleSplitLineRight.setStyle(Paint.Style.FILL_AND_STROKE);
        triangleSplitLineRight.setStrokeWidth(2);

        wattsLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wattsLinePaint.setStyle(Paint.Style.STROKE);
        wattsLinePaint.setStrokeWidth(3);
        wattsLinePaint.setColor(Color.parseColor("#84B8FF"));

        wattsTickPaint = new Paint();
        wattsTickPaint.setColor(Color.parseColor("#BF9E9E9E"));
        wattsTickPaint.setStyle(Paint.Style.STROKE);
        wattsTickPaint.setStrokeWidth(2);

        mWattsFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWattsFillPaint.setStrokeCap(Paint.Cap.ROUND);
        mWattsFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mWattsFillPaint.setStrokeWidth(1);
        mWattsFillPaint.setShader(new LinearGradient(0, 0, 0, 200,  Color.parseColor("#CC1E4C8D"), Color.parseColor ("#B302193A"), Shader.TileMode.MIRROR));

        mMainZoneFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMainZoneFillPaint.setColor(Color.parseColor("#CC36783C"));
        mMainZoneFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mMainZoneFillPaint.setStrokeWidth(1);

        mLowerZoneFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLowerZoneFillPaint.setColor(Color.parseColor("#CC1E4C8D"));
        mLowerZoneFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mLowerZoneFillPaint.setStrokeWidth(1);

        mWarmupFTPPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWarmupFTPPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mWarmupFTPPaint.setStrokeWidth(1);
        mWarmupFTPPaint.setAlpha(128);
        mWarmupFTPPaint.setShader(new LinearGradient(0f, mGraphBottomY, 0f, mGraphTopY, Color.parseColor("#FFB34D13"), Color.parseColor("#FFF3B125"), Shader.TileMode.CLAMP));

        mStage1ZoneFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStage1ZoneFillPaint.setColor(Color.parseColor("#00234E"));
        mStage1ZoneFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mStage1ZoneFillPaint.setStrokeWidth(1);
        mStage1ZoneFillPaint.setAlpha(76);

        mStage2ZoneFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStage2ZoneFillPaint.setColor(Color.parseColor("#009245"));
        mStage2ZoneFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mStage2ZoneFillPaint.setStrokeWidth(1);
        mStage2ZoneFillPaint.setAlpha(76);

        mStage3ZoneFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStage3ZoneFillPaint.setColor(Color.parseColor("#C81C2D"));
        mStage3ZoneFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mStage3ZoneFillPaint.setStrokeWidth(1);
        mStage3ZoneFillPaint.setAlpha(76);

        mMainZoneLinePaint = new Paint();
        mMainZoneLinePaint.setColor(Color.parseColor("#3F9C47"));
        mMainZoneLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mMainZoneLinePaint.setStrokeWidth(4);

        mLowerZoneLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLowerZoneLinePaint.setColor(Color.parseColor("#3578D6"));
        mLowerZoneLinePaint.setStyle(Paint.Style.STROKE);
        mLowerZoneLinePaint.setStrokeWidth(4);

        mDebugPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDebugPaint.setColor(Color.parseColor("#ff0000"));
        mDebugPaint.setStyle(Paint.Style.STROKE);
        mDebugPaint.setStrokeWidth(1);
        mDebugPaint.setTextSize(12);

        alpahOverWattsNumericsAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        alpahOverWattsNumericsAreaPaint.setColor(Color.parseColor("#BF000000"));
        alpahOverWattsNumericsAreaPaint.setStyle(Paint.Style.FILL);
        alpahOverWattsNumericsAreaPaint.setStrokeWidth(1);

        wattsLineScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wattsLineScalePaint.setStyle(Paint.Style.STROKE);
        wattsLineScalePaint.setStrokeWidth(1);

        darkAlphaPaint = new Paint();
        darkAlphaPaint.setColor(Color.parseColor("#aa000000"));
        darkAlphaPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        darkAlphaPaint.setStrokeWidth(1);

        triangleSplitLineLeft = new Paint(Paint.ANTI_ALIAS_FLAG);
        triangleSplitLineLeft.setColor(Color.parseColor("#B2B2B2"));
        triangleSplitLineLeft.setStyle(Paint.Style.FILL);
        triangleSplitLineLeft.setStrokeWidth(3);

        triangleSplitLineRight = new Paint(Paint.ANTI_ALIAS_FLAG);
        triangleSplitLineRight.setColor(Color.parseColor("#999999"));
        triangleSplitLineRight.setStyle(Paint.Style.FILL);
        triangleSplitLineRight.setStrokeWidth(3);
    }
}
