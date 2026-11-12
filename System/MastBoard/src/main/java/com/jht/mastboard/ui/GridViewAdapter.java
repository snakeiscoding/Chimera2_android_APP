package com.jht.mastboard.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.jht.mastboard.R;
import com.jht.mastboard.ui.legacy.ScaleView;

public class GridViewAdapter extends BaseAdapter {
    Context context;

    public GridViewAdapter(Context applicationContext) {
        this.context = applicationContext;
    }

    @Override
    public int getCount() {
        return 36;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.grid_view_item, viewGroup, false);
            ScaleView.scaleContents(view);
        }
        View icon = view.findViewById(R.id.grid_button);
        icon.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int[] location = new int[2];
                icon.getLocationOnScreen(location);
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    icon.setBackgroundColor(Color.parseColor("#a32424"));
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    icon.setBackgroundColor(Color.parseColor("#ffffffff"));
                }
                return false;
            }
        });
        return view;
    }
}