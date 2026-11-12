package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface;

import static com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.TreadMillInterface.TM_ROLLER_GUARD_ERROR;

import android.content.Context;
import android.util.Log;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.process.KillProcessHelper;
import com.jht.chimera.service.hal.lib.lcb.TM548ECBControl;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ControlType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DataUtils;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IUniDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.UniDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IGeneralHandlerCallback;
import com.jht.chimera.service.hal.lib.machine.Machine;
import com.jht.serialport.SerialPortConfiguration;

public class LCBInterface {

    private ILCBCallback serviceCallback; // Used to provide library notifications.

    private static final String TAG = "LCBInterface";
    private GeneralHandler mGeneralHandler;
    private ILcbComm mRealLcb;
    private IDeviceDataTransmitted mDeviceDataTransmitted;
    //    private final String LCB_DEVICE_PATH = "/dev/ttymxc3";
    private final String LCB_DEVICE_PATH = ProductConfig.portMappings.lcbPath(); //NON-NLS
    private final SerialPortConfiguration.BaudRate LCB_BAUD_RATE = SerialPortConfiguration.BaudRate.B9600;
    private IUniDataTransmitted mUniDataTransmitted;

    private static JHTCommonTypes.MachineClassificationType machineClassType = JHTCommonTypes.MachineClassificationType.Bike;
    private static JHTCommonTypes.MachineType machineType = JHTCommonTypes.MachineType.RecumbentBike;
    private static boolean isLifestyle = false;
    private byte oldLCBStatus = 0x00;
    private TreadMillInterface mTreadmill = null;
    private BikeInterface mBike = null;
    private StepperInterface mStepper = null;
    private EnClimbmillInterface mClimbmill = null;
    private EllipticalInterface mElliptical = null;
    private AscentInterface mAscent = null;
    private SpinBikeInterface mSpinBike = null;
    private TcvFxSpinBikeInterface mTcvFxSpinBike = null;
    private JhtADboardInterface mJhtADboard = null;
    //private LcbType machineLcbType = LcbType.LCB_UNKNOWN;
    private RpmRamper mRpmRamp = null;
    private static TM548ECBControl mTM548ECBControl = null;
    private static FrameConfig mFrameConfig = null;
    private static final byte DEFAULT_TM548_STATUS_BYTE = (byte) 0x03;//default initial Bit 1 & Bit 2 set 1

    private IGeneralHandlerCallback mGeneralHandlerCallback = new IGeneralHandlerCallback() {
        @Override
        public void onErrorCode(int lcbErrorCode) {
            //Log.d(TAG, String.format("LcbHandleCallback onErrorCode lcbErrorCode=%X, ", lcbErrorCode));
            if (serviceCallback != null) {

                if (lcbErrorCode == 690)//0x02B2
                {
                    if (isHybridTmIrEnabled()) {
                        if (!mTreadmill.isHybridTmIrEStopTriggered()) {
                            mTreadmill.triggerHybridTmIrEStopCheck();
                        }
                    } else {
                        serviceCallback.onLCBSafetykeyLockedCode(true);
                    }
                } else if (lcbErrorCode == 689) //0x02B1
                {
                    if (isHybridTmIrEnabled() && mTreadmill.isHybridTmIrEStopTriggered()) {
                        Log.d(TAG, "Hybrid TM IR-EStop active, ignoring 02B1");
                    } else {
                        serviceCallback.onLCBSafetykeyLockedCode(false);
                    }
//<20260413 ESDV-1184 Vortex-Chimera modify Hybrid TM IR-EStop no send 02B2 & 02B1 error code to UI.
                } else if (mTreadmill != null && lcbErrorCode == TM_ROLLER_GUARD_ERROR) {
                    Log.d(TAG, "Roller guard error code detected");
                    mTreadmill.triggerRollerGuard();
                    serviceCallback.onTmRollerGuard(true);
                } else {
                    serviceCallback.onErrorCode(lcbErrorCode);
                }
            }
        }

        @Override
        public void onLcbType(byte lcbType) {
            int intLcbType = DataUtils.unsignedByteToInt(lcbType);
            LcbType machineLcbType = LcbType.fromInt(intLcbType);

			
            if (serviceCallback != null){
                serviceCallback.onLcbType(lcbType);
            }
        }

        @Override
        public void onLcbVersion(int major, int minor) {
            if (serviceCallback != null)
                serviceCallback.onLcbVersion(major, minor);
        }

        @Override
        public void onLcbInitialFinish() {
            if (serviceCallback != null)
                serviceCallback.onLcbInitialFinish();
        }

        @Override
        public void onInclineMotorDownwardStatus(boolean motorDownwardStatus) {
            if (serviceCallback != null)
                serviceCallback.onInclineMotorDownwardStatus(motorDownwardStatus);
        }

        @Override
        public void onInclineMotorUpwardStatus(boolean motorUpwardStatus) {
            if (serviceCallback != null)
                serviceCallback.onInclineMotorUpwardStatus(motorUpwardStatus);
        }

        @Override
        public void onMainMotorStatus(boolean motorStatus) {
            if (serviceCallback != null)
                serviceCallback.onMainMotorStatus(motorStatus);
        }

        @Override
        public void onEncoderRPMStatus(boolean encoderRPMStatus) {
            if (serviceCallback != null)
                serviceCallback.onEncoderRPMStatus(encoderRPMStatus);
        }

        @Override
        public void onBatteryCapacityStatus(boolean batteryCapacityStatus) {
            if (serviceCallback != null)
                serviceCallback.onBatteryCapacityStatus(batteryCapacityStatus);
        }

        @Override
        public void onCommandErrorStatus(boolean commandErrorStatus) {
            if (serviceCallback != null)
                serviceCallback.onCommandErrorStatus(commandErrorStatus);
        }

        @Override
        public void onInitialCalibrationStatus(boolean initialCalibrationStatus) {
            if (serviceCallback != null)
                serviceCallback.onInitialCalibrationStatus(initialCalibrationStatus);
        }

        @Override
        public void onBatteryVoltage(int intBatteryVoltage) {
            if (serviceCallback != null)
                serviceCallback.onBatteryVoltage(intBatteryVoltage);

        }

        @Override
        public void onLcbStatus(byte lcbstatus) {
            if (oldLCBStatus != lcbstatus && serviceCallback != null) {
                if (machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill){
                    serviceCallback.onLcbTMStatus(lcbstatus);
                }else {
                    serviceCallback.onLcbStatus(lcbstatus);
                }
                oldLCBStatus = lcbstatus;
            }
        }

        @Override
        public void onClearLcbSnStatus(boolean clearStatus) {

        }

        @Override
        public void onSetLcbSnStatus(boolean setStatus) {
            if (setStatus) {
                Log.v(TAG, "Set Lcb Serial Number success.");
            } else {
                Log.e(TAG, "Set Lcb Serial Number error !!!");
            }

        }

        @Override
        public void onGetLcbSerialNumber(boolean getStatus, String getString) {
            if (getStatus) {
                Log.v(TAG, "Get Lcb Serial Number success.");
                if (serviceCallback != null)
                    serviceCallback.onReadLcbSerialNumber(getString);
            } else {
                Log.e(TAG, "Get Lcb Serial Number error !!!");
            }

        }

        @Override
        public void onGetLcbSnMaxLength(boolean getStatus, int intMaxLength) {

        }

        @Override
        public void onIntoLcbUpdateMode(boolean intoSuccess) {
            if (intoSuccess) {
                Log.e(TAG, "Update Lcb into lcb update mode success.");
            } else {
                Log.e(TAG, "Update Lcb into lcb update mode error !!!");
            }
        }

        @Override
        public void onLcbFinishMagnetAngleValue(int intMagnetAngle) {
                Log.i(TAG, "Calibration Magnet Angle value = "+intMagnetAngle);
            if (serviceCallback != null)
				serviceCallback.onTreadmillMagnetAngleValue(intMagnetAngle);
        }

        @Override
        public void onMcbMotoType(int intMotoType) {
            if (serviceCallback != null)
                serviceCallback.onTreadmillMotorTypeValue(intMotoType);
        }

        @Override
        public void onGetMcbVoltageValue(int intVoltage) {
            Log.i(TAG, "Get Mcb Voltage value = "+intVoltage);
            if (serviceCallback != null)
                serviceCallback.onTreadmillMcbVoltageValue(intVoltage);
        }
    };

