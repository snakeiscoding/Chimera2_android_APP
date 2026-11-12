package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.datamatrixlib.ParcelableDataDefault;

import java.util.LinkedList;
import java.util.List;

public enum UsbMediaDeviceKeys {
    DATA_IS_CONNECTED_BOOL(new DataDefault<>(false)),
    DATA_DEVICE_NAME_STRING(new DataDefault<>("DEVICE NAME")),
    DATA_HAS_VIDEO_BOOL(new DataDefault<>(false)),
    DATA_IS_SCANNING_BOOL(new DataDefault<>(false)),
    DATA_PLAYBACK_POSITION_INT(new DataDefault<>(0)),
    DATA_CURRENT_MEDIA_ITEM_PARCELABLE(new ParcelableDataDefault()),
    DATA_ALBUM_ART_BYTES(new DataDefault<>(new byte[0])),
    DATA_IS_PLAYING_BOOL(new DataDefault<>(false)),
    DATA_REPEAT_MODE_ENUM_INT(new DataDefault<>(0)),
    DATA_IS_SHUFFLE_BOOL(new DataDefault<>(false)),
    ACTION_SELECT_TOP_LEVEL_BOOL,
    ACTION_SELECT_PARCELABLE,
    ACTION_MEDIA_BACK,
    ACTION_MEDIA_PLAY,
    ACTION_MEDIA_PAUSE,
    ACTION_MEDIA_NEXT,
    ACTION_MEDIA_PREVIOUS,
    ACTION_MEDIA_FASTFORWARD,
    ACTION_MEDIA_REWIND,
    ACTION_MEDIA_SHUFFLE,
    ACTION_MEDIA_REPEAT,
    EVENT_TRACK_FOUND_PARCELABLE,
    ACTION_GET_MEDIA_URI_PARCELABLE,
    DATA_MEDIA_URI_STRING(new DataDefault<>("")),
    ACTION_SEEK_INT,
    ACTION_AUTO_PLAY,
    ACTION_PLAY_PAUSE_DOWN,
    ACTION_REPEAT_DOWN,
    ACTION_SHUFFLE_DOWN,
    ACTION_TOUCH_UP;

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (UsbMediaDeviceKeys s : UsbMediaDeviceKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    UsbMediaDeviceKeys(){
        this.DataDefault = null;
    }

    UsbMediaDeviceKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }

    public interface OnMediaItemFoundListener {
        void onMediaItemFound(MediaItem item);
    }

    public static void registerEventMediaItemFound(DataMatrix dataMatrix, final OnMediaItemFoundListener onMediaItemFoundListener) {
        dataMatrix.createParcelableAdapter(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, MediaItem.class, item -> {
            if (onMediaItemFoundListener != null) {
                onMediaItemFoundListener.onMediaItemFound(item);
            }
        });
    }
}
