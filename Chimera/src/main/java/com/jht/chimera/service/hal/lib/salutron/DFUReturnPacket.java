package com.jht.chimera.service.hal.lib.salutron;

import java.io.InvalidObjectException;

public class DFUReturnPacket {
    public static final int PACKET_SIZE = 6;
    protected int mExpectedPacketNum;
    protected int mPayloadLength;
    protected int mSeqNumber;
    protected int mHeaderChecksum;

    public DFUReturnPacket(byte[] packet) throws InvalidObjectException {
        if (packet.length != 6) {
            throw new InvalidObjectException("Packet length is invalid " + packet.length);
        } else {
            this.mExpectedPacketNum = packet[1];
            this.mPayloadLength = packet[2];
            this.mSeqNumber = packet[3];
            this.mHeaderChecksum = packet[4];
        }
    }

    public int getAckNumber() {
        return this.mExpectedPacketNum >> 3 & 7;
    }
}
