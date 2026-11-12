package com.jht.chimera.lib.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.datamatrixlib.DataMatrix;

public class DataMatrixCheckBox extends androidx.appcompat.widget.AppCompatCheckBox {
    public DataMatrixCheckBox(@NonNull Context context) {
        super(context);
    }

    public DataMatrixCheckBox(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DataMatrixCheckBox(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void linkCheckBox(Object key) {
        setChecked(DataMatrix.getBoolean(key, false));
        setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(key, isChecked));
    }
}
