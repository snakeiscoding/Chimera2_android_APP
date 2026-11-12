package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler;

import android.content.Context;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.AdvancedLogging;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.Constants;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ControlType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DataUtils;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IUniDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbProcComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.MachineCategoryType;
import com.jht.serialport.SerialPortConfiguration;

import java.nio.charset.Charset;

public class GeneralHandler implements IGeneralHandler {
    private static final String TAG = "GeneralHandler";
    private static final boolean IS_NXP_DEV_BOARD = false;

    private SerialPortConfiguration.BaudRate mStartLcbBaudrate;
    private String mStartLcbPath;

    public boolean mIsLCBErrorCodeDetected = false;
    public static int lastErrorCode = 0;

    protected ILcbComm mLcb;
    protected IDeviceDataTransmitted mDeviceLcb;
    private GeneralCallbackHandlerLCB mGeneralLcbCommReceive = new GeneralCallbackHandlerLCB(this);
    protected IGeneralHandlerCallback mGeneralHandlerCallback;
    private AdvancedLogging mAdvancedLogging;
    protected IUniDataTransmitted mUniLcb;
    private LcbProcComm mLcbProcComm = LcbProcComm.getInstance();

    public boolean mLoggingMsg = false;
    public boolean mLoggingDetail = false;

    //Hanle LCB Initial ===>
    private LcbType machineLcbType = LcbType.LCB_UNKNOWN;
    private MachineCategoryType machineCategoryType = MachineCategoryType.CATEGORY_UNKNOWN;
    private ControlType machineControlType = ControlType.CONTROL_UNKNOWN;
    private volatile InitialLcbState mInitialLcbState = InitialLcbState.LCB_INIT_NONE;

    public volatile boolean mBootUpInitial = false;
    public volatile boolean mInitialCmdDone = false;
    public volatile boolean mDefaultInitialCmdDone = false;
    public volatile boolean mOperatingCmdDone = false;
    public volatile boolean mInitialFinish = false;
    public volatile boolean mSkipErrorStage = false;
    public volatile boolean mInitialHaveError = false;

    private static final int CHECK_LCB_INITIAL_TIMES = 3;
    private int mCheckLcbInitialCounter = 0;
    private static final int LCB_INITIAL_RETRIES = 1;
    private int mRetryLcbInitial = 0;

    private static final int CHECK_NON_LCB_INFO_TIMES = 3;
    private int mCheckLcbInfoCounter = 0;
    private static final int GET_LCB_INFO_RETRIES = 8;
    private int mRetryGetLcbInfo = 0;
    public  static int calPoint = 0;
    public enum InitialLcbState {
        LCB_INIT_NONE,
        LCB_INIT_CMD_INITIAL,
        LCB_INIT_DEFAULT_CMD_INITIAL,
        LCB_INIT_CMD_OPERATING_STOP,
        LCB_INIT_CMD_OPERATING_ACTION,
        LCB_INIT_TM_3_CMDS_INITIAL,
        LCB_INIT_FINISH
    }
//<=== Hanle LCB Initial

    //Hanle LCB Auto Heart Beat ===>
    private volatile boolean _isRunning = false;
    private static final Object _notifyHeartBeatSync = new Object();
    private static Thread _heartBeatThread = null;
    private static final int HEART_BEAT_TIMER_PERIOD = 1000;
    private static volatile JHTTimer mStartHeartBeatTimer;
    private static volatile JHTTimer mStartReStartHandlerLcbTimer;

    private volatile boolean _isAutoReplyCmd = false;
    private volatile boolean _isAutoInitialLcb = false;

    //get battery status used
    private volatile boolean _isAutoReplyBatteryStatus = false;
    private static final int TIMES_BATTERY_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesBattery = 0; //HEART_BEAT_TIMER_PERIOD x times

    //get Magnet Angle value  used
    private volatile boolean _isAutoReplyMagnetAngleValue = false;
    private static final int TIMES_MAGNET_ANGLE_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesMagnetAngle = 0; //HEART_BEAT_TIMER_PERIOD x times
	
//<=== Hanle LCB Auto Heart Beat 

    //Hanle LCB Error ===>
    private volatile GetLcbErrorState mGetLcbErrorState = GetLcbErrorState.LCB_ERROR_NONE;

    public volatile boolean mSafeKeyStopMachine = false;
    private volatile boolean mRunProcEStopLcb = false;

    private static final int CHECK_GET_ERROR_STATE_TIMES = 4; // with addition of EUP on ICR70 there could be 3 packets ahead of first error read
    private int mCheckGetErrorStateCounter = 0;
    private static final int CHECK_SKIP_ERROR_STATE_TIMES = 3;
    private int mCheckSkipErrorStateCounter = 0;

    private static final int CHECK_MCB_ESTOP_TIMER_OUT = 5000;
    private JHTTimer mMcbEStopErrorTimer = null;
    private volatile boolean mSafeKeySignalEStopError = false;
    private volatile boolean mMcbEStopError = false;
    private volatile boolean mEStopRevert = false;

    public enum GetLcbErrorState {
        LCB_ERROR_NONE,
        LCB_ERROR_GET,
        LCB_ERROR_SKIP
    }
//<=== Hanle LCB Error


//    private static volatile AbsoluteTimer mLcbStatusHeartBeatTimer;


    private GeneralHandler() {
    }

    private static class SingletonHolder {
        private static GeneralHandler Instance = new GeneralHandler();
    }

    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    public static GeneralHandler getInstance() {
        return SingletonHolder.Instance;
    }

