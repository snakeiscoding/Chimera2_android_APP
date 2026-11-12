package com.jht.chimera.service.hal.lib.lcb;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.service.hal.LCBService;


public class LCBDataTimerHandler {

    private static int SAFETYKEY_RELEASE_DELAY = 2;
    private static int SAFETYKEY_UNLOCK_CHECK = 6;

    //Treadmill Timer
    private int tmSafetykeyCodeReleaseCheck = 0;

    //EnClimbmill Timer
    private int cmSafetyReleaseDelayTimer = 0;

    private LCBService service = null;

    private JHTTimer safetykeyTimer;

    public LCBDataTimerHandler(LCBService service) {

        this.service = service;

        safetykeyTimer = new PeriodicTimer(1000, 1000, new JHTTimer.IOnTimer() {
            @Override
            public void onTimer() {

                if (FrameConfigManager.getInstance().getFrameConfig().machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                    checkTmSafetykeyRelease();
                } else if (FrameConfigManager.getInstance().getFrameConfig().machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill ) {
                    checkCmSafetykeyRelease();
                }
            }
        });

        safetykeyTimer.start("SafeKeyTimer");
    }

    /*
     *  Treadmill Safetykey function
     */

    public void stopTmReleaseCheckTimer() { tmSafetykeyCodeReleaseCheck = 0; }

    public void setTmReleaseCheckTimer() { tmSafetykeyCodeReleaseCheck = SAFETYKEY_UNLOCK_CHECK; }

    public void setTmReleaseDelayTimer() {
        tmSafetykeyCodeReleaseCheck = SAFETYKEY_RELEASE_DELAY;
    }


    private void checkTmSafetykeyRelease() {

        if(tmSafetykeyCodeReleaseCheck > 0) {

            if ((--tmSafetykeyCodeReleaseCheck) == 0) {
                if(!LCBInfo.getMCBLockState())
                    service.onTmSafetykeyReset();
            }
        }
    }

    /*
     *  ENClimbmill
     */

    public void setCmlcbLockStateTimer(boolean lockState) {

        cmSafetyReleaseDelayTimer = (lockState) ? 0 : SAFETYKEY_RELEASE_DELAY;
    }

    private void checkCmSafetykeyRelease() {

        if(cmSafetyReleaseDelayTimer>0) {
            if(--cmSafetyReleaseDelayTimer == 0) {
                service.onCmSafetykeyRelease();
            }
        }
    }

    public void onDestroy() {
        safetykeyTimer.stop();
    }
}
