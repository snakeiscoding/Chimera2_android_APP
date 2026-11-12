package com.jht.chimera.lib.widget.slider;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import com.jht.chimera.R;
import com.jht.chimera.lib.widget.ScrollViewNoTouch;
import com.jht.chimera.lib.workout.workoutparameters.MinMaxDictionary;

import java.util.Locale;

/**
 * A general slider to handle inputs from the user.
 * <p>
 * Created by alex.nguyen on 9/19/2017.
 */
public class SliderWidget extends RelativeLayout {
    final int DEFAULT_WIDTH = 80 + 94;
    final int DEFAULT_WIDTH_SLIM = 94;

    float mRangeMin = 0.0f;
    float mRangeMax = 100.0f;
    float mStepSize = 0.1f;
    int mDisplayPrecision = 1;
    String mCaptionText = "";
    String mUnitText = "";
    int mExtraTextSpacePx = 0;
    boolean mIsSlimline;
    float mValue = mRangeMin;

    IValueChangedListener mValueChangedListener;
    IKeypadLaunchListener mKeypadLaunchListener;

    int mBackgroundColor = Color.BLACK;
    int mHighlightColor = 0xfff2a61a;
    int mBorderColor = 0x50808080;

    float mWidth, mHeight;

    ScrollView mScrollView;
    VerticalSeekBar mSeekBar;

    AppCompatImageButton mPlusButton;
    AppCompatImageButton mMinusButton;
    TextView mSliderValueText;
    private View mSliderNumberButton;

    Handler mHandler = new Handler();
    boolean mOnSizeCalled = false;

    // Subclass this interface to be notified when the slider's value changes.
    public interface IValueChangedListener {
        void onValueChanged(float value);
    }

    // Subclass this interface to be notified when the slider's keypad widget is pressed.
    public interface IKeypadLaunchListener {
        void onKeypadLaunch(SliderWidget slider);
    }

    public SliderWidget(Context context) {
        super(context);
        init(context);
    }

