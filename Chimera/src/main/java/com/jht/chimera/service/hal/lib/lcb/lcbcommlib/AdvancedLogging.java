package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

public class AdvancedLogging {
    private static final String TAG = "AdvancedLogging";
    private byte byteLoggingStatus = (byte) 0x00;

    private AdvancedLogging() {
    }

    private static class SingletonHolder {
        private static AdvancedLogging Instance = new AdvancedLogging();
    }

    public static AdvancedLogging getInstance() {
        return SingletonHolder.Instance;
    }

    // LOGGING STATUS BIT use ==>
    public enum LOGGING_STATUS_BIT {
        FILTER_LOGGING_STATUS_B0_BIT_MESSAGES(0x01),
        FILTER_LOGGING_STATUS_B1_BIT_MESSAGE_DETAILS(0x02);

        private int Value;

        LOGGING_STATUS_BIT(int value) {
            this.Value = value;
        }

        public static LOGGING_STATUS_BIT fromInt(int i) {
            for (LOGGING_STATUS_BIT b : LOGGING_STATUS_BIT.values()) {
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
//<==LOGGING STATUS BIT use 

    /*******************************************************************************
     * Function Name  : setAdvLoggingStatus()
     * Description    : set byteLoggingStatus
     * Input          : byte byteStatus
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAdvLoggingStatus(byte byteStatus) {
        byteLoggingStatus = byteStatus;
    }

    /*******************************************************************************
     * Function Name  : getAdvLoggingStatus()
     * Description    : get byteLoggingStatus
     * Input          : byte byteLoggingStatus
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public byte getAdvLoggingStatus() {
        return byteLoggingStatus;
    }

}

