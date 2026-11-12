package com.jht.chimera.service.usbmedia.ios.lib.iap2;


import com.jht.chimera.service.usbmedia.ios.IOSDeviceConnection;

import java.nio.ByteBuffer;


public class ReadThread implements Runnable {
    private static final String TAG = "ReadThread";

    private IOSDeviceConnection mConnection;
    private IIAP2Callback mCallback;
    private boolean mRunning;
    private boolean mConnected;

    public ReadThread(IOSDeviceConnection connection, boolean connected, IIAP2Callback callback) {
        mConnection = connection;
        mConnected = connected;
        mCallback = callback;
        start();
    }

    private void start() {
        if(mRunning)
            return;

        mRunning = true;
        Thread t = new Thread(this);
        t.start();

    }

    public void stop() {
        mRunning = false;
//        mConnection.close();
    }

    @Override
    public void run() {
        while(mRunning && mConnected) {
            byte[] buffer = mConnection.read();
            if(buffer == null) {
                continue;
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(2);
            byteBuffer.put(buffer, 0, 2);
            byteBuffer.position(0);
            char start = byteBuffer.getChar();
            if(start == 0xFF5A) {
                Packet packet = new Packet();
                packet.parse(buffer);
//                Log.d("ATS", "read : " + packet.toString());
                parse(packet);
            } else if(start == 0xFF55) {
                mCallback.onInit(buffer);
            }
        }

//        mConnection.close();
    }

    private void parse(Packet packet) {
        if(packet.getControlByte() == Packet.ControlMask.SYNACK) {
            mCallback.onSynchronize(packet);
        } else if (packet.getControlByte() == Packet.ControlMask.RST) {
            mCallback.onReset();
        } else if(packet.getControlByte() == Packet.ControlMask.ACK) {
            if(packet.getSessionId() == Sessions.SessionID.CONTROL.getId()) {
                Message m = packet.getMessage();
                if (m == null) {
                    return;
                }
                Sessions.ControlMessageType messageType = Sessions.ControlMessageType.getType(m.getId());
                switch (messageType) {
                    case REQ_AUTHENTICATION_CERTIFICATE:
                        mCallback.onReqAuthCertificate(packet);
                        break;
                    case REQ_AUTHENTICATION_CHALLENGE_RESPONSE:
                        mCallback.onReqAuthChallengeResponse(packet);
                        break;
                    case AUTHENTICATION_FAILED:
                        mCallback.onAuthFailed(packet);
                        break;
                    case AUTHENTICATION_SUCCEEDED:
                        mCallback.onAuthSucceeded(packet);
                        break;
                    case START_IDENTIFICATION:
                        mCallback.onStartIdentification(packet);
                        break;
                    case IDENTIFICATION_ACCEPTED:
                        mCallback.onIdentificationAccepted(packet);
                        break;
                    case DEVICE_INFORMATION_UPDATE:
                        mCallback.onDeviceInfoUpdate(packet);
                        break;
                    case USB_DEVICE_MODE_AUDIO_INFORMATION:
                        mCallback.onUSBDeviceModeAudioInfo(packet);
                        break;
                    case POWER_UPDATE:
                        mCallback.onPowerUpdate(packet);
                        break;
                    case NOW_PLAYING_UPDATE:
                        mCallback.onNowPlayingUpdate(packet);
                        break;
                    case MEDIA_LIBRARY_INFORMATION:
                        mCallback.onMediaLibraryInfo(packet);
                        break;
                    case MEDIA_LIBRARY_UPDATE:
                        mCallback.onMediaLibraryUpdate(packet);
                        break;

                }
            } else if(packet.getSessionId() == Sessions.SessionID.FILE_TRANSFER.getId()) {
                FileTransferSession.DatagramType datagram = FileTransferSession.DatagramType.getDatagramType(packet.getDatagramId());
                switch (datagram) {
                    case SETUP:
                        mCallback.onSetupDatagram(packet);
                        break;
                    case FIRST_DATA:
                        mCallback.onFirstData(packet);
                        break;
                    case FIRST_AND_ONLY_DATA:
                        mCallback.onFirstAndOnlyData(packet);
                        break;
                    case DATA:
                        mCallback.onData(packet);
                        break;
                    case LAST_DATA:
                        mCallback.onLastData(packet);
                        break;
                    case CANCEL:
                        mCallback.onCancel(packet);
                        break;
                    case PAUSE:
                        break;

                }
            }
        }

    }

    public void setConnected(boolean connected) {
        mConnected = connected;
    }
}
