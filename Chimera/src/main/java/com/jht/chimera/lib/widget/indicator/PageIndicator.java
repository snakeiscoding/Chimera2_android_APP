package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class PageIndicator extends LinearLayout
{
    private final int mPageSelectedColor;
    private final int mPageUnselectedColor;

    public PageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOrientation(HORIZONTAL);
        this.setGravity(Gravity.CENTER_VERTICAL);
        mPageSelectedColor = 0xFFF0B323;
        mPageUnselectedColor = 0xff111111;
    }

    public void setNumberOfPages(int numPages)
    {
        removeAllViews();

        if(numPages <= 1) {
            return;
        }

        for(int i = 0; i < numPages; i++)
        {
            ImageView view = new ImageView(getContext());
            view.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            view.setPadding(3, 3, 3, 3);
            ShapeDrawable circle = new ShapeDrawable(new OvalShape());
            circle.setIntrinsicHeight(8);
            circle.setIntrinsicWidth(8);
            circle.getPaint().setColor(Color.WHITE);
            view.setImageDrawable(circle);
            addView(view);
            view.setColorFilter(mPageUnselectedColor);
        }
        ((ImageView)getChildAt(0)).setColorFilter(mPageSelectedColor);
    }

    public void setCurrentPage(int pageIndex) {
        int pageCount = getChildCount();
        if(pageIndex < pageCount) {
            for(int i = 0; i < pageCount; i++)
            {
                ((ImageView)getChildAt(i)).setColorFilter(mPageUnselectedColor);
            }
            ((ImageView)getChildAt(pageIndex)).setColorFilter(mPageSelectedColor);
        }
    }
}
