package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

import android.util.Log;

import com.jht.chimera.service.hal.lib.frictiontest.TreadmillOperateRxData;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeInfoSpinDown;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class LcbProcComm {
    private static final String TAG = "LcbProcComm";

    private ArrayList<ILcbCommReceive> _receiveListenerList = new ArrayList<>();
    private final Object _receiveListenerListLock = new Object();

    private static final int COMM_LCB_ERROR = 0x04B0;
    private static final int E_STOP_ERROR = 0x02B2;
    private static final int E_STOP_MCB_ERROR = 0x024F;
    private static final int E_STOP_UNLOCK = 0x02B1;
    private static final int TM_NO_RPM_RESPONSE = 0x00A1;

    private final ErrorCodeRecord _errorCodeRecordList = ErrorCodeRecord.getInstance();

    private final DeviceDataProcReceived mDeviceDataProcReceived = DeviceDataProcReceived.getInstance();
    private final UniDataProcReceived mUniDataProcReceived = UniDataProcReceived.getInstance();

    private GeneralHandler mGeneralHandler;
    private LcbType machineLcbType = LcbType.LCB_UNKNOWN;

    private final IndoorBikeInfoSpinDown      _indoorBikeInfoSpinDown         = new IndoorBikeInfoSpinDown();

    private LcbProcComm() {
    }

    private static class SingletonHolder {
        private static final LcbProcComm Instance = new LcbProcComm();
    }

    public static LcbProcComm getInstance() {
        return SingletonHolder.Instance;
    }


    public void setReceiveListenerList(ArrayList<ILcbCommReceive> lcbComReceiveListenerList) {
        _receiveListenerList = lcbComReceiveListenerList;
    }

    /*******************************************************************************
     * Function Name  : procChangeLcbInclineMotorDownwardStatusHandler()
     * Description    : process LCB status byte incline motor downward bit change handler
     * Input          : boolean isLcbDownwardStatus
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procChangeLcbInclineMotorDownwardStatusHandler(boolean isLcbDownwardStatus) {
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.changeLcbInclineMotorDownwardStatusHandler(isLcbDownwardStatus);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procChangeLcbInclineMotorUpwardStatusHandler()
     * Description    : process LCB status byte incline motor upward bit change handler
     * Input          : boolean isLcbUpwardStatus
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procChangeLcbInclineMotorUpwardStatusHandler(boolean isLcbUpwardStatus) {
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.changeLcbInclineMotorUpwardStatusHandler(isLcbUpwardStatus);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procChangeLcbMainMotorStatusHandler()
     * Description    : process LCB status byte main motor status bit change handler
     * Input          : boolean isMainMotorStatus
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procChangeLcbMainMotorStatusHandler(boolean isMainMotorStatus) {
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.changeLcbMainMotorStatusHandler(isMainMotorStatus);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procChangeLcbEncoderRPMStatusHandler()
     * Description    : process LCB status byte encoder. RPM status bit change handler
     *				This bit will be to indicate the power source when
     *                           the control board is an AC / DC (self-powered) system for
     *                           bike series. 1 : AC 0: DC )
     * Input          : boolean isEncoderRPMStatus
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procChangeLcbEncoderRPMStatusHandler(boolean isEncoderRPMStatus) {
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.changeLcbEncoderRPMStatusHandler(isEncoderRPMStatus);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procChangeLcbBatteryCapacityStatusHandler()
     * Description    : process LCB status byte battery capacity status bit change handler
     * Input          : boolean isBatteryCapacityStatus
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procChangeLcbBatteryCapacityStatusHandler(boolean isBatteryCapacityStatus) {
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.changeLcbBatteryCapacityStatusHandler(isBatteryCapacityStatus);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procChangeLcbCommandErrorStatusHandler()
     * Description    : process LCB status byte command error status bit change handler
     * Input          : boolean isCommandErrorStatus
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procChangeLcbCommandErrorStatusHandler(boolean isCommandErrorStatus) {
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.changeLcbCommandErrorStatusHandler(isCommandErrorStatus);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procChangeLcbLcbErrorStatusDetectedHandler()
     * Description    : process LCB status byte Lcb error status bit change handler
     * Input          : boolean isLcbErrorStatus
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procChangeLcbLcbErrorStatusDetectedHandler(boolean isLcbErrorStatus) {
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.changeLcbLcbErrorStatusDetectedHandler(isLcbErrorStatus);
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procChangeLcbInitialCalibrationStatusHandler()
     * Description    : process LCB status byte initial (calibration ) status bit change handler
     * Input          : boolean isInitialCalibrationStatus
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procChangeLcbInitialCalibrationStatusHandler(boolean isInitialCalibrationStatus) {
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.changeLcbInitialCalibrationStatusHandler(isInitialCalibrationStatus);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbInitialHandler()
     * Description    : process set LCB initial handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbInitialHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.setLcbInitialHandler();
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbStatusHandler()
     * Description    : process get LCB status handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbStatusHandler(LcbPacket packet) {

        byte byteLcbStatus = packet.getRxLcbStatus();

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbStatusHandler(byteLcbStatus);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbErrorCodeHandler()
     * Description    : process get LCB error code handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbErrorCodeHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_ERROR_CODE) {
            Log.v(TAG, "Data ERROR_CODE received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortLcbErrorCode = byteBuffer.getShort();
        int intLcbErrorCode = DataUtils.shortToInt(shortLcbErrorCode);

        {
            synchronized (_receiveListenerListLock) {
                int listSize = _receiveListenerList.size();
                for (int j = 0; j < listSize; j++) {
                    ILcbCommReceive listener = _receiveListenerList.get(j);

                    if (listener != null)
                        listener.getLcbErrorCodeHandler(intLcbErrorCode);
                }
            }
        }

        //error code 0x0000 don't return error code to UI
        if (intLcbErrorCode != 0x0000) {
            //add LCB lib ErrorCode to store
            _errorCodeRecordList.addErrorCodeRecordItem(intLcbErrorCode);
            //callback errorCodeRecordList to UI
            int errorRecordCounter = _errorCodeRecordList.getErrorRecordCounter();
            if (errorRecordCounter != 0) {
                for (int i = 0; i < errorRecordCounter; i++) {
                    synchronized (_receiveListenerListLock) {
                        int rtnUIErrorCode = _errorCodeRecordList.getErrorCodeRecord();
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listenerList = _receiveListenerList.get(j);

                            if (listenerList != null)
                                listenerList.returnErrorCodeToUIHandler(rtnUIErrorCode);
                        }
                    }
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procCommLcbErrorHandler()
     * Description    : process CommunicationThread timeout error (0x04B0) handler
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procCommLcbErrorHandler() {
        int intLcbErrorCode = COMM_LCB_ERROR;

        {
            synchronized (_receiveListenerListLock) {
                int listSize = _receiveListenerList.size();
                for (int j = 0; j < listSize; j++) {
                    ILcbCommReceive listener = _receiveListenerList.get(j);

                    if (listener != null)
                        listener.getLcbErrorCodeHandler(intLcbErrorCode);
                }
            }
        }

        if (intLcbErrorCode != 0x0000) {
            //add LCB lib ErrorCode to store
            _errorCodeRecordList.addErrorCodeRecordItem(intLcbErrorCode);

            //callback errorCodeRecordList to UI
            int errorRecordCounter = _errorCodeRecordList.getErrorRecordCounter();
            if (errorRecordCounter != 0) {
                for (int i = 0; i < errorRecordCounter; i++) {
                    synchronized (_receiveListenerListLock) {
                        int rtnUIErrorCode = _errorCodeRecordList.getErrorCodeRecord();
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listenerList = _receiveListenerList.get(j);

                            if (listenerList != null)
                                listenerList.returnErrorCodeToUIHandler(rtnUIErrorCode);
                        }
                    }
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procEStopErrorHandler()
     * Description    : process E-Stopp error (0x02B2) handler
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procEStopErrorHandler() {
        int intLcbErrorCode = E_STOP_ERROR;

        {
            synchronized (_receiveListenerListLock) {
                int listSize = _receiveListenerList.size();
                for (int j = 0; j < listSize; j++) {
                    ILcbCommReceive listener = _receiveListenerList.get(j);

                    if (listener != null)
                        listener.getLcbErrorCodeHandler(intLcbErrorCode);
                }
            }
        }

        if (intLcbErrorCode != 0x0000) {
            //add LCB lib ErrorCode to store
            _errorCodeRecordList.addErrorCodeRecordItem(intLcbErrorCode);

            //callback errorCodeRecordList to UI
            int errorRecordCounter = _errorCodeRecordList.getErrorRecordCounter();
            if (errorRecordCounter != 0) {
                for (int i = 0; i < errorRecordCounter; i++) {
                    synchronized (_receiveListenerListLock) {
                        int rtnUIErrorCode = _errorCodeRecordList.getErrorCodeRecord();
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listenerList = _receiveListenerList.get(j);

                            if (listenerList != null)
                                listenerList.returnErrorCodeToUIHandler(rtnUIErrorCode);
                        }
                    }
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procEStopMcbErrorHandler()
     * Description    : process E-Stopp error Mcb can not send 0x02B2 then send 0x024F handler
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procEStopMcbErrorHandler() {
        int intLcbErrorCode = E_STOP_MCB_ERROR;

        {
            synchronized (_receiveListenerListLock) {
                int listSize = _receiveListenerList.size();
                for (int j = 0; j < listSize; j++) {
                    ILcbCommReceive listener = _receiveListenerList.get(j);

                    if (listener != null)
                        listener.getLcbErrorCodeHandler(intLcbErrorCode);
                }
            }
        }

        if (intLcbErrorCode != 0x0000) {
            //add LCB lib ErrorCode to store
            _errorCodeRecordList.addErrorCodeRecordItem(intLcbErrorCode);

            //callback errorCodeRecordList to UI
            int errorRecordCounter = _errorCodeRecordList.getErrorRecordCounter();
            if (errorRecordCounter != 0) {
                for (int i = 0; i < errorRecordCounter; i++) {
                    synchronized (_receiveListenerListLock) {
                        int rtnUIErrorCode = _errorCodeRecordList.getErrorCodeRecord();
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listenerList = _receiveListenerList.get(j);

                            if (listenerList != null)
                                listenerList.returnErrorCodeToUIHandler(rtnUIErrorCode);
                        }
                    }
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procEStopUnlockHandler()
     * Description    : process E-Stopp unlock and initial lcb finish then send 0x02B1 handler
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procEStopUnlockHandler() {
        int intLcbErrorCode = E_STOP_UNLOCK;

        {
            synchronized (_receiveListenerListLock) {
                int listSize = _receiveListenerList.size();
                for (int j = 0; j < listSize; j++) {
                    ILcbCommReceive listener = _receiveListenerList.get(j);

                    if (listener != null)
                        listener.getLcbErrorCodeHandler(intLcbErrorCode);
                }
            }
        }

        if (intLcbErrorCode != 0x0000) {
            //add LCB lib ErrorCode to store
            _errorCodeRecordList.addErrorCodeRecordItem(intLcbErrorCode);

            //callback errorCodeRecordList to UI
            int errorRecordCounter = _errorCodeRecordList.getErrorRecordCounter();
            if (errorRecordCounter != 0) {
                for (int i = 0; i < errorRecordCounter; i++) {
                    synchronized (_receiveListenerListLock) {
                        int rtnUIErrorCode = _errorCodeRecordList.getErrorCodeRecord();
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listenerList = _receiveListenerList.get(j);

                            if (listenerList != null)
                                listenerList.returnErrorCodeToUIHandler(rtnUIErrorCode);
                        }
                    }
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procTmNoRpmErrorHandler()
     * Description    : process If UCB not get RPM from LCB when already set RPM to LCB within 40 seconds.Send 00A1 error
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procTmNoRpmErrorHandler() {
        int intLcbErrorCode = TM_NO_RPM_RESPONSE;

        {
            synchronized (_receiveListenerListLock) {
                int listSize = _receiveListenerList.size();
                for (int j = 0; j < listSize; j++) {
                    ILcbCommReceive listener = _receiveListenerList.get(j);

                    if (listener != null)
                        listener.getLcbErrorCodeHandler(intLcbErrorCode);
                }
            }
        }

        if (intLcbErrorCode != 0x0000) {
            //add LCB lib ErrorCode to store
            _errorCodeRecordList.addErrorCodeRecordItem(intLcbErrorCode);

            //callback errorCodeRecordList to UI
            int errorRecordCounter = _errorCodeRecordList.getErrorRecordCounter();
            if (errorRecordCounter != 0) {
                for (int i = 0; i < errorRecordCounter; i++) {
                    synchronized (_receiveListenerListLock) {
                        int rtnUIErrorCode = _errorCodeRecordList.getErrorCodeRecord();
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listenerList = _receiveListenerList.get(j);

                            if (listenerList != null)
                                listenerList.returnErrorCodeToUIHandler(rtnUIErrorCode);
                        }
                    }
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbVersionHandler()
     * Description    : process get LCB version handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbVersionHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_VERSION) {
            Log.v(TAG, "Data LCB version received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortLcbVersion = byteBuffer.getShort();
        int intLcbVersion = DataUtils.shortToInt(shortLcbVersion);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbVersionHandler(intLcbVersion);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbCalibrateHandler()
     * Description    : process set LCB Calibrate handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbCalibrateHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.setLcbCalibrateHandler();
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbUpdateProgramHandler()
     * Description    : process set LCB  Update program command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbUpdateProgramHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_UPDATE_PROGRAM) {
            Log.v(TAG, "Data Set UpdateProgram received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Byte byteRsp = byteBuffer.get();
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbUpdateProgramHandler(byteRsp);
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbSkipCurrErrorHandler()
     * Description    : process set LCB SkipCurrError handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbSkipCurrErrorHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.setLcbSkipCurrErrorHandler();
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbSpeExtCmdHandler()
     * Description    : process set LCB  Special extend command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbSpeExtCmdHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_SPE_EXT_CMD) {
            Log.v(TAG, "Data Set SpeExtCmd received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Byte byteCmd = byteBuffer.get();
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbSpeExtCmdHandler(byteCmd);
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbDciVersionHandler()
     * Description    : process get LCB DCI Version handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbDciVersionHandler(LcbPacket packet) {
        byte[] data = packet.getData();
        if (data.length != Constants.RECEIVE_DATA_SIZE_DCI_VERSION) {
            Log.v(TAG, "Data DCI Version received not match definition size !!");
            return;
        }

        //set data
        byte[] getData = new byte[Constants.RECEIVE_DATA_SIZE_DCI_VERSION]; //MID:1 PID:1 Interface_Soft_Ver: 4 Interface_Soft_CRC: 4
        //Motor_Model: 5 Motor_Soft_Ver: 4 Motor_Soft_CRC: 8
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (int i = 0; i < Constants.RECEIVE_DATA_SIZE_DCI_VERSION; i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbDciVersionHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procGetLcbDciEnvHandler()
     * Description    : process get LCB DCI Env handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbDciEnvHandler(LcbPacket packet) {
        byte[] data = packet.getData();
        if (data.length != Constants.RECEIVE_DATA_SIZE_DCI_ENV) {
            Log.v(TAG, "Data DCI Environment received not match definition size !!");
            return;
        }

        //set data
        byte[] getData = new byte[Constants.RECEIVE_DATA_SIZE_DCI_ENV]; //42 words (16 bit), 84 bytes of environment data for troubleshooting errors in the field
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (int i = 0; i < Constants.RECEIVE_DATA_SIZE_DCI_ENV; i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbDciEnvHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbCmdConsolePowerHandler()
     * Description    : process set LCB  CmdConsolePower command (Set the console power to LCB)
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbCmdConsolePowerHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbCmdConsolePowerHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbProtocolVersionHandler()
     * Description    : process get protocol version handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbProtocolVersionHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_PROTOCOL_VERSION) {
            Log.v(TAG, "Data protocol version received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortProtocolVersion = byteBuffer.getShort();
        int intProtocolVersion = DataUtils.shortToInt(shortProtocolVersion);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbProtocolVersionHandler(intProtocolVersion);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbCmdSerialNumberHandler()
     * Description    : process get protocol version handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbCmdSerialNumberHandler(LcbPacket packet) {
        byte[] data = packet.getData();
        //set data
        byte[] getData = new byte[data.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (int i = 0; i < data.length; i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.setLcbCmdSerialNumberHandler(getData);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbTreadmillOperateHandler()
     * Description    : process Set TreadmillOperate handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbTreadmillOperateHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_TREADMILL_OPERATE) {
            Log.e(TAG, "Treadmill operate data does not match size correctly!!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        TreadmillOperateRxData rxData = new TreadmillOperateRxData();
        rxData.currentError = byteBuffer.getShort();
        rxData.motorTorque = byteBuffer.getShort();
        rxData.actualMotorSpeed = byteBuffer.getShort();
        rxData.actualInclinePos = byteBuffer.getShort();
        rxData.motorVolts = byteBuffer.getShort();
        rxData.motorCurrent = byteBuffer.getShort();
        rxData.status = byteBuffer.getShort();
        byte inUse = byteBuffer.get();
        rxData.treadmillInUse = inUse == (byte) 0xFF; //0xFF: used , 0x55: non used


        synchronized (_receiveListenerListLock) {
            for (ILcbCommReceive listener : _receiveListenerList) {
                if (listener != null) listener.setLcbTreadmillOperateHandler(rxData);
            }
        }
    }	

    /*******************************************************************************
     * Function Name  : procGetLcbTreadmillCadenceHandler()
     * Description    : process Get Cadence handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbTreadmillCadenceHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        synchronized (_receiveListenerListLock) {
            for (ILcbCommReceive listener : _receiveListenerList) {
                if (listener != null) listener.getLcbTreadmillCadenceHandler(data);
            }
        }
    }

    /*******************************************************************************
    * Function Name  : procSetLcbMotorSpeedRtnRPMHandler()
    * Description    : process set LCB  Motor Speed Rtn RPM command
    * Input          : LcbPacket
    * Output         : None
    * Return         : None
    *******************************************************************************/
  public void procSetLcbMotorSpeedRtnRPMHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_MOTOR_SPEED_RTN_RPM) {
            Log.v(TAG, "Data Set MotorSpeedRtnRPM received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortRtnRPM = byteBuffer.getShort();
        int intRtnRPM = DataUtils.shortToInt(shortRtnRPM);

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.setLcbMotorSpeedRtnRPMHandler(intRtnRPM);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbMotorRPMHandler()
     * Description    : process set LCB  Motor RPM command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbMotorRPMHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbMotorRPMHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbIncPWMStepHandler()
     * Description    : process set LCB  Inc PWM Step command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbIncPWMStepHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbIncPWMStepHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbDecPWMStepHandler()
     * Description    : process set LCB  Dec PWM Step command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbDecPWMStepHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbDecPWMStepHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbStopPWMStepHandler()
     * Description    : process set LCB  Stop PWM Step command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbStopPWMStepHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbStopPWMStepHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbInclineActionHandler()
     * Description    : process set Incline Action command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbInclineActionHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbInclineActionHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbInclineLocHandler()
     * Description    : process set LCB  Incline Loc command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbInclineLocHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbInclineLocHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbOperatingStatusHandler()
     * Description    : process set Operating Status command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbOperatingStatusHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbOperatingStatusHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbRollerRPMHandler()
     * Description    : process get roller rpm handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbRollerRPMHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_ROLLER_RPM) {
            Log.v(TAG, "Data roller rpm received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortRollerRPM = byteBuffer.getShort();
        int intRollerRPM = DataUtils.shortToInt(shortRollerRPM);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbRollerRPMHandler(intRollerRPM);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbMotorRPMHandler()
     * Description    : process get motor rpm handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbMotorRPMHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_ROLLER_RPM) {
            Log.v(TAG, "Data roller rpm received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortMotorRPM = byteBuffer.getShort();
        // bit 1-15 indicate the motor RPM
        int intMotorRPM = shortMotorRPM & 0x7FFF;
        // bit 16 indicate the forward/reverse direction of motor RPM, 0: forward, 1: reverse
        boolean isReverse = (shortMotorRPM & 0x8000) != 0;
        //Log.v(TAG, "procGetLcbMotorRPMHandler: int=" + DataUtils.shortToInt(shortMotorRPM) + " isReverse=" + isReverse + " rpm=" + intMotorRPM );

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbMotorRPMHandler(intMotorRPM);
            }
        }
    }

    public void procGetOneShotLcbMotorRpmHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_ROLLER_RPM) {
            Log.v(TAG, "Data roller rpm received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortMotorRPM = byteBuffer.getShort();
        int intMotorRPM = DataUtils.shortToInt(shortMotorRPM);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getOneShotLcbMotorRpmHandler(intMotorRPM);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbInclineLocHandler()
     * Description    : process get Incline Loc handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbInclineLocHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_INCLINE_LOC) {
            Log.v(TAG, "Data incline location received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortInclineLoc = byteBuffer.getShort();
        int intInclineLoc = DataUtils.shortToInt(shortInclineLoc);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbInclineLocHandler(intInclineLoc);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbTuneEndInclineHandler()
     * Description    : process set LCB  Tune EndPoint Incline command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbTuneEndInclineHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbTuneEndInclineHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbTuneEndIncline2Handler()
     * Description    : process set LCB  Tune End Point Incline 2 command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbTuneEndIncline2Handler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbTuneEndIncline2Handler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbInclineStrokeHandler()
     * Description    : process set LCB  Incline Stroke command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbInclineStrokeHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbInclineStrokeHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbCompensationVolHandler()
     * Description    : process set LCB  Compensation Vol command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbCompensationVolHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_COMPENSATION_VOL) {
            Log.v(TAG, "Data Set Compensation Vol received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortRtnVol = byteBuffer.getShort();
        int intRtnVol = DataUtils.shortToInt(shortRtnVol);

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.setLcbCompensationVolHandler(intRtnVol);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbMotorSpecificationHandler()
     * Description    : process set LCB  Motor Specification command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbMotorSpecificationHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbMotorSpecificationHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbMotorTypeHandler()
     * Description    : process get MotorType handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbMotorTypeHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_MOTOR_TYPE) {
            Log.v(TAG, "Data Motor Type received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Byte byteMotorType = byteBuffer.get();


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbMotorTypeHandler(byteMotorType);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbDCIdataMaxLenHandler()
     * Description    : process get DCI data Max Len handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbDCIdataMaxLenHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_DCI_DATA_MAX_LEN) {
            Log.v(TAG, "Data DCI data Max Len received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Byte byteDCIdataMaxLen = byteBuffer.get();


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbDCIdataMaxLenHandler(byteDCIdataMaxLen);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbMotorPowerOffHandler()
     * Description    : process set Motor Power Off command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbMotorPowerOffHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbMotorPowerOffHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbDCIforceInclineHandler()
     * Description    : process set DCI force Incline command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbDCIforceInclineHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbDCIforceInclineHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbTreadmillInUsedHandler()
     * Description    : process get Treadmill In Used handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbTreadmillInUsedHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_TREADMILL_IN_USED) {
            Log.v(TAG, "Data Treadmill In Used received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Byte byteTreadmillInUsed = byteBuffer.get();


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbTreadmillInUsedHandler(byteTreadmillInUsed);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbMainMotorInfoHandler()
     * Description    : process get LCB Main Motor Info handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbMainMotorInfoHandler(LcbPacket packet) {
        byte[] data = packet.getData();

/*        if (data.length != Constants.RECEIVE_DATA_SIZE_MAIN_MOTOR_INFO) {
            Log.v(TAG, "Data Main Motor Info received not match definition size !!");
            return;
        }

	//set data
	byte[] getData = new byte[Constants.RECEIVE_DATA_SIZE_MAIN_MOTOR_INFO]; // 12 bytes of  info ,Byte0~1:Output Freq(0.01Hz) Byte2~3:Output Current(0.1A) Byte4~5:Output Volt(0.1V)
																		//Byte6~7:DC Bus Volt(0.1V) Byte8~9:IGBT Temp(0.1C) Byte10~11:Electromagnet Temp(0.1C)
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

	for(int i=0;i<Constants.RECEIVE_DATA_SIZE_MAIN_MOTOR_INFO;i++){   
	   getData[i] = byteBuffer.get();
	}
*/
	//set data
	byte[] getData = new byte[data.length]; 
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

	for(int i=0;i<data.length;i++){   
	   getData[i] = byteBuffer.get();
	}
	


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbMainMotorInfoHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procGetLcbTreadmillExtStatusHandler()
     * Description    : process Get the status of the LCB for Treadmill Ext (LCB Type 0xC6 0xC7 ) command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbTreadmillExtStatusHandler(LcbPacket packet) {
        byte[] data = packet.getData();

                if (data.length != Constants.RECEIVE_DATA_SIZE_TREADMILL_EXT_STATUS) {
                    Log.v(TAG, "Data Treadmill Ext Status received not match definition size !!");
                    return;
                }

                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.BIG_ENDIAN);

                Short shortTreadmillExtStatus = byteBuffer.getShort();
                int intTreadmillExtStatus = DataUtils.shortToInt(shortTreadmillExtStatus);


                synchronized (_receiveListenerListLock) {
                    int listSize = _receiveListenerList.size();
                    for (int j = 0; j < listSize; j++) {
                        ILcbCommReceive listener = _receiveListenerList.get(j);

                        if (listener != null)
                            listener.getLcbTreadmillExtStatusHandler(intTreadmillExtStatus);
                    }
                }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbDCIinclineRangeHandler()
     * Description    : process get DCI Incline Range
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbDCIinclineRangeHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_DCI_INCLINE_RANGE) {
            Log.v(TAG, "Data DCI Incline Range received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        int intInclineRange = byteBuffer.getInt();

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbDCIinclineRangeHandler(intInclineRange);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbDCIinclineRangeHandler()
     * Description    : process set DCI Incline Range
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbDCIinclineRangeHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbDCIinclineRangeHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbDCIrpmSpeedRangeHandler()
     * Description    : process get DCI Rpm Speed Range
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbDCIrpmSpeedRangeHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_DCI_RPM_SPEED_RANGE) {
            Log.v(TAG, "Data DCI Rrp Speed Range received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byte[] rpmSpeedRange = new byte[4];

        for (int i = 0; i < 4; i++) {
            rpmSpeedRange[i] = byteBuffer.get();
        }
        int intRpmSpeedRange = DataUtils.unsignedBytesToInt((byte) rpmSpeedRange[0], (byte) rpmSpeedRange[1], (byte) rpmSpeedRange[2], (byte) rpmSpeedRange[3]);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbDCIrpmSpeedRangeHandler(intRpmSpeedRange);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbDCIrpmSpeedRangeHandler()
     * Description    : process set DCI Rpm Speed Range
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbDCIrpmSpeedRangeHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbDCIrpmSpeedRangeHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbUniversalHandler()
     * Description    : process set LCB  Universal command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbUniversalHandler(LcbPacket packet) {
        byte[] data = packet.getData();
        //set data
        byte[] getData = new byte[data.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

	if(data.length != 0){
	        for (int i = 0; i < data.length; i++) {
	            getData[i] = byteBuffer.get();
	        }

		if(data.length == 3){
			//callback intRtnValue
		        int intRtnValue = DataUtils.unsignedBytesToInt(getData[1],getData[2]);
		        synchronized (_receiveListenerListLock) {
		            int listSize = _receiveListenerList.size();
		            for (int j = 0; j < listSize; j++) {
		                ILcbCommReceive listener = _receiveListenerList.get(j);

		                if (listener != null)
		                    listener.setLcbUniversalHandler(intRtnValue);
		            }
		        }
		}
	        mUniDataProcReceived.analyzeUniData(getData);
	}else{
            Log.i(TAG, "Set Lcb Universal received no data !!");
	}

    }

    /*******************************************************************************
     * Function Name  : procSetLcbDCIManualCalibrationHandler()
     * Description    : process set DCI Manual Calibration
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbDCIManualCalibrationHandler(LcbPacket packet) {


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.setLcbDCIManualCalibrationHandler();
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbDeviceDataHandler()
     * Description    : process set LCB Device Data handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbDeviceDataHandler(LcbPacket packet) {
        byte[] data = packet.getData();
        //set data
        byte[] getData = new byte[data.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

	if(data.length != 0){
        for (int i = 0; i < data.length; i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.setLcbDeviceDataHandler(getData);
            }
        }

        mDeviceDataProcReceived.analyzeDeviceData(getData);
	}else{
            Log.i(TAG, "Set Lcb Device Data received no data !!");
	}
		
    }

    /*******************************************************************************
     * Function Name  : procSetLcbResetECBHandler()
     * Description    : process set Reset ECB
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbResetECBHandler(LcbPacket packet) {


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.setLcbResetECBHandler();
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procSetLcbECBpositionHandler()
     * Description    : process set LCB  ECB position command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbECBpositionHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbECBpositionHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbECBrpmHandler()
     * Description    : process get ECB rpm
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbECBrpmHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_ECB_RPM) {
            Log.v(TAG, "Data ECB rpm received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortECBrpm = byteBuffer.getShort();
        int intECBrpm = DataUtils.shortToInt(shortECBrpm);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbECBrpmHandler(intECBrpm);
            }
        }

    }

    public void procGetOneShotLcbECBrpmHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_ECB_RPM) {
            Log.v(TAG, "Data ECB rpm received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortECBrpm = byteBuffer.getShort();
        int intECBrpm = DataUtils.shortToInt(shortECBrpm);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getOneShotLcbECBrpmHandler(intECBrpm);
            }
        }
    }			

    /*******************************************************************************
    * Function Name  : procGetLcbIndoorBikeSensorDataHandler()
    * Description    : process get IndoorBike SensorData
    * Input          : LcbPacket
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void procGetLcbIndoorBikeSensorDataHandler(LcbPacket packet) {
        byte[] data = packet.getData();
        // 0x51 payload is variable length. Forward raw bytes to every listener; each
        // machineHandler package (IndoorBike, TcvFxSpinBike) filters on data[0] and
        // parses into its own typed struct.
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);
                if (listener != null) listener.getLcbIndoorBikeSensorDataHandler(data);
            }
        }
    }

    public void procGetOneShotLcbIndoorBikeSensorDataHandler(LcbPacket packet) {
        byte[] data = packet.getData();
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);
                if (listener != null) listener.getOneShotLcbIndoorBikeSensorDataHandler(data);
            }
        }
    }

    /*******************************************************************************
    * Function Name  : procSetLcbIndoorBikeStepperMotorPositionHandler()
    * Description    : process set Stepper Motor Position
    * Input          : LcbPacket
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void procSetLcbIndoorBikeStepperMotorPositionHandler(LcbPacket packet) {


	synchronized (_receiveListenerListLock) {
		int listSize = _receiveListenerList.size();
		for (int j = 0 ; j < listSize ; j++) {
			 ILcbCommReceive listener = _receiveListenerList.get(j);

	              if (listener != null)
	                  listener.setLcbIndoorBikeStepperMotorPositionHandler();
		}
	}
    }			

    /*******************************************************************************
    * Function Name  : procSetLcbIndoorBikeCalcParaOfDeviceHandler()
    * Description    : process set Calc Para Of Device
    * Input          : LcbPacket
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void procSetLcbIndoorBikeCalcParaOfDeviceHandler(LcbPacket packet) {


	synchronized (_receiveListenerListLock) {
		int listSize = _receiveListenerList.size();
		for (int j = 0 ; j < listSize ; j++) {
			 ILcbCommReceive listener = _receiveListenerList.get(j);

	              if (listener != null)
	                  listener.setLcbIndoorBikeCalcParaOfDeviceHandler();
		}
	}
    }			

    public void procGetLcbIndoorBikeCalcParaOfDeviceHandler(LcbPacket packet) {
        byte[] data = packet.getData();
	byte[] getData = new byte[data.length]; 

/*        if (data.length != Constants.RECEIVE_DATA_SIZE_INDOORBIKE_CALC_PARA_OF_DEVICE) {
            Log.v(TAG, "Data IndoorBike SensorData received not match definition size !!");
            return;
        }
*/
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

	//get  calculation parameters of device or algorithm
	for(int i=0;i<data.length;i++){   
	   getData[i] = byteBuffer.get();
	}

	synchronized (_receiveListenerListLock) {
		int listSize = _receiveListenerList.size();
		for (int j = 0 ; j < listSize ; j++) {
			 ILcbCommReceive listener = _receiveListenerList.get(j);

	              if (listener != null){
	                  listener.getLcbIndoorBikeCalcParaOfDeviceHandler(getData);
	              	}
		}
	}
			
    }
    /*******************************************************************************
    * Function Name  : procSetLcbIndoorBikeActionSpinDownHandler()
    * Description    : process set LCB the action of spin down handler
    * Input          : LcbPacket
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void procSetLcbIndoorBikeActionSpinDownHandler(LcbPacket packet) {
        byte[] data = packet.getData();
	//set data
	byte[] getData = new byte[data.length]; 

/*        if (data.length != Constants.RECEIVE_DATA_SIZE_INDOORBIKE_ACTION_SPIN_DOWN) {
            Log.v(TAG, "Data IndoorBike ActionSpinDown received not match definition size !!");
            return;
        }
*/
	
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

	for(int i=0;i<data.length;i++){   
	   getData[i] = byteBuffer.get();
	}

	synchronized (_receiveListenerListLock) {
		int listSize = _receiveListenerList.size();
		for (int j = 0 ; j < listSize ; j++) {
			 ILcbCommReceive listener = _receiveListenerList.get(j);

	              if (listener != null)
	                  listener.setLcbIndoorBikeActionSpinDownHandler(getData);
		}
	}

    }		

    /*******************************************************************************
    * Function Name  : procGetLcbIndoorBikeInfoSpinDownHandler()
    * Description    : process get Spin Down info
    * Input          : LcbPacket
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void procGetLcbIndoorBikeInfoSpinDownHandler(LcbPacket packet) {
/*        byte[] data = packet.getData();
	byte[] getData = new byte[data.length]; 

//*        if (data.length != Constants.RECEIVE_DATA_SIZE_INDOORBIKE_INFO_SPIN_DOWN) {
            Log.v(TAG, "Data IndoorBike InfoSpinDown received not match definition size !!");
            return;
        }
*
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

	//get the info of spin down
	for(int i=0;i<data.length;i++){   
	   getData[i] = byteBuffer.get();
	}

	synchronized (_receiveListenerListLock) {
		int listSize = _receiveListenerList.size();
		for (int j = 0 ; j < listSize ; j++) {
			 ILcbCommReceive listener = _receiveListenerList.get(j);

	              if (listener != null){
	                  listener.getLcbIndoorBikeInfoSpinDownHandler(getData);
	              	}
		}
	}
*/
        byte[] data = packet.getData();
	byte[] getData = new byte[4]; 

/*        if (data.length != Constants.RECEIVE_DATA_SIZE_INDOORBIKE_INFO_SPIN_DOWN) {
            Log.v(TAG, "Data IndoorBike InfoSpinDown received not match definition size !!");
            return;
        }
*/
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

	//get SpinDown status
        Byte byteStatus = byteBuffer.get();
        _indoorBikeInfoSpinDown.status= DataUtils.unsignedByteToInt(byteStatus);

	//get Flywheel rotation
        Short shortFlywheelRotation = byteBuffer.getShort();
        _indoorBikeInfoSpinDown.flywheelRotation= DataUtils.shortToInt(shortFlywheelRotation);

	//get  torque scalar
        Float floatTorqueScalar = byteBuffer.getFloat();
        _indoorBikeInfoSpinDown.torqueScalar= floatTorqueScalar;

	//get  correction  factor
        Float floatCorrectionFactor = byteBuffer.getFloat();
        _indoorBikeInfoSpinDown.correctionFactor= floatCorrectionFactor;

	//get  inertia
        Float floatInertia = byteBuffer.getFloat();
        _indoorBikeInfoSpinDown.inertia= floatInertia;

	synchronized (_receiveListenerListLock) {
		int listSize = _receiveListenerList.size();
		for (int j = 0 ; j < listSize ; j++) {
			 ILcbCommReceive listener = _receiveListenerList.get(j);

	              if (listener != null)
	                  listener.getLcbIndoorBikeInfoSpinDownHandler(_indoorBikeInfoSpinDown);
		}
	}
			
    }	

    /*******************************************************************************
    * Function Name  : procSetLcbIndoorBikeErgModeSettingsHandler()
    * Description    : process set LCB Erg Mode Settings handler
    * Input          : LcbPacket
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void procSetLcbIndoorBikeErgModeSettingsHandler(LcbPacket packet) {
        byte[] data = packet.getData();
	//set data
	byte[] getData = new byte[data.length]; 

/*        if (data.length != Constants.RECEIVE_DATA_SIZE_INDOORBIKE_ERG_MODE_SETTINGS) {
            Log.v(TAG, "Data IndoorBike ErgModeSettings received not match definition size !!");
            return;
        }
*/
	
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

	for(int i=0;i<data.length;i++){   
	   getData[i] = byteBuffer.get();
	}

	synchronized (_receiveListenerListLock) {
		int listSize = _receiveListenerList.size();
		for (int j = 0 ; j < listSize ; j++) {
			 ILcbCommReceive listener = _receiveListenerList.get(j);

	              if (listener != null)
	                  listener.setLcbIndoorBikeErgModeSettingsHandler(getData);
		}
	}

    }		

    /*******************************************************************************
    * Function Name  : procGetLcbIndoorBikeErgModeSettingsHandler()
    * Description    : process get Erg Mode Settings
    * Input          : LcbPacket
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void procGetLcbIndoorBikeErgModeSettingsHandler(LcbPacket packet) {
        byte[] data = packet.getData();
	byte[] getData = new byte[data.length]; 

/*        if (data.length != Constants.RECEIVE_DATA_SIZE_INDOORBIKE_ERG_MODE_SETTINGS) {
            Log.v(TAG, "Data IndoorBike ErgModeSettings received not match definition size !!");
            return;
        }
*/
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

	//get the info of Erg Mode Settings
	for(int i=0;i<data.length;i++){   
	   getData[i] = byteBuffer.get();
	}

	synchronized (_receiveListenerListLock) {
		int listSize = _receiveListenerList.size();
		for (int j = 0 ; j < listSize ; j++) {
			 ILcbCommReceive listener = _receiveListenerList.get(j);

	              if (listener != null){
	                  listener.getLcbIndoorBikeErgModeSettingsHandler(getData);
	              	}
		}
	}
			
    }	

    /*******************************************************************************
    * Function Name  : procSetLcbIndoorBikeResistanceLevelHandler()
    * Description    : process set Resistance Level
    * Input          : LcbPacket
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void procSetLcbIndoorBikeResistanceLevelHandler(LcbPacket packet) {
        synchronized (_receiveListenerListLock) {
		int listSize = _receiveListenerList.size();
		for (int j = 0 ; j < listSize ; j++) {
			 ILcbCommReceive listener = _receiveListenerList.get(j);

	              if (listener != null)
	                  listener.setLcbIndoorBikeResistanceLevelHandler();
		}
	}
    }			

    /*******************************************************************************
    * Function Name  : procGetLcbECBadcHandler()
    * Description    : process get ECB adc
    * Input          : LcbPacket
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void procGetLcbECBadcHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_ECB_ADC) {
            Log.v(TAG, "Data ECB adc received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortECBadc = byteBuffer.getShort();
        int intECBadc = DataUtils.shortToInt(shortECBadc);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbECBadcHandler(intECBadc);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbPWMpercentHandler()
     * Description    : process set LCB  PWM percent command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbPWMpercentHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbPWMpercentHandler();
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbBatteryVoltageHandler()
     * Description    : process get battery voltage
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbBatteryVoltageHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_BATTERY_VOLTAGE) {
            Log.v(TAG, "Data battery voltage received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //self power only use battery voltage , protocol doc have error. Low byte is battery vol and high byte is generator vol.
        Byte byteGeneratorVoltage = byteBuffer.get();
        int intGeneratorVoltage = DataUtils.unsignedByteToInt(byteGeneratorVoltage);

        Byte byteBatteryVoltage = byteBuffer.get();
        int intBatteryVoltage = DataUtils.unsignedByteToInt(byteBatteryVoltage);

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbBatteryVoltageHandler(intBatteryVoltage);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbWattsHandler()
     * Description    : process set LCB  watts command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbWattsHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbWattsHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbRPMgearRatioHandler()
     * Description    : process set LCB  RPM gear ratio command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbRPMgearRatioHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbRPMgearRatioHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbGeneratorPolePairHandler()
     * Description    : process set the generator pole pair command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbGeneratorPolePairHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbGeneratorPolePairHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbLimitRPMchargeHandler()
     * Description    : process set LCB  Set limit RPM for charge command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbLimitRPMchargeHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbLimitRPMchargeHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbLimitRPMresistanceHandler()
     * Description    : process set LCB  Set limit RPM for resistance command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbLimitRPMresistanceHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbLimitRPMresistanceHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbMachineTypeHandler()
     * Description    : process set Set machine type command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbMachineTypeHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbMachineTypeHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbPowerOffHandler()
     * Description    : process Set power off (minute) command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbPowerOffHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbPowerOffHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbBatteryChargeLevelHandler()
     * Description    : process Set the battery charge level (%) command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbBatteryChargeLevelHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbBatteryChargeLevelHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbMinResistanceInclineWorkingHandler()
     * Description    : process Set min resistance when incline working command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbMinResistanceInclineWorkingHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbMinResistanceInclineWorkingHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbInclinePercentHandler()
     * Description    : process set LCB  Set incline travel speed ( 0: 0% 32768: 100% ) command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbInclinePercentHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbInclinePercentHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procGetLcbInclinePercentHandler()
     * Description    : process get incline travel speed ( 0: 0% 32768: 100% )
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbInclinePercentHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_INCLINE_PERCENT) {
            Log.v(TAG, "Data incline percent received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortInclinePercent = byteBuffer.getShort();
        int intInclinePercent = DataUtils.shortToInt(shortInclinePercent);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbInclinePercentHandler(intInclinePercent);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbECBactionHandler()
     * Description    : process Set ECB action Data Values (0x01: upward , 0x80: downward , 0x11: stop) command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbECBactionHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbECBactionHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procGetLcbECBstatusHandler()
     * Description    : process get ECB status handler command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbECBstatusHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_ECB_STATUS) {
            Log.v(TAG, "Data ECB status received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Byte byteECBstatus = byteBuffer.get();


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbECBstatusHandler(byteECBstatus);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetLcbECBcountHandler()
     * Description    : process get ECB count ( 0 ~ 65535 ) command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbECBcountHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_ECB_COUNT) {
            Log.v(TAG, "Data ECB count received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortECBcount = byteBuffer.getShort();
        int intECBcount = DataUtils.shortToInt(shortECBcount);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbECBcountHandler(intECBcount);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbElectromagneticCurrentHandler()
     * Description    : process set LCB  Electromagnetic Current ( mA ) command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbElectromagneticCurrentHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbElectromagneticCurrentHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbResistanceTypeHandler()
     * Description    : process set LCB  Resistance Type (0.1ohm)command
     * b15 ~ b13 : load type b12 ~ b0 : resistance 000: electromagnet exp. 0x123 = 29.1  001: resistor
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbResistanceTypeHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbResistanceTypeHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbBeginBatteryChargeHandler()
     * Description    : process set LCB  Set the battery charge current of no resistance 0 ~ 1000 (mA)command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbBeginBatteryChargeHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbBeginBatteryChargeHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procGetLcbBatteryCapacityHandler()
     * Description    : process Get battery the percentage of capacity ( 0 ~ 1000 mA ) command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbBatteryCapacityHandler(LcbPacket packet) {
        byte[] data = packet.getData();


        if (data.length != Constants.RECEIVE_DATA_SIZE_BATTERY_CAPACITY) {
            Log.v(TAG, "Data Battery Capacity received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortBatteryCapacity = byteBuffer.getShort();
        int intBatteryCapacity = DataUtils.shortToInt(shortBatteryCapacity);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbBatteryCapacityHandler(intBatteryCapacity);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbClimbmillStatusHandler()
     * Description    : process Set Set the action (include E-STOP) of the LCB for climbmill. command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbClimbmillStatusHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbClimbmillStatusHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procGetLcbClimbmillStatusHandler()
     * Description    : process Get Get the status of the LCB for climbmill (LCB Type 0x17 before is return 1 byte) command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbClimbmillStatusHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        mGeneralHandler = GeneralHandler.getInstance();
        if (mGeneralHandler != null)
            machineLcbType = mGeneralHandler.getMachineLcbType();


        switch (machineLcbType) {
            case LCB_ClimbMill_or_A30: {
                if (data.length != Constants.RECEIVE_DATA_SIZE_CLIMBMILL_STATUS) {
                    Log.v(TAG, "Data Climbmill Status received not match definition size !!");
                    return;
                }

                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.BIG_ENDIAN);

                Byte byteCmd = byteBuffer.get();
                int intClimbmillStatus = DataUtils.unsignedByteToInt(byteCmd);


                synchronized (_receiveListenerListLock) {
                    int listSize = _receiveListenerList.size();
                    for (int j = 0; j < listSize; j++) {
                        ILcbCommReceive listener = _receiveListenerList.get(j);

                        if (listener != null)
                            listener.getLcbClimbmillStatusHandler(intClimbmillStatus);
                    }
                }
            }
            break;
            case LCB_Commercial_Climbmill: 
            case LCB_Retail_Climbmill: 
            case LCB_Performance_Climbmill_DC36V:
	        {
                if (data.length != Constants.RECEIVE_DATA_SIZE_ENCLIMBMILL_STATUS) {
                    Log.v(TAG, "Data EnClimbmill Status received not match definition size !!");
                    return;
                }

                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                byteBuffer.order(ByteOrder.BIG_ENDIAN);

                Short shortClimbmillStatus = byteBuffer.getShort();
                int intEnClimbmillStatus = DataUtils.shortToInt(shortClimbmillStatus);


                synchronized (_receiveListenerListLock) {
                    int listSize = _receiveListenerList.size();
                    for (int j = 0; j < listSize; j++) {
                        ILcbCommReceive listener = _receiveListenerList.get(j);

                        if (listener != null)
                            listener.getLcbClimbmillStatusHandler(intEnClimbmillStatus);
                    }
                }
            }
            break;
            default:
                return;
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbEStopParameterHandler()
     * Description    : process Set the E-STOP parameter. command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbEStopParameterHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbEStopParameterHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procGetLcbBrakePressureValueHandler()
     * Description    : process Get brake pressure value (0~65535) command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbBrakePressureValueHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_BRAKE_PRESSURE_VALUE) {
            Log.v(TAG, "Data brake pressure value received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortBrakePressureValue = byteBuffer.getShort();
        int intBrakePressureValue = DataUtils.shortToInt(shortBrakePressureValue);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbBrakePressureValueHandler(intBrakePressureValue);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procGetLcbWattsHandler()
     * Description    : process Get Watts (0~65535) command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbWattsHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_WATTS) {
            Log.v(TAG, "Data Watts received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortWatts = byteBuffer.getShort();
        int intWatts = DataUtils.shortToInt(shortWatts);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.getLcbWattsHandler(intWatts);
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbClimbmillCalHandler()
     * Description    : process set LCB ClimbmillCal command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbClimbmillCalHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbClimbmillCalHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbRecordCurrentHandler()
     * Description    : process set LCB RecordCurrent command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbRecordCurrentHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbRecordCurrentHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbPWMHandler()
     * Description    : process set LCB  Set PWM 0 - 1000(0.0% -100.0%) command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbPWMHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbPWMHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbFinishCalibratingHandler()
     * Description    : process Set finished calibrating one point. command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbFinishCalibratingHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbFinishCalibratingHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbSPMHandler()
     * Description    : process set LCB  Set SPM 0.1SPM command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbSPMHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbSPMHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procGetLcbSPMHandler()
     * Description    : process Get SPM 0.1 SPM command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbSPMHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_SPM) {
            Log.v(TAG, "Data SPM received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortSPM = byteBuffer.getShort();
        int intSPM = DataUtils.shortToInt(shortSPM);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbSPMHandler(intSPM);
            }
        }

    }

    public void procGetOneShotLcbSpmHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_SPM) {
            Log.v(TAG, "Data SPM received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortSPM = byteBuffer.getShort();
        int intSPM = DataUtils.shortToInt(shortSPM);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);
                if (listener != null)
                    listener.getOneShotLcbSpmHandler(intSPM);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbCalScalingHandler()
     * Description    : process set LCB  Set CalScaling 0.1% command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbCalScalingHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbCalScalingHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procGetLcbCalScalingHandler()
     * Description    : process Get CalScaling 0.1% command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbCalScalingHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_CAL_SCALING) {
            Log.v(TAG, "Data CalScaling received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortCalScaling = byteBuffer.getShort();
        int intCalScaling = DataUtils.shortToInt(shortCalScaling);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbCalScalingHandler(intCalScaling);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbFrameCalScalingHandler()
     * Description    : process set LCB  Set FrameCalScaling 0.1% command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbFrameCalScalingHandler(LcbPacket packet) {

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbFrameCalScalingHandler();
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procGetLcbFrameCalScalingHandler()
     * Description    : process Get FrameCalScaling 0.1% command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetLcbFrameCalScalingHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_FRAME_CAL_SCALING) {
            Log.v(TAG, "Data FrameCalScaling received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortFrameCalScaling = byteBuffer.getShort();
        int intFrameCalScaling = DataUtils.shortToInt(shortFrameCalScaling);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getLcbFrameCalScalingHandler(intFrameCalScaling);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetLcbMachineTypeHandler()
     * Description    : process set Set machine type command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetLcbTreadmillEcbActionHandler(LcbPacket packet) {

         byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_TREADMILL_ECB_ACTION) {
            Log.v(TAG, "Data Set TreadmillEcbAction received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Byte byteData = byteBuffer.get();
        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setLcbTreadmillEcbActionHandler(byteData);
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procSetTreadMillRpmValueHandler()
     * Description    : proc Set TreadMill Rpm Value Handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetTreadMillRpmValueHandler(LcbPacket packet) {

        byte[] data = packet.getData();

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortSetRPM = byteBuffer.getShort();
        int intSetRpm = DataUtils.shortToInt(shortSetRPM);

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setTreadMillRpmValueHandler(intSetRpm);
                }
            }
        }

    }
	
    /*******************************************************************************
     * Function Name  : procSetTreadMillAdcValueHandler()
     * Description    : proc Set TreadMill Adc Value Handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procSetTreadMillAdcValueHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortSetAdc = byteBuffer.getShort();
        int intSetAdc = DataUtils.shortToInt(shortSetAdc);

        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.setTreadMillAdcValueHandler(intSetAdc);
                }
            }
        }

    }
	
    /*******************************************************************************
     * Function Name  : procGetIrAddrVersionHandler()
     * Description    : process get Ir Addr Board version handler
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetIrAddrVersionHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_VERSION) {
            Log.v(TAG, "Data Ir Addr version received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Short shortIrAddrVersion = byteBuffer.getShort();
        int intIrAddrVersion = DataUtils.shortToInt(shortIrAddrVersion);


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.getIrAddrVersionHandler(intIrAddrVersion);
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procGetIrAddrIrStatusHandler()
     * Description    : process get Hybrid TM Ir Addr Ir Status
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procGetIrAddrIrStatusHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_HYBRID_TM_IR_ADDR_IR_STATUS) {
            Log.v(TAG, "Data Hybrid TM Ir Addr Ir Status received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Byte byteHybridTMIrAddrIrStatus = byteBuffer.get();


        synchronized (_receiveListenerListLock) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                ILcbCommReceive listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.getIrAddrIrStatusHandler(byteHybridTMIrAddrIrStatus);
            }
        }
    }

}
