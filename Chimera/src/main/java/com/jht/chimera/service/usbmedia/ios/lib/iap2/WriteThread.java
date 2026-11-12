package com.jht.chimera.service.usbmedia.ios.lib.iap2;

import android.util.Log;

import com.jht.chimera.service.usbmedia.ios.IOSDeviceConnection;
import com.jht.chimera.service.usbmedia.ios.IOSDeviceManager;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class WriteThread implements Runnable {
    private static final String TAG = "WriteThread";

    /*private static final byte HID_OUTPUT_REPORT_ID = 0x14;
    private static final int OUTPUT_PACKET_SIZE = 194;*/
    private BlockingQueue<Packet> mPacketToSend = new LinkedBlockingQueue<>();
    private IOSDeviceConnection mConnection;
    private boolean mRunning;

    public WriteThread(IOSDeviceConnection connection) {
        mConnection = connection;
        start();
    }

    public void addPacket(Packet packet) {
        try {
            mPacketToSend.put(packet);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void start() {
        if(mRunning) {
            return;
        }

        mRunning = true;
        Thread t = new Thread(this);
        t.start();

    }

    public void stop() {
        mRunning = false;
    }

    @Override
    public void run() {
        try {
            while(mRunning) {
                Packet packet = mPacketToSend.take();
                write(packet);
            }

        } catch (InterruptedException e) {
            Log.w(TAG, e);
        }
    }

    private void write(Packet packet) {
        if(packet.isInitialize()) {
            ByteBuffer report = ByteBuffer.allocate(IOSDeviceManager.OUTPUT_PACKET_SIZE);
            report.put(IOSDeviceManager.HID_OUTPUT_REPORT_ID);
            report.put((byte) 0x00);
            report.put(packet.getAll());
            mConnection.write(report.array(), packet);
        } else {
            byte[] buffer = packet.getAll();
            int reportCount = (buffer.length / (IOSDeviceManager.OUTPUT_PACKET_SIZE - 2)) + 1;
            int index = 0;
            for(int i = 0; i < reportCount; i++) {
                int reportLength;
                if(buffer.length - index > IOSDeviceManager.OUTPUT_PACKET_SIZE - 2) {
                    reportLength = IOSDeviceManager.OUTPUT_PACKET_SIZE;
                } else {
                    reportLength = buffer.length - index + 2;
                }
//                Log.d(TAG, "write: report length = " + reportLength);
                ByteBuffer report = ByteBuffer.allocate(reportLength);

                report.put(IOSDeviceManager.HID_OUTPUT_REPORT_ID);
                // Link control
                byte linkControl = 0x00;
                if(reportCount == 1) {
                    report.put(linkControl);
                } else {
                    if(i > 0) {
                        linkControl |= 0x01;
                    }

                    if(i < reportCount - 1) {
                        linkControl |= 0x02;
                    }
//                    Log.d(TAG, "write: link control = " + String.format("%#04x", linkControl));
                    report.put(linkControl);
                }
//                Log.d(TAG, "write: offset = " + index + ", length = " + (reportLength - 2));
                report.put(buffer, index, reportLength - 2);
                index += (reportLength - 2);
//                Log.d(TAG, "write: " + Packet.byteArray2Hex(report.array()));
//                Log.d("ATS", "write: " + packet.toString());
                mConnection.write(report.array(), packet);
            }
        }

    }
}
