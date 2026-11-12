package com.jht.chimera.lib.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * Created by alex.nguyen on 7/27/2017.
 */

public class ScrollViewNoTouch extends ScrollView
{

    public ScrollViewNoTouch(Context context)
    {
        super(context);
    }

    public ScrollViewNoTouch(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ScrollViewNoTouch(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        //Ignore intercepting of touch events
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        //Ignore onTouch events
        return true;
    }
}
