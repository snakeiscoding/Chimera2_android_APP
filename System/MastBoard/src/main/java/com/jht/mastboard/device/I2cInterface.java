package com.jht.mastboard.device;

import androidx.annotation.NonNull;

public interface I2cInterface {

    /**
     * Reads a byte array
     * @param deviceAddress
     * @param data
     * @param options
     */
    void i2cRead(byte deviceAddress, @NonNull byte[] data, @NonNull Mastboard.TransferOption options);

    /**
     * Reads 1 byte at register address and return it
     * @param deviceAddress
     * @param registerAddress
     * @param options
     * @return
     */
    byte i2cRead(byte deviceAddress, byte registerAddress, @NonNull Mastboard.TransferOption options);

    /**
     * Reads a byte array at register address
     * @param deviceAddress
     * @param registerAddress
     * @param data
     * @param options
     */
    void i2cRead(byte deviceAddress, byte registerAddress, @NonNull byte[] data, @NonNull Mastboard.TransferOption options);

    /**
     * Write a byte array
     * @param deviceAddress
     * @param data
     * @param options
     */
    void i2cWrite(byte deviceAddress, @NonNull byte[] data, @NonNull Mastboard.TransferOption options);
}
