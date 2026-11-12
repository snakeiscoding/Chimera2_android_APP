package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.updateHandler;

import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.ILcbUpdateCommReceive;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.UpdateComm;

public class UpdateLcbCallbackHandlerLCB implements ILcbUpdateCommReceive {
    private static final String TAG = "UpdateLcbCallbackHan...";

    private UpdateLcbHandler mUpdateLcbHandler;

    public UpdateLcbCallbackHandlerLCB(UpdateLcbHandler updateLcbHandler) {
        mUpdateLcbHandler = updateLcbHandler;
    }

    @Override
    public void returnEraseFlashHandler(boolean isEraseOK) {

    }

    @Override
    public void returnWriteFlashHandler() {

    }

    @Override
    public void returnReadFlashHandler(byte[] readData) {

    }

    @Override
    public void returnProgramStateHandler(byte byteProgramState) {

    }

    @Override
    public void returnFlashStateHandler(boolean isFlashUnlock) {

    }

    @Override
    public void returnCheckCodeHandler(boolean isSuccess) {

    }

    @Override
    public void returnProduceIDHandler(byte[] produceID) {

    }

    @Override
    public void returnMcuTypeHandler() {

    }

    @Override
    public void returnMcuIDHandler() {

    }

    @Override
    public void returnUpdateModeHandler(boolean isUpdateMode) {

    }

    @Override
    public void returnPowerOffHandler() {

    }

    @Override
    public void commTimeoutErrorHandler(int timeoutErrorCode) {

    }

    @Override
    public void processRtnReceiveCmdErrorHandler(UpdateComm.ProcessReturnCmdError processReturnCmdError) {

    }
}
