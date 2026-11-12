package com.jht.chimera.service.hal.lib.io.iocommlib;

public enum PowerType {
    rs485Power((byte)0x01),
    csafePower((byte)0x03),
    hdmiPower((byte)0x04),
    audioSAPower((byte)0x05),
    voicePower((byte)0x07),
    rfidPower((byte)0x08),
    qiPower((byte)0x09),
    tvPower((byte)0x0A),
    cabPower((byte)0x0B),
    ipodPower((byte)0x0C),
    heartRatePower((byte)0x0D),
    gymkitPower((byte)0x0E),
    usb_5V((byte)0x0F),
    ir_5V((byte) 0x11),
    ext_14V((byte)0x10);

    private byte id;
    PowerType(byte id){
        this.id = id;
    }

    public static PowerType format(byte id){
        for(PowerType type: PowerType.values()){
            if(id == type.id){
                return type;
            }
        }
        return null;
    }

    public byte getID(){
        return id;
    }
}
