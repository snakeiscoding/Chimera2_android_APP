package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMAthena;

import android.util.Log;

import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.BinFileManager;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.ILcbUpdateComm;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.ILcbUpdateCommReceive;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.ILcbUpdateProcessReceive;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.UpdateComm;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.UpdateCommands;
import com.jht.serialport.ISerialPort;

import java.util.ArrayList;

public class CHIMAthenaLcbUpdateFlow implements ILcbUpdateComm,ILcbUpdateCommReceive {
    public final String TAG = "CHIMAthenaLcbUpdateFlow";
    private ArrayList<ILcbUpdateProcessReceive> _receiveProcessListenerList = new ArrayList<>();
    private ILcbUpdateCommReceive mListener;
    private CHIMAthenainfoAddrLen mCHIMAthenainfoAddrLen = new CHIMAthenainfoAddrLen(); 
    public static CHIMAthenaLcbUpdateFlow Instance = new CHIMAthenaLcbUpdateFlow();
    private BinFileManager mBinFileManager = new BinFileManager();
    private String mBinFilePathName;

    private int mRetryUpdateModeCounter = 0;
    private final int UPDATE_MODE_RETRY_TIMES 	=  2;

    /* Password */
    private final byte[] Unlock_Password = {'J', 'o', 'h', 'n', 's', 'o', 'n'};
    private int mRetryFlashUnlockCounter = 0;
    private final int FLASH_UNLOCK_RETRY_TIMES 	=  2;

