package com.jht.chimera.lib.tv.switft;

import java.util.List;

public class SwiftCrc {

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

    public static int calCrc(final List<Byte> buffer, int length) {
        int _Temp = 0;
        int _Data = 0;
        int _CrcSum = 0;

        for (int i = 0; i < length; i++) {
            _Data = (buffer.get(i)) & 0xff;

            _Temp = _Temp + _Data;
            _CrcSum = _Temp;
        }

        return _CrcSum;
    }
}
