package com.jht.mastboard.device;

import static com.ftdi.j2xx.MPSSE.GPIO_Value;
import static com.ftdi.j2xx.MPSSE.I2C_TRANSFER_OPTIONS_FAST_TRANSFER;
import static com.ftdi.j2xx.MPSSE.I2C_TRANSFER_OPTIONS_NACK_LAST_BYTE;
import static com.ftdi.j2xx.MPSSE.I2C_TRANSFER_OPTIONS_START_BIT;
import static com.ftdi.j2xx.MPSSE.I2C_TRANSFER_OPTIONS_STOP_BIT;
import static com.ftdi.j2xx.MPSSE.MAX_LATENCY_TIMER;
import static com.ftdi.j2xx.MPSSE.Pins;
import static com.ftdi.j2xx.MPSSE.SPI_CONFIG_OPTION_MODE0;
import static com.ftdi.j2xx.MPSSE.SPI_TRANSFER_OPTIONS_SIZE_IN_BYTES;
import static com.jht.mastboard.device.Peripherals.PWM_CONTROLLER_PCA9685_ADDR;
import static com.jht.mastboard.device.PwmMapping.LEFT_BACKLIGHT_PWM;
import static com.jht.mastboard.device.PwmMapping.RIGHT_BACKLIGHT_PWM;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ftdi.j2xx.MPSSE;
import com.jht.mastboard.lib.PanelSide;
import com.jht.mastboard.lib.TouchData;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The mastboard communicates to the FT2232 via the MPSSE protocol through USB.
 * The following peripherals are connected to it: I2C PWM controller, 2 I2C touch sensors,
 * 2 I2C buzzer, 2 SPI LCD, and 2 I2C-to-UART.
 *
 * The FT2232 has 2 channels so we should be able to communicate with both channels at the same time.
 * -Channel A is used for: SPI, GPIO AC and AD
 * -Channel B is used for: I2C, GPIO BC and BD
 *
 * There are 2 I2C buzzers, 2 I2C touch sensors, and 2 SPI LCDs:
 * -GPIO BD5 controls sending I2C commands to the left or right I2C peripherals
 * -GPIO AD3 controls sending SPI commands to the left or right SPI peripherals
 *
 */
public class Mastboard implements I2cInterface, SpiInterface, MastboardInterface {

    final String TAG = "Mastboard";
    final boolean DEBUG = false;

    enum TransferOption {
        OPTION_1(I2C_TRANSFER_OPTIONS_START_BIT | I2C_TRANSFER_OPTIONS_FAST_TRANSFER),
        OPTION_2(I2C_TRANSFER_OPTIONS_START_BIT | I2C_TRANSFER_OPTIONS_NACK_LAST_BYTE),
        OPTION_3(I2C_TRANSFER_OPTIONS_STOP_BIT | I2C_TRANSFER_OPTIONS_START_BIT
                | I2C_TRANSFER_OPTIONS_NACK_LAST_BYTE | I2C_TRANSFER_OPTIONS_FAST_TRANSFER);

        final int Value;

        TransferOption(int value) {
            Value = value;
        }
    }

    public interface InitListener {
        void onInitComplete();
    }

    InitListener initStatus;

    final int FTDI_CHANNELS = 2;

    final MPSSE MPSSE;
    int consoleLedColor, sideLedColor, button1Color, button2Color = -1;
    int currentLEDBrightness = 100;
    int currentBacklightPercent = 100;
    boolean hapticsOn = true;
    boolean updateBrightness = false;

    SmartGrip.Side i2cSide = null;
    SmartGrip.Side spiSide = null;
    I2cPwmController i2cPwmController;
    SmartGrip leftSmartGrip, rightSmartGrip;

    final Object spiLock = new Object();
    final Object i2cLock = new Object();
    boolean isUpdating = false;
    AtomicBoolean isInitialized = new AtomicBoolean(false);
    Context context;
    int failedInitCounter = 0;
    File rebootDetectionFile;

