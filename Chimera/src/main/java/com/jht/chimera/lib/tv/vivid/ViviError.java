package com.jht.chimera.lib.tv.vivid;


public enum ViviError {
    SUCCESS                                 (0x00),

    UART_ERROR_WRONG_MESSAGE_CRC            (0xA0),
    UART_ERROR_WRONG_MESSAGE_FORMAT         (0xA1),
    UART_ERROR_WRONG_MESSAGE_LENGTH         (0xA2),

    UART_ERROR_COMMUNICATION_FAIL           (0xB0),
    UART_ERROR_COMMUNICATION_LOST           (0xB1),
    UART_ERROR_COMMUNICATION_RX_TIMEOUT     (0xB2);

    private int Value;

    ViviError(int value) {
        this.Value = value;
    }

    public static ViviError fromInt(int i) {
        for (ViviError b : ViviError.values()) {
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
