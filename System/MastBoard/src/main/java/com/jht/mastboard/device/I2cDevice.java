package com.jht.mastboard.device;

import androidx.annotation.NonNull;

/**
 * Abstract base class that defines some basic functionality for an I2C device
 */
public abstract class I2cDevice {

    byte i2cAddress;
    I2cInterface i2CInterface;
    Mastboard.TransferOption transferOption;

    public I2cDevice(byte i2cAddress, @NonNull I2cInterface i2CInterface, @NonNull Mastboard.TransferOption transferOption) {
        this.i2cAddress = i2cAddress;
        this.i2CInterface = i2CInterface;
        this.transferOption = transferOption;
        init();
    }

    /**
     * Initialize the I2C device
     */
    abstract void init();

    /**
     * Helper function to simply the I2C read by passing in the I2C address and transfer option from the constructor
     *
     * Reads a byte buffer (assumes a I2C write was sent before)
     *
     * @param data
     */
    void i2cRead(byte[] data) {
        i2CInterface.i2cRead(i2cAddress, data, transferOption);
    }

    /**
     * Helper function to simply the I2C read by passing in the I2C address and transfer option from the constructor
     *
     * Reads 1 byte from the register address
     *
     * @param registerAddress
     * @return the byte read
     */
    byte i2cRead(byte registerAddress) {
        return i2CInterface.i2cRead(i2cAddress, registerAddress, transferOption);
    }

    /**
     * Helper function to simply the I2C read by passing in the I2C address and transfer option from the constructor
     *
     * Reads a byte array from the register address
     *
     * @param registerAddress
     * @param data
     */
    void i2cRead(byte registerAddress, byte[] data) {
        i2CInterface.i2cRead(i2cAddress, registerAddress, data, transferOption);
    }

    /**
     * Helper function to simply the I2C read by passing in the I2C address and transfer option from the constructor
     *
     * Writes a byte array
     *
     * @param data
     */
    void i2cWrite(byte[] data) {
        i2CInterface.i2cWrite(i2cAddress, data, transferOption);
    }

    /**
     * Re-initializes the devices
     */
    public void reset() {
        init();
    }
}
