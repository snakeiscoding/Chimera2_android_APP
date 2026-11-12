package com.jht.chimera.lib.widget.selectron;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Checkable;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.jht.chimera.R;

// NOTE: Using AppCompatButton doesn't seem to work with drawablePressState attribute
public class SelectronButton extends Button implements Checkable {
    private static boolean DEBUG_COLOR_MODE = false;
    private static boolean THEME_ENABLED = false;

    private int DEFAULT_PRESS_COLOR = 0X40000000;
    private int DEFAULT_DISABLE_COLOR = 0XFF333333;
    private int THEME_COLOR = 0xFFFF0000;

    private Drawable mIconDrawable;
    private BitmapDrawable mIconBitmap;
    private BitmapDrawable mIconBitmapPressed;
    private BitmapDrawable mIconBitmapChecked;
    private Drawable mBackgroundDrawable;
    private Drawable mPressDrawable;
    private Drawable mDisableDrawable;
    private Drawable mCheckedDrawable;
    private int mIconWidth = -1;
    private int mIconHeight = -1;

    private int mClickTime = 500;
    private long mMsTime = 0;
    private float mScaleX = 1f;
    private float mScaleY = 1f;
    private int mTint = 0;
    private int mTintPressed = 0;
    private int mTintChecked = 0;
    private boolean mUseTint = false;
    private boolean mUseTintPressed = false;
    private boolean mUseTintChecked = false;
    private int mIconGravity = Gravity.CENTER;
    private int mIconPaddingLeft;
    private int mIconPaddingTop;
    private int mIconPaddingRight;
    private int mIconPaddingBottom;

    private LayerDrawable mDefaultDrawableLayer;
    private LayerDrawable mPressedDrawableLayer;
    private LayerDrawable mDisabledDrawableLayer;
    private LayerDrawable mCheckedDrawableLayer;

    private boolean mUseRadioButtonStyle;
    private boolean mChecked;
    private boolean mBroadcasting;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private OnCheckedChangeListener mOnCheckedChangeWidgetListener;

    public interface OnCheckedChangeListener {
        void onCheckedChanged(SelectronButton buttonView, boolean isChecked);
    }

    public SelectronButton(Context context) {
        super(context);
        initButton();
    }

