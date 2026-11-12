package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.datamatrixlib.DataMatrix;

public class TopBarHeadphoneIndicator extends AppCompatImageButton {

    private final DataMatrix mDataMatrix = new DataMatrix();
    boolean connected = false;


    public TopBarHeadphoneIndicator(Context context) {
        super(context);
        init();
    }

    public TopBarHeadphoneIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        updateIndicator(connected);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            mDataMatrix.createBoolAdapter(TopBarKeys.DATA_BT_HEADPHONE_CONNECTED_BOOL, value -> {
                if (connected != value) {
                    connected = value;
                    updateIndicator(value);
                }
            }, true);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            mDataMatrix.finish();
        }
    }

    private void updateIndicator(boolean connected) {
        setBackground(connected? AppCompatResources.getDrawable(getContext(), R.drawable.ic_headphones_enabled) : AppCompatResources.getDrawable(getContext(), R.drawable.ic_headphones_disabled));
    }

}
