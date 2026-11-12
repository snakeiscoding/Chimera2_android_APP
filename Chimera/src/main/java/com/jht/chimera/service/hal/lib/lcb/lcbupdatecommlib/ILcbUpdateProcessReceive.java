package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib;
/*************************************************************************
*   This file is for update flow process message , error or fault handle
*
*************************************************************************/

public interface ILcbUpdateProcessReceive {

    // triggered by reveive show update process message string (enum MessageString)
    void rtnUpdateMessageHandler(String strMessage);	

    // triggered by reveive show update process error string (enum CheckLcbInfoError)
    void rtnUpdateLcbInfoErrorHandler(String strErrorString);	

    // triggered by reveive show update flow send command error string ( enum UpdateFlowSendError)
    void rtnFlowSendCmdErrorHandler(String strSendErrorString);	

    // triggered by reveive show update flow receive command error string ( enum UpdateFlowReceiveError)
    void rtnFlowReceiveCmdErrorHandler(String strReceiveErrorString);	

    // triggered by reveive show update flow fail string (enum UpdateFlowFail)
    void returnUpdateFlowFailHandler(String strFailString);	

    // this would be send during the update to indicate the percentage of the update EraseFlashData process
    void onUpdateEraseFlashDataPercent(float percentComplete);
    // this would be send during the update to indicate the percentage of the update WriteFlashData process
    void onUpdateWriteFlashDataPercent(float percentComplete);
    // this would be send during the update to indicate the percentage of the update ReadFlashData process
    void onUpdateReadFlashDataPercent(float percentComplete);

    // triggered by after the update has been completed
     void onUpdateComplete();	
    // triggered by after LCB the update complete and set lcbPowerOffProcess
    void onLcbPowerOff();
}
