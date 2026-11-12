package com.jht.chimera.lib.tv.hdmi;
public interface OnHdmiInfoChangedListener {
    void onHdmiInfoChanged(HdmiInputInfo newInfo, HdmiInputInfo oldInfo);
}