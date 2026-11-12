package com.jht.mastboard.device;

import static com.jht.mastboard.device.Peripherals.BUZZER_DRV2605_ADDR;
import static com.jht.mastboard.device.Peripherals.TOUCH_SENSOR_FT7311_ADDR;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.jht.mastboard.lib.TouchData;

import java.io.File;

/**
 * Smart grip is the combination of the buzzer, touch sensor, and LCD for one side.
 *
 * The mastboard has a GPIO output to switch the I2C side and a different GPIO output to
 * switch the SPI side. This class has static lockI2cSide and lockSpiSide so to lock
 * logic that requires setting the I2C/SPI communication side.
 */
public class SmartGrip {

    public enum Side {LEFT, RIGHT}

    static final Object lockI2cSide = new Object();
    static final Object lockSpiSide = new Object();

    MastboardInterface mastboardInterface;
    I2cTouchSensor i2cTouchSensor;
    I2cBuzzer i2cBuzzer;
    SpiLCD spiLCD;
    Side side;

    public SmartGrip(@NonNull I2cInterface i2CInterface, @NonNull SpiInterface spiInterface, @NonNull MastboardInterface mastboardInterface, @NonNull Side side) {
        this.mastboardInterface = mastboardInterface;
        this.side = side;

        synchronized (lockI2cSide) {
            mastboardInterface.setI2cSide(side);
            i2cTouchSensor = new I2cTouchSensor(TOUCH_SENSOR_FT7311_ADDR, i2CInterface, Mastboard.TransferOption.OPTION_3);
            i2cBuzzer = new I2cBuzzer(BUZZER_DRV2605_ADDR, i2CInterface, Mastboard.TransferOption.OPTION_1);
        }

        synchronized (lockSpiSide) {
            mastboardInterface.setSpiSide(side);
            spiLCD = new SpiLCD(spiInterface);
        }
    }

    /**
     * Buzz with the specified strength
     *
     * Make sure to lock the I2C side since buzzer is I2C device
     *
     * @param strength
     */
    public void buzz(int strength) {
        synchronized (lockI2cSide) {
            mastboardInterface.setI2cSide(side);
            i2cBuzzer.buzz(strength);
        }
    }

    /**
     * Get the touch data
     *
     * Make sure to lock the I2C side since touch sensor is I2C device
     *
     * @return touch data
     */
    public TouchData getTouchData() {
        synchronized (lockI2cSide) {
            mastboardInterface.setI2cSide(side);
            return i2cTouchSensor.getTouchData();
        }
    }

    /**
     * Draw a bitmap to LCD
     *
     * Make sure to lock SPI side since LCD is a SPI device
     *
     * @param bitmap
     */
    public void drawBitmap(Bitmap bitmap) {
        synchronized (lockSpiSide) {
            mastboardInterface.setSpiSide(side);
            spiLCD.drawBitmap(bitmap);
        }
    }

    /**
     * Reset the buzzer, touch sensor, and LCD
     *
     * Make sure to lock the I2C/SPI side for the device type
     */
    public void reset() {
        synchronized (lockSpiSide) {
            mastboardInterface.setSpiSide(side);
            spiLCD.reset();
        }
        synchronized (lockI2cSide) {
            mastboardInterface.setI2cSide(side);
            i2cBuzzer.reset();
            i2cTouchSensor.reset();
        }
    }

    /**
     * Return whether there is touch data
     * @return
     */
    public boolean isTouchActive() {
        return i2cTouchSensor.isTouchActive();
    }

    /**
     * Check if the LCD is responding
     *
     * @return true if there is a response; otherwise false
     */
    public boolean isLcdResponding() {
        synchronized (lockSpiSide) {
            mastboardInterface.setSpiSide(side);
            return spiLCD.isResponding();
        }
    }

    public boolean isTouchSensorResponding() {
        synchronized (lockI2cSide) {
            mastboardInterface.setI2cSide(side);
            return i2cTouchSensor.isResponding();
        }
    }

    /**
     * Get the touch sensor firmware version
     * @return
     */
    public String getTouchFirmwareVersion() {
        synchronized (lockI2cSide) {
            mastboardInterface.setI2cSide(side);
            return i2cTouchSensor.getFirmwareVersion();
        }
    }

    /**
     * Update the touch sensor firmware
     * @param binary
     * @param updateStatus
     * @return true if successful; otherwise false
     */
    public boolean updateTouchFirmware(@NonNull File binary, @NonNull UpdateStatus updateStatus) {
        synchronized (lockI2cSide) {
            mastboardInterface.setI2cSide(side);
            return i2cTouchSensor.updateTouchFirmware(binary, updateStatus);
        }
    }
}
