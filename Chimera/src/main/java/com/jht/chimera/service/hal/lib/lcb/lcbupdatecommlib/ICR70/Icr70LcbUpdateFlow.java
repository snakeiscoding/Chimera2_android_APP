package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.ICR70;

import android.util.Log;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.common.string.BinaryAsciiSearch;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.ILcbUpdateComm;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.ILcbUpdateCommReceive;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.ILcbUpdateProcessReceive;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.UpdateComm;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.UpdateCommands;
import com.jht.serialport.ISerialPort;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;

/*
    Trying to quickly throw together an update flow based on the Athena flow in order to test the LCB firmware update.
    Seems like flow will check ID, check bin file, erase all blocks, write all blocks, read/verify all blocks, some checks, kick program state.
    Update flash region is 0x8000 up to 0xF7FF, flash page is 512 bytes. Bootloader is at 0x2000.
 */
public class Icr70LcbUpdateFlow  implements ILcbUpdateComm, ILcbUpdateCommReceive {
    public static final String TAG = "Icr70LcbUpdateFlow";
    public static Icr70LcbUpdateFlow Instance = new Icr70LcbUpdateFlow();
    public static final String ID = "ICR70";

    private final ArrayList<ILcbUpdateProcessReceive> _receiveProcessListenerList = new ArrayList<>();
    private ILcbUpdateCommReceive mListener;
    private BinFile updateFile;

    private int mRetryUpdateModeCounter = 0;
    private final static int UPDATE_MODE_RETRY_TIMES 	=  2;
    private final byte[] Unlock_Password = {'J', 'o', 'h', 'n', 's', 'o', 'n'};
    private int mRetryFlashUnlockCounter = 0;
    private final static int FLASH_UNLOCK_RETRY_TIMES 	=  2;
    private final static byte[] WriteData = new byte[]{(byte) 0xD0, (byte) 0xD1, (byte) 0xD2, (byte) 0xD3,
            (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88,
            (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC,
            (byte) 0xDF, (byte) 0xEE, (byte) 0xEF};

    private Icr70LcbUpdateFlow() {
        UpdateComm.getInstance().addReceiveListener(this);
    }
    public void addProcessReceiveListener(ILcbUpdateProcessReceive listener) {
        _receiveProcessListenerList.add(listener);
    }
    public void removeProcessReceiveListener(ILcbUpdateProcessReceive listener) {
        _receiveProcessListenerList.remove(listener);
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

    public void startLcbUpdateFlowProcess(String binFilePathName) {
        //==> Main Function of LCB Update

        //lcb update initial
        procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_UPDATE_INITIAL);
        lcbUpdateInitial(binFilePathName);
    }

    private void lcbUpdateInitial(String binFilePathName) {
        File file = new File(binFilePathName);
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "Cannot find or read " + file);
            return;
        }
        updateFile = new BinFile(file);
        mRetryUpdateModeCounter = 0;
        mRetryFlashUnlockCounter = 0;

        //lcb update command flow CmdReadProduceID
        lcbUpdateCommand(UpdateCommands.CmdReadProduceID);
    }

    private boolean lcbUpdateCheckBinFileContent() {
        if (updateFile == null) {
            Log.d(TAG, "lcbUpdateCheckBinFileContent bin file null");
            return false;
        }
        if (!updateFile.findId(ID)) {
            Log.d(TAG, "lcbUpdateCheckBinFileContent bin missing ID");
            return false;
        }
        if (updateFile.flashEndAddress < updateFile.flashAddress) {
            Log.d(TAG, "lcbUpdateCheckBinFileContent bin too short");
            return false;
        }
        Log.d(TAG, "Bin file verified");
        return true;
    }

    private boolean notFinished() { return updateFile != null && !updateFile.isEnd(); }
    private float progress() { return updateFile != null ? updateFile.getProgress() : 0; }

