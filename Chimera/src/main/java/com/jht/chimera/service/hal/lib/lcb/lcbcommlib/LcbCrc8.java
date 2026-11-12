package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

public class LcbCrc8 {

    private static final int[] CRC8_Table =
            {
                    0x00, 0x31, 0x62, 0x53, 0xC4, 0xF5, 0xA6, 0x97,
                    0xB9, 0x88, 0xDB, 0xEA, 0x7D, 0x4C, 0x1F, 0x2E,
            };

    public static byte doLcbCrc8(byte[] buffer, int length) {
        int by_CRC = 0;
        int by_Temp = 0;
        int by_Data = 0;

        for (int by_Loop = 0; by_Loop < length; by_Loop++) {
            by_Data = buffer[by_Loop] & 0xFF;

            by_Temp = by_CRC >> 4;
            by_CRC <<= 4;
            by_CRC &= 0x00ff;
            by_CRC ^= CRC8_Table[by_Temp ^ (by_Data >>> 4)];

            by_Temp = (by_CRC >> 4);
            by_CRC <<= 4;
            by_CRC &= 0x00ff;
            by_CRC ^= CRC8_Table[by_Temp ^ (by_Data & 0x0F)];
        }

        return (byte) by_CRC;
    }

    // for computing crc one byte at a time using an instance of LcbCrc8
    private int crc = 0;
    public void reset() { crc = 0; }
    public void feedByte(byte data) {
        int by_Data = data & 0xFF;
        int by_Temp = crc >> 4;
        crc <<= 4;
        crc &= 0x00ff;
        crc ^= CRC8_Table[by_Temp ^ (by_Data >>> 4)];

        by_Temp = (crc >> 4);
        crc <<= 4;
        crc &= 0x00ff;
        crc ^= CRC8_Table[by_Temp ^ (by_Data & 0x0F)];
    }
    public byte getCrc() { return (byte) (crc & 0xFF); }

}
