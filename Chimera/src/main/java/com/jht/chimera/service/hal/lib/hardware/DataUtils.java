/******************************************************************************
 *
 *  Copyright (C) 2012 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
package com.jht.chimera.service.hal.lib.hardware;

public class DataUtils {
    public static int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    public static int unsignedBytesToInt(byte lsb, byte msb) {
        return (unsignedByteToInt(lsb) + (unsignedByteToInt(msb) << 8));
    }

    public static int unsignedBytesToInt(byte b0, byte b1, byte b2, byte b3) {
        return (unsignedByteToInt(b0)
                + (unsignedByteToInt(b1) << 8))
                + (unsignedByteToInt(b2) << 16)
                + (unsignedByteToInt(b3) << 24);
    }

    public static float bytesToFloat(byte lsb, byte msb) {
        int mantissa = unsignedByteToInt(lsb) + ((unsignedByteToInt(msb) & 0x0F) << 8);
        mantissa = unsignedToSigned(mantissa, 12);
        int exponent = unsignedByteToInt(msb) >> 4;
        exponent = unsignedToSigned(exponent, 4);
        return (float)(mantissa * Math.pow(10, exponent));
    }

    public static float bytesToFloat(byte b0, byte b1, byte b2, byte b3) {
        int mantissa = unsignedByteToInt(b0)
                + (unsignedByteToInt(b1) << 8)
                + (unsignedByteToInt(b2) << 16);
        mantissa = unsignedToSigned(mantissa, 24);
        return (float)(mantissa * Math.pow(10, b3));
    }

    private static int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size-1)) != 0) {
            unsigned = -1 * ((1 << size-1) - (unsigned & ((1 << size-1) - 1)));
        }
        return unsigned;
    }
}
