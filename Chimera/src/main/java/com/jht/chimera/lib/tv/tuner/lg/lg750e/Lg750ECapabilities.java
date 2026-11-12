package com.jht.chimera.lib.tv.tuner.lg.lg750e;

import com.jht.serialport.peripheral.tuner.ITunerCapabilities;

public class Lg750ECapabilities implements ITunerCapabilities {
    @Override
    public boolean channelList() {
        return true;
    }

    @Override
    public boolean cc() {
        return true;
    }

    @Override
    public boolean sap() {
        return true;
    }

    @Override
    public boolean lastChannel() {
        return false;
    }

    @Override
    public boolean info() {
        return false;
    }

    @Override
    public boolean colorButtons() {
        return false;
    }

    @Override
    public boolean keypad() {
        return true;
    }

    @Override
    public boolean update() {
        return false;
    }

    @Override
    public boolean importChannelList() {
        return true;
    }

    @Override
    public boolean menu() {
        return true;
    }

    @Override
    public boolean exit() {
        return true;
    }

    @Override
    public boolean enter() {
        return true;
    }

    @Override
    public boolean select() {
        return false;
    }

    @Override
    public boolean modifyChannelList() {
        return true;
    }

    @Override
    public boolean autoRefresh() {
        return false;
    }

    @Override
    public boolean dash() {
        return false;
    }

    @Override
    public boolean cabCompatible() {
        return false;
    }

    @Override
    public boolean canEditChannelName() {
        return true;
    }

    @Override
    public boolean isSetTopBox() {
        return false;
    }

    @Override
    public boolean hdmi() {
        return true;
    }

    @Override
    public boolean hasIPScanList() {
        return false;
    }

    @Override
    public boolean guide() {
        return false;
    }

    @Override
    public boolean canSkipChannel() {
        return true;
    }

    @Override
    public boolean setup() {
        return true;
    }

    @Override
    public boolean back() {
        return true;
    }

    @Override
    public boolean input() {
        return true;
    }

    @Override
    public boolean volume() {
        return false;
    }
}
