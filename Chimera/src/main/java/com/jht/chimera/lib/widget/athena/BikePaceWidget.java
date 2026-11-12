package com.jht.chimera.lib.widget.athena;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import org.jetbrains.annotations.NonNls;

import java.util.Locale;

public class BikePaceWidget extends View
{
    final private String TAG = "BikePaceWidget";

    private int m_StrokeWidth = 4;
    private float blackLineWidth = 2;

    private RectF mBikeRect;

    private Paint mForegroundPaint;

    private float mTraveledDistance;
    private float mGhostTraveledDistance;
    private float nextDistanceMarker = .1f;
    private float nextNextDistanceMarker = .2f;

    private float mCurrentElapsedTime = 0;

    private Paint topShadowPaint;
    private Paint mBikePaint;
    private Paint bottomShadowPaint;
    private Paint redLinePaint;
    private Paint blackLinePaint;
    private Paint distanceMarkerBackgroundPaint;
    private Paint yellowRoadLinePaint;
    private Paint whiteRoadLinePaint;

    private Paint markerBoxLinePaint;
    private Paint markerTextPaint;
    private Paint markerTextPaint2;
    private Paint markerBoxPaint;
    private Paint markerShadowPaint;

    private Paint roadPaint;
    private Paint bottomShadow;

    private Path leftCornerGrayTriangle = new Path();
    private Path rightCornerGrayTriangle = new Path();

    private Path leftRoadPath = new Path();
    private Path rightRoadPath = new Path();
    private Path yellowLinePath = new Path();

    private Path markerPath = new Path();
    @NonNls private String unitString = "KM";

    final int boxWidth = 283;
    final int boxBottom = (int)(boxWidth*.44f);     //  .44 calculated from design asset
    final int boxHeight = (int)(boxBottom + boxBottom/3);

    final private Rect boxRect = new Rect();
    private Rect resizeRect = new Rect();

    private Bitmap markerBitmap1;
    private Bitmap markerBitmap2;
    private Canvas markerCanvas1;
    private Canvas markerCanvas2;
    private float mGhostKPH = 0;
    private float topHalfSize;
    private float bottomHalfSize;

    public BikePaceWidget(Context context)
    {
        super(context);
        init();
    }

