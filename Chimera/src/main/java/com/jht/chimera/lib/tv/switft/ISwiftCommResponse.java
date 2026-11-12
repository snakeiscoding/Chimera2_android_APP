package com.jht.chimera.lib.tv.switft;

//this interface will be implemented by the TV service layer
public interface ISwiftCommResponse {
    // SetSwiftNone
    void onSetSwiftNone(byte byteCmd);

    // ErrorCode
    void onErrorCode(byte byteError);

    // this would be triggered by response of "PON" 0x00
    void onPowerOn();

    // this would be triggered by response of "MEM" 0x1A
    void onMenu();

    // this would be triggered by response of "001" 0x0C
    void onNumber1();

    // this would be triggered by response of "002" 0x0D
    void onNumber2();

    // this would be triggered by response of "003" 0x0E
    void onNumber3();

    // this would be triggered by response of "004" 0x0F
    void onNumber4();

    // this would be triggered by response of "005" 0x10
    void onNumber5();

    // this would be triggered by response of "006" 0x11
    void onNumber6();

    // this would be triggered by response of "007" 0x12
    void onNumber7();

    // this would be triggered by response of "008" 0x13
    void onNumber8();

    // this would be triggered by response of "009" 0x14
    void onNumber9();

    // this would be triggered by response of "000" 0x16
    void onNumber0();

    // this would be triggered by response of "100/." 0x17
    void onDash();

    // this would be triggered by response of "ENT" 0x1C
    void onEnter();

    // this would be triggered by response of "CUP" 0x19
    void onChannelUp();

    // this would be triggered by response of "CDN" 0x1E
    void onChannelDown();

    // this would be triggered by response of "ART" 0x1B
    void onVolumeUp();

    // this would be triggered by response of "ALT" 0x1D
    void onVolumeDown();

    // this would be triggered by response of "POF" 0x60
    void onPowerOff();

    // this would be triggered by response of "MUT" 0x40
    void onMute();

    // this would be triggered by response of "SRC" 0x08
    void onInputSource();

    // this would be triggered by response of "EXT" 0x18
    void onExit();

    // this would be triggered by response of "RTN" 0x15
    void onReturn();

    // this would be triggered by response of "CCD" 0x47
    void onClosedCaption();

    // this would be triggered by response of "RED" 0x55
    void onRed();

    // this would be triggered by response of "GRE" 0x56
    void onGreen();

    // this would be triggered by response of "YEL" 0x57
    void onYellow();

    // this would be triggered by response of "BLU" 0x58
    void onBlue();

    // this would be triggered by response of "APT" 0x43
    void onAspectRatio();

    // this would be triggered by response of "VCP" 0x4F
    void onVChip();

    // this would be triggered by response of "EPG" 0x48
    void onEPG();

    // this would be triggered by response of "TXT" 0x54
    void onTeletext();

    // this would be triggered by response of "CBS" 0x59
    void onCompositeIn();

    // this would be triggered by response of "SVO" 0x5A
    void onSVideoIn();

    // this would be triggered by response of "CMP" 0x5B
    void onComponentIn();

    // this would be triggered by response of "VGA" 0x5C
    void onVGAIn();

    // this would be triggered by response of "TNR" 0x5D
    void onTunerIn();

    // this would be triggered by response of "HDM" 0x5E
    void onHDMIIn();

    // this would be triggered by response of "FAV" 0x66
    void onFavorite();

    // this would be triggered by response of "ADI" 0xFB
    void onGetTunerType();

    // this would be triggered by response of "ADI" 0xFB
    void onGetTunerType(byte irCode);

    // this would be triggered by response of "OM1" 0xE1
    void onSetComponent1080i();

    // this would be triggered by response of "OM2" 0xE2
    void onSetComponent480i();

    // this would be triggered by response of "OM3" 0xE3
    void onSetComponent576i();

    // this would be triggered by response of "OM4" 0xE4
    void onSetComponent480p();

    // this would be triggered by response of "OM5" 0xE5
    void onSetComponent576p();

    // this would be triggered by response of "OM6" 0xE6
    void onSetComponent720p();

    // this would be triggered by response of "GOM" 0x69
    void onGetComponentOutput(byte[] payload);

    // this would be triggered by response of "OH1" 0xE9
    void onSetHDMI720p();

    // this would be triggered by response of "OH2" 0xEA
    void onSetHDMI1080p();

    // this would be triggered by response of "GOH" 0x70
    void onGetHDMIOutput(byte[] payload);

    // this would be triggered by response of "STA" 0x5F
    void onGetStatus(byte irCode);

    // this would be triggered by response of "MPC" 0x68
    void onGetManufacturer(byte[] payload);

    // this would be triggered by response of "VER" 0x67
    void onGetSoftwareVersion(byte[] payload);

    // this would be triggered by response of "MUN" 0x40
    void onMuteOn();

    // this would be triggered by response of "MUF" 0x40
    void onMuteOff();

    // this would be triggered by response of "GMS" 0xEC
    void onGetMuteStatus(byte[] payload);

    // this would be triggered by response of "0x01 0x00 Vol" 0xED
    void onSetVolume(byte[] payload);

    // this would be triggered by response of "GVL" 0xEB
    void onGetVolume(byte[] payload);

    // this would be triggered by response of "GCH" 0x45
    void onGetChannel(byte[] payload);

    // this would be triggered by response of "SCH" 0x49
    void onSetChannel();

    // this would be triggered by response of "SDH" 0x4A
    void onSetDefaultChannel();

    // this would be triggered by response of "CLO" 0xDB
    void onCloneOutDataRequire(byte[] payload);

    // this would be triggered by response of "LEN" 0xDC
    void onGetCloneDataLength(byte[] payload);

    // this would be triggered by response of "EXS" 0xD0
    void onGetCloneOutData(byte[] payload);

    // this would be triggered by response of "ABC" 0x00
    void onTest1();

    // this would be triggered by response of "123" 0x00
    void onTest2();

    // this would be triggered by response of "" 0x00
    void onTest3();

    // this would be triggered by response of "CLI" 0xDB
    void onNotifyCloneIn(byte[] payload);

    // this would be triggered by response of "DLH" 0xDD
    void onNotifyCloneDataLength(byte[] payload);

    // this would be triggered by response of "IMS" 0xD1
    void onSendCloneInData(byte[] payload);

    // this would be triggered by response of "" 0xDE
    void onGetImportResult(byte[] payload);

    // this would be triggered by response of "UPG" 0xEB
    void onNotifyUpgradeFirmware(byte[] payload);

    // this would be triggered by response of "" 0xDF
    void onGetUpgradeResult(byte[] payload);

    // this would be triggered by response of "USB"
    void onGetUSBHandler(byte[] payload);

    // this would be triggered by response of "ACS"
    void onSetAudioChannelSwitch(byte[] payload);
}
