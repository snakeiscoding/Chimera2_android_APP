package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.lcbupdateInterface;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IUniDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.UniDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.AscentInterface;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.BikeInterface;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.EllipticalInterface;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.EnClimbmillInterface;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.StepperInterface;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.TreadMillInterface;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IGeneralHandlerCallback;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.ILcbUpdateProcessReceive;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.updateHandler.IUpdateLcbHandlerCallback;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.updateHandler.UpdateLcbHandler;
import com.jht.serialport.SerialPortConfiguration;

public class LCBUpdateInterface {

    private ILCBUpdateCallback serviceCallback; // Used to provide library notifications.

    private static final String TAG = "LCBInterface";
    private ILcbComm mRealLcb;
    private IDeviceDataTransmitted mDeviceDataTransmitted;
    private final String LCB_DEVICE_PATH = ProductConfig.portMappings.lcbPath();
    private final SerialPortConfiguration.BaudRate LCB_BAUD_RATE = SerialPortConfiguration.BaudRate.B9600;
    private UpdateLcbHandler mUpdateLcbHandler;
    private final String LCB_UPDATE_DEVICE_PATH = ProductConfig.portMappings.lcbPath();
    private final SerialPortConfiguration.BaudRate LCB_UPDATE_BAUD_RATE = SerialPortConfiguration.BaudRate.B115200;

    private GeneralHandler mUpdateLcbGeneralHandler;
    private ILcbComm mUpdateLcbComm;
    private IDeviceDataTransmitted mUpdateLcbDeviceDataTransmitted;
    private IUniDataTransmitted mUpdateLcbUniDataTransmitted;

    private static JHTCommonTypes.MachineClassificationType control_tpye;
    private byte oldLCBStatus = 0x00;
    public TreadMillInterface mTreadmill = null;
    public BikeInterface mBike = null;
    public StepperInterface mStepper = null;
    public EnClimbmillInterface mEnClimbmill = null;
    public EllipticalInterface mElliptical = null;
    public AscentInterface mAscent = null;

    private IGeneralHandlerCallback mGeneralHandlerCallback = new IGeneralHandlerCallback() {
        @Override
        public void onErrorCode(int lcbErrorCode) {
            //Log.d(TAG, String.format("LCBUpdateInterface LcbHandleCallback onErrorCode lcbErrorCode=%X, ", lcbErrorCode));
        }

        @Override
        public void onLcbType(byte lcbType) {
        }

        @Override
        public void onLcbVersion(int major, int minor) {
        }

        @Override
        public void onLcbInitialFinish() {
        }

        @Override
        public void onInclineMotorDownwardStatus(boolean motorDownwardStatus) {
        }

        @Override
        public void onInclineMotorUpwardStatus(boolean motorUpwardStatus) {
        }

        @Override
        public void onMainMotorStatus(boolean motorStatus) {
        }

        @Override
        public void onEncoderRPMStatus(boolean encoderRPMStatus) {
        }

        @Override
        public void onBatteryCapacityStatus(boolean batteryCapacityStatus) {
        }

        @Override
        public void onCommandErrorStatus(boolean commandErrorStatus) {
        }

        @Override
        public void onInitialCalibrationStatus(boolean initialCalibrationStatus) {
        }

        @Override
        public void onBatteryVoltage(int intBatteryVoltage) {

        }

        @Override
        public void onLcbStatus(byte lcbstatus) {

        }

        @Override
        public void onClearLcbSnStatus(boolean clearStatus) {

        }

        @Override
        public void onSetLcbSnStatus(boolean setStatus) {

        }

        @Override
        public void onGetLcbSerialNumber(boolean getStatus, String getString) {

        }

        @Override
        public void onGetLcbSnMaxLength(boolean getStatus, int intMaxLength) {

        }

        @Override
        public void onIntoLcbUpdateMode(boolean intoSuccess) {
            if (intoSuccess) {
                Log.e(TAG, "Update Lcb into lcb update mode success.");
                //stop & unregister update mode general heart beat and lcb communication
                stopGeneralHandler();

            } else {
                Log.e(TAG, "Update Lcb into lcb update mode error !!!");
            }
        }

        @Override
        public void onLcbFinishMagnetAngleValue(int intMagnetAngle) {

        }

        @Override
        public void onMcbMotoType(int intMotoType) {

        }

        @Override
        public void onGetMcbVoltageValue(int intVoltage) {

        }
    };

