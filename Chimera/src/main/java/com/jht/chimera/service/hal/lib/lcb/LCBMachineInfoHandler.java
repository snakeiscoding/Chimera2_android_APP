package com.jht.chimera.service.hal.lib.lcb;

import android.os.Handler;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.service.hal.LCBService;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DataUtils;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.LCBInterface;
import com.jht.datamatrixlib.DataMatrix;

public class LCBMachineInfoHandler {
    private static final String TAG = "LCBMachineInfoHandler";
    LCBInterface lcb;
    LCBService service;
    FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
    DataMatrix mDataMatrix = new DataMatrix();

    private Handler handler;

    private int ascentCountTimer = 0;
    private int magnetAngleTuningCountTimer = 0;

    private enum CalState {
        READY(0),
        CALIBRATING(1),
        SUCCESS(2),
        ERROR(3);

        private int value;

        private CalState(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public LCBMachineInfoHandler(LCBService service,LCBInterface lcb) {
        this.service = service;
        this.lcb = lcb;

        handler = new Handler();

        updateCalibrationStatus(CalState.READY);

    }

    /* TM Calibration */
    private int mPreviousADC = 0;
    private int mLowADC = 100;
    private int mHighADC = 100;

    /*  TM Calibration
     *   1. Send command to move the incline moto down
     *   2. Check the incline ADV every 3 secs
     *   3. Save min ADC
     *   4. Send command to move the incline moto up
     *   5. Check the incline ADV every 3 secs
     *   6. Save max ADC
     *   7. Update ADC value
     * */

    private JHTTimer downInclineTimer = new PeriodicTimer(5000, 3000, new JHTTimer.IOnTimer() {

        @Override
        public void onTimer() {

            if (Math.abs(service.machineData.inclineADC - mPreviousADC) < 200) {

                mLowADC = service.machineData.inclineADC;

                //stop incline
                lcb.setTmInclineActionStop();

	        downInclineTimer.stop();
                //Check calibration incline difference
                handler.postDelayed(() -> updateTMCalibrateData(), 1500);
            }
            mPreviousADC = service.machineData.inclineADC;
        }
    });

    private JHTTimer upInclineTimer = new PeriodicTimer(3000, 3000, new JHTTimer.IOnTimer() {

        @Override
        public void onTimer() {
            if (Math.abs(service.machineData.inclineADC - mPreviousADC) < 200) {

                mHighADC = service.machineData.inclineADC;

	        upInclineTimer.stop();
                //stop incline then down
                lcb.stopinclineAction();
                mPreviousADC = 0;
                startInclineDown();

            }
            mPreviousADC = service.machineData.inclineADC;
        }
    });

    private void startInclineDown() {


        //start calibration : incline down
        lcb.setTmInclineActionDown();

        downInclineTimer.start();
    }

    private void startInclineUp() {

	//initial value
	mPreviousADC = 0;
	mLowADC = 100;
	mHighADC = 100;

        //Set incline up
        lcb.setTmInclineActionUp();

        upInclineTimer.start();
    }

    private void updateTMCalibrateData() {

        //Stop receive incline adc value
        if(frameConfig.MODEL != FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
            lcb.setTreadmillAutoReplyIncline(false);
        }

        if ( mHighADC - mLowADC > 400) {

            //Update max/min calibration value
            mDataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MIN_INT, mLowADC+DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MIN_INT));
            mDataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MAX_INT, mHighADC-DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MAX_INT));
            FrameConfigManager.getInstance().updateSettings(); // update the frame config

