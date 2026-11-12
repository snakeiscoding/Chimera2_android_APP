package com.jht.mastboard.device;

import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.jht.mastboard.lib.GestureDetector;
import com.jht.mastboard.lib.TouchData;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Touch sensor for FT7311 that defines the I2C protocol
 */
public class I2cTouchSensor extends I2cDevice {
    private final String TAG = "I2cTouchSensor";
    final boolean DEBUG = false;
    byte VERSION_REGISTER = (byte) 0xA6;

    boolean foundSensor = false;
    TouchData touchData = new TouchData();
    GestureDetector gestureDetector = new GestureDetector();
    final AtomicBoolean isTouchActive = new AtomicBoolean();
    boolean downEvent = false;
    byte[] buffer = new byte[4];

    public I2cTouchSensor(byte i2cAddress, @NonNull I2cInterface i2CInterface, @NonNull Mastboard.TransferOption transferOption) {
        super(i2cAddress, i2CInterface, transferOption);
    }

    @Override
    void init() {

        int tmp = 1;
        byte[] buff = new byte[2];

        /* A4 register: Interrupt mode
         * 0x00 = INT trigger mode
         * 0x01 = INT polling mode
         * */
        buff[0] = (byte) 0xA4; //register, interrupt mode.  Default is 0x01, but still good to ensure correct value is set at init.
        buff[1] = (byte) 0x01;

        i2cWrite(buff);

        // NOTE: Not sure if this logic is necessary still since we apparently don't read the GPIO for the touch interrupt.
        // Keeping it in case we want to see if we can optimize reading I2C touch data by using the GPIO interrupt
        while (i2cRead((byte) 0xA4) != 0x01) {  //need to be absolutely certain that the write succeeded.  Have had it fail before, so this check ensures that it worked before moving forward.
            SystemClock.sleep(5);
            i2cWrite(buff);
            tmp++;
            if (tmp > 10) {
                Log.d(TAG, "Failed to set interrupt mode for LCD Touch Driver after this many tries: " + tmp);
                return;
            }
        }

        Log.d(TAG, "Was able to write to FT7311 0xA4 after this many tries : " + tmp);
        foundSensor = true;
    }

    /**
     * Get the touch data
     * @return touch data
     */
    public TouchData getTouchData() {
        i2cRead((byte) 0x03, buffer);
        return processTouchData(buffer);
    }

    /**
     * Return whether the sensor is being touched
     *
     * @return true if there is touch data; false otherwise
     */
    public boolean isTouchActive() {
        return isTouchActive.get();
    }

    /**
     * Process the touch byte array
     *
     * @param data
     * @return TouchData or null if EVENT_FLAG_LIFT_UP is detected
     */
    TouchData processTouchData(byte[] data) {
/*
        0x01 GEST_ID 0x00 [7:0]Gesture ID
        0x02 TD_STATUS 0x00 [3:0] Number of touch  points
        0x03 P1_XH 0xFF [7:6]1st  Event Flag [3:0] 1st Touch X Position[11:8]
        0x04 P1_XL 0xFF [7:0] 1st Touch X Position
        0x05 P1_YH 0xFF [7:4] 1st Touch ID [3:0] 1st Touch Y Position[11:8]
        0x06 P1_YL 0xFF [7:0] 1st Touch Y Position
        0x07 P1_WEIGHT 0xFF [7:0] 1st Touch Weight
        0x08 P1_MISC 0xFF [7:4] 1st Touch Area
 */
        // parse the touch data from the byte array
        touchData.eventFlag = (data[0] & 0xC0) >> 6;
        boolean validTouch = touchData.eventFlag == TouchData.EVENT_FLAG_TOUCH;

        touchData.x = ((int) (data[0] & 0x0F) << 8) + (int) (data[1] & 0xff);
        touchData.y = ((int) (data[2] & 0x0F) << 8) + (int) (data[3] & 0xff);
        int tmp_gesture = gestureDetector.addDataPoint(touchData.x, touchData.y, validTouch);
        touchData.gestureID = TouchData.GestureID.getGesture(tmp_gesture);

        if (touchData.gestureID == TouchData.GestureID.Push) {
            // This is trying to prevent ghost touch which seems to be happening. Not sure if it works
            if (touchData.eventFlag == TouchData.EVENT_FLAG_LIFT_UP) return null;

            if (!isTouchActive.get() && !downEvent) {
                // this is the first sample of push
                touchData.mEventAction = MotionEvent.ACTION_DOWN;
                touchData.lastTouchDownTime = SystemClock.uptimeMillis();
            } else {
                downEvent = true;
                // still held down so treat as a move
                touchData.mEventAction = MotionEvent.ACTION_MOVE;
            }
        } else if ((touchData.gestureID == TouchData.GestureID.TapRelease) ||
                (touchData.gestureID == TouchData.GestureID.MoveDown) ||
                (touchData.gestureID == TouchData.GestureID.MoveUp) ||
                (touchData.gestureID == TouchData.GestureID.MoveLeft) ||
                (touchData.gestureID == TouchData.GestureID.MoveRight)) {
            downEvent = false;
            touchData.mEventAction = MotionEvent.ACTION_UP;
        }

        isTouchActive.set(validTouch);

        return touchData;
    }

