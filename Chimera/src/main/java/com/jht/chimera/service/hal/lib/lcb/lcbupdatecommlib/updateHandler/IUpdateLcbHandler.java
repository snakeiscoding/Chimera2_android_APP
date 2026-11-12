package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.updateHandler;


public interface IUpdateLcbHandler {
    //Register UpdateLcbHandler callback
    void setUpdateLcbHandlerCallback(IUpdateLcbHandlerCallback updateLcbHandlerCallback);

    //Open Lcb Update Port
    boolean openLcbUpdatePort();
}
