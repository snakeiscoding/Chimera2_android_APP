package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib;

public class UpdateCrc8 {

    static char[] CRC8_Table =
            {
                    0x00, 0x31, 0x62, 0x53, 0xC4, 0xF5, 0xA6, 0x97,
                    0xB9, 0x88, 0xDB, 0xEA, 0x7D, 0x4C, 0x1F, 0x2E,
            };

    public static byte doUpdateCrc8(byte[] buffer, int length) {
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
}

