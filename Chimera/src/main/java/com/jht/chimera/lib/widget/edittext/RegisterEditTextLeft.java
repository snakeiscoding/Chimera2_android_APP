package com.jht.chimera.lib.widget.edittext;

import android.content.Context;
import android.util.AttributeSet;

import com.jht.chimera.R;


public class RegisterEditTextLeft extends RegisterEditTextBase {
    public RegisterEditTextLeft(Context context) {
        super(context);
    }

    public RegisterEditTextLeft(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RegisterEditTextLeft(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    int getLayout() {
        return R.layout.register_edit_text;
    }
}
