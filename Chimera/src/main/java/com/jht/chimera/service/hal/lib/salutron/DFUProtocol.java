package com.jht.chimera.service.hal.lib.salutron;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class DFUProtocol {
    public static final int MAX_DATA_PACKET_SIZE = 512;
    private  static final byte SLIP_END    = (byte) 0xC0;
    private static final byte SLIP_ESC     = (byte) 0xDB;
    private static final byte SLIP_ESC_END = (byte) 0xDC;
    private static final byte SLIP_ESC_ESC = (byte) 0xDD;

    public static byte[] encode(byte[] data) {
        ArrayList<Byte> buff = new ArrayList<>();
        buff.add(SLIP_END);
        for (byte datum : data) {
            switch (datum) {
                case SLIP_END:
                    buff.add(SLIP_ESC);
                    buff.add(SLIP_ESC_END);
                    break;
                case SLIP_ESC:
                    buff.add(SLIP_ESC);
                    buff.add(SLIP_ESC_ESC);
                    break;
                default:
                    buff.add(datum);
                    break;
            }

        }

        buff.add(SLIP_END);
        byte[] buffer = new byte[buff.size()];
        for(int i = 0; i < buffer.length; ++i) {
            buffer[i] = buff.get(i);
        }

        return buffer;

    }

    // from header to application image size
    public static DFUPacket buildStartPacket(ImageType type, long payloadLength) throws IOException {
        ByteBuffer buff = ByteBuffer.allocate(20);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        buff.putInt(3);
        buff.putInt(type.getValue());
        buff.putInt(type == ImageType.SOFTDEVICE ? (int) payloadLength : 0);
        buff.putInt(type == ImageType.BOOTLOADER ? (int) payloadLength : 0);
        buff.putInt(type == ImageType.APPLICATION ? (int) payloadLength : 0);
        return new DFUPacket(1, 1, 2, 1, 14, buff.array());
    }

    public static DFUPacket buildInitPacket(int deviceType, int deviceRevision, long appVer, int[] softDevices, long imageCRC) throws IOException {
        ByteBuffer buff = ByteBuffer.allocate(18 + softDevices.length * 2);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        buff.putInt(1);
        buff.putChar((char) deviceType);
        buff.putChar((char) deviceRevision);
        buff.putInt((int) appVer);
        buff.putChar((char) softDevices.length);
        for (int i = 0; i < softDevices.length; i++) {
            buff.putChar((char) softDevices[i]);
        }
        buff.putChar((char) imageCRC);
        buff.putChar((char) 0);
        return new DFUPacket(1, 1, 3, 2, 14, buff.array());
    }

    public static DFUPacket buildDataPacket(byte[] payload, int offset, int seq) throws IOException {
        int pageSize = payload.length - offset;
        if (pageSize > MAX_DATA_PACKET_SIZE) {
            pageSize = MAX_DATA_PACKET_SIZE;
        } else if (pageSize <= 0) {
            return null;
        }

        ByteBuffer buff = ByteBuffer.allocate(4 + pageSize);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        buff.putInt(4);
        buff.put(ByteBuffer.wrap(payload, offset, pageSize));
        return new DFUPacket(1, 1, seq + 1, seq, 14, buff.array());
    }

    public static DFUPacket buildStopPacket(int seq) throws IOException {
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        buff.putInt(5);
        return new DFUPacket(1, 1, seq + 1, seq, 14, buff.array());
    }
}