    public SelectronButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getAttributes(context, attrs);
        initButton();
    }

    public SelectronButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
        initButton();
    }

    private void getAttributes(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SelectronButton, 0, 0);
        for (int i = 0; i < ta.getIndexCount(); ++i) {
            int attr = ta.getIndex(i);
            if (attr == R.styleable.SelectronButton_drawableIcon) {
                int id = ta.getResourceId(R.styleable.SelectronButton_drawableIcon, -1);
                if (id != -1) {
                    mIconDrawable = AppCompatResources.getDrawable(context, id);
                }
            } else if (attr == R.styleable.SelectronButton_drawableBackground) {
                try {
                    // try to load color
                    int color = ta.getColor(attr, 0);
                    if (Color.BLACK == color) {
                        DEFAULT_PRESS_COLOR = 0x40FFFFFF;
                    }
                    mBackgroundDrawable = ta.getDrawable(attr);
                    continue;
                } catch (Exception e) {
                }

                // try to load image
                int id = ta.getResourceId(R.styleable.SelectronButton_drawableBackground, -1);
                if (id != -1) {
                    mBackgroundDrawable = AppCompatResources.getDrawable(context, id);
                }

            } else if (attr == R.styleable.SelectronButton_drawablePressState) {
                try {
                    // try to load color
                    int color = ta.getColor(attr, 0); // needed for try catch
                    mPressDrawable = ta.getDrawable(attr);
                    continue;
                } catch (Exception e) {
                }

                // try to load image
                int id = ta.getResourceId(R.styleable.SelectronButton_drawablePressState, -1);
                if (id != -1) {
                    mPressDrawable = AppCompatResources.getDrawable(context, id);
                }
            } else if (attr == R.styleable.SelectronButton_drawableDisableState) {
                try {
                    // try to load color
                    int color = ta.getColor(attr, 0);
                    DEFAULT_DISABLE_COLOR = color;
                    mDisableDrawable = ta.getDrawable(attr);
                    continue;
                } catch (Exception e) {
                }

                // try to load image
                int id = ta.getResourceId(R.styleable.SelectronButton_drawableDisableState, -1);
                if (id != -1) {
                    mDisableDrawable = AppCompatResources.getDrawable(context, id);
                }
            } else if (attr == R.styleable.SelectronButton_drawableCheckedState) {
                try {
                    // try to load color
                    int color = ta.getColor(attr, 0); // needed for try catch
                    mCheckedDrawable = ta.getDrawable(attr);
                    continue;
                } catch (Exception e) {
                }

                // try to load image
                int id = ta.getResourceId(R.styleable.SelectronButton_drawableCheckedState, -1);
                if (id != -1) {
                    mCheckedDrawable = AppCompatResources.getDrawable(context, id);
                }
            } else if (attr == R.styleable.SelectronButton_clickableTimeMiliSecond) {
                mClickTime = ta.getInt(attr, 500);
            } else if (attr == R.styleable.SelectronButton_iconWidth) {
                mIconWidth = ta.getDimensionPixelSize(attr, -1);
            } else if (attr == R.styleable.SelectronButton_iconHeight) {
                mIconHeight = ta.getDimensionPixelSize(attr, -1);
            } else if (attr == R.styleable.SelectronButton_drawableIconGravity) {
                mIconGravity = ta.getInt(attr, Gravity.NO_GRAVITY);
            } else if (attr == R.styleable.SelectronButton_drawableIconTint) {
                mUseTint = true;
                mTint = ta.getColor(attr, 0);
            } else if (attr == R.styleable.SelectronButton_drawableIconPressedTint) {
                mUseTintPressed = true;
                mTintPressed = ta.getColor(attr, 0);
            } else if (attr == R.styleable.SelectronButton_drawableIconCheckedTint) {
                mUseTintChecked = true;
                mTintChecked = ta.getColor(attr, 0);
            } else if (attr == R.styleable.SelectronButton_useRadioButtonStyle) {
                mUseRadioButtonStyle = ta.getBoolean(attr, false);
            } else if (attr == R.styleable.SelectronButton_iconPaddingLeft) {
                mIconPaddingLeft = (int) ta.getDimension(attr, 0f);
            } else if (attr == R.styleable.SelectronButton_iconPaddingTop) {
                mIconPaddingTop = (int) ta.getDimension(attr, 0f);
            } else if (attr == R.styleable.SelectronButton_iconPaddingRight) {
                mIconPaddingRight = (int) ta.getDimension(attr, 0f);
            } else if (attr == R.styleable.SelectronButton_iconPaddingBottom) {
                mIconPaddingBottom = (int) ta.getDimension(attr, 0f);
            }
        }
        ta.recycle();
    }

    private void initButton() {

        // Note: this seem to break some click function (forget which edge case now)
//        this.setOnLongClickListener(new OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                if (DEBUG_COLOR_MODE) {
//                    ColorWheelDialog colorWheelDialog = new ColorWheelDialog(getContext());
//
//                    colorWheelDialog.setOnColorChangListener(new ColorWheelDialog.OnColorChangListener() {
//                        @Override
//                        public void colorWheelChanged(int color) {
//                            THEME_COLOR = color;
//                        }
//                    });
//                    colorWheelDialog.show();
//                }
//                return false;
//            }
//        });
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        NavigationClickListener clickListener = new NavigationClickListener(listener);
        super.setOnClickListener(clickListener);
    }

    @Override
    public void setBackground(Drawable background) {
        mBackgroundDrawable = background;
        createSelectronDrawable();
    }

    public void setIconHeight(int height) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mIconHeight = (int) (height * metrics.density);
        refreshButton();
    }



    /**
     * Sets the mIconBitmapDrawable for the Selectron button
     *
     * @param iconBitmapDrawable The mIconBitmapDrawable for the selectron button
     */
    public void setIcon(BitmapDrawable iconBitmapDrawable) {
        mIconDrawable = iconBitmapDrawable;
        refreshButton();
    }

    public void setIconDrawable(Drawable iconDrawable) {
        mIconDrawable = iconDrawable;
        refreshButton();
    }


    /**
     * Sets the icon displayed in the center of the Selectron button
     *
     * @param resourceId The drawable resource to use
     */
    public void setIcon(int resourceId) {
        if (resourceId <= 0) {
            mIconDrawable = null;
        } else {
            mIconDrawable = AppCompatResources.getDrawable(getContext(), resourceId);
        }
        refreshButton();
    }

    private void resizeIcon(int width, int height) {
        if (mIconDrawable == null || width <= 0 || height <= 0) return;

        if (mIconDrawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) mIconDrawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null) {
                Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmap, width, height, true);
                mIconBitmap = new BitmapDrawable(getResources(), bitmapResized);
            }
        }

        // for vector drawables
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        mIconDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        if (mUseTint) {
            mIconDrawable.setColorFilter(new PorterDuffColorFilter(mTint, PorterDuff.Mode.MULTIPLY));
        }
        mIconDrawable.draw(canvas);
        mIconBitmap = new BitmapDrawable(getResources(), bitmap);

        if (mUseTintPressed) {
            Bitmap bitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas2 = new Canvas(bitmap2);
            mIconDrawable.setBounds(0, 0, canvas2.getWidth(), canvas2.getHeight());
            mIconDrawable.setColorFilter(new PorterDuffColorFilter(mTintPressed, PorterDuff.Mode.MULTIPLY));
            mIconDrawable.draw(canvas2);
            mIconBitmapPressed = new BitmapDrawable(getResources(), bitmap2);
        }

        if (mUseTintChecked) {
            Bitmap bitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas2 = new Canvas(bitmap2);
            mIconDrawable.setBounds(0, 0, canvas2.getWidth(), canvas2.getHeight());
            mIconDrawable.setColorFilter(new PorterDuffColorFilter(mTintChecked, PorterDuff.Mode.MULTIPLY));
            mIconDrawable.draw(canvas2);
            mIconBitmapChecked = new BitmapDrawable(getResources(), bitmap2);
        }

    }

    private void refreshButton() {
        if (mIconDrawable != null && mIconWidth != -1 && mIconHeight != -1) {
            resizeIcon(Math.round(mIconWidth * mScaleX), Math.round(mIconHeight * mScaleY));
        } else if (mIconDrawable != null) {
            int width = getWidth() - getPaddingLeft() - getPaddingRight();
            int height = getHeight() - getPaddingTop() - getPaddingBottom();
            if (mIconDrawable instanceof BitmapDrawable) {
                // do nothing
            } else {
                // compute proportional resize
                int intrinsicWidth = mIconDrawable.getIntrinsicWidth();
                int intrinsicHeight = mIconDrawable.getIntrinsicHeight();
                float ratioWidth = (float) intrinsicWidth / (float) width;
                float ratioHeight = (float) intrinsicHeight / (float) height;

                if (ratioWidth > ratioHeight) {
                    height = (int) (intrinsicHeight / ratioWidth);
                } else {
                    width = (int) (intrinsicWidth / ratioHeight);
                }
            }
            resizeIcon(width, height);
        }

        createSelectronDrawable();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        refreshButton();
    }

    private void createSelectronDrawable() {
        SelectronDrawable selector = new SelectronDrawable();

        if (mIconBitmap == null) {
            mIconBitmap = new BitmapDrawable(getResources(), (Bitmap) null);
        }

        if (mBackgroundDrawable == null) {
            mBackgroundDrawable = new ColorDrawable(Color.TRANSPARENT);
        }

        if (mPressDrawable == null) {
            mPressDrawable = new ColorDrawable(DEFAULT_PRESS_COLOR);
        }

        if (mDisableDrawable == null) {
            mDisableDrawable = new ColorDrawable(DEFAULT_PRESS_COLOR);
        }

        mIconBitmap.setGravity(mIconGravity);
        if (mIconBitmapPressed != null) mIconBitmapPressed.setGravity(mIconGravity);
        if (mIconBitmapChecked != null) mIconBitmapChecked.setGravity(mIconGravity);

        // TODO it looks like the transparency doesn't seem to work in the color definition
        Drawable[] defaultDrawable = {mBackgroundDrawable, mIconBitmap};
        Drawable[] pressedDrawable = {mBackgroundDrawable, mPressDrawable, mIconBitmapPressed == null ? mIconBitmap : mIconBitmapPressed};
        Drawable[] disabledDrawable = {mBackgroundDrawable, mDisableDrawable, mIconBitmap};

        mDefaultDrawableLayer = new LayerDrawable(defaultDrawable);
        mPressedDrawableLayer = new LayerDrawable(pressedDrawable);
        mDisabledDrawableLayer = new LayerDrawable(disabledDrawable);

        mDefaultDrawableLayer.setLayerInset(1, mIconPaddingLeft, mIconPaddingTop, mIconPaddingRight, mIconPaddingBottom);
        mPressedDrawableLayer.setLayerInset(2, mIconPaddingLeft, mIconPaddingTop, mIconPaddingRight, mIconPaddingBottom);
        mDisabledDrawableLayer.setLayerInset(2, mIconPaddingLeft, mIconPaddingTop, mIconPaddingRight, mIconPaddingBottom);

        if (mCheckedDrawable != null) {
            Drawable[] checkedDrawable = {mBackgroundDrawable, mCheckedDrawable, mIconBitmapChecked == null ? mIconBitmap : mIconBitmapChecked};
            mCheckedDrawableLayer = new LayerDrawable(checkedDrawable);
            mCheckedDrawableLayer.setLayerInset(2, mIconPaddingLeft, mIconPaddingTop, mIconPaddingRight, mIconPaddingBottom);
            selector.addState(new int[]{android.R.attr.state_checked}, mCheckedDrawableLayer);
        }


        selector.addState(new int[]{-android.R.attr.state_enabled}, mDisabledDrawableLayer);
        selector.addState(new int[]{android.R.attr.state_pressed}, mPressedDrawableLayer);
        selector.addState(new int[]{}, mDefaultDrawableLayer);

        super.setBackground(selector);
    }

    private class NavigationClickListener implements OnClickListener {

        OnClickListener parentListener;

        public NavigationClickListener(OnClickListener clickListener) {
            parentListener = clickListener;
        }

        @Override
        public void onClick(View v) {
            if (parentListener != null) {
                if (Math.abs((System.currentTimeMillis() - mMsTime)) > mClickTime) {
                    mMsTime = System.currentTimeMillis();
                    parentListener.onClick(v);
                }
            }

            //Change color permanently

        }

    }

    private class SelectronDrawable extends StateListDrawable {

        /* DISABLING THEME SUPPORT FOR NOW
        private PorterDuffColorFilter defaultFilter = new PorterDuffColorFilter(THEME_COLOR, PorterDuff.Mode.MULTIPLY);
        private PorterDuffColorFilter pressedFilter = new PorterDuffColorFilter(THEME_COLOR, PorterDuff.Mode.MULTIPLY);
        private PorterDuffColorFilter disabledFilter = new PorterDuffColorFilter(DEFAULT_DISABLE_COLOR, PorterDuff.Mode.MULTIPLY);

        private boolean pressed;
        private boolean enabled;
        */

        @Override
        protected boolean onStateChange(int[] stateSet) {
            /* DISABLING THEME SUPPORT FOR NOW
            pressed = false;
            enabled = true; // TODO fix me... Seems like Android 5.1.1 call this twice where first state button is disabled
            for (int state : stateSet) {
                if (state == android.R.attr.state_enabled) {
                    enabled = true;
                } else if (state == android.R.attr.state_pressed) {
                    pressed = true;
                }
            }
            if (!enabled) {
                setColorFilter(disabledFilter);
            } else if (pressed) {
                if (THEME_ENABLED) {
                    setColorFilter(pressedFilter);
                }
            } else {
                if (THEME_ENABLED) {
                    setColorFilter(defaultFilter);
                } else {
                    clearColorFilter();
                }
            }
            */
            return super.onStateChange(stateSet);
        }
    }

    @Override
    public boolean performClick() {
        if (mCheckedDrawable != null) toggle();
        return super.performClick();
    }

    public BitmapDrawable getIcon() {
        return mIconBitmap;
    }

    public void scaleIcon(float xScale, float yScale) {
        if (xScale > 0 && yScale > 0) {
            mScaleX = xScale;
            mScaleY = yScale;
        }
    }

    public boolean isIconSquare() {
        if ((mIconWidth == -1) || (mIconWidth != mIconHeight)) return false;
        return true;
    }

    public void setTint(int color) {
        mUseTint = true;
        mTint = color;
        createSelectronDrawable();
    }

    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    public void setOnCheckedChangeWidgetListener(@Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeWidgetListener = listener;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();

            // Avoid infinite recursions if setChecked() is called from a listener
            if (mBroadcasting) {
                return;
            }

            mBroadcasting = true;
            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
            }
            if (mOnCheckedChangeWidgetListener != null) {
                mOnCheckedChangeWidgetListener.onCheckedChanged(this, mChecked);
            }

            mBroadcasting = false;
        }
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        if (mUseRadioButtonStyle) {
            // we override to prevent toggle when the radio is already
            // checked (as opposed to check boxes widgets)
            if (!isChecked()) setChecked(!mChecked);
        } else {
            setChecked(!mChecked);
        }
    }

    private static final int[] CHECKED_STATE_SET = {
            android.R.attr.state_checked
    };

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

}