    public static final long WRITE_SUCCESSFUL_TIMESTAMP = -1;
    AtomicLong firstWriteTimestampMs = new AtomicLong(0);

    protected Mastboard(@NonNull Context context) {
        this.context = context;
        MPSSE = new MPSSE();
        rebootDetectionFile = new File(context.getFilesDir(), "mastboard_reboot_detection");
        init();
    }

    void close() {
        // an error happened so set isInitialized to false
        isInitialized.set(false);
        MPSSE.close();
    }

    /**
     * Return if the mastboard is ready to be used
     *
     * @return
     */
    public boolean isReady() {
        // check if the FT devices are not null, if we are not updating, and if the initialization completed
        return MPSSE.isFullyInit() && !isUpdating && isInitialized.get();
    }

    /**
     * Return whether the first write failed. A failed write is determined if the write was started
     * and it exceeds a threshold.
     *
     * @return true if the first write failed; otherwise false
     */
    public boolean didFirstWriteFail() {
        // only check if the fd devices have been opened successfully
        if (MPSSE.isFullyInit()) {
            // if the first write was attempted and it didn't finish
            if (firstWriteTimestampMs.get() > 0 && firstWriteTimestampMs.get() != WRITE_SUCCESSFUL_TIMESTAMP) {
                // compute time it took
                long deltaMs = SystemClock.uptimeMillis() - firstWriteTimestampMs.get();
                // return failure if it's taking more than 100 ms
                if (deltaMs > 100) return true;
            }
        }
        return false;
    }

    /**
     * Set a callback for when the initialization completes in case we want to start a
     * touch sampling or error detection logic
     *
     * @param initStatus
     */
    synchronized public void setInitListener(@NonNull InitListener initStatus) {
        this.initStatus = initStatus;

        // trigger if already initialized
        if (isInitialized.get()) {
            initStatus.onInitComplete();
        }
    }

    /**
     * Initialize the SPI/I2C channels then setup the peripherals
     */
    synchronized public void init() {
        Log.d(TAG, "init MPSSE channels: " + MPSSE.FT_GetNumChannels() + ", isFullyInit: " + MPSSE.isFullyInit());
        // initialize if mastboard is found (when it founds the correct number of FTDI channels) and if it's not initialized
        if (MPSSE.FT_GetNumChannels() == FTDI_CHANNELS && !MPSSE.isFullyInit()) {
            try {
                MPSSE.SPI_InitChannel(15000000, MAX_LATENCY_TIMER, SPI_CONFIG_OPTION_MODE0);
                MPSSE.I2C_InitChannel(400000, 1, 0);

                // NOTE for some reason, the first ft_device write can hang on boot.
                // set the timestamp to be the current uptime
                firstWriteTimestampMs.set(SystemClock.uptimeMillis());

                // this needs to be off during start up to prevent start up sequence from showing
                set12VENpin(false);

                // set the timestamp to be successful
                firstWriteTimestampMs.set(WRITE_SUCCESSFUL_TIMESTAMP);

                // create PWM controller
                i2cPwmController = new I2cPwmController(PWM_CONTROLLER_PCA9685_ADDR, this, TransferOption.OPTION_1);
                MPSSE.SetGPIOOutput(Pins.BC4, GPIO_Value.LOW); // this enables PWM outputs. on boot this is held high

                // create left and right smart grips
                // each smart grip will contains a buzzer, touch sensor, and lcd
                // TODO add logic for detecting and resetting smart grip????
                resetSmartGripsPower();

                leftSmartGrip = new SmartGrip(this, this, this, SmartGrip.Side.LEFT);
                rightSmartGrip = new SmartGrip(this, this, this, SmartGrip.Side.RIGHT);

                // turn on disabled startup Mast Board peripherals
                setQIPower_Enable(true);
                set12VENpin(true);

                isInitialized.set(true);
                if (initStatus != null) initStatus.onInitComplete();

                // reset counter and enable logic to reboot once if we cannot find mastboard on boot since we know it was previously there
                failedInitCounter = 0;
                if (!rebootDetectionFile.exists()) rebootDetectionFile.createNewFile();

                Log.d(TAG, "init finished");

            } catch (Exception e) {
                Log.e(TAG, "This shouldn't be happening.", e);
            }
        }

        // Include a retry to work around timing issues observed with wake from ERP.
        if (!isInitialized.get()) {
            Log.d(TAG, "scheduling init retry");
            failedInitCounter++;
            if (failedInitCounter > 30) { // 30 * 500ms = 15s, this should be long enough to determine mastboard is not detected?
                // stop retrying
                Log.e(TAG, "--> Failed to find mastboard!");

                // if we need to reboot
                // TODO is there a better way to detect that the mastboard failed because the USB to mastboard is not working
                if (rebootDetectionFile.exists()) {
                    rebootDetectionFile.delete(); // delete file so we only reboot once
                    Log.w(TAG, "Rebooting from no mastboard detection");
                    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    powerManager.reboot("");
                }
            } else {
                // retry
                new Handler().postDelayed(this::init, 500);
            }
        }
    }

