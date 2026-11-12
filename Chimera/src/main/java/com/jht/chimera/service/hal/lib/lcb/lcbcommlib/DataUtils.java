package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class DataUtils {
    private static final String TAG = "DataUtils";

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
        return (float) (mantissa * Math.pow(10, exponent));
    }

    public static float bytesToFloat(byte b0, byte b1, byte b2, byte b3) {
        byte[] bytesDataToFloat = new byte[4];

	bytesDataToFloat[0] = b0;
	bytesDataToFloat[1] = b1;
	bytesDataToFloat[2] = b2;
	bytesDataToFloat[3] = b3;

	return ByteBuffer.wrap(bytesDataToFloat).getFloat();

    }

    private static int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size - 1)) != 0) {
            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
        }
        return unsigned;
    }

    public static int shortToInt(short s) {
        return s & 0xFFFF;
    }

    /*******************************************************************************
     * Function Name  : hexStringToByteArray
     * Description    : process hex string convert to bytes array
     * Input          : String hexString
     * Output         : None
     * Return         : byte[]
     *******************************************************************************/
    public static byte[] hexStringToByteArray(String hexString) {
        int len = (hexString == null ? 0 : hexString.length());
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }

        return data;
    }

    /*******************************************************************************
     * Function Name  : byteArrayToHexString
     * Description    : process bytes array convert to hex string
     * Input          : byte[] bytes
     * Output         : None
     * Return         : String
     *******************************************************************************/
    public static String byteArrayToHexString(byte[] bytes) {
        String rtnString = "";
        if (bytes != null) {
            for (byte b : bytes) {
                rtnString = rtnString + String.format("%02x", b);
            }
        }

        return rtnString;
    }

    /*******************************************************************************
     * Function Name  : intToOneByte
     * Description    : process int (0~255) convert to one byte
     * Input          : int (0~255)
     * Output         : None
     * Return         : byte
     *******************************************************************************/
    public static byte intToOneByte(int intValue) {
        byte rtnByte = (byte) 0x00;
        if ((intValue > 255) || (intValue < 0))
            rtnByte = (byte) 0x00;
        else
            rtnByte = (byte) (intValue & 0xFF);

        return rtnByte;
    }

    /*******************************************************************************
     * Function Name  : byteGetNthOfInt
     * Description    : process to get Nth byte of int
     * Input          : int , number of Nth byte(0-3)
     * Output         : None
     * Return         : byte
     *******************************************************************************/
    public static byte byteGetNthOfInt(int intValue, int number) {
        int inputValue = intValue;
        byte rtnByte = (byte) 0x00;
        switch (number) {
            case 0:
                inputValue = intValue;
                break;
            case 1:
                inputValue = inputValue >> 8;
                break;
            case 2:
                inputValue = inputValue >> 16;
                break;
            case 3:
                inputValue = inputValue >> 24;
                break;
        }
        rtnByte = (byte) (inputValue & 0xFF);

        return rtnByte;
    }

    /*******************************************************************************
     * Function Name  : byteGetNthOfInt
     * Description    : process to get Nth byte of int
     * Input          : int , number of Nth byte(0-3)
     * Output         : None
     * Return         : byte
     *******************************************************************************/
    public static byte[] charsToBytes (char[] chars) {
	   Charset cs = Charset.forName ("UTF-8");
	   CharBuffer cb = CharBuffer.allocate (chars.length);
	   cb.put (chars);
	   cb.flip ();
	   ByteBuffer bb = cs.encode (cb);
	  
	   return bb.array();

    }
}