    private Context context;

    public LCBInterface(final ILCBCallback callback, Context context)//, LCBMessageType.LCB_CONTROL_TPYE type)
    {
        this.serviceCallback = callback;
        this.context = context;
        mGeneralHandler = GeneralHandler.getInstance();
        mRealLcb = LcbComm.getInstance();
        mDeviceDataTransmitted = DeviceDataTransmitted.getInstance();
        mUniDataTransmitted = UniDataTransmitted.getInstance();

        mGeneralHandler.setGeneralHandlerCallback(mGeneralHandlerCallback);

    }


    public boolean startLCB() {
        mGeneralHandler.setContext(context);
        return mGeneralHandler.autoStartGeneralHandlerLCB(mRealLcb, mDeviceDataTransmitted, mUniDataTransmitted, LCB_DEVICE_PATH, LCB_BAUD_RATE);
    }

    public void initLCBModule(FrameConfig frameConfig) {

	mFrameConfig = frameConfig;
        machineClassType = frameConfig.machineClassType;
        machineType = frameConfig.machineType;
        isLifestyle = frameConfig.isLifestyle;

        unbindPreviousInterface();

        createLCBMachineModule();

        createMachineFrame();

        setLCBControlType();

    }

    public void reInitLcb() {
        mGeneralHandler.eStopRevertLcbInitial();
    }

    private void createLCBMachineModule() {

        switch (machineType) {
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case RecumbentElliptical:
                this.mBike = new BikeInterface(serviceCallback);
                break;
            case JhtADboardRBike:
            case JhtADboardUBike:
            case JhtADboardElliptical:
                this.mJhtADboard = new JhtADboardInterface(serviceCallback);
                break;
            case CardioSpinBike:
            case GroupSpinBike:
                this.mSpinBike = new SpinBikeInterface(serviceCallback);
                break;
            case ManualResistanceSpinBike:
                this.mTcvFxSpinBike = new TcvFxSpinBikeInterface(serviceCallback);
                break;
            case Stepper:
                this.mStepper = new StepperInterface(serviceCallback);
                break;
            case Ascent:
            case LowerBodyAscent:
                this.mAscent = new AscentInterface(serviceCallback);
                break;
            case SDrive:
                break;
            case Climbmill:
                this.mClimbmill = new EnClimbmillInterface(serviceCallback);
                break;
            case Treadmill:
                this.mTreadmill = new TreadMillInterface(serviceCallback);
                break;
            case Elliptical:
                this.mElliptical = new EllipticalInterface(serviceCallback);
                break;
            default:
                this.mBike = new BikeInterface(serviceCallback);
                break;

        }

    }