    /*******************************************************************************
     * Function Name  : setGeneralHandlerCallback()
     * Description    : register IGeneralHandlerCallback
     * Input          : IGeneralHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setGeneralHandlerCallback(IGeneralHandlerCallback generalHandlerCallback) {
        mGeneralHandlerCallback = generalHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : unRegisterGeneralHandlerCallback()
     * Description    : unregister IGeneralHandlerCallback
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void unRegisterGeneralHandlerCallback() {
        mGeneralHandlerCallback = null;
    }


    /*******************************************************************************
     * Function Name  : eStopMachineStop()
     * Description    : recevice I/O e-stop trigger 0x02B2 and E-Stop LCB used priority cmd
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void eStopMachineStop() {
        mSafeKeyStopMachine = true;
        mLcb.procEStopMachineStop();
    }

    /*******************************************************************************
     * Function Name  : eStopRevertLcbInitial()
     * Description    : Revert eStop and initial Lcb
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void eStopRevertLcbInitial() {
        //marked ,because safe kay quickly turn on/off Lcb service will control not re-entry during 2-3 sec
//	if(mRunProcEStopLcb){
        Log.i(TAG, "generalHandler processes revert e-stop and re-initial LCB");
        mCheckLcbInitialCounter = 0;
        mRetryLcbInitial = 0;
        procInitialLcb();
        mEStopRevert = true;
//    	}

    }

    /*******************************************************************************
     * Function Name  : forceStopMachine()
     * Description    : force stop Machine
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void forceStopMachine() {
        procForceStopMachine();
    }

    /*******************************************************************************
     * Function Name  : setControlType()
     * Description    : set LCB Control Type
     * Input          : byte onsoleType
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setControlType(byte controlType) {
        procSetControlType(controlType);
    }

    /*******************************************************************************
     * Function Name  : getMotoType()
     * Description    : get Moto Type
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getMotoType() {
        mUniLcb.funUniGetMotoType();
    }

    /*******************************************************************************
     * Function Name  : procEStopLcb()
     * Description    : process EStop Lcb
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    protected void procEStopLcb() {
        Log.i(TAG, "GeneralHandler processes e-stop LCB");
        mLcb.setLcbMotorRPMhighestPriority(0);
        mLcb.setLcbInclineActionHighestPriority((byte) 0x00);
        mRunProcEStopLcb = true;
    }

    /*******************************************************************************
     * Function Name  : procInitialLcb()
     * Description    : process Initial Lcb
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    protected void procInitialLcb() {
        Log.i(TAG, "##GeneralHandler processes Initial LCB");
        //initial flag
        _isAutoInitialLcb = false;
        mInitialCmdDone = false;
        mDefaultInitialCmdDone = false;
        mOperatingCmdDone = false;
        mInitialFinish = false;
	//Modify TM use 3 cmds to initial Lcb
//        setInitialLcbState(GeneralHandler.InitialLcbState.LCB_INIT_CMD_INITIAL);
        setInitialLcbState(GeneralHandler.InitialLcbState.LCB_INIT_TM_3_CMDS_INITIAL);
        autoInitialGeneralHandlerLCB();
    }

    /*******************************************************************************
     * Function Name  : procForceStopMachine()
     * Description    : process force stop machine
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void procForceStopMachine() {
        Log.i(TAG, "GeneralHandler processes Force Stop Machine");

        if (machineCategoryType.Value == MachineCategoryType.CATEGORY_TM.Value) {
            mLcb.setLcbMotorRPMhighestPriority(0);
            mLcb.setLcbInclineActionHighestPriority((byte) 0x00);
        } else if (machineCategoryType.Value == MachineCategoryType.CATEGORY_BIKE.Value) {
            switch (machineLcbType) {
                case LCB_Performance_Ascent_DC36V:
                case LCB_LCB2:
                    mLcb.setLcbPWMpercentHighestPriority(0);
                    mLcb.setLcbInclineActionHighestPriority((byte) 0x00);
                    break;
                case LCB_ClimbMill_or_A30:
                case LCB_Commercial_Climbmill:
                case LCB_Retail_Climbmill:
                case LCB_Performance_Climbmill_DC36V:
                    mLcb.setLcbClimbmillStatusHighestPriority((byte) 0x01);
                    mLcb.setLcbLimitRPMresistanceHighestPriority(0);
                    break;
                case LCB_LCBA:
                case LCB_1x_Bike_LCB:
                case LCB_Performance_Bike_DC36V:
                    mLcb.setLcbWattsHighestPriority(0);
                    break;
				case LCB_ATHENA_LCB:
                case LCB_TCV_FX:
                    break;
				case LCB_JHT_AD_Board:
				break;
                default:
                    if (machineControlType.Value == ControlType.CONTROL_STEPPER.Value) {
                        mLcb.setLcbLimitRPMresistanceHighestPriority(0);
                    } else {
                        mLcb.setLcbPWMpercentHighestPriority(0);
                    }

                    break;
            }
        } else {
            Log.e(TAG, "procForceStopMachine Machine Category Type error!");
        }

    }

    /*******************************************************************************
     * Function Name  : procSetControlType()
     * Description    : process Set Console Type
     * Input          : byte controlType
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void procSetControlType(byte controlType) {
        Log.i(TAG, "GeneralHandler processes Set Console Type");
        setMachineControlType(controlType);
        mLcb.setLcbMachineType(controlType);
    }
//============================================================================
//============================================================================

//Hanle LCB Initial ===>

    /*******************************************************************************
     * Function Name  : autoStartGeneralHandlerLCB()
     * Description    : auto start lcb
     * Input          : ILcbComm lcbComm, IDeviceDataTransmitted deviceDataTransmitted, IUniDataTransmitted uniDataTransmitted,String path, int baudrate
     * Output         : rtnStart
     * Return         : None
     *******************************************************************************/
    public boolean autoStartGeneralHandlerLCB(ILcbComm lcbComm, IDeviceDataTransmitted deviceDataTransmitted, IUniDataTransmitted uniDataTransmitted, String path, SerialPortConfiguration.BaudRate baudrate) {
        boolean rtnStart = false;

        mStartLcbPath = path;
        mStartLcbBaudrate = baudrate;
        autoStopGeneralHandlerLCB();
        Log.i(TAG, "lcbHandler auto start LCB");

        mLcb = lcbComm;
        mDeviceLcb = deviceDataTransmitted;
        mUniLcb = uniDataTransmitted;

        mLcb.addReceiveListener(mGeneralLcbCommReceive);    //LCB general use
        mDeviceLcb.addReceiveListener(mGeneralLcbCommReceive); //LCB general use
        mUniLcb.addReceiveListener(mGeneralLcbCommReceive); //LCB general use

        rtnStart = mLcb.start(LcbComm.createLcbPort(path, baudrate));
        if (rtnStart) {
            mCheckLcbInfoCounter = 0;
            mRetryGetLcbInfo = 0;
		    mSkipErrorStage = true;
		    mInitialHaveError = false;
            leaveLcbErpMode(); // if needed, wake LCB
            mLcb.getLcbVersionHighestPriority();


            //timer start Lcb Handler auto HeartBeatThread
            timerStartAutoHeartBeatThread();
        } else {
            Log.e(TAG, "Starting serial port error !!!");
        }


        return rtnStart;

    }


