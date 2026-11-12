package com.jht.chimera.service.hal.lib.lcb;

import static com.jht.androidcommonalgorithms.type.JHTCommonTypes.MachineClassificationType.Treadmill;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_CLIMBMILL_CZ;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_CLIMBMILL_HANDRAIL;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_CLIMBMILL_IR;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_CLIMBMILL_UCB;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_NOT_PULLED;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_TM_HYBRID_IR_ESTOP;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_TM_ROLLER_GUARD;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_TREADMILL;
import static com.jht.chimera.service.hal.lib.lcb.lcbcommlib.Constants.RECEIVE_DATA_SIZE_GET_CADENCE;
import static com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.TreadMillInterface.TM_ROLLER_GUARD_ERROR;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androidcommonalgorithms.util.SerialNumberUtil;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.SystemUtils;
import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.LcbToFramelMapper;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.frictiontest.ReceiveTreadmillOperate;
import com.jht.chimera.service.hal.lib.lcb.ReceiveTreadmillCadence;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.chimera.service.hal.lib.frictiontest.TreadmillOperateRxData;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DataUtils;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.LCBInterface;
import com.jht.chimera.service.hal.lib.machine.Machine;
import com.jht.chimera.service.hal.lib.machine.MachineData;
import com.jht.chimera.ui.machinesettings.pages.General;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.liberror.ErrorCodeManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LCBDataHandler {

    private static final String TAG = "LCBDataHandler";
    private final FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();

    private LCBInterface lcbInterface;
    private Context context;
    private MachineData machineData;

    private boolean bInBeltWarningCountdown = false;
    private int zeroRpmCount = 0;
    // private final boolean requireSafetyKeyReset = false;
    private TM548LCBDataHandler mTM548LcbDataHandler;
    private boolean mWasHybridTmSafetyKeyTriggered = false;


    public LCBDataHandler(@NonNull LCBInterface lcb, @NonNull Context context, @NonNull MachineData machineData) {
        this.lcbInterface = lcb;
        this.context = context;
        this.machineData = machineData;
        lcbInterface.startLCB();
    }


    /**
     * This is only for unit tests.  Do not use in other situations.  You will crash with null pointers.
     */
    public LCBDataHandler(Context context) {
        this.context = context;
    }


    public void onErrorCode(int errorCode) {
        ErrorCodeManager.handleLCBErrorCode(errorCode, LCBInfo._inCalibration);
    }

    public void onLcbType(byte lcbType) {
        onLcbType(lcbType, DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL));
    }

    public void onLcbType(byte lcbType, boolean isDemoMode) {
        int lcbTypeInt = lcbType & 0xff;
        LCBInfo.lcbType = lcbType;
        Log.d(TAG, String.format("===> onLCBType 0x%02X", lcbTypeInt));
        DataMatrix.putInt(HALKeys.DATA_LCB_TYPE_INT, lcbTypeInt);
        DataMatrix.putInt(SettingKeys.General.DATA_LCB_TYPE_INT, lcbTypeInt);

        // Logic that will show FTSU if the know LCB type doesn't match frame prefix for retail for console swaps.
        // Retail LCB currently do not support saving serial numbers.
        // This logic was removed from commercial where console will always trust the frame serial from LCB to determine frame type (CGC-22158)
        if (ProductConfig.isRetail()) {
            FrameConfigEnum frameConfigEnum = FrameConfigEnum.getFrameTypeFromString(DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING));
            boolean doesLcbMatchFrame = LcbToFramelMapper.doesLcbMatchFrame(lcbTypeInt, frameConfigEnum);
            boolean isKnownLcbType = LcbToFramelMapper.isKnownLcbType(lcbTypeInt);
            Log.i(TAG, "===> doesLcbMatchFrame " + doesLcbMatchFrame);
            Log.i(TAG, "===> isKnownLcbType " + isKnownLcbType);

            if (!isDemoMode && !doesLcbMatchFrame && isKnownLcbType && !DataMatrix.getBoolean(MainAppKeys.DATA_RUNNING_INSTRUMENTATION_TEST_BOOL)) {
                setupMachine();
            }
        }

        AndroidSetting.putInt(context.getContentResolver(), "chimera_lcb_type", lcbTypeInt); //NON-NLS
    }

    public void onLcbVersion(int major, int minor) {
        if (minor == -1) {
            String version = major + "";
            Log.d(TAG, "===> onLcbVersion " + version);
            DataMatrix.putString(HALKeys.DATA_VERSION_LCB_STRING, version);
            AndroidSetting.putString(context.getContentResolver(), "chimera_lcb_version", version + ".0.0.0"); //NON-NLS
        } else {
            String version = major + "." + minor;
            Log.d(TAG, "===> onLcbVersion " + version);
            DataMatrix.putString(HALKeys.DATA_VERSION_LCB_STRING, version);
            AndroidSetting.putString(context.getContentResolver(), "chimera_lcb_version", version + ".0.0"); //NON-NLS
        }
    }

    public void onRpm(int rpm) {
        if (!DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL)) {
            machineData.rpm = rpm;
        }

        //For C6 only ( zeroRpm = rpm < 24, because rpm value not always 0)
        checkZeroRPM(rpm < 24);


        if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
            if (mTM548LcbDataHandler != null) {
                mTM548LcbDataHandler.processTM548ControlEcbState();
            }
        }

    }

    public void updateCurrentInclineADC(int currentInclineADC) {
        if (mTM548LcbDataHandler != null) {
            mTM548LcbDataHandler.updateCurrentInclineADC(currentInclineADC);
        }
    }

    private void checkZeroRPM(boolean zeroRpm) {

        //For C6, user push belt on start workout countdown which will make 0 rpm should pause workout.
        if (!DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL) && frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill) && frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS) {

            if (!zeroRpm || !SystemUtils.isInWorkout()) {
                zeroRpmCount = 0;
                return;
            }

            boolean startSpeed = (float) (Math.round(machineData.targetKPH * 100)) / 100 == DataMatrix.getFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT);

            // In start workout, 0 rpm 4 secs (count every 500 ms)
            if (startSpeed && zeroRpmCount++ > 7) {

                //Checking : mainMotorStatus ( MCB not control motor), encoderRPM ( zero RPM state )
                if (!LCBInfo._status.mainMotorStatus && !LCBInfo._status.encoderRPMStatus) {
                    //pause workout
                    DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT);

                    // Only show message if not in demo mode
                    if (!DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL)) {
                        //show warning
                        MessageCenterMessage msgBeltRunning = new MessageCenterMessage()
                                .setMessageText(context.getString(R.string.stop_shaking_belt_pg))
                                .setTextColor(Color.WHITE)
                                .setBackgroundColor(Color.BLACK)
                                .setHideTime(10);
                        DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msgBeltRunning);
                    }
                }
                zeroRpmCount = 0;
            }
        }
    }


    public void onWatts(int watts) {
        machineData.watts = watts;
    }

    public void onSpm(int spm) {

        if (!DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL)) {
            machineData.spm = ((spm + 5) / 10); // round center
        }
    }


    public void onInclineADC(int inclineADC) {

    }

    @SuppressWarnings("unused")
    public void onPersonInPlace(boolean personInPlace) {
    }

    public void onTreadmillCadenceReceived(byte[] bytesCadenceData) {
        if (bytesCadenceData.length < RECEIVE_DATA_SIZE_GET_CADENCE) return; // make sure the payload is valid

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytesCadenceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        int cadenceSpm = DataUtils.shortToInt(byteBuffer.getShort());       // [0-1]
        // skip groundContactTimeLeft[2-3], flightTimeLeft[4-5],
        //      groundContactTimeRight[6-7], flightTimeRight[8-9],
        //      leftBalance[10-11], rightBalance[12-13]
        byteBuffer.position(14);
        int accumulateSteps = DataUtils.shortToInt(byteBuffer.getShort());  // [14-15]

        machineData.spm = cadenceSpm;
        ReceiveTreadmillCadence rxCadence = new ReceiveTreadmillCadence(cadenceSpm, accumulateSteps);
        DataMatrix.putParcelable(HALKeys.EVENT_RECEIVED_TREADMILL_CADENCE_PARCELABLE, rxCadence);
    }

    public void onTreadmillOperateReceived(TreadmillOperateRxData data) {
        float currentSpeedKph = Machine.rpmToKph(data.actualMotorSpeed);
        float currentIncline = Machine.adcToIncline(data.actualInclinePos);
        ReceiveTreadmillOperate rxOperate = new ReceiveTreadmillOperate(data.currentError, data.motorTorque,
                currentSpeedKph, currentIncline, data.motorVolts, data.motorCurrent, data.status, data.treadmillInUse);
        DataMatrix.putParcelable(HALKeys.ACTION_RECEIVED_TREADMILL_OPERATE_PARCELABLE, rxOperate);
    }

    @SuppressWarnings("unused")
    public void onTreadMillMotorInfo(byte[] bytesMotorInfo) {
        if (bytesMotorInfo.length > 14) {
            int intDCbusCurrent = DataUtils.unsignedBytesToInt(bytesMotorInfo[15], bytesMotorInfo[14]);
            checkTreadmillCurrentOC(intDCbusCurrent);
        }
    }

    private void checkTreadmillCurrentOC(int intDCbusCurrentvalue) {
        // Stop check Current OC when in workout
        if (!SystemUtils.isInWorkout()) return;

        DataMatrix.putInt(HALKeys.DATA_DC_BUS_CURRENT_OC_INT, intDCbusCurrentvalue);
    }


    @SuppressWarnings("unused")
    public void onBatteryVoltage(int batteryVoltage) {
    }


    public void onTmSafetykeyPulled(boolean pulled) {
        int previousState = DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT);
        int safetyKeyState = previousState;

        // update the EStop state
        if (frameConfig.HAS_IR_SENSOR) {
            boolean isPhysicalKeyActive = DataMatrix.getBoolean(HALKeys.DATA_IO_IS_SAFETY_PULLED_BOOL);
            boolean isCodeLockActive = (LCBInfo._mcbLockstate & LCBInfo.MCBState.MCB_CODE_LOCK.getId()) > 0;

            if (isPhysicalKeyActive) {
                mWasHybridTmSafetyKeyTriggered = true;
            }

            // Clean bits for Hybrid TM re-evaluation
            safetyKeyState &= ~SAFETY_KEY_TREADMILL;
            safetyKeyState &= ~SAFETY_KEY_TM_HYBRID_IR_ESTOP;

            if (isPhysicalKeyActive) {
                safetyKeyState |= SAFETY_KEY_TREADMILL;
            }

            // IR bit logic:
            // 1. If 02B2 and the physical key is IN, it's definitely an IR event.
            // 2. If 02B2 and the IR sensor is still TRIGGERED, we MUST keep the bit (to keep button grey).
            if (isCodeLockActive && (!isPhysicalKeyActive || lcbInterface.isHybridTmIrEStopTriggered())) {
                safetyKeyState |= SAFETY_KEY_TM_HYBRID_IR_ESTOP;
            }

            if (!isCodeLockActive && !isPhysicalKeyActive) {
                // Fully unlocked
                mWasHybridTmSafetyKeyTriggered = false;
            }
        } else {
            safetyKeyState = pulled ? (previousState | SAFETY_KEY_TREADMILL) : (previousState & ~SAFETY_KEY_TREADMILL);
        }

        // update the Roller Guard state
        safetyKeyState =  lcbInterface. isTmRollerGuardTriggered() ? (safetyKeyState | SAFETY_KEY_TM_ROLLER_GUARD) : (safetyKeyState & ~SAFETY_KEY_TM_ROLLER_GUARD);

        if (previousState != safetyKeyState) {
            // only update key for treadmill
            if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                DataMatrix.updateInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, safetyKeyState);

                // If the overweight check is needed, we need to check if the safety needs to be reset
                /*if (BuildConfig.CHECK_OVERWEIGHT_ERROR_02A1) {
                    if (requireSafetyKeyReset && pulled) {
                        requireSafetyKeyReset = false;
                        DataMatrix.putBoolean(HALKeys.DATA_REQUIRES_SAFETY_RESET_BOOL, false);
                        MessageCenterMessage msg = new MessageCenterMessage()
                                .setId(R.string.error_cycle_safety_key_pg)
                                .setHidden(true);
                        DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msg);
                    }
                }*/

            }
            //Set TM safety state for zero rpm control
            lcbInterface.setTreadmillSafetyLock(pulled);
            if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
                if (mTM548LcbDataHandler != null) {
                    mTM548LcbDataHandler.checkTM548EcbEStopState(pulled);
                }
            }
        }
    }

    public void onCmMastSafetykeyPulled(boolean pulled) {
        int lastSafetyKeyState = DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT);
        int safetyKeyState = lastSafetyKeyState & ~SAFETY_KEY_CLIMBMILL_UCB;
        if (pulled) safetyKeyState = lastSafetyKeyState | SAFETY_KEY_CLIMBMILL_UCB;
        if (lastSafetyKeyState != safetyKeyState) {
            // only update key for climbmill
            if ((frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill)) {
                DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, safetyKeyState);
            }
        }

    }

    public void onCmStartControl(boolean value) {
        boolean demoMode = DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL);
        // only update key for climbmill
        if (!demoMode && (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill)) {
            DataMatrix.putBoolean(HALKeys.DATA_CLIMB_START_CONTROL_STATUS_BOOL, value);
        }
    }


    public void onCmHandrailSafetykeyPulled(boolean pulled) {
        int lastSafetyKeyState = DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT);
        int safetyKeyState = lastSafetyKeyState & ~SAFETY_KEY_CLIMBMILL_HANDRAIL;
        if (pulled) safetyKeyState = lastSafetyKeyState | SAFETY_KEY_CLIMBMILL_HANDRAIL;
        if (lastSafetyKeyState != safetyKeyState) {
            // only update key for climbmill
            if ((frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill)) {
                DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, safetyKeyState);
            }
        }
    }

    public void onLcbStatusUpdate(LCBStatus status) {
        if (status.selfPower)
            Log.i(TAG, "onLcbStatus: selfpower mode.");
    }

    public void onLcbTmStatusUpdate(LCBStatus status) {

        updateTreadmillIncline(status.inclineUp || status.inclineDown);

        updateTreadmillmainMotorStatus(status.mainMotorStatus);

        //For performance+ treadmill, check the rem state
        checkTreadmillRPMStatus(status.mainMotorStatus, status.encoderRPMStatus);
    }

    private void updateTreadmillIncline(boolean isInclineMoving) {
        if (DataMatrix.getBoolean(HALKeys.DATA_INCLINE_MOVING_BOOL) != isInclineMoving) {
            DataMatrix.putBoolean(HALKeys.DATA_INCLINE_MOVING_BOOL, isInclineMoving);
        }
    }

    private void updateTreadmillmainMotorStatus(boolean mainMotorStatus) {
        if (DataMatrix.getBoolean(HALKeys.DATA_MAIN_MOTOR_STATUS_BOOL) != mainMotorStatus) {
            DataMatrix.putBoolean(HALKeys.DATA_MAIN_MOTOR_STATUS_BOOL, mainMotorStatus);
        }
    }

    private JHTTimer treadmillBeltWarningTimeout = new JHTTimer(5000, () -> {
        Log.i(TAG, "onLcbTmStatusUpdate: time out show belt running message ");

        // Only show message if not in demo mode
        if (!DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL)) {
            MessageCenterMessage msgBeltRunning = new MessageCenterMessage()
                    .setMessageText(context.getString(R.string.stop_shaking_belt_pg))
                    .setTextColor(Color.WHITE)
                    .setBackgroundColor(Color.BLACK)
                    .setHideTime(10);
            DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msgBeltRunning);
        }
    });

    private void checkTreadmillRPMStatus(boolean mainMotorStatus, boolean encoderRPMStatus) {

        //MCB not control motor
        if (!mainMotorStatus) {

            if (encoderRPMStatus) {
                startBeltWarning();

            } else {
                //Zero rpm, stop warning timer
                stopBeltWarning();

            }
            if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS)
                DataMatrix.putBoolean(HALKeys.DATA_RPM_ENCODER_STATUS_BOOL, encoderRPMStatus);
        } else {   //MCB control motor or rpm zero, stop timer
            stopBeltWarning();
        }
    }

    private void startBeltWarning() {
        if (!bInBeltWarningCountdown) {
            treadmillBeltWarningTimeout.start();
            bInBeltWarningCountdown = true;
        }
    }

    private void stopBeltWarning() {
        bInBeltWarningCountdown = false;
        treadmillBeltWarningTimeout.stop();
    }

    public void onEnClimbmillCZ(boolean cz) {
        int lastSafetyKeyState = DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT);
        int safetyKeyState = lastSafetyKeyState & ~SAFETY_KEY_CLIMBMILL_CZ;
        if (cz) safetyKeyState = lastSafetyKeyState | SAFETY_KEY_CLIMBMILL_CZ;
        if (lastSafetyKeyState != safetyKeyState) {
            // only update key for climbmill
            if ((frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill)) {
                DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, safetyKeyState);
            }
        }
    }

    public void onEnClimbmillIR(boolean ir) {
        int lastSafetyKeyState = DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT);
        int safetyKeyState = lastSafetyKeyState & ~SAFETY_KEY_CLIMBMILL_IR;
        if (ir) safetyKeyState = lastSafetyKeyState | SAFETY_KEY_CLIMBMILL_IR;
        if (lastSafetyKeyState != safetyKeyState) {
            // only update key for climbmill
            if ((frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill)) {
                DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, safetyKeyState);
            }
        }
    }

    public void onEnclimbmillCZcapaIRfreq(int cz, int ir) {

        //TODO Update CZ-capacitance IR-frequency for Engineer mode
        if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
            DataMatrix.putInt(SettingKeys.Calibration.DATA_CLIMBMILL_CZ_FREQUENCY_INT, cz);
            DataMatrix.putInt(SettingKeys.Calibration.DATA_CLIMBMILL_IR_FREQUENCY_INT, ir);
        }
    }

    public void onEnClimbMillWatts(int watts) {
        machineData.wattsLCB = watts;
    }

    /*
    public void onEnClimbmillCalibraton(boolean calibrating) {
        LCBInfo._inCalibration = calibrating;
    }
     */

    public void onInclineDownAction(boolean motorDownwardStatus) {
        if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
            if (mTM548LcbDataHandler != null) {
                mTM548LcbDataHandler.onInclineDownAction(motorDownwardStatus);
            }
        }
    }

    public void onInclineUpAction(boolean motorUpwardStatus) {
        if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
            if (mTM548LcbDataHandler != null) {
                mTM548LcbDataHandler.onInclineUpAction(motorUpwardStatus);
            }
        }
    }


    public void initLCBModule(boolean force) {

        if (!DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL) && !force) {
            Log.i(TAG, "LCB Data : Machine not match, don't initMachine. ");
            return;
        } else if (LCBInfo._machineInitialized && (!force)) return;
        else if (!force) Log.i(TAG, "LCB ready, start init LCB module ");
        else Log.i(TAG, "LCB : Re-init Machine ");

        //Init LCB module : Set LCB machine type and control type
        lcbInterface.initLCBModule(frameConfig);

        //Set machine control parameter
        setLCBMachineControl();

        //Set special frame machine control parameter or cmd
        setMachineFrameControl();

        LCBInfo._machineInitialized = true;

        //Start reply rpm
        lcbInterface.startReplyData(true);
    }

    private void setLCBMachineControl() {

        switch (frameConfig.machineType) {
            case Treadmill:
                setTreadmillMachineControl();
                break;
            case Elliptical:
                setEllipticalMachineControl();
                break;
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case RecumbentElliptical:
                setBikeMachineControl();
                break;
            case Stepper:
                setStepperMachineControl();
                break;
            case Ascent:
            case LowerBodyAscent:
                setAscentMachineControl();
                break;
            case Climbmill:
                setClimbmillMachineControl();
                break;
            case CardioSpinBike:
            case GroupSpinBike:
                setSpinBikeMachineControl();
                break;
            case JhtADboardRBike:
            case JhtADboardUBike:
                setJhtADboardBikeMachineControl();
                break;
            case JhtADboardElliptical:
                setJhtADboardEPMachineControl();
                break;
            default:
                break;
        }
    }

    private void setTreadmillMachineControl() {
        lcbInterface.setTreadmillRPM(0);

        //Set incline direction
        lcbInterface.setTreadmillInclineDirection(frameConfig.USES_REVERSE_INCLINE);

        if (DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT) != LcbType.LCB_JHT_VAVE_LS_Board.getValue()) {
            lcbInterface.setTreadMillMotorType(FrameConfigManager.getInstance().getMotorType());
        }

        //Set safetykey get lock state on/off
        lcbInterface.setTreadmillExtStatusOn(frameConfig.HAS_TM_LOCK_STATE);

        //Stop incline, prevent incline lock from getting relay close
        lcbInterface.setTmInclineActionStop();
        synchronized (FrameConfigManager.getInstance().FRAME_UPDATE_LOCK) {
            if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
                //get TM548 Power On Incline Adc
                lcbInterface.getTM548PowerOnInclineAdc();
            } else {
                lcbInterface.setTreadMillIncline(frameConfig.INCLINE_ADC_MIN);
            }
        }
        lcbInterface.setTreadMillNoPersonDetect(DataMatrix.getBoolean(SettingKeys.Workout.DATA_TREAD_SENSE_ENABLED_BOOL));
        lcbInterface.setTreadMillCurrentOcDetect(frameConfig.HAS_CURRENT_OC);
        lcbInterface.setTreadmillAutoRampUpAndLastRpmValue(frameConfig.HAS_TM_AUTO_RAMP_UP_AND_LAST_RPM_VALUE,
                frameConfig.RPM_RAMP_UP_STAGE_VALUE,
                frameConfig.RPM_RAMP_DOWN_STAGE_VALUE);

        //Set TM Over Speed enable
        lcbInterface.setTreadmillOverSpeed(frameConfig.HAS_TM_OVER_SPEED);
		
        //Set HybridTM Ir Detection enable
        lcbInterface.setHybridTMIrAddrIrDetection(frameConfig.HAS_IR_SENSOR);

    }

    private void setEllipticalMachineControl() {
        lcbInterface.setGearRatio(1750);
        lcbInterface.setGeneratorPole((byte) 0x04);
        lcbInterface.setResistanceType((((1 << 13) & 0xE000) | 100));
    }

    private void setBikeMachineControl() {
        if (LCBInfo.isChopper()) {
            lcbInterface.setGearRatio(840);
            lcbInterface.setResistanceType(0);
        } else {
            lcbInterface.setGearRatio(1500);
            lcbInterface.setResistanceType((0x2000 | 100));
        }
        lcbInterface.setGeneratorPole((byte) 0x04);
    }

    private void setStepperMachineControl() {
        lcbInterface.setGearRatio(1590);
        lcbInterface.setGeneratorPole((byte) 0x04);
        lcbInterface.setResistanceType(0);
    }

    private void setClimbmillMachineControl() {
        lcbInterface.setResistanceType(0);
        lcbInterface.setGeneratorPole((byte) 0x50);

        //Only lifestyle 0x17 open fan
        if (DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT) == 0x17)
            lcbInterface.setClimbmillLCBFanOn();

        lcbInterface.setmClimbmillControlZoneOn((frameConfig.HAS_CM_CONTROL_ZONE) && (DataMatrix.getBoolean(SettingKeys.Workout.DATA_CM_ESTOP_ENABLE_BOOL)));
        lcbInterface.setClimbmillNoPersonOnOff(DataMatrix.getBoolean(SettingKeys.Workout.DATA_CM_DETECT_PERSON_BOOL));
    }

    private void setAscentMachineControl() {
        lcbInterface.setGearRatio(1750);
        lcbInterface.setResistanceType((0x2000 | 100));
        lcbInterface.setGeneratorPole((byte) 0x04);
        lcbInterface.tuneInclineEnd1((byte) 0x14, (byte) 0x14);
        lcbInterface.setAscentIncline(0);
    }

    private void setSpinBikeMachineControl() {
        lcbInterface.getSpinBikeRaptorPara();

    }

    private void setJhtADboardBikeMachineControl() {
        lcbInterface.setGearRatio(1385);
    }

    private void setJhtADboardEPMachineControl() {
        lcbInterface.setGearRatio(1780);
    }

    //Same Frame but different Machine control to set
    private void setMachineFrameControl() {

        switch (frameConfig.machineType) {
            case Treadmill:
                //MODEL_PERFORMANCE_PLUS_02 Set ECB board power on (only for PERFORMANCE_PLUS_02)
                if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
                    if (mTM548LcbDataHandler == null) {
                        mTM548LcbDataHandler = new TM548LCBDataHandler(lcbInterface);
                    }
                    lcbInterface.getTreadmillMcbVoltage();
                }
                break;
            case Elliptical:
                //None
                break;
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case RecumbentElliptical:
                //None
                break;
            case Stepper:
                //None
                break;
            case Ascent:
            case LowerBodyAscent:
                //None
                break;
            case Climbmill:
                //None
                break;
            case CardioSpinBike:
            case GroupSpinBike:
            case ManualResistanceSpinBike:
                //None
                break;
            case JhtADboardRBike:
            case JhtADboardUBike:
                //None
                break;
            case JhtADboardElliptical:
                //None
                break;
            default:
                //None
                break;
        }
    }

    public void onDestroy() {
    }

    public void dataHandlerGetLcbSN() {
        lcbInterface.getILcbSerialNumber();
    }

    /**
     * Some LCB's have serial numbers stored on them.  If the both the console and LCB have a valid console
     * serial number the console wins.  If both the console and LCB have a valid frame serial number
     * the LCB wins.  If one is valid and the other is invalid then the valid one wins.  If they
     * are both invalid or equal move along and do nothing.
     *
     * @param lcbSerialNumbers Serial numbers read from the LCB.
     */
    public void dataHandlerParseLcbSN(String lcbSerialNumbers) {
        dataHandlerParseLcbSN(lcbSerialNumbers, DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL));
    }

    public void dataHandlerParseLcbSN(String lcbSerialNumbers, boolean isDemoMode) {

        String consoleSerialFromUCB = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
        String frameSerialFromUCB = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
        String consoleSerialFromLCB = "";
        String frameSerialFromLCB = "";
        boolean flashSerialToLCB = false;

        // In LCBCmdHandler, ModifyWriteLcbSerialNumber writes the serials to the LCB in Console/Frame order
        String[] serialNumbers = lcbSerialNumbers.split("/");
        // Factory sometimes flashes SNs without consoles, so if the LCB only reports a single Serial Number, we will assume it is for the Frame.
        if (serialNumbers.length == 1) {
            if (isValidSerialNumber(serialNumbers[0])) {
                frameSerialFromLCB = serialNumbers[0];
            }
        } else if (serialNumbers.length >= 2) {
            // Console is written to the left side (Index 0).
            if (isValidSerialNumber(serialNumbers[0])) {
                consoleSerialFromLCB = serialNumbers[0];
            }
            // Frame is written to the right side (Index 1).
            if (isValidSerialNumber(serialNumbers[1])) {
                frameSerialFromLCB = serialNumbers[1];
            }
        }

        Log.d(TAG, "LCB serial numbers " + consoleSerialFromLCB + " " + frameSerialFromLCB);
        Log.d(TAG, "UCB serial numbers " + consoleSerialFromUCB + " " + frameSerialFromUCB);

        // Skip the serial number logic if console in in demo mode
        if (isDemoMode) {
            Log.i(TAG, "Skipping serial number logic in demo mode");
            return;
        }

        try {

            // Logic to determine which frame serial number to use and sync them if different
            if (frameSerialFromLCB.equalsIgnoreCase(frameSerialFromUCB)) {
                Log.i(TAG, "Frame serial numbers from UCB and LCB are the same so do nothing");
            } else { // handle logic where the frame serial numbers are different
                if (isValidSerialNumber(frameSerialFromLCB)) {
                    Log.d(TAG, "Frame serial numbers from UCB and LCB are different so always trust the one from LCB");
                    DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(frameSerialFromLCB));
                } else if (isValidSerialNumber(frameSerialFromUCB)) {
                    Log.d(TAG, "Frame serial number from LCB is not valid so use valid one from UCB");
                    flashSerialToLCB = true;
                }
            }

            // Logic to determine which console serial number to use and sync them if different
            if (consoleSerialFromLCB.equalsIgnoreCase(consoleSerialFromUCB)) {
                Log.i(TAG, "Console serial numbers from UCB and LCB are the same so do nothing");
            } else { // handle logic where console serial numbers are different
                if (isValidSerialNumber(consoleSerialFromUCB)) {
                    Log.d(TAG, "Console serial numbers from UCB and LCB are different so always trust the one from UCB");
                    flashSerialToLCB = true;
                } else if (isValidSerialNumber(consoleSerialFromLCB)) {
                    Log.d(TAG, "Console serial number from UCB is not valid so use valid one from LCB");
                    DataMatrix.putString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(consoleSerialFromLCB));
                }
            }

            // Flash the serial numbers to the LCB if needed
            if (flashSerialToLCB) DataMatrix.sendEvent(HALKeys.ACTION_UPDATE_LCB_SERIAL_NUMBERS);

            // Logic that will update the frame type based on serial number
            String frameSerial = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
            FrameConfigEnum frameTypeFromSerial = FrameConfigEnum.getFrameFromSerial(frameSerial, 0); // use lcb type 0 to not force lcb type check
            FrameConfigEnum currentFrameType = FrameConfigManager.getInstance().getFrameType();
            Log.i(TAG, "frameSerial = " + frameSerial + " | getFrameFromSerial = " + frameTypeFromSerial);
            Log.i(TAG, "currentFrameType = " + currentFrameType);
            if (frameTypeFromSerial != null) {
                if (currentFrameType != null && currentFrameType.equals(frameTypeFromSerial)) {
                    Log.i(TAG, "Frame type from serial number is the same as current frame type.");
                } else {
                    Log.d(TAG, "Warning: The frame type was updated from " + currentFrameType + " to " + frameTypeFromSerial + ".");
                    FrameConfigManager.getInstance().setFrame(frameTypeFromSerial);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Serial Number Logic Error", e);
        }

    }

    private void setupMachine() {
        if (ProductConfig.isRetail()) { // PRKS-1545 - apparently the expectation is factory reset when switching frames
            //SettingKeys.resetFactoryDefault(); // just calling this seems to get it stuck, deferring to what the actual button does
            // Check DATA_IS_MACHINE_SETUP_BOOL as a way to avoid possibility of getting into a factory reset loop
            if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL)) {
                General.performFactoryReset(context, true);
            } else {
                LaunchAppUtil.launchSplashScreen();
            }
        } else { // just do FTSU
            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, false);
            LaunchAppUtil.launchMachineSetup();
        }
    }

    private boolean isValidSerialNumber(String sn) {
        if (sn == null) {
            return false;
        } else return sn.length() >= 9 && sn.length() <= 20;
    }

    @SuppressWarnings("unused")
    public void onSpinBikeInfoSpinDown(int spinDownStatus, int spinDownRPM, float spinDownTorqueScalar, float spinDownCorrectionFactor, float spinDownInertia) {

        if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike) {
            DataMatrix.putInt(HALKeys.DATA_SPINDOWN_TEST_STATUS_INT, spinDownStatus);
            DataMatrix.putInt(HALKeys.DATA_INFO_SPIN_DOWN_RPM_INT, spinDownRPM);
            DataMatrix.putFloat(HALKeys.DATA_INFO_SPIN_DOWN_TORQUE_SCALAR_FLOAT, spinDownTorqueScalar);
            DataMatrix.putFloat(HALKeys.DATA_INFO_SPIN_DOWN_CORRECTION_FACTOR_FLOAT, spinDownCorrectionFactor);
            DataMatrix.putFloat(HALKeys.DATA_INFO_SPIN_DOWN_INERTIA_FLOAT, spinDownCorrectionFactor);
        }
    }


    public void onSpinBikeRaptorPara(byte[] bytesRaptorPara) {
        try {
            if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike) {
                final int VALID_CAL_SIZE = 85;
                if (bytesRaptorPara == null || bytesRaptorPara.length != VALID_CAL_SIZE)
                    return; // make sure the array actually has data before accessing indices on it

                String paraString = DataMatrix.getString(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING);
                byte[] backup = DataUtils.hexStringToByteArray(paraString);

                //1555555501   ==>  length : 0x15 , header : 0x555555 , version : 0x01
                if (paraString.equals("") || (paraString.length() == 0) || backup.length != VALID_CAL_SIZE || (bytesRaptorPara[4] > backup[4])) {
                    //put raptor para data to DataMatrix
                    DataMatrix.putString(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING, DataUtils.byteArrayToHexString(bytesRaptorPara));
                    DataMatrix.putString(SettingKeys.Application.DATA_VERSION_RAPTOR_PARAMETER_STRING, String.valueOf(bytesRaptorPara[4]));

                    //Log.d(TAG, "===> AUTO BACKUP RAPTO PARAMETER | old RaptorPara=" + paraString);
                    //Log.d(TAG, "===> AUTO BACKUP RAPTOR PARAMETER | new RaptorPara=" + DataUtils.byteArrayToHexString(bytesRaptorPara));
                } else if (bytesRaptorPara[4] == backup[4]) {  //nothing to do
                    DataMatrix.putString(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING, DataUtils.byteArrayToHexString(bytesRaptorPara));
                    DataMatrix.putString(SettingKeys.Application.DATA_VERSION_RAPTOR_PARAMETER_STRING, String.valueOf(bytesRaptorPara[4]));
                } else if (backup[4] > bytesRaptorPara[4]) {
                    //Log.d(TAG, "===> AUTO RESTORE RAPTO PARAMETER | backup=" + backup[4] + ", lcb=" + bytesRaptorPara[4]);
                    DataMatrix.sendEvent(HALKeys.ACTION_RAPTOR_PARA_BACKUP);                    //Log.d(TAG, "===> setLcbCalculationParameters | current lcb calibration=" + mHAL.byteArrayToHexString(parameters));
                    //Log.d(TAG, "===> sendEvent(HALKeys.ACTION_RAPTOR_PARA_BACKUP) | new lcb calibration=" + paraString);
                    DataMatrix.putString(SettingKeys.Application.DATA_VERSION_RAPTOR_PARAMETER_STRING, String.valueOf(backup[4]));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onSpinBikeRaptorPara ERROR", e);
        }
    }


}
