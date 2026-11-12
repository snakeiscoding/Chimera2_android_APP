package com.jht.chimera.lib.widget.statbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.jht.chimera.R;

import java.util.ArrayList;
import java.util.List;

public class StatOverlayColumn extends LinearLayout {
    private static final String TAG = "StatOverlayColumn";
    private List<StatOverlayItem> mStatViews;

    public StatOverlayColumn(Context context) {
        super(context);
        initialize(context);
    }

    public StatOverlayColumn(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public StatOverlayColumn(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.overlay_stat_column, this);

        StatOverlayItem mStatView0 = findViewById(R.id.stat_item_0);
        StatOverlayItem mStatView1 = findViewById(R.id.stat_item_1);
        StatOverlayItem mStatView2 = findViewById(R.id.stat_item_2);
        StatOverlayItem mStatView3 = findViewById(R.id.stat_item_3);
        mStatViews = new ArrayList<>();
        mStatViews.add(mStatView0);
        mStatViews.add(mStatView1);
        mStatViews.add(mStatView2);
        mStatViews.add(mStatView3);
    }

    public List<StatOverlayItem> getStatItemViews() {
        return mStatViews;
    }
}
