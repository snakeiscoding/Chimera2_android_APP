package com.jht.chimera.lib.tv.switft;


public class Constants {
    public static final byte     ANT_SCANNING_STATE_MASK = 0x01;
    public static final byte     BLE_SCANNING_STATE_MASK = 0x02;
    public static final byte FEC_BROADCASTING_STATE_MASK = 0x04;

    public enum PROXIMITY {
        THRESHOLD_CUSTOM(0x80), ///< Custom proximity search selection.
        THRESHOLD_OFF(0x00), ///< Disable proximity search detection.
        THRESHOLD_1(0x01), ///< Proximity search detection radius > preset threshold (~ -44dBm on nRF51)
        THRESHOLD_2(0x02), ///< Proximity search detection radius > preset threshold (~ -48dBm on nRF51).
        THRESHOLD_3(0x03), ///< Proximity search detection radius > preset threshold (~ -52dBm on nRF51).
        THRESHOLD_4(0x04), ///< Proximity search detection radius > preset threshold (~ -56dBm on nRF51).
        THRESHOLD_5(0x05), ///< Proximity search detection radius > preset threshold (~ -60dBm on nRF51).
        THRESHOLD_6(0x06), ///< Proximity search detection radius > preset threshold (~ -64dBm on nRF51).
        THRESHOLD_7(0x07), ///< Proximity search detection radius > preset threshold (~ -68dBm on nRF51).
        THRESHOLD_8(0x08), ///< Proximity search detection radius > preset threshold (~ -72dBm on nRF51).
        THRESHOLD_9(0x09), ///< Proximity search detection radius > preset threshold (~ -76dBm on nRF51).
        THRESHOLD_10(0x0A); ///< Proximity search detection radius > preset threshold (~ -80dBm on nRF51).

        private int Value;

        PROXIMITY(int value) {
            this.Value = value;
        }

        public int getValue() {
            return Value;
        }
    }

    public enum CONN_STATE {
        STATE_DISCONNECTED(0x00),
        STATE_CONNECTING(0x01),
        STATE_CONNECTED(0x02),
        STATE_RECONNECTING(0x03),
        STATE_DISCONNECTING(0x04);

        private int Value;

        CONN_STATE(int value) {
            this.Value = value;
        }

        public static CONN_STATE fromInt(int i) {
            for (CONN_STATE b : CONN_STATE.values()) {
                if (b.Value == i) {
                    return b;
                }
            }
            return null;
        }

        public int getValue() {
            return Value;
        }
    }

    public enum PROFILE {
        PROFILE_FEC(0x00),
        PROFILE_BIKE_POWER(0x01),
        PROFILE_BIKE_CADENCE_SPEED(0x02);

        private int Value;

        PROFILE(int value) {
            this.Value = value;
        }

        public static PROFILE fromInt(int i) {
            for (PROFILE b : PROFILE.values()) {
                if (b.Value == i) {
                    return b;
                }
            }
            return null;
        }

        public int getValue() {
            return Value;
        }
    }
}
