package com.jht.chimera.ui.machinesettings.viewpager;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;

import com.jht.androiduiwidgets.viewpager.IPageHolder;
import com.jht.androiduiwidgets.viewpager.NoScrollViewPager;
import com.jht.androiduiwidgets.viewpager.StandardPageAdapter;

import java.util.LinkedList;
import java.util.List;

/*
 * This provides a simple animator for going between pages.  This is useful in the Engineering screen.
 * The current default animation here fades the page and moves it to the end and then brings in the next page
 * from the left fade it in.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CustomSettingViewPager extends NoScrollViewPager {

    int currentSelectionIndex = -1;

    /**
     * Initalize the view pager.
     *
     * @param context The context that contains the view.
     */
    public CustomSettingViewPager(Context context) {
        super(context);
    }

    /**
     * Initalize the view pager.
     *
     * @param context The context that contains the view.
     * @param attrs   The XML attributes for the pager.
     */
    public CustomSettingViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Set the current page in teh view pager.
     *
     * @param position The page position.
     * @param pages    The list of pages.
     */
    public void setCurrentItem(final int position, final List<IPageHolder> pages, boolean force) {

        // do nothing if the index hasn't changed
        if (!force && currentSelectionIndex == position) return;

        if(force) {
            currentSelectionIndex = -1;
        }

        List<IPageHolder> list = new LinkedList<>();
        if(position >= 0 && position < pages.size()) {
            list.add(pages.get(position));
        }
        if (currentSelectionIndex != -1) {
            pages.get(currentSelectionIndex).hide(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    setAdapter(new StandardPageAdapter(list));
                    currentSelectionIndex = position;
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            }, true);
        } else {
            setAdapter(new StandardPageAdapter(list));
            currentSelectionIndex = position;
        }

    }

    @Override
    public int getCurrentItem() {
        return currentSelectionIndex;
    }


}