    private IUpdateLcbHandlerCallback mUpdateLcbHandlerCallback = new IUpdateLcbHandlerCallback() {

    };

    private ILcbUpdateProcessReceive mLcbUpdateProcessListener = new ILcbUpdateProcessReceive() {
        @Override
        public void rtnUpdateMessageHandler(String strMessage) {
        }

        @Override
        public void rtnUpdateLcbInfoErrorHandler(String strErrorString) {
        }

        @Override
        public void rtnFlowSendCmdErrorHandler(String strSendErrorString) {
        }

        @Override
        public void rtnFlowReceiveCmdErrorHandler(String strReceiveErrorString) {
        }

        @Override
        public void returnUpdateFlowFailHandler(String strFailString) {
        }

        @Override
        public void onUpdateEraseFlashDataPercent(float percentComplete) {
            broadcastUpdateStatus(101, percentComplete * 100);
        }

        @Override
        public void onUpdateWriteFlashDataPercent(float percentComplete) {
            broadcastUpdateStatus(102, percentComplete * 100);
        }

        @Override
        public void onUpdateReadFlashDataPercent(float percentComplete) {
            broadcastUpdateStatus(103, percentComplete * 100);
        }

        @Override
        public void onUpdateComplete() {
            broadcastUpdateStatus(104, 0);
        }

        @Override
        public void onLcbPowerOff() {
        }
    };

    private Context context;

    public LCBUpdateInterface(final ILCBUpdateCallback callback, Context context)//, LCBMessageType.LCB_CONTROL_TPYE type)
    {
        this.serviceCallback = callback;
        this.context = context;
        mUpdateLcbGeneralHandler = GeneralHandler.getInstance();
        mRealLcb = LcbComm.getInstance();
        mDeviceDataTransmitted = DeviceDataTransmitted.getInstance();

        mUpdateLcbGeneralHandler.setGeneralHandlerCallback(mGeneralHandlerCallback);

        mUpdateLcbHandler = UpdateLcbHandler.getInstance();
        mUpdateLcbHandler.setUpdateLcbHandlerCallback(mUpdateLcbHandlerCallback);


    }

    public boolean startLcbIntoUpdateMode() {
        boolean bStartLcb = false;
        if (mUpdateLcbGeneralHandler != null) {
            mUpdateLcbGeneralHandler = GeneralHandler.getInstance();
            mUpdateLcbComm = LcbComm.getInstance();
            mUpdateLcbDeviceDataTransmitted = DeviceDataTransmitted.getInstance();
            mUpdateLcbUniDataTransmitted = UniDataTransmitted.getInstance();
            bStartLcb = mUpdateLcbGeneralHandler.autoStartGeneralHandlerLCB(mUpdateLcbComm, mUpdateLcbDeviceDataTransmitted, mUpdateLcbUniDataTransmitted, LCB_DEVICE_PATH, LCB_BAUD_RATE);
            if (bStartLcb)
                mUpdateLcbGeneralHandler.setLcbIntoUpdateProgramMode();
        }
        return bStartLcb;
    }

    public void stopGeneralHandler() {
        if (mUpdateLcbGeneralHandler != null) {
            mUpdateLcbGeneralHandler.autoStopGeneralHandlerLCB();//stop lcb communiction ,machine general callback & heart beat
        }
    }

    public void openLcbUpdatePort() {
        if (mUpdateLcbHandler != null) {
            boolean openUpdatePort = mUpdateLcbHandler.openLcbUpdatePort();
            if (!openUpdatePort) {
                Log.e(TAG, "Update Lcb Open Update Port error !!!");
            }
        }
    }

    public void startUpdateLcbProcess() {
        if (mUpdateLcbHandler != null) {
            mUpdateLcbHandler.startUpdateLcbProcess();
        }
    }

    public void startLcbUpdateFlow(LcbType mLcbType, String filePath) {
        if (mUpdateLcbHandler != null) {
            broadcastUpdateStatus(100, 0);
            mUpdateLcbHandler.startLcbUpdateFlow(mLcbType, filePath, mLcbUpdateProcessListener);
        }
    }

    public void broadcastUpdateStatus(int statusCommand, float percent) {
        Intent intent = new Intent("com.jht.updatelcb.result");
        intent.putExtra("status", statusCommand);
        intent.putExtra("percent", percent); //NON-NLS
        context.sendBroadcast(intent);
    }
}