    /**
     * Disable/enable power for Qi charger
     * @param isON
     */
    void setQIPower_Enable(boolean isON) {
        setGPIOOutput(Pins.BOTH_QI_STATUS_PINS, isON ? GPIO_Value.HIGH : GPIO_Value.LOW);
    }

    /**
     * Enable/Disable the 12V pin
     *
     * @param isOn
     */
    void set12VENpin(boolean isOn) {
        setGPIOOutput(Pins.AC1, isOn ? GPIO_Value.HIGH : GPIO_Value.LOW);
    }

    /**
     * This is used to reset the LCM which appears to be the buzzer, touch sensors, and
     * LCDs. It will resets both sides.
     */
    void resetSmartGripsPower() {
        // this is a reset of the toggle screens and IS NECESSARY for screens to be used
        setGPIOOutput(Pins.AD6, GPIO_Value.LOW);
        SystemClock.sleep(1); // minimum of 15 us
        setGPIOOutput(Pins.AD6, GPIO_Value.HIGH);
    }

    @Override
    public void spiSend(byte command, @NonNull byte[] data) {
        if (!MPSSE.isFullyInit()) return;
        synchronized (spiLock) {
            try {
                // First Byte is the command, remainder is the data
                setCSpin(0); // start of message
                setDCpin(true); // command mode
                MPSSE.SPI_Write(new byte[]{command}, 1, SPI_TRANSFER_OPTIONS_SIZE_IN_BYTES);
                if (data.length > 0) {
                    setDCpin(false); // data mode
                    MPSSE.SPI_Write(data, data.length, SPI_TRANSFER_OPTIONS_SIZE_IN_BYTES);
                }
                setCSpin(1); // end of message
            } catch (Exception e) {
                close();
            }
        }
    }

    @Override
    public int spiRead(byte command, @NonNull byte[] data) {
        if (!MPSSE.isFullyInit()) return 0;
        synchronized (spiLock) {
            int bytesRead = 0;
            try {
                // First Byte is the command, remainder is the data
                setCSpin(0); // start of message
                setDCpin(true); // command mode
                MPSSE.SPI_Write(new byte[]{command}, 1, SPI_TRANSFER_OPTIONS_SIZE_IN_BYTES);
                bytesRead = MPSSE.SPI_Read(MPSSE.ft_device1, data, data.length, SPI_TRANSFER_OPTIONS_SIZE_IN_BYTES);
                setCSpin(1); // end of message catch (Exception e) {
            } catch (Exception e) {
                close();
            }
            return bytesRead;
        }
    }

    @Override
    public void i2cRead(byte deviceAddress, @NonNull byte[] data, @NonNull TransferOption options) {
        if (!MPSSE.isFullyInit()) return;
        synchronized (i2cLock) {
            try {
                MPSSE.I2C_DeviceRead(deviceAddress, data.length, data, options.Value);
            } catch (Exception e) {
                close();
            }
        }
    }

