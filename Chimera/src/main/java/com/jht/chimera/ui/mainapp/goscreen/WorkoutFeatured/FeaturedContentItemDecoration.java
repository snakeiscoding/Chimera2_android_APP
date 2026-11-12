package com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Handles spacing items horizontally for the sub-items of the featured content
 */
public class FeaturedContentItemDecoration extends RecyclerView.ItemDecoration {
    private final int spanCount;
    private final int spacing;
    private final boolean offsetPosition;

    public FeaturedContentItemDecoration(int spanCount, int spacing, boolean offsetPosition) {
        this.spanCount = spanCount;
        this.spacing = spacing;
        this.offsetPosition = offsetPosition;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);

        //0th position is taken by the featured carousel if it exists.
        if(offsetPosition && position == 0){
            return;
        }

        //Column takes into account whether the 0th position is taken by the featured carousel
        int column = (position-(offsetPosition? 1 : 0)) % spanCount;

        // Each element needs to have the same total spacing so they are the same size, this formula handles that
        outRect.left = spacing - (spacing * column / spanCount) ;
        outRect.right = spacing * (column+1) / spanCount ;
    }
}
