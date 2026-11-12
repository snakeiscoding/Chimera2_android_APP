package com.jht.chimera.lib.tv.settopbox;

import java.util.List;

interface ICommHandler {
    void open(SetTopBoxSetting settings);
    void close();
    void receivedData(byte[] buffer, int bytesRead);
    void sendData(byte[] buffer, int size);
    int readData(byte[] buffer);
    int waitForResponse();
    void clearReceiveHandler();
    List<byte[]> getMessage(SetTopBoxSetting.RemoteButton button);
}