    private void createMachineFrame() {

        switch (machineType) {
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case RecumbentElliptical:
                break;
            case JhtADboardRBike:
            case JhtADboardUBike:
            case JhtADboardElliptical:
                break;
            case CardioSpinBike:
            case GroupSpinBike:
            case ManualResistanceSpinBike:
                break;
            case Stepper:
                break;
            case Ascent:
            case LowerBodyAscent:
                break;
            case SDrive:
                break;
            case Climbmill:
                break;
            case Treadmill:
                if (mFrameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
                    createTM548EcbControl();
                }
                break;
            case Elliptical:
                break;
            default:
                break;

        }

    }

    private void setLCBControlType() {
        switch (machineType) {
            case Elliptical:    //EP 0x6C cmd set 0x00: bike
            case RecumbentElliptical:
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case JhtADboardRBike:
            case JhtADboardUBike:
            case JhtADboardElliptical:
                mGeneralHandler.setControlType(ControlType.CONTROL_BIKE.getByteValue() );
                break;
            case CardioSpinBike:
            case GroupSpinBike:
            case ManualResistanceSpinBike:
                mGeneralHandler.setControlType(ControlType.CONTROL_INDOORBIKE.getByteValue() );
                break;
            case Stepper:
                mGeneralHandler.setControlType(ControlType.CONTROL_STEPPER.getByteValue() );
                break;
            case Ascent:
            case LowerBodyAscent:
                mGeneralHandler.setControlType(ControlType.CONTROL_RAMP_EP.getByteValue() );
                break;
            case Climbmill:
                if(isLifestyle) {
                    mGeneralHandler.setControlType(ControlType.CONTROL_RETAIL_CLIMBMILL.getByteValue() );
                }
                else {
                    mGeneralHandler.setControlType(ControlType.CONTROL_CLIMBMILL.getByteValue());
                }
                break;
            case Treadmill:
                break;
            default:
                break;

        }
    }

    public void unbindPreviousInterface() {
        if (mTreadmill != null) {
            mTreadmill.unInitial();
            mTreadmill = null;
        }

        if (mBike != null) {
            mBike.unInitial();
            mBike = null;
        }

        if (mSpinBike != null) {
            mSpinBike.unInitial();
            mSpinBike = null;
        }

        if (mTcvFxSpinBike != null) {
            mTcvFxSpinBike.unInitial();
            mTcvFxSpinBike = null;
        }

        if (mAscent != null) {
            mAscent.unInitial();
            mAscent = null;
        }

        if (mElliptical != null) {
            mElliptical.unInitial();
            mElliptical = null;
        }

        if (mStepper != null) {
            mStepper.unInitial();
            mStepper = null;
        }

        if (mClimbmill != null) {
            mClimbmill.unInitial();
            mClimbmill = null;
        }

        if (mJhtADboard != null) {
            mJhtADboard.unInitial();
            mJhtADboard = null;
        }
    }

    public void startReplyData(boolean start) {

        switch (machineType) {
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case RecumbentElliptical:
	            if (mBike != null)
	                mBike.startReplyData(start);
                break;
            case CardioSpinBike:
            case GroupSpinBike:
                if (mSpinBike != null)
                    mSpinBike.startReplyData(start);
                break;
            case ManualResistanceSpinBike:
                if (mTcvFxSpinBike != null)
                    mTcvFxSpinBike.startReplyData(start);
                break;
            case JhtADboardRBike:
            case JhtADboardUBike:
            case JhtADboardElliptical:
	            if (mJhtADboard != null)
	                mJhtADboard.startReplyData(start);
            case Stepper:
                if (mStepper != null)
                    mStepper.startReplyData(start);
                break;
            case Ascent:
            case LowerBodyAscent:
                if (mAscent != null)
                    mAscent.startReplyData(start);
                break;
            case SDrive:
                break;
            case Climbmill:
                if (mClimbmill != null)
                    mClimbmill.startReplyData(start);
                break;
            case Treadmill:
                if (mTreadmill != null)
                    mTreadmill.startReplyData(start);
                break;
            case Elliptical:
                if (mElliptical != null)
                    mElliptical.startReplyData(start);
                break;
            default:
                break;

        }

    }

    /*   Treadmill Control Function   */
    public void setTreadmillRPM(int rpm) {
        if (mTreadmill != null) {
                mTreadmill.setTreadMillRPM(rpm);
        }
    }

    /*public void setTreadmillRampRPM(int rpm) {
        if (mRpmRamp != null) mRpmRamp.setTargetRpm(rpm);
    }*/

    public void setTreadmillInclineDirection(boolean reverse) {
        if (mTreadmill != null) {
            mTreadmill.setTreadmillInclineReverse(reverse);
        }
    }

    public void setTreadMillIncline(int adc) {
        if (mTreadmill != null) {
            mTreadmill.setTreadMillIncline(adc);
        }
    }

    public void setTreadmillOperate(int treadmillRpm, int inclineLoc) {
        if (mTreadmill != null) {
            mTreadmill.setTreadmillOperate(treadmillRpm, inclineLoc);
        }
    }

    public void setTmInclineActionDown() {
        if (mTreadmill != null)
            mTreadmill.setTreadMillInclineAction((byte) 0xFF);
    }


    public void setTmInclineActionUp() {
        if (mTreadmill != null)
            mTreadmill.setTreadMillInclineAction((byte) 1);
    }