    public BikePaceWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initTypedArray(context, attrs);
        init();
    }

    public BikePaceWidget(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    private void init()
    {
        mBikeRect = new RectF();
        //mTriangleRectf = new RectF();
        initPaints();
        //fullScaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bike_guy_temp);

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        markerBitmap1 = Bitmap.createBitmap(boxWidth, boxHeight, conf);
        markerBitmap2 = Bitmap.createBitmap(boxWidth, boxHeight, conf);

        markerCanvas1 = new Canvas(markerBitmap1);
        markerCanvas2 = new Canvas(markerBitmap2);
    }

    protected void initPaints()
    {
        mForegroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mForegroundPaint.setColor(Color.WHITE);
        mForegroundPaint.setStyle(Paint.Style.STROKE);
        mForegroundPaint.setStrokeWidth(m_StrokeWidth);
        mForegroundPaint.setTextSize(20);

        mBikePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBikePaint.setColor(Color.WHITE);
        mBikePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mBikePaint.setStrokeWidth(1);
        mBikePaint.setTextSize(20);

        topShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        topShadowPaint.setColor(Color.WHITE);
        topShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        bottomShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bottomShadowPaint.setColor(Color.parseColor("#555555"));
        bottomShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        redLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redLinePaint.setStrokeWidth(1);
        redLinePaint.setStyle(Paint.Style.STROKE);
        redLinePaint.setColor(Color.RED);

        blackLinePaint = new Paint();
        blackLinePaint.setStrokeWidth(blackLineWidth);
        blackLinePaint.setStyle(Paint.Style.STROKE);
        blackLinePaint.setColor(Color.BLACK);

        distanceMarkerBackgroundPaint = new Paint();
        distanceMarkerBackgroundPaint.setStrokeWidth(1);
        distanceMarkerBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        distanceMarkerBackgroundPaint.setColor(Color.BLACK);

        yellowRoadLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        yellowRoadLinePaint.setStrokeWidth(1);
        yellowRoadLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        yellowRoadLinePaint.setColor (Color.YELLOW);

        whiteRoadLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteRoadLinePaint.setStrokeWidth(2);
        whiteRoadLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        whiteRoadLinePaint.setColor (Color.WHITE);

        markerBoxLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerBoxLinePaint.setStrokeWidth(1);
        markerBoxLinePaint.setStyle(Paint.Style.STROKE);
        markerBoxLinePaint.setColor (Color.WHITE);

        markerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerTextPaint.setStrokeWidth(1);
        markerTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        markerTextPaint.setColor (Color.WHITE);
        markerTextPaint.setTextAlign(Paint.Align.RIGHT);
        markerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        markerTextPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerTextPaint2.setStrokeWidth(1);
        markerTextPaint2.setStyle(Paint.Style.FILL_AND_STROKE);
        markerTextPaint2.setColor (Color.LTGRAY);
        markerTextPaint2.setTextAlign(Paint.Align.LEFT);
        markerTextPaint2.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        markerBoxPaint = new Paint();
        markerBoxPaint.setStyle(Paint.Style.FILL);
        markerBoxPaint.setColor (Color.BLACK);

        markerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerShadowPaint.setStrokeWidth(5);
        markerShadowPaint.setStyle(Paint.Style.STROKE);
        markerShadowPaint.setColor(Color.parseColor("#66000000"));

        bottomShadow = new Paint();
        bottomShadow.setStrokeWidth(1);
        bottomShadow.setStyle(Paint.Style.FILL_AND_STROKE);

        roadPaint = new Paint();
        roadPaint.setColor(Color.parseColor("#393939"));
    }

    private void initTypedArray(Context context, AttributeSet attrs)
    {

    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        final int widthSize = w;
        final int heightSize = h;

        int m_Height = Math.max(100, heightSize);
        int m_Width = Math.max(100, widthSize);

        int bikeLength = m_Height / 3;
        int bikeWidth = bikeLength / 4;

        //fullScaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bike_road_temp);
        //pingBitmap = getResizedBitmap(fullScaledBitmap, w, h);

        int m_X = m_Width / 2 - bikeWidth / 2;
        int m_Y = m_Height / 4;
        int right = m_Width / 2 + bikeWidth / 2;
        int bottom = m_Height * 3 / 4;
        mBikeRect.set(m_X, m_Y, right, bottom);

        mForegroundPaint.setStrokeWidth(m_Height / 150);

        int mPadding = m_Width / 20;

        Log.e(TAG, "On Size Changed Measure Called ...w=" + w + "  " + h);

        float mBikeX = mBikeRect.centerX();// - fullScaledBitmap.getWidth();

        topHalfSize = h * .53f;
        bottomHalfSize = h * .47f;

        topShadowPaint.setShader(new LinearGradient(0, 0, w,  0,  Color.parseColor("#0A0A0A"), Color.parseColor ("#222222"), Shader.TileMode.CLAMP));
        bottomShadow.setShader(new LinearGradient(0, h, 0, topHalfSize +2,  Color.parseColor ("#00000000"), Color.parseColor("#aa000000"), Shader.TileMode.CLAMP));

        roadPaint.setDither(true);
        // Note:  blurring may or may not work depending on OS/Hardware integration
        roadPaint.setMaskFilter(new BlurMaskFilter(w, BlurMaskFilter.Blur.NORMAL));

        float w1 = w / 20;
        float w2 = w * .4f;

        leftRoadPath.reset();
        leftRoadPath.moveTo (w1, h);
        leftRoadPath.lineTo (w2, topHalfSize +1);
        leftRoadPath.lineTo (w2-2, topHalfSize +1);
        leftRoadPath.lineTo (w1-10, h);

        leftCornerGrayTriangle.reset();
        leftCornerGrayTriangle.moveTo(0, h);
        leftCornerGrayTriangle.lineTo(0, topHalfSize);
        leftCornerGrayTriangle.lineTo (w2, topHalfSize);
        leftCornerGrayTriangle.lineTo(w/20, h);
        leftCornerGrayTriangle.close();

        w1 = w *.6f;
        w2 = w*19/20;
        rightCornerGrayTriangle.reset();
        rightCornerGrayTriangle.moveTo(w, h);
        rightCornerGrayTriangle.lineTo(w, topHalfSize);
        rightCornerGrayTriangle.lineTo (w1, topHalfSize);
        rightCornerGrayTriangle.lineTo(w2, h);
        rightCornerGrayTriangle.close();

        rightRoadPath.reset();
        rightRoadPath.moveTo (w2, h);
        rightRoadPath.lineTo (w1, topHalfSize +1);
        rightRoadPath.lineTo (w1+2, topHalfSize +1);
        rightRoadPath.lineTo (w2+10, h);
        rightRoadPath.close();

        yellowLinePath.reset();
        yellowLinePath.moveTo (w/2-10, h);
        yellowLinePath.lineTo (w/2-2, topHalfSize +1);
        yellowLinePath.lineTo (w/2+2, topHalfSize +1);
        yellowLinePath.lineTo (w/2+10, h);
        yellowLinePath.close();

        boxRect.left = 0;
        boxRect.top = 0;
        boxRect.right = boxWidth;
        boxRect.bottom = boxHeight;

        float carrotHeight = boxHeight - boxBottom;
        float carrotHalfWidth = carrotHeight * .45f;

        float carrotPosition = boxWidth * .75f;
        markerPath.moveTo (carrotPosition, boxHeight);
        markerPath.lineTo (carrotPosition-carrotHalfWidth, boxBottom);
        markerPath.lineTo (0, boxBottom);
        markerPath.lineTo (0, 0);
        markerPath.lineTo (boxWidth, 0);
        markerPath.lineTo (boxWidth, boxBottom);
        markerPath.lineTo (carrotPosition+carrotHalfWidth, boxBottom);
        markerPath.close();
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        // draw entire background in roadpaint
        canvas.drawRect (0, 0, canvas.getWidth(), canvas.getHeight(), roadPaint);
        //lastBitmapY += 1;
        //if (lastBitmapY > pingBitmap.getHeight()) lastBitmapY = 0;

        // draw top 1/2 of widget with dark gradient
        canvas.drawRect (0, 0, getWidth(), topHalfSize, topShadowPaint);

        // draw lower left triangle gradient
        canvas.drawPath (leftCornerGrayTriangle, bottomShadowPaint);
        // draw lower right triangle gradient
        canvas.drawPath (rightCornerGrayTriangle, bottomShadowPaint);
        // draw white-ish road lines
        canvas.drawPath (rightRoadPath, whiteRoadLinePaint);
        canvas.drawPath (leftRoadPath, whiteRoadLinePaint);

        // draw center yellow line
        canvas.drawPath (yellowLinePath, yellowRoadLinePaint);

        float marker1 =  getHeight() - ((nextDistanceMarker - mTraveledDistance)* 10 * getHeight());

        float ghostLine =  getHeight() - ((mGhostTraveledDistance - mTraveledDistance)* 10 * getHeight());

//        String debugm1 = String.format("%.1f", marker1);
//        canvas.drawText (debugm1,getWidth()/3, marker1, markerTextPaint);

//        Log.d (TAG, "half width starts " + getHeight()/2 + "-" + getHeight());
//        Log.d (TAG, "nextDistanceMarker=" + nextDistanceMarker + "  traveledDistance=" + mTraveledDistance);
//        Log.d (TAG, "calculation next - current / height =" + marker1);

        // place shadow gradient over bottom half of widget
        canvas.drawRect (0, topHalfSize, getWidth(), getHeight(), bottomShadow);

        if (ghostLine >= topHalfSize) {
            drawPaceLine(canvas, ghostLine, redLinePaint);
        }

        if (marker1 >= topHalfSize)
        {
            drawRoadDistanceLine (canvas, marker1);
        }

//        String debugText = String.format ("%.2f", mTraveledDistance);
//        canvas.drawText (debugText,getWidth()/2, getHeight()/2, markerTextPaint);
//        debugText = String.format ("%.2f", nextDistanceMarker);
//        canvas.drawText (debugText,getWidth()/2, getHeight()/3, markerTextPaint);
//        debugText = String.format ("%.2f", nextNextDistanceMarker);
//        canvas.drawText (debugText,getWidth()/2, getHeight()/4, markerTextPaint);
    }

    private void drawPaceLine(Canvas canvas, float yOffset, Paint painter)
    {
        float sideMargin1 = getWidth() / 20;
        float sideMargin2 = getWidth() * .40f;

        float wideRoadWidth = getWidth() - 2 * sideMargin1;
        float narrowRoadWidth = getWidth() - 2 * sideMargin2;

        float lineWidth = (wideRoadWidth-narrowRoadWidth) * ((yOffset - topHalfSize) / bottomHalfSize) + narrowRoadWidth;

        float startx = getWidth() - lineWidth;
        startx /= 2f;
        float endx = startx + lineWidth;

        canvas.drawLine (startx, yOffset, endx, yOffset, painter);
    }

    private void drawRoadDistanceLine(Canvas canvas, float yOffset)
    {
        float sideMargin1 = getWidth() / 20;
        float sideMargin2 = getWidth() * .39f;

        float wideRoadWidth = getWidth() - 2 * sideMargin1;
        float narrowRoadWidth = getWidth() - 2 * sideMargin2;

        float lineWidth = (wideRoadWidth-narrowRoadWidth) * ((yOffset - topHalfSize)/ bottomHalfSize) + narrowRoadWidth;

        float startx = getWidth() - lineWidth;
        startx /= 2f;
        float endx = startx + lineWidth;

        float sizeFactor = lineWidth / wideRoadWidth;

        float yLine = yOffset;
        float xLine = startx-100*sizeFactor;

        float carrotHeight = boxHeight/3 * sizeFactor;
        float boxBottom = yOffset-carrotHeight;
        float boxTop = boxBottom - boxHeight * sizeFactor;
        float boxLeft = startx - boxWidth * sizeFactor;
        float boxRight = boxLeft + boxWidth * sizeFactor;

        canvas.drawLine (boxLeft, yOffset, boxRight, yOffset, markerShadowPaint);

        blackLinePaint.setStrokeWidth(yLine/getHeight());
        canvas.drawLine (xLine, yLine, endx, yLine, blackLinePaint);

        resizeRect.left = (int) boxLeft;
        resizeRect.top = (int) boxTop;
        resizeRect.right = (int) boxRight;
        resizeRect.bottom = (int) boxBottom;
        canvas.drawBitmap(markerBitmap1, boxRect, resizeRect, null);
    }

    private float setTextSizeForWidth (Paint paint, float desiredWidth, float desiredHeight, String text)
    {
        // Pick a reasonably large value for the test. Larger values produce
        // more accurate results, but may cause problems with hardware
        // acceleration. But there are workarounds for that, too; refer to
        // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
        final float testTextSize = 160f;

        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize (testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds (text, 0, text.length(), bounds);

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSizeW = testTextSize * desiredWidth / bounds.width();
        float desiredTextSizeH = testTextSize * desiredHeight / bounds.height();

        float textSize = Math.min (desiredTextSizeW, desiredTextSizeH);

        //textSize = (int) ((textSize/2) * 2);

        // Set the paint for that size.
        paint.setTextSize (textSize);
        markerTextPaint2.setTextSize( textSize*.5f);

        return textSize;
    }

    public void updateWidget(float distance, float timeInSeconds)
    {
        // The next check handles when the user switches between imperial and metric (miles <--> Km)
        if (distance < mTraveledDistance || mTraveledDistance + .1 < distance)
        {
            mTraveledDistance = distance;
            nextDistanceMarker = (int)(mTraveledDistance * 10) / 10;
            nextNextDistanceMarker = nextDistanceMarker + .1f;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(mTraveledDistance, distance);

        animator.setInterpolator(new LinearInterpolator());

        animator.setDuration(1000);

        animator.addUpdateListener(animation ->
        {
            float value = (float) animation.getAnimatedValue();
            long playTime = animation.getCurrentPlayTime();

            mTraveledDistance = value;
            mCurrentElapsedTime = timeInSeconds + playTime/1000f;
            mGhostTraveledDistance = mGhostKPH  * mCurrentElapsedTime/3600;

            if (mTraveledDistance > nextDistanceMarker)
            {
                nextDistanceMarker = nextNextDistanceMarker;
                nextNextDistanceMarker += .1f;

                String text = String.format(Locale.ENGLISH,"%.1f", nextDistanceMarker);
                // The XX is to force the measurment routine to calculate a width for both KM and MI in a mono-spaced font like way
                String measureText = String.format(Locale.ENGLISH,"%.1f XX", mTraveledDistance);

                float fontSize = setTextSizeForWidth (markerTextPaint,  boxWidth, boxHeight, measureText);
                markerCanvas1.drawColor(Color.parseColor("#00000000"));
                markerCanvas1.drawPath (markerPath, markerBoxPaint);
                markerCanvas1.drawPath (markerPath, markerBoxLinePaint);
                markerCanvas1.drawText(text, boxWidth * .66f + 1, boxBottom - fontSize/3f + 2, markerTextPaint);
                markerCanvas1.drawText(unitString, boxWidth * .66f + 2, boxBottom - fontSize/3f + 2, markerTextPaint2);

                text = String.format(Locale.ENGLISH,"%.1f", nextNextDistanceMarker);
                measureText = String.format(Locale.ENGLISH, "%.1f %s", mTraveledDistance, unitString);

                fontSize = setTextSizeForWidth (markerTextPaint,  boxWidth, boxHeight, measureText);

                markerCanvas2.drawColor(Color.parseColor("#00000000"));
                markerCanvas2.drawPath (markerPath, markerBoxPaint);
                markerCanvas2.drawPath (markerPath, markerBoxLinePaint);
                markerCanvas2.drawText(text, boxWidth * .6f + 10, boxBottom - fontSize/3f + 2, markerTextPaint);
                markerCanvas2.drawText(unitString, boxWidth * .6f + 11, boxBottom - fontSize/3f + 2, markerTextPaint2);
            }

            post(() -> invalidate());
        });

        animator.start();
    }

    public void changeUnits (String unitString, float currentDistanceInNewUnits, float ghostSpeedInNewUnits)
    {
        this.unitString = unitString;
        mGhostKPH = ghostSpeedInNewUnits;
        mTraveledDistance = currentDistanceInNewUnits;
        nextDistanceMarker = (int)(mTraveledDistance * 10) / 10;
        nextNextDistanceMarker = nextDistanceMarker + .1f;
    }

//    public void setUnitString (String unitString)
//    {
//        this.unitString = unitString;
//    }
//    public void setGhostSpeedInKPH (float ghostKPH) { mGhostKPH = ghostKPH;}
}

