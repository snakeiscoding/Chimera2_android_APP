package com.jht.chimera.service.usbmedia.ios.lib.iap2;

public interface IIAP2Callback {
    // Control Session Callback Function
    void onInit(byte[] data);
    void onReset();
    void onSynchronize(Packet packet);
    void onReqAuthCertificate(Packet packet);
    void onReqAuthChallengeResponse(Packet packet);
    void onAuthSucceeded(Packet packet);
    void onAuthFailed(Packet packet);
    void onStartIdentification(Packet packet);
    void onIdentificationAccepted(Packet packet);
    void onDeviceInfoUpdate(Packet packet);
    void onUSBDeviceModeAudioInfo(Packet packet);
    void onPowerUpdate(Packet packet);
    void onNowPlayingUpdate(Packet packet);
    void onMediaLibraryInfo(Packet packet);
    void onMediaLibraryUpdate(Packet packet);

    // File Transfer Session Callback Function
    void onSetupDatagram(Packet packet);
    void onFirstData(Packet packet);
    void onFirstAndOnlyData(Packet packet);
    void onData(Packet packet);
    void onLastData(Packet packet);
    void onCancel(Packet packet);

}
