package com.jht.chimera.service.usbmedia.ios.lib.iap2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Parameter {
    private static final int HEADER_SIZE = 4;
    private char mLength;
    private char mId;
    private byte[] mData;
    private List<Byte> mDatas;

    public Parameter(){
        mLength = HEADER_SIZE;
        mDatas = new ArrayList<>();
    }

    public Parameter(char id) {
        this();
        mId = id;
//        mLength = HEADER_SIZE;
    }

    public void setData(byte[] data) {
        /*if(data != null) {
            mData = new byte[data.length];
            System.arraycopy(data, 0, mData, 0, data.length);
            mLength = (char) (mLength + data.length);
        }*/
        if(data != null) {
            for(byte b : data) {
                mDatas.add(b);
                mLength++;
            }
        }
    }

    public byte[] getAll() {
        ByteBuffer buffer = ByteBuffer.allocate(mLength);
        buffer.putChar(mLength);
        buffer.putChar(mId);
        for(byte b : mDatas) {
            buffer.put(b);
        }
//        if(mData != null) {
//            buffer.put(mData);
//        }
        return buffer.array();
    }

    public void parse(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.put(data);
        buffer.position(0);

        mLength = buffer.getChar();
        mId = buffer.getChar();
        if(mLength > HEADER_SIZE) {
            mData = new byte[mLength - HEADER_SIZE];
            buffer.get(mData, 0, mLength - HEADER_SIZE);
        }

    }

    public byte[] getData() {
        return mData;
    }

    public char getLength() {
        return mLength;
    }

    public char getId() {
        return mId;
    }
}