    private byte[] WriteData = new byte[]{(byte) 0xD0, (byte) 0xD1, (byte) 0xD2, (byte) 0xD3,
                                          (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88,
                                          (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC,
                                          (byte) 0xDF, (byte) 0xEE, (byte) 0xEF};

    //erase flash data used ==>
    private CHIMAthenaEraseFlashDataManager mCHIMAthenaEraseFlashDataManager = new CHIMAthenaEraseFlashDataManager();
    //<==erase flash data used 

    //write flash data used ==>
    private CHIMAthenaWriteFlashDataManager mCHIMAthenaWriteFlashDataManager = new CHIMAthenaWriteFlashDataManager();
    //<==write flash data used 

    //read flash data used ==>
    private CHIMAthenaReadFlashDataManager mCHIMAthenaReadFlashDataManager = new CHIMAthenaReadFlashDataManager();
    //<==read flash data used 

    private static final int LCB_TYPE_Athena = 0x00;

    private final char[] Athena_ProduceID = {'A', 't', 'h','e', 'n', 'a', ' ', 'L', 'C', 'B'};

    //Message show update process what step
    public enum MessageString {
        STR_RTN_MESSAGE_NONE,
        STR_RTN_MESSAGE_UPDATE_INITIAL,
        STR_RTN_MESSAGE_CHECK_BIN_FILE,
        STR_RTN_MESSAGE_ENTER_UPDATE_MODE,
        STR_RTN_MESSAGE_READ_UPDATE_MODE,
        STR_RTN_MESSAGE_FLASH_UNLOCK,
        STR_RTN_MESSAGE_ERASE_FLASH,
        STR_RTN_MESSAGE_WRITE_FLASH,
        STR_RTN_MESSAGE_READ_FLASH_DATA_CHECK,
        STR_RTN_MESSAGE_WRITE_CHECK_CODE,
        STR_RTN_MESSAGE_READ_PROGRAM_STATE,
        STR_RTN_MESSAGE_READ_PRODUCE_ID,
        STR_RTN_MESSAGE_LCB_UPDATE_OK
    }

    //Check bin file LCB info show error string
    public enum CheckLcbInfoError {
        CHECK_INFO_ERROR_NONE,
        CHECK_INFO_ERROR_MCU_TYPE,
        CHECK_INFO_ERROR_MODULE_NAME,
        CHECK_INFO_ERROR_MODULE_NO
    }

    //Update flow send command error show string
    public enum UpdateFlowSendError {
        SEND_ERROR_NONE,
        SEND_ERROR_DATA_LEN_NOT_MATCH,
        SEND_ERROR_CHECK_CODE_DATA_LEN_NOT_MATCH,
        SEND_ERROR_PACKET_NULL,
    }

    //Update flow receive command error show string
    public enum UpdateFlowReceiveError {
        RECEIVE_ERROR_NONE,
        RECEIVE_ERROR_DATA_LEN,
        RECEIVE_ERROR_WRITE_DATA_CRC,
        RECEIVE_ERROR_WRITE_DATA_ADDR,
        RECEIVE_ERROR_READ_DATA_ADDR,
        RECEIVE_ERROR_READ_DATA_LEN,
        RECEIVE_ERROR_READ_DATA_BYTE
    }

    //Update flow fail show string
    public enum UpdateFlowFail {
        UPDATE_FLOW_FAIL_NONE,
        UPDATE_FLOW_FAIL_READ_UPDATE_MODE,
        UPDATE_FLOW_FAIL_FLASH_UNLOCK,
        UPDATE_FLOW_FAIL_ERASE_FLASH,
        UPDATE_FLOW_FAIL_READ_DATA_BYTE_COMPARE,
        UPDATE_FLOW_FAIL_CHECK_CODE,
        UPDATE_FLOW_FAIL_NON_UPDATED,
        UPDATE_FLOW_FAIL_BLOCK_DATA_WRONG
    }

    private CHIMAthenaLcbUpdateFlow() {
        UpdateComm.getInstance().addReceiveListener(this);
    }

    @Override
    public void addReceiveListener(ILcbUpdateCommReceive listener) {
        mListener = listener;
    }

    @Override
    public void removeReceiveListener(ILcbUpdateCommReceive listener) {
        mListener = null;
    }

    @Override
    public boolean start(ISerialPort device) {
        return true;
    }

    @Override
    public void stop() {
    }

    /*******************************************************************************
    * Function Name  : addProcessReceiveListener()
    * Description    : add ILcbUpdateProcessReceive Listener
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void addProcessReceiveListener(ILcbUpdateProcessReceive listener) {
        _receiveProcessListenerList.add(listener);
    }

    /*******************************************************************************
    * Function Name  : removeProcessReceiveListener()
    * Description    : remove ILcbUpdateProcessReceive Listener
    * Input          : ILcbUpdateProcessReceive
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void removeProcessReceiveListener(ILcbUpdateProcessReceive listener) {
        _receiveProcessListenerList.remove(listener);
    }

    /*******************************************************************************
    * Function Name  : procMessageStringrHandler()
    * Description    :process update lcb message string handler
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private void procMessageStringrHandler(MessageString rtnMessageStr) {
        String rtnString = "MessageString"; //NON-NLS

        switch (rtnMessageStr) {
            case STR_RTN_MESSAGE_NONE:
                break;
            case STR_RTN_MESSAGE_UPDATE_INITIAL:
                rtnString = "Update Initial"; //NON-NLS
                break;
            case STR_RTN_MESSAGE_CHECK_BIN_FILE:
                rtnString = "Check Bin file match or not"; //NON-NLS
                break;
            case STR_RTN_MESSAGE_ENTER_UPDATE_MODE:
                rtnString = "LCB Enter Update Mode"; //NON-NLS
                break;
            case STR_RTN_MESSAGE_READ_UPDATE_MODE:
                rtnString = "LCB Cmd Read Update Mode"; //NON-NLS
                break;
            case STR_RTN_MESSAGE_FLASH_UNLOCK:
                rtnString = "LCB Cmd Flash Unlock"; //NON-NLS
                break;
            case STR_RTN_MESSAGE_ERASE_FLASH:
                rtnString = "LCB Cmd Rrase Flash"; //NON-NLS
                break;
            case STR_RTN_MESSAGE_WRITE_FLASH:
                rtnString = "LCB Cmd Write Flash"; //NON-NLS
                break;
            case STR_RTN_MESSAGE_READ_FLASH_DATA_CHECK:
                rtnString = "LCB Cmd Read Flash Data Check"; //NON-NLS
                break;
            case STR_RTN_MESSAGE_WRITE_CHECK_CODE:
                rtnString = "LCB Cmd Write Check Code"; //NON-NLS
                break;
            case STR_RTN_MESSAGE_READ_PROGRAM_STATE:
                rtnString = "LCB Cmd Read Program State"; //NON-NLS
                break;
            case STR_RTN_MESSAGE_READ_PRODUCE_ID:
                rtnString = "LCB Cmd Read Produce ID"; //NON-NLS
                break;
            case STR_RTN_MESSAGE_LCB_UPDATE_OK:
                rtnString = "LCB Update OK, Please Reset Power"; //NON-NLS
                break;

        }

        for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
            listener.rtnUpdateMessageHandler(rtnString);
        }
    }

    /*******************************************************************************
    * Function Name  : procBinFileLcbInfoErrorHandler(CheckLcbInfoError rtnErrorStr)
    * Description    :process BinFile lcb info error string handler
    * Input          : CheckLcbInfoError rtnErrorStr
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private void procBinFileLcbInfoErrorHandler(CheckLcbInfoError rtnErrorStr) {
        String rtnString = "ErrorString"; //NON-NLS

        switch (rtnErrorStr) {
            case CHECK_INFO_ERROR_NONE:
                rtnString = "None Error"; //NON-NLS
                break;
            case CHECK_INFO_ERROR_MCU_TYPE:
                rtnString = "Bin file MCU_TYPE error "; //NON-NLS
                break;
            case CHECK_INFO_ERROR_MODULE_NAME:
                rtnString = "Bin file MODULE_NAME error "; //NON-NLS
                break;
            case CHECK_INFO_ERROR_MODULE_NO:
                rtnString = "Bin file MODULE_NO error "; //NON-NLS
                break;

        }
        Log.e(TAG, "procBinFileLcbInfoErrorHandler! " + rtnString);

        for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
            listener.rtnUpdateLcbInfoErrorHandler(rtnString);
        }
    }

    /*******************************************************************************
    * Function Name  : procFlowSendCmdError(UpdateFlowSendError rtnErrorEnum)
    * Description    :process update flow send command error string handler
    * Input          : UpdateFlowSendError rtnErrorEnum
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private void procFlowSendCmdError(UpdateFlowSendError rtnErrorEnum) {
        String rtnString = "FailString"; //NON-NLS

        switch (rtnErrorEnum) {
            case SEND_ERROR_DATA_LEN_NOT_MATCH:
                rtnString = "Update Flow send cmd data len not match error "; //NON-NLS
                break;
            case SEND_ERROR_CHECK_CODE_DATA_LEN_NOT_MATCH:
                rtnString = "Update Flow send cmd check code data len not match error "; //NON-NLS
                break;
            case SEND_ERROR_PACKET_NULL:
                rtnString = "Update Flow send packet null error "; //NON-NLS
                break;
        }
        Log.e(TAG, "procFlowSendCmdError! " + rtnString);

        for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
            listener.rtnFlowSendCmdErrorHandler(rtnString);
        }
    }

    /*******************************************************************************
    * Function Name  : procFlowReceiveCmdError(UpdateFlowReceiveError rtnErrorEnum)
    * Description    :process update flow receive command error string handler
    * Input          : UpdateFlowReceiveError rtnErrorEnum
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private void procFlowReceiveCmdError(UpdateFlowReceiveError rtnErrorEnum) {
        String rtnString = "FailString"; //NON-NLS

        switch (rtnErrorEnum) {
            case RECEIVE_ERROR_DATA_LEN:
                rtnString = "Update Flow receive cmd data len error "; //NON-NLS
                break;
            case RECEIVE_ERROR_WRITE_DATA_CRC:
                rtnString = "Update Flow receive cmd write data crc error "; //NON-NLS
                break;
            case RECEIVE_ERROR_WRITE_DATA_ADDR:
                rtnString = "Update Flow receive cmd write data addr error "; //NON-NLS
                break;
            case RECEIVE_ERROR_READ_DATA_ADDR:
                rtnString = "Update Flow receive cmd read data addr error "; //NON-NLS
                break;
            case RECEIVE_ERROR_READ_DATA_LEN:
                rtnString = "Update Flow receive cmd read data len error "; //NON-NLS
                break;
            case RECEIVE_ERROR_READ_DATA_BYTE:
                rtnString = "Update Flow receive cmd read data byte error "; //NON-NLS
                break;
        }

        Log.e(TAG, "procFlowReceiveCmdError! " + rtnString);

        for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
            listener.rtnFlowReceiveCmdErrorHandler(rtnString);
        }
    }

    /*******************************************************************************
    * Function Name  : procUpdateFlowFailHandler(UpdateFlowFail rtnFailEnum)
    * Description    :process BinFile lcb info error string handler
    * Input          : CheckLcbInfoError rtnFailEnum
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private void procUpdateFlowFailHandler(UpdateFlowFail rtnFailEnum) {
        String rtnString = "FailString"; //NON-NLS

        switch (rtnFailEnum) {
            case UPDATE_FLOW_FAIL_READ_UPDATE_MODE:
                //Update Flow read update mode retry 3 times fail
                rtnString = "LCB Enter Update Mode Fail"; //NON-NLS
                break;
            case UPDATE_FLOW_FAIL_FLASH_UNLOCK:
                //Update Flow flash unlock retry 3 times fail
                rtnString = "LCB Flash Unlock Fail"; //NON-NLS
                break;
            case UPDATE_FLOW_FAIL_ERASE_FLASH:
                //Erase Flash Fail
                rtnString = "LCB Erase Flash Fail"; //NON-NLS
                break;
            case UPDATE_FLOW_FAIL_READ_DATA_BYTE_COMPARE:
                //Read Flash Data Bytes compare with Bin File Data Fail
                rtnString = "Flash Data Check Fail"; //NON-NLS
                break;
            case UPDATE_FLOW_FAIL_CHECK_CODE:
                //Check code CRC fail
                rtnString = "Check Code Fail"; //NON-NLS
                break;
            case UPDATE_FLOW_FAIL_NON_UPDATED:
                //NonUpdated
                rtnString = "The main program did not update."; //NON-NLS
                break;
            case UPDATE_FLOW_FAIL_BLOCK_DATA_WRONG:
                //B;ock Data Wrong
                rtnString = "The main program block currently the data is wrong."; //NON-NLS
                break;
        }

        Log.e(TAG, "procUpdateFlowFailHandler! " + rtnString);

        for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
            listener.returnUpdateFlowFailHandler(rtnString);
        }
    }

    /*******************************************************************************
    * Function Name  : lcbUpdateInitial(String binFilePathName)
    * Description    : LCB Update Initial
    * Input          : String binFilePathName
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private void lcbUpdateInitial(String binFilePathName) {
        mBinFilePathName = binFilePathName;
        mRetryUpdateModeCounter = 0;
        mRetryFlashUnlockCounter = 0;

        //lcb update command flow CmdReadProduceID
        lcbUpdateCommand(UpdateCommands.CmdReadProduceID);
        
    }

    /*******************************************************************************
    * Function Name  : lcbUpdateCheckBinFileContent()
    * Description    : LCB Update bin file check
    * Input          :
    * Output         : None
    * Return         : boolean isChecked
    *******************************************************************************/
    private boolean lcbUpdateCheckBinFileContent() {
        char[] RtnChar = new char[20];
        boolean dataMatch = false;
        boolean isModuleNo = false;

        //check MCU type
        mBinFileManager.readBinFileDataChar(mBinFilePathName, mCHIMAthenainfoAddrLen.getBinFileMcuTypeAddr(), RtnChar, 20);

        dataMatch = binFileCheckInformationContent(RtnChar, mCHIMAthenainfoAddrLen.getMcuType());
        if (!dataMatch) {
            procBinFileLcbInfoErrorHandler(CheckLcbInfoError.CHECK_INFO_ERROR_MCU_TYPE);
            return false;
        }

        //check Module Name
        mBinFileManager.readBinFileDataChar(mBinFilePathName, mCHIMAthenainfoAddrLen.getBinFileModuleNameAddr(), RtnChar, 20);
        dataMatch = binFileCheckInformationContent(RtnChar, mCHIMAthenainfoAddrLen.getModuleName());
        if (!dataMatch) {
            procBinFileLcbInfoErrorHandler(CheckLcbInfoError.CHECK_INFO_ERROR_MODULE_NAME);
            return false;
        }

        //check Module No
        mBinFileManager.readBinFileDataChar(mBinFilePathName, mCHIMAthenainfoAddrLen.getBinFileModuleNoAddr(), RtnChar, 20);
        isModuleNo = binFileCheckInformationContent(RtnChar, mCHIMAthenainfoAddrLen.getModuleNo());
        if (!isModuleNo) {
            procBinFileLcbInfoErrorHandler(CheckLcbInfoError.CHECK_INFO_ERROR_MODULE_NAME);
            return false;
        }

        return true;
    }

    /*******************************************************************************
    * Function Name  : binFileCheckInformationContent(char[] srcChar ,char[] distChar)
    * Description    : Check data match or not
    * Input          : char[] srcChar ,char[] distChar
    * Output         : None
    * Return         : boolean match or not
    *******************************************************************************/
    private boolean binFileCheckInformationContent(char[] srcChar, char[] distChar) {
        if (srcChar.length != distChar.length)
            return false;

        for (int i = 0; i < srcChar.length; i++) {
            if (srcChar[i] != distChar[i])
                return false;
        }

        return true;
    }

    /*******************************************************************************
    * Function Name  : startCHIMAthenaLcbUpdateFlowProcess(String binFilePathName )
    * Description    : LCB Update Flow Process
    * Input          : String binFilePathName
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void startCHIMAthenaLcbUpdateFlowProcess(String binFilePathName) {
        //==> Main Function of LCB Update

        //lcb update initial
        procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_UPDATE_INITIAL);
        lcbUpdateInitial(binFilePathName);
    }

    /*******************************************************************************
    * Function Name  : lcbUpdateCommand(UpdateCommands command)
    * Description    :lcb update command
    * Input          :    UpdateCommands command
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private void lcbUpdateCommand(UpdateCommands command) {
        switch (command) {
            case CmdEraseFlash: {
                UpdateComm.ProcessCmdError processCmdError;
                //lcb update command flow CmdEraseFlash
                procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_ERASE_FLASH);

                //check all block have finished
                if (!mCHIMAthenaEraseFlashDataManager.isAllBlockFinish()) {

                    //calc prcent erase flash data
                    float floatErasePercent = mCHIMAthenaEraseFlashDataManager.calcEraseFlashDataPercent();
                    for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                        listener.onUpdateEraseFlashDataPercent(floatErasePercent);
                    }

                    //calc and process erase flash data
                    boolean haveData = mCHIMAthenaEraseFlashDataManager.getNextBlockData();
                    if (haveData) {
                        //lcbEraseFlashProcess
                        processCmdError = UpdateComm.getInstance().lcbEraseFlashProcess(
                                mCHIMAthenaEraseFlashDataManager.getBlockEraseFlashDataAddr()
                                , mCHIMAthenaEraseFlashDataManager.getBlockEraseFlashDataLen());

                        if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL) {
                            procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_PACKET_NULL);
                        }
                    }
                }
            }
            break;
            case CmdWriteFlash: {
                UpdateComm.ProcessCmdError processCmdError;
                //lcb update command flow CmdWriteFlash
                procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_WRITE_FLASH);

                //check all block have finished
                if (!mCHIMAthenaWriteFlashDataManager.isAllBlockFinish()) {

                    //calc prcent write flash data
                    float floatWritePercent = mCHIMAthenaWriteFlashDataManager.calcWriteFlashDataPercent();
                    for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                        listener.onUpdateWriteFlashDataPercent(floatWritePercent);
                    }

                    //calc and process write flash data and bin file data
                    boolean haveData = mCHIMAthenaWriteFlashDataManager.getNextBlockData();
                    if (haveData) {
                        //lcbWriteFlashProcess
                        processCmdError = UpdateComm.getInstance().lcbWriteFlashProcess(
                                mCHIMAthenaWriteFlashDataManager.getBlockWriteFlashDataAddr()
                                , mCHIMAthenaWriteFlashDataManager.getBlockWriteFlashDataLen()
                                , mCHIMAthenaWriteFlashDataManager.getBlockWriteFlashData());

                        if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_DATA_LEN_NOT_MATCH) {
                            procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_DATA_LEN_NOT_MATCH);
                        } else if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL) {
                            procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_PACKET_NULL);
                        }
                    }
                }
            }
            break;
            case CmdReadFlash: {
                UpdateComm.ProcessCmdError processCmdError;
                //lcb update command flow CmdReadFlash
                procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_READ_FLASH_DATA_CHECK);

                //check all block have finished
                if (!mCHIMAthenaReadFlashDataManager.isAllBlockFinish()) {
                    //calc prcent read flash data
                    float floatReadPercent = mCHIMAthenaReadFlashDataManager.calcReadFlashDataPercent();
                    for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                        listener.onUpdateReadFlashDataPercent(floatReadPercent);
                    }

                    //calc and process read flash data and bin file data
                    boolean haveData = mCHIMAthenaReadFlashDataManager.getNextBlockData();
                    if (haveData) {
                        //lcbReadFlashProcess
                        processCmdError = UpdateComm.getInstance().lcbReadFlashProcess(
                                mCHIMAthenaReadFlashDataManager.getBlockReadFlashDataAddr()
                                , mCHIMAthenaReadFlashDataManager.getBlockReadFlashDataLen());

                        if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL) {
                            procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_PACKET_NULL);
                        }
                    }
                }
            }
            break;
            case CmdProgramState: {
                UpdateComm.ProcessCmdError processCmdError;
                //lcb update command flow CmdProgramState
                procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_READ_PROGRAM_STATE);
                processCmdError = UpdateComm.getInstance().lcbProgramStateProcess();
                if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL) {
                    procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_PACKET_NULL);
                }
            }
            break;
            case CmdFlashUnlock: {
                UpdateComm.ProcessCmdError processCmdError;
                //lcb update command flow CmdFlashUnlock
                procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_FLASH_UNLOCK);
                processCmdError = UpdateComm.getInstance().lcbFlashUnlockProcess(Unlock_Password);
                if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL) {
                    procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_PACKET_NULL);
                }
            }
            break;
            case CmdWriteCheckCode: {
                UpdateComm.ProcessCmdError processCmdError;
                //lcb update command flow CmdWriteCheckCode
                procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_WRITE_CHECK_CODE);
                processCmdError = UpdateComm.getInstance().lcbWriteCheckCodeProcess(WriteData);
                if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_CHECK_CODE_DATA_LEN_NOT_MATCH) {
                    procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_CHECK_CODE_DATA_LEN_NOT_MATCH);
                } else if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL) {
                    procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_PACKET_NULL);
                }
            }
            break;
            case CmdReadProduceID: {
                UpdateComm.ProcessCmdError processCmdError;
                //lcb update command flow CmdReadProduceID
                procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_READ_PRODUCE_ID);
                processCmdError = UpdateComm.getInstance().lcbReadProduceIDProcess();
                if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL) {
                    procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_PACKET_NULL);
                }
            }
            break;
            case CmdReadMcuType: {
            }
            break;
            case CmdReadMcuID: {
            }
            break;
            case CmdReadUpdateMode: {
                UpdateComm.ProcessCmdError processCmdError;
                //lcb update command flow CmdReadUpdateMode
                procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_READ_UPDATE_MODE);
                processCmdError = UpdateComm.getInstance().lcbReadUpdateModeProcess();
                if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL) {
                    procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_PACKET_NULL);
                }
            }
            break;
        }
    }

    /*******************************************************************************
    * Function Name  : compReturnIDAndAthenaID()
    * Description    : return ProduceID compare with Athena LCB ProduceID
    * Input          : byte[] produceID
    * Output         : None
    * Return         : boolean data are same or not
    *******************************************************************************/
    public boolean compReturnIDAndAthenaID(byte[] produceID) {
        for (int i = 0; i < produceID.length; i++) {
            if (produceID[i] != Athena_ProduceID[i]) {
                // return ProduceID bytes compare with Athena LCB produceID bytes not match
                return false;
            }
        }

        return true;
    }

    @Override
    public void returnEraseFlashHandler(boolean isEraseOK) {
        if (!isEraseOK) {//Erase Flash Fail
            // Erase Flash show fail
            procUpdateFlowFailHandler(UpdateFlowFail.UPDATE_FLOW_FAIL_ERASE_FLASH);
        } else {    //Erase Flash Success

            if (!mCHIMAthenaEraseFlashDataManager.isAllBlockFinish()) {
                //lcb update command flow CmdEraseFlash
                lcbUpdateCommand(UpdateCommands.CmdEraseFlash);
            } else {
                //calc prcent erase flash data
                float floatErasePercent = mCHIMAthenaEraseFlashDataManager.calcEraseFlashDataPercent();
                for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                    listener.onUpdateEraseFlashDataPercent(floatErasePercent);
                }

                //lcb update write flash data initial
                mCHIMAthenaWriteFlashDataManager.initialWriteFlashData(mBinFilePathName, mCHIMAthenainfoAddrLen);
                //lcb update command flow CmdWriteFlash
                lcbUpdateCommand(UpdateCommands.CmdWriteFlash);
            }
        }
    }

    @Override
    public void returnWriteFlashHandler() {
        if (!mCHIMAthenaWriteFlashDataManager.isAllBlockFinish()) {
            //lcb update command flow CmdWriteFlash
            lcbUpdateCommand(UpdateCommands.CmdWriteFlash);
        } else {
            //calc prcent write flash data
            float floatWritePercent = mCHIMAthenaWriteFlashDataManager.calcWriteFlashDataPercent();
            for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                listener.onUpdateWriteFlashDataPercent(floatWritePercent);
            }

            //lcb update read flash data initial
            mCHIMAthenaReadFlashDataManager.initialReadFlashData(mBinFilePathName, mCHIMAthenainfoAddrLen);
            //lcb update command flow CmdReadFlash
            lcbUpdateCommand(UpdateCommands.CmdReadFlash);
        }
    }

