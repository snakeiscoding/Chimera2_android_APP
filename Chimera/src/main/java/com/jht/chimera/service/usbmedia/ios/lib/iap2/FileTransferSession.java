package com.jht.chimera.service.usbmedia.ios.lib.iap2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class FileTransferSession {
    public enum DatagramType {
        SETUP((byte) 0x04),
        START((byte) 0x01),
        FIRST_DATA((byte) 0x80),
        FIRST_AND_ONLY_DATA((byte) 0xC0),
        DATA((byte) 0x00),
        LAST_DATA((byte) 0x40),
        CANCEL((byte) 0x02),
        PAUSE((byte) 0x03),
        SUCCESS((byte) 0x05),
        FAILURE((byte) 0x06),
        UNKNOWN((byte) 0xFF);

        private byte id;
        DatagramType(byte id) {
            this.id = id;
        }

        public byte getId() {
            return id;
        }

        public static DatagramType getDatagramType(byte id) {
            DatagramType type = UNKNOWN;
            for(DatagramType t : DatagramType.values()) {
                if(t.getId() == id) {
                    type = t;
                    break;
                }
            }

            return type;
        }
    }

    private Context mContext;
    private Link mLink;
    private WriteThread mWriteThread;
    private int mFileTransferId;
    private byte[] mFileData;
    private int mPos;

    public FileTransferSession(Context context, Link link, WriteThread writeThread, int fileTransId, int fileSize) {
        mContext = context;
        mLink = link;
        mWriteThread = writeThread;
        mFileTransferId = fileTransId;
        mFileData = new byte[fileSize];
    }

    public void sendStartDatagram() {
        Packet packet = new Packet(mLink, Packet.ControlMask.ACK, mLink.getLastReceivedSequence(), Sessions.SessionID.FILE_TRANSFER.getId());
        byte[] data = new byte[2];
        data[0] = (byte) mFileTransferId;
        data[1] = DatagramType.START.getId();
        packet.setPayloadData(data);
        mWriteThread.addPacket(packet);
    }

    public void setFileTransferId(int fileTransId) {
        mFileTransferId = fileTransId;
    }

    public int getFileTransferId() {
        return mFileTransferId;
    }

    public void setFileSize(int fileSize) {
        mFileData = new byte[fileSize];
    }

    public void setFileData(int fileId, byte[] fileData) {
        if(fileId != mFileTransferId) {
            return;
        }

        System.arraycopy(fileData, 0, mFileData, mPos, fileData.length);
        mPos += fileData.length;
    }

    public byte[] getFileData() {
        return mFileData;
    }

    public Bitmap getArtwork() {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inPreferredConfig = Bitmap.Config.RGB_565;
////        options.inJustDecodeBounds = true;
//        options.inSampleSize = 2;
//        options.inJustDecodeBounds = false;
//        return BitmapFactory.decodeByteArray(mFileData, 0, mFileData.length, options);
        return BitmapFactory.decodeByteArray(mFileData, 0, mFileData.length);
    }

    public void sendSuccessDatagram() {
        Packet packet = new Packet(mLink, Packet.ControlMask.ACK, mLink.getLastReceivedSequence(), Sessions.SessionID.FILE_TRANSFER.getId());
        byte[] data = new byte[2];
        data[0] = (byte) mFileTransferId;
        data[1] = DatagramType.SUCCESS.getId();
        packet.setPayloadData(data);
        mWriteThread.addPacket(packet);
    }

    public void acknowledge(Packet packet) {
        mLink.setLastReceivedSequence(packet.getSequence());
        Packet packetToWrite = new Packet(mLink, Packet.ControlMask.ACK, mLink.getLastReceivedSequence(), (byte) 0x00);
        mWriteThread.addPacket(packetToWrite);
    }
}