    public void setTmInclineActionStop() {
        mRealLcb.setLcbInclineActionHighestPriority((byte) 0); // stop incline may be highest priority.
    }

    public void startTreadmillMagnentAngleTuning(boolean start) {
        Log.d(TAG, "startTreadmillMagnentAngleTuning: start "+start);

        if(start)
            mGeneralHandler.startMagnetAngleCalibration();

        mGeneralHandler.setAutoReplyGeneralMagnetAngle(start);
    }

    public void getTreadmillMagnentAngle() {
            mGeneralHandler.getMagnetAngle();
    }

    public void getTreadmillMcbVoltage() {
        if (mGeneralHandler != null)
            mGeneralHandler.getMcbVoltage();
    }

    public void switchPFCfunction(boolean enable) {
        mGeneralHandler.setPFCmode(enable);
    }

    public void onTmSafetykeyUnlock() {
        mGeneralHandler.eStopRevertLcbInitial();
    }

    public void setTreadMillNoPersonDetect(boolean enable) {
        if (mTreadmill != null)
            mTreadmill.setTreadMillNoPersonDetect(enable);
    }

    public void setTreadMillMotorType(int motorType) {
        if (mTreadmill != null)
            mTreadmill.setTreadMillMotorType(motorType);
    }
    public void setTreadmillSafetyLock(boolean lock) {
        if (mTreadmill != null)
            mTreadmill.setTreadmillSafetyLock(lock);
    }

    public void getTreadMillMotorType() {
        if (mGeneralHandler != null)
            mGeneralHandler.getMotoType();
    }

    public void setTreadMillCurrentOcDetect(boolean enable) {
        if (mTreadmill != null)
            mTreadmill.setTreadMillCurrentOcDetect(enable);
    }

    //Calibration auto update incline data
    public void setTreadmillAutoReplyIncline(boolean enable) {
        if (mTreadmill != null)
            mTreadmill.setAutoReplyTmIncline(enable);
    }

    public void setTreadmillExtStatusOn(boolean enable) {
        if (mTreadmill != null)
            mTreadmill.setExtStatusOn(enable);
    }

    public void setTreadmillStopMachine() {
        if (mTreadmill != null)
            mTreadmill.setTreadmillStopMachine();
    }

    public void setTreadmillDetectMachine(boolean on) {
        if (mTreadmill != null)
            mTreadmill.setTreadmillDetectMachine(on);
    }


    public void createTM548EcbControl() {
        if (mTM548ECBControl == null) {
            mTM548ECBControl = new TM548ECBControl();
        }	
    }

    public byte setTM548EcbPowerOnLock() {
	    byte byteRtnECBPowerOnLock = (byte)DEFAULT_TM548_STATUS_BYTE;
        if (mTM548ECBControl != null) {
		byteRtnECBPowerOnLock = mTM548ECBControl.setTM548EcbPowerOnLock();
        }	

	    return byteRtnECBPowerOnLock;
    }

    public byte setTM548EcbPowerOff() {
        byte byteRtnECBPowerOff = (byte)DEFAULT_TM548_STATUS_BYTE;
        if (mTM548ECBControl != null) {
		byteRtnECBPowerOff = mTM548ECBControl.setTM548EcbPowerOff();
        }	


	    return byteRtnECBPowerOff;
    }

    public byte setTM548EcbEstopResistance() {
        byte byteRtnECBEstopResistance = (byte)DEFAULT_TM548_STATUS_BYTE;
        if (mTM548ECBControl != null) {
            byteRtnECBEstopResistance = mTM548ECBControl.setTM548EcbEstopResistance();
        }


        return byteRtnECBEstopResistance;
    }

    public byte setTM548EcbEstopMotorOffBrake() {
	    byte byteRtnECBEstopMotorOffBrake = (byte)DEFAULT_TM548_STATUS_BYTE;
        if (mTM548ECBControl != null) {
		    byteRtnECBEstopMotorOffBrake = mTM548ECBControl.setTM548EcbEstopMotorOffBrake();
        }	


	    return byteRtnECBEstopMotorOffBrake;
    }

    public byte setTM548EcbEstopMotor2StageBrake() {
	    byte byteRtnECBEstopMotor2StageBrake = (byte)DEFAULT_TM548_STATUS_BYTE;
        if (mTM548ECBControl != null) {
		byteRtnECBEstopMotor2StageBrake = mTM548ECBControl.setTM548EcbEstopMotor2StageBrake();
        }	


	    return byteRtnECBEstopMotor2StageBrake;
    }

    public byte setTM548EcbEstopRelease() {
		byte byteRtnECBEstopRelease = (byte)DEFAULT_TM548_STATUS_BYTE;
        if (mTM548ECBControl != null) {
		byteRtnECBEstopRelease = mTM548ECBControl.setTM548EcbEstopRelease();
        }	


    	return byteRtnECBEstopRelease;
    }


    public byte setTM548EcbWorkoutRelease() {
		byte byteRtnECBWorkoutRelease = (byte)DEFAULT_TM548_STATUS_BYTE;
        if (mTM548ECBControl != null) {
		byteRtnECBWorkoutRelease = mTM548ECBControl.setTM548EcbWorkoutRelease();
        }	


	return byteRtnECBWorkoutRelease;
    }

    public byte setTM548EcbWorkoutBrake() {
		byte byteRtnECBWorkoutBrake = (byte)DEFAULT_TM548_STATUS_BYTE;
        if (mTM548ECBControl != null) {
		byteRtnECBWorkoutBrake = mTM548ECBControl.setTM548EcbWorkoutBrake();
        }	


	return byteRtnECBWorkoutBrake;
    }

