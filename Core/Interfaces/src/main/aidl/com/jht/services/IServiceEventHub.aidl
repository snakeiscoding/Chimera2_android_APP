package com.jht.services;

import com.jht.services.IServiceEventCallback;
import com.jht.services.ServiceEvent;

interface IServiceEventHub {
    void sendToCentral(in String id, in ServiceEvent e);
    void registerCallback(in String id, IServiceEventCallback cb);
    void unregisterCallback(in String id, IServiceEventCallback cb);
}
