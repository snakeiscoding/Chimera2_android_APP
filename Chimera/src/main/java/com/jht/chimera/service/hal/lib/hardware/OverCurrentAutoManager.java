package com.jht.chimera.service.hal.lib.hardware;

import android.graphics.Color;

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
import com.jht.chimera.service.hal.lib.machine.Machine;
import com.jht.datamatrixlib.DataMatrix;

public class OverCurrentAutoManager {

    public static final String TAG = "OverCurrentAutoManager";

    private boolean bAutoOCstruggling = false;
    private boolean bAutoOCchange = false;
    private float speedAutoOCKPH;

    private boolean bAutoOCtrigger;

    private final DataMatrix mDataMatrix = new DataMatrix();

    public static OverCurrentAutoManager getInstance() {
        return OverCurrentAutoManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final OverCurrentAutoManager INSTANCE = new OverCurrentAutoManager();
    }

    private int struggleCounter = 0;

    public OverCurrentAutoManager() {

        resetMaxSpeed();

	    bAutoOCtrigger = false;
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, value -> checkTreadmillStruggling());
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, value -> {
            if(value == WorkoutKeys.Constants.RUN_STATE_STOP) {
                struggleCounter = 0;
                resetMaxSpeed();
            }
        });
        
        mDataMatrix.createFloatAdapter(SettingKeys.Workout.DATA_MAX_SPEED_KPH_FLOAT, value -> resetMaxSpeed(),false);
        mDataMatrix.createFloatAdapter(SettingKeys.Workout.DATA_MAX_SPEED_MPH_FLOAT, value -> resetMaxSpeed(), false);

        mDataMatrix.createBoolAdapter(HALKeys.ACTION_AUTO_OVER_CURRENT_STATUS_BOOL, hasDetect -> {
			bAutoOCtrigger = hasDetect;
			if (hasDetect) {
				startAutoOverCurrentHandle();
			}else{
				stopAutoOverCurrentHandle();
			}
        });		
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, value -> checkAutoOCstruggling());
    }

    public void closeOverCurrentAuto() {
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
                if (bAutoOCtrigger){
		            struggleCounter++;
	                if(struggleCounter >= 40){
	                    targetKph = actualKph;
	                            DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, targetKph);
	                    struggleCounter = 0;
	                }
        	 }
            }
        }
    }

    private void startAutoOverCurrentHandle() {
        bAutoOCstruggling = true;
    }

    private void stopAutoOverCurrentHandle() {
        checkAutoOCstruggling();
        bAutoOCstruggling = false;
        if(bAutoOCchange){
            bAutoOCchange = false;
            DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_KPH_FLOAT, speedAutoOCKPH);
            DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_MPH_FLOAT, SpeedConvertUtil.convertKphToMph(speedAutoOCKPH));

            float actualKph = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);
            DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, actualKph);
        }
    }

    private void checkAutoOCstruggling() {
		if(!bAutoOCstruggling) return;
	
	        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
	        float targetKph = DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT);
	        float actualKph = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);

	        int targetRpm = (int)  Machine.kphToRpm(targetKph);
	        int actualRpm = (int) Machine.kphToRpm(actualKph);

	        if((targetRpm - actualRpm) > 10) {
				bAutoOCchange = true;
				speedAutoOCKPH = actualKph;
	                    MessageCenterMessage msgSpeedReduced = new MessageCenterMessage()
                                .setMessageText((ContextUtil.getString(R.string.speed_reduce_pg)))
	                            .setTextColor(Color.WHITE)
	                            .setBackgroundColor(Color.BLACK)
	                            .setHideTime(6);
	                    DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msgSpeedReduced);
	        }

    }

}
