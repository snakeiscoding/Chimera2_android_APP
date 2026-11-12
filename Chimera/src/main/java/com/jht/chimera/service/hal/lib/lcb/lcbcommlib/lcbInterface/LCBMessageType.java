package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface;

public class LCBMessageType {

    public enum LCB_CONTROL_TPYE {
        CHIMERA_BIKE_CONTROL(0x00),
        CHIMERA_STEPPER_CONTROL(0x01),
        CHIMERA_RAMP_CONTROL(0x02),
        CHIMERA_SWING_CONTROL(0x03),
        CHIMERA_TREADMILL_CONTROL(0x04),
        CHIMERA_CLIMBMILL_CONTROL(0x05),
        CHIMERA_CLIMBMILL_RETAIL_CONTROL(0x06);

        private final byte id;

        LCB_CONTROL_TPYE(int id) {
            this.id = (byte) id;
        }

        public byte getId() {
            return this.id;
        }
    }

    ;

    public enum LCB_TYPES {
        CHIMERA_AC_JIS((char) 0x01),
        CHIMERA_LCB_1_JHT((char) 0x02),
        CHIMERA_LCB_2_JHT((char) 0x03),
        CHIMERA_DC_LCB_JIS((char) 0x04),
        CHIMERA_LCB_PANATTA((char) 0x06),
        CHIMERA_LCB_1_PWM_JHT((char) 0x07),
        CHIMERA_LCB_3_CURRENT_JHT((char) 0x08),
        CHIMERA_LCB_1_PANATTA((char) 0x09),
        CHIMERA_LCB_3_A5_REDESIGN((char) 0x0A),
        CHIMERA_CLIMBMILL((char) 0x0B),
        CHIMERA_DCI_DAUGHTER_BOARD((char) 0xC0),
        CHIMERA_TOPTEK((char) 0xC1),
        CHIMERA_DELTA((char) 0xC2),
        CHIMERA_LCBA((char) 0x13),
        CHIMERA_ONE_X_BIKE_LCB((char) 0x14);

        private char _value;

        private LCB_TYPES(char value) {
            _value = value;
        }

        public char getValue() {
            return _value;
        }
    }

    public static final byte STOP_CLIMBMILL_CONTROL = 0x01;
    public static final byte START_CLIMBMILL_CONTROL = 0x00;
    public static final byte E_STOP_DISABLE = (byte) 0xA0;
    public static final byte E_STOP_ENABLE_IR_ENABLE = (byte) 0xA1;
    public static final byte E_STOP_DISABLE_IR_ENABLE = (byte) 0xA2;
    public static final byte E_STOP_ENABLE_IR_DISABLE = (byte) 0xA3;
    public static final byte E_STOP_UNLOCK = (byte) 0xA5;
    public static final byte DETECT_PERSON_DISABLE = (byte) 0xB0;
    public static final byte DETECT_PERSON_ENABLE = (byte) 0xB1;
}
