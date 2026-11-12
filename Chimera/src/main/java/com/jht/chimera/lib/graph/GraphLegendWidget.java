package com.jht.chimera.lib.graph;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jht.chimera.R;

import org.jetbrains.annotations.NonNls;

public class GraphLegendWidget extends LinearLayout {

    private final int mTextSizePx;
    private final int mTextLeftMarginPx;
    private final int mColorSwatchWidthPx, mColorSwatchHeightPx, mColorSwatchHeightPx2;
    private final int mLineHeightPx;

    public GraphLegendWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        setLayoutParams(params);
        // TODO: Make this customizable per instance of GraphLegendWidget?
        Resources resources = context.getResources();
        mTextSizePx = resources.getDimensionPixelSize(R.dimen.runScreenLib_graphLegendTextSize);
        mTextLeftMarginPx = resources.getDimensionPixelSize(R.dimen.runScreenLib_graphLegendItemTextLeftMargin);
        mColorSwatchWidthPx = resources.getDimensionPixelSize(R.dimen.runScreenLib_graphLegendItemColorSwatchWidth);
        mColorSwatchHeightPx2 = resources.getDimensionPixelSize(R.dimen.runScreenLib_graphLegendItemColorSwatchHeight2);
        mColorSwatchHeightPx = resources.getDimensionPixelSize(R.dimen.runScreenLib_graphLegendItemColorSwatchHeight);
        mLineHeightPx = resources.getDimensionPixelSize(R.dimen.runScreenLib_graphLegendItemLineHeight);
    }

    public void reset() {
        removeAllViews();
    }

    public void addItem(String label, @NonNls String blockColor, @NonNls String textColor)
    {
        addItem(label, Color.parseColor(blockColor), Color.parseColor(textColor));
    }

    public void addItem (String label, String blockColor)
    {
        addItem(label, Color.parseColor(blockColor), Color.parseColor("#7E7F7F"));
    }

    public void addItem (String label, int blockColor)
    {
        addItem(label, blockColor, Color.parseColor("#7E7F7F"));
    }

    public void addItem(String label, int blockColor, int textColor) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, mLineHeightPx);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        if (getOrientation() == VERTICAL) {
            layout.setLayoutParams(params);
            addView(layout);

            View view = new View(getContext());
            view.setLayoutParams(new LayoutParams(mColorSwatchWidthPx, mColorSwatchHeightPx));
            view.setBackgroundColor(blockColor);
            layout.addView(view);

            TextView textView = new TextView(getContext());
            LayoutParams textParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            textParams.leftMargin = mTextLeftMarginPx;
            textView.setLayoutParams(textParams);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mTextSizePx);
            textView.setTextColor(Color.WHITE);
            textView.setText(label);
            textView.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            textView.setAutoSizeTextTypeUniformWithConfiguration (8, mTextSizePx, 1, TypedValue.COMPLEX_UNIT_DIP);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            layout.addView(textView);
        }
        else
        {
            params.topMargin = 2;
            params.bottomMargin = 2;
            layout.setLayoutParams(params);
            addView(layout);

            View view = new View(getContext());
            view.setLayoutParams(new LayoutParams(mColorSwatchWidthPx, mColorSwatchHeightPx2));
            view.setBackgroundColor(blockColor);
            layout.addView(view);

            TextView textView = new TextView(getContext());
            LayoutParams textParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            textParams.weight = 2;
            textParams.rightMargin = mTextLeftMarginPx * 2;
            textParams.leftMargin = mTextLeftMarginPx;
            textView.setLayoutParams(textParams);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mTextSizePx);
            textView.setTextColor(textColor);
            textView.setText(label);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            textView.setMaxLines(1);
            textView.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            layout.addView(textView);
        }
    }
}
