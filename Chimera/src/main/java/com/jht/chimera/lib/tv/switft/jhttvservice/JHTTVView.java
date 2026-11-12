package com.jht.chimera.lib.tv.switft.jhttvservice;

//import com.jht.tv.swift.jhttvservice.atsc.ATSC_STATUS;

import com.jht.chimera.lib.tv.switft.jhttvservice.atsc.ATSC_STATUS;

public interface JHTTVView {
    void onServiceConnected();

    void onServiceDisconnected();

    void onTunerVol(int vol);

    void onTunerMuteStat(boolean state);

    void onStreamFailed();

    void onATSCChannelInfo();

    void onImportStatus(ATSC_STATUS status);

    void onExportStatus(ATSC_STATUS status);

    void onUpgradeStatus(ATSC_STATUS status);

    void onProgress(int value);
}