     /**
     * Check if the touch sensor is responding
     *
     * @return true if there's a response; otherwise false
     */
    public boolean isResponding() {
        // TODO if we want to reduce the amount of I2C reads, since we are polling the touch
        // data, we might be able to determine if there's a valid response from the touch
        // data bytes read instead of needed to read the chip vendor ID
        byte chipVendorID = i2cRead((byte)0xA3);
        if (chipVendorID == -1) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Gets the firmware version
     * @return firmware version
     */
    public String getFirmwareVersion() {
        int version = i2cRead(VERSION_REGISTER) & 0xFF;
        // if disconnected, we get all bits high (check all bits, or would MSB be enough?)
        // do we need more range checking for valid versions?
        if (version == 0xFF) return "?";
        // I'm assuming this is major and minor per nibble
        int major = (version >> 4);
        int minor = (version & 0xF);
        return major + "." + minor;
        //return Integer.toHexString(version & 0xFF); // this was old return, which was hard to line up to the corresponding update pack version
    }

    /**
     * The steps in this process to update the touch firmware on the LCDs comes from attachments and
     * comments on CGC-12602.
     *
     * @param binary
     * @return true if successful; otherwise false
     */
    public boolean updateTouchFirmware(@NonNull File binary, @NonNull UpdateStatus updateStatus) {
        final String subtag = " LCDFirmwareUpdate";
        byte ecc = 0; //used for error checking by computing bitwise xor for the bytes in the firmware bin file

        try {
            Log.d(TAG + subtag, "Starting updateLCDs, have the I2C lock");

            Log.d(TAG + subtag, "Converting firmware to bytes");
            byte[] firmwareBytes;
            try {
                firmwareBytes = convertFileToBytes(binary);
            } catch (IOException e) {
                Log.d(TAG + subtag, "Failed to convert file to bytes", e);
                updateStatus.onCompletionPercentage(100);
                return false;
            }
            updateStatus.onCompletionPercentage(1); // sending some heuristic progress

            if (firmwareBytes.length > 55296) { //limit of 54kB
                Log.d(TAG + subtag, "Firmware file over the size limit");
                updateStatus.onCompletionPercentage(100);
                return false;
            }
            updateStatus.onCompletionPercentage(2);

            for (byte b : firmwareBytes) {
                ecc ^= b;
            }
            Log.d(TAG + subtag, "ECC check: " + Integer.toHexString(ecc & 0xFF));
            Log.d(TAG + subtag, "Number of bytes in file: " + firmwareBytes.length);
            updateStatus.onCompletionPercentage(3);

            byte fileFirmwareVersion = firmwareBytes[24640];
            byte toggleFirmwareVersion = i2cRead(VERSION_REGISTER);

            Log.d(TAG + subtag, "Comparing toggle firmware version: " + Integer.toHexString(toggleFirmwareVersion & 0xFF) +
                    " and bin file version: " + Integer.toHexString(fileFirmwareVersion & 0xFF));
            if (fileFirmwareVersion == toggleFirmwareVersion) {
                Log.d(TAG + subtag, "Versions match, skipping update");
                updateStatus.onCompletionPercentage(100);
                return true;
            }
            Log.d(TAG + subtag, "Versions do not match, proceeding with update");
            updateStatus.onCompletionPercentage(4);


            // 1. Switch HID I2C to STD I2C
            Log.d(TAG + subtag, "Step 1: Switch HID to STD");
            if (!switchHIDtoSTD()) {
                Log.d(TAG + subtag, "Switch to STD failed");
                updateStatus.onCompletionPercentage(100);
                return false;
            }
            SystemClock.sleep(15);
            updateStatus.onCompletionPercentage(5);


            // 2. write 0xAA to 0xFC register
            //    write 0x55 to 0xFC register
            Log.d(TAG + subtag, "Step 2: Send app upgrade flag");
            i2cWrite(new byte[]{(byte) 0xFC, (byte) 0xAA});
            i2cWrite(new byte[]{(byte) 0xFC, (byte) 0x55});


            // 3. SystemClock.sleep 200ms to wait for app to set flag and soft reset, then bootloader starts, initialization complete
            Log.d(TAG + subtag, "Step 3: SystemClock.sleep to allow flag to be set, and soft reset");
            SystemClock.sleep(200);
            updateStatus.onCompletionPercentage(6);


            // 4. Switch HID I2C to STD I2C
            Log.d(TAG + subtag, "Step 4: Switch HID to STD");
            if (!switchHIDtoSTD()) {
                Log.d(TAG + subtag, "Switch to STD failed");
                updateStatus.onCompletionPercentage(100);
                return false;
            }
            SystemClock.sleep(5);
            updateStatus.onCompletionPercentage(7);


            // 5. Send upgrade command
            Log.d(TAG + subtag, "Step 5: Send upgrade command");
            i2cWrite(new byte[]{(byte) 0x55, (byte) 0xAA});
            SystemClock.sleep(5);
            updateStatus.onCompletionPercentage(8);


            // 6. Read ID number with this write, then read back 2 bytes, expect 0x54, 0x2C for success, try reading 10 times
            byte[] readID = new byte[]{(byte) 0x90, (byte) 0x00, (byte) 0x00, (byte) 0x00};
            byte[] checkID = new byte[]{(byte) 0x54, (byte) 0x2C};
            Log.d(TAG + subtag, "Step 6: Read ID number");
            if (!verifyBytes(readID, 2, 6, checkID, 5)) return false;
            updateStatus.onCompletionPercentage(9);

            Log.d(TAG + subtag, "Step 6.5: Send FTS_CMD_FLASH_MODE and FLASH_MODE_UPGRADE_VALUE");
            i2cWrite(new byte[]{(byte) 0x09, (byte) 0x0B});


            // 7. Send Firmware Total Length Command
            Log.d(TAG + subtag, "Step 7: Send total length of firmware");
            byte[] firmwareLength = computeFirmwareSize(binary);
            i2cWrite(new byte[]{(byte) 0xB0, firmwareLength[0], firmwareLength[1], firmwareLength[2]});
            SystemClock.sleep(5);
            updateStatus.onCompletionPercentage(10);


            // 8. Erase Flash - send 0x61 command and wait 1350ms
            Log.d(TAG + subtag, "Step 8: Send erase command, then SystemClock.sleep 1350ms");
            i2cWrite(new byte[]{(byte) 0x61});
            SystemClock.sleep(1350);
            updateStatus.onCompletionPercentage(11);

            byte[] getFlashStatus = new byte[]{(byte) 0x6A};
            byte[] checkFlash = new byte[]{(byte) 0xF0, (byte) 0xAA};
            verifyBytes(getFlashStatus, 2, 8, checkFlash, 10);
            updateStatus.onCompletionPercentage(12);


            // 9. Send data
            Log.d(TAG + subtag, "Step 9: Send the bin file as bytes");
            try {
                sendFilePackets(firmwareBytes);
            } catch (IOException e) {
                Log.e(TAG + subtag, "Sending data failed", e);
                updateStatus.onCompletionPercentage(100);
                return false;
            }
            updateStatus.onCompletionPercentage(96);


            // 10. verification
            byte[] verifyECC1 = new byte[]{(byte) 0xCC};
            byte[] eccCheck = new byte[]{ecc};
            Log.d(TAG + subtag, "Step 10: Two way verification of ECC");
            if (!verifyBytes(verifyECC1, 1, 10, eccCheck, 5)) return false;
            updateStatus.onCompletionPercentage(97);

            Log.d(TAG + subtag, "Pre step 11: parity reset command, obtain verification result ");
            i2cWrite(new byte[]{(byte) 0x64});
            SystemClock.sleep(5);
            Log.d(TAG + subtag, "firmwareLength: " + Integer.toHexString(firmwareLength[0] & 0xFF)
                    + " " + Integer.toHexString(firmwareLength[1] & 0xFF)
                    + " " + Integer.toHexString(firmwareLength[2] & 0xFF));
            // Calculate ECC
            i2cWrite(new byte[]{(byte) 0x65, (byte) 0x00, (byte) 0x00, firmwareLength[0], firmwareLength[1], firmwareLength[2]});
            SystemClock.sleep(firmwareBytes.length / 256);
            updateStatus.onCompletionPercentage(98);

            // Read calculated ECC
            byte[] verifyECC2 = new byte[]{(byte) 0x66};
            if (!verifyBytes(verifyECC2, 1, 10, eccCheck, 5)) return false;
            updateStatus.onCompletionPercentage(99);


            // 11. Reset
            Log.d(TAG + subtag, "Step 11: Reset");
            i2cWrite(new byte[]{(byte) 0x07});
            SystemClock.sleep(5);
            updateStatus.onCompletionPercentage(100);
        } catch (Exception e) {
            Log.e(TAG + subtag, "Update failed", e);
            return false;
        }

        return true;
    }

    boolean verifyBytes(byte[] command, int bufferSize, int step, byte[] check, int delay) {
        String subtag = " LCDFirmwareUpdate";
        byte[] buffer = new byte[bufferSize];
        i2cWrite(command);
        SystemClock.sleep(delay);

        boolean readSuccess = false;
        for (int i = 1; i <= 10; i++) {
            if (DEBUG) Log.d(TAG + subtag, "Step " + step + ": read attempt " + i);
            i2cRead(buffer);
            SystemClock.sleep(5);
            if (DEBUG) {
                for (byte b : buffer) {
                    Log.d(TAG + subtag, "Hex: " + Integer.toHexString(b & 0xFF));
                }
            }
            //We expect to read what is in the check
            if (DEBUG) Log.d(TAG + subtag, "Comparing " + Arrays.toString(buffer) + " to the expected " + Arrays.toString(check));
            if (Arrays.equals(buffer, check)) {
                readSuccess = true;
                break;
            }
        }
        //we failed to read correctly
        if (!readSuccess) {
            Log.d(TAG + subtag, "failed to read correctly for step " + step);
        }
        return readSuccess;
    }

    void sendFilePackets(byte[] firmwareBytes) throws IOException {
        String subtag = " LCDFirmwareUpdate";
        long packet_length = 128; //max size supported
        long packet_number = firmwareBytes.length / packet_length;
        long remainder = firmwareBytes.length % packet_length;
        if (remainder > 0) packet_number++;
        long offset;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = 0; i < packet_number; i++) {
            offset = i * packet_length;

            //handle final packet
            if ((i == packet_number - 1) && remainder != 0) packet_length = remainder;

            byte ADD_H = (byte) ((offset >> 16) & 0xFF); //will always be 0x00 with these file sizes
            byte ADD_M = (byte) ((offset >> 8) & 0xFF);
            byte ADD_L = (byte) (offset & 0xFF);
            byte LEN_H = (byte) ((packet_length >> 8) & 0xFF); //also always 0
            byte LEN_M = (byte) (packet_length & 0xFF);

            //create the packet
            byte[] packet_no_data = new byte[]{(byte) 0xBF, ADD_H, ADD_M, ADD_L, LEN_H, LEN_M};
            outputStream.write(packet_no_data);
            outputStream.write(firmwareBytes, Math.toIntExact(offset), Math.toIntExact(packet_length));
            byte[] full_packet = outputStream.toByteArray();
            if (DEBUG) {
                StringBuilder result = new StringBuilder();
                result.append("[");
                for (byte aByte : full_packet) {
                    result.append(String.format("%02x", aByte));
                    result.append(",");
                }
                result.append("]");
                Log.d(TAG + subtag, "full packet: " + result);
            }
            outputStream.reset();

            //send the packet
            i2cWrite(full_packet);
            SystemClock.sleep(15);

            //check for first data of 0x1000 start code
            //for 128 byte packets, this will show (0x10 Ox00), (0x10 0x01), (0x10 0x02), ...
            i2cWrite(new byte[]{(byte) 0x6A});
            byte[] buffer = new byte[2];
            i2cRead(buffer);
            SystemClock.sleep(5);
            if (DEBUG) {
                for (byte b : buffer) {
                    Log.d(TAG + subtag, "byte: " + b + " hex: " + Integer.toHexString(b & 0xFF));
                }
            }
        }
    }

