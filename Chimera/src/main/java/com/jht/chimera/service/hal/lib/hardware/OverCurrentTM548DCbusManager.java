package com.jht.chimera.service.hal.lib.hardware;

import android.graphics.Color;

import com.jht.androidcommonalgorithms.math.Resolution;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.datamatrixlib.DataMatrix;

public class OverCurrentTM548DCbusManager {

    public static final String TAG = "OverCurrentTM548DCbusManager";


    private static int TRIGGER_LIMIT_UPPER;
    private static int TRIGGER_LIMIT_LOWER;
    private boolean bCurrentOCtriggered = false;


    private final DataMatrix mDataMatrix = new DataMatrix();

    public static OverCurrentTM548DCbusManager getInstance() {
        return OverCurrentTM548DCbusManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final OverCurrentTM548DCbusManager INSTANCE = new OverCurrentTM548DCbusManager();
    }


    class OverCurrentTM548Protection {
        private int counter;
        private boolean ocOccurred;
        private final int firstIgnoreCount;
        private final int secondIgnoreCount;
        private final double speedDropKPH;
        private final double speedDropLimitKPH;   // 8.047KPH 5.0MPH

        OverCurrentTM548Protection(int firstIgnoreCount, int secondIgnoreCount, double speedDropKPH,double speedDropLimitKPH) {
            counter = 0;
            ocOccurred = false;
            this.firstIgnoreCount = firstIgnoreCount;
            this.secondIgnoreCount = secondIgnoreCount;
            this.speedDropKPH = speedDropKPH;
            this.speedDropLimitKPH = speedDropLimitKPH;
        }

        void eventOccurred() {
            counter++;
        }

        boolean dropSpeed() {
            float targetKph = DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT);
            FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
            if(!ocOccurred){
                return false;
            }

            if(counter == firstIgnoreCount) {

                if(Resolution.compare(targetKph, speedDropLimitKPH, 0.1) < 0) {
                    // Ignore.
                    return false;
                }
                // Need to drop the speed.
                if(Resolution.compare(targetKph - speedDropLimitKPH, config.speedKph.getMin(), 0.1) > 0) {
                    targetKph -= (float)speedDropKPH;

                    //show Speed Reduced
                    MessageCenterMessage msgSpeedReduced = new MessageCenterMessage()
                            .setMessageText((ContextUtil.getString(R.string.speed_reduce_pg)))
                            .setTextColor(Color.WHITE)
                            .setBackgroundColor(Color.BLACK)
                            .setHideTime(6);
                    DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msgSpeedReduced);

                }else{
                    targetKph = config.speedKph.getMin();
                }

                if(targetKph < speedDropLimitKPH)
                    targetKph = (float)speedDropLimitKPH;

                DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, targetKph);
                DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_KPH_FLOAT, targetKph);
                DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_MPH_FLOAT, SpeedConvertUtil.convertKphToMph(targetKph));
                ocOccurred = false;
                return true;
            }else if(counter > (firstIgnoreCount + secondIgnoreCount)){
                if(Resolution.compare(targetKph, speedDropLimitKPH, 0.1) < 0) {
                    // Ignore.
                    return false;
                }
                // Need to drop the speed.
                int dropCount = (counter - firstIgnoreCount) % secondIgnoreCount;
                if(dropCount == 0){
                    if(Resolution.compare(targetKph - speedDropLimitKPH, config.speedKph.getMin(), 0.1) > 0) {
                        targetKph -= (float)speedDropKPH;

                        //show Speed Reduced
                        MessageCenterMessage msgSpeedReduced = new MessageCenterMessage()
                                .setMessageText((ContextUtil.getString(R.string.speed_reduce_pg)))
                                .setTextColor(Color.WHITE)
                                .setBackgroundColor(Color.BLACK)
                                .setHideTime(6);
                        DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msgSpeedReduced);

                    }else{
                        targetKph = config.speedKph.getMin();
                    }
                }

                if(targetKph < speedDropLimitKPH){
                    targetKph = (float)speedDropLimitKPH;

                    //show Speed Reduced
                    MessageCenterMessage msgSpeedReduced = new MessageCenterMessage()
                            .setMessageText((ContextUtil.getString(R.string.speed_reduce_pg)))
                            .setTextColor(Color.WHITE)
                            .setBackgroundColor(Color.BLACK)
                            .setHideTime(6);
                    DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msgSpeedReduced);
                }

                DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, targetKph);
                DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_KPH_FLOAT, targetKph);
                DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_MPH_FLOAT, SpeedConvertUtil.convertKphToMph(targetKph));
                ocOccurred = false;
                return true;
            }

            return false;
        }


        private void reset() {
            counter = 0;
            ocOccurred = false;
        }


    }

    private final OverCurrentTM548Protection oc1 = new OverCurrentTM548Protection(1, 2, 3.2, 8.047);
    private final OverCurrentTM548Protection oc2 = new OverCurrentTM548Protection(5, 2, 1.6, 8.047);


    private OverCurrentTM548DCbusManager() {
        resetMaxSpeed();

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, value -> checkTreadmillStruggling());
        mDataMatrix.createIntAdapter(HALKeys.DATA_DC_BUS_CURRENT_OC_INT, this::dcBusTM548OCprotectionCount);
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, value -> {
            if(value == WorkoutKeys.Constants.RUN_STATE_STOP) {
                oc1.reset();
                oc2.reset();
                resetMaxSpeed();

            }
        });
        mDataMatrix.createFloatAdapter(SettingKeys.Workout.DATA_MAX_SPEED_KPH_FLOAT, value -> resetMaxSpeed());
        mDataMatrix.createFloatAdapter(SettingKeys.Workout.DATA_MAX_SPEED_MPH_FLOAT, value -> resetMaxSpeed());
        mDataMatrix.createIntAdapter(HALKeys.DATA_DC_BUS_TRIGGER_LIMIT_UPPER_INT, this::setTriggerLimitUpper);
        mDataMatrix.createIntAdapter(HALKeys.DATA_DC_BUS_TRIGGER_LIMIT_LOWER_INT, this::setTriggerLimitLower);
        mDataMatrix.createIntAdapter(HALKeys.DATA_GET_MOTOR_VOLTAGE_INT, value -> setTriggerPointerByVoltage());
    }

    private void setTriggerLimitUpper(int value){
        TRIGGER_LIMIT_UPPER = value;
    }

    private void setTriggerLimitLower(int value){
        TRIGGER_LIMIT_LOWER = value;
    }

    private void setTriggerPointerByVoltage(){
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        DataMatrix.putInt(HALKeys.DATA_DC_BUS_TRIGGER_LIMIT_UPPER_INT, frameConfig.getCbusTriggerLimitUpper());
        DataMatrix.putInt(HALKeys.DATA_DC_BUS_TRIGGER_LIMIT_LOWER_INT, frameConfig.getCbusTriggerLimitLower());
    }


    public void closeOverCurrentDCbus() {
        mDataMatrix.finish();
    }

    private void resetMaxSpeed() {

        DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_KPH_FLOAT, DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_SPEED_KPH_FLOAT));
        DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_MPH_FLOAT, DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_SPEED_MPH_FLOAT));
    }

    private void checkTreadmillStruggling() {
        if(oc1.ocOccurred){
                oc1.dropSpeed();
        }else if(oc2.ocOccurred){
                oc2.dropSpeed();
        }

    }

    private void dcBusTM548OCprotectionCount(int intDcBusCurrent) {
        boolean bOC1Trigger = false;
        boolean bOC2Trigger = false;

        if(intDcBusCurrent >= (TRIGGER_LIMIT_UPPER + 1))// OC1 trigger
        {
            bCurrentOCtriggered = true;

            bOC1Trigger = true;
            bOC2Trigger = false;

        }else if(intDcBusCurrent < TRIGGER_LIMIT_LOWER){// no trigger
            bCurrentOCtriggered = false;

            bOC1Trigger = false;
            bOC2Trigger = false;

            oc1.reset();
            oc2.reset();

        }else{ // OC2 trigger
            bCurrentOCtriggered = true;

            bOC1Trigger = false;
            bOC2Trigger = true;
        }

        if(bCurrentOCtriggered){
            bCurrentOCtriggered = false;

            if(bOC1Trigger)
                checkTM548CurrentOC1();

            if(bOC2Trigger)
                checkTM548CurrentOC2();
        }
    }

    private void checkTM548CurrentOC1() {
        if(FrameConfigManager.getInstance().checkOverCurrentEnabled()) {
            oc1.ocOccurred = true;
            oc1.eventOccurred();
            oc2.reset();
        }
    }

    private void checkTM548CurrentOC2() {
        if(FrameConfigManager.getInstance().checkOverCurrentEnabled()) {
            oc2.ocOccurred = true;
            oc2.eventOccurred();
            oc1.reset();
        }
    }

}
