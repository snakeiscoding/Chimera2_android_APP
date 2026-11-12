package com.jht.chimera.service.usbmedia;

import android.content.Context;

import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.chimera.service.usbmedia.mtpums.lib.media.MediaItemCreator;
import com.jht.chimera.service.usbmedia.mtpums.lib.media.Track;

public abstract class UsbMediaDevice {
    private static final String TAG = "UsbMediaDevice";
    protected Context mContext;
    protected String mDeviceName;
    protected JHTTypes.MediaDeviceType mDeviceType = JHTTypes.MediaDeviceType.UNKNOWN;
    protected MediaItemCreator mAudioCreator;
    protected MediaItemCreator mVideoCreator;
    protected boolean mHasAudioFocus;

    public enum DeviceType {
        iOS, MTP, UMS, UNKNOWN
    }

    protected UsbMediaDevice(Context context) {
        mContext = context;
    }

    public abstract void scan();

    public void selectTopLevel(boolean video) {
    }
    public abstract boolean select(MediaItem item);
    public abstract void back();
    public abstract void play(boolean auto);
    public abstract void pause(boolean auto);
    public abstract void next();
    public abstract void previous();
    public abstract void fastForward();
    public abstract void rewind();
    public abstract void seek(int pos);
    public abstract void repeat();
    public abstract void repeatDown();
    public abstract void touchUp();
    public abstract void shuffle();
    public abstract void shuffleDown();
    public abstract void playPauseButtonDown();
    protected abstract byte[] getAlbumArt(Track track);
    public abstract String getUri(MediaItem item);

    public void disconnect() {

        mHasAudioFocus = false;
    }
}
