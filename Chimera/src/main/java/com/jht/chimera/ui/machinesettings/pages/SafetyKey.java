package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.service.hal.lib.hardware.ParkerDriver;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;

import java.util.Arrays;

public class SafetyKey extends SettingPageBase {
    private View view;
    private final PeriodicTimer readTimer = new PeriodicTimer();
    private static final String TAG = "SafetyKey";
    private final DataMatrix mDataMatrix = new DataMatrix();
    private final int saveInt = DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT);


    @Override
    protected View onCreate(Context context, ViewGroup container) {
        view = LayoutInflater.from(context).inflate(R.layout.safety_test, null);
        container.addView(view);
        return view;
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, 1);
        mDataMatrix.createBoolAdapter(HALKeys.DATA_IO_IS_SAFETY_PULLED_BOOL, value -> {
            if (value){
                ((TextView)findViewById(R.id.hr_test_label)).setText("Safety Key on");

            }
            else{
                ((TextView)findViewById(R.id.hr_test_label)).setText("Safety Key off");
            }
        });

    }


    @Override
    public void hide(Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, saveInt);
        readTimer.stop();
        mDataMatrix.finish();
    }
}

