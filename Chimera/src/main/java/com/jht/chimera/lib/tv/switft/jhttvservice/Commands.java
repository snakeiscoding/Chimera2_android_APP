package com.jht.chimera.lib.tv.switft.jhttvservice;

/**
 * Created by mutombolin on 2018/08/27.
 */

public enum Commands {
    POWER_ON((byte) 0x01, true),
    POWER_OFF((byte) 0x00, true),
    MENU((byte) 0x04, false),
    CLOSEDCAPTIONSTATE((byte) 0x17, false),
    MUTE((byte) 0x03, false),
    ENTER((byte) 0x05, false),
    EXIT((byte) 0x06, false),
    BTN1((byte) 0x0C, false),
    BTN2((byte) 0x0D, false),
    BTN3((byte) 0x0E, false),
    BTN4((byte) 0x0F, false),
    BTN5((byte) 0x10, false),
    BTN6((byte) 0x11, false),
    BTN7((byte) 0x12, false),
    BTN8((byte) 0x13, false),
    BTN9((byte) 0x14, false),
    BTNDASH((byte) 0x15, false),
    BTN0((byte) 0x0B, false),
    BTNUP((byte) 0x09, false),
    BTNDOWN((byte) 0x0A, false),
    BTNLEFT((byte) 0x07, false),
    BTNRIGHT((byte) 0x08, false),
    LASTCHANNEL((byte) 0x16, false),
    TUNER_ON((byte) 0x00, false),
    TUNER_OFF((byte) 0x01, false),
    RED((byte) 0x23, false),
    GREEN((byte) 0x24, false),
    YELLOW((byte) 0x25, false),
    BLUE((byte) 0x26, false),
    MPC((byte) 0x2A, false),
    OUTPUT1080i((byte) 0x2C, false),
    OUTPUT480i((byte) 0x2D, false),
    OUTPUT576i((byte) 0x2E, false),
    OUTPUT480p((byte) 0x2F, false),
    OUTPUT576p((byte) 0x30, false),
    OUTPUT720p((byte) 0x31, false),
    HDMIOUTPUT((byte) 0x32, false),
    HDMIOUTPUT720p((byte) 0x33, false),
    HDMIOUTPUT1080p((byte) 0x34, false),
    VOLDOWNJP((byte) 0x35, false),
    VOLUPJP((byte) 0x36, false),
    GMS((byte) 0x39, false), // return current mute status
    GVL((byte) 0x38, false), // return current volume value
    PCN((byte) 0x37, false), // return current channel
    USBHANDLER((byte) 0x3B, false), // acquire usb handler from tuner, km4 only.
    MUTEON((byte) 0x3C, false),
    MUTEOFF((byte) 0x3D, false),
    GCH((byte) 0x3E, false),    // Return current channel, Swift-info
    GVR((byte) 0x3F, false), // Return Tuner Type Version, Swift-info
    CLO((byte) 0x40, false), // Closed Caption Display Subtitle, Swift-info
    LEN((byte) 0x41, false), // TV feedback clone data length to console, byte 1 for high byte, Swift-info
    SEN((byte) 0x42, false), // Notify TV start sending clone data, Swift-info
    CLI((byte) 0x43, false), // Notify TV clone in, Swift-info
    DLH((byte) 0x44, false), // Notify TV clone data/software data nlength, Swift-info
    UPG((byte) 0x45, false), // Nofify TV software upgrade, Swift-info
    EXS((byte) 0x46, false),
    IMS((byte) 0x47, false),
    MSI((byte) 0x48, false);

    private byte command = (byte) 0x00;
    private boolean isPowerCommand = false;

    Commands(byte command, boolean isPowerCommand) {
        this.command = command;
        this.isPowerCommand = isPowerCommand;
    }

    public byte getCommand() {
        return command;
    }

    public boolean isPowerCommand() {
        return isPowerCommand;
    }
}