    @Override
    public void returnReadFlashHandler(byte[] readData) {
        boolean isMatch = false;

        // Compare bin file data and flash data
        isMatch = mCHIMAthenaReadFlashDataManager.compReadFlashDataAndBinFile(readData);
        if (!isMatch) {
            procUpdateFlowFailHandler(UpdateFlowFail.UPDATE_FLOW_FAIL_READ_DATA_BYTE_COMPARE);
        } else {// data match
            if (!mCHIMAthenaReadFlashDataManager.isAllBlockFinish()) {
                //lcb update command flow CmdReadFlash
                lcbUpdateCommand(UpdateCommands.CmdReadFlash);
            } else {
                //calc prcent read flash data
                float floatReadPercent = mCHIMAthenaReadFlashDataManager.calcReadFlashDataPercent();
                for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                    listener.onUpdateReadFlashDataPercent(floatReadPercent);
                }

                //lcb update command flow CmdWriteCheckCode
                lcbUpdateCommand(UpdateCommands.CmdWriteCheckCode);
            }
        }
    }

    @Override
    public void returnProgramStateHandler(byte byteProgramState) {
        if (byteProgramState == (byte) 0x00) {
            //NonUpdated
            procUpdateFlowFailHandler(UpdateFlowFail.UPDATE_FLOW_FAIL_NON_UPDATED);
        } else if (byteProgramState == (byte) 0x01) {
            //Updated
            procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_LCB_UPDATE_OK);
            //callback LCB update complete
            for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                listener.onUpdateComplete();
            }
        } else if (byteProgramState == (byte) 0xFF) {
            //block data is wrong
            procUpdateFlowFailHandler(UpdateFlowFail.UPDATE_FLOW_FAIL_BLOCK_DATA_WRONG);
        }
    }

    @Override
    public void returnFlashStateHandler(boolean isFlashUnlock) {
        if (!isFlashUnlock) {//Lock
            if (mRetryFlashUnlockCounter < FLASH_UNLOCK_RETRY_TIMES) {
                //lcb update command flow CmdFlashUnlock
                lcbUpdateCommand(UpdateCommands.CmdFlashUnlock);
                mRetryFlashUnlockCounter++;
            } else {
                // > FLASH_UNLOCK_RETRY_TIMES show fail
                procUpdateFlowFailHandler(UpdateFlowFail.UPDATE_FLOW_FAIL_FLASH_UNLOCK);
            }
        } else {    //Unlock
            //lcb update erase flash data initial
            mCHIMAthenaEraseFlashDataManager.initialEraseFlashData(mCHIMAthenainfoAddrLen);
            //lcb update command flow CmdEraseFlash
            lcbUpdateCommand(UpdateCommands.CmdEraseFlash);
        }
    }

    @Override
    public void returnCheckCodeHandler(boolean isSuccess) {
        if (!isSuccess) {//CRC error
            //  show fail
            procUpdateFlowFailHandler(UpdateFlowFail.UPDATE_FLOW_FAIL_CHECK_CODE);
        } else {    //Success
            //lcb update command flow CmdProgramState
            lcbUpdateCommand(UpdateCommands.CmdProgramState);
        }
    }

    @Override
    public void returnProduceIDHandler(byte[] produceID) {
        boolean isChecked = false;
        // Compare return produceID and LCB produceID
            mCHIMAthenainfoAddrLen.setLcbTypeUpdate(LCB_TYPE_Athena);

        //lcb bin file comtent check
        procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_CHECK_BIN_FILE);
        isChecked = lcbUpdateCheckBinFileContent();

        if (isChecked) {
            //lcb entry update mode
            procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_ENTER_UPDATE_MODE);
            lcbUpdateCommand(UpdateCommands.CmdReadUpdateMode);
        }
        
    }

    @Override
    public void returnMcuTypeHandler() {
    }

    @Override
    public void returnMcuIDHandler() {
    }

    @Override
    public void returnUpdateModeHandler(boolean isUpdateMode) {
        if (!isUpdateMode) {//Normal Mode
            if (mRetryUpdateModeCounter < UPDATE_MODE_RETRY_TIMES) {
                //lcb update command flow CmdReadUpdateMode
                lcbUpdateCommand(UpdateCommands.CmdReadUpdateMode);
                mRetryUpdateModeCounter++;
            } else {
                // > UPDATE_MODE_RETRY_TIMES show fail
                procUpdateFlowFailHandler(UpdateFlowFail.UPDATE_FLOW_FAIL_READ_UPDATE_MODE);
            }

        } else {    //Update Mode
            //lcb update command flow CmdFlashUnlock
            lcbUpdateCommand(UpdateCommands.CmdFlashUnlock);
        }
    }

    @Override
    public void returnPowerOffHandler() {
        //callback onLcbPowerOff
        for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
            listener.onLcbPowerOff();
        }
    }

    @Override
    public void commTimeoutErrorHandler(int timeoutErrorCode) {
    }

    @Override
    public void processRtnReceiveCmdErrorHandler(UpdateComm.ProcessReturnCmdError processReturnCmdError) {

        switch (processReturnCmdError) {
            case CMD_RTN_ERROR_DATA_LEN:
                procFlowReceiveCmdError(UpdateFlowReceiveError.RECEIVE_ERROR_DATA_LEN);
                break;
            case CMD_RTN_ERROR_WRITE_DATA_CRC:
                procFlowReceiveCmdError(UpdateFlowReceiveError.RECEIVE_ERROR_WRITE_DATA_CRC);
                break;
            case CMD_RTN_ERROR_WRITE_DATA_ADDR:
                procFlowReceiveCmdError(UpdateFlowReceiveError.RECEIVE_ERROR_WRITE_DATA_ADDR);
                break;
            case CMD_RTN_ERROR_READ_DATA_ADDR:
                procFlowReceiveCmdError(UpdateFlowReceiveError.RECEIVE_ERROR_READ_DATA_ADDR);
                break;
            case CMD_RTN_ERROR_READ_DATA_LEN:
                procFlowReceiveCmdError(UpdateFlowReceiveError.RECEIVE_ERROR_READ_DATA_LEN);
                break;
        }

    }
}

