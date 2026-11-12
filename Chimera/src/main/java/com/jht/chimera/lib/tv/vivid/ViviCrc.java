package com.jht.chimera.lib.tv.vivid;

public class ViviCrc {

    public static byte calCrc(final byte[] buffer, int length) {
        int _Temp = 0;
        int _Data = 0;
        int _CrcSum = 0;

        for (int i = 0; i < length; i++) {
            _Data = buffer[i] & 0xFF;

            _Temp = _Temp + _Data;
            _CrcSum = _Temp & 0xFF;
        }

        return (byte) _CrcSum;
    }
}