    // fixme: this needs to be checked. I don't think this is the best way to do this...
    byte[] convertFileToBytes(File file) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            //read file into bytes[]
            fis.read(bytes);
        } catch (Exception ignored) {
        }

        return bytes;
    }


    //get size of firmware represented by 3 bytes
    byte[] computeFirmwareSize(File firmware) {
        byte[] result = new byte[3];

        if (firmware != null && firmware.exists()) {
            long len = firmware.length();
            result[0] = (byte) ((len >> 16) & 0xFF);
            result[1] = (byte) ((len >> 8) & 0xFF);
            result[2] = (byte) ((len) & 0xFF);
        }

        return result;
    }

    boolean switchHIDtoSTD() {
        boolean success = false;
        String subtag = " LCDFirmwareUpdate";
        byte[] buffer = new byte[3];
        i2cWrite(new byte[]{(byte) 0xEB, (byte) 0xAA, (byte) 0x09});
        if (DEBUG) Log.d(TAG + subtag, "Wrote: 0xEB 0xAA 0x09");
        i2cRead(buffer);
        if (DEBUG) {
            Log.d(TAG + subtag, "Response: " + Arrays.toString(buffer));
            for (byte b : buffer) {
                Log.d(TAG + subtag, "hex: " + Integer.toHexString(b & 0xFF));
            }
        }
        byte[] check = new byte[]{(byte) 0xEB, (byte) 0xAA, (byte) 0x08};
        success = Arrays.equals(buffer, check);

        return success;
    }
}
