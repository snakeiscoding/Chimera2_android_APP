package com.jht.mastboard.device;

public interface SpiInterface {
    /**
     * Sends the data array for the byte command
     * @param command
     * @param data
     */
    void spiSend(byte command, byte[] data);

    /**
     * Reads the data array for the byte command
     * @param command
     * @param data
     * @return
     */
    int spiRead(byte command, byte[] data);
}
