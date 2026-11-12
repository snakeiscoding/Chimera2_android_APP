package com.jht.chimera.service.hal.lib.salutron;


public class salutronCommands {

    public static final byte CLASS_1C = 0x1C;
    public static final byte CLASS_1E = 0x1E;


    public enum DeviceCommands {

        //class + SUB
        //HR Data
        HRM_5Khz_name(0x0C01),
        HRM_BLE_name(0x0C02),
        HRM_BLE_FullName(0x0C12),
        HRM_ANT_ID(0x0C03),

        BLE_HR_DATA(0x0C20),
        ANT_HR_DATA(0x0C21),
        WL5K_HR_DATA(0x0C22),
        CONTACT_HR_DATA(0x0C23),

        //RR Data
        BLE_RRData(0x0F11),
        ANT_RRData(0x0F12),
        Contact_RRData(0x0F14),

        //Error Code
        HRM_EVENT(0x0C05),

        //Status
        Status(0x0C06),

        //Acknowledge
        ACLK(0x0C07),

        //software Version
        Firmware_Version(0x0C08),

        //Contact status
        Contact_Stat(0x0C09),

        ANT_Device(0x0C10),

        //RSSI
        BLE_RSSI(0x0D10),
        ANT_RSSI(0x0D20),

        //BLE MAC
        BLE_MAC(0x0C13),

        RESET(0x0E30),

        //ANT Status
        ANT_STATUS(0x1C0A),

        Disconnect_all_success(0x2922);

        private int Value;

        DeviceCommands(int value) {
            Value = value;
        }

