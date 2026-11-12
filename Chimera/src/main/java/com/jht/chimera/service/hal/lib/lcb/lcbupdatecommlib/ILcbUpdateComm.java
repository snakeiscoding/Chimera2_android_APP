package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib;

import com.jht.serialport.ISerialPort;

/*************************************************************************
 *   This file is for update lcb send command handle
 *
 *************************************************************************/

public interface ILcbUpdateComm {

    public void addReceiveListener(ILcbUpdateCommReceive listener);

    public void removeReceiveListener(ILcbUpdateCommReceive listener);

    //serial port start
    public boolean start(ISerialPort device);

    //serial port stop
    public void stop();
}
