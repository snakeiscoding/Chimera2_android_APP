package com.jht.chimera.service.hal.lib.io.iocommlib;

public class CRC_8 {

    private static final int[] crc8_Table = {0x00,0x5A,0xB4,0xEE,0x32,0x68,0x86,0xDC,0x64,0x3E,0xD0,0x8A,0x56,0x0C,0xE2,0xB8};
    private static int ByteToUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    // static function for calculating CRC of given array all at once
    public static byte getCrc8_U(byte[] data, int length) {
        int ptrIndex = 0;
        byte highNibble_U, dataTemp_U, newCrc_U = 0;

        while(length-- != 0) {
            dataTemp_U = data[ptrIndex];
            highNibble_U = (byte)(ByteToUnsignedInt(newCrc_U) / 16);
            newCrc_U = (byte)(ByteToUnsignedInt(newCrc_U) << 4);
            newCrc_U = (byte)(ByteToUnsignedInt(newCrc_U) ^ crc8_Table[ByteToUnsignedInt(highNibble_U) ^ ByteToUnsignedInt(dataTemp_U) / 16]);

            highNibble_U = (byte)(ByteToUnsignedInt(newCrc_U) / 16);
            newCrc_U = (byte)(ByteToUnsignedInt(newCrc_U) << 4);
            newCrc_U = (byte)(ByteToUnsignedInt(newCrc_U) ^ crc8_Table[ByteToUnsignedInt(highNibble_U) ^ ByteToUnsignedInt(dataTemp_U) & 0x0F]);
            ptrIndex++;
        }

//            Log.d("McuPacketManager","CRC Code: " + String.format("%X",newCrc_U));
        return newCrc_U;
    }

    // for computing crc one byte at a time using an instance of CRC_8
    private byte crc = 0;
    public void reset() { crc = 0; }
    public void feedByte(byte data) {
        byte highNibble_U;
        highNibble_U = (byte)(ByteToUnsignedInt(crc) / 16);
        crc = (byte)(ByteToUnsignedInt(crc) << 4);
        crc = (byte)(ByteToUnsignedInt(crc) ^ crc8_Table[ByteToUnsignedInt(highNibble_U) ^ ByteToUnsignedInt(data) / 16]);

        highNibble_U = (byte)(ByteToUnsignedInt(crc) / 16);
        crc = (byte)(ByteToUnsignedInt(crc) << 4);
        crc = (byte)(ByteToUnsignedInt(crc) ^ crc8_Table[ByteToUnsignedInt(highNibble_U) ^ ByteToUnsignedInt(data) & 0x0F]);
    }
    public byte getCrc() { return crc; }

}
