package com.jht.chimera.ui.mainapp.goscreen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;

import java.util.Locale;

/**
 * This keypad is used for Sprint 8 run screen only.  This must not be used for any other keypad without
 * a good reason.  There is a keypad in the android UI widgets that handles all of the edge cases.  That
 * is the default keypad that should be used everywhere.
 */
public class SimpleKeypadView extends ConstraintLayout {

    private static final float TOLERANCE = 0.00005f;
    private static final int SET_TO_VALID_VALUE_DELAY = 1500;
    private static char DOT = '\u2022';

    private TextView mValueTextView;
    private TextView mLabelTextView;
    private SparseArray<Button> mKeys = new SparseArray<>();
    private View mConfirmButton;
    private ImageView mConfirmIcon;
    private View mBackButton;

    public IKeypadPanelCallback mCallback;
    private Handler mRoundHandler = new Handler();
    private Handler mAutoHideHandler = new Handler();

    private String mStringInput = "";
    private String mLabel;
    private int mPrecision = 0;
    private int mMaxDigits = 0;
    private float mMin;
    private float mMax;
    private float mStepSize;
    private float mInitialValue;
    private boolean mIsInitialValue = false;
    private boolean mResetInitialValue = false;
    private AlphaAnimation alphaAnimation;
    private boolean mIsPasscode = false;

    public SimpleKeypadView(Context context) {
        super(context);
    }

    public SimpleKeypadView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public interface IKeypadPanelCallback {
        void keypadValueConfirmed(float value);
        void onAutoHide();
    }

    public void initialize(Context context, int layoutId) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(layoutId, this);

        mKeys.append(0, findViewById(R.id.key0_text_view));
        mKeys.append(1, findViewById(R.id.key1_text_view));
        mKeys.append(2, findViewById(R.id.key2_text_view));
        mKeys.append(3, findViewById(R.id.key3_text_view));
        mKeys.append(4, findViewById(R.id.key4_text_view));
        mKeys.append(5, findViewById(R.id.key5_text_view));
        mKeys.append(6, findViewById(R.id.key6_text_view));
        mKeys.append(7, findViewById(R.id.key7_text_view));
        mKeys.append(8, findViewById(R.id.key8_text_view));
        mKeys.append(9, findViewById(R.id.key9_text_view));

        mConfirmButton = findViewById(R.id.keypad_select_button);
        mConfirmIcon = findViewById(R.id.keypad_select_icon);
        mBackButton = findViewById(R.id.keypad_backspace_button);
        mValueTextView = findViewById(R.id.value_text_view);
        mLabelTextView = findViewById(R.id.label_text_view);

        alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setDuration(250);
        alphaAnimation.setRepeatMode(Animation.REVERSE);
        alphaAnimation.setRepeatCount(5);
        alphaAnimation.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation)
            {

            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                checkResetInitialValue();
                mLabelTextView.setText(mLabel);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }
        });
        mLabelTextView.setText(mLabel);
        setupTouchEvents();
    }

    /**
     * Gives the keypad the backing logic to handle the keypad. Also begins handling touch
     * events for the keypad.
     *
     * @param label        The label for the keypad
     */
    public void setupKeypad(float min, float max, float stepSize, int precision, int maxDigits, String label) {
        mMin = min;
        mMax = max;
        mStepSize = stepSize;
        mPrecision = precision;
        mMaxDigits = maxDigits;
        mLabel = label;
        setUnit(mLabel);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchEvents() {

        for (int i = 0; i < 10; i++) {
            TextView keyView = mKeys.get(i);
            keyView.setOnTouchListener(new KeypadTouchListener(i));
        }

        mBackButton.setOnClickListener(v -> {
            checkResetInitialValue();
            deleteChar();
            startAutoHideTimer();
            startRoundTimer();
        });

        mConfirmButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if(mCallback != null)
                {
                    mCallback.keypadValueConfirmed(getValue());
                }
            }
            return false;
        });
    }

    public void setCallback(IKeypadPanelCallback keypadPanelCallback){
        mCallback = keypadPanelCallback;
    }

    /**
     * Removes the last input character from the number keypad
     */
    public void deleteChar() {
        mIsInitialValue = false;
        if (!mStringInput.isEmpty()) {
            mStringInput = mStringInput.substring(0, mStringInput.length() - 1);
            sendFormattedString(getValue());
            float value = getValue();
            setConfirmEnabled(isValid(value));
        }
    }

    /**
     * Rounds the currently input keypad value to the closest valid value
     */
    public void roundValue() {
        float value = getValue();
        float correctedValue = getClosestStepValue(getValue());
        if (Math.abs(value - correctedValue) >= TOLERANCE)
        {
            String doubleString = formatFloat(correctedValue);
            mStringInput = doubleString.replace(".", "");
            sendFormattedString(correctedValue);
            if(value > mMax){
                setConfirmEnabled(false);
                mResetInitialValue = true;
                mLabelTextView.setText(getContext().getString(R.string.max_lc, getContext()));
            }
            else{
                if(value < mMin){
                    mLabelTextView.setText(getContext().getString(R.string.min_lc, getContext()));
                }
                setConfirmEnabled(true);
                mResetInitialValue = false;
            }
            mIsInitialValue = true;
            mValueTextView.startAnimation(alphaAnimation);
        }
    }

    /**
     * Sets a value that appears at the beginning before the changing the value
     *
     * @param value The initial value of the keypad
     */
    public void setInitialValue(float value) {
        mInitialValue = getClosestStepValue(value);
        mIsInitialValue = true;
        String number = formatFloat(mInitialValue);
        StringBuilder numberValue = new StringBuilder();

        for (char character : number.toCharArray()) {
            if (Character.isDigit(character)) {
                numberValue.append(character);
            }
        }

        mStringInput = numberValue.toString();
        setConfirmEnabled(isValid(mInitialValue));
        sendFormattedString(mInitialValue);
    }

    public void setUnit(String unit){
        if(mLabelTextView != null){
            mLabelTextView.setText(unit);
        }
    }

    private class KeypadTouchListener implements OnTouchListener {
        private int mNumber;

        KeypadTouchListener(int number) {
            mNumber = number;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                checkResetInitialValue();
                inputNumber(mNumber);
                startAutoHideTimer();
                startRoundTimer();
            }
            return false;
        }
    }

    /**
     * Starts a task that updates the keypad to the closest valid value
     */
    private void startRoundTimer() {
        mRoundHandler.removeCallbacksAndMessages(null);
        if (!isValid(getValue())) {
            mRoundHandler.postDelayed(this::roundValue, SET_TO_VALID_VALUE_DELAY);
        }
    }

    private void startAutoHideTimer() {
        mAutoHideHandler.removeCallbacksAndMessages(null);
        mAutoHideHandler.postDelayed(() -> {
            if(mCallback != null){
                mCallback.onAutoHide();
            }
        }, 10000);
    }

    /**
     * Should be called when a digit is entered into the keypad. Only accepts 0-9.
     *
     * @param digit The number from 0 through 9
     */
    private void inputNumber(int digit) {
        if (mIsInitialValue) {
            mStringInput = "";
            mIsInitialValue = false;
        }
        if (digit >= 0 && digit <= 9) {
            // Remove the first 0 digit so that we can add in values after the initial number
            if (mStringInput.length() >= mMaxDigits && mStringInput.length() > 0 && mStringInput.charAt(0) == '0') {
                mStringInput = mStringInput.substring(1);
            }

            if (mStringInput.length() < mMaxDigits) {
                mStringInput += String.valueOf(digit);
                float value = getValue();
                setConfirmEnabled(isValid(value));
                sendFormattedString(value);
            }
        }
    }

    /**
     * Gets the value of the current keypad input
     *
     * @return The value of the current keypad input
     */
    private float getValue() {
        if (mStringInput.isEmpty()) {
            return 0.0f;
        } else {
            // Add the decimal to the string. Add zeros at the beginning to avoid issues with short string values
            StringBuilder builder = new StringBuilder(mStringInput);
            builder.insert(0, "00000");
            builder.insert(builder.length() - mPrecision, ".");
            String valueString = builder.toString();
            return Float.parseFloat(valueString);
        }
    }

    private void sendFormattedString(float value) {
        String stringValue = formatFloat(value);
        mValueTextView.setText(stringValue);
    }

    private String formatFloat(float floatValue) {
        String formatStr = "%.0" + mPrecision + "f"; //NON-NLS
        String resultStr;

        if (0 == mPrecision) {
            resultStr = String.format(Locale.US, "%d", (int) (floatValue + 0.5));
        } else {
            resultStr = String.format(Locale.US, formatStr, floatValue);
        }

        if (mIsPasscode) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < resultStr.length(); i++) {
                stringBuilder.append(DOT);
            }
            resultStr = stringBuilder.toString();
        }

        return resultStr;
    }

    private boolean isValid(float value)
    {
        return value >= (mMin - TOLERANCE) && value <= (mMax + TOLERANCE) && (Math.abs(value - getClosestStepValue(value)) < TOLERANCE);
    }

    private float getClosestStepValue(float value)
    {
        if (value > mMax)
        {
            return mMax;
        }
        else if (value < mMin)
        {
            return mMin;
        }
        else
        {
            return Math.round(value/mStepSize) * mStepSize;
        }
    }

    private void setConfirmEnabled(boolean enabled){
        mConfirmButton.setEnabled(enabled);
        mConfirmIcon.setEnabled(enabled);
    }

    private void stopTimers(){
        mAutoHideHandler.removeCallbacksAndMessages(null);
        mRoundHandler.removeCallbacksAndMessages(null);
    }

    private void checkResetInitialValue(){
        if(mResetInitialValue){
            mResetInitialValue = false;
            setInitialValue(mInitialValue);
        }
        mValueTextView.clearAnimation();
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        startAutoHideTimer();
    }

    @Override
    protected void onDetachedFromWindow()
    {
        mLabelTextView.setText(mLabel);
        super.onDetachedFromWindow();
        stopTimers();
    }

    public void setPasscode(boolean isPasscode) {
        mIsPasscode = isPasscode;
    }
}