    @Override
    public byte i2cRead(byte deviceAddress, byte registerAddress, @NonNull TransferOption options) {
        if (!MPSSE.isFullyInit()) return 0;
        synchronized (i2cLock) {
            byte[] data = new byte[1];
            try {
                MPSSE.I2C_DeviceWrite(deviceAddress, 1, new byte[]{registerAddress}, options.Value);
                MPSSE.I2C_DeviceRead(deviceAddress, data.length, data, options.Value);
            } catch (Exception e) {
                close();
            }
            return data[0];
        }
    }

    @Override
    public void i2cRead(byte deviceAddress, byte registerAddress, @NonNull byte[] data, @NonNull TransferOption options) {
        if (!MPSSE.isFullyInit()) return;
        synchronized (i2cLock) {
            try {
                MPSSE.I2C_DeviceWrite(deviceAddress, 1, new byte[]{registerAddress}, options.Value);
                MPSSE.I2C_DeviceRead(deviceAddress, data.length, data, options.Value);
            } catch (Exception e) {
                close();
            }
        }
    }

    @Override
    public void i2cWrite(byte deviceAddress, @NonNull byte[] data, @NonNull TransferOption options) {
        if (!MPSSE.isFullyInit()) return;
        synchronized (i2cLock) {
            try {
                MPSSE.I2C_DeviceWrite(deviceAddress, data.length, data, options.Value);
            } catch (Exception e) {
                close();
            }
        }
    }

    void setGPIOOutput(Pins pin, GPIO_Value value) {
        if (!MPSSE.isFullyInit()) return;
        try {
            MPSSE.SetGPIOOutput(pin, value);
        } catch (Exception e) {
            close();
        }
    }

    /**
     * Set the DC pin for SPI
     * @param is_command
     */
    void setDCpin(boolean is_command) {
        if (is_command) {
            setGPIOOutput(Pins.AD4, GPIO_Value.LOW);
            setGPIOOutput(Pins.AC5, GPIO_Value.LOW);
        } else {
            setGPIOOutput(Pins.AD4, GPIO_Value.HIGH);
            setGPIOOutput(Pins.AC5, GPIO_Value.HIGH);
        }
    }

    /**
     * Set the CS pin for SPI
     *
     * @param isHigh
     */
    void setCSpin(int isHigh) {
        if (isHigh == 0) {
            setGPIOOutput(Pins.AD5, GPIO_Value.LOW);
            setGPIOOutput(Pins.AC4, GPIO_Value.LOW);
        } else {
            setGPIOOutput(Pins.AD5, GPIO_Value.HIGH);
            setGPIOOutput(Pins.AC4, GPIO_Value.HIGH);
        }
    }

    @Override
    public void setI2cSide(@NonNull SmartGrip.Side side) {
        if (i2cSide == side) return;
        i2cSide = side;
        setGPIOOutput(Pins.BD5, side == SmartGrip.Side.RIGHT ? GPIO_Value.HIGH : GPIO_Value.LOW);
    }

    @Override
    public void setSpiSide(@NonNull SmartGrip.Side side) {
        if (spiSide == side) return;
        spiSide = side;
        setGPIOOutput(Pins.AD3, side == SmartGrip.Side.RIGHT ? GPIO_Value.HIGH : GPIO_Value.LOW);
    }

    /**
     * Set the LED color for the specified LED enum
     *
     * @param color
     * @param led
     */
    void setLEDColor(int color, PwmMapping.LED led) {
        i2cPwmController.setPwmGroup(PwmMapping.getRegisterAddress(led), PwmMapping.convertColorToRGB(color, led, currentLEDBrightness));
    }

    /**
     * Set the console LED color
     *
     * @param color
     */
    public void setConsoleLEDColor(int color) {
        if (!isReady()) return;
        consoleLedColor = color;
        setLEDColor(color, PwmMapping.LED.Console);
    }

    /**
     * Set the side LED color
     *
     * @param color
     */
    public void setSideLEDColor(int color) {
        if (!isReady()) return;
        sideLedColor = color;
        setLEDColor(color, PwmMapping.LED.Side);
    }

