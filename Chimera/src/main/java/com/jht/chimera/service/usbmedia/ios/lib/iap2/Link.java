package com.jht.chimera.service.usbmedia.ios.lib.iap2;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class Link {
    private WriteThread mWriteThread;

    private int initSequence;
    private int nextSequence;
    private int lastReceivedSequence;

    private Timer mTimer;
    private byte[] initData = {(byte) 0xFF, 0x55, 0x02, 0x00, (byte) 0xEE, 0x10};

    public Link(WriteThread writeThread) {
        mWriteThread = writeThread;
    }

    /**
     * Send FF 55 02 00 EE 10 to confirm the device supports iAP2 once a second until a response is received from the Apple device.
     *
     */
    public void init() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Packet packet = new Packet(initData, true);
                mWriteThread.addPacket(packet);
            }
        }, 0, 1000);
    }

    public void stopTimer() {
        if(mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }
    }

    public void sync() {
        initSequence = (int) (Math.random() * 255);
        nextSequence = initSequence;
        Packet packet = new Packet(this, Packet.ControlMask.SYN, 0x00, (byte) 0x00);

        // Link Synchronization Payload
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put((byte) 0x01);    // Link Version
        buffer.put((byte) 0x05);    // Max Number of Outstanding Packets
        buffer.put((byte) 0x10);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x04);
        buffer.put((byte) 0x0b);
        buffer.put((byte) 0x00);    // cumulative acknowledgement timeout MSB
        buffer.put((byte) 0xC8);    // cumulative acknowledgement timeout LSB
        buffer.put((byte) 0x1e);
        buffer.put((byte) 0x03);
        buffer.put(Sessions.SessionID.CONTROL.getId());                             // Session id
        buffer.put((byte) Sessions.SessionType.CONTROL_SESSION.ordinal());          // Session type
        buffer.put((byte) 0x01);                                                    // Session version
        buffer.put(Sessions.SessionID.FILE_TRANSFER.getId());                       // Session id
        buffer.put((byte) Sessions.SessionType.FILE_TRANSFER_SESSION.ordinal());    // Session type
        buffer.put((byte) 0x02);                                                    // Session version
        packet.setPayloadData(buffer.array());
        mWriteThread.addPacket(packet);
    }

    public int getNextSequence() {
        return nextSequence;
    }

    public void setNextSequence(int nextSequence) {
        this.nextSequence = nextSequence;
    }

    public int getLastReceivedSequence() {
        return this.lastReceivedSequence;
    }

    public void setLastReceivedSequence(int lastReceivedSequence) {
        this.lastReceivedSequence = lastReceivedSequence;
    }
}
