/**
 * Johnson Health Tech
 * 1600 Landmark Drive
 * Cottage Grove, WI  53527
 * <p>
 * Copyright (C) 2015
 * Proprietary and confidential
 *
 * @author Blong.Thor
 */
package com.jht.chimera.lib.widget.dial;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.jht.chimera.R;

import java.util.Locale;

public class JHTDialView extends RelativeLayout {
    int mDialBackgroundRes = R.drawable.dial_background_circle;
    int mDialFillRes = R.drawable.dial_level_fill;
    int mDialFill4Res = R.drawable.dial_level_fill_4;
    int mDialFill3Res = R.drawable.dial_level_fill_3;
    int mDialFill2Res = R.drawable.dial_level_fill_2;
    int mDialFill1Res = R.drawable.dial_level_fill_1;
    int mDialNeedleRes = R.drawable.dial_icon_needle;
    int mDialMinMaxMarkerRes = 0;
    int mDialTargetZoneRes = R.drawable.dial_target_fill;

    TextView mValueText;
    View mDialFill = null;
    Drawable mFillBg = null;

    Paint mFillClearPaint;
    View mTargetFill = null;
    View mNeedle = null;

    final float MAX_SWEEP_ANGLE = 298.0f;
    float mCurrentAngle = 0.0f;
    Thread mAnimateLevelThread = null;

    float mMin = 0.0f;
    float mMax = 1.0f;
    int mPrecision = 0;
    float mTrgetZoneStartValue = 0.0f;
    float mTrgetZoneStopValue = 0.0f;
    String mPreviousValue = "";

    ValueAnimator mAnimatorRotateFill = new ValueAnimator();
    int mCurrentFillBg = 0;
    final float DIAL_FILL_ZERO_OFFSET = 62.0f;

    private AnimatorUpdateListener mFillUpdateListener = animation -> {
        Float angle = (Float) animation.getAnimatedValue();
        if (angle < 0.0f) {
            angle = 0.0f;
        } else if (angle > MAX_SWEEP_ANGLE) {
            angle = MAX_SWEEP_ANGLE;
        }

        if (angle >= 240.0f && mCurrentFillBg != mDialFillRes) {
            mFillBg = ContextCompat.getDrawable(getContext(), mDialFillRes).getCurrent();
            mDialFill.setBackground(mFillBg);
            mCurrentFillBg = mDialFillRes;
        } else if (angle >= 180.0f && angle < 240.0f && mCurrentFillBg != mDialFill4Res) {
            mFillBg = ContextCompat.getDrawable(getContext(), mDialFill4Res).getCurrent();
            mDialFill.setBackground(mFillBg);
            mCurrentFillBg = mDialFill4Res;
        } else if (angle >= 120.0f && angle < 180.0f && mCurrentFillBg != mDialFill3Res) {
            mFillBg = ContextCompat.getDrawable(getContext(), mDialFill3Res).getCurrent();
            mDialFill.setBackground(mFillBg);
            mCurrentFillBg = mDialFill3Res;
        } else if (angle >= 60f && angle < 120.0f && mCurrentFillBg != mDialFill2Res) {
            mFillBg = ContextCompat.getDrawable(getContext(), mDialFill2Res).getCurrent();
            mDialFill.setBackground(mFillBg);
            mCurrentFillBg = mDialFill2Res;
        } else if (angle >= 0.0f && angle < 60.0f && mCurrentFillBg != mDialFill1Res) {
            mFillBg = ContextCompat.getDrawable(getContext(), mDialFill1Res).getCurrent();
            mDialFill.setBackground(mFillBg);
            mCurrentFillBg = mDialFill1Res;
        }

        mDialFill.setRotation(angle + DIAL_FILL_ZERO_OFFSET);
        setAngleNeedle(angle);
    };

