package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.ui.machinesettings.EngineeringIdleTimerManager;
import com.jht.chimera.ui.machinesettings.factory.IcrCalibrationActivity;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;


/**
 * Implements the factory only page which requires USB file presence to be seen.
 */
public class FactoryOnly extends SettingPageBase {
    //private final DataMatrix dataMatrix = new DataMatrix();
    private final FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        View view = LayoutInflater.from(context).inflate(R.layout.factory_only, null);
        container.addView(view);
        return view;
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        Button btnIcrCalibration = findViewById(R.id.icrCalibration);
        btnIcrCalibration.setOnClickListener(v -> {
            EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring(); // these should be in whatever the button launches instead of here
            DataMatrix.sendEvent(HALKeys.ACTION_STOP_MACHINE);
            LaunchAppUtil.launchInternalScreen(IcrCalibrationActivity.class.getCanonicalName());
        });

        if (config.supportsLcbType(LcbType.LCB_JIS_ATHENA.Value)) {
            btnIcrCalibration.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        //dataMatrix.finish();
        EngineeringIdleTimerManager.idleTimerManagement.monitorTimers(); // in case one of the functions turned them off
    }

}