    /*******************************************************************************
     * Function Name  : autoStopGeneralHandlerLCB()
     * Description    : process auto stop lcb
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void autoStopGeneralHandlerLCB() {
        Log.i(TAG, "GeneralHandler auto stop LCB");
        //stop HeartBeatThread
        stopAutoHeartBeatThread();

        if (mLcb != null) {
            mLcb.stop();
            mLcb.removeReceiveListener(mGeneralLcbCommReceive);
            mDeviceLcb.removeReceiveListener(mGeneralLcbCommReceive);
            mLcb = null;
            mDeviceLcb = null;
        }
        mIsLCBErrorCodeDetected = false;
    }

    /*******************************************************************************
     * Function Name  : timerReStartHandlerLcb()
     * Description    : used timer start reStartHandlerLcb
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void timerReStartHandlerLcb() {
        mStartReStartHandlerLcbTimer = new JHTTimer(100, new JHTTimer.IOnTimer() {
            @Override
            public void onTimer() {
                reStartHandlerLcb();
                mStartReStartHandlerLcbTimer.stop();
            }
        });
        mStartReStartHandlerLcbTimer.start("LcbRestartTimer");
    }

    /*******************************************************************************
     * Function Name  : reStartHandlerLcb()
     * Description    : Lcb communication exception then re-start HandlerLcb
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void reStartHandlerLcb() {
        reStopHandlerLcb();
        Log.i(TAG, "!!! re-start Handler LCB !!!");

        mLcb.addReceiveListener(mGeneralLcbCommReceive);    //LCB general use
        mDeviceLcb.addReceiveListener(mGeneralLcbCommReceive); //LCB general use
        mUniLcb.addReceiveListener(mGeneralLcbCommReceive); //LCB general use

        mLcb.restart();

    }


    /*******************************************************************************
     * Function Name  : reStopHandlerLcb()
     * Description    : Lcb communication exception  then re-stop lcb
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void reStopHandlerLcb() {
        Log.i(TAG, "!!! re-stop Handler LCB !!!");
        if (mLcb != null) {
            mLcb.stop();
            mLcb.removeReceiveListener(mGeneralLcbCommReceive);
            mDeviceLcb.removeReceiveListener(mGeneralLcbCommReceive);
            mLcb = null;
            mDeviceLcb = null;
        }
    }

    /*******************************************************************************
     * Function Name  : getDeviceGetLCBversion()
     * Description    : process funDeviceGetLCBversion
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getDeviceGetLCBversion() {
    //		mDeviceLcb.funDeviceGetLCBversion();
        mDeviceLcb.funDeviceGetLCBversionHighestPriority();
    }

    /*******************************************************************************
     * Function Name  : autoDeviceGetLcbInformation()
     * Description    : process funDeviceGetLCBinformation
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void autoDeviceGetLcbInformation() {
//		mDeviceLcb.funDeviceGetLCBinformation();
        mDeviceLcb.funDeviceGetLCBinformationHighestPriority();
    }

    /*******************************************************************************
     * Function Name  : setMachineCategory(LcbType mLcbType)
     * Description    : process set Machine Category
     * Input          : LcbType mLcbType
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setMachineLcbType(byte lcbType) {
        int intLcbType = DataUtils.unsignedByteToInt(lcbType);

        machineLcbType = LcbType.fromInt(intLcbType);
        Log.v(TAG, String.format(" Set machine lcb type = %X .", machineLcbType.Value));
        //LcbType Category
        setMachineCategoryType(machineLcbType);

        byte[] byteGroup = {1, 2, 3, 4, 5};
        switch(machineLcbType)
        {
            case LCB_Electromagnetic_brake_Incline_LCB:
            case LCB_BIKE:
            case LCB_Global_Induction:
            case LCB_Global_Full_Function:
            case LCB_Global_Induction_Incline: {
                calPoint = 0;
                mDeviceLcb.funDeviceReadPWM(byteGroup[0]);
                mDeviceLcb.funDeviceReadPWM(byteGroup[1]);
                mDeviceLcb.funDeviceReadPWM(byteGroup[2]);
                mDeviceLcb.funDeviceReadPWM(byteGroup[3]);
                mDeviceLcb.funDeviceReadPWM(byteGroup[4]);
                Log.i(TAG, "start read lcb pwm");
                break;
            }
            default:
                break;
        }
    }

    /*******************************************************************************
     * Function Name  : getMachineLcbType()
     * Description    : process get Machine Category
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public LcbType getMachineLcbType() {
        return machineLcbType;
    }

    /*******************************************************************************
     * Function Name  : setMachineCategoryType(LcbType mLcbType)
     * Description    : process set Machine Category Type
     * Input          : LcbType mLcbType
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void setMachineCategoryType(LcbType mLcbType) {
        switch (mLcbType) {
            case LCB_AC_LCB:
            case LCB_DC_LCB:
            case LCB_TM_LCB:
            case LCB_DCI_daughter_board:
            case LCB_Toptek:
            case LCB_DELTA:
            case LCB_DELTA_AC_11KW:
            case LCB_DELTA_Hybrid:
            case LCB_LS_MD_MCB:
            case LCB_DELTA_T1:
            case LCB_DELTA_spare_1:
            case LCB_DELTA_spare_2:
            case LCB_DELTA_spare_3:
            case LCB_E0:
            case LCB_E1:
            case LCB_Global_200_325HP_110V:
            case LCB_Global_200_325HP_220V:
            case LCB_DC_LCB_300_325:
            case LCB_JHT_VAVE_PS_ES_Board:
            case LCB_JHT_VAVE_T600_Board:
            case LCB_JHT_VAVE_LS_Board:
            case LCB_JHT_VAVE_PP_Board:
            case LCB_JHT_PMSM_22kw:
            case LCB_PS_PMSM_22kw:
                machineCategoryType = MachineCategoryType.CATEGORY_TM;
                break;

            case LCB_LCB1_PWM:
            case LCB_LCB3_PWM:
            case LCB_LCB_CD:
            case LCB_LCB1_Current:
            case LCB_LCB3_Current:
            case LCB_LCB1:
            case LCB_Performance_Ascent_DC36V:
            case LCB_LCB2:
            case LCB_ClimbMill_or_A30:
            case LCB_LCBA:
            case LCB_1x_Bike_LCB:
            case LCB_Commercial_Climbmill:
            case LCB_Retail_Climbmill:
            case LCB_Performance_Climbmill_DC36V:
			case LCB_ATHENA_LCB:
            case LCB_TCV_FX:
            case LCB_Performance_Bike_DC36V:
            case LCB_JHT_AD_Board:
                machineCategoryType = MachineCategoryType.CATEGORY_BIKE;
                break;
            default:
                machineCategoryType = MachineCategoryType.CATEGORY_BIKE;
                break;
        }
        Log.v(TAG, String.format(" Set machine lcb type category = %X .", machineCategoryType.Value));

    }

    /*******************************************************************************
     * Function Name  : getMachineCategoryType()
     * Description    : process get Machine Category
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public MachineCategoryType getMachineCategoryType() {
        return machineCategoryType;
    }

    /*******************************************************************************
     * Function Name  : setMachineControlType(byte controlType)
     * Description    : process set ControlType
     * Input          : byte controlType
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setMachineControlType(byte controlType) {
        int intControlType = DataUtils.unsignedByteToInt(controlType);

        machineControlType = ControlType.fromInt(intControlType);
        Log.v(TAG, String.format(" Set console type = %X .", machineLcbType.Value));
    }

    /*******************************************************************************
     * Function Name  : getControlType()
     * Description    : process get Machine Category
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public ControlType getControlType() {
        return machineControlType;
    }


    /*******************************************************************************
     * Function Name  : checkHaveLcbType()
     * Description    : check machineLcbType
     * Input          : None
     * Output         : None
     * Return         : boolean Have Lcb type
     *******************************************************************************/
    private synchronized boolean checkHaveLcbType() {
        boolean rtnHave = false;

        switch (machineLcbType) {
            case LCB_UNKNOWN:
                mCheckLcbInfoCounter++;
                if (mCheckLcbInfoCounter >= CHECK_NON_LCB_INFO_TIMES) {
                    //Log.v(TAG, "Not have Lcb information.");
                    mCheckLcbInfoCounter = 0;
		    /* modify always get lcb version
		         case: power off and remove 3 or 6 pin can not get lcb version
		                  but 04B0 not always send*/
                    //if (mRetryGetLcbInfo < GET_LCB_INFO_RETRIES) {
                    //    mRetryGetLcbInfo++;
                    if (true) {
                        mLcb.getLcbVersionHighestPriority();
                    } else {
                        //Log.e(TAG, "Not have Lcb information error !! retry "+mRetryGetLcbInfo);
                    }
                }
                rtnHave = false;
                break;
            default:
                mCheckLcbInfoCounter = 0;
                mRetryGetLcbInfo = 0;
                rtnHave = true;
                break;

        }

        return rtnHave;
    }

