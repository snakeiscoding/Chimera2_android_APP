package com.jht.chimera.lib.tv.switft.jhttvservice;

/**
 * Created by mutombolin on 2018/05/22.
 */

public class CommandData implements Cloneable {

    private Commands command = Commands.MENU;

    public CommandData(Commands command) {
        this.command = command;
    }

    public byte getCommand() {
        return command.getCommand();
    }

    public boolean isPowerCommand() {
        return command.isPowerCommand();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
