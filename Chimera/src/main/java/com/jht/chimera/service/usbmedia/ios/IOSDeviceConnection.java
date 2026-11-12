package com.jht.chimera.service.usbmedia.ios;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;
import android.util.SparseIntArray;

import com.jht.chimera.service.usbmedia.ios.lib.iap2.Packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class IOSDeviceConnection {
    private static final String TAG = "IOSDeviceConnection";

    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private SparseIntArray mInputReportLengths = new SparseIntArray();
    private UsbInterface mUsbInterface;
    private UsbEndpoint mEpIn;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbRequest mUsbRequest;
    private int mMaxSize;
    private int totalWrite;
    private int flag;
    private boolean mOpen;

    public IOSDeviceConnection(UsbManager usbManager, UsbDevice usbDevice) {
        mUsbManager = usbManager;
        mUsbDevice = usbDevice;

        mInputReportLengths.put(1, 6);
        mInputReportLengths.put(2, 10);
        mInputReportLengths.put(3, 14);
        mInputReportLengths.put(4, 18);
        mInputReportLengths.put(5, 26);
        mInputReportLengths.put(6, 50);
        mInputReportLengths.put(7, 96);
        mInputReportLengths.put(8, 194);
        mInputReportLengths.put(9, 258);
        mInputReportLengths.put(10, 386);
        mInputReportLengths.put(11, 514);
        mInputReportLengths.put(12, 768);

    }

    public boolean open() {
        for(int i = 0; i < mUsbDevice.getInterfaceCount(); i++) {
            UsbInterface intf = mUsbDevice.getInterface(i);
            if(intf.getInterfaceClass() == UsbConstants.USB_CLASS_HID
                    && intf.getInterfaceSubclass() == 0
                    && intf.getInterfaceProtocol() == 0) {
                mUsbInterface = intf;
                break;
            }
        }

        for(int i = 0; i < mUsbInterface.getEndpointCount(); i++) {
            UsbEndpoint ep = mUsbInterface.getEndpoint(i);
            if(mUsbInterface.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                if(ep.getDirection() == UsbConstants.USB_DIR_IN) {
                    mEpIn = mUsbInterface.getEndpoint(i);
                    mMaxSize = mEpIn.getMaxPacketSize();
                    break;
                }
            }
        }

        if (mUsbInterface != null) {
            UsbDeviceConnection connection;
            if(mUsbManager.hasPermission(mUsbDevice)) {
                connection = mUsbManager.openDevice(mUsbDevice);
                if (connection == null) {
                    return false;
                }

                if (connection.claimInterface(mUsbInterface, true)) {
                    Log.d(TAG, "claim HID interface succeeded.");

                    mUsbDeviceConnection = connection;
                    mUsbRequest = new UsbRequest();
                    mUsbRequest.initialize(mUsbDeviceConnection, mEpIn);
                } else {
                    Log.e(TAG, "claim HID interface failed.");
                    connection.close();
                    return false;
                }
            } else {
                Log.d(TAG, "Fail to get permission from device with vendor id [" + mUsbDevice.getVendorId() + "]");
                return false;
            }
        }
        mOpen = true;
        return true;
    }

    public void write(byte[] buffer, Packet packet) {
        if(mUsbDeviceConnection != null) {
            synchronized (this) {
                boolean resend = false;
                totalWrite++;
                int rtn = mUsbDeviceConnection.controlTransfer(0x21, 0x09, 0x0214, 0x02, buffer, buffer.length, 0);

                if (rtn < 0) {
                    resend = true;
                    flag++;
                }
//                Log.d("ATS", "write: " + packet.toString() + ", length of data transferred: " + rtn + "(" + flag + "/" + totalWrite + ")");
                if(resend) {
//                    Log.d(TAG, "write: resend " + packet.toString() + ", length of data transferred: " + rtn + "(" + flag + "/" + totalWrite + ")");
                    write(buffer, packet);
                }
            }

        }
    }

    public byte[] read() {
        boolean doLoop;
        List<Byte> reportBytes = new ArrayList<>();
        if(!mOpen) {
            return null;
        }
        do {
            List<Byte> buffer = getHIDReport();
            if(buffer.size() > 0) {
                if(buffer.get(1) != 0) {
                    doLoop = (buffer.get(1) & 0x02) == 0x02;
                } else {
                    doLoop = false;
                }

                reportBytes.addAll(buffer.subList(2, buffer.size()));
            } else  {
                return null;
            }
        } while(doLoop);

        byte[] rtn = new byte[reportBytes.size()];
        for(int i = 0; i < reportBytes.size(); i++) {
            rtn[i] = reportBytes.get(i);
        }
        return rtn;
    }

    private List<Byte> getHIDReport() {
        List<Byte> buffers = new ArrayList<>();
        boolean doLoop = false;
        byte reportId;
        int reportLength = 0;
        int readBytes = 0;

        ByteBuffer buffer = ByteBuffer.allocate(mMaxSize);
        do {
            if (!mOpen) {
                return buffers;
            }

            if(mUsbRequest == null) {
                return buffers;
            }
            mUsbRequest.queue(buffer, mMaxSize);
            if(mUsbDeviceConnection == null) {
                return buffers;
            }
            if (mUsbDeviceConnection.requestWait() == mUsbRequest) {
                byte[] byteArray = buffer.array();
                for (byte b : byteArray) {
                    buffers.add(b);
                }

                if (!doLoop) {
                    reportId = buffers.get(0);
                    if (mInputReportLengths.get(reportId) != 0) {
                        reportLength = mInputReportLengths.get(reportId);
                    }
                }

                readBytes += mMaxSize;
                if (reportLength <= mMaxSize) {
                    doLoop = false;
                } else {
                    doLoop = readBytes < reportLength;
                }
            }
        } while (doLoop);

        return buffers;
    }

    public void close() {
        mOpen = false;
        if(mUsbRequest != null) {
            mUsbRequest.close();
            mUsbRequest = null;
        }

        if(mUsbDeviceConnection != null) {
            mUsbDeviceConnection.releaseInterface(mUsbInterface);
            mUsbDeviceConnection.close();
            mUsbDeviceConnection = null;
        }
    }
}
