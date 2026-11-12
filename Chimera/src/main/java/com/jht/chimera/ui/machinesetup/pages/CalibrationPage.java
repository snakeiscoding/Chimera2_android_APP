package com.jht.chimera.ui.machinesetup.pages;

import android.animation.Animator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.jht.chimera.R;
import com.jht.chimera.ui.machinesettings.pages.Calibration;
import com.jht.chimera.ui.machinesetup.MachineSetupPageData;

/*
    MachineSetupPageBase -> StandardPageBase and original page extends SettingPageBase; StandardPageBase and SettingPageBase are basically the same?
    Is it worth trying to pull common elements out of Calibration page for use in both at this time, given pending kotlin/compose conversion? Would it still be somewhat awkward?
    To avoid messing with a lot of code and knowing FTS and MES are on the list to be reworked, this is a hacky way to display the calibration page in the setup flow.
 */

public class CalibrationPage extends MachineSetupPageBase {
    private final Calibration calibrationMes;

    public CalibrationPage(MachineSetupPageData pageData) {
        super(pageData);
        calibrationMes = new Calibration();
    }

    @Override
    public boolean onNext() {
        pageData().wasCalibrationDone = calibrationMes.wasCalibrationDone;
        return true;
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        return null; // only used by super.create() but we are deferring to hosted page
    }
    @Override
    public void create(ViewGroup container) {
        //super.create(container); // defer to hosted page
        calibrationMes.create(container);
    }

    @Override
    public void destroy(ViewGroup container) {
        //super.destroy(container); // defer to hosted page
        calibrationMes.destroy(container);
    }

    @Override
    public boolean isViewFromObject(View view) {
        return calibrationMes.isViewFromObject(view);
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        //super.show(listener, start); // defer to hosted page
        pageData().previousButton.setVisibility(View.VISIBLE);
        pageData().nextButton.setVisibility(View.VISIBLE);
        pageData().title.setText(R.string.calibration_tc);
        calibrationMes.show(listener, start);
    }
    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        //super.hide(listener, start); // defer to hosted page
        calibrationMes.hide(listener, start);
    }
}
