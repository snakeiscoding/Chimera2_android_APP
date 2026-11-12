package com.jht.chimera.service.hal.lib.hardware;

import com.jht.androidcommonalgorithms.math.Resolution;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.liberror.ErrorCodes;

public class OverCurrentErrorCodeManager {

    public static final String TAG = "OverCurrentErrorCode...";
    private static final double HIGH_END_KPH = 16.1; // 10 MPH
    private static final double LOW_END_KPH = 9.7; // 6 MPH
    private static final double MIN_KPH = 9.7; // 6 MPH

    private final DataMatrix mDataMatrix = new DataMatrix();

    public static OverCurrentErrorCodeManager getInstance() {
        return OverCurrentErrorCodeManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final OverCurrentErrorCodeManager INSTANCE = new OverCurrentErrorCodeManager();
    }

    static class OverCurrentProtection {
        private int counter;
        private final int ignoreCount;
        private final double speedDropHighKPH;  // OC1 : 3.2
        private final double speedDropMidKPH;   // 1.6
        private final double speedDropLowKPH;   // 0.8
        private final double minKPH;

        OverCurrentProtection(int ignoreCount, double speedDropHighKPH, double speedDropMidKPH, double speedDropLowKPH, double minKPH) {
            counter = 0;
            this.ignoreCount = ignoreCount;
            this.speedDropHighKPH = speedDropHighKPH;
            this.speedDropMidKPH = speedDropMidKPH;
            this.speedDropLowKPH = speedDropLowKPH;
            this.minKPH = minKPH;
        }

        void eventOccurred() {
            counter++;
        }

        boolean dropSpeed() {
            float targetKph = DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT);
            FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
            if(counter > (ignoreCount-1)) {

                if(Resolution.compare(targetKph, minKPH, 0.1) < 0) {
                    // Ignore.
                    return false;
                }
                // Need to drop the speed.
                if(Resolution.compare(targetKph, HIGH_END_KPH, 0.1) > 0) {
                    targetKph -= (float)speedDropHighKPH;
                }
                else if(Resolution.compare(targetKph, LOW_END_KPH, 0.1) > 0) {
                    targetKph -= (float)speedDropMidKPH;
                }
                else if(Resolution.compare(targetKph - speedDropLowKPH, config.speedKph.getMin(), 0.1) > 0) {
                    targetKph -= (float)speedDropLowKPH;
                }
                else {
                    targetKph = config.speedKph.getMin();
                }
                DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, targetKph);
                DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_KPH_FLOAT, targetKph);
                DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_MPH_FLOAT, SpeedConvertUtil.convertKphToMph(targetKph));
                reset();
                return true;
            }
            return false;
        }


        private void reset() {
            counter = 0;
        }


    }

    private boolean ocOccurred = false;

    private final OverCurrentProtection oc1 = new OverCurrentProtection(1, 3.2, 1.6, 0.8, MIN_KPH);

    public OverCurrentErrorCodeManager() {

        resetMaxSpeed();

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, value -> checkTreadmillStruggling());
        mDataMatrix.createIntAdapter(HALKeys.DATA_LCB_ERROR_CODE_INT, this::checkOverCurrentError);
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, value -> {
            if(value == WorkoutKeys.Constants.RUN_STATE_STOP) {
                oc1.reset();
                ocOccurred = false;
                resetMaxSpeed();
            }
        }, false);
        mDataMatrix.createFloatAdapter(SettingKeys.Workout.DATA_MAX_SPEED_KPH_FLOAT, value -> resetMaxSpeed(), false);
        mDataMatrix.createFloatAdapter(SettingKeys.Workout.DATA_MAX_SPEED_MPH_FLOAT, value -> resetMaxSpeed(), false);

    }

    public void closeOverCurrentErrorCode() {
        mDataMatrix.finish();
    }

    private void resetMaxSpeed() {
        DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_KPH_FLOAT, DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_SPEED_KPH_FLOAT));
        DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_MPH_FLOAT, DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_SPEED_MPH_FLOAT));
    }

    private void checkTreadmillStruggling() {
        float targetKph = DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT);
        float actualKph = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);
        if(FrameConfigManager.getInstance().checkOverCurrentEnabled() && (actualKph < targetKph)) {
            if(((targetKph - actualKph) / targetKph) >= 0.08) {
                if(oc1.dropSpeed()) {
                    speedChange();
                }
                else if(ocOccurred) {
                    DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_KPH_FLOAT, targetKph);
                    DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_MPH_FLOAT, SpeedConvertUtil.convertKphToMph(targetKph));
                }
            }
        }
    }

    private void speedChange() {
        oc1.reset();
    }

    private void checkOverCurrentError(final int error) {
        if (FrameConfigManager.getInstance().checkOverCurrentEnabled()) {
            if (ErrorCodes.MotorOverloading.matches(error)) {
                oc1.eventOccurred();
                ocOccurred = true;
            }
        }
    }
}
