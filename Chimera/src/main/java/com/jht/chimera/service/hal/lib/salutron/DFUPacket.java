package com.jht.chimera.service.hal.lib.salutron;

import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DFUPacket {
    @NonNls private static final String TAG = "DFUPacket";
    private int reliablePacket;
    private int integrityCheck;
    private int ackNumber;
    private int seqNumber;
    private int packetType;
    private byte[] payload;

    public DFUPacket(int reliablePacket, int integrityCheck, int ackNumber, int seqNumber, int packetType, byte[] payload) {
        this.reliablePacket = reliablePacket;
        this.integrityCheck = integrityCheck;
        this.ackNumber = ackNumber;
        this.seqNumber = seqNumber;
        this.packetType = packetType;
        this.payload = payload;
    }

    public int getPayloadLength() {
        return this.payload.length & 15;
    }

    public int getPayloadLengthContinued() {
        return this.payload.length >> 4;
    }

    public int getAckNumber() {
        return this.ackNumber;
    }

    public int getSeqNumber() {
        return this.seqNumber;
    }

    private byte byte2() {
        return (byte)((this.reliablePacket & 1) << 7 | (this.integrityCheck & 1) << 6 | (this.ackNumber & 7) << 3 | this.seqNumber & 7);
    }

    private byte byte3() {
        return (byte)((this.getPayloadLength() & 15) << 4 | this.packetType & 15);
    }

    private byte byte4() {
        return (byte)(this.getPayloadLengthContinued() & 255);
    }

    private byte[] createHeader() {
        return new byte[]{this.byte2(), this.byte3(), this.byte4()};
    }

    public byte[] toByteArray() {
        try {
            byte[] header = this.createHeader();
            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            buff.write(header);
            buff.write(checksum8(header));
            buff.write(this.payload);
            return buff.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    public static byte checksum8(byte[] data) {
        int cs = 0;
        byte[] var2 = data;
        int var3 = data.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            byte d = var2[var4];
            cs += d;
        }

        cs &= 255;
        cs ^= 255;
        ++cs;
        return (byte)(cs & 255);
    }

    public static byte[] crc16(byte[] data) {
        return crc16(data, data.length);
    }

    public static byte[] crc16(byte[] data, int length) {
        char crc = '\uffff';

        for(int offset = 0; length > 0; --length) {
            byte x = (byte)(crc >> 8 ^ data[offset]);
            byte a = (byte)(x >> 4 & 15);
            x ^= a;
            char b = (char)(crc << 8);
            char c = (char)(x << 12);
            char d = (char)(x << 5 & 8191);
            char e = (char)(x & 255);
            crc = (char)(b ^ c ^ d ^ e);
            ++offset;
        }

        return new byte[]{(byte)(crc & -1), (byte)(crc >> 8 & -1)};
    }
}
