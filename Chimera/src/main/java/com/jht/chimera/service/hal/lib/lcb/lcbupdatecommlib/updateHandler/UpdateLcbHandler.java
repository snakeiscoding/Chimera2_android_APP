package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.updateHandler;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMAthena.CHIMAthenaLcbUpdateFlow;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMClimb.CHIMClimbLcbUpdateFlow;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMClimb.ClimbLCBinfo;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCB1.CHIMLCB1LcbUpdateFlow;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCB1xBike.CHIMLCB1xBikeLcbUpdateFlow;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCB2.CHIMLCB2LcbUpdateFlow;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCBA.CHIMLCBALcbUpdateFlow;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.E600ADboard.E600ADboardLcbUpdateFlow;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.ICR70.Icr70LcbUpdateFlow;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.ILcbUpdateComm;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.ILcbUpdateProcessReceive;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.UpdateComm;
import com.jht.serialport.SerialPortConfiguration;

public class UpdateLcbHandler implements IUpdateLcbHandler {
    private static final String TAG = "UpdateLcbHandler";

    private SerialPortConfiguration.BaudRate mStartLcbBaudrate;
    protected ILcbUpdateComm mUpdateLcb;
    private SerialPortConfiguration.BaudRate mStartLcbUpdateBaudrate;
    private String mStartLcbUpdatePath;

    private UpdateLcbCallbackHandlerLCB mUpdateLcbCommReceive = new UpdateLcbCallbackHandlerLCB(this);
    protected IUpdateLcbHandlerCallback mUpdateLcbHandlerCallback;

    private final String LCB_UPDATE_DEVICE_PATH = ProductConfig.portMappings.lcbPath();
    private final SerialPortConfiguration.BaudRate LCB_UPDATE_BAUD_RATE = SerialPortConfiguration.BaudRate.B115200;

    private boolean mOpenLcbUpdatePort = false;

    //Hanle LCB Auto Heart Beat ===>
    private static volatile JHTTimer mStartUpdateLcbProcessTimer;
//<=== Hanle LCB Auto Heart Beat 

    private LcbType machineLcbType = LcbType.LCB_UNKNOWN;
    private String updateFilePath = "";


    private UpdateLcbHandler() {
    }

    private static class SingletonHolder {
        private static UpdateLcbHandler Instance = new UpdateLcbHandler();
    }

