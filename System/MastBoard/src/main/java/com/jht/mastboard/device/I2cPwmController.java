package com.jht.mastboard.device;

import androidx.annotation.NonNull;

/**
 * PWM controller for PCA9685 that defines the I2C protocol
 */
public class I2cPwmController extends I2cDevice {

    // PWM register address
    public static final byte PWM0 = (byte) 0x06;
    public static final byte PWM1 = (byte) 0x0A;
    public static final byte PWM2 = (byte) 0x0E;
    public static final byte PWM3 = (byte) 0x12;
    public static final byte PWM4 = (byte) 0x16;
    public static final byte PWM5 = (byte) 0x1A;
    public static final byte PWM6 = (byte) 0x1E;
    public static final byte PWM7 = (byte) 0x22;
    public static final byte PWM8 = (byte) 0x26;
    public static final byte PWM9 = (byte) 0x2A;
    public static final byte PWM10 = (byte) 0x2E;
    public static final byte PWM11 = (byte) 0x32;
    public static final byte PWM12 = (byte) 0x36;
    public static final byte PWM13 = (byte) 0x3A;
    public static final byte PWM14 = (byte) 0x3E;
    public static final byte PWM15 = (byte) 0x42;
    public static final byte PWM_ALL = (byte) 0xFA;

    public I2cPwmController(byte i2cAddress, @NonNull I2cInterface i2CInterface, @NonNull Mastboard.TransferOption transferOption) {
        super(i2cAddress, i2CInterface, transferOption);
    }

    @Override
    void init() {

        // Turns restart enabled, autoinc disabled, and low power mode on
        i2cWrite(new byte[]{(byte) 0x00, (byte) 0x00}); //MODE1 register was 0001 0000

        // NOT NEEDED? and does not cause flash.
        // Turns restart enabled, autoinc disabled, and low power mode on
        i2cWrite(new byte[]{(byte) 0x00, (byte) 0x90}); //MODE1 register 1001 0000

        // if this is commented out will allow fan to be on fan.
        // Turns restart enabled, autoinc enabled, and normal mode on
        i2cWrite(new byte[]{(byte) 0x00, (byte) 0xA0}); // MODE1 1010 0000

        // if this is commented out will allow fan to be on full blast
        // Output logic inverted, out change on ack, totem pole structure and drivers not enabled
        i2cWrite(new byte[]{(byte) 0x01, (byte) 0x1C}); //MODE2 register 0001 1100

        // set all PWM to 0
        setPwm(PWM_ALL, 0);

        // Set scalar for PWM
        i2cWrite(new byte[]{(byte) 0xFE, (byte) 0x03});
    }

    /**
     * Convert an integer and returns the byte array representation
     * 4095 is the max int value
     * 0 is represented by high byte 0x10 and low byte 0x00
     *
     * @param percent
     * @return two bytes - low byte followed by high byte
     */
    byte[] getLEDOnChannelByteValues(int percent) {
        int ON_count = (int) Math.round(percent / 100.0 * 4095.0);
        byte lowON_count = (byte) (ON_count & 0xFF);
        byte highON_count = (byte) (ON_count >> 8 & 0xFF);

        if (ON_count == 0) {
            lowON_count = 0;
            highON_count = 0x10;
        }

        return new byte[]{lowON_count, highON_count};
    }


    /**
     * Set the PWM value for a group of consecutive PWMs
     *
     * @param registerAddress
     * @param percentArray
     */
    public void setPwmGroup(byte registerAddress, @NonNull int[] percentArray) {

        // register address byte + 4 bytes per PWM value
        byte[] commandBytes = new byte[1 + percentArray.length * 4];
        byte[] ON_count;
        int index = 0;

        // first byte is register address
        commandBytes[index++] = registerAddress;

        for (int percent : percentArray) {
            // updating the PWM value which takes 4 bytes
            ON_count = getLEDOnChannelByteValues(percent);
            commandBytes[index++] = ON_count[0];
            commandBytes[index++] = ON_count[1];
            commandBytes[index++] = 0;
            commandBytes[index++] = 0;
        }
        i2cWrite(commandBytes);
    }

    /**
     * Set the PWM value
     *
     * @param registerAddress
     * @param percent
     */
    public void setPwm(byte registerAddress, int percent) {
        setPwmGroup(registerAddress, new int[]{percent});
    }

}
