//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.ftdi.j2xx;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Copy the D2xxManager from d2xx.jar of jhtandroidserialport artifact and add the flag FLAG_IMMUTABLE because it will throw the following IllegalArgumentException.
 *
 * java.lang.IllegalArgumentException: com.jht.chimera: Targeting S+ (version 31 and above) requires that one of FLAG_IMMUTABLE or FLAG_MUTABLE be specified when creating a PendingIntent.
 * Strongly consider using FLAG_IMMUTABLE, only use FLAG_MUTABLE if some functionality depends on the PendingIntent being mutable, e.g. if it needs to be used with inline replies or bubbles
 */
public class D2xxManager2 {
    private static D2xxManager2 mInstance = null;
    private static boolean bRequestPermission = true;
    private static boolean bRegisterBroadcast = true;
    private static final String TAG = "D2xx::";
    protected static final String ACTION_USB_PERMISSION = "com.ftdi.j2xx";
    public static final byte FT_DATA_BITS_7 = 7;
    public static final byte FT_DATA_BITS_8 = 8;
    public static final byte FT_STOP_BITS_1 = 0;
    public static final byte FT_STOP_BITS_2 = 2;
    public static final byte FT_PARITY_NONE = 0;
    public static final byte FT_PARITY_ODD = 1;
    public static final byte FT_PARITY_EVEN = 2;
    public static final byte FT_PARITY_MARK = 3;
    public static final byte FT_PARITY_SPACE = 4;
    public static final short FT_FLOW_NONE = 0;
    public static final short FT_FLOW_RTS_CTS = 256;
    public static final short FT_FLOW_DTR_DSR = 512;
    public static final short FT_FLOW_XON_XOFF = 1024;
    public static final byte FT_PURGE_RX = 1;
    public static final byte FT_PURGE_TX = 2;
    public static final byte FT_CTS = 16;
    public static final byte FT_DSR = 32;
    public static final byte FT_RI = 64;
    public static final byte FT_DCD = -128;
    public static final byte FT_OE = 2;
    public static final byte FT_PE = 4;
    public static final byte FT_FE = 8;
    public static final byte FT_BI = 16;
    public static final byte FT_EVENT_RXCHAR = 1;
    public static final byte FT_EVENT_MODEM_STATUS = 2;
    public static final byte FT_EVENT_LINE_STATUS = 4;
    public static final byte FT_EVENT_REMOVED = 8;
    public static final byte FT_FLAGS_OPENED = 1;
    public static final byte FT_FLAGS_HI_SPEED = 2;
    public static final int FT_DEVICE_232B = 0;
    public static final int FT_DEVICE_8U232AM = 1;
    public static final int FT_DEVICE_UNKNOWN = 3;
    public static final int FT_DEVICE_2232 = 4;
    public static final int FT_DEVICE_232R = 5;
    public static final int FT_DEVICE_245R = 5;
    public static final int FT_DEVICE_2232H = 6;
    public static final int FT_DEVICE_4232H = 7;
    public static final int FT_DEVICE_232H = 8;
    public static final int FT_DEVICE_X_SERIES = 9;
    public static final int FT_DEVICE_4222_0 = 10;
    public static final int FT_DEVICE_4222_1_2 = 11;
    public static final int FT_DEVICE_4222_3 = 12;
    public static final byte FT_BITMODE_RESET = 0;
    public static final byte FT_BITMODE_ASYNC_BITBANG = 1;
    public static final byte FT_BITMODE_MPSSE = 2;
    public static final byte FT_BITMODE_SYNC_BITBANG = 4;
    public static final byte FT_BITMODE_MCU_HOST = 8;
    public static final byte FT_BITMODE_FAST_SERIAL = 16;
    public static final byte FT_BITMODE_CBUS_BITBANG = 32;
    public static final byte FT_BITMODE_SYNC_FIFO = 64;
    public static final int FTDI_BREAK_OFF = 0;
    public static final int FTDI_BREAK_ON = 16384;
    private static Context mContext = null;
    private static PendingIntent mPendingIntent = null;
    private static IntentFilter mPermissionFilter = null;
    private static List<FtVidPid> mSupportedDevices = new ArrayList(Arrays.asList(new FtVidPid(1027, 24597), new FtVidPid(1027, 24596), new FtVidPid(1027, 24593), new FtVidPid(1027, 24592), new FtVidPid(1027, 24577), new FtVidPid(1027, 24582), new FtVidPid(1027, 24604), new FtVidPid(1027, 64193), new FtVidPid(1027, 64194), new FtVidPid(1027, 64195), new FtVidPid(1027, 64196), new FtVidPid(1027, 64197), new FtVidPid(1027, 64198), new FtVidPid(1027, 24594), new FtVidPid(2220, 4133), new FtVidPid(5590, 1), new FtVidPid(1027, 24599)));
    private ArrayList<FT_Device> mFtdiDevices;
    private static UsbManager mUsbManager;
    private BroadcastReceiver mUsbPlugEvents = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice dev = null;
            FT_Device ftDev = null;
            if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)) {
                dev = (UsbDevice)intent.getParcelableExtra("device");

                for(ftDev = D2xxManager2.this.findDevice(dev); ftDev != null; ftDev = D2xxManager2.this.findDevice(dev)) {
                    ftDev.close();
                    synchronized(D2xxManager2.this.mFtdiDevices) {
                        D2xxManager2.this.mFtdiDevices.remove(ftDev);
                    }
                }
            } else if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)) {
                dev = (UsbDevice)intent.getParcelableExtra("device");
                D2xxManager2.this.addUsbDevice(dev);
            }

        }
    };
    private static BroadcastReceiver mUsbDevicePermissions = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.ftdi.j2xx".equals(action)) {
                synchronized(this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra("device");
                    if (!intent.getBooleanExtra("permission", false)) {
                        Log.d("D2xx::", "permission denied for device " + device);
                    }
                }
            }

        }
    };

    private FT_Device findDevice(UsbDevice usbDev) {
        FT_Device rtDev = null;
        synchronized(this.mFtdiDevices) {
            int nr_dev = this.mFtdiDevices.size();

            for(int i = 0; i < nr_dev; ++i) {
                FT_Device ftDevice = (FT_Device)this.mFtdiDevices.get(i);
                UsbDevice dev = ftDevice.getUsbDevice();
                if (dev.equals(usbDev)) {
                    rtDev = ftDevice;
                    break;
                }
            }

            return rtDev;
        }
    }

    public boolean isFtDevice(UsbDevice dev) {
        boolean rc = false;
        if (mContext == null) {
            return rc;
        } else {
            FtVidPid vidPid = new FtVidPid(dev.getVendorId(), dev.getProductId());
            if (mSupportedDevices.contains(vidPid)) {
                rc = true;
            }

            Log.v("D2xx::", vidPid.toString());
            return rc;
        }
    }

    private static synchronized boolean updateContext(Context context) {
        boolean rc = false;
        if (context == null) {
            return rc;
        } else {
            if (mContext != context) {
                mContext = context;
                mPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, new Intent("com.ftdi.j2xx"), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                mPermissionFilter = new IntentFilter("com.ftdi.j2xx");
                mContext.getApplicationContext().registerReceiver(mUsbDevicePermissions, mPermissionFilter);
            }

            rc = true;
            return rc;
        }
    }

    private boolean isPermitted(UsbDevice dev) {
        boolean rc = false;
        if (bRequestPermission && !mUsbManager.hasPermission(dev)) {
            mUsbManager.requestPermission(dev, mPendingIntent);
        }

        if (mUsbManager.hasPermission(dev)) {
            rc = true;
        }

        return rc;
    }

    private boolean checkAskPermitted(UsbDevice dev) {
        boolean rc = false;
        if (!mUsbManager.hasPermission(dev)) {
            mUsbManager.requestPermission(dev, mPendingIntent);
        }

        if (mUsbManager.hasPermission(dev)) {
            rc = true;
        }

        return rc;
    }

    private D2xxManager2(Context parentContext) throws D2xxManager.D2xxException {
        Log.v("D2xx::", "Start constructor");
        if (parentContext == null) {
            throw new D2xxManager.D2xxException("D2xx init failed: Can not find parentContext!");
        } else {
            updateContext(parentContext);
            if (!findUsbManger()) {
                throw new D2xxManager.D2xxException("D2xx init failed: Can not find UsbManager!");
            } else {
                this.mFtdiDevices = new ArrayList();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
                filter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
                parentContext.getApplicationContext().registerReceiver(this.mUsbPlugEvents, filter);
                Log.v("D2xx::", "End constructor");
            }
        }
    }

    public static synchronized D2xxManager2 getInstance(Context parentContext) throws D2xxManager.D2xxException {
        if (mInstance == null) {
            mInstance = new D2xxManager2(parentContext);
        }

        if (parentContext != null) {
            updateContext(parentContext);
        }

        return mInstance;
    }

    private static boolean findUsbManger() {
        if (mUsbManager == null && mContext != null) {
            mUsbManager = (UsbManager)mContext.getApplicationContext().getSystemService("usb");
        }

        return mUsbManager != null;
    }

    public boolean setVIDPID(int vendorId, int productId) {
        boolean rc = false;
        if (vendorId != 0 && productId != 0) {
            FtVidPid vidpid = new FtVidPid(vendorId, productId);
            if (mSupportedDevices.contains(vidpid)) {
                Log.i("D2xx::", "Existing vid:" + vendorId + "  pid:" + productId);
                return true;
            }

            if (!mSupportedDevices.add(vidpid)) {
                Log.d("D2xx::", "Failed to add VID/PID combination to list.");
            } else {
                rc = true;
            }
        } else {
            Log.d("D2xx::", "Invalid parameter to setVIDPID");
        }

        return rc;
    }

    public int[][] getVIDPID() {
        int listSize = mSupportedDevices.size();
        int[][] arrayVIDPID = new int[2][listSize];

        for(int i = 0; i < listSize; ++i) {
            FtVidPid vidpid = (FtVidPid)mSupportedDevices.get(i);
            arrayVIDPID[0][i] = vidpid.getVid();
            arrayVIDPID[1][i] = vidpid.getPid();
        }

        return arrayVIDPID;
    }

    private void clearDevices() {
        synchronized(this.mFtdiDevices) {
            int nr_dev = this.mFtdiDevices.size();

            for(int i = 0; i < nr_dev; ++i) {
                this.mFtdiDevices.remove(0);
            }

        }
    }

    public int createDeviceInfoList(Context parentContext) {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        //android.util.Log.e("FTDI485", " deviceList = " + deviceList);
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        ArrayList<FT_Device> devices = new ArrayList();
        FT_Device ftDev = null;
        int rc = 0;
        if (parentContext == null) {
            return rc;
        } else {
            updateContext(parentContext);

            while(true) {
                UsbDevice usbDevice;
                do {
                    if (!deviceIterator.hasNext()) {
                        synchronized(this.mFtdiDevices) {
                            this.clearDevices();
                            this.mFtdiDevices = devices;
                            rc = this.mFtdiDevices.size();
                            return rc;
                        }
                    }

                    usbDevice = (UsbDevice)deviceIterator.next();

                    Log.e("FTDI485", " this.isFtDevice(usbDevice) = " + this.isFtDevice(usbDevice));
                } while(!this.isFtDevice(usbDevice));

                int numInterfaces = usbDevice.getInterfaceCount();
                Log.d("FTDI485", " numInterfaces = " + numInterfaces);

                for(int i = 0; i < numInterfaces; ++i) {
                    if (this.isPermitted(usbDevice)) {
                        synchronized(this.mFtdiDevices) {
                            ftDev = this.findDevice(usbDevice);
                            Log.d("FTDI485", " ftDev = " + ftDev);
                            if (ftDev == null) {
                                ftDev = new FT_Device(parentContext, mUsbManager, usbDevice, usbDevice.getInterface(i));
                            } else {
                                this.mFtdiDevices.remove(ftDev);
                                ftDev.setContext(parentContext);
                            }

                            devices.add(ftDev);
                        }
                    }
                }
            }
        }
    }

    public synchronized int getDeviceInfoList(int numDevs, D2xxManager.FtDeviceInfoListNode[] deviceList) {
        for(int i = 0; i < numDevs; ++i) {
            deviceList[i] = ((FT_Device)this.mFtdiDevices.get(i)).mDeviceInfoNode;
        }

        return this.mFtdiDevices.size();
    }

    public synchronized D2xxManager.FtDeviceInfoListNode getDeviceInfoListDetail(int index) {
        return index <= this.mFtdiDevices.size() && index >= 0 ? ((FT_Device)this.mFtdiDevices.get(index)).mDeviceInfoNode : null;
    }

    public static int getLibraryVersion() {
        return 545259520;
    }

    private boolean tryOpen(Context parentContext, FT_Device ftDev, D2xxManager.DriverParameters params) {
        boolean rc = false;
        if (ftDev == null) {
            return rc;
        } else if (parentContext == null) {
            return rc;
        } else {
            ftDev.setContext(parentContext);
            if (params != null) {
                ftDev.setDriverParameters(params);
            }

            if (ftDev.openDevice(mUsbManager) && ftDev.isOpen()) {
                rc = true;
            }

            return rc;
        }
    }

    public synchronized FT_Device openByUsbDevice(Context parentContext, UsbDevice dev, D2xxManager.DriverParameters params) {
        FT_Device ftDev = null;
        if (this.isFtDevice(dev)) {
            ftDev = this.findDevice(dev);
            if (!this.tryOpen(parentContext, ftDev, params)) {
                ftDev = null;
            }
        }

        return ftDev;
    }

    public synchronized FT_Device openByUsbDevice(Context parentContext, UsbDevice dev) {
        return this.openByUsbDevice(parentContext, dev, (D2xxManager.DriverParameters)null);
    }

    public synchronized FT_Device openByIndex(Context parentContext, int index, D2xxManager.DriverParameters params) {
        FT_Device ftDev = null;
        if (index < 0) {
            return ftDev;
        } else if (parentContext == null) {
            return ftDev;
        } else {
            updateContext(parentContext);
            ftDev = (FT_Device)this.mFtdiDevices.get(index);
            if (!this.tryOpen(parentContext, ftDev, params)) {
                ftDev = null;
            }

            return ftDev;
        }
    }

    public synchronized FT_Device openByIndex(Context parentContext, int index) {
        return this.openByIndex(parentContext, index, (D2xxManager.DriverParameters)null);
    }

    public synchronized FT_Device openBySerialNumber(Context parentContext, String serialNumber, D2xxManager.DriverParameters params) {
        D2xxManager.FtDeviceInfoListNode devInfo = null;
        FT_Device ftDev = null;
        if (parentContext == null) {
            return ftDev;
        } else {
            updateContext(parentContext);

            for(int i = 0; i < this.mFtdiDevices.size(); ++i) {
                FT_Device tmpDev = (FT_Device)this.mFtdiDevices.get(i);
                if (tmpDev != null) {
                    devInfo = tmpDev.mDeviceInfoNode;
                    if (devInfo == null) {
                        Log.d("D2xx::", "***devInfo cannot be null***");
                    } else if (devInfo.serialNumber.equals(serialNumber)) {
                        ftDev = tmpDev;
                        break;
                    }
                }
            }

            if (!this.tryOpen(parentContext, ftDev, params)) {
                ftDev = null;
            }

            return ftDev;
        }
    }

    public synchronized FT_Device openBySerialNumber(Context parentContext, String serialNumber) {
        return this.openBySerialNumber(parentContext, serialNumber, (D2xxManager.DriverParameters)null);
    }

    public synchronized FT_Device openByDescription(Context parentContext, String description, D2xxManager.DriverParameters params) {
        D2xxManager.FtDeviceInfoListNode devInfo = null;
        FT_Device ftDev = null;
        if (parentContext == null) {
            return ftDev;
        } else {
            updateContext(parentContext);

            for(int i = 0; i < this.mFtdiDevices.size(); ++i) {
                FT_Device tmpDev = (FT_Device)this.mFtdiDevices.get(i);
                if (tmpDev != null) {
                    devInfo = tmpDev.mDeviceInfoNode;
                    if (devInfo == null) {
                        Log.d("D2xx::", "***devInfo cannot be null***");
                    } else if (devInfo.description.equals(description)) {
                        ftDev = tmpDev;
                        break;
                    }
                }
            }

            if (!this.tryOpen(parentContext, ftDev, params)) {
                ftDev = null;
            }

            return ftDev;
        }
    }

    public synchronized FT_Device openByDescription(Context parentContext, String description) {
        return this.openByDescription(parentContext, description, (D2xxManager.DriverParameters)null);
    }

    public synchronized FT_Device openByLocation(Context parentContext, int location, D2xxManager.DriverParameters params) {
        D2xxManager.FtDeviceInfoListNode devInfo = null;
        FT_Device ftDev = null;
        if (parentContext == null) {
            return ftDev;
        } else {
            updateContext(parentContext);

            for(int i = 0; i < this.mFtdiDevices.size(); ++i) {
                FT_Device tmpDev = (FT_Device)this.mFtdiDevices.get(i);
                if (tmpDev != null) {
                    devInfo = tmpDev.mDeviceInfoNode;
                    if (devInfo == null) {
                        Log.d("D2xx::", "***devInfo cannot be null***");
                    } else if (devInfo.location == location) {
                        ftDev = tmpDev;
                        break;
                    }
                }
            }

            if (!this.tryOpen(parentContext, ftDev, params)) {
                ftDev = null;
            }

            return ftDev;
        }
    }

    public synchronized FT_Device openByLocation(Context parentContext, int location) {
        return this.openByLocation(parentContext, location, (D2xxManager.DriverParameters)null);
    }

    public int addUsbDevice(UsbDevice dev) {
        int rc = 0;
        if (this.isFtDevice(dev)) {
            int numInterfaces = dev.getInterfaceCount();

            for(int i = 0; i < numInterfaces; ++i) {
                if (this.checkAskPermitted(dev)) {
                    synchronized(this.mFtdiDevices) {
                        FT_Device ftDev = this.findDevice(dev);
                        if (ftDev == null) {
                            ftDev = new FT_Device(mContext, mUsbManager, dev, dev.getInterface(i));
                        } else {
                            ftDev.setContext(mContext);
                            this.mFtdiDevices.remove(ftDev);
                        }

                        this.mFtdiDevices.add(ftDev);
                        ++rc;
                    }
                }
            }
        }

        return rc;
    }

    public boolean getRequestPermissionSetting() {
        return bRequestPermission;
    }

    public void setRequestPermission(boolean bRequest) {
        bRequestPermission = bRequest;
    }

    public boolean getUsbRegisterBroadcastSetting() {
        return bRegisterBroadcast;
    }

    public void setUsbRegisterBroadcast(boolean bRegister) {
        if (bRegister && !bRegisterBroadcast) {
            bRegisterBroadcast = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
            filter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
            mContext.getApplicationContext().registerReceiver(this.mUsbPlugEvents, filter);
        } else if (!bRegister && bRegisterBroadcast) {
            bRegisterBroadcast = false;
            mContext.getApplicationContext().unregisterReceiver(this.mUsbPlugEvents);
        }

    }

}
