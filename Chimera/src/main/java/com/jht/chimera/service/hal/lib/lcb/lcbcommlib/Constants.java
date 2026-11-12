package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;


public class Constants {

    //Receive data length of data bytes limited max.255
    public static final int RECEIVE_DATA_LENGTH_LIMITED_MAX = 255;
    //Receive data size define
    public static final int RECEIVE_DATA_SIZE_INFO = 2;
    public static final int RECEIVE_DATA_SIZE_ERROR_CODE = 2;
    public static final int RECEIVE_DATA_SIZE_VERSION = 2;
    public static final int RECEIVE_DATA_SIZE_ERP_MODE = 1;
    public static final int RECEIVE_DATA_SIZE_UNKNOW_LCB_COMM = 1;
    public static final int RECEIVE_DATA_SIZE_POS_SENSOR_CAL = 4;
    public static final int RECEIVE_DATA_SIZE_ENTER_UPDATE_LCB_MODE = 1;
    public static final int RECEIVE_DATA_SIZE_SENSORS_STATE = 2;
    public static final int RECEIVE_DATA_SIZE_SET_CALIBRATE_SENSORS = 1;
    public static final int RECEIVE_DATA_SIZE_UPDATE_PROGRAM = 1;
    public static final int RECEIVE_DATA_SIZE_SPE_EXT_CMD = 1;
    public static final int RECEIVE_DATA_SIZE_DCI_VERSION = 27;
    public static final int RECEIVE_DATA_SIZE_DCI_ENV = 84;
    public static final int RECEIVE_DATA_SIZE_CMD_EUPS_MODE = 1;
    public static final int RECEIVE_DATA_SIZE_PROTOCOL_VERSION = 2;
    public static final int RECEIVE_DATA_SIZE_MOTOR_SPEED_RTN_RPM = 2;
    public static final int RECEIVE_DATA_SIZE_TREADMILL_OPERATE = 17;
    public static final int RECEIVE_DATA_SIZE_GET_CADENCE = 16;
    public static final int RECEIVE_DATA_SIZE_ROLLER_RPM = 2;
    public static final int RECEIVE_DATA_SIZE_INCLINE_LOC = 2;
    public static final int RECEIVE_DATA_SIZE_COMPENSATION_VOL = 2;
    public static final int RECEIVE_DATA_SIZE_MOTOR_TYPE = 1;
    public static final int RECEIVE_DATA_SIZE_DCI_DATA_MAX_LEN = 1;
    public static final int RECEIVE_DATA_SIZE_TREADMILL_IN_USED = 1;
    public static final int RECEIVE_DATA_SIZE_MAIN_MOTOR_INFO = 12;
    public static final int RECEIVE_DATA_SIZE_TREADMILL_EXT_STATUS = 2;
    public static final int RECEIVE_DATA_SIZE_DCI_INCLINE_RANGE = 4;
    public static final int RECEIVE_DATA_SIZE_DCI_RPM_SPEED_RANGE = 4;
    public static final int RECEIVE_DATA_SIZE_UNIVERSAL = 3; /* cmd 1 byte + data 2 bytes*/
    public static final int RECEIVE_DATA_SIZE_ECB_RPM = 2;
    public static final int RECEIVE_DATA_SIZE_ECB_ADC = 2;
    public static final int RECEIVE_DATA_SIZE_BATTERY_VOLTAGE = 2;
    public static final int RECEIVE_DATA_SIZE_INCLINE_PERCENT = 2;
    public static final int RECEIVE_DATA_SIZE_ECB_STATUS = 1;
    public static final int RECEIVE_DATA_SIZE_ECB_COUNT = 2;
    public static final int RECEIVE_DATA_SIZE_BATTERY_CAPACITY = 2;
    public static final int RECEIVE_DATA_SIZE_CLIMBMILL_STATUS = 1;
    public static final int RECEIVE_DATA_SIZE_ENCLIMBMILL_STATUS = 2;
    public static final int RECEIVE_DATA_SIZE_BRAKE_PRESSURE_VALUE = 2;
    public static final int RECEIVE_DATA_SIZE_WATTS = 2;
    public static final int RECEIVE_DATA_SIZE_SPM = 2;
    public static final int RECEIVE_DATA_SIZE_CAL_SCALING = 2;
    public static final int RECEIVE_DATA_SIZE_FRAME_CAL_SCALING = 2;
    public static final int  RECEIVE_DATA_SIZE_INDOORBIKE_SENSORDATA            = 19;
    public static final int  RECEIVE_DATA_SIZE_TCV_FX_SPINBIKE_SENSORDATA       = 11;
    public static final int  RECEIVE_DATA_SIZE_INDOORBIKE_CALC_PARA_OF_DEVICE          = 41;
    public static final int  RECEIVE_DATA_SIZE_INDOORBIKE_ACTION_SPIN_DOWN            = 1;
    public static final int  RECEIVE_DATA_SIZE_INDOORBIKE_INFO_SPIN_DOWN          = 15;
    public static final int  RECEIVE_DATA_SIZE_INDOORBIKE_ERG_MODE_SETTINGS            = 88;
    public static final int  RECEIVE_DATA_SIZE_TREADMILL_ECB_ACTION           = 1;

