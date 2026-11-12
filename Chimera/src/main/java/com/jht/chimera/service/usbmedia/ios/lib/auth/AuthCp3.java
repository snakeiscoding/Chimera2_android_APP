package com.jht.chimera.service.usbmedia.ios.lib.auth;

import android.os.SystemClock;
import android.util.Log;

import com.jht.chimera.drivers.i2c.I2C;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AuthCp3 implements AutoCloseable {
    private enum Registers {
        DEVICE_ID(0x04),
        AUTH_CONTROL_AND_STATUS(0x10),
        CHALLENGE_RESPONSE_DATA_LENGTH(0x11),
        CHALLENGE_RESPONSE_DATA(0x12),
        CHALLENGE_DATA_LENGTH(0x20),
        CHALLENGE_DATA(0x21),
        ACCESSORY_CERT_DATA_LENGTH(0x30),
        ACCESSORY_CERT_DATA_PAGE01(0x31);

        private int address;

        Registers(int address) {
            this.address = address;
        }

        public int getAddress() {
            return address;
        }
    }

    private enum Status {
        ACCESSORY_CHALLENGE_RESPONSE_GENERATED_SUCCESS(0x10),
        ERROR(0x80);

        private int value;

        Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    private static final String TAG = "AuthCp3";
    private static final byte WRITE_ADDRESS = 0x20;
    private static final byte READ_ADDRESS = 0x21;

    private int mFd = -1;

    public AuthCp3(String path) {
        try {
            close(); // seems like mFd would need to be static for this to do anything?

            mFd = I2C.open(path);
            Log.i(TAG, "AuthCp3: open " + path + " result: " + mFd);
        } catch (UnsatisfiedLinkError|NoClassDefFoundError e) {
            Log.e(TAG, "Native code missing", e);
        } catch (Exception e) {
            Log.e(TAG, "Constructor threw", e);
        }
    }

    public boolean isConnected() { return mFd >= 0; }

    @Override
    public void close() {
        if (isConnected()) {
            try { I2C.close(mFd); }
            catch (UnsatisfiedLinkError|NoClassDefFoundError|Exception ignored) {}
            mFd = -1;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        // finalize is considered bad form, consider an open/close pattern?
        close(); // making sure native resource is released at some point
        super.finalize();
    }

    public byte[] getAccessoryCertificate() {
        Log.d(TAG, "enter getAccessoryCertificate()");
        List<Byte> data = new ArrayList<>();
        int len = 0;
        for (int i = 0; i < 10; i++) {
            len = getAccessoryCertificateLen();
            if (len > 0) {
                break;
            }
//            SystemClock.sleep(500);
        }
//        int len = getAccessoryCertificateLen();
        Log.d(TAG, "getAccessoryCertificate: Accessory Certificate Data Length: " + len);
        int page = len / 128;
        if(len % 128 != 0) {
            page++;
        }

        for(int i = 0; i < page; i++) {
            byte[] buffer = new byte[128];
            int result = read(Registers.ACCESSORY_CERT_DATA_PAGE01.getAddress() + i, buffer);
            if(result < 0) {
                continue;
            }

            for(byte b : buffer) {
                data.add(b);
            }
        }

        int dataSize = data.size();
        byte[] dataArray = new byte[dataSize];
        for(int i = 0; i < data.size(); i++) {
            dataArray[i] = data.get(i);
        }

        return dataArray;
    }

    private int getAccessoryCertificateLen() {
        byte[] value = new byte[2];
        int result = read(Registers.ACCESSORY_CERT_DATA_LENGTH.getAddress(), value);
        if(result < 0) {
            return result;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.put(value);
        byteBuffer.position(0);
        return (int) byteBuffer.getChar();
    }

    public byte[] getChallengeResponse(byte[] challengeData) {
        if(write(Registers.CHALLENGE_DATA.getAddress(), challengeData) < 0) {
            Log.w(TAG, "getChallengeResponse: write challenge data failed.");
            return null;
        }

        // initiate challenge response computation
        byte[] data = {0x01};
        if(write(Registers.AUTH_CONTROL_AND_STATUS.getAddress(), data) < 0) {
            Log.w(TAG, "getChallengeResponse: write failed.");
            return null;
        }

        byte[] status = new byte[1];
        boolean foundStatus = false;
        // Wait for okay
        for(int i = 0; i < 30; i++) {
            Log.d(TAG, "getChallengeResponse: i: " + i);
            SystemClock.sleep(500);
            if(read(Registers.AUTH_CONTROL_AND_STATUS.getAddress(), status) > 0) {
                foundStatus = true;
                break;
            }

        }

        if(!foundStatus) {
            Log.w(TAG, "getChallengeResponse: read failed.");
            return null;
        }

        // check status
        if((status[0] & Status.ACCESSORY_CHALLENGE_RESPONSE_GENERATED_SUCCESS.getValue()) != Status.ACCESSORY_CHALLENGE_RESPONSE_GENERATED_SUCCESS.getValue()) {
            Log.w(TAG, "getChallengeResponse: challenge response generation failed. " + String.format("%#04x", status[0])); //NON-NLS
            return null;
        }

        byte[] responseLen = new byte[2];
        if(read(Registers.CHALLENGE_RESPONSE_DATA_LENGTH.getAddress(), responseLen) < 0) {
            Log.w(TAG, "getChallengeResponse: read challenge response data length failed.");
            return null;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.put(responseLen);
        byteBuffer.position(0);
        int challengeResponseLen = (int) byteBuffer.getChar();
        Log.d(TAG, "getChallengeResponse: challenge response data length: " + challengeResponseLen);
        if(challengeResponseLen != 64) {
            Log.w(TAG, "getChallengeResponse: challenge response data length error.");
            return null;
        }

        byte[] responseData = new byte[challengeResponseLen];
        if(read(Registers.CHALLENGE_RESPONSE_DATA.getAddress(), responseData) < 0) {
            Log.w(TAG, "getChallengeResponse: read challenge response data failed.");
            return null;
        }

        return responseData;
    }

    /**
     * Reading data from Apple Authentication 3.0 Coprocessor
     *
     * @param register the register address from which will read
     * @param buffer the buffer into which the data is read
     * @return the total number of bytes read into the buffer or negative number if something wrong
     */
    private int read(int register, byte[] buffer) {
        if(mFd < 0) {
            return mFd;
        }

        I2C.write(mFd, WRITE_ADDRESS, register, buffer, 0);
        SystemClock.sleep(100);
        return I2C.read(mFd, READ_ADDRESS, buffer, buffer.length);
    }

    /**
     * Writing data to Apple Authentication 3.0 Coprocessor
     *
     * @param register the register address into which data is written
     * @param value the data into which is written
     * @return the total number of bytes written into the register or negative number if something wrong
     */
    private int write(int register, byte[] value) {
        if(mFd < 0) {
            return mFd;
        }

        return I2C.write(mFd, WRITE_ADDRESS, register, value, value.length);
    }
}