    /**
     * Set the button 1 LED color
     *
     * @param color
     */
    public void setButton1LEDColor(int color) { //pause button
        if (!isReady()) return;
        if (color == button1Color) return;
        if (DEBUG) Log.d(TAG, String.format("setButton1LEDColor: %02x", color));
        button1Color = color;
        setLEDColor(color, PwmMapping.LED.Button1);
    }

    /**
     * Set the button 2 LED color
     *
     * @param color
     */
    public void setButton2LEDColor(int color) { //play button
        if (!isReady()) return;
        if (color == button2Color) return;
        if (DEBUG) Log.d(TAG, String.format("setButton2LEDColor: %02x", color));
        button2Color = color;
        setLEDColor(color, PwmMapping.LED.Button2);
    }

    /**
     * Set the all the LED color
     *
     * @param color
     */
    public void setAllLEDColor(int color) {
        if (!isReady()) return;
        if (consoleLedColor == color && sideLedColor == color
                && button1Color == color && button2Color == color && !updateBrightness) {
            return;
        }
        if (DEBUG) Log.d(TAG, String.format("setAllLEDColor: %02x", color));
        consoleLedColor = color;
        sideLedColor = color;
        button1Color = color;
        button2Color = color;
        setLEDColor(color, PwmMapping.LED.All);
        updateBrightness = false;
    }

    /**
     * Send the command through the mastboard to update all LED brightness
     *
     * @param percent
     */
    public void setLEDBrightness(int percent) {
        if (!isReady()) return;
        if (currentLEDBrightness == percent) return;
        if (DEBUG) Log.d(TAG, "setLEDBrightness: " + percent);
        currentLEDBrightness = percent;
        updateBrightness = true;
    }

    /**
     * Set the LCD backlight for the smart grips
     *
     * 0 high and 100 low
     *
     * @param percent
     */
    public void setLCDBacklight(int percent) {
        if (!isReady()) return;
        if (currentBacklightPercent == percent) return;
        if (DEBUG) Log.d(TAG, "setLCDBacklight: " + percent);
        currentBacklightPercent = percent;
        i2cPwmController.setPwm(LEFT_BACKLIGHT_PWM, percent);
        i2cPwmController.setPwm(RIGHT_BACKLIGHT_PWM, percent);
    }

    /**
     * Resets Left LCD SPI and I2C connections.
     */
    public void resetLeftLCD() {
        leftSmartGrip.reset();
    }

    /**
     * Resets Right LCD SPI and I2C connections.
     */
    public void resetRightLCD() {
        rightSmartGrip.reset();
    }

    /**
     * Buzz the specified side
     *
     * @param strength
     * @param side
     */
    public void buzz(int strength, @NonNull final PanelSide side) {
        if (!isReady()) return;
        if (!hapticsOn) return;
        if (DEBUG) Log.d(TAG, "buzz: " + strength + ", side: " + side);
        if (side == PanelSide.LEFT) {
            leftSmartGrip.buzz(strength);
        } else {
            rightSmartGrip.buzz(strength);
        }
    }

    /**
     * Reset both smart grips by cycling power then calling reset
     */
    public void fullResetLCD() {
        if (!isReady()) return;
        resetSmartGripsPower();
        leftSmartGrip.reset();
        rightSmartGrip.reset();
    }

    /**
     * Draw a bitmap on the specified LCD side
     *
     * @param bitmap
     * @param side
     */
    public void drawBitmap(Bitmap bitmap, @NonNull final PanelSide side) {
        if (!isReady()) return;
        if (DEBUG) Log.d(TAG, "drawBitmap side: " + side);
        if (side == PanelSide.LEFT) {
            leftSmartGrip.drawBitmap(bitmap);
        } else {
            rightSmartGrip.drawBitmap(bitmap);
        }
    }

