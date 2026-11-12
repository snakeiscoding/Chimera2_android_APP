package com.jht.chimera.lib.tv.switft.jhttvservice;

import android.content.Context;


/**
 * Created by mutombolin on 2018/05/22.
 */

public class CommandHelper {

    Context context = null;
    private JHTTVModuleCommand commandImpl;

    public CommandHelper(JHTTVModuleCommand commandImpl) {
        this.commandImpl = commandImpl;
    }

    public void send(CommandData data) {
        commandImpl.sendCommand(data);
    }

    public void initialize() {
        commandImpl.initialize();
    }

    public void uninitialize() {
        commandImpl.uninitialize();
    }
}