    /*******************************************************************************
     * Function Name  : checkInitialFinish()
     * Description    : check Initial flag
     * Input          : None
     * Output         : None
     * Return         : boolean Done
     *******************************************************************************/
    public synchronized boolean checkInitialFinish() {
        boolean rtnFinish = false;

        switch (machineLcbType) {
            case LCB_Toptek:
            case LCB_DELTA:
            case LCB_DELTA_AC_11KW:
            case LCB_DELTA_Hybrid:
            case LCB_LS_MD_MCB:
            case LCB_DELTA_T1:
            case LCB_DELTA_spare_1:
            case LCB_DELTA_spare_2:
            case LCB_DELTA_spare_3:
            case LCB_E0:
            case LCB_E1:
            case LCB_TM_LCB:
            case LCB_Global_200_325HP_110V:
            case LCB_Global_200_325HP_220V:
            case LCB_DC_LCB_300_325:
            case LCB_JHT_VAVE_PS_ES_Board:
            case LCB_JHT_VAVE_T600_Board:
            case LCB_JHT_VAVE_LS_Board:
            case LCB_JHT_VAVE_PP_Board:
            case LCB_JHT_PMSM_22kw:
            case LCB_PS_PMSM_22kw:
                if (mInitialCmdDone && mOperatingCmdDone && mInitialFinish) {
                    rtnFinish = true;
                } else {
                    rtnFinish = false;
                }
                break;
            default:
                if (mDefaultInitialCmdDone && mInitialFinish) {
                    rtnFinish = true;
                } else {
                    rtnFinish = false;
                }
                break;

        }

        return rtnFinish;
    }

