package com.jht.chimera.lib.widget.edittext;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.InputFilter;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.jht.chimera.R;
import com.jht.chimera.lib.widget.selectron.SelectronButton;


/**
 * An edit text that can be validated and display icons and errors for validation.
 * <p>
 * Created by chris.foyer on 2/9/2017.
 */

public class CheckedEditText extends LinearLayout {
    private static final String TAG = "CheckedEditText";
    private EditText mCheckedEditText;
    private TextView mErrorTextView;
    private SelectronButton mStatusImageView;
    private TextView mEndTextView;
    private String mPrefix = "";
    private float mOriginalLeftPadding = -1;

    private ICheckedEditTextEvent mCallback;

    private Handler mHandler;

    public CheckedEditText(Context context) {
        super(context);
        initialize(context);
    }

    public CheckedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public CheckedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        final LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater.inflate(R.layout.checked_edit_text, this);
        mCheckedEditText = findViewById(R.id.checked_edit_box);
        mErrorTextView = findViewById(R.id.error_text_view);
        mStatusImageView = findViewById(R.id.checked_image);
        mEndTextView = findViewById(R.id.checked_edit_end_text_box);
        mHandler = new Handler();

        mCheckedEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (mCallback != null && !hasFocus)
                mCallback.textInput(mCheckedEditText.getText().toString());
        });

        mCheckedEditText.setOnEditorActionListener((v, actionId, event) -> {
            int result = actionId & EditorInfo.IME_MASK_ACTION;
            switch (result) {
                case EditorInfo.IME_ACTION_GO:
                case EditorInfo.IME_ACTION_SEARCH:
                case EditorInfo.IME_ACTION_PREVIOUS:
                case EditorInfo.IME_ACTION_NEXT:
                case EditorInfo.IME_ACTION_DONE:
                    if (mCallback != null)
                        mCallback.textInput(mCheckedEditText.getText().toString());
                    break;
            }
            return false;
        });

        setOnFocusChangeListener((v, hasFocus) -> mCheckedEditText.requestFocus());
    }

    public static final int STATUS_NONE = 0;
    public static final int STATUS_CHECKING = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_SUCCESS_NO_DRAWABLE = 4;
    public static final int STATUS_FAILED_NO_DRAWABLE = 5;

    public void setStatus(final int status) {
        mHandler.post(() -> {
            switch (status) {
                case STATUS_NONE:
                    mStatusImageView.clearAnimation();
                    mStatusImageView.setIcon(0);
                    setEditTextBackground(new ColorDrawable(Color.WHITE));
                    break;
                case STATUS_CHECKING:
                    Drawable bgChecking = ContextCompat.getDrawable(getContext(), R.drawable.check_edit_box_bg_selected);
                    Animation rotateAnimation;
                    rotateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.spinning_clockwise);
                    mStatusImageView.setTint(0xbb999999);
                    mStatusImageView.setIcon(R.drawable.ic_spinner);
                    mStatusImageView.startAnimation(rotateAnimation);
                    setEditTextBackground(bgChecking);
                    break;
                case STATUS_SUCCESS:
                    mStatusImageView.clearAnimation();
                    mStatusImageView.setTint(0xff25bd1a);
                    mStatusImageView.setIcon(R.drawable.ic_check_white);
                    setEditTextBackground(new ColorDrawable(Color.WHITE));
                    mCheckedEditText.setBackgroundColor(Color.WHITE);
                    break;
                case STATUS_FAILED:
                    mStatusImageView.clearAnimation();
                    Drawable bgFailed = ContextCompat.getDrawable(getContext(), R.drawable.check_edit_box_bg_error);
                    setEditTextBackground(bgFailed);
                    mStatusImageView.setTint(0xffff0014);
                    mStatusImageView.setIcon(R.drawable.ic_close_white);
                    break;
                case STATUS_SUCCESS_NO_DRAWABLE:
                    mStatusImageView.clearAnimation();
                    mStatusImageView.setIcon(0);
                    setEditTextBackground(new ColorDrawable(Color.WHITE));
                    mCheckedEditText.setBackgroundColor(Color.WHITE);
                    break;
                case STATUS_FAILED_NO_DRAWABLE:
                    mStatusImageView.clearAnimation();
                    Drawable bgFailedNoDrawable = ContextCompat.getDrawable(getContext(), R.drawable.check_edit_box_bg_error);
                    setEditTextBackground(bgFailedNoDrawable);
                    mStatusImageView.setIcon(0);
                    break;
                default:
                    Log.e(TAG, "Incorrect status was sent to the CheckedEditText");
                    break;
            }
        });
    }

    /**
     * When setting the background the padding values are lost so this will reset them
     */
    private void setEditTextBackground(Drawable drawable) {
        int pL = mCheckedEditText.getPaddingLeft();
        int pT = mCheckedEditText.getPaddingTop();
        int pR = mCheckedEditText.getPaddingRight();
        int pB = mCheckedEditText.getPaddingBottom();

        mCheckedEditText.setBackground(drawable);
        mCheckedEditText.setPadding(pL, pT, pR, pB);
    }

    /**
     * Sets and shows error text in a box below the edit text box.
     *
     * @param errorMessage The message to display to the user.
     */
    public void setErrorText(final String errorMessage) {
        mHandler.post(() -> {
            mErrorTextView.setText(errorMessage);
            mErrorTextView.setVisibility(VISIBLE);
        });
    }

    /**
     * Gets error text in a box below the edit text box.
     *
     * @return an error string
     */
    public String getErrorText() {
        return mErrorTextView.getText().toString();
    }

    /**
     * Hides the error message that was displayed with setErrorText()
     */
    public void hideErrorMessage() {
        mHandler.post(() -> mErrorTextView.setVisibility(GONE));
    }

    /**
     * Sets the hint of the EditText
     *
     * @param hint Text to set as hint
     */
    public void setHint(final String hint) {
        mHandler.post(() -> mCheckedEditText.setHint(hint));
    }

    /**
     * Sets the input type of the edit text. Input types must be from EditorInfo
     *
     * @param inputType Input type from EditorInfo
     */
    public void setInputType(int inputType) {
        mCheckedEditText.setInputType(inputType);
    }

    /**
     * Allows setting a mask over the characters for password fields or other purposes
     *
     * @param maskCharacters The mask to put over the field
     */
    public void setMaskCharacters(boolean maskCharacters) {
        if (maskCharacters) {
            mCheckedEditText.setTransformationMethod(new PasswordTransformationMethod());
        } else {
            mCheckedEditText.setTransformationMethod(null);
        }
    }

    /**
     * Gets the EditText object
     *
     * @return The EditText object
     */
    public EditText getEditText() {
        return mCheckedEditText;
    }

    /**
     * Sets the list of input filters that will be used if the buffer is
     * Editable. Has no effect otherwise.
     */
    public void setFilters(InputFilter[] filters) {
        mCheckedEditText.setFilters(filters);
    }

    /**
     * Callback to send back the set text in the edit box
     */
    public interface ICheckedEditTextEvent {
        /**
         * Sends the text that was input in the edit text after the user un-focuses the edit text.
         * Used for validating that the text is correct.
         *
         * @param text The text that was input in the edit text
         */
        void textInput(String text);
    }

    /**
     * Sets the callback for this CheckedEditText.
     *
     * @param callback An ICheckedEditTextEvent callback
     */
    public void setCheckedEditTextListener(ICheckedEditTextEvent callback) {
        mCallback = callback;
    }

    public String getText() {
        return mCheckedEditText.getText().toString();
    }

    /**
     * Sets the text size for the EditText and the post script TextView
     *
     * @param textSize The text size to set it to in DP.
     */
    public void setTextSize(int textSize) {
        mCheckedEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
        mEndTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
    }

    /**
     * Sets the text to appear at the end of the checked edit text.
     */
    public void setEndText(String text) {
        mEndTextView.setText(text);
    }

    /**
     * Gets the text to appear at the end of the checked edit text.
     *
     * @return a string that appears at the end of the checked edit text.
     */
    public String getEndText() {
        return mEndTextView.getText().toString();
    }

    /**
     * Sets the text of the EditText.
     *
     * @param text The text to be put into the edit text
     */
    public void setText(String text) {
        mCheckedEditText.setText(text);
    }

    /**
     * Set the IME option of the EditText
     *
     * @param imeOptions
     */
    public void setImeOptions(int imeOptions) {
        mCheckedEditText.setImeOptions(imeOptions);
    }

    public void setPrefix(String prefix) {
        mPrefix = prefix;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec,
                             int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        calculatePrefix();
    }

    private void calculatePrefix() {
        if (mOriginalLeftPadding == -1) {
            float[] widths = new float[mPrefix.length()];
            mCheckedEditText.getPaint().getTextWidths(mPrefix, widths);
            float textWidth = 0;
            for (float w : widths) {
                textWidth += w;
            }
            mOriginalLeftPadding = mCheckedEditText.getCompoundPaddingLeft();
            mCheckedEditText.setPadding((int) (textWidth + mOriginalLeftPadding),
                    mCheckedEditText.getPaddingRight(), mCheckedEditText.getPaddingTop(),
                    mCheckedEditText.getPaddingBottom());
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawText(mPrefix, mOriginalLeftPadding,
                mCheckedEditText.getLineBounds(0, null),
                mCheckedEditText.getPaint());
    }

    public void setNextFocusForwardId(int id) {
        mCheckedEditText.setNextFocusForwardId(id);
    }


}
