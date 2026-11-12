package com.jht.chimera.lib.common.datamatrixkeys;


import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.ParcelableDataDefault;

import java.util.LinkedList;
import java.util.List;


public enum BTSinkKeys {
    DATA_BT_SINK_PLAYBACK_IS_PLAYING_BOOL(new DataDefault<>(false)),
    DATA_BT_SINK_PLAYBACK_ARTIST_STRING(new DataDefault<>("")),
    DATA_BT_SINK_PLAYBACK_SONG_STRING(new DataDefault<>("")),
    DATA_BT_SINK_CURRENT_FOLDER_BYTES(new DataDefault<>(new byte[0])),
    DATA_BT_SINK_NUM_FOLDER_ITEMS_INT(new DataDefault<>(-1)),
    DATA_BT_SINK_GET_PLAYBACK_STATUS_INT(new DataDefault<>(0)),
    DATA_BT_SINK_PLAY_STATUS_PARCELABLE(new ParcelableDataDefault()),
    DATA_BT_SINK_PAIR_PARCELABLE(new ParcelableDataDefault()),
    EVENT_BT_SINK_RESET_SEQUENCE,
    DATA_BT_SINK_CAN_START_BROWSING_PROCESS_BOOL(new DataDefault<>(false)),
    DATA_BT_SINK_CAN_START_SONG_POSITION_TIMER_BOOL(new DataDefault<>(false)),
    DATA_BT_SINK_PINCODE_NOTIFICATION_INT(new DataDefault<>(0)),
    DATA_BT_SINK_SHOULD_STOP_SCANNER_TIMER_BOOL(new DataDefault<>(false)),
    DATA_BT_SINK_IS_AVRCP_READY_BOOL(new DataDefault<>(false)),
    DATA_BT_SINK_MISSING_TITLE_MEDIA_ITEM_PARCELABLE(new ParcelableDataDefault()),

    DATA_BT_SET_SHUFFLE_BOOL(new DataDefault<>(false)),
    DATA_BT_SET_REPEAT_INT(new DataDefault<>(0)),

    ACTION_MEDIA_FOLDER_ITEM_DISCOVER_PARCELABLE,
    DATA_MEDIA_FOLDER_ITEMS_PARCELABLE(new ParcelableDataDefault()),

    ACTION_PAIRING_COMPLETE;

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (BTSinkKeys s : BTSinkKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    BTSinkKeys(){
        this.DataDefault = null;
    }

    BTSinkKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }

}