        public static DeviceCommands fromInt(int i) {
            for (DeviceCommands b : DeviceCommands.values()) {
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

    //0x05
    public enum HRM_Event {
        //5kHZ error code
        Connection_lose(0x01),
        Corrupted_HR(0x02),
        Relearn(0x03),
//        Wireless_Noisy(0x04),
        Noisy_Off(0x05),
        Wireless_Over_Limit(0x06),
        Cross_Talk(0x07),

        //Common error
        CON_lost(0x31), //ANT or BLE
        CON_failed(0x32), //ANT or BLE
        Device_not_found(0x33),
        Scanning_Done(0x34),
        Device_connected(0x35),

        //Package error
        Packet_Corrupted(0x41),
        Buffer_Full(0x42),


        //Contact error
        CT_Connection_lost(0x51),
        CT_Corrupted_HR(0x52),
        CT_Relearn(0x53),
        CT_Noisy(0x54),
        //CT_Noisy_Off(0x55),
        CT_Over_Limit(0x56);

        private int Value;

        HRM_Event(int value) {
            Value = value;
        }

        public static HRM_Event fromInt(int i) {
            for (HRM_Event b : HRM_Event.values()) {
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

    //0x06
    public enum Status {
        STAT_Disconnected(0x01),
        STAT_SearchDevice(0x02),
        STAT_DeviceFound(0x04),
        STAT_IdleMode(0x05),
        STAT_Connected(0x06),
        STAT_Scanning(0x07),
        STAT_StopScanning(0x08),
        STAT_WorkoutStart(0x09),
        STAT_WorkoutStop(0x0A),
        STAT_5KHZ_Active(0x0B),
        STAT_Contat_Active(0x0C),
        STAT_FEC_Status(0x0D);

        private int Value;

        Status(int value) {
            Value = value;
        }

        public static Status fromInt(int i) {
            for (Status b : Status.values()) {
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

    public enum SalutronCommand {

        //Workout
        START(CLASS_1C, (byte) 0x04, (byte) 0x01),
        START_RSSI_96(CLASS_1C, (byte) 0x04, (byte) 0x60),
        STOP(CLASS_1C, (byte) 0x04, (byte) 0x04),
        RESET(CLASS_1C, (byte) 0x09, (byte) 0x01),
        
        ENABLE_BLE_UPDATE(CLASS_1E, (byte) 0x04, (byte) 0x01),
        ENABLE_SERIAL_UPDATE(CLASS_1E, (byte) 0x04, (byte) 0x02),

        //Scanning
        SCANNING(CLASS_1C, (byte) 0x05, (byte) 0x01),

        //Get Status
        GET_STATUS(CLASS_1C, (byte) 0x06, (byte) 0x01),
        GET_VER(CLASS_1C, (byte) 0x06, (byte) 0x02),

        //5KHZ command
        AUTO_CONNECT(CLASS_1C, (byte) 0x07, (byte) 0x01),
        DIS_AUTO_CONNECT(CLASS_1C, (byte) 0x07, (byte) 0x02),

        //Contact Option
        CONTACT_OVERRIDE_ON(CLASS_1C, (byte) 0x08, (byte) 0x01),
        CONTACT_OVERRIDE_OFF(CLASS_1C, (byte) 0x08, (byte) 0x02),
        CONTACT_RR_ENABLE(CLASS_1C, (byte) 0x08, (byte) 0x03),
        CONTACT_RR_DISABLE(CLASS_1C, (byte) 0x08, (byte) 0x04),

        DISCONNECT_5K((byte) 0x26,(byte) 0x21, (byte)0x01),
        DISCONNECT_ANT((byte) 0x27,(byte) 0x21, (byte)0x01),
        DISCONNECT_BLE((byte) 0x28,(byte) 0x21, (byte)0x01),
        DISCONNECT_ALL((byte) 0x29,(byte) 0x21, (byte)0x01),

        //BLE Option
//        BLE_BROADCAST_START(CLASS_1E, (byte) 0x01, (byte) 0x01),
//        BLE_BROADCAST_STOP(CLASS_1E, (byte) 0x01, (byte) 0x02),
        BLE_RR_ENABLE(CLASS_1E, (byte) 0x01, (byte) 0x03),
        BLE_RR_DISABLE(CLASS_1E, (byte) 0x01, (byte) 0x04),
        BLE_DISABLE_FULLNAME(CLASS_1E, (byte) 0x12, (byte) 0x00),
        BLE_ENABLE_FULLNAME(CLASS_1E, (byte) 0x12, (byte) 0x01),
        GET_MAC_ADDRESS(CLASS_1E, (byte) 0x13, (byte) 0x01),
        BLE_NAME(CLASS_1E, (byte)0x14, (byte) 0x00),
        ENABLE_BLE_HEART_RATE_PROFILE(CLASS_1E, (byte)0x05, (byte) 0x01),
        DISABLE_BLE_HEART_RATE_PROFILE(CLASS_1E, (byte)0x05, (byte) 0x00),

        // ANT Options
//        ENABLE_FEC(CLASS_1E, (byte) 0x02, (byte) 0x01),
//        DISABLE_FEC(CLASS_1E, (byte) 0x02, (byte) 0x02),
        ANT_RR_ENABLE(CLASS_1E, (byte) 0x02, (byte) 0x01),
        ANT_RR_DISABLE(CLASS_1E, (byte) 0x02, (byte) 0x02),
        GET_ANT_STATUS(CLASS_1C, (byte)0x0A, (byte)0xFF),
        ANT_ID((byte) 0x17, (byte) 0x01, (byte) 0x00),

        ENABLE_FE_PROFILE((byte) 0x1F, (byte) 0x20, (byte) 0x01),
        DIAABLE_FE_PROFILE((byte) 0x1F, (byte) 0x20, (byte) 0x02),
        FE_TRANSMISSION_PATTERN((byte) 0x1F, (byte) 0x24, (byte) 0x00),

        FEC_STATIONARY_BIKE_PAGE_21((byte) 0x1F,(byte) 0x21, (byte) 0x01),
        FEC_STATIONARY_BIKE_PAGE_25((byte) 0x1F,(byte) 0x21, (byte) 0x00),

        ENABLE_PWR_PROFILE((byte) 0x20, (byte) 0x20, (byte) 0x01),
        DISABLE_PWR_PROFILE((byte) 0x20, (byte) 0x20, (byte) 0x02),
        PWR_TRANSMISSION_PATTERN((byte) 0x20, (byte) 0x24, (byte) 0x00),

        ENABLE_ANT_EXD((byte) 0x24, (byte) 0x20, (byte) 0x01),
        DISABLE_ANT_EXD((byte) 0x24, (byte) 0x20, (byte) 0x02),

        ENABLE_ANT_BSC_PROFILE((byte) 0x21, (byte) 0x20, (byte) 0x01),
        DISABLE_ANT_BSC_PROFILE((byte) 0x21, (byte) 0x20, (byte) 0x02),

        ENABLE_ANT_HEART_RATE_PROFILE((byte) 0x25, (byte) 0x20, (byte) 0x01),
        DISABLE_ANT_HEART_RATE_PROFILE((byte) 0x25, (byte) 0x20, (byte) 0x02);

        private byte commandClass;
        private byte subType;
        private byte subData;

        SalutronCommand(byte commandClass, byte subtype, byte subData) {
            this.commandClass = commandClass;
            this.subType = subtype;
            this.subData = subData;
        }

        public byte getCommandClass() {
            return commandClass;
        }

        public byte getSubType() {
            return subType;
        }

        public byte getSubData() {
            return subData;
        }

    }

    public enum ProfilePage {

        FE_PAGE_01((byte) 0x1F, (byte) 0x25, (byte) 0x01),
        FE_PAGE_02((byte) 0x1F, (byte) 0x25, (byte) 0x02),
        FE_PAGE_16((byte) 0x1F, (byte) 0x25, (byte) 0x10),
        FE_PAGE_17((byte) 0x1F, (byte) 0x25, (byte) 0x11),
        FE_PAGE_18((byte) 0x1F, (byte) 0x25, (byte) 0x12),
        FE_PAGE_19((byte) 0x1F, (byte) 0x25, (byte) 0x13),
        FE_PAGE_20((byte) 0x1F, (byte) 0x25, (byte) 0x14),
        FE_PAGE_21((byte) 0x1F, (byte) 0x25, (byte) 0x15),
        FE_PAGE_22((byte) 0x1F, (byte) 0x25, (byte) 0x16),
        FE_PAGE_23((byte) 0x1F, (byte) 0x25, (byte) 0x17),
        FE_PAGE_24((byte) 0x1F, (byte) 0x25, (byte) 0x18),
        FE_PAGE_25((byte) 0x1F, (byte) 0x25, (byte) 0x19),
        FE_PAGE_26((byte) 0x1F, (byte) 0x25, (byte) 0x1A),
        FE_PAGE_54((byte) 0x1F, (byte) 0x25, (byte) 0x36),
        FE_PAGE_55((byte) 0x1F, (byte) 0x25, (byte) 0x37),
        FE_PAGE_80((byte) 0x1F, (byte) 0x28, (byte) 0x50),
        FE_PAGE_81((byte) 0x1F, (byte) 0x28, (byte) 0x51),
        //Bike Power Data
        PWR_PAGE_16((byte) 0x20, (byte) 0x25, (byte) 0x10),
        PWR_PAGE_17((byte) 0x20, (byte) 0x25, (byte) 0x11),
        PWR_PAGE_18((byte) 0x20, (byte) 0x25, (byte) 0x12),
        PWR_PAGE_32((byte) 0x20, (byte) 0x25, (byte) 0x20),
        PWR_PAGE_80((byte) 0x20, (byte) 0x28, (byte) 0x50),
        PWR_PAGE_81((byte) 0x20, (byte) 0x28, (byte) 0x51),

        PWR_PAGE_02((byte) 0x20, (byte) 0x26, (byte) 0x02),

        //Bike Power Calibration
        PWR_PAGE_01((byte) 0x20, (byte) 0x29, (byte) 0x01),
        PWR_PAGE_03((byte) 0x20, (byte) 0x29, (byte) 0x03),

        //Bike Combined Speed/Cadence Data
        BCS_PAGE_0((byte) 0x21, (byte) 0x25, (byte) 0x00);

        private byte commandClass;
        private byte subType;
        private byte pageNum;

        ProfilePage(byte commandClass, byte subType, byte pageNum) {
            this.commandClass = commandClass;
            this.subType = subType;
            this.pageNum = pageNum;
        }

        public byte getCommandClass() {
            return commandClass;
        }

        public byte getSubType() {
            return subType;
        }

        public byte getPageNum() {
            return pageNum;
        }
    }

    public enum FTMS {

        SET_FTMS_CONFIG(CLASS_1E, (byte) 0x20),
        SEND_FTMS_TREADMILL(CLASS_1E, (byte) 0x21),
        SEND_FTMS_INDOORBIKE(CLASS_1E, (byte) 0x22),
        SEND_FTMS_CROSSTRAINER(CLASS_1E, (byte) 0x23),
        SEND_FTMS_STATUS(CLASS_1E, (byte) 0x24),
        SEND_FTMS_CONTROL_POINT(CLASS_1E, (byte) 0x25),
        SEND_FTMS_SUPPORTED_RANGE(CLASS_1E, (byte) 0x26),
        SEND_FTMS_STEP_CLIMBER(CLASS_1E, (byte) 0x27),
        SEND_FTMS_STAIR_CLIMBER(CLASS_1E, (byte) 0x28),
        SEND_FTMS_ROWER(CLASS_1E, (byte) 0x29),
        CHANGE_BLE_ADVERTISING_NAME(CLASS_1E, (byte) 0x14);


        private byte commandClass;
        private byte subType;

        FTMS(byte commandClass, byte subType) {
            this.commandClass = commandClass;
            this.subType = subType;
        }

        public byte getCommandClass() {
            return commandClass;
        }

        public byte getSubType() {
            return subType;
        }
    }

    public enum DeviceType {

        FIVE_KHZ((byte) 0x1C, (byte) 0x01),
        BLE((byte) 0x1C, (byte) 0x02),
        ANT_HR((byte) 0x1C, (byte) 0x03),
        ANT_EXD((byte) 0x24, (byte) 0x60);

        private byte commandClass;
        private byte Sub;

        DeviceType(byte commandClass, byte Sub) {
            this.commandClass = commandClass;
            this.Sub = Sub;
        }

        public byte getSub() {
            return Sub;
        }

        public byte getCommandClass(){
            return commandClass;
        }

    }

}