    public void setTM548EcbStatusAction(byte actionByte) {
        if (mTreadmill != null){
            mTreadmill.setTreadMillEcbAction((byte) actionByte);
        }
    }

    public void getTM548PowerOnInclineAdc() {
        if (mTreadmill != null){
            mTreadmill.getTreadMillOneShotReplyInclineAdc();
        }
    }

	
    public void setTreadmillAutoRampUpAndLastRpmValue(boolean on,int rampUpStage,int rampDownStage) {
        if (mTreadmill != null) {

            mTreadmill.setTreadmillAutoRampUpAndLastRpmValue(on,rampUpStage,rampDownStage);
        }
    }

    public void setTreadmillOverSpeed(boolean bOverSpeed) {
        if (mTreadmill != null) {
                mTreadmill.initTreadmillOverSpeed(bOverSpeed);
        }
    }

    public void getHybridTMIrAddrVersion() {
        if (mTreadmill != null){
            mTreadmill.getHybridTMIrAddrVersion();
        }
    }

    public void setHybridTMIrAddrIrDetection(boolean bEnable) {
        if (mTreadmill != null){
            mTreadmill.setHybridTMIrAddrIrDetection(bEnable);
        }
    }

    public void setHybridTMIrAddrEnterErpMode() {
        if (mTreadmill != null){
            mTreadmill.setHybridTMIrAddrEnterErpMode();
        }
    }

    public void setHybridTMIrAddrLeaveErpMode() {
        if (mTreadmill != null){
            mTreadmill.setHybridTMIrAddrLeaveErpMode();
        }
    }

    /* Ascent Control Function*/
    public void autoAscentCalibrateIncline() {
        if (mAscent != null)
            mAscent.autoAscentCalibrateIncline();
    }

    public void stopAscentCalibrate() {
        if (mAscent != null)
            mAscent.stopAscentCalibrate();
    }

    public void stopinclineAction() {
        if (mTreadmill != null)
        mRealLcb.setLcbInclineActionHighestPriority((byte) 0); // stop incline may be highest priority.		
        else if (mAscent != null)
            mAscent.stopAscentInclineAction();
    }

    public void setAscentIncline(int incline) {
        if (mAscent != null)
            mAscent.setAscentIncline(incline);
    }

    public void setAscentResistance(int resistance) {
        if (mAscent != null)
            mAscent.setAscentResistance(resistance);
    }

    public void setLowLimitResistanceAscent(int resistance) {
        if (mAscent != null)
            mAscent.setAscentLowLimitResistance(resistance);
    }

    public void setAscentLowLimitCharge(int charge) {
        if (mAscent != null)
            mAscent.setAscentLowLimitCharge(charge);
    }


    public void setAscentTuneEndIncline(int incline) {
        if (mAscent != null)
            mAscent.setAscentTuneEndIncline(incline);
    }


    public void setAscentConsolePower(int power) {
        if (mAscent != null)
            mAscent.setAscentConsolePower(power);
    }


    public void setGearRatioStepper(int resistance) {
        if (mStepper != null)
            mStepper.setStepperGearRatio(resistance);
    }

    public void setErgWatts(int targetWatts) {
        if (mSpinBike != null)
        {
            Log.d (TAG, "target watts=" + targetWatts);
            mSpinBike.setErgWattsSpinBike(targetWatts);
        }
    }

    /* Bike Control Function */
    public void setResistanceBike(int resistance) {
        if (mBike != null)
            mBike.setBikeWattOrResistance(resistance);
    }

    public void setResistanceLowLimit(int resistance) {
        if (mBike != null)
            mBike.setBikeLowLimitResistance(resistance);
    }

    public void setBikeLowLimitCharge(int charge) {
        if (mBike != null)
            mBike.setBikeLowLimitCharge(charge);
    }


    public void setClimbmillRPM(int rpm) {
    }

    public void setAutoReplyClimbmillCZcapaIRfreq(boolean enable) {
        if (mClimbmill != null)
            mClimbmill.setAutoReplyEnClimbCZcapaIRfreq(enable);
    }

    public void setClimbmillCommand(byte data) {
        if (mClimbmill != null)
            mClimbmill.setEnClimbClimbmillCommand(data);
    }

    public void setClimbmillNoPersonOnOff(boolean on_off) {
        if (mClimbmill != null)
            mClimbmill.setEnClimbmillNoPersonOnOff(on_off);
    }

    public void setmClimbmillControlZoneOn(boolean on) {
        if (mClimbmill != null)
            mClimbmill.setEnClimbmillCzOn(on);
    }

    /* Climbmill Calibration Function */
    public void setClimbmillCalibration(byte[] bytes) {
        if (mClimbmill != null)
            mClimbmill.setEnClimbClimbmillCal(bytes);
    }

    public void getClimbmillCalScaling() {

        if (mClimbmill != null)
            mClimbmill.getEnClimbClimbmillCalScaling();
    }

    public void setClimbmillCalScaling(int intValue) {

        if (mClimbmill != null)
            mClimbmill.setEnClimbClimbmillCalScaling(intValue);
    }

    public void getEnClimbClimbmillFrameCalScaling() {

        if (mClimbmill != null){
            mClimbmill.getEnClimbClimbmillFrameCalScaling();
        }
    }
    