    public static final int RECEIVE_DATA_SIZE_HYBRID_TM_IR_ADDR_IR_STATUS = 1;

    //0x73 rtn GetLCBVersion use
    public static final int RTN_HAVE_0X50_DATA = (int) 0xFF00;        //If return data is 0xFF00 LCB will support command 0x50/ Data 0xF0 function

    //0x7A set CmdEUPsMode use
    public static final byte CMD_EUPS_MODE_NOTIFY_ENTER_EUPS = (byte) 0xFF;        //Notify the LCB to enter the EUPs mode
    public static final byte CMD_EUPS_MODE_ASK_LEAVE_EUPS = (byte) 0x00;        //Ask that whether the LCB leaves the EUPs mode
    public static final byte CMD_EUPS_RTN_CAN_REQUISTION = (byte) 0x01;        //LCB can implement the requisition of UCB
    public static final byte CMD_EUPS_RTN_CAN_NOT_REQUISTION = (byte) 0xA1;        //LCB can not implement the requisition of UCB now
    public static final byte CMD_EUPS_RTN_NOT_SUPPORT = (byte) 0x00;    //LCB not support EUPs function.

    //0xF7 set SetLCBOperatingStatus use
    public static final byte OPERATING_STOP = (byte) 0x00;
    public static final byte OPERATING_ACTION = (byte) 0x01;

    // LCB STATUS BIT use ==>
    public enum LCB_STATUS_BIT {
        FILTER_LCB_STATUS_B0_BIT_INCLINE_DOWN_ACTION(0x01),
        FILTER_LCB_STATUS_B1_BIT_INCLINE_UP_ACTION(0x02),
        FILTER_LCB_STATUS_B2_BIT_MAIN_MOTOR_ACTION(0x04),
        FILTER_LCB_STATUS_B3_BIT_POWER_STATUS_AC(0x08),
        FILTER_LCB_STATUS_B4_BIT_BATTERY_CAP_NOT_ENOUGH(0x10),
        FILTER_LCB_STATUS_B5_BIT_COMMAND_ERROR(0x20),
        FILTER_LCB_STATUS_B6_BIT_LCB_ERROR(0x40),
        FILTER_LCB_STATUS_B7_BIT_CALIBRATION_ACTION(0x80);

        private int Value;

        LCB_STATUS_BIT(int value) {
            this.Value = value;
        }

        public static LCB_STATUS_BIT fromInt(int i) {
            for (LCB_STATUS_BIT b : LCB_STATUS_BIT.values()) {
                if (b.Value == i) {
                    return b;
                }
            }
            return null;
        }

        public int getValue() {
            return Value;
        }

        public byte getByteValue() {
            byte byteValue = (byte) Value;
            return byteValue;
        }
    }
//<==LCB STATUS BIT use 


}

