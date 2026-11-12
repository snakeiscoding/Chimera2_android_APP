package com.jht.chimera.service.usbmedia;

import android.bluetooth.BluetoothA2dp;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.mtp.MtpDevice;
import android.os.SystemClock;
import android.util.Log;

import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.service.usbmedia.ios.IOSDeviceManager;
import com.jht.chimera.service.usbmedia.mtpums.lib.mtp.MtpMediaController;
import com.jht.chimera.service.usbmedia.mtpums.lib.ums.UmsMediaController;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.util.UsbPermission;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MediaDeviceController {
    private static final String TAG = "MediaDeviceController";

    private Context mContext;
    private UsbManager mUsbManager;
    private JHTTypes.MediaDeviceType mDeviceType = JHTTypes.MediaDeviceType.UNKNOWN;
    private MtpDevice mMtpDevice;
    private UsbMediaDevice mUsbMediaDevice;
    private DataMatrix mDataMatrix = new DataMatrix();
    private List<UsbDevice> mUsbDevices = new ArrayList<>();
    private UsbDevice mAttachedDevice;
    private BluetoothA2dp mA2dpService;
    private File mUSBPath;

    MediaDeviceController(Context context) {
        mContext = context;
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        initDataMatrix();
    }

    private void initDataMatrix() {
        mDataMatrix.createBoolAdapter(UsbMediaDeviceKeys.ACTION_SELECT_TOP_LEVEL_BOOL, this::selectTopLevel, false);
        mDataMatrix.createParcelableAdapter(UsbMediaDeviceKeys.ACTION_SELECT_PARCELABLE, MediaItem.class, item -> {
            synchronized (this) {
                boolean rtn = select(item);
            }
        });
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_MEDIA_BACK, event -> back(), false);
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_MEDIA_PLAY, event -> play(), false);
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_MEDIA_PAUSE, event -> pause(), false);
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_MEDIA_NEXT, event -> next(), false);
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_MEDIA_PREVIOUS, event -> previous(), false);
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_MEDIA_FASTFORWARD, event -> fastForward(), false);
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_MEDIA_REWIND, event -> rewind(), false);
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_MEDIA_REPEAT, event -> repeat(), false);
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_MEDIA_SHUFFLE, event -> shuffle(), false);
        mDataMatrix.createIntAdapter(UsbMediaDeviceKeys.ACTION_SEEK_INT, this::seek, false);
        mDataMatrix.createParcelableAdapter(UsbMediaDeviceKeys.ACTION_GET_MEDIA_URI_PARCELABLE, MediaItem.class, item -> {
            String uri = getUri(item);
            DataMatrix.putString(UsbMediaDeviceKeys.DATA_MEDIA_URI_STRING, uri);
        });
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_REPEAT_DOWN, event -> repeatDown());
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_TOUCH_UP, event -> touchUp());
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_SHUFFLE_DOWN, event -> shuffleDown());
        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_PLAY_PAUSE_DOWN, event -> playPauseButtonDown());
    }

    public void setA2dpService(BluetoothA2dp a2dpService) {
        mA2dpService = a2dpService;
    }

    void findDevice() {
        if (!ProductConfig.isLightCommercial()) {
            new Thread(() -> {
                if (isUms()) {
                    mDeviceType = JHTTypes.MediaDeviceType.UMS;
                    Log.d(TAG, "findDevice: device type: " + mDeviceType);
                    mUsbMediaDevice = new UmsMediaController(mContext, mDataMatrix);
                }
            }).start();
        }

        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera) || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2)) {
            for (Map.Entry<String, UsbDevice> entry : mUsbManager.getDeviceList().entrySet()) {
                UsbDevice device = entry.getValue();
                UsbPermission.requestUsbPermission(mContext, device);
                if (isiOSDevice(device)) {
                    if(DataMatrix.getBoolean(SettingKeys.Hardware.DATA_MFI_ENABLE_BOOL)) {
                        mAttachedDevice = device;
                        mDeviceType = JHTTypes.MediaDeviceType.iOS;
                        Log.d(TAG, "findDevice: device type: " + mDeviceType);
                        mUsbMediaDevice = new IOSDeviceManager(mContext, device);
                    }
                    return;
                }
                mUsbDevices.add(device);
            }
        }

        for (UsbDevice d : mUsbDevices) {
            if (isMtpDevice(d)) {
                mAttachedDevice = d;
                mDeviceType = JHTTypes.MediaDeviceType.MTP;
                Log.d(TAG, "findDevice: device type: " + mDeviceType );
                mUsbMediaDevice = new MtpMediaController(mContext, mMtpDevice, mDataMatrix);
                return;
            }
        }
    }

    void checkDeviceType(UsbDevice device) {
        if (mDeviceType != JHTTypes.MediaDeviceType.UNKNOWN) {
            return;
        }

        UsbPermission.requestUsbPermission(mContext, device);
        if (isiOSDevice(device)) {
            if (!ProductConfig.hasMFI() || !DataMatrix.getBoolean(SettingKeys.Hardware.DATA_MFI_ENABLE_BOOL)) {
                return;
                // no MFI coprocess on parker hardware
            } else {
                mDeviceType = JHTTypes.MediaDeviceType.iOS;
                mUsbMediaDevice = new IOSDeviceManager(mContext, device);
            }
        } else if (isMtpDevice(device)){
            mDeviceType = JHTTypes.MediaDeviceType.MTP;
            mUsbMediaDevice = new MtpMediaController(mContext, mMtpDevice, mDataMatrix);
        } else if (!ProductConfig.isLightCommercial() && isUms()) {
            mDeviceType = JHTTypes.MediaDeviceType.UMS;
            mUsbMediaDevice = new UmsMediaController(mContext, mDataMatrix);
        }
        mAttachedDevice = device;

        Log.d(TAG, "checkDeviceType: device type: " + mDeviceType);

    }

    public UsbDevice getAttachedDevice() {
        return mAttachedDevice;
    }

    public JHTTypes.MediaDeviceType getDeviceType() {
        return mDeviceType;
    }

    public File getUSBPath() {
        return mUSBPath;
    }

    private boolean isiOSDevice(UsbDevice device) {
        if (device != null && device.getVendorId() == 0x05AC && (device.getProductId() & 0x1200) == 0x1200) {
            Log.i(TAG, "found an iOS device. vid: " + device.getVendorId() + " pid: " + device.getProductId());
            return true;
        }

        return false;
    }

    private boolean isMtpDevice(UsbDevice device) {
        SystemClock.sleep(500);
        UsbDeviceConnection usbDeviceConnection = mUsbManager.openDevice(device);
        if(usbDeviceConnection == null) {
            return false;
        }
        mMtpDevice = new MtpDevice(device);
        return mMtpDevice.open(usbDeviceConnection);
    }

    private boolean isUms() {
        Log.d(TAG, "isUms: ");
        boolean isMount;
        int timeout = 50;
        while(true) {
            isMount = checkUsbMountPoint();
            if(isMount) {
                return true;
            }

            if(timeout == 0) {
                break;
            }

            timeout--;

            SystemClock.sleep(200);
        }

        return false;
    }

    private boolean checkUsbMountPoint() {
        boolean result = false;
        File file = StorageUtils.getUSBPath();
        String[] fileList = file.list();
        if(fileList != null && fileList.length > 0) {
            mUSBPath = file;
            result = true;
        }

        return result;
    }

    private void scan() {}

    private void selectTopLevel(boolean video) {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.selectTopLevel(video);
        }
    }

    private boolean select(MediaItem item) {
        boolean rtn = false;
        if (mUsbMediaDevice != null) {
            rtn = mUsbMediaDevice.select(item);
        }
        return rtn;
    }

    synchronized private void back() {
        if (mUsbMediaDevice != null) {
            new Thread(() -> mUsbMediaDevice.back()).start();
        }
    }

    synchronized private void play() {
        if (mUsbMediaDevice != null) {
            new Thread(() -> mUsbMediaDevice.play(false)).start();
        }
    }

    synchronized private void pause() {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.pause(false);
        }
    }

    synchronized private void next() {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.next();
        }
    }

    synchronized private void previous() {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.previous();
        }
    }

    synchronized private void fastForward() {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.fastForward();
        }
    }

    synchronized private void rewind() {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.rewind();
        }
    }

    synchronized private void repeat() {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.repeat();
        }
    }

    synchronized private void repeatDown() {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.repeatDown();
        }
    }

    synchronized private void touchUp() {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.touchUp();
        }
    }

    synchronized private void shuffle() {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.shuffle();
        }
    }

    synchronized private void shuffleDown() {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.shuffleDown();
        }
    }

    synchronized private void playPauseButtonDown() {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.playPauseButtonDown();
        }
    }

    private void seek(int pos) {
        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.seek(pos);
        }
    }

    private String getUri(MediaItem item) {
        String uri = "";
        if (mUsbMediaDevice != null) {
            uri = mUsbMediaDevice.getUri(item);
        }
        return uri;
    }

    public void disconnect() {
        DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_CONNECTED_BOOL, false);
        DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_HAS_VIDEO_BOOL, false);
        DataMatrix.putString(UsbMediaDeviceKeys.DATA_DEVICE_NAME_STRING, "");
        DataMatrix.putParcelable(UsbMediaDeviceKeys.DATA_CURRENT_MEDIA_ITEM_PARCELABLE, null);
        DataMatrix.putBytes(UsbMediaDeviceKeys.DATA_ALBUM_ART_BYTES, null);
        DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_SHUFFLE_BOOL, false);
        DataMatrix.putInt(UsbMediaDeviceKeys.DATA_REPEAT_MODE_ENUM_INT, 0);
        DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_PLAYING_BOOL, false);
        DataMatrix.putInt(UsbMediaDeviceKeys.DATA_PLAYBACK_POSITION_INT, 0);

        if (mUsbMediaDevice != null) {
            mUsbMediaDevice.disconnect();

            if (mMtpDevice != null) {
                mMtpDevice.close();
            }
        }

        mDataMatrix.finish();
    }
}
