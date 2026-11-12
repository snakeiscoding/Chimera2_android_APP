package com.jht.chimera.service.hal.lib.lcb;

import static com.jht.androidcommonalgorithms.units.UnitConverter.MPH_PER_KPH;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.LCBInterface;
import com.jht.datamatrixlib.DataMatrix;

public class TM548LCBDataHandler {

    //private static final String TAG = TM548LCBDataHandler.class.getName();
    private final FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
    private final LCBInterface mLcbInterface;

    private int mCurrentInclineADC = -1;

    private enum TM548ECBStatus {
        TM548_ECB_NONE(0x00),
        TM548_ECB_POWER_ON_LOCK(0x01),
        TM548_ECB_WORKOUT_RELEASE(0x02),
        TM548_ECB_WORKOUT_BRAKE(0x03),
        TM548_ECB_ESTOP_ON_LOCK(0x04),
        TM548_ECB_ESTOP_RESISTANCE(0x05),
        TM548_ECB_ESTOP_RELEASE(0x06);

        public int Value;

        TM548ECBStatus(int value) {
            Value = value;
        }
        public int getValue() {
            return Value;
        }

        public static TM548ECBStatus fromInt(int i) {
            for (TM548ECBStatus b : TM548ECBStatus.values()) {
                if (b.Value == i) {
                    return b;
                }
            }
            return null;
        }

        public byte getByteValue() {
            return (byte) Value;
        }
    }

    private enum TM548ControlState {
        TM548_CONTROL_NONE(0x00), // new TM548ControlState enum set
        TM548_CONTROL_INITIAL_INCLINE_SPEED(0x01), // initial incline > 0.5 => set incline = 0 , Speed = 0.2
        TM548_CONTROL_INITIAL_ECB(0x02), // initial finish incline = 0 , Speed = 0
        TM548_CONTROL_ECB_POWER_ON_LOCK(0x03), // ECB power on lock

        TM548_CONTROL_WORKOUT_RUNNING_INCLINE_SPEED(0x11), // Running incline speed check
        TM548_CONTROL_WORKOUT_RUNNING_ECB_RELEASE(0x12), // Running ECB release
        TM548_CONTROL_ECB_WORKOUT_RELEASE(0x13), // ECB release

        TM548_CONTROL_WORKOUT_PAUSE_INCLINE_SPEED(0x21), // Pause incline speed check
        TM548_CONTROL_WORKOUT_PAUSE_ECB_BRAKE(0x22), // Pause ECB brake
        TM548_CONTROL_ECB_PAUSE_BRAKE(0x23), // ECB brake

        TM548_CONTROL_WORKOUT_STOP_INCLINE_SPEED(0x31), // Stop incline speed check
        TM548_CONTROL_WORKOUT_STOP_ECB_BRAKE(0x32), // Stop ECB brake
        TM548_CONTROL_ECB_STOP_BRAKE(0x33), // ECB brake

        TM548_CONTROL_ESTOP_RELEASE(0x51), // EStop release check
        TM548_CONTROL_ESTOP_RELEASE_BRAKE(0x52), // EStop release brake
        TM548_CONTROL_ECB_ESTOP_RELEASE_BRAKE(0x53); // EStop release ECB brake

        public int Value;

        TM548ControlState(int value) {
            Value = value;
        }
        public int getValue() {
            return Value;
        }

        public static TM548ControlState fromInt(int i) {
            for (TM548ControlState b : TM548ControlState.values()) {
                if (b.Value == i) {
                    return b;
                }
            }
            return null;
        }

        public byte getByteValue() {
            return (byte) Value;
        }
    }

    private enum TM548RunState {
        TM548_RUN_STATE_RUNNING,
        TM548_RUN_STATE_PAUSE,
        TM548_RUN_STATE_STOP;

        public int getValue() {
            return ordinal();
        }
    }

    private TM548LCBDataHandler.TM548ECBStatus mTm548ECBStatus = TM548LCBDataHandler.TM548ECBStatus.TM548_ECB_NONE;
    private TM548LCBDataHandler.TM548ControlState mTM548ControlState;
    DataMatrix mDataMatrix = new DataMatrix();
    private boolean mTM548Initial = false;
    private boolean mTM548EStopTriggered = false;
    private float mTM548ECBBackupTargetIncline = 0.0f;
    private static final float TM548ECB_CONTROL_INCLINE_LIMIT = 0.5f;
    private static final float TM548ECB_CONTROL_SPEED_LIMIT = 0.2f;
    private static final float TM548ECB_BRAKE_SPEED_2MPH_LIMIT = 2.0f;
    private static final float TM548ECB_ESTOP_INCLINE_1_STAGE_BRAKE_LIMIT = 8.0f;
    private static final float TM548ECB_ESTOP_SPEED_1_STAGE_BRAKE_LIMIT = 9.0f;
    private boolean mHasSetMotorOffBrake = false;

