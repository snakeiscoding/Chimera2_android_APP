package com.jht.chimera.lib.widget.radiogroup;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class TabbedUIGroup extends LinearLayout {
    private OnClickListener mListener = null;
    View mCurrentSelected = null;

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            for (int i = 0; i < TabbedUIGroup.this.getChildCount(); i++) {
                View childView = TabbedUIGroup.this.getChildAt(i);
                if (childView != v) {
                    childView.setSelected(false);
                } else {
                    if (mCurrentSelected != v) {
                        childView.setSelected(true);
                        if (null != mListener) {
                            mListener.onClick(v);
                        }
                        mCurrentSelected = v;
                    }
                }
            }
        }
    };

    public TabbedUIGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        child.setOnClickListener(mOnClickListener);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        super.addView(child, params);
    }


    @Override
    public void addView(View child, int index) {
        super.addView(child, index);
    }

    @Override
    public void addView(View child) {
        super.addView(child);
    }

    @Override
    public void addView(View child, int width, int height) {
        super.addView(child, width, height);
    }

    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    public void clearCheck() {
        if (null != mCurrentSelected) {
            mCurrentSelected.setSelected(false);
            mCurrentSelected = null;
        }
    }

    public int getCheckedRadioButtonId() {
        return mCurrentSelected.getId();
    }

    public void setChecked(int i) {
        if ((0 <= i) && (i < this.getChildCount())) {
            this.getChildAt(i).setSelected(true);
            if (null != mOnClickListener) {
                mOnClickListener.onClick(this.getChildAt(i));
            }
        }
    }
}
