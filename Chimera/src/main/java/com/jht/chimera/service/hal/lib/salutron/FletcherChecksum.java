package com.jht.chimera.service.hal.lib.salutron;

public class FletcherChecksum {

    public static short fletcher16(byte[] data) {
        byte sum1 = 0;
        byte sum2 = 0;
        short modulus = 255;

        for (int i = 0; i < data.length; i++) {
            sum1 = (byte) (((sum1 & 0xFF) + (data[i] & 0xFF)) % modulus);
            sum2 = (byte) (((sum2 & 0xFF) + (sum1 & 0xFF)) % modulus);
        }
        return (short) (((sum2 & 0xFF) << 8) | (sum1 & 0xFF));
    }


    public static byte[] check_bytes(byte... data) {
        short csum;
        byte f0, f1;
        short[] temp = new short[2];
        byte[] checkSum = new byte[2];

        csum = fletcher16(data);
        f0 = (byte) (csum & 0xFF);
        f1 = (byte) ((csum >> 8) & 0xFF);

        temp[0] = (short) (0xFF - (((f0 & 0xFF) + (f1 & 0xFF)) % 0xFF));
        temp[1] = (short) (0xFF - (((f0 & 0xFF) + (temp[0] & 0xFF))) % 0xFF);

        checkSum[0] = (byte) temp[0];
        checkSum[1] = (byte) temp[1];

//        Log.d("CheckSumTest", "sum: " + String.format("%X",csum) + " c0: " + String.format("%X",checkSum[0]) + " c1: " + String.format("%X",checkSum[1]));

        return checkSum;

    }

}
