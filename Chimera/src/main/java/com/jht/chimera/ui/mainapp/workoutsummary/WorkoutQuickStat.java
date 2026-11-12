package com.jht.chimera.ui.mainapp.workoutsummary;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.jht.chimera.R;

public class WorkoutQuickStat extends LinearLayout {
    TextView label;
    TextView value;
    TextView unit;

    public WorkoutQuickStat(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    // need initialize to populate 3 TextViews in a stack?
    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        int color = ContextCompat.getColor(context, R.color.white);
        ColorStateList colorList = ColorStateList.valueOf(color);
        label = new TextView(context);
        label.setLayoutParams(lp);
        label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        label.setTextColor(color);
        TextViewCompat.setCompoundDrawableTintList(label, colorList);
        label.setCompoundDrawablePadding(15);
        addView(label);
        value = new TextView(context);
        value.setLayoutParams(lp);
        value.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 48);
        value.setTextColor(color);
        value.setTypeface(value.getTypeface(), Typeface.BOLD);
        addView(value);
        unit = new TextView(context);
        unit.setLayoutParams(lp);
        unit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        unit.setTextColor(color);
        addView(unit);
    }

    public void setLabel(String text, @DrawableRes final int drawableId) {
        label.setText(text); // should we take string, or assume id will be used?
        label.setCompoundDrawablesRelativeWithIntrinsicBounds(drawableId, 0, 0, 0);
    }

    public void setValue(String text) {
        value.setText(text);
    }

    public void setUnit(String text) {
        unit.setText(text);
    }
}
