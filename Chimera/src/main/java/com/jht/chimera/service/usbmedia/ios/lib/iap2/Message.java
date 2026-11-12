package com.jht.chimera.service.usbmedia.ios.lib.iap2;

import java.nio.ByteBuffer;
import java.util.List;

public class Message {
    private static final char SOM = 0x4040;
    public static final int HEADER_SIZE = 6;

    private char mId;
    private char mLength;
    private byte[] mData;
    private Parameter mParameter;
    private List<Parameter> mParameters;

    public Message(){}

    public Message(char id) {
        mId = id;
        mLength = HEADER_SIZE;
    }

    public void setParameter(Parameter parameter) {
        mParameter = parameter;
        mLength = (char) (mLength + mParameter.getLength());
    }

    public void setParameters(List<Parameter> parameters) {
        mParameters = parameters;
        for(Parameter p : parameters) {
            mLength = (char) (mLength + p.getLength());
        }
    }

    public byte[] getAll() {
        ByteBuffer buffer = ByteBuffer.allocate(mLength);
        buffer.putChar(SOM);
        buffer.putChar(mLength);
        buffer.putChar(mId);
        if(mParameters != null) {
            for(Parameter p : mParameters) {
                buffer.put(p.getAll());
            }
        }

//        if(mParameter != null) {
//            buffer.put(mParameter.getAll());
//        }

        return buffer.array();
    }

    public void parse(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.put(data);
        buffer.position(0);
        if(buffer.getChar() != SOM) {
            return;
        }
        mLength = buffer.getChar();
        mId = buffer.getChar();
        if(mLength > HEADER_SIZE) {
            mData = new byte[mLength - HEADER_SIZE];
            buffer.get(mData, 0, mLength - HEADER_SIZE);
        }

        /*ByteBuffer headerBuffer = ByteBuffer.allocate(HEADER_SIZE);
        headerBuffer.put(data, 0, HEADER_SIZE);
        headerBuffer.position(0);
        if(headerBuffer.getChar() != SOM) {
            return;
        }

        mLength = headerBuffer.getChar();
        mId = headerBuffer.getChar();

        if(mLength > HEADER_SIZE) {
            mParameters = new ArrayList<>();
            ByteBuffer dataBuffer = ByteBuffer.allocate(mLength - HEADER_SIZE);
            dataBuffer.put(data, HEADER_SIZE, mLength - HEADER_SIZE);
            dataBuffer.position(0);
            byte[] allParameterBytes = dataBuffer.array();
            int parameterLen = dataBuffer.getChar();
            boolean parsing = true;
            int offset = 0;
            while(parsing) {
                byte[] parameterByte = new byte[parameterLen];
                System.arraycopy(allParameterBytes, offset, parameterByte, 0, parameterLen);
                mParameter = new Parameter();
                mParameter.parse(parameterByte);
                mParameters.add(mParameter);
                offset+=parameterLen;
                parameterLen = mLength - HEADER_SIZE - parameterLen;
                if(parameterLen <= 0) {
                    parsing = false;
                }
            }

        }*/

//        ByteBuffer buffer = ByteBuffer.allocate(data.length);
//        buffer.put(data);
//        buffer.position(0);
//
//        if(buffer.getChar() != SOM) {
//            return;
//        }
//
//        mLength = buffer.getChar();
//        mId = buffer.getChar();
//
//        if(mLength > HEADER_SIZE) {
//            byte[] parameterData = new byte[mLength - HEADER_SIZE];
//            buffer.get(parameterData, 0, mLength - HEADER_SIZE);
//            mParameter = new Parameter();
//            mParameter.parse(parameterData);
//            mParameters.add(mParameter);
//
//        }

    }

    public char getLength() {
        return mLength;
    }

    public char getId() {
        return mId;
    }

    public Parameter getParameter() {
        return mParameter;
    }

    public List<Parameter> getParameters() {
        return mParameters;
    }

    public byte[] getData() {
        return mData;
    }
}
