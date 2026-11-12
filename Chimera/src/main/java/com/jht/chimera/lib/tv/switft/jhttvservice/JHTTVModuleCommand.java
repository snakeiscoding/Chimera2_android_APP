package com.jht.chimera.lib.tv.switft.jhttvservice;

/**
 * Created by mutombolin on 2018/05/22.
 */

public interface JHTTVModuleCommand {
    public void sendCommand(CommandData data);

    public void initialize();

    public void uninitialize();
}
