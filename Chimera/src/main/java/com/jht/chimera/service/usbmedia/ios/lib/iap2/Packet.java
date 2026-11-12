package com.jht.chimera.service.usbmedia.ios.lib.iap2;

import java.nio.ByteBuffer;

public class Packet {
    private static final String TAG = "Packet";

    private static final char START_BYTE = 0xFF5A;
    private static final int HEADER_SIZE = 9;

    private Link mLink;
    private char mLength;
    private ControlMask mControl;
    private char mSeqNum;
    private int mSequence;
    private byte mAckNum;
    private int mAck;
    private byte mSessionId;
    private byte mHeaderChecksum;
    private byte[] mPayloadData;
    private byte mDataChecksum;
    private boolean mIsInitialize = false;
    private Message mMessage;

    // File Transfer Session
    private int mFileTransId;
    private byte mDatagramId;

    public enum ControlMask {
        UNKNOWN((byte) 0x00),
        SYN((byte) 0x80),
        ACK((byte) 0x40),
        EAK((byte) 0x20),
        RST((byte) 0x10),
        SLP((byte) 0x08),
        SYNACK((byte) 0xC0);

        private byte mValue;

        ControlMask(byte value) {
            mValue = value;
        }

        public byte getValue() {
            return mValue;
        }

        public static ControlMask getType(byte value) {
            for(ControlMask c : ControlMask.values()) {
                if(c.getValue() == value) {
                    return c;
                }
            }

            return UNKNOWN;
        }
    }

    public Packet(){
        mIsInitialize = true;
        mControl = ControlMask.UNKNOWN;
    }

    public Packet(byte[] data, boolean initialize) {
        mIsInitialize = initialize;
        setPayloadData(data);
    }

    public Packet(Link link, ControlMask ctl, byte ack, byte sessionId) {
        mLink = link;
        mControl = ctl;
        mAckNum = ack;
        mSessionId = sessionId;
        mIsInitialize = false;
    }

    public Packet(Link link, ControlMask ctl, int ack, byte sessionId) {
        mLink = link;
        mControl = ctl;
        mAck = ack;
        mSessionId = sessionId;
        mIsInitialize = false;
    }

//    public char getSeqNum() {
//        return mSeqNum;
//    }

    public int getSequence() {
        return this.mSequence;
    }

    public void setPayloadData(byte[] data) {
        if(data == null) {
            return;
        }

        mPayloadData = new byte[data.length];
        System.arraycopy(data, 0, mPayloadData, 0, data.length);

//        if(mControl != null && (mControl == ControlMask.SYN || mControl == ControlMask.ACK)) {
//            mSeqNum = (char) (mLink.getNextSentPsn() + 1);
//            mLink.setNextSentPsn(mSeqNum);
//        }

    }

    public byte[] getData() {
        return mPayloadData;
    }

    public boolean isInitialize() {
        return mIsInitialize;
    }

    public ControlMask getControlByte() {
        return this.mControl;
    }



    public byte[] getAll() {
        ByteBuffer buffer = null;

        if(mIsInitialize) {
            return mPayloadData;
        } else {
            int len;
            if(mPayloadData != null && mPayloadData.length > 0) {
                len = HEADER_SIZE + mPayloadData.length + 1;
            } else {
                len = HEADER_SIZE;
            }
            mLength = (char) len;
            buffer = ByteBuffer.allocate(len);

            buffer.putChar(START_BYTE);
            buffer.putChar(mLength);
            buffer.put(mControl.getValue());
//            mSeqNum = mLink.getNextSentPsn();
//            buffer.put((byte) mSeqNum);
            if(mControl == ControlMask.SYN || (mPayloadData != null && mPayloadData.length > 0)) {
                mSequence = mLink.getNextSequence() + 1 > 255 ? 0 : mLink.getNextSequence() + 1;
            } else {
                mSequence = mLink.getNextSequence();
            }
            buffer.put((byte) mSequence);
//            buffer.put(mAckNum);
            buffer.put((byte) mAck);
            buffer.put(mSessionId);
            for(byte b : buffer.array()) {
                mHeaderChecksum += b;
            }
            mHeaderChecksum = (byte) (0 - mHeaderChecksum);
            buffer.put(mHeaderChecksum);
            if(mPayloadData != null) {
                for(byte b : mPayloadData) {
                    mDataChecksum += b;
                    buffer.put(b);
                }
                mDataChecksum = (byte) (0 - mDataChecksum);
                buffer.put(mDataChecksum);
            }

            mLink.setNextSequence(mSequence);

//            if(mControl == ControlMask.SYN || (mPayloadData != null && mPayloadData.length > 0)) {
////                mLink.setNextSentPsn((char) (mSeqNum + 1));
//                mLink.setNextSequence(mSequence + 1);
//            }


        }

        return buffer.array();
    }

    public void parse(byte[] data) {
        if(data == null || data.length < 9) {
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.put(data);
        buffer.position(0);

        if(buffer.getChar() != START_BYTE) {
            return;
        }

        mLength = buffer.getChar();
        if(data.length < mLength) {
            return;
        }

        mControl = ControlMask.getType(buffer.get());
//        mSeqNum = (char) buffer.get();
        mSequence = buffer.get() & 0xff;
//        mAckNum = buffer.get();
        mAck = buffer.get() & 0xff;
        mSessionId = buffer.get();
        mHeaderChecksum = buffer.get();

        if(mSessionId != 0) {
            mPayloadData = new byte[(int) mLength - HEADER_SIZE - 1];
            buffer.get(mPayloadData, 0, (int) mLength - HEADER_SIZE - 1);
            mDataChecksum = buffer.get();
//            Log.d(TAG, "parse: mDataChecksum = " + mDataChecksum);

            if(mSessionId == Sessions.SessionID.CONTROL.getId()) {
                mMessage = new Message();
                mMessage.parse(mPayloadData);
            } else if(mSessionId == Sessions.SessionID.FILE_TRANSFER.getId()) {
                mFileTransId = mPayloadData[0] & 0xff;
                mDatagramId = mPayloadData[1];
            }
        }

        mIsInitialize = false;


    }

    public Message getMessage() {
        return mMessage;
    }

    public byte getSessionId() {
        return mSessionId;
    }

    public int getFileTransId() {
        return mFileTransId;
    }

    public byte getDatagramId() {
        return mDatagramId;
    }

    @Override
    public String toString() {
//        return "iAP2 Packet:\n" +
//                "\tLength: " + (int) mLength + "\n" +
//                "\tControl Byte: " + mControl + "\n" +
//                "\tSequence: " + mSequence + "\n" +
//                "\tAck: " + mAck + "\n" +
//                "\tSession id: " + mSessionId + "\n" +
//                "\tHeader Checksum: " + (int) mHeaderChecksum + "\n" +
//                "\tPayload Data: " + byteArray2Hex(mPayloadData);
        return "Type: " + mControl + " Seq: " + mSequence + " Ack: " + mAck + " Length: " + (int) mLength;


    }

    public static String byteArray2Hex(byte[] data) {
        if(data == null) {
            return "";
        }
        StringBuffer formatedString = new StringBuffer();
        for(byte b: data) {
            formatedString.append(String.format("%#04x", b) + " ");
        }
        return formatedString.toString();
    }
}