            Log.i(TAG, "LCB  Machine : DATA_TREADMILL_INLINE_ADC_MIN_INT : " + DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MIN_INT) + "  DATA_TREADMILL_INLINE_ADC_MAX_INT : " + DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MAX_INT));
            Log.i(TAG, "LCB  Machine : DATA_TREADMILL_INCLINE_ADC_OFFSET_MIN_INT : " + DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MIN_INT) + "  DATA_TREADMILL_INCLINE_ADC_OFFSET_MAX_INT : " + DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MAX_INT));

            //Notify UI calibration success
            updateCalibrationStatus(CalState.SUCCESS);

        } else {
            Log.e(TAG, "LCB Calibration : fail ,  MIN_ADC "+service.machineData.inclineADC+" MAX_ADC "+mLowADC);
            updateCalibrationStatus(CalState.ERROR);
        }

        Log.i(TAG, "LCB Calibration : reset incline "+frameConfig.INCLINE_ADC_MIN);

        //Reset incline
        lcb.setTreadMillIncline(frameConfig.INCLINE_ADC_MIN);
    }

    public void treadmillCalibration(boolean start) {
        if (start) {

            lcb.unlockMachine();

            //Start ADC reply
            lcb.setTreadmillAutoReplyIncline(true);

            startInclineUp();

        } else {

            downInclineTimer.stop();
            upInclineTimer.stop();

            if(frameConfig.MODEL != FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
                lcb.setTreadmillAutoReplyIncline(false);
            }

            //stop incline
            lcb.setTmInclineActionStop();

            //Reset incline
            lcb.setTreadMillIncline(frameConfig.INCLINE_ADC_MIN);
        }
    }

    public void getEnClimbClimbmillFrameCalScaling() {
            lcb.getEnClimbClimbmillFrameCalScaling();
    }

    public void setEnClimbClimbmillFrameCalScaling(int intValue) {
        lcb.setClimbmillCalScaling(intValue);
    }

    /*
    *   Performance MCB Magnet angle auto tuning function
    *   1. Send 0x24 0x00 command to MCB to tune the magnet angle
    *   2. Send 0x24 0x01 command to get magnet angle
    *   3. Get 0xFF 0xFF from MCB, show "Tuning"
    *   4. Get magnet angle value (not 0xFF), show "Finished"
    *   Note:
    *   - Not define time out,use 40 secs
    *   - MCB don't have stop command
    * */


    private JHTTimer magnetAngleTuningTimer = new PeriodicTimer(2000, 1000, new JHTTimer.IOnTimer() {
        @Override
        public void onTimer() {
            //40 Sec Time out, magnet angle tuning fail
            if (magnetAngleTuningCountTimer++ > 40) {
                magnetAngleTuningCountTimer = 0;
	        Log.d(TAG, "updateMagnetAngleValue: timeout");
                startMagnetAngleAutoTuning(false);
                mDataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_MAGNET_ANGLE_INT, 0xFF);  //255
            }
        }
    });


    public void updateMagnetAngleValue(int data) {
        //Get magnet angle value, auto tuning finish
        if((data > 0) && (data < 3600)) {//0 ~3600
            Log.d(TAG, "updateMagnetAngleValue: finish "+data);
            startMagnetAngleAutoTuning(false);
            DataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_MAGNET_ANGLE_INT, data);
        }else {
            Log.d(TAG, "updateMagnetAngleValue: out of range");
            DataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_MAGNET_ANGLE_INT, 0xFF);  //255
        }
    }

    public void updateMotorTypeValue(int data) {
        DataMatrix.putInt(HALKeys.DATA_GET_MOTOR_TYPE_INT, data);
    }

    public void updateMcbVoltageValue(int data) {
        DataMatrix.putInt(HALKeys.DATA_GET_MOTOR_VOLTAGE_INT, data);
    }

    public void startMagnetAngleAutoTuning(boolean start) {

        // Check treadmill or magnet angle value
        if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill){
			
            //start/stop magnet angle auto tuning
            lcb.startTreadmillMagnentAngleTuning(start);
			
            //Timeout timer
            if(start) {
                magnetAngleTuningCountTimer = 0;
                magnetAngleTuningTimer.start();
            }else {
                magnetAngleTuningTimer.stop();
            }
        }
    }

    public void getMagnetAngle() {
            lcb.getTreadmillMagnentAngle();
    }

    public void getTreadMillMotorType() {
        lcb.getTreadMillMotorType();
    }

    public void switchPFCfunction(boolean bOn) {
        //switch PFC mode
        if (bOn)
			lcb.switchPFCfunction(true);
        else
			lcb.switchPFCfunction(false);
    }

    /*  CM Calibration
     *   1. Send command to LCB for calibration (time : DATA_CLIMBMILL_CAL_TIME_SEC_INT).
     *   2. Timer timeout checking is LCB still on calibration (DATA_CLIMBMILL_CAL_TIME_SEC_INT +CLIMBMILL_CAL_DELAY)
     *   3. Check B13 of the climbmill status is 0
     *   4. Send command to LCB for getting calibration scaling
     *   5. Get scaling, stop calibration.
     *   6. Console, calibration complete, show scaling.
     * */
    private boolean mCMCalibrateSt = false;

    public void onCMCalibrateStatus(boolean calibrating) {
        //Get calibration state from LCB
        mCMCalibrateSt = calibrating;
    }

    public void onClimbmillCalScaling(int Scaling) {
        if(!LCBInfo._inCalibration)
            return;
        Log.i(TAG, "LCB Machine : Get Calibration Scaling " + Scaling);
        //Get result, stop check result timer
        climbmillCalibrationResultWaitTimer.stop();

        //Notify UI calibration success
        updateCalibrationStatus(CalState.SUCCESS);

        //Stop Calibration
        lcb.setClimbmillCommand((byte) 0x01);
    }
    public void onEnClimbMillFrameCalScaling(int Scaling) {
        Log.i(TAG, "LCB Machine : Get Frame Calibration Scaling " + Scaling);
        //Notify UI show factor calibration
        mDataMatrix.putInt(HALKeys.DATA_CLIMBMILL_FRAME_SCALE_INT, Scaling);
    }
    

    private void checkClimbmillCalibrationScaling() {

        climbmillCalibrationTimer.stop();

        //send command 0x8B to get status
        lcb.getClimbmillCalScaling();

        //wait calibration result, timeout mean fail
        climbmillCalibrationResultWaitTimer.start(2 * 1000);
    }

    private JHTTimer climbmillCalibrationResultWaitTimer = new JHTTimer(new JHTTimer.IOnTimer() {
        @Override
        public void onTimer() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //Stop Calibration
                    lcb.setClimbmillCommand((byte) 0x01);
                    updateCalibrationStatus(CalState.ERROR);
                }
            });
        }
    });

    private JHTTimer climbmillCalibrationTimer = new JHTTimer(new JHTTimer.IOnTimer() {
        @Override
        public void onTimer() {
            handler.post(new Runnable() {
                @Override
                public void run() {

                    if(mCMCalibrateSt) {
                        Log.e(TAG, "LCB Machine : CM calibration overtime");
                        //Over Time, Stop Calibration
                        lcb.setClimbmillCommand((byte) 0x01);
                        updateCalibrationStatus(CalState.ERROR);
                    }
                    else {
                        Log.i(TAG, "LCB Machine : CM calibration finish, get scaling ");
                        //send command 0x8B to get status
                        checkClimbmillCalibrationScaling();
                    }
                }
            });
        }
    });

    private JHTTimer climbmillUnlockDelayTimer = new JHTTimer(new JHTTimer.IOnTimer() {
        @Override
        public void onTimer() {
            handler.post(new Runnable() {
                @Override
                public void run() {

                    byte[] data = new byte[6];

                    //Set calibration weight, spm & time
                    int intWeight = (int) (DataMatrix.getFloat(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_WEIGHT_KG_FLOAT) * 10);
                    byte byte0Weight = DataUtils.byteGetNthOfInt(intWeight, 1);
                    data[0] = (byte)byte0Weight;
                    byte byte1Weight = DataUtils.byteGetNthOfInt(intWeight, 0);
                    data[1] = (byte)byte1Weight;
                    int intSPM = (int) (DataMatrix.getInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_SPM_INT) * 10);
                    byte byte0SPM = DataUtils.byteGetNthOfInt(intSPM, 1);
                    data[2] = (byte)byte0SPM;
                    byte byte1SPM = DataUtils.byteGetNthOfInt(intSPM, 0);
                    data[3] = (byte)byte1SPM;
                    int intSecond = (int) (DataMatrix.getInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_TIME_SEC_INT));
                    byte byte0Second = DataUtils.byteGetNthOfInt(intSecond, 1);
                    data[4] = (byte)byte0Second;
                    byte byte1Second = DataUtils.byteGetNthOfInt(intSecond, 0);
                    data[5] = (byte)byte1Second;

                    //send command 0x8F to start
                    lcb.setClimbmillCalibration(data);
                    climbmillCalibrationTimer.start((DataMatrix.getInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_TIME_SEC_INT)+12)*1000 );
					
                }
            });
        }
    });


    public void climbmillCalibration(boolean start) {

        if (start) {
            //unlock machine
            lcb.unlockMachine();

            //delay timer, waiting unlock
            climbmillUnlockDelayTimer.start(3000);
        } else {
            updateCalibrationStatus(CalState.ERROR);
            //send command 0x8A to stop
            lcb.setClimbmillCommand((byte) 0x01);
            climbmillCalibrationTimer.stop();
            climbmillCalibrationResultWaitTimer.stop();
            lcb.stopMachine();
        }
    }

    private JHTTimer ascentCalibratCountTimer = new PeriodicTimer(2000, 1000, new JHTTimer.IOnTimer() {

        @Override
        public void onTimer() {

            if (ascentCountTimer++ > 3 * 60) {
                //Timeout Stop incline action 0xF5
                updateCalibrationStatus(CalState.ERROR);
                ascentCalibration(false);
            } else if (!LCBInfo._status.initialCalibration) {
                updateCalibrationStatus(CalState.SUCCESS);
                ascentCalibration(false);
            }
        }
    });

    public void ascentCalibration(boolean start) {
        if (start) {
            lcb.autoAscentCalibrateIncline();
            ascentCalibratCountTimer.start();

        } else {
            //Stop incline action 0xF5
            ascentCalibratCountTimer.stop();
            lcb.stopAscentCalibrate();
            lcb.setAscentIncline(0);
        }

    }

    private void updateCalibrationStatus(CalState state) {

        LCBInfo._inCalibration = (state == CalState.CALIBRATING);

        DataMatrix.putInt(HALKeys.DATA_CALIBRATION_STATUS_INT, state.value);

    }

    public void startCalibration(boolean start) {

        updateCalibrationStatus((start) ? CalState.CALIBRATING : CalState.READY);

        if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
            treadmillCalibration(start);
        } else if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill ) {
            climbmillCalibration(start);
        } else if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Ascent) {
            ascentCalibration(start);
        }

    }

    public void handleCMpersonInPlaceError() {
        if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill ) {
            lcb.setClimbmillCommand((byte) 0x01);
            updateCalibrationStatus(CalState.ERROR);
		}
    }
    

    public void onDestroy() {

        downInclineTimer.stop();
        upInclineTimer.stop();

        climbmillCalibrationResultWaitTimer.stop();
        climbmillCalibrationTimer.stop();

        ascentCalibratCountTimer.stop();

    }
}
