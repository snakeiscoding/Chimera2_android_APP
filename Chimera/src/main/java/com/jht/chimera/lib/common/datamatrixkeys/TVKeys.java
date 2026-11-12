package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;

import java.util.LinkedList;
import java.util.List;

/**
 * Class to contain keys that are controlled by the Sprint8 activity in the main app
 */
public enum TVKeys {
    /** any process → HAL service: a command for the tuner, sent through {@code Lg750ECommandSender}. */
    ACTION_SEND_TUNER_COMMAND_STRING,

    DATA_REMOTE_TV_NUM_CHANNELS_INT(new DataDefault<>(0)),
    DATA_REMOTE_TV_CURRENT_CHANNEL_INT(new DataDefault<>(0)),

    DATA_IPTV_CURRENT_CHANNEL_INT(new DataDefault<>(0)),
    DATA_IPTV_PREVIOUS_CHANNEL_INT(new DataDefault<>(0)),

    DATA_STB_CURRENT_CHANNEL_INT(new DataDefault<>(0)),
    DATA_STB_PREVIOUS_CHANNEL_INT(new DataDefault<>(0)),

    DATA_US8_CURRENT_CHANNEL_INT(new DataDefault<>(0)),
    DATA_KM6_CURRENT_CHANNEL_INT(new DataDefault<>(0)),

    DATA_US9_CURRENT_CHANNEL_INT(new DataDefault<>(0)),
    DATA_KM7_CURRENT_CHANNEL_INT(new DataDefault<>(0)),
    DATA_BR3_CURRENT_CHANNEL_INT(new DataDefault<>(0)),
    DATA_JP1_CURRENT_CHANNEL_INT(new DataDefault<>(0)),

    DATA_US4_CURRENT_CHANNEL_INT(new DataDefault<>(0)),
    DATA_US4_PREVIOUS_CHANNEL_INT(new DataDefault<>(0)),

    DATA_SWIFT_CURRENT_CHANNEL_INDEX_INT(new DataDefault<>(-1)),
    DATA_SWIFT_PREVIOUS_CHANNEL_INDEX_INT(new DataDefault<>(-1)),
    ACTION_GET_TUNER_NETWORK_DETAILS,
    EVENT_TUNER_IP_ADDRESS_STRING,
    EVENT_TUNER_MAC_ADDRESS_STRING,
    EVENT_TUNER_DHCP_STRING,
    EVENT_TUNER_SUBNET_STRING,
    EVENT_TUNER_GATEWAY_STRING,
    EVENT_TUNER_DNS1_STRING,
    EVENT_TUNER_DNS2_STRING,
    EVENT_TUNER_MODEL_STRING,

    DATA_TUNER_TYPE_INT(new DataDefault<>(0)),
    DATA_TUNER_VERSION_STRING(new DataDefault<>("--")),
    EVENT_PARSE_CHANNELS_PARCELABLE,
    EVENT_UPDATE_DEFAULT_CHANNEL_OPTION,
    EVENT_UPDATE_CHANNEL_LIST,
    EVENT_KM4_ISC_START,
    DATA_CAMERA_IS_OPEN_BOOL(new DataDefault<>(false)),
    DATA_USE_TUNER_EMULATOR_BOOL(new DataDefault<>(false)),
    EVENT_US8_TUNER_HOST_INPUT_STREAM,
    EVENT_US8_TUNER_HOST_OUTPUT_STREAM,
    EVENT_US7_TUNER_HOST_INPUT_STREAM,
    EVENT_US7_TUNER_HOST_OUTPUT_STREAM,
    EVENT_US6_TUNER_HOST_INPUT_STREAM,
    EVENT_US6_TUNER_HOST_OUTPUT_STREAM,
    EVENT_CAB_TUNER_HOST_INPUT_STREAM,
    EVENT_CAB_TUNER_HOST_OUTPUT_STREAM,
    EVENT_KM5_TUNER_HOST_INPUT_STREAM,
    EVENT_KM5_TUNER_HOST_OUTPUT_STREAM,
    EVENT_KM6_TUNER_HOST_INPUT_STREAM,
    EVENT_KM6_TUNER_HOST_OUTPUT_STREAM,
    EVENT_US4_TUNER_HOST_INPUT_STREAM,
    EVENT_US4_TUNER_HOST_OUTPUT_STREAM,
    ACTION_SHOW_CLOSED_CAPTION_STATUS_BOOL,
    DATA_TV_TUNER_TYPE_DETECTED_INT(new DataDefault<>(0)),

    /** UI → service: triggers an LG 750E channel import via the STB WebSocket. */
    ACTION_LG750E_CHANNEL_IMPORT_START,
    /** service → UI: JSON array string of all imported LG channels, delivered when import completes. */
    EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING,
    /** service → UI: error message string if the LG 750E channel import fails. */
    EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING,
    DATA_LG750E_TUNER_CONNECTED_BOOL(new DataDefault<>(false));

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (TVKeys s : TVKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    TVKeys(){
        this.DataDefault = null;
    }

    TVKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }
}
