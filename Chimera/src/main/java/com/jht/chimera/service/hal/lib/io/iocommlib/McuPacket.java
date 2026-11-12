package com.jht.chimera.service.hal.lib.io.iocommlib;


import java.util.Arrays;

public class McuPacket {

    public int commandCode = 0;
    public int status = 0;
    public byte[] data = null;

    private final int packetTimeoutMs;

    private static final byte StartByte_1 = (byte)0x55;
    private static final byte StartByte_2 = (byte)0xAA;
    public static final int MAX_DATA_PAYLOAD_SIZE = 0xFFFF;

    public enum CommandType{
        REPLAY,
        EVENT,
        UPDATE
    }

    McuPacket(){
        packetTimeoutMs = 1000;
    }


    public McuPacket(McuProtocol.McuCommands mcuCommand, byte... data){
        this(mcuCommand, 1000, data);
    }

    public McuPacket(McuProtocol.McuCommands mcuCommand, int timeoutMs, byte... data){
        this.commandCode = mcuCommand.get_command();
        this.packetTimeoutMs = timeoutMs;
        this.data = (data.length == 0 ? null : data); // don't bother with empty arrays
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof McuPacket)) return false;
        McuPacket other = (McuPacket) obj;
        if (commandCode != other.commandCode) return false;
        if (status != other.status) return false;
        int size = getDataLength();
        if (size != other.getDataLength()) return false;
        return size == 0 || Arrays.equals(data, other.data); // consider null and zero length array the same
    }

    public int getDataLength() {
        return (data == null ? 0 : data.length);
    }

    public byte[] getBytes(){
        int dataLength = getDataLength();
        byte[] txPacket = new byte[dataLength + 9];
        int i = 0;
        txPacket[i++] = StartByte_1;
        txPacket[i++] = StartByte_2;
        txPacket[i++] = getLsb(commandCode);
        txPacket[i++] = getMsb(commandCode);
        txPacket[i++] = getLsb(status);
        txPacket[i++] = getMsb(status);
        txPacket[i++] = getLsb(dataLength);
        txPacket[i++] = getMsb(dataLength);

        for (int j = 0; j < dataLength; ++j) {
            txPacket[i++] = data[j];
        }

        txPacket[i] = CRC_8.getCrc8_U(txPacket, dataLength + 8);
        return txPacket;
    }

    private byte getMsb(int u16) {
        return (byte)((u16 >> 8) & 0xff);
    }
    private byte getLsb(int u16) {
        return (byte)((u16) & 0xff);
    }


    public CommandType getCommandType() {
        switch (getMsb(commandCode)) {
            case 0x02: return CommandType.EVENT;
            case 0x04: return CommandType.UPDATE;
            default: return CommandType.REPLAY;
        }
    }


    public String getCommandString() {
        McuProtocol.McuCommands command = McuProtocol.McuCommands.fromInt(commandCode);
        return (command == null ? "0x" + String.format("%04X", commandCode) : command.toString());
    }

    public String getStatusString() {
        McuProtocol.McuStatus packetStatus = McuProtocol.McuStatus.fromInt(status);
        return (packetStatus == null ? "0x" + String.format("%04X", status) : packetStatus.toString());
    }

    public boolean isCommandNoReply() {
        return (getMsb(commandCode) == 0x02 || McuProtocol.McuCommands.REFRESH.equals(commandCode));
    }

    public String getDataString() {
        int dataLength = getDataLength();
        if (dataLength == 0) return "";

        StringBuilder sb = new StringBuilder(dataLength * 3); // Pre-allocate size
        for (int i = 0; i < dataLength; ++i) {
            sb.append(String.format("%02X ", data[i]));
        }
        return sb.toString().trim();
    }

    public int getPacketTimeoutMs() { return packetTimeoutMs; }
}
