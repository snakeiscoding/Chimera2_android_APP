package com.jht.chimera.lib.widget.statbar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.jht.chimera.R;

public class StatOverlayItem extends ConstraintLayout implements IStatView {
    private TextView mTitleTextView;
    private TextView mValueTextView;
    private View mNormalStatLayout, mSplitStatLayout;
    private TextView mValueSplit1TextView, mValueSplit2TextView;
    private ImageView mIconView;
    private Handler mHandler = new Handler();

    private Integer mTextColor = null;
    private Integer mTitleColor = null;

    public StatOverlayItem(Context context) {
        super(context);
        initialize(context);
    }

    public StatOverlayItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public StatOverlayItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.overlay_stat_item, this);

        mNormalStatLayout = findViewById(R.id.statNormalLayout);
        mSplitStatLayout = findViewById(R.id.statSplitLayout);
        mTitleTextView = findViewById(R.id.title_text_box);
        mValueTextView = findViewById(R.id.value_text_box);
        mValueSplit1TextView = findViewById(R.id.textViewSplitValue);
        mValueSplit2TextView = findViewById(R.id.textViewSplitValue2);
        mIconView = findViewById(R.id.icon_image_view);
    }

    /**
     * Changes the color of the text
     *
     * @param valueColor The color to set the value to
     * @param titleColor the color to set the title to
     */
    public void changeTextColor(final int valueColor, final int titleColor) {
        if (mTextColor == null || mTitleColor == null ||
                mTextColor != valueColor || mTitleColor != titleColor) {
            mTextColor = valueColor;
            mTitleColor = titleColor;
            mHandler.post(() -> {
                mTitleTextView.setTextColor(valueColor);
                mValueTextView.setTextColor(titleColor);
            });
        }
    }

    @Override
    public void titleChanged(final String title) {
        mHandler.post(() -> mTitleTextView.setText(title));
    }

    @Override
    public void valueChanged(final String value) {
        mHandler.post(() -> mValueTextView.setText(value));
    }

    @Override
    public void setIcon(Integer iconId) {
        mHandler.post(() -> {
            if (iconId == null) {
                mIconView.setImageDrawable(null);
            } else {
                mIconView.setImageResource(iconId);
            }
        });
    }

    @Override
    public void setStatBackgroundColor(Integer color) {
        mHandler.post(() -> {
            if (color == null) {
                Drawable defaultBackground = ContextCompat.getDrawable(getContext(), R.drawable.va_button);
                setBackground(defaultBackground);
            } else {
                // Create a drawable with matching radius
                GradientDrawable bgDrawable = new GradientDrawable();
                float radius = 7f;
                Resources r = getResources();
                float pxRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radius, r.getDisplayMetrics());
                bgDrawable.setCornerRadius(pxRadius);
                bgDrawable.setColor(color);
                setBackground(bgDrawable);
            }
        });
    }

    @Override
    public void setTextColor(Integer color) {
        mHandler.post(() -> {
            if (color == null) {
                mValueTextView.setTextColor(Color.WHITE);
                mTitleTextView.setTextColor(Color.WHITE);
            } else {
                mValueTextView.setTextColor(color);
                mTitleTextView.setTextColor(color);
            }
        });
    }

    @Override
    public void setStatVisibility(final int visibility) {
        mHandler.post(() -> setVisibility(visibility));
    }

    public void showNormalStat() {
        mSplitStatLayout.setVisibility(GONE);
        mNormalStatLayout.setVisibility(VISIBLE);
    }

    public void showSplitStat() {
        mNormalStatLayout.setVisibility(GONE);
        mSplitStatLayout.setVisibility(VISIBLE);
    }

    public void updateSplitValues(String split1, String split2) {
        mValueSplit1TextView.setText(split1);
        mValueSplit2TextView.setText(split2);
    }
}
