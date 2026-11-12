package com.jht.chimera.lib.widget.edittext;

import android.content.Context;
import android.util.AttributeSet;

import com.jht.chimera.R;


public class RegisterEditTextRight extends RegisterEditTextBase {
    public RegisterEditTextRight(Context context) {
        super(context);
    }

    public RegisterEditTextRight(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RegisterEditTextRight(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    int getLayout() {
        return R.layout.register_edit_text_right;
    }
}
