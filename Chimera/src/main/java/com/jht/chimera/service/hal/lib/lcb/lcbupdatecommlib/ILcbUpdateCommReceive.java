package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib;


/*************************************************************************
*   This file is for update lcb receive command handle
*
*************************************************************************/
public interface ILcbUpdateCommReceive {
    // triggered by ReturnEraseFlash 0x11
    void returnEraseFlashHandler(boolean isEraseOK);
    // triggered by ReturnWriteFlash 0x13
    void returnWriteFlashHandler();
    // triggered by ReturnReadFlash 0x15
    void returnReadFlashHandler(byte[] readData);
    // triggered by ReturnProgramState 0x21
    void returnProgramStateHandler(byte byteProgramState);
    // triggered by ReturnFlashState 0x25
    void returnFlashStateHandler(boolean isFlashUnlock);
    // triggered by ReturnCheckCode 0x27
    void returnCheckCodeHandler(boolean isSuccess);
    // triggered by ReturnProduceID 0x31
    void returnProduceIDHandler(byte[] produceID);
    // triggered by ReturnMcuType 0x33
    void returnMcuTypeHandler();
    // triggered by ReturnMcuID 0x35
    void returnMcuIDHandler();
    // triggered by ReturnUpdateMode 0x41
    void returnUpdateModeHandler(boolean isUpdateMode);
    // triggered by ReturnPowerOff 0x47
    void returnPowerOffHandler();
    // triggered by reveive timeout and communication timeout
    void commTimeoutErrorHandler(int timeoutErrorCode);
    // triggered by reveive command error handle
    void processRtnReceiveCmdErrorHandler(UpdateComm.ProcessReturnCmdError processReturnCmdError);
}
