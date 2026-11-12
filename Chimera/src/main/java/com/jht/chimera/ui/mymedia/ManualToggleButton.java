package com.jht.chimera.ui.mymedia;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ToggleButton;

public class ManualToggleButton extends ToggleButton {

    public ManualToggleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ManualToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ManualToggleButton(Context context) {
        super(context);
    }

    @Override
    public void toggle() {
        // do nothing
    }
}