    public void setClimbmillLCBFanOn() {

        if(mClimbmill != null)
            mClimbmill.setEnClimbmillLCBFanOn();
    }

    public void setClimbmillLock(boolean lock) {
        if(mClimbmill != null)
            mClimbmill.setEnClimbLock(lock);
    }

    public void setLCBControlTypeCmd(byte typeCmd) {
        mGeneralHandler.setControlType(typeCmd);
    }

    public void setGearRatio(int gearRatio) {
        switch (machineType) {
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case RecumbentElliptical:
                if (mBike != null)
                    mBike.setBikeGearRatio(gearRatio);
                break;
            case CardioSpinBike:
            case GroupSpinBike:
            case ManualResistanceSpinBike:
                break;
            case Stepper:
                break;
            case Ascent:
            case LowerBodyAscent:
                if (mAscent != null)
                    mAscent.setAscentGearRatio(gearRatio);
                break;
            case SDrive:
                break;
            case Climbmill:
                if (mClimbmill != null)
                    mClimbmill.setEnClimbGearRatio(gearRatio);
                break;
            case Treadmill:
                break;
            case Elliptical:
                if (mElliptical != null)
                    mElliptical.setEllipticalGearRatio(gearRatio);
                break;
            case JhtADboardRBike:
            case JhtADboardUBike:
            case JhtADboardElliptical:
                if (mJhtADboard != null)
                    mJhtADboard.setJhtADboardGearRatio(gearRatio);
                break;
            default:
                break;

        }
    }

    public void setResistanceType(int resistanceType) {
        switch (machineType) {
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case RecumbentElliptical:
                if (mBike != null)
                    mBike.setBikeResistanceType(resistanceType);
                break;
            case CardioSpinBike:
            case GroupSpinBike:
            case ManualResistanceSpinBike:
                break;
            case Stepper:
                if (mStepper != null)
                    mStepper.setStepperResistanceType(resistanceType);
                break;
            case Ascent:
            case LowerBodyAscent:
                if (mAscent != null)
                    mAscent.setAscentResistanceType(resistanceType);
                break;
            case SDrive:
                break;
            case Climbmill:
                if (mClimbmill != null)
                    mClimbmill.setEnClimbResistanceType(resistanceType);
                break;
            case Treadmill:
                break;
            case Elliptical:
                if (mElliptical != null)
                    mElliptical.setEllipticalResistanceType(resistanceType);
                break;
            default:
                break;

        }
    }


    public void turnOffLcb(int minutes) {
        switch (machineType) {
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case RecumbentElliptical:
                if (mBike != null)
                    mBike.setBikeTurnOffLcb(minutes);
                break;
            case CardioSpinBike:
            case GroupSpinBike:
            case ManualResistanceSpinBike:
                break;
            case Stepper:
                if (mStepper != null)
                    mStepper.setStepperTurnOffLcb(minutes);
                break;
            case Ascent:
            case LowerBodyAscent:
                if (mAscent != null)
                    mAscent.setAscentTurnOffLcb(minutes);
                break;
            case SDrive:
                break;
            case Climbmill:
                break;
            case Treadmill:
                break;
            case Elliptical:
                if (mElliptical != null)
                    mElliptical.setEllipticalTurnOffLcb(minutes);
                break;
            default:
                break;

        }
    }


    public void setLowLimitResistance(int lowLimit) {
        switch (machineType) {
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case RecumbentElliptical:
                if (mBike != null)
                    mBike.setBikeLowLimitResistance(lowLimit);
                break;
            case CardioSpinBike:
            case GroupSpinBike:
            case ManualResistanceSpinBike:
                break;
            case Stepper:
            case Ascent:
            case LowerBodyAscent:
                if (mAscent != null)
                    mAscent.setAscentLowLimitResistance(lowLimit);
                break;
            case SDrive:
                break;
            case Climbmill:
                break;
            case Treadmill:
                break;
            case Elliptical:
                if (mElliptical != null)
                    mElliptical.setEllipticalLowLimitResistance(lowLimit);
                break;
            default:
                break;

        }
    }


    public void setLowLimitCharge(int charge) {
        switch (machineType) {
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case RecumbentElliptical:
                if (mBike != null)
                    mBike.setBikeLowLimitCharge(charge);
                break;
            case CardioSpinBike:
            case GroupSpinBike:
            case ManualResistanceSpinBike:
                break;
            case Stepper:
            case Ascent:
            case LowerBodyAscent:
                if (mAscent != null)
                    mAscent.setAscentLowLimitCharge(charge);
                break;
            case SDrive:
                break;
            case Climbmill:
                break;
            case Treadmill:
                break;
            case Elliptical:
                if (mElliptical != null)
                    mElliptical.setEllipticalLowLimitCharge(charge);
                break;
            default:
                break;

        }
    }

    public void tuneInclineEnd1(byte maxOffset, byte minOffset) {
        mAscent.setAscentTuneEndIncline(DataUtils.unsignedBytesToInt(minOffset, maxOffset));
    }

    public void sendClimbmillCommand(byte command) {
        if (mClimbmill != null)
            mClimbmill.setEnClimbClimbmillCommand(command);
    }

    public void setGeneratorPole(byte pole) {
        int pole_value = (int) pole & 0xff;

        switch (machineType) {
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case RecumbentElliptical:
                break;
            case CardioSpinBike:
            case GroupSpinBike:
            case ManualResistanceSpinBike:
                break;
            case Stepper:
                break;
            case Ascent:
            case LowerBodyAscent:
                if (mAscent != null)
                    mAscent.setAscentGeneratorPole(pole_value);
                break;
            case SDrive:
                break;
            case Climbmill:
                if (mClimbmill != null)
                    mClimbmill.setEnClimbGeneratorPole(pole_value);
                break;
            case Treadmill:
                break;
            case Elliptical:
                break;
            default:
                break;

        }

    }