    private static final int TM548_INCLINE_CONTROL_LIMIT_ADC = 2944;

    private boolean mTM548EInclineDownAction = false;
    private boolean mTM548EInclineUpAction = false;
    private JHTTimer checkInclineActionTimer;
    private boolean mNotJudgeInclineAction = false;

    public TM548LCBDataHandler(LCBInterface lcbInterface) {
        mLcbInterface = lcbInterface;

        mTM548ControlState = TM548ControlState.TM548_CONTROL_INITIAL_INCLINE_SPEED;
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, this::workoutRunStateChanged);
        if (BuildConfig.DEBUG) mDataMatrix.createIntAdapter(HALKeys.DATA_TM548_CONTROL_STATE_INT);

    }

    /**
     * public method in order to get rid of an unnecessary datamatrix key.
     * updated directly from the lcb, every second as part of the stats in LCBService
     * @param currentInclineADC the incline adc from the lcb
     */
    public void updateCurrentInclineADC(int currentInclineADC) {
        if (currentInclineADC != mCurrentInclineADC) {
            mCurrentInclineADC = currentInclineADC;
        }
    }

    public void processTM548ControlEcbState() {
        if(!DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL) && frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill) && frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02){
            //float targetSpeedKph = DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT);
            //float targetInclinePercentage = DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT);
            float actualSpeedKph = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);
            float actualInclinePercentage = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_INCLINE_FLOAT);
            if(actualSpeedKph < 0)
                actualSpeedKph = 0;

            checkTM548SetMotoOr2StageBrake(actualSpeedKph,actualInclinePercentage);

            if (BuildConfig.DEBUG) DataMatrix.putInt(HALKeys.DATA_TM548_CONTROL_STATE_INT, mTM548ControlState.getValue());
            switch (mTM548ControlState) {
                case TM548_CONTROL_INITIAL_INCLINE_SPEED: {
                    if (mCurrentInclineADC != -1) {
                        boolean inScope = Math.abs(TM548_INCLINE_CONTROL_LIMIT_ADC - mCurrentInclineADC) > 1024;
                        if(inScope) {
                            mLcbInterface.setTreadMillIncline(frameConfig.INCLINE_ADC_MIN);
                        }
                    }

                    mTM548Initial = true;
                    mTM548ControlState = TM548ControlState.TM548_CONTROL_INITIAL_ECB;

                    break;
                }
                case TM548_CONTROL_INITIAL_ECB: {
                    //TM548 control initial finish then ECB_POWER_ON_LOCK
                    byte byteECBPowerOnLock = mLcbInterface.setTM548EcbPowerOnLock();
                    mLcbInterface.setTM548EcbStatusAction(byteECBPowerOnLock);
                    mTm548ECBStatus = TM548ECBStatus.TM548_ECB_POWER_ON_LOCK;

                    //speed reset to 0
                    DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, 0.0f);

                    mTM548ControlState = TM548ControlState.TM548_CONTROL_ECB_POWER_ON_LOCK;
                    break;
                }

                case TM548_CONTROL_ECB_POWER_ON_LOCK: {
                    //Do Nothing
                    break;
                }

                case TM548_CONTROL_WORKOUT_RUNNING_INCLINE_SPEED: {
                    //TM548 check workout Running State
                    checkTM548WorkoutRunningState();
                    break;
                }
                case TM548_CONTROL_WORKOUT_RUNNING_ECB_RELEASE: {
                    if ((mTm548ECBStatus != TM548ECBStatus.TM548_ECB_WORKOUT_RELEASE) && (mTm548ECBStatus != TM548ECBStatus.TM548_ECB_ESTOP_RESISTANCE)) {//check ECB_WORKOUT_RELEASE , EStop trigger timing
                        //TM548 control ECB release
                        byte byteECBWorkoutRelease = mLcbInterface.setTM548EcbWorkoutRelease();
                        mLcbInterface.setTM548EcbStatusAction(byteECBWorkoutRelease);
                        mTm548ECBStatus = TM548ECBStatus.TM548_ECB_WORKOUT_RELEASE;
                    }

                    mTM548ControlState = TM548ControlState.TM548_CONTROL_ECB_WORKOUT_RELEASE;
                    break;
                }
                case TM548_CONTROL_ECB_WORKOUT_RELEASE: {
                    //Do Nothing
                    break;
                }


                case TM548_CONTROL_WORKOUT_PAUSE_INCLINE_SPEED: {
                    //TM548 check workout Pause State
                    checkTM548WorkoutPauseState();
                    break;
                }
                case TM548_CONTROL_WORKOUT_PAUSE_ECB_BRAKE: {
                    if (mTm548ECBStatus == TM548ECBStatus.TM548_ECB_WORKOUT_RELEASE) {
                        //TM548 control ECB brake
                        byte byteECBWorkoutBrake = mLcbInterface.setTM548EcbWorkoutBrake();
                        mLcbInterface.setTM548EcbStatusAction(byteECBWorkoutBrake);
                        mTm548ECBStatus = TM548ECBStatus.TM548_ECB_WORKOUT_BRAKE;
                    }
                    DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, 0.0f);

                    mTM548ControlState = TM548ControlState.TM548_CONTROL_ECB_PAUSE_BRAKE;
                    break;
                }
                case TM548_CONTROL_ECB_PAUSE_BRAKE: {
                    //Do Nothing
                    break;
                }


                case TM548_CONTROL_WORKOUT_STOP_INCLINE_SPEED: {
                    //TM548 check workout Stop State
                    checkTM548WorkoutStopState();
                    break;
                }
                case TM548_CONTROL_WORKOUT_STOP_ECB_BRAKE: {
                    if (mTm548ECBStatus == TM548ECBStatus.TM548_ECB_WORKOUT_RELEASE) {
                        //TM548 control ECB brake
                        byte byteECBWorkoutBrake = mLcbInterface.setTM548EcbWorkoutBrake();
                        mLcbInterface.setTM548EcbStatusAction(byteECBWorkoutBrake);
                        mTm548ECBStatus = TM548ECBStatus.TM548_ECB_WORKOUT_BRAKE;
                    }
                    DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, 0.0f);

                    mTM548ControlState = TM548ControlState.TM548_CONTROL_ECB_STOP_BRAKE;
                    break;
                }
                case TM548_CONTROL_ECB_STOP_BRAKE: {
                    //TM548 workout stop and control ECB brake then set to
                    if((actualInclinePercentage < TM548ECB_CONTROL_INCLINE_LIMIT) && ((actualSpeedKph < TM548ECB_CONTROL_SPEED_LIMIT) )){
                        mTM548ControlState = TM548ControlState.TM548_CONTROL_INITIAL_ECB;
                    }
                    break;
                }

                case TM548_CONTROL_ESTOP_RELEASE: {
                    //TM548 check EStop release
                    checkTM548EStopRelease();
                    break;
                }
                case TM548_CONTROL_ESTOP_RELEASE_BRAKE: {
                    byte byteECBEstopRelease = mLcbInterface.setTM548EcbEstopRelease();
                    mLcbInterface.setTM548EcbStatusAction(byteECBEstopRelease);
                    mTm548ECBStatus = TM548ECBStatus.TM548_ECB_ESTOP_RELEASE;

                    mTM548ControlState = TM548ControlState.TM548_CONTROL_ECB_ESTOP_RELEASE_BRAKE;
                    break;
                }
                case TM548_CONTROL_ECB_ESTOP_RELEASE_BRAKE: {
                    //TM548 ESop release and control ECB brake then set to initial
                    if((actualInclinePercentage < TM548ECB_CONTROL_INCLINE_LIMIT) && ((actualSpeedKph < TM548ECB_CONTROL_SPEED_LIMIT) )){
                        mTM548ControlState = TM548ControlState.TM548_CONTROL_INITIAL_ECB;
                    }

                    break;
                }

            }
        }
    }

    private void workoutRunStateChanged(int runState) {
        if(!DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL) && frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill) && frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02){
            if((!mTM548Initial) || (mTM548EStopTriggered))
                return;

            switch (runState) {
                case WorkoutKeys.Constants.RUN_STATE_RUNNING:{
                    new JHTTimer(200, () -> processTM548RunStateControl(TM548RunState.TM548_RUN_STATE_RUNNING.getValue())).start();
                    break;
                }
                case WorkoutKeys.Constants.RUN_STATE_SWITCHING:
                case WorkoutKeys.Constants.RUN_STATE_PAUSE:{
                    new JHTTimer(400, () -> processTM548RunStateControl(TM548RunState.TM548_RUN_STATE_PAUSE.getValue())).start();
                    break;
                }
                case WorkoutKeys.Constants.RUN_STATE_STOP:{
                    new JHTTimer(400, () -> processTM548RunStateControl(TM548RunState.TM548_RUN_STATE_STOP.getValue())).start();
                    break;
                }
            }
        }

    }

    private void processTM548RunStateControl(int runState) {
        if(!DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL) && frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill) && frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02){

            if(runState == TM548LCBDataHandler.TM548RunState.TM548_RUN_STATE_RUNNING.getValue()){
                //pause then resume mTM548ECBBackupTargetIncline != 0, incline back to orginal incline value
                if(mTM548ECBBackupTargetIncline > 0){
                    DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, mTM548ECBBackupTargetIncline);
                }

                mTM548ControlState = TM548ControlState.TM548_CONTROL_WORKOUT_RUNNING_INCLINE_SPEED;

            }else if(runState == TM548LCBDataHandler.TM548RunState.TM548_RUN_STATE_PAUSE.getValue()){
                //pause incline backup
                float targetInclinePercentage = DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT);
                float actualInclinePercentage = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_INCLINE_FLOAT);
                mTM548ECBBackupTargetIncline = targetInclinePercentage;

                //avoid IP , incline set 0 when pause or stop
                if( actualInclinePercentage >= TM548ECB_CONTROL_INCLINE_LIMIT) {
                    DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, 0.0f);
                    if(checkInclineActionTimer == null) {
                        checkInclineActionTimer = new JHTTimer(3000, () -> {
                            mNotJudgeInclineAction = false;
                            checkInclineActionTimer.stop();
                            checkInclineActionTimer = null;
                        });
                        checkInclineActionTimer.start();
                        mNotJudgeInclineAction = true;
                    }
                }

                //avoid IP , set speed to 0.2 mph
                if(actualInclinePercentage >= TM548ECB_CONTROL_INCLINE_LIMIT) {
                    float kph = SpeedConvertUtil.convertMphToKph(TM548ECB_CONTROL_SPEED_LIMIT); //speed set to 0.2 mph
                    DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, kph);
                }

                mTM548ControlState = TM548ControlState.TM548_CONTROL_WORKOUT_PAUSE_INCLINE_SPEED;

            }else if(runState == TM548RunState.TM548_RUN_STATE_STOP.getValue()){
                float actualInclinePercentage = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_INCLINE_FLOAT);

                //stop incline backup reset to 0
                mTM548ECBBackupTargetIncline = 0;

                if( actualInclinePercentage >= TM548ECB_CONTROL_INCLINE_LIMIT) {
                    DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, 0.0f);
                    if(checkInclineActionTimer == null) {
                        checkInclineActionTimer = new JHTTimer(3000, () -> {
                            mNotJudgeInclineAction = false;
                            checkInclineActionTimer.stop();
                            checkInclineActionTimer = null;
                        });
                        checkInclineActionTimer.start();
                        mNotJudgeInclineAction = true;
                    }
                }

                //avoid IP , set speed to 0.2 mph
                if(actualInclinePercentage >= TM548ECB_CONTROL_INCLINE_LIMIT) {
                    float kph = SpeedConvertUtil.convertMphToKph(TM548ECB_CONTROL_SPEED_LIMIT); //speed set to 0.2 mph
                    DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, kph);
                }

                mTM548ControlState = TM548ControlState.TM548_CONTROL_WORKOUT_STOP_INCLINE_SPEED;

            }

        }

    }

    private void checkTM548WorkoutRunningState() {
        float actualSpeedKph = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);
        if(actualSpeedKph < 0)
            actualSpeedKph = 0;

        float judgeSpeedKph = frameConfig.speedKph.getMinModel() / 2;

        //go or resume judge release brake
        if (actualSpeedKph >= judgeSpeedKph) {
            mTM548ControlState = TM548ControlState.TM548_CONTROL_WORKOUT_RUNNING_ECB_RELEASE;
        }

    }

    private void checkTM548WorkoutPauseState() {
        float actualSpeedKph = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);
        float actualInclinePercentage = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_INCLINE_FLOAT);
        if(actualSpeedKph < 0)
            actualSpeedKph = 0;

        if( mNotJudgeInclineAction )
            return;

        if (actualInclinePercentage < TM548ECB_CONTROL_INCLINE_LIMIT) { //incline value not real 0
            if (!mTM548EInclineDownAction) { //incline value not real 0

                float judgeSpeedKph = TM548ECB_BRAKE_SPEED_2MPH_LIMIT / (float) MPH_PER_KPH; // speed < 2mph brake

                //TM548 Pause check speed < 2 mph to brake
                if (actualSpeedKph <= judgeSpeedKph) {
                    mTM548ControlState = TM548ControlState.TM548_CONTROL_WORKOUT_PAUSE_ECB_BRAKE;
                }
            }
        }
    }

    private void checkTM548WorkoutStopState() {
        float actualSpeedKph = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);
        float actualInclinePercentage = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_INCLINE_FLOAT);
        if(actualSpeedKph < 0)
            actualSpeedKph = 0;


        //TM548 Pause Incline=0 check speed < 2 mph to brake
        if (actualInclinePercentage < TM548ECB_CONTROL_INCLINE_LIMIT) { //Incline value not real 0
            if( mNotJudgeInclineAction )
                return;

            if (!mTM548EInclineDownAction) { //incline value not real 0

                float judgeSpeedKph = TM548ECB_BRAKE_SPEED_2MPH_LIMIT / (float) MPH_PER_KPH; // speed < 2mph brake

                //TM548 Stop check speed < 2 mph to brake
                if (actualSpeedKph <= judgeSpeedKph) {
                    mTM548ControlState = TM548ControlState.TM548_CONTROL_WORKOUT_STOP_ECB_BRAKE;
                }
            }
        }else{
            //speed set to 0.2 mph
            float kph = SpeedConvertUtil.convertMphToKph(TM548ECB_CONTROL_SPEED_LIMIT);
            DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, kph);
        }
    }

    private void checkTM548EStopRelease() {
        float actualInclinePercentage = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_INCLINE_FLOAT);

        if (actualInclinePercentage < TM548ECB_CONTROL_INCLINE_LIMIT) { //incline value not real 0
            mTM548ControlState = TM548ControlState.TM548_CONTROL_ESTOP_RELEASE_BRAKE;
        }
    }

    public void checkTM548EcbEStopState(boolean pulled) {
        if(pulled){
            mTM548EStopTriggered = true;

            byte byteECBEstopResistance = mLcbInterface.setTM548EcbEstopResistance();
            mLcbInterface.setTM548EcbStatusAction(byteECBEstopResistance);
            mTm548ECBStatus = TM548ECBStatus.TM548_ECB_ESTOP_RESISTANCE;

            mTM548ECBBackupTargetIncline = 0;

        }else{
            mTM548EStopTriggered = false;
            mTM548ControlState = TM548ControlState.TM548_CONTROL_ESTOP_RELEASE;
        }
    }

    private void checkTM548SetMotoOr2StageBrake(float fAactualSpeedKph,float fAtualInclinePercentage) {
        float actualSpeedKph = fAactualSpeedKph;
        if(actualSpeedKph < 0)
            actualSpeedKph = 0;

        float judgeSpeedKph = TM548ECB_ESTOP_SPEED_1_STAGE_BRAKE_LIMIT / (float) MPH_PER_KPH;

        if((fAtualInclinePercentage >= TM548ECB_ESTOP_INCLINE_1_STAGE_BRAKE_LIMIT) && ((actualSpeedKph <= judgeSpeedKph))){//EStop incline >=8% & speed <= 9mph Motor power off brake
            if(mHasSetMotorOffBrake) {
                return;
            }

            //Set Motor Off Brake
            byte byteECBEstopMotorOffBrake = mLcbInterface.setTM548EcbEstopMotorOffBrake();
            mLcbInterface.setTM548EcbStatusAction(byteECBEstopMotorOffBrake);

            mHasSetMotorOffBrake = true;
        }else{
            if(!mHasSetMotorOffBrake) {
                return;
            }

            //Set Motor 2 Stage Brake
            byte byteECBEstopMotor2StageBrake = mLcbInterface.setTM548EcbEstopMotor2StageBrake();
            mLcbInterface.setTM548EcbStatusAction(byteECBEstopMotor2StageBrake);

            mHasSetMotorOffBrake = false;
        }

    }

    public void onInclineDownAction(boolean downAction) {
        mTM548EInclineDownAction = downAction;
    }

    public void onInclineUpAction(boolean upAction) {
        mTM548EInclineUpAction = upAction;
    }

}
