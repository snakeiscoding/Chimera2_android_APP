package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.lcbupdateInterface;


public interface ILCBUpdateCallback {

    //LCB update lcb type error
    void onUpdateLcbTypeError();

    //LCB update lcb type error
    void onUpdateLcbFilePathError();

    //LCB update process finished
    void onLcbUpdateFinish();

}
