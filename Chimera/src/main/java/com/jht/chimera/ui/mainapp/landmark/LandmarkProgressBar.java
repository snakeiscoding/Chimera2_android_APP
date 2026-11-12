package com.jht.chimera.ui.mainapp.landmark;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;

import com.jht.chimera.R;
import com.jht.chimera.lib.widget.progressbar.VerticalProgressBar;


public class LandmarkProgressBar extends RelativeLayout {
    TextView mMakerTextView;
    VerticalProgressBar mVerticalProgressBar;
    Space mTextViewSpace;
    int mMax = 100;

    public void setMax(int max) {
        mMax = max;
        mVerticalProgressBar.setMax(max);
    }

    public LandmarkProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.landmark_progress_bar, this);

        mTextViewSpace = findViewById(R.id.marker_text_position);
        mVerticalProgressBar = findViewById(R.id.landmark_progress_bar);
        mMakerTextView = findViewById(R.id.marker_text_view);

    }

    public void setProgress(int value) {
        mVerticalProgressBar.setProgress(value);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mTextViewSpace.getLayoutParams();
        float percent = (float) value / (float) mMax * 1000f;
        percent = Math.min(percent, 1000f);
        layoutParams.weight = percent;
        mTextViewSpace.setLayoutParams(layoutParams);
        mMakerTextView.setText(String.valueOf(value));
    }

}