    private void lcbUpdateCommand(UpdateCommands command) {
        Log.d(TAG, "lcbUpdateCommand " + command + " flash " + Integer.toHexString(updateFile.flashAddress));
        switch (command) {
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
            case CmdEraseFlash: {
                UpdateComm.ProcessCmdError processCmdError;
                //lcb update command flow CmdEraseFlash
                procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_ERASE_FLASH);

                //check all block have finished
                if (notFinished()) {

                    //calc percent erase flash data
                    float floatErasePercent = progress();
                    for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                        listener.onUpdateEraseFlashDataPercent(floatErasePercent);
                    }

                    //calc and process erase flash data
                    boolean haveData = !updateFile.isEnd(); // don't actually need data to erase flash? should we waste time reading data?
                    if (haveData) {
                        //lcbEraseFlashProcess
                        processCmdError = UpdateComm.getInstance().lcbEraseFlashProcess(updateFile.flashAddress, BinFile.pageSize);

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
                if (notFinished()) {

                    //calc percent write flash data
                    float floatWritePercent = progress();
                    for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                        listener.onUpdateWriteFlashDataPercent(floatWritePercent);
                    }

                    //calc and process write flash data and bin file data
                    boolean haveData = updateFile.read();
                    if (haveData) {
                        //lcbWriteFlashProcess
                        updateFile.zeroUnusedData(); // this call expects array length to match the passed size for some reason? so zero any extra bytes in the page
                        processCmdError = UpdateComm.getInstance().lcbWriteFlashProcess(updateFile.flashAddress, updateFile.data.length, updateFile.data);

                        if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_DATA_LEN_NOT_MATCH) {
                            procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_DATA_LEN_NOT_MATCH);
                        } else if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL) {
                            procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_PACKET_NULL);
                        }
                    } else {
                        Log.e(TAG, "Bin file could not be read @ " + updateFile.fileOffset);
                        procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_DATA_LEN_NOT_MATCH);
                    }
                }
            }
            break;
            case CmdReadFlash: {
                UpdateComm.ProcessCmdError processCmdError;
                //lcb update command flow CmdReadFlash
                procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_READ_FLASH_DATA_CHECK);

                //check all block have finished
                if (notFinished()) {
                    //calc percent read flash data
                    float floatReadPercent = progress();
                    for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                        listener.onUpdateReadFlashDataPercent(floatReadPercent);
                    }

                    //calc and process read flash data and bin file data
                    boolean haveData = updateFile.read();
                    if (haveData) {
                        //lcbReadFlashProcess
                        processCmdError = UpdateComm.getInstance().lcbReadFlashProcess(updateFile.flashAddress, updateFile.dataSize);

                        if (processCmdError == UpdateComm.ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL) {
                            procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_PACKET_NULL);
                        }
                    } else {
                        Log.e(TAG, "Bin file could not be read @ " + updateFile.fileOffset);
                        procFlowSendCmdError(UpdateFlowSendError.SEND_ERROR_DATA_LEN_NOT_MATCH);
                    }
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
            case CmdReadMcuType:
            case CmdReadMcuID:
                // not used in this flow currently
                break;
        }
    }


    @Override
    public void returnProduceIDHandler(byte[] produceID) {
        boolean isChecked = false;
        Log.d(TAG, "Produce ID " + new String(produceID));
        // Compare return produceID and LCB produceID
        BinaryAsciiSearch searcher = new BinaryAsciiSearch(ID, false);
        if (searcher.search(produceID)) {
            //lcb bin file content check
            procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_CHECK_BIN_FILE);
            isChecked = lcbUpdateCheckBinFileContent();
        }

        if (isChecked) {
            //lcb entry update mode
            procMessageStringrHandler(MessageString.STR_RTN_MESSAGE_ENTER_UPDATE_MODE);
            lcbUpdateCommand(UpdateCommands.CmdReadUpdateMode);
        } else {
            Log.e(TAG, "Produce ID or BIN check failed");
        }
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
            updateFile.reset();
            //lcb update command flow CmdEraseFlash
            lcbUpdateCommand(UpdateCommands.CmdEraseFlash);
        }
    }

    @Override
    public void returnEraseFlashHandler(boolean isEraseOK) {
        if (!isEraseOK) {//Erase Flash Fail
            Log.e(TAG, "Erase flash failed " + updateFile.flashAddress);
            // Erase Flash show fail
            procUpdateFlowFailHandler(UpdateFlowFail.UPDATE_FLOW_FAIL_ERASE_FLASH);
        } else {    //Erase Flash Success
            if (updateFile.nextPage()) {
                //lcb update command flow CmdEraseFlash
                lcbUpdateCommand(UpdateCommands.CmdEraseFlash);
            } else {
                //calc percent erase flash data
                float floatErasePercent = progress();
                for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                    listener.onUpdateEraseFlashDataPercent(floatErasePercent);
                }
                //lcb update write flash data initial
                updateFile.reset();
                //lcb update command flow CmdWriteFlash
                lcbUpdateCommand(UpdateCommands.CmdWriteFlash);
            }
        }
    }

    @Override
    public void returnWriteFlashHandler() {
        if (updateFile.nextPage()) {
            //lcb update command flow CmdWriteFlash
            lcbUpdateCommand(UpdateCommands.CmdWriteFlash);
        } else {
            //calc percent write flash data
            float floatWritePercent = progress();
            for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                listener.onUpdateWriteFlashDataPercent(floatWritePercent);
            }

            //lcb update read flash data initial
            updateFile.reset();
            //lcb update command flow CmdReadFlash
            lcbUpdateCommand(UpdateCommands.CmdReadFlash);
        }
    }

    @Override
    public void returnReadFlashHandler(byte[] readData) {
        boolean isMatch;

        // Compare bin file data and flash data
        isMatch = updateFile.matchesData(readData);
        if (!isMatch) {
            procUpdateFlowFailHandler(UpdateFlowFail.UPDATE_FLOW_FAIL_READ_DATA_BYTE_COMPARE);
        } else {// data match
            if (updateFile.nextPage()) {
                //lcb update command flow CmdReadFlash
                lcbUpdateCommand(UpdateCommands.CmdReadFlash);
            } else {
                //calc percent read flash data
                float floatReadPercent = progress();
                for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
                    listener.onUpdateReadFlashDataPercent(floatReadPercent);
                }

                //lcb update command flow CmdWriteCheckCode
                lcbUpdateCommand(UpdateCommands.CmdWriteCheckCode);
            }
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
    public void returnMcuTypeHandler() {

    }

    @Override
    public void returnMcuIDHandler() {

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


    //region messaging
    // marking as non nls, because the other LCB updater flows ignored translating these messages?
    // if we did need translations, could put string id into enums and use string lookup instead of big switch blocks?
    private void procMessageStringrHandler(MessageString msg) { postMessage(msg.message);}
    private void postMessage(@NonNls String message) {
        Log.d(TAG, "postMessage: " + message);
        for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
            listener.rtnUpdateMessageHandler(message);
        }
    }
    private void procBinFileLcbInfoErrorHandler(CheckLcbInfoError rtnErrorStr) { postErrorMessage(rtnErrorStr.message); }
    private void postErrorMessage(@NonNls String message) {
        Log.d(TAG, "postErrorMessage: " + message);
        for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
            listener.rtnUpdateLcbInfoErrorHandler(message);
        }
    }
    private void procUpdateFlowFailHandler(UpdateFlowFail rtnFailEnum) {postUpdateFlowFailMessage(rtnFailEnum.message);}
    private void postUpdateFlowFailMessage(@NonNls String message) {
        Log.d(TAG, "postUpdateFlowFailMessage: " + message);
        for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
            listener.returnUpdateFlowFailHandler(message);
        }
    }
    private void procFlowSendCmdError(UpdateFlowSendError rtnErrorEnum) {postFlowSendCmdErrorMessage(rtnErrorEnum.message);}
    private void postFlowSendCmdErrorMessage(@NonNls String message) {
        Log.d(TAG, "postFlowSendCmdErrorMessage: " + message);
        for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
            listener.rtnFlowSendCmdErrorHandler(message);
        }
    }
    private void procFlowReceiveCmdError(UpdateFlowReceiveError rtnErrorEnum) {
        Log.d(TAG, "procFlowReceiveCmdError: " + rtnErrorEnum.message);
        for (ILcbUpdateProcessReceive listener : _receiveProcessListenerList) {
            listener.rtnFlowReceiveCmdErrorHandler(rtnErrorEnum.message);
        }
    }

    //Message show update process what step
    public enum MessageString {
        STR_RTN_MESSAGE_NONE(null),
        STR_RTN_MESSAGE_UPDATE_INITIAL("Update Initial"),
        STR_RTN_MESSAGE_CHECK_BIN_FILE("Check Bin file match or not"),
        STR_RTN_MESSAGE_ENTER_UPDATE_MODE("LCB Enter Update Mode"),
        STR_RTN_MESSAGE_READ_UPDATE_MODE("LCB Cmd Read Update Mode"),
        STR_RTN_MESSAGE_FLASH_UNLOCK("LCB Cmd Flash Unlock"),
        STR_RTN_MESSAGE_ERASE_FLASH("LCB Cmd Rrase Flash"),
        STR_RTN_MESSAGE_WRITE_FLASH("LCB Cmd Write Flash"),
        STR_RTN_MESSAGE_READ_FLASH_DATA_CHECK("LCB Cmd Read Flash Data Check"),
        STR_RTN_MESSAGE_WRITE_CHECK_CODE("LCB Cmd Write Check Code"),
        STR_RTN_MESSAGE_READ_PROGRAM_STATE("LCB Cmd Read Program State"),
        STR_RTN_MESSAGE_READ_PRODUCE_ID("LCB Cmd Read Produce ID"),
        STR_RTN_MESSAGE_LCB_UPDATE_OK("LCB Update OK, Please Reset Power");
        MessageString(@NonNls String msg) { message = msg; }
        public final String message;
    }

    //Check bin file LCB info show error string
    public enum CheckLcbInfoError {
        CHECK_INFO_ERROR_NONE("None Error"),
        CHECK_INFO_ERROR_MCU_TYPE("Bin file MCU_TYPE error"),
        CHECK_INFO_ERROR_MODULE_NAME("Bin file MODULE_NAME error "),
        CHECK_INFO_ERROR_MODULE_NO("Bin file MODULE_NO error ");
        CheckLcbInfoError(@NonNls String msg) { message = msg; }
        public final String message;
    }

    //Update flow send command error show string
    public enum UpdateFlowSendError {
        SEND_ERROR_NONE(null),
        SEND_ERROR_DATA_LEN_NOT_MATCH("Update Flow send cmd data len not match error "),
        SEND_ERROR_CHECK_CODE_DATA_LEN_NOT_MATCH("Update Flow send cmd check code data len not match error "),
        SEND_ERROR_PACKET_NULL("Update Flow send packet null error ");
        UpdateFlowSendError(@NonNls String msg) { message = msg; }
        public final String message;
    }

    //Update flow receive command error show string
    public enum UpdateFlowReceiveError {
        RECEIVE_ERROR_NONE(null),
        RECEIVE_ERROR_DATA_LEN("Update Flow receive cmd data len error "),
        RECEIVE_ERROR_WRITE_DATA_CRC("Update Flow receive cmd write data crc error "),
        RECEIVE_ERROR_WRITE_DATA_ADDR("Update Flow receive cmd write data addr error "),
        RECEIVE_ERROR_READ_DATA_ADDR("Update Flow receive cmd read data addr error "),
        RECEIVE_ERROR_READ_DATA_LEN("Update Flow receive cmd read data len error "),
        RECEIVE_ERROR_READ_DATA_BYTE("Update Flow receive cmd read data byte error ");
        UpdateFlowReceiveError(@NonNls String msg) { message = msg; }
        public final String message;
    }

    //Update flow fail show string
    public enum UpdateFlowFail {
        UPDATE_FLOW_FAIL_NONE(null),
        UPDATE_FLOW_FAIL_READ_UPDATE_MODE("LCB Enter Update Mode Fail"),
        UPDATE_FLOW_FAIL_FLASH_UNLOCK("LCB Flash Unlock Fail"),
        UPDATE_FLOW_FAIL_ERASE_FLASH("LCB Erase Flash Fail"),
        UPDATE_FLOW_FAIL_READ_DATA_BYTE_COMPARE("Flash Data Check Fail"),
        UPDATE_FLOW_FAIL_CHECK_CODE("Check Code Fail"),
        UPDATE_FLOW_FAIL_NON_UPDATED("The main program did not update."),
        UPDATE_FLOW_FAIL_BLOCK_DATA_WRONG("The main program block currently the data is wrong.");
        UpdateFlowFail(@NonNls String msg) { message = msg; }
        public final String message;
    }
    //endregion messaging


    private static class BinFile {
        public static final int pageSize = 0x200;
        public static final int flashStart = 0x8000;
        public static final int binStart = 0x2000; // must skip bootloader section
        public File file;
        public int flashAddress;
        public int flashEndAddress;
        public int fileOffset;
        public byte[] data;
        public int dataSize;

        public BinFile(File file) {
            this.file = file;
            reset();
        }

        public void reset() {
            fileOffset = binStart;
            flashAddress = flashStart;
            flashEndAddress = flashStart + ((int)file.length() - binStart);
            data = new byte[pageSize];
            dataSize = 0;
        }

        public float getProgress() {
            int done = getFlashBytesDone();
            int total = getFlashBytesTotal();
            return total > 0 ? (float) done / total : 0;
        }

        public int getFlashBytesTotal() { return flashEndAddress - flashStart; }
        public int getFlashBytesDone() { return flashAddress - flashStart; }
        public int getFlashBytesRemaining() { return flashEndAddress - flashAddress; }

        public boolean isEnd() { return fileOffset < 0; }
        public boolean nextPage() {
            int remaining = getFlashBytesRemaining();
            if (remaining > pageSize) { // we have more beyond the current page
                flashAddress += pageSize;
                fileOffset += pageSize;
            } else { // we are on the last block already
                flashAddress = flashEndAddress;
                fileOffset = -1;
            }
            return !isEnd();
        }

        public boolean read() {
            //Log.d(TAG, "read " + fileOffset);
            dataSize = 0;
            if (fileOffset < 0) return false;
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(fileOffset);
                dataSize = raf.read(data); // should maybe loop until we get the expected number of bytes?
                //Log.d(TAG, "read " + dataSize + " @ " + fileOffset);
                return (dataSize > 0);
            } catch (Exception e) {
                Log.e(TAG, "Fail reading binary block @ " + fileOffset, e);
                return false;
            }
        }

        public boolean findId(String id) {
            BinaryAsciiSearch searcher = new BinaryAsciiSearch(id, true);
            byte[] data = FileUtils.readFile(file); // should probably stream through this, but I guess its a small file
            return searcher.search(data);
        }

        public boolean matchesData(byte[] block) {
            if (block.length != this.dataSize) return false;
            for (int i = 0; i < block.length; ++i) {
                if (block[i] != this.data[i]) return false;
            }
            return true;
        }

        public void zeroUnusedData() {
            if (this.dataSize < this.data.length) Arrays.fill(this.data, this.dataSize, this.data.length, (byte) 0); // [from, to)
        }
    } // END CLASS: BinFile

}