    public SliderWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SliderWidget, 0, 0);
        for (int i = 0; i < ta.getIndexCount(); ++i) {
            int attr = ta.getIndex(i);
            if (attr == R.styleable.SliderWidget_captionText) {
                mCaptionText = ta.getString(attr);
            } else if (attr == R.styleable.SliderWidget_rangeMin) {
                mRangeMin = ta.getFloat(attr, mRangeMin);
            } else if (attr == R.styleable.SliderWidget_rangeMax) {
                mRangeMax = ta.getFloat(attr, mRangeMax);
            } else if (attr == R.styleable.SliderWidget_quantizeStepSize) {
                mStepSize = ta.getFloat(attr, mStepSize);
            } else if (attr == R.styleable.SliderWidget_highlightColor) {
                mHighlightColor = ta.getColor(attr, mHighlightColor);
            } else if (attr == R.styleable.SliderWidget_borderColor) {
                mBorderColor = ta.getColor(attr, mBorderColor);
            } else if (attr == R.styleable.SliderWidget_displayPrecision) {
                mDisplayPrecision = ta.getInt(attr, mDisplayPrecision);
            } else if (attr == R.styleable.SliderWidget_extraTextSpace) {
                mExtraTextSpacePx = (int) ta.getDimension(attr, mExtraTextSpacePx);
            } else if (attr == R.styleable.SliderWidget_isSlimline) {
                mIsSlimline = ta.getBoolean(attr, mIsSlimline);
            }
        }
        ta.recycle();

        init(context);
    }

    /**
     * @param context inflate the context for the Slider
     */
    private void init(final Context context) {
        // Inflate the layout.
        inflate(context, R.layout.slider_widget, this);

        mScrollView = (ScrollViewNoTouch) findViewById(R.id.sliderScrollView);
        mSliderValueText = findViewById(R.id.sliderValueText);
        mSeekBar = findViewById(R.id.sliderSeekbar);
        mSeekBar.setMax(1000);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = mRangeMax - mRangeMin; // Get the range
                value = progress * value / seekBar.getMax(); // Get the progress into the range
                value = Math.round(value / mStepSize) * mStepSize; // Round to closest step size
                value += mRangeMin; // Add the min range to get valid range value
                setValue(value, true);
            }

            /**
             * @param seekBar The SeekBar in which the touch gesture began
             */
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            /**
             * @param seekBar The SeekBar in which the touch gesture began
             */
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mSeekBar.setOnReleaseListener(() -> {
            if (mValueChangedListener != null) mValueChangedListener.onValueChanged(mValue);
        });

        mPlusButton = findViewById(R.id.sliderPlusButton);
        mPlusButton.setOnClickListener(v -> {
            setValue(Math.min(mValue + mStepSize, mRangeMax));
            if (mValueChangedListener != null) mValueChangedListener.onValueChanged(mValue);
        });

        mMinusButton = findViewById(R.id.sliderMinusButton);
        mMinusButton.setOnClickListener(v -> {
            setValue(Math.max(mValue - mStepSize, mRangeMin));
            if (mValueChangedListener != null) mValueChangedListener.onValueChanged(mValue);
        });

        mSliderNumberButton = findViewById(R.id.sliderNumberButton);

        setValue(mRangeMin);
        if (mIsSlimline) mScrollView.setVisibility(View.GONE);
    }

    private void setProgressDrawable() {
        ShapeDrawable sliderBlackBorder = new ShapeDrawable(new RectShape());
        sliderBlackBorder.setIntrinsicHeight((int) mWidth);
        sliderBlackBorder.getPaint().setColor(mBackgroundColor);

        ShapeDrawable sliderWhite = new ShapeDrawable(new RectShape());
        sliderBlackBorder.setIntrinsicHeight((int) mWidth);
        sliderWhite.getPaint().setColor(Color.WHITE);

        ShapeDrawable sliderBlackBorder2 = new ShapeDrawable(new RectShape());
        sliderBlackBorder.setIntrinsicHeight((int) mWidth);
        sliderBlackBorder2.getPaint().setColor(mBackgroundColor);

        ShapeDrawable sliderOrange = new ShapeDrawable(new RectShape());
        sliderBlackBorder.setIntrinsicHeight((int) mWidth);
        sliderOrange.getPaint().setColor(mHighlightColor);

        BitmapDrawable markerDrawable = new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.sliderwidget_backplate_markers_horizontal));
        markerDrawable.setGravity(Gravity.FILL);

        Drawable[] progressMarkArray = {sliderBlackBorder, sliderWhite, markerDrawable, sliderBlackBorder2, sliderOrange};

        LayerDrawable progressMarkLayers = new LayerDrawable(progressMarkArray);
        progressMarkLayers.setLayerInset(0, 0, getXPercent(11), 0, getXPercent(11));
        progressMarkLayers.setLayerInset(1, 0, getXPercent(12), 0, getXPercent(12));
        progressMarkLayers.setLayerInset(3, 0, getXPercent(41), 0, getXPercent(41));
        progressMarkLayers.setLayerInset(4, 0, getXPercent(42), 0, getXPercent(42));

        ClipDrawable progressDrawable = new ClipDrawable(progressMarkLayers, Gravity.START, ClipDrawable.HORIZONTAL);
        mSeekBar.setProgressDrawable(progressDrawable);
    }

    private void setThumbSize() {
        ShapeDrawable thumbSize = new ShapeDrawable(new RectShape());
        thumbSize.setIntrinsicWidth(getXPercent(88));
        thumbSize.setIntrinsicHeight(getXPercent(94));
        thumbSize.getPaint().setColor(Color.TRANSPARENT);

        Drawable[] thumbArray = {thumbSize, ContextCompat.getDrawable(getContext(), R.drawable.sliderwidget_knob_drawable)};

        LayerDrawable thumbLayers = new LayerDrawable(thumbArray);
        mSeekBar.setThumb(thumbLayers);
    }

    /**
     * Get percent of Slider Widget bar.
     * @param pixelValue width of Slider Widget bar
     * @return integer value of width
     */
    private int getXPercent(int pixelValue) {
        if (mIsSlimline) return Math.round(mWidth * pixelValue / DEFAULT_WIDTH_SLIM);
        return Math.round(mWidth * pixelValue / DEFAULT_WIDTH);
    }

    /**
     * Set value to specified float.
     * @param value to set the Slider Widget to
     */
    public void setValue(float value) {
        setValue(value, false);
    }

    /**
     * Set value to specified float IF its not the same as mValue and the listener isn't false.
     * @param value to be set to
     * @param triggerListener whether to activate the listener
     */
    public void setValue(float value, boolean triggerListener) {
        if (mValue == value && triggerListener) return;
        mValue = value;
        if (!mOnSizeCalled) return;
        mSliderValueText.setText(String.format(Locale.ENGLISH, "%.0" + mDisplayPrecision + "f", mValue));
        int progress = Math.round((mValue - mRangeMin) / (mRangeMax - mRangeMin) * mSeekBar.getMax());
        mSeekBar.setProgress(progress);
        mScrollView.scrollTo(0, (mSeekBar.getHeight() - getXPercent(10)) * mSeekBar.getProgress() / mSeekBar.getMax());
        if (triggerListener && mValueChangedListener != null)
            mValueChangedListener.onValueChanged(mValue);
    }

    /**
     * Set range based on given min/max.
     * @param min minimum of range
     * @param max maximum of range
     */
    public void setRange(float min, float max) {
        mRangeMin = min;
        mRangeMax = max;
    }

    public void setQuantizationStepSize(float stepSize) {
        mStepSize = stepSize;
    }

    public void setHighlightColor(int color) {
        mHighlightColor = color;
        setProgressDrawable();
    }

    public void setDisplayedValuePrecision(int precision) {
        mDisplayPrecision = precision;
    }

    public void setCaptionText(String captionText) {
        mCaptionText = captionText;
        ((TextView) findViewById(R.id.sliderBarCaption)).setText(mCaptionText);
    }

    public void setUnitText(String unitText) {
        mUnitText = unitText;
    }

    /**
     * @param valueChangedListener specifies the listener for when a change occurs
     */
    public void setOnValueChangedListener(IValueChangedListener valueChangedListener) {
        mValueChangedListener = valueChangedListener;
    }

    /**
     * @param keypadLaunchListener listener for when keypad is opened
     */
    public void setKeypadWidgetTouchedListener(IKeypadLaunchListener keypadLaunchListener) {
        mKeypadLaunchListener = keypadLaunchListener;
    }

    public float getValue() {
        return mValue;
    }

    public float getMin() {
        return mRangeMin;
    }

    public float getMax() {
        return mRangeMax;
    }

    public float getQuantizationStepSize() {
        return mStepSize;
    }

    public int getDisplayedValuePrecision() {
        return mDisplayPrecision;
    }

    public String getUnitString() {
        return mUnitText;
    }

    private MinMaxDictionary createMinMaxDictionary() {
        MinMaxDictionary unit = new MinMaxDictionary();
        unit.min = mRangeMin;
        unit.max = mRangeMax;
        unit.stepSize = mStepSize;
        unit.unitString = mUnitText;
        unit.captionText = mCaptionText;
        unit.precision = mDisplayPrecision;
        return unit;
    }

    /**
     * @param w Current width of this view.
     * @param h Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        setProgressDrawable();
        setThumbSize();
        setClipChildren(false);
        ViewParent parent = getParent();
        if (parent != null) {
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).setClipChildren(false);
            }
        }
        if (!mOnSizeCalled) {
            mOnSizeCalled = true;
            setValue(mValue);
        }
    }
}