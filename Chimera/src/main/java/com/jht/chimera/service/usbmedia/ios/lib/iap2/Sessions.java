package com.jht.chimera.service.usbmedia.ios.lib.iap2;

public class Sessions {
    public enum ControlMessageType {
        UNKNOWN_MESSAGE((char) 0x0000),
        REQ_AUTHENTICATION_CERTIFICATE((char) 0xAA00),
        AUTHENTICATION_CERTIFICATE((char) 0xAA01),
        REQ_AUTHENTICATION_CHALLENGE_RESPONSE((char) 0xAA02),
        AUTHENTICATION_RESPONSE((char) 0xAA03),
        AUTHENTICATION_FAILED((char) 0xAA04),
        AUTHENTICATION_SUCCEEDED((char) 0xAA05),
        START_IDENTIFICATION((char) 0x1D00),
        IDENTIFICATION_INFORMATION((char) 0x1D01),
        IDENTIFICATION_ACCEPTED((char) 0x1D02),
        IDENTIFICATION_REJECTED((char) 0x1D03),
        CANCEL_IDENTIFICATION((char) 0x1D05),
        IDENTIFICATION_INFORMATION_UPDATE((char) 0x1D06),
        START_MEDIA_LIBRARY_INFORMATION((char) 0x4C00),
        MEDIA_LIBRARY_INFORMATION((char) 0x4C01),
        STOP_MEDIA_LIBRARY_INFORMATION((char) 0x4C02),
        START_MEDIA_LIBRARY_UPDATES((char) 0x4C03),
        MEDIA_LIBRARY_UPDATE((char) 0x4C04),
        STOP_MEDIA_LIBRARY_UPDATES((char) 0x4C05),
//        PLAY_MEDIA_LIBRARY_CURRENT_SELECTION((char) 0x4C06),
        PLAY_MEDIA_LIBRARY_ITEMS((char) 0x4C07),
        PLAY_MEDIA_LIBRARY_COLLECTION((char) 0x4C08),
        PLAY_MEDIA_LIBRARY_SPECIAL((char) 0x4C09),
        DEVICE_INFORMATION_UPDATE((char) 0x4E09),
        DEVICE_LANGUAGE_UPDATE((char) 0x4E0A),
        START_NOW_PLAYING_UPDATES((char) 0x5000),
        NOW_PLAYING_UPDATE((char) 0x5001),
        STOP_NOW_PLAYING_UPDATES((char) 0x5002),
        SET_NOW_PLAYING_INFORMATION((char) 0x5003),
        START_HID((char) 0x6800),
        DEVICE_HID_REPORT((char) 0x6801),
        ACCESSORY_HID_REPORT((char) 0x6802),
        STOP_HID((char) 0x6803),
        START_POWER_UPDATES((char) 0xAE00),
        POWER_UPDATE((char) 0xAE01),
        STOP_POWER_UPDATES((char) 0xAE02),
        POWER_SOURCE_UPDATE((char) 0xAE03),
        START_USB_DEVICE_MODE_AUDIO((char) 0xDA00),
        USB_DEVICE_MODE_AUDIO_INFORMATION((char) 0xDA01),
        STOP_USB_DEVICE_MODE_AUDIO((char) 0xDA02);

        private char mId;

        ControlMessageType(char id) {
            mId = id;
        }

        public char getId() {
            return mId;
        }

        public static ControlMessageType getType(char id) {
            for(ControlMessageType type : ControlMessageType.values()) {
                if(type.getId() == id) {
                    return type;
                }
            }

            return UNKNOWN_MESSAGE;
        }
    }

    public enum SessionID {
        CONTROL((byte) 0x0A),
        FILE_TRANSFER((byte) 0x0B);

        private byte mId;
        SessionID(byte id) {
            mId = id;
        }

        public byte getId() {
            return mId;
        }
    }

    public enum SessionType {
        CONTROL_SESSION,
        FILE_TRANSFER_SESSION,
        EXTERNAL_ACCESSORY_SESSION,
        NUMBER
    }
}