    /**
     * Get the touch data for the specified side.
     * Will return null if there is no valid touch data
     *
     * @param side
     * @return touch data
     */
    public TouchData getTouchData(PanelSide side) {
        if (!isReady()) return null;

        if (side == PanelSide.LEFT) {
            return leftSmartGrip.getTouchData();
        } else {
            return rightSmartGrip.getTouchData();
        }
    }

    /**
     * Returns if the the user is touch the screen. It seems to be used
     * disable the I2C-to-UART where the user is touching the screen so
     * we don't lose any touch data with the competing I2C commands.
     *
     * @return true if the user is touching the smart grips; otherwise false
     */
    public boolean isEitherScreenInActiveTouch() {
        if (!isReady()) return true; // this is currently used to disable I2C-to-UART so return true to disable UART if not ready
        return leftSmartGrip.isTouchActive() || rightSmartGrip.isTouchActive();
    }

    /**
     * Set the setting to enable/disable buzzing
     *
     * @param hapticsOn
     */
    public void setHapticsOn(boolean hapticsOn) {
        this.hapticsOn = hapticsOn;
    }


    /**
     * Check if the SPI communication to smart grip LCD is responding
     *
     * @param side
     * @return true there is communication; false otherwise
     */
    public boolean SPIisResponding(PanelSide side) {
        // return true if mastboard is not ready so it doesn't trigger error code
        if (!isReady()) return true;

        if (side == PanelSide.LEFT) {
            return leftSmartGrip.isLcdResponding();
        } else {
            return rightSmartGrip.isLcdResponding();
        }
    }

    /**
     * Check if the I2C communication to smart grip touch sensor is responding
     *
     * @param side
     * @return true there is communication; false otherwise
     */
    public boolean I2CisResponding(PanelSide side) {
        // return true if mastboard is not ready so it doesn't trigger error code
        if (!isReady()) return true;

        if (side == PanelSide.LEFT) {
            return leftSmartGrip.isTouchSensorResponding();
        } else {
            return rightSmartGrip.isTouchSensorResponding();
        }
    }

    /**
     * Get the touch firmware version
     *
     * @return firmware versions in format of "key: value, key: value"
     */
    public String getTouchFirmwareVersion() {
        String version = String.format("L: %s, R: %s", leftSmartGrip.getTouchFirmwareVersion(), rightSmartGrip.getTouchFirmwareVersion());
        if (DEBUG) Log.d(TAG, "getTouchFirmwareVersion: " + version);
        return version;
    }

    /**
     * Helper class to compute the correct update percentage for 2 or more updates.
     * For example, we need to perform 2 touch firmware updates.
     */
    class UpdateStatusTracker implements UpdateStatus {

        public int progress = 0;
        int numUpdates;
        UpdateStatus updateStatus;

        public UpdateStatusTracker(@NonNull UpdateStatus updateStatus, int numUpdates) {
            this.updateStatus = updateStatus;
            this.numUpdates = numUpdates;
        }

        @Override
        public void onCompletionPercentage(int percent) {
            int totalPercent = progress + percent / numUpdates;
            updateStatus.onCompletionPercentage(totalPercent);
        }
    }

    /**
     * Update the touch firmware
     *
     * @param binary
     * @param updateStatus
     * @return
     */
    public synchronized boolean updateTouchFirmware(@NonNull File binary, @NonNull UpdateStatus updateStatus) {
        if (!isReady()) return false;
        isUpdating = true;
        SystemClock.sleep(1000);

        // track the update status correctly for 2 updates
        UpdateStatusTracker updateStatusTracker = new UpdateStatusTracker(updateStatus, 2);

        // update the left side
        boolean success = leftSmartGrip.updateTouchFirmware(binary, updateStatusTracker);
        // if left side update fail, return false
        if (!success) {
            isUpdating = false;
            return false;
        }

        // set the update status tracker progress to 50 since we are halfway
        updateStatusTracker.progress = 50;

        // update the right side
        success = rightSmartGrip.updateTouchFirmware(binary, updateStatusTracker);

        isUpdating = false;
        return success;
    }
}
