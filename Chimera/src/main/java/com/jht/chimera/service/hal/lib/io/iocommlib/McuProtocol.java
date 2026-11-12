package com.jht.chimera.service.hal.lib.io.iocommlib;


public class McuProtocol {

    public static final byte LED_ERP_OFF =  0x00;
    public static final byte LED_ERP_ON =   0x01;
    public static final byte LED_RFID_OFF = 0x02;
    public static final byte LED_RFID_ON =  0x03;

    public enum McuCommands{
        //Control Commands
        TONE(              (byte)0x00, (byte)0x01),
        FAN(               (byte)0x00, (byte)0x02),
        POWER(             (byte)0x00, (byte)0x03),
        ERP(               (byte)0x00, (byte)0x04),
        DEBUG(             (byte)0x00, (byte)0x05),
        REBOOT(            (byte)0x00, (byte)0x06),
        IWDG(              (byte)0x00, (byte)0x07),
        RS485(             (byte)0x00, (byte)0x08),
        QI(                (byte)0x00, (byte)0x09),
        HW_MONITOR_SWITCH( (byte)0x00, (byte)0x0B),
        KEY_SETTING(       (byte)0x00, (byte)0x0C),
        ERP_LED(           (byte)0x00, (byte)0x0D),
        LED_COLOR(         (byte)0x00, (byte)0x11),
        //Watchdog Commands
        WATCHDOG(          (byte)0x01, (byte)0x00),
        REFRESH(           (byte)0x01, (byte)0x01),
        //Event Commands
        KEY_EVENT(         (byte)0x02, (byte)0x00),
        SAFEKEY_EVENT(     (byte)0x02, (byte)0x01),
        DEBUG_MESSAGE(     (byte)0x02, (byte)0x02),
        LCB_STATUS(        (byte)0x02, (byte)0x03),
        HW_MONITOR_EVENT(  (byte)0x02, (byte)0x04),
        SOC_EVENT(         (byte)0x02, (byte)0x05), // payload is U16, value of 1 is soc reboot event
        HEART_RATE_EVENT(  (byte)0x02, (byte)0x06),
        //Information/State Commands
        VERSION(           (byte)0x03, (byte)0x00),
        SAFEKEY(           (byte)0x03, (byte)0x01),
        POWER_STATE(       (byte)0x03, (byte)0x02),
        BOOT(              (byte)0x03, (byte)0x03),
        LCB_TYPE(          (byte)0x03, (byte)0x04),
        RECOVERY_STATE(    (byte)0x03, (byte)0x05),
        FAN_SPEED(         (byte)0x03, (byte)0x06),
        HW_MONITOR_STATE(  (byte)0x03, (byte)0x08),
        HW_MONITOR(        (byte)0x03, (byte)0x0A),
        //Firmware Update Commands
        UPDATE_PARAMETERS( (byte)0x04, (byte)0x00),
        ERASE(             (byte)0x04, (byte)0x01),
        WRITE_PAGE(        (byte)0x04, (byte)0x02),
        READ_PAGE(         (byte)0x04, (byte)0x03),
        START_UPDATE(      (byte)0x04, (byte)0x04),
        END_UPDATE(        (byte)0x04, (byte)0x05),
        OPERATION_MODE(    (byte)0x04, (byte)0x06),
        APP_VERSION(       (byte)0x04, (byte)0x07),
        PROGRAM_STATE(     (byte)0x04, (byte)0x08),
        //CEC
        SET_CEC_CONFIG(    (byte)0x05, (byte)0x00),
        SEND_CEC_COMMAND(  (byte)0x05, (byte)0x01),
        GET_CEC_RESPONSE(  (byte)0x05, (byte)0x02),
        //IR
        SEND_IR_COMMAND(   (byte)0x06, (byte)0x01),
        SEND_IR_FREQUENCY(   (byte)0x06, (byte)0x02),
        // Pass through
        SEND_BUMP_SENSOR_COMMAND(   (byte)0x07, (byte)0x06),
        SEND_CAB_COMMAND(   (byte)0x07, (byte)0x07),
        //CAN
        CAN_TX(       (byte)0x07, (byte)0x08),
        CAN_RX(      (byte)0x07, (byte)0x09);

        // END of command list


        private final byte command_1;
        private final byte command_2;

        McuCommands(byte command_1, byte command_2){
            this.command_1 = command_1;
            this.command_2 = command_2;
        }

        public static McuCommands fromInt(int i) {
            for (McuCommands b : McuCommands.values()) {
                if (b.get_command() == i) return b;
            }
            return null;
        }

        public int get_command(){
            return (command_1 << 8) + command_2;
        }

        public byte get_command_1(){
            return command_1;
        }

        public byte get_command_2(){
            return command_2;
        }

        public boolean equals(int value) {
            return (value == this.get_command());
        }

    }

    public enum McuStatus {
        Success(0),
        UnknownCommand(1),
        InvalidPayloadLength(2),
        InvalidParameter(3),
        InvalidAddress(4),
        InvalidPassword(5),
        InvalidChecksum(6),
        WriteError(7);

        private final int value;
        McuStatus(int value) {
            this.value = value;
        }
        public int getValue() {
            return this.value;
        }
        public boolean equals(int value) {
            return (value == this.value);
        }
        public static McuStatus fromInt(int i) {
            for (McuStatus b : McuStatus.values()) {
                if (b.getValue() == i) return b;
            }
            return null;
        }
    }

}