    public JHTDialView(Context context, AttributeSet attrs) {
        super(context, attrs);

        View.inflate(context, R.layout.jhtdial_layout, this);

        View background = findViewById(R.id.jhtDialBackground);
        mDialFill = findViewById(R.id.jhtDialFill);
        mTargetFill = findViewById(R.id.jhtDialTargetFill);
        mNeedle = findViewById(R.id.jhtDialNeedle);
        mValueText = findViewById(R.id.jhtDialNumericValue);

        mFillClearPaint = new Paint();
        mFillClearPaint.setAntiAlias(true);
        mFillClearPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));

        mAnimatorRotateFill.addUpdateListener(mFillUpdateListener);

        // xml config
        float size = 22.0f;
        int color = Color.BLACK;
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.JHTDial, 0, 0);

        for (int i = 0; i < ta.getIndexCount(); i++) {
            int attr = ta.getIndex(i);
            if (attr == R.styleable.JHTDial_dial_background) {
                mDialBackgroundRes = ta.getResourceId(attr, R.drawable.dial_background_circle);
                background.setBackgroundResource(mDialBackgroundRes);
                // Resize the dial
                ViewGroup dial = findViewById(R.id.jhtDial);
                ViewGroup.LayoutParams lp = dial.getLayoutParams();
                lp.width = ContextCompat.getDrawable(getContext(), mDialBackgroundRes).getMinimumWidth();
                lp.height = ContextCompat.getDrawable(getContext(), mDialBackgroundRes).getMinimumHeight();
                dial.setLayoutParams(lp);
            } else if (attr == R.styleable.JHTDial_dial_fill) {
                mDialFillRes = ta.getResourceId(attr, R.drawable.dial_level_fill);
            } else if (attr == R.styleable.JHTDial_dial_targetZone) {
                mDialTargetZoneRes = ta.getResourceId(attr, 0);
                if (0 != mDialTargetZoneRes) {
                    mTargetFill.setVisibility(View.VISIBLE);
                    mTargetFill.setBackgroundResource(mDialTargetZoneRes);
                }
            } else if (attr == R.styleable.JHTDial_dial_needle) {
                mDialNeedleRes = ta.getResourceId(attr, R.drawable.dial_icon_needle);
                mNeedle.setBackgroundResource(mDialNeedleRes);
            } else if (attr == R.styleable.JHTDial_dial_minMaxMarker) {
                mDialMinMaxMarkerRes = ta.getResourceId(attr, R.drawable.dial_min_max_marker);
                findViewById(R.id.jhtDialMinMaxMarker).setBackgroundResource(mDialMinMaxMarkerRes);
            } else if (attr == R.styleable.JHTDial_dial_targetZoneStartValue) {
                mTrgetZoneStartValue = ta.getFloat(attr, 0.0f);
                setTargetZone(mTrgetZoneStartValue, mTrgetZoneStopValue);
            } else if (attr == R.styleable.JHTDial_dial_targetZoneStopValue) {
                mTrgetZoneStopValue = ta.getFloat(attr, 0.0f);
                setTargetZone(mTrgetZoneStartValue, mTrgetZoneStopValue);
            } else if (attr == R.styleable.JHTDial_dial_numbericTextBottomMargin) {
                int bottom = ta.getInt(attr, 0);
                RelativeLayout numericVg = findViewById(R.id.jhtDialNumeric);
                LayoutParams numericLp = (LayoutParams) numericVg.getLayoutParams();
                numericLp.setMargins(0, 0, 0, bottom);
                numericVg.setLayoutParams(numericLp);
            } else if (attr == R.styleable.JHTDial_dial_numericTextSize) {
                size = ta.getDimension(attr, 30.0f);
                ((TextView) findViewById(R.id.jhtDialNumericValue)).setTextSize(size);
            } else if (attr == R.styleable.JHTDial_dial_numericTextColor) {
                color = ta.getColor(attr, Color.BLACK);
                ((TextView) findViewById(R.id.jhtDialNumericValue)).setTextColor(color);
            } else if (attr == R.styleable.JHTDial_dial_unitLabelText) {
                int labelId = ta.getResourceId(attr, 0);
                ((TextView) findViewById(R.id.jhtDialNumericUnitLabel)).setText(labelId);
            } else if (attr == R.styleable.JHTDial_dial_unitLabelTextSize) {
                size = ta.getDimension(attr, 20.0f);
                ((TextView) findViewById(R.id.jhtDialNumericUnitLabel)).setTextSize(size);
            } else if (attr == R.styleable.JHTDial_dial_unitLabelTextColor) {
                color = ta.getColor(attr, Color.BLACK);
                ((TextView) findViewById(R.id.jhtDialNumericUnitLabel)).setTextColor(color);
            } else if (attr == R.styleable.JHTDial_dial_minLabelValue) {
                mMin = ta.getFloat(attr, 0.0f);
                setMinMaxLabel();
            } else if (attr == R.styleable.JHTDial_dial_maxLabelValue) {
                mMax = ta.getFloat(attr, 0.0f);
                if (mMax < mMin) {
                    mMax = mMin + 1.0f;
                }
                setMinMaxLabel();
            } else if (attr == R.styleable.JHTDial_dial_minMaxLabelTextSize) {
                size = ta.getDimension(attr, 12.0f);
                ((TextView) findViewById(R.id.jhtDialNumericMinLabel)).setTextSize(size);
                ((TextView) findViewById(R.id.jhtDialNumericMaxLabel)).setTextSize(size);
            } else if (attr == R.styleable.JHTDial_dial_minMaxLabelTextColor) {
                color = ta.getColor(attr, Color.BLACK);
                ((TextView) findViewById(R.id.jhtDialNumericMinLabel)).setTextColor(color);
                ((TextView) findViewById(R.id.jhtDialNumericMaxLabel)).setTextColor(color);
            } else if (attr == R.styleable.JHTDial_dial_precisionValue) {
                mPrecision = ta.getInt(attr, 0);
            } else {
            }
        }

        ta.recycle();

        setValue(0.0f);
    }

    public float getMaxSweepAngle() {
        return MAX_SWEEP_ANGLE;
    }

    public void rotateTargetZone(float startAngle, float stopAngle) {
        if (0 <= startAngle && MAX_SWEEP_ANGLE >= startAngle
                && 0 <= stopAngle && MAX_SWEEP_ANGLE >= stopAngle) {
            Bitmap targetFillBitmap = null;
            Bitmap localBitmap = null;
            Bitmap localMutableBitmap = null;
            try {
                stopAngle += 120;
                startAngle += 120;

                float sweep = 360 - (stopAngle - startAngle);

                targetFillBitmap = BitmapFactory.decodeResource(getResources(), mDialTargetZoneRes);
                localBitmap = Bitmap.createBitmap(targetFillBitmap);
                localMutableBitmap = localBitmap.copy(Bitmap.Config.ARGB_8888, true);

                int innerRadius = scanForInnerRadius(localMutableBitmap);
                int boardThickness = scanForBoarderThickness(localMutableBitmap);

                Canvas targetCanvas = new Canvas(localMutableBitmap);
                targetCanvas.setBitmap(localMutableBitmap);

                // set target fill beginning and end boarders
                Paint boarderPaint = new Paint();
                boarderPaint.setStrokeWidth(1.0f);
                boarderPaint.setColor(0xFF38743E);
                boarderPaint.setAntiAlias(true);

                RectF boarderArea = new RectF(boardThickness, boardThickness,
                        localMutableBitmap.getWidth() - boardThickness, localMutableBitmap.getHeight() - boardThickness);

                // draw boarder 1 degree inside the target fill
                targetCanvas.drawArc(boarderArea, stopAngle - 1, sweep + 2, true, boarderPaint);
                targetCanvas.drawCircle(targetFillBitmap.getWidth() / 2,
                        targetFillBitmap.getHeight() / 2,
                        innerRadius, mFillClearPaint);

                // show target fill segment
                int maskThickness = boardThickness - 2;
                RectF maskArea = new RectF(maskThickness, maskThickness,
                        localMutableBitmap.getWidth() - maskThickness, localMutableBitmap.getHeight() - maskThickness);
                targetCanvas.drawArc(maskArea, stopAngle, sweep, true, mFillClearPaint);

                mTargetFill.setBackground(new BitmapDrawable(getResources(), localMutableBitmap));
            }
            finally {
                if(targetFillBitmap != null) { targetFillBitmap.recycle(); }
                if(localBitmap != null) { localBitmap.recycle(); }
                if(localMutableBitmap != null) { localMutableBitmap.recycle(); }
            }
        }
    }

    private void setAngleNeedle(float angle) {
        // use with setFillAngle
        if (0.0f <= angle && MAX_SWEEP_ANGLE >= angle) {
            angle -= 150.0f;

            mNeedle.animate().setDuration(0);
            mNeedle.animate().rotation(angle).start();
        }
    }

    public void setMinMax(float min, float max, int precision) {
        mMin = min;

        if (min < max) {
            mMax = max;
        } else {
            mMax = min + 1.0f; // ensure max is > min
        }

        mPrecision = precision;

        setMinMaxLabel();
        setTargetZone(mTrgetZoneStartValue, mTrgetZoneStopValue);
    }

    public void setUnit(int labelResourceId) {
        TextView unitLabelText = findViewById(R.id.jhtDialNumericUnitLabel);
        unitLabelText.setText(labelResourceId);
    }

    public void setValue(float value) {
        // Normalize value
        float angle = (value - mMin) * (getMaxSweepAngle() / (mMax - mMin));
        animateLevel(mCurrentAngle, angle);

        updateValue(value);
    }

    private void animateLevel(final float startAngle, final float endAngle) {
        // thread
        if (endAngle != mCurrentAngle) {
            mAnimatorRotateFill.setFloatValues(startAngle, endAngle);
            mAnimatorRotateFill.start();

            mCurrentAngle = endAngle;
        }
    }

    public void showNumeric() {
        TextView valueText = findViewById(R.id.jhtDialNumericValue);
        valueText.setVisibility(View.VISIBLE);

        TextView unitLabelText = findViewById(R.id.jhtDialNumericUnitLabel);
        unitLabelText.setVisibility(View.VISIBLE);
    }

    public void hideNumeric() {
        TextView valueText = findViewById(R.id.jhtDialNumericValue);
        valueText.setVisibility(View.GONE);

        TextView unitLabelText = findViewById(R.id.jhtDialNumericUnitLabel);
        unitLabelText.setVisibility(View.GONE);
    }

    public void setTargetZone(float startValue, float stopValue) {
        // Normalize value
        mTrgetZoneStartValue = startValue;
        mTrgetZoneStopValue = stopValue;
        float startAngle = (startValue - mMin) * (getMaxSweepAngle() / (mMax - mMin));
        float stopAngle = (stopValue - mMin) * (getMaxSweepAngle() / (mMax - mMin));
        rotateTargetZone(startAngle, stopAngle);
    }

    public void showTargetZone() {
        mTargetFill.setVisibility(View.VISIBLE);
    }

    public void hideTargetZone() {
        mTargetFill.setVisibility(View.GONE);
    }

    private String formatValue(float value) {
        String valueStr = "";

        if (0 == mPrecision) {
            valueStr = String.format(Locale.US, "%d", (int) (value + 0.5f));
        } else {
            String format = "%.0" + mPrecision + "f"; //NON-NLS
            valueStr = String.format(Locale.US, format, value);
        }

        return valueStr;
    }

    private void setMinMaxLabel() {
        TextView minLabel = findViewById(R.id.jhtDialNumericMinLabel);
        minLabel.setText(formatValue(mMin));

        TextView maxLabel = findViewById(R.id.jhtDialNumericMaxLabel);
        maxLabel.setText(formatValue(mMax));
    }

    private void updateValue(float value) {
        String newValue = formatValue(value);
        if (newValue.compareTo(mPreviousValue) != 0) {
            mValueText.setText(newValue);
            mPreviousValue = newValue;
        }
    }

    private int scanForInnerRadius(Bitmap image) {
        int innerRadius = 0;
        int verticalY = image.getHeight() / 2;
        boolean bFoundFill = false;
        for (int i = 0; i < image.getWidth() / 2; i++) {
            int color = image.getPixel(i, verticalY);
            if (0 != color && false == bFoundFill) {
                bFoundFill = true;
            }

            if (bFoundFill && 0 == color) {
                innerRadius = image.getWidth() / 2 - i;
                break;
            }
        }
        return innerRadius;
    }

    private int scanForBoarderThickness(Bitmap image) {
        int boarder = 0;
        int verticalY = image.getHeight() / 2;
        for (int i = 0; i < image.getWidth() / 2; i++) {
            int color = image.getPixel(i, verticalY);
            if (0 != color) {
                boarder = i;
                break;
            }
        }
        return boarder;
    }
}