    /*******************************************************************************
     * Interface Name  : setLcbInitBeforeWorkout
     * Description    : Initialize LCB and MCB before starting the workout.
     *******************************************************************************/
    public void setLcbInitBeforeWorkout() {
        if (mGeneralHandler != null) {
            mGeneralHandler.initLcbMcbBeforeWorkout();
        }
    }

    /*
     *   TM force stop
     * */
    public void eStopMachineStop() {
        mGeneralHandler.eStopMachineStop();
    }

    /*
     *    Force stop for  Ascent & ENClimbmill
     *    1.Ascent calibration
     *    2.EnClimbmill calibration
     * */
    public void forceStopMachine() {
        mGeneralHandler.forceStopMachine();
    }


    //for General
    public void stopMachine() {

        if (machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill && mTreadmill != null) {
            setTreadmillStopMachine();
            setTreadmillDetectMachine(false);
        } else if (machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill && mClimbmill != null) {
            setSPM(0); // not needed for frames but this helps with simulators that don't zero this after receiving the lock
            setClimbmillLock(true);
        } else if (machineClassType == JHTCommonTypes.MachineClassificationType.Bike && mBike != null) {
            setResistanceBike(0);
        } else if (machineClassType == JHTCommonTypes.MachineClassificationType.Ascent && mAscent !=null) {
            setAscentResistance(0);
        }
    }

    public void unlockMachine() {
        if (machineType == JHTCommonTypes.MachineType.Treadmill) {
            setTreadmillDetectMachine(true);
        }
        if (machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
            setClimbmillLock(false);
        }
    }


    //Set moto RPM 0xF1
    public void setRpm(int rpm) {
        if (machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
            if (mTreadmill != null)
                mTreadmill.setTreadMillRPM(rpm);
        }
    }


    public void stopGeneralHandler() {
        if (mGeneralHandler != null) {
            mGeneralHandler.autoStopGeneralHandlerLCB();//stop lcb communiction ,machine general callback & heart beat
        }
    }

    public void startLcbIntoUpdateMode() {
        if (mGeneralHandler != null) {
            mGeneralHandler.setLcbIntoUpdateProgramMode();
        }
    }

    public void setILcbSerialNumber(String stringLcbSN) {
        if (mGeneralHandler != null) {
            mGeneralHandler.setLcbSerialNumber(stringLcbSN);
        }
    }

    public void getILcbSerialNumber() {
        if (mGeneralHandler != null) {
            mGeneralHandler.getLcbSerialNumber();
        }
    }

    /*
    *   Indoor Cycle Function
    *
    */
    public void setResistanceSpinBike (int resistance) {
        if (mSpinBike != null)
        {
            //Log.d (TAG, " set resistance=" + resistance);
            mSpinBike.setResistanceSpinBike(resistance);
        }
    }

    public void setAutoReplySpinBikeInfoSpinDown(boolean enable) {
        if (mSpinBike != null)
            mSpinBike.setAutoReplySpinBikeInfoSpinDown(enable);
    }

    public void setSpinBikeSpinDownOnOff(boolean on_off) {
        if (mSpinBike != null)
            mSpinBike.setSpinBikeSpinDownOn(on_off);
    }

    public void requestRawAdc(int index) {
        if (machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike) {
            if (mSpinBike != null) mSpinBike.requestRawAdc(index);
        }
    }
	
    public void setSpinBikeClearTorque() {
        if (mSpinBike != null)
            mSpinBike.setSpinBikeClearTorque();
    }

    public void setSpinBikeCorrectionFactor(float factor) {
        if (mSpinBike != null)
            mSpinBike.setSpinBikeCorrectionFactor(factor);
    }

    public void setSpinBikeInertia(float inertia) {
        if (mSpinBike != null)
            mSpinBike.setSpinBikeInertia(inertia);
    }

    public void getSpinBikeRaptorPara() {
        if (mSpinBike != null)
            mSpinBike.getSpinBikeRaptorPara();
    }

    public void setSpinBikeRaptorPara(String strRaptorPara) {
        if (mSpinBike != null)
            mSpinBike.setSpinBikeRaptorPara(DataUtils.hexStringToByteArray(strRaptorPara));
    }

    public void erpStopUartPort() {
        if (mGeneralHandler != null) {
            mGeneralHandler.erpStopUartPort();//stop lcb communiction uart port
        }
    }

    public void setErp(boolean enterErp) {
        if (mGeneralHandler != null) {
            if (enterErp) {
                // stop adding LCB commands
                if (mSpinBike != null) mSpinBike.stopAutoHeartBeatThread();
                if (mTcvFxSpinBike != null) mTcvFxSpinBike.stopAutoHeartBeatThread();
                // issue LCB sleep command
                mGeneralHandler.enterLcbErpMode();
            } else {
                mGeneralHandler.leaveLcbErpMode();
            }
        }
    }

    /* set ClimbMill SPM */
    public void setSPM(double spm) {
        if (machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
	    if (mClimbmill != null)
	        mClimbmill.setEnClimbSPM((int) spm * 10);
        }
    }