    public static UpdateLcbHandler getInstance() {
        return SingletonHolder.Instance;
    }

    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }


    /*******************************************************************************
     * Function Name  : setUpdateLcbHandlerCallback()
     * Description    : register IUpdateLcbHandlerCallback
     * Input          : IUpdateLcbHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setUpdateLcbHandlerCallback(IUpdateLcbHandlerCallback updateLcbHandlerCallback) {
        mUpdateLcbHandlerCallback = updateLcbHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : startUpdateLcbHandler()
     * Description    :start update lcb flow
     * Input            : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public boolean startUpdateLcbHandler(ILcbUpdateComm lcbUpdateComm, String path, SerialPortConfiguration.BaudRate baudrate) {
        boolean rtnStart = false;

        mStartLcbUpdateBaudrate = baudrate;
        mStartLcbUpdatePath = path;

        mUpdateLcb = lcbUpdateComm;
        mUpdateLcb.addReceiveListener(mUpdateLcbCommReceive);    //LCB general use

        rtnStart = mUpdateLcb.start(LcbComm.createLcbPort(path, baudrate));

        if (rtnStart) {//start update lcb
            //timer start Lcb Update Process Thread
            SystemClock.sleep(500);
            timerStartAutoUpdateLcbThread();
        }

        return rtnStart;
    }

    /*******************************************************************************
     * Function Name  : openLcbUpdatePort()
     * Description    :open Update Port
     * Input            : None
     * Output         : None
     * Return         : boolean
     *******************************************************************************/
    public boolean openLcbUpdatePort() {
        boolean openUpdatePort = false;

        if (UpdateComm.getInstance().start(LcbComm.createLcbPort(LCB_UPDATE_DEVICE_PATH, LCB_UPDATE_BAUD_RATE))) {
            openUpdatePort = true;
            mOpenLcbUpdatePort = true;
        } else {
            mOpenLcbUpdatePort = false;
        }

        return openUpdatePort;
    }

    /*******************************************************************************
     * Function Name  : startUpdateLcbProcess()
     * Description    : startUpdateLcbProcess
     * Input            : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void startUpdateLcbProcess() {
        //implement all kinds of Lcbs
        CHIMLCBALcbUpdateFlow.Instance.startCHIMLCBALcbUpdateFlowProcess(StorageUtils.getUSBPath().getAbsolutePath() + "/LCBA_B_S003_20150708.bin"); //NON-NLS
    }

    /*******************************************************************************
     * Function Name  : startLcbUpdateFlow
     * Description    :start update lcb flow
     * Input            : LcbType mLcbType , String filePath
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void startLcbUpdateFlow(LcbType mLcbType, String filePath, ILcbUpdateProcessReceive lcbUpdateProcessListener) {

        machineLcbType = mLcbType;
        updateFilePath = filePath;

        if (machineLcbType == LcbType.LCB_UNKNOWN) {
            Log.e(TAG, "startLcbUpdateFlow Update Lcb Type Unknow. !!!");
            return;
        }

        if ((updateFilePath == null) || (updateFilePath == "")) {
            Log.e(TAG, "startLcbUpdateFlow Update File Path is empty. !!!");
            return;
        }

        if (!UpdateComm.getInstance().start(LcbComm.createLcbPort(LCB_UPDATE_DEVICE_PATH, LCB_UPDATE_BAUD_RATE))) {//start update lcb
            Log.e(TAG, "startLcbUpdateFlow Update Open Serial Port Error. !!!");
        } else {
            SystemClock.sleep(500);
            switch (machineLcbType) {
                case LCB_LCB1_Current:
                    CHIMLCB1LcbUpdateFlow.Instance.startCHIMLCB1LcbUpdateFlowProcess(updateFilePath);
                    CHIMLCB1LcbUpdateFlow.Instance.addProcessReceiveListener(lcbUpdateProcessListener);
                    break;
                case LCB_LCB2:
                    CHIMLCB2LcbUpdateFlow.Instance.startCHIMLCB2LcbUpdateFlowProcess(updateFilePath);
                    CHIMLCB2LcbUpdateFlow.Instance.addProcessReceiveListener(lcbUpdateProcessListener);
                    break;
                case LCB_Commercial_Climbmill:
                    ClimbLCBinfo.consoleLcbType = LcbType.LCB_Commercial_Climbmill.Value;
                    CHIMClimbLcbUpdateFlow.Instance.startCHIMClimbLcbUpdateFlowProcess(updateFilePath);
                    CHIMClimbLcbUpdateFlow.Instance.addProcessReceiveListener(lcbUpdateProcessListener);
                    break;
                case LCB_Retail_Climbmill:
                    ClimbLCBinfo.consoleLcbType = LcbType.LCB_Retail_Climbmill.Value;
                    CHIMClimbLcbUpdateFlow.Instance.startCHIMClimbLcbUpdateFlowProcess(updateFilePath);
                    CHIMClimbLcbUpdateFlow.Instance.addProcessReceiveListener(lcbUpdateProcessListener);
                    break;
                case LCB_LCBA:
                    CHIMLCBALcbUpdateFlow.Instance.startCHIMLCBALcbUpdateFlowProcess(updateFilePath);
                    CHIMLCBALcbUpdateFlow.Instance.addProcessReceiveListener(lcbUpdateProcessListener);
                    break;
                case LCB_1x_Bike_LCB:
                    CHIMLCB1xBikeLcbUpdateFlow.Instance.startCHIMLCB1xBikeLcbUpdateFlowProcess(updateFilePath);
                    CHIMLCB1xBikeLcbUpdateFlow.Instance.addProcessReceiveListener(lcbUpdateProcessListener);
                    break;
                case LCB_JHT_AD_Board:
                    E600ADboardLcbUpdateFlow.Instance.startE600ADboardLcbUpdateFlowProcess(updateFilePath);
                    E600ADboardLcbUpdateFlow.Instance.addProcessReceiveListener(lcbUpdateProcessListener);
                    break;
                case LCB_ATHENA_LCB:
                    CHIMAthenaLcbUpdateFlow.Instance.startCHIMAthenaLcbUpdateFlowProcess(updateFilePath);
                    CHIMAthenaLcbUpdateFlow.Instance.addProcessReceiveListener(lcbUpdateProcessListener);
                    break;
                case LCB_JIS_ATHENA: // ICR70
                    Icr70LcbUpdateFlow.Instance.startLcbUpdateFlowProcess(updateFilePath);
                    Icr70LcbUpdateFlow.Instance.addProcessReceiveListener(lcbUpdateProcessListener);
                    break;
                default:
                    Log.e(TAG, "startLcbUpdateFlow cannot find update flow for " + machineLcbType);
            }
        }
    }
//Hanle Auto Update Lcb ===>

    /*******************************************************************************
     * Function Name  : timerStartAutoUpdateLcbThread()
     * Description    : used timer start auto  Update Lcb Thread
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void timerStartAutoUpdateLcbThread() {
        //initial flag
        mStartUpdateLcbProcessTimer = new JHTTimer(100, new JHTTimer.IOnTimer() {
            @Override
            public void onTimer() {
                mStartUpdateLcbProcessTimer.stop();
                CHIMLCBALcbUpdateFlow.Instance.startCHIMLCBALcbUpdateFlowProcess(StorageUtils.getUSBPath().getAbsolutePath() + "/LCBA_B_S003_20150708.bin"); //NON-NLS
            }
        });
        mStartUpdateLcbProcessTimer.start();
    }
//<===Hanle Auto Update Lcb
}
