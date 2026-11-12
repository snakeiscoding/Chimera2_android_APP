package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib;

public enum UpdateCommands {
    // LCB Update Command
    CmdNone(0x00),
    // UCB --> LCB
    CmdEraseFlash(0x10),
    CmdWriteFlash(0x12),
    CmdReadFlash(0x14),
    CmdProgramState(0x20),
    CmdFlashUnlock(0x24),
    CmdWriteCheckCode(0x26),
    CmdReadProduceID(0x30),
    CmdReadMcuType(0x32),
    CmdReadMcuID(0x34),
    CmdReadUpdateMode(0x40),
    CmdPowerOff(0x46),

    // LCB --> UCB
    CmdReturnEraseFlash(0x11),
    CmdReturnWriteFlash(0x13),
    CmdReturnReadFlash(0x15),
    CmdReturnProgramState(0x21),
    CmdReturnFlashState(0x25),
    CmdReturnCheckCode(0x27),
    CmdReturnProduceID(0x31),
    CmdReturnMcuType(0x33),
    CmdReturnMcuID(0x35),
    CmdReturnUpdateMode(0x41),
    CmdReturnPowerOff(0x47),

    CmdReturnInvalidCommand(0x7F);

    public int Value;

    UpdateCommands(int value) {
        Value = value;
    }

    public static UpdateCommands fromInt(int i) {
        for (UpdateCommands b : UpdateCommands.values()) {
            if (b.Value == i) {
                return b;
            }
        }
        return null;
    }
}