    /*******************************************************************************
     * Function Name  : setInitialLcbState(InitialLcbState initState)
     * Description    : process set mInitialLcbState
     * Input          : InitialLcbState initState
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setInitialLcbState(InitialLcbState initState) {
        mInitialLcbState = initState;
    }

    /*******************************************************************************
     * Function Name  : getInitialLcbState()
     * Description    : process get mInitialLcbState
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public InitialLcbState getInitialLcbState() {
        return mInitialLcbState;
    }

    /*******************************************************************************
     * Function Name  : autoInitialGeneralHandlerLCB()
     * Description    : Initial LCB handle state
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void autoInitialGeneralHandlerLCB() {

        switch (mInitialLcbState) {
            case LCB_INIT_NONE:
                break;
            case LCB_INIT_TM_3_CMDS_INITIAL:

            case LCB_INIT_CMD_INITIAL:
            case LCB_INIT_DEFAULT_CMD_INITIAL:
                autoInitialLcbCmd();
                break;
            case LCB_INIT_CMD_OPERATING_STOP:
            case LCB_INIT_CMD_OPERATING_ACTION:
                autoInitialLcbCmd();
                break;

            case LCB_INIT_FINISH:
                mInitialFinish = true;
                mBootUpInitial = false;
                mRunProcEStopLcb = false;
                mSafeKeySignalEStopError = false;
                mMcbEStopError = false;
                lastErrorCode = 0;
	  	        mSkipErrorStage = false;
				mInitialHaveError = false;
                if (checkInitialFinish()) {
                    if (mEStopRevert) {
                        mEStopRevert = false;
                        mLcb.procEStopUnlock();
                    }

                    if (mGeneralHandlerCallback != null) {
                        mGeneralHandlerCallback.onLcbInitialFinish();
                    }
                    mInitialLcbState = GeneralHandler.InitialLcbState.LCB_INIT_NONE;
                }

                break;

        }

    }

    /*******************************************************************************
     * Function Name  : autoInitialLcbCmd()
     * Description    : Initial LCB and send command
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void autoInitialLcbCmd() {
        switch (machineLcbType) {
            case LCB_Toptek:
            case LCB_DELTA:
            case LCB_DELTA_AC_11KW:
            case LCB_DELTA_Hybrid:
            case LCB_LS_MD_MCB:
            case LCB_DELTA_T1:
            case LCB_DELTA_spare_1:
            case LCB_DELTA_spare_2:
            case LCB_DELTA_spare_3:
            case LCB_E0:
            case LCB_E1:
            case LCB_TM_LCB:
            case LCB_Global_200_325HP_110V:
            case LCB_Global_200_325HP_220V:
            case LCB_DC_LCB_300_325:
            case LCB_JHT_VAVE_PS_ES_Board:
            case LCB_JHT_VAVE_T600_Board:
            case LCB_JHT_VAVE_LS_Board:
            case LCB_JHT_VAVE_PP_Board:
            case LCB_JHT_PMSM_22kw:
            case LCB_PS_PMSM_22kw:
                if (mInitialLcbState == InitialLcbState.LCB_INIT_TM_3_CMDS_INITIAL) {
		        	if(mSkipErrorStage){
		                    mLcb.setLcbInitial();
		                    mLcb.setLcbOperatingStatus(Constants.OPERATING_STOP);
		                    mLcb.setLcbOperatingStatus(Constants.OPERATING_ACTION);
					}else{
		                    mLcb.setLcbInitialHighestPriority();
		                    mLcb.setLcbOperatingStatusHighestPriority(Constants.OPERATING_STOP);
		                    mLcb.setLcbOperatingStatusHighestPriority(Constants.OPERATING_ACTION);
					}
                } else if (mInitialLcbState == InitialLcbState.LCB_INIT_CMD_INITIAL) {
//                if (mInitialLcbState == InitialLcbState.LCB_INIT_CMD_INITIAL) {
		        	if(mSkipErrorStage){
                	     mLcb.setLcbInitial();
					}else{
	                    mLcb.setLcbInitialHighestPriority();
					}
                } else if (mInitialLcbState == InitialLcbState.LCB_INIT_CMD_OPERATING_STOP) {
		        	if(mSkipErrorStage){
	                    mLcb.setLcbOperatingStatus(Constants.OPERATING_STOP);
					}else{
	                    mLcb.setLcbOperatingStatusHighestPriority(Constants.OPERATING_STOP);
					}
                } else if (mInitialLcbState == InitialLcbState.LCB_INIT_CMD_OPERATING_ACTION) {
		        	if(mSkipErrorStage){
	                    mLcb.setLcbOperatingStatus(Constants.OPERATING_ACTION);
					}else{
	                    mLcb.setLcbOperatingStatusHighestPriority(Constants.OPERATING_ACTION);
					}
                    if (mBootUpInitial == true) {
//				        byte[] data = new byte[1];
//					data[0] = (byte)0x03;
//					mLcb.setLcbUniversal(data);
                    }
                }
                break;
            default:
                if ((mInitialLcbState == InitialLcbState.LCB_INIT_CMD_INITIAL)||(mInitialLcbState == InitialLcbState.LCB_INIT_TM_3_CMDS_INITIAL)) {
                    setInitialLcbState(GeneralHandler.InitialLcbState.LCB_INIT_DEFAULT_CMD_INITIAL);
		        	if(mSkipErrorStage){
	                    mLcb.setLcbInitial();
					}else{
	                    mLcb.setLcbInitialHighestPriority();
					}
                }
                break;

        }

    }
//<=== Hanle LCB Initial

//Hanle LCB Auto Heart Beat ===>

    /*******************************************************************************
     * Function Name  : timerStartAutoHeartBeatThread()
     * Description    : used timer start auto HeartBeatThread
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void timerStartAutoHeartBeatThread() {
        //initial flag
        mBootUpInitial = true;
        _isAutoReplyCmd = false;
        _isAutoInitialLcb = false;
        mStartHeartBeatTimer = new JHTTimer(100, new JHTTimer.IOnTimer() {
            @Override
            public void onTimer() {
                startAutoHeartBeatThread();
                mStartHeartBeatTimer.stop();
            }
        });
        mStartHeartBeatTimer.start("GeneralHbTimer");
    }

    /*******************************************************************************
     * Function Name  : startAutoHeartBeatThread()
     * Description    : process start auto HeartBeatThread
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void startAutoHeartBeatThread() {
        stopAutoHeartBeatThread();
        _heartBeatThread = new HeartBeatThread();
        if (_heartBeatThread != null)
            _heartBeatThread.start();
    }

    /*******************************************************************************
     * Function Name  : stopAutoHeartBeatThread()
     * Description    : process  stop auto HeartBeatThread
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void stopAutoHeartBeatThread() {
        if (_isRunning) {
            _isRunning = false;
            _heartBeatThread.interrupt();

            //initial flag
            _isAutoReplyCmd = false;
            _isAutoInitialLcb = false;

            // wake thread
            synchronized (_notifyHeartBeatSync) {
                _notifyHeartBeatSync.notifyAll();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            _heartBeatThread = null;
        }
    }

    /*******************************************************************************
     * Function Name  : HeartBeatThread
     * Description    : HeartBeat Thread class
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private class HeartBeatThread extends Thread {
        HeartBeatThread() { super("GeneralHbThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "HeartBeatThread+++");

            _isRunning = true;
            try {
                while (_isRunning && !isInterrupted()) {
                    synchronized (_notifyHeartBeatSync) {
                        _notifyHeartBeatSync.wait(HEART_BEAT_TIMER_PERIOD);
                        if (_isRunning) {
                            //chech have lcb info then auto reply cmd
                            if (!_isAutoReplyCmd) {
                                if (checkHaveLcbType()) {
                                    _isAutoReplyCmd = true;
                                }

                            } else {
                                //run auto reply cmd
                                autoReplyCmd();
                            }

                            //chech lcb have finished initialize if not then retry initial
                            if (!_isAutoInitialLcb) {
                                if (checkInitialFinish()) {
                                    _isAutoInitialLcb = true;
                                } else {
                                    mCheckLcbInitialCounter++;
                                    if (mCheckLcbInitialCounter >= CHECK_LCB_INITIAL_TIMES) {
                                        //Log.v(TAG, "LCB initial not finish.");
                                        mCheckLcbInitialCounter = 0;
                                        if (mRetryLcbInitial < LCB_INITIAL_RETRIES) {
                                            mRetryLcbInitial++;
                                            //re-initial LCB
                                            procInitialLcb();
                                        } else {
                                            //Log.e(TAG, "Can Not initial Lcb error !! retry "+mRetryLcbInitial);
                                        }
                                    }
                                }

                            }

                        }
                    }

                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                Log.e(TAG, "Exception in heartBeat thread! ", ex);
                _isRunning = false;
                //re-start HeartBeatThread
                Log.v(TAG, "Re-start HeartBeatThread . ");
                timerStartAutoHeartBeatThread();
            }

            Log.v(TAG, "HeartBeatThread---");
        }
    }

    /*******************************************************************************
     * Function Name  : autoReplyCmd()
     * Description    : process Heart Beat to AutoReply cmd
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyCmd() {
        // add the heartbeat if there's no message in the queue
        if (mLcb != null && mLcb.getTransmitQueueSize() == 0) {
            autoReplyHeartBeatStatus();
        }

        if (_isAutoReplyBatteryStatus)
            autoReplyBatteryStatus();

        if (_isAutoReplyMagnetAngleValue )
            autoReplyMagnetAngleValue();
		
    }

    /*******************************************************************************
     * Function Name  : autoReplyHeartBeatStatus()
     * Description    : process Heart Beat to AutoReply LCB status
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyHeartBeatStatus() {
        /*Prevention mechanism:
         * If communication thread exception will re-start reStartHandlerLcb()
         */
	/*if(mLcb.checkCommunicationThread()){
		timerReStartHandlerLcb();
	}else{
		mLcb.getLcbStatusLowPriority();
	}
	*/
        /* If communication thread exception will show 04B0 error code  */
        if (!mLcb.checkCommThreadExcept()) {
            mLcbProcComm.procCommLcbErrorHandler();
            Log.e(TAG, "CommunicationThread exception ! ");
        } else {
            switch (machineLcbType) {
                case LCB_Toptek: // LCB 0xC1 does not support this command
                    //Log.d(TAG, "Skipping get LCB status for LCB 0xC1");
                    break;
                default:
                    mLcb.getLcbStatusLowPriority(LcbComm.QueueMode.ReplaceSimilar);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyGeneralBattery()
     * Description    : set General AutoReplyCmd Battery Status
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyGeneralBattery(boolean enable) {
        if (enable)
            _isAutoReplyBatteryStatus = true;
        else
            _isAutoReplyBatteryStatus = false;
    }

    /*******************************************************************************
     * Function Name  : autoReplyBatteryStatus()
     * Description    : process Heart Beat to AutoReply LCB General Battery Status
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyBatteryStatus() {
        if (cntIntervalTimesBattery == TIMES_BATTERY_INTERVAL) {
            cntIntervalTimesBattery = 0;
            mLcb.getLcbBatteryVoltageLowPriority(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesBattery++;
        }
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyGeneralMagnetAngle()
     * Description    : set General AutoReplyCmd Magnet Angle 
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyGeneralMagnetAngle(boolean enable) {
        if (enable)
            _isAutoReplyMagnetAngleValue = true;
        else
            _isAutoReplyMagnetAngleValue = false;
    }

    /*******************************************************************************
     * Function Name  : autoReplyMagnetAngleValue()
     * Description    : process Heart Beat to AutoReply LCB General MagnetAngle value
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyMagnetAngleValue() {
        if (cntIntervalTimesMagnetAngle == TIMES_MAGNET_ANGLE_INTERVAL) {
            cntIntervalTimesMagnetAngle = 0;

	        mUniLcb.funUniGetMagnetAngle(LcbComm.QueueMode.ReplaceSimilar);//> Get  magnet  angle
			
        } else {
            cntIntervalTimesMagnetAngle++;
        }
    }

//<===Hanle LCB Auto Heart Beat

//Hanle LCB Error ===>

    /*******************************************************************************
     * Function Name  : setGetLcbErrorState(GetLcbErrorState getErrorState)
     * Description    : process set mGetLcbErrorState
     * Input          : GetLcbErrorState getErrorState
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public synchronized void setGetLcbErrorState(GetLcbErrorState getErrorState) {
        mGetLcbErrorState = getErrorState;
    }

    /*******************************************************************************
     * Function Name  : getGetLcbErrorState()
     * Description    : process get mGetLcbErrorState
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public GetLcbErrorState getGetLcbErrorState() {
        return mGetLcbErrorState;
    }

    /*******************************************************************************
     * Function Name  : checkInGetErrorState()
     * Description    : check mGetLcbErrorState is LCB_ERROR_NONE
     * Input          : None
     * Output         : None
     * Return         : boolean mGetLcbErrorState is LCB_ERROR_NONE
     *******************************************************************************/
    public synchronized boolean checkInGetErrorState() {
        boolean rtnProcess = false;

        switch (mGetLcbErrorState) {
            case LCB_ERROR_NONE:
                mCheckGetErrorStateCounter = 0;
                mCheckSkipErrorStateCounter = 0;
                rtnProcess = false;
                break;
            case LCB_ERROR_GET:
                mCheckGetErrorStateCounter++;
                //If get error code command not response, set mGetLcbErrorState LCB_ERROR_NONE.
                //If not do this will always int LCB_ERROR_GET state
                if (mCheckGetErrorStateCounter >= CHECK_GET_ERROR_STATE_TIMES) {
                    mGetLcbErrorState = GetLcbErrorState.LCB_ERROR_NONE;
                    mCheckGetErrorStateCounter = 0;
                }
                rtnProcess = true;
                break;
            case LCB_ERROR_SKIP:
                mCheckSkipErrorStateCounter++;
                //If skip error code command not response, set mGetLcbErrorState LCB_ERROR_NONE.
                //If not do this will always int LCB_ERROR_SKIP state
                if (mCheckSkipErrorStateCounter >= CHECK_SKIP_ERROR_STATE_TIMES) {
                    mGetLcbErrorState = GetLcbErrorState.LCB_ERROR_NONE;
                    mCheckSkipErrorStateCounter = 0;
                }
                rtnProcess = true;
                break;
            default:
                Log.e(TAG, "Get Lcb Error State error!");
                mCheckGetErrorStateCounter = 0;
                mCheckSkipErrorStateCounter = 0;
                mGetLcbErrorState = GetLcbErrorState.LCB_ERROR_NONE;
                rtnProcess = false;
                break;

        }

        return rtnProcess;
    }

    /*******************************************************************************
     * Function Name  : runGetErrorProcesses()
     * Description    : Processes Get Error State
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public synchronized void runGetErrorProcesses() {


        switch (mGetLcbErrorState) {
            case LCB_ERROR_NONE:
                mCheckGetErrorStateCounter = 0;
                mCheckSkipErrorStateCounter = 0;
                break;
            case LCB_ERROR_GET:
                mLcb.getLcbErrorCodeHighPriority();
                break;
            case LCB_ERROR_SKIP:
                mLcb.setLcbSkipCurrErrorHighPriority();
                break;
        }

    }

    /*******************************************************************************
     * Function Name  : handleExceptionErrorCode(int lcbErrorCode)
     * Description    : handle Exception ErrorCode
     * Input          : int lcbErrorCode
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void handleExceptionErrorCode(int lcbErrorCode) {


        switch (lcbErrorCode) {
            case (int) 0x02B2: {    //E-Stop
                if (machineCategoryType.Value == MachineCategoryType.CATEGORY_TM.Value) {
                    Log.v(TAG, "02B2 E-Stop highest priority stop cmd.");
                    if (!mRunProcEStopLcb) {
                        procEStopLcb();
                    }

                    if (mSafeKeyStopMachine == true) {//Safe key signal Stop Machine trigger 02B2
                        mSafeKeySignalEStopError = true;
                        //marked, Safety key signal not received whren got 02B2 within 5 sec. Don't show 024F
                        //startCheckMcbEStopError();
                    } else {//Mcb E-Stop trigger 02B2
                        mMcbEStopError = true;
                    }

                } else if (machineCategoryType.Value == MachineCategoryType.CATEGORY_BIKE.Value) {
                } else {
                    Log.e(TAG, "Machine Category Type error!");
                }
                break;
            }
            case (int) 0x024F: {    //E-Stop Mcb error
                Log.e(TAG, "Get estop signal but MCB not have return 02B2 error code within 5 seconds. Sned 0x024F.");
                mSafeKeySignalEStopError = false;
                mMcbEStopError = false;
                break;
            }

            default:
                break;
        }

    }

    /*******************************************************************************
     * Description    : Check Mcb E-Stop have 02B2 error code
     *******************************************************************************/
    private void startCheckMcbEStopError() {
        stopCheckMcbEStopError();
        mMcbEStopErrorTimer = new JHTTimer(CHECK_MCB_ESTOP_TIMER_OUT, new JHTTimer.IOnTimer() {
            @Override
            public void onTimer() {
                checkMcbEStopError();
            }
        });
        mMcbEStopErrorTimer.start("EStopErrorTimer");
    }

    private void stopCheckMcbEStopError() {
        mMcbEStopErrorTimer.stop();
    }

    /*******************************************************************************
     * Function Name  : checkMcbEStopError()
     * Description    : process check Mcb EStop Error
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void checkMcbEStopError() {
        stopCheckMcbEStopError();
        if (mSafeKeySignalEStopError && mMcbEStopError) {
            mSafeKeySignalEStopError = false;
            mMcbEStopError = false;
        } else {
            //trigger 024F error code
            mLcb.procEStopMcbError();
        }
    }

//<=== Hanle LCB Error

    /*******************************************************************************
     * Function Name  : setAdvancedLogging()
     * Description    : set Advanced Logging logMessages and logMessageDetails
     * Input          : boolean logMessages, boolean logMessageDetails
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAdvancedLogging(boolean logMessages, boolean logMessageDetails) {
        mLoggingMsg = logMessages;
        mLoggingDetail = logMessageDetails;

        mAdvancedLogging = AdvancedLogging.getInstance();
        byte setLoggingStatus = (byte) 0x00;

        if (logMessages) {
            setLoggingStatus = (byte) (setLoggingStatus | AdvancedLogging.LOGGING_STATUS_BIT.FILTER_LOGGING_STATUS_B0_BIT_MESSAGES.getByteValue());
        }

        if (logMessageDetails) {
            setLoggingStatus = (byte) (setLoggingStatus | AdvancedLogging.LOGGING_STATUS_BIT.FILTER_LOGGING_STATUS_B1_BIT_MESSAGE_DETAILS.getByteValue());
        }

        mAdvancedLogging.setAdvLoggingStatus(setLoggingStatus);
        mLcb.procSetAdvLoggingStatus();
    }

    /*******************************************************************************
     * Function Name  : clearLcbSerialNumber()
     * Description    : clear Lcb SerialNumber
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void clearLcbSerialNumber() {
        byte[] data = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00};

        mLcb.setLcbCmdSerialNumber(data);
    }

    /*******************************************************************************
     * Function Name  : setLcbSerialNumber()
     * Description    : set Lcb Serial Number
     * Input          : String serial
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setLcbSerialNumber(String serial) {
        byte[] data = new byte[serial.length() + 3];
        int serialDataIndex = 0;
        int dataIndex = 0;

        data[0] = (byte) 0x01;
        data[1] = (byte) 0x00;
        data[2] = (byte) serial.length();
        for (byte b : serial.getBytes()) {
            dataIndex = 3 + serialDataIndex;
            data[dataIndex] = b;
            serialDataIndex++;
        }


        mLcb.setLcbCmdSerialNumber(data);
    }

    /*******************************************************************************
     * Function Name  : getLcbSerialNumber()
     * Description    : get Lcb SerialNumber
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getLcbSerialNumber() {
        byte[] data = new byte[]{(byte) 0x02, (byte) 0x00, (byte) 0x00};//read

        mLcb.setLcbCmdSerialNumber(data);
    }

    /*******************************************************************************
     * Function Name  : getLcbMcbSelfSerialNumber()
     * Description    : get Lcb/Mcb itself SerialNumber , this function different getLcbSerialNumber.
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getLcbMcbSelfSerialNumber() {
        byte[] data = new byte[]{(byte) 0x04, (byte) 0x00, (byte) 0x00};//read  Lcb/Mcb itself SerialNumber

        mLcb.setLcbCmdSerialNumber(data);
    }

    /*******************************************************************************
     * Function Name  : getLcbSnMaxLength()
     * Description    : get Lcb SerialNumber Max Length
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getLcbSnMaxLength() {
        byte[] data = new byte[]{(byte) 0x03, (byte) 0x00, (byte) 0x00};//read  SerialNumber max length

        mLcb.setLcbCmdSerialNumber(data);
    }

    /*******************************************************************************
     * Function Name  : setLcbIntoUpdateProgramMode()
     * Description    : set Lcb Into Update Program Mode
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setLcbIntoUpdateProgramMode() {
        String editString = "JS"; //NON-NLS

        Charset characterSet = Charset.forName("US-ASCII");
        byte[] inputData = editString.getBytes(characterSet);

        mLcb.setLcbUpdateProgram(inputData);
    }

    /*******************************************************************************
     * Function Name  : startMagnetAngleCalibration()
     * Description    : start Magnet Angle Calibration
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void startMagnetAngleCalibration() {
        byte setStartState = (byte) 0x00; //> Start to tuning  magnet 

        mUniLcb.funUniSetMagnetAngleState(setStartState);
    }
    /*******************************************************************************
     * Function Name  : getMagnetAngle()
     * Description    : get Magnet Angle Calibration
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getMagnetAngle() {
        mUniLcb.funUniGetMagnetAngle(LcbComm.QueueMode.AlwaysAdd);
    }

    /*******************************************************************************
     * Function Name  : getMcbVoltage()
     * Description    : get Mcb Voltage
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getMcbVoltage() {
        mUniLcb.funUniGetMcbVoltage(LcbComm.QueueMode.AlwaysAdd);
    }

    /*******************************************************************************
     * Function Name  : setPFCmode()
     * Description    : set PFC mode 
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setPFCmode(boolean enable) {
        byte setMode = (byte) 0x00; //> PFC mode ON
	if(enable)
		setMode = (byte) 0x00;//enable
	else
		setMode = (byte) 0x01;//disable

        mUniLcb.funUniSetPFCmode(setMode);
    }

    /*******************************************************************************
     * Function Name  : initLcbMcbBeforeWorkout()
     * Description    : Initialize LCB and MCB before starting the workout.
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initLcbMcbBeforeWorkout() {
        mLcb.setLcbInitialHighestPriority();
    }

//======================================================================
//======================================================================
//======================================================================

    public boolean generalHandlerStartLCBwithoutHeartBeat(ILcbComm lcbComm, String path, SerialPortConfiguration.BaudRate baudrate) {
        boolean rtnStart = false;

        mStartLcbPath = path;
        mStartLcbBaudrate = baudrate;
        Log.i(TAG, "generalHandler startLCB without heart beat");

        mLcb = lcbComm;
        mLcb.addReceiveListener(mGeneralLcbCommReceive);
        mDeviceLcb.removeReceiveListener(mGeneralLcbCommReceive);
        mUniLcb.removeReceiveListener(mGeneralLcbCommReceive);

        rtnStart = mLcb.start(LcbComm.createLcbPort(path, baudrate));

        return rtnStart;

    }

    /*******************************************************************************
     * Function Name  : erpStopUartPort()
     * Description    : erp Stop Uart Port
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void erpStopUartPort() {
        Log.i(TAG, "GeneralHandler erp Stop Uart Port");
        if (mLcb != null) {
            mLcb.erpStopUartPort();
        }
    }


/*	
    *******************************************************************************
    * Function Name  : generalHandlerStartLCB()
    * Description    : process start lcb
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************
    public boolean generalHandlerStartLCB(ILcbComm lcbComm, String path, int baudrate) {
	boolean	rtnStart = false;

	mStartLcbPath = path;
	mStartLcbBaudrate = baudrate;
        generalHandlerStopLCB();
        Log.i(TAG, "generalHandler startLCB");

        mLcb = lcbComm;
        mLcb.addReceiveListener(mGeneralLcbCommReceive);
        rtnStart = mLcb.start(new SerialPort(), path, baudrate);

	if(rtnStart){
		mLcb.setLcbInitial();
		startLcbStatusHeartBeat();
	}
	
	return rtnStart;
		
    }

    *******************************************************************************
    * Function Name  : generalHandlerStopLCB()
    * Description    : process stop lcb
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************
    public void generalHandlerStopLCB() {
        Log.i(TAG, "generalHandler stopLCB");
        stopLcbStatusHeartBeat();
        if (mLcb != null) {
            mLcb.stop();
            mLcb.removeReceiveListener(mGeneralLcbCommReceive);
            mLcb = null;
        }
        mIsLCBErrorCodeDetected = false;
    }

   *******************************************************************************
    * Function Name  : startLcbStatusHeartBeat()
    * Description    : process start Heart Beat to get lcb status
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************
    public synchronized void startLcbStatusHeartBeat() {
        stopLcbStatusHeartBeat();
        mLcbStatusHeartBeatTimer = new AbsoluteTimer();
        mLcbStatusHeartBeatTimer.scheduleAtFixedRate(new AbsoluteTimerTask() {
            @Override
            public void run() {
                // add the heartbeat if there's no message in the queue
                if (mLcb != null && mLcb.getTransmitQueueSize() == 0) {
                    mLcb.getLcbStatus();
                }
            }
        }, 300, HEART_BEAT_TIMER_PERIOD);
    }

    *******************************************************************************
    * Function Name  : stopLcbStatusHeartBeat()
    * Description    : process stop Heart Beat getting lcb status
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************
    public synchronized void stopLcbStatusHeartBeat() {
        if (mLcbStatusHeartBeatTimer != null) {
            mLcbStatusHeartBeatTimer.cancel();
            mLcbStatusHeartBeatTimer.purge();
            mLcbStatusHeartBeatTimer = null;
        }
    }
*/

//ERP enter/leave function , Chimera project no used ======>
/*
    *******************************************************************************
    * Function Name  : generalHandlerLeaveErpStartLCB()
    * Description    : process LeaveErp and Start  lcb
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************
    public boolean generalHandlerLeaveErpStartLCB(ILcbComm lcbComm, String path, int baudrate) {
	boolean	rtnStart = false;

	mStartLcbPath = path;
	mStartLcbBaudrate = baudrate;
        generalHandlerStopLCB();
        Log.i(TAG, "generalHandler startLCB");

        mLcb = lcbComm;
        mLcb.addReceiveListener(mGeneralLcbCommReceive);
        rtnStart = mLcb.start(new SerialPort(), path, baudrate);

	//check boot up lcb leave erp and will callback leave status
	mLcb.checkBootUpLeaveLcbErp();
	
	return rtnStart;
		
    }

    public void generalHandlerEnterLcbErpMode() {
		mLcb.enterLcbErpMode();
    }

    public void generalHandlerLeaveLcbErpMode() {
		mLcb.leaveLcbErpMode(mStartLcbPath, mStartLcbBaudrate);
    }

    public void bootUpLeaveLcbErpSuccess() {
		mLcb.setLcbInitial();
		startLcbStatusHeartBeat();
    }
*/
//<=====ERP enter/leave function , Chimera project no used

    public void enterLcbErpMode() {
        if (!ProductConfig.hasIO()) {
            stopAutoHeartBeatThread(); // stop adding LCB commands
            mLcb.enterLcbErpMode(); // IO normally handles this
        }
    }

    public void leaveLcbErpMode() {
        if (!ProductConfig.hasIO()) {
            mLcb.leaveLcbErpMode(mStartLcbPath, mStartLcbBaudrate); // IO normally handles this
        }
    }

}