    /* set Raw Incline */
    public void setRawIncline(int incline) {
        if (machineClassType == JHTCommonTypes.MachineClassificationType.Ascent) {
            //incline (%) = 0 ~ 32768
	    if (mAscent != null)
	        mAscent.setAscentIncline(incline);
        } else {
	    if (mTreadmill != null)
	        mTreadmill.setTreadMillIncline(incline);
        }
    }


    /* set Raw Resistance */
    public void setRawResistance(int resistance) {
        switch (machineType) {
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case RecumbentElliptical:
	        if (mBike != null)
	            mBike.setBikeWattOrResistance(resistance);
                break;
            case CardioSpinBike:
            case GroupSpinBike:
            //Log.i(TAG, "SpinBike setRawResistance: "+resistance);
	        if (mSpinBike != null)
	            mSpinBike.setResistanceSpinBike(resistance);
                break;
            case Stepper:
	        if (mStepper != null)
	            mStepper.setStepperResistance(resistance);
                break;
            case Ascent:
            case LowerBodyAscent:
	        if (mAscent != null)
	            mAscent.setAscentResistance(resistance);
                break;
            case Climbmill:
	        if (mClimbmill != null)
	            mClimbmill.setEnClimbResistance(resistance);
                break;
            case Elliptical:
	        if (mElliptical != null)
	            mElliptical.setEllipticalResistance(resistance);
                break;
            case JhtADboardRBike:
            case JhtADboardUBike:
            case JhtADboardElliptical:
	        if (mJhtADboard != null)
	            mJhtADboard.setJhtADboardWattOrResistance(resistance);
                break;
            default:
                break;

        }
		
    }

    public boolean isOverSpeed(float currentKph) {
        if (mRpmRamp != null) {
            if (mRpmRamp.getTargetRpm() > 0) {
                float rampTargetKph = mRpmRamp.getCurrentKph();
                return (currentKph - rampTargetKph >= SpeedConvertUtil.convertMphToKph(3.5f)) && rampTargetKph > 0;
            }
        }
        return false;
    }

    public boolean isTmRollerGuardTriggered() {
        if (mTreadmill != null) return mTreadmill.isRollerGuardTriggered();
        return false;
    }

    private boolean isHybridTmIrEnabled() {
        return mFrameConfig != null && mFrameConfig.HAS_IR_SENSOR && mTreadmill != null;
    }

    public boolean isHybridTmIrEStopTriggered() {
        if (mTreadmill != null) return mTreadmill.isHybridTmIrEStopTriggered();
        return false;
    }

    // some retail treadmill LCBs seem to ignore big jumps in RPM, perhaps as a safety feature
    // Black Widow IO firmware ramps RPM at 28 per step at 200ms per step
    private class RpmRamper extends Thread {
        RpmRamper() { super("RpmRamper"); }
        static final int RPM_STEP = 28;
        private int currentRpm = -1; // don't really know what current rpm is, so force a set at the start
        private volatile int targetRpm = 0;

        public void setTargetRpm(int rpm) { targetRpm = rpm; }
        public int getTargetRpm() { return targetRpm; }
        public float getCurrentKph() { return currentRpm < 0 ? 0 : Machine.rpmToKph(currentRpm); }

        @Override
        public void run() {
            super.run();
            try {
                while (!isInterrupted()) {
                    if (currentRpm != targetRpm) {
                        // stop
                        if (targetRpm == 0) {
                            currentRpm = targetRpm; // ramp doesn't seem required for going down? UNLESS you need overspeed check?
                            if (currentRpm < 0) currentRpm = 0; // ensure we do not send a negative value
                            if (mTreadmill != null) mTreadmill.setTreadMillRPM(currentRpm, true); // overwrite any pending
                        }
                        // slowing down
                        else if (currentRpm > targetRpm) {
                            //currentRpm -= Machine.kphToRpm(2.5f); // 3.5mph = 5.6327kph
                            currentRpm -= RPM_STEP;
                            if (currentRpm < targetRpm) currentRpm = targetRpm; // under shoot
                            if (currentRpm < 0) currentRpm = 0; // ensure we do not send a negative value
                            if (mTreadmill != null) mTreadmill.setTreadMillRPM(currentRpm, true); // overwrite any pending
                        }
                        // speeding up
                        else if (currentRpm < targetRpm) {
                            currentRpm += RPM_STEP;
                            if (currentRpm > targetRpm) currentRpm = targetRpm; // over shoot
                            if (currentRpm < 0) currentRpm = 0; // ensure we do not send a negative value
                            if (mTreadmill != null) mTreadmill.setTreadMillRPM(currentRpm, false); // let these queue up
                        }
                    }
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                Log.e(TAG, "Exception in TreadMill RPM ramp thread! ", ex);
                // restart it ?
            }
        }
    } // END CLASS: RPM RAMPER
    private void startRpmRamper() {
        //stopRpmRamper();
        if (mRpmRamp == null) { // only one at at time
            mRpmRamp = new RpmRamper();
            mRpmRamp.start();
        }
    }
    private void stopRpmRamper() {
        KillProcessHelper.stopThread(mRpmRamp);
        mRpmRamp = null;
    }
    private static boolean needsRpmRamper(int lcb) {
        // Should we just check if current flavor is retail? or just check for retail treadmill LCBs?
        //return HardwareConfig.isRetail();
        return (BuildConfig.CHECK_TM_OVERSPEED); // we need ramp down for overspeed check
        /*switch (lcb) {
            case 0x06: case 0x0E:
                return true;
        }
        return false;*/
    }

}
