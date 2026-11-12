package com.jht.chimera.service.hal.lib.hardware;

import android.os.SystemClock;
import android.util.Log;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Abstracted IR Driver to handle low-level IR operations.
 */
public class IrDriver implements AutoCloseable {
    private static final String TAG = "IrDriver";

    // LIRC Constants
    public static final int LIRC_MODE_PULSE = 0x00000002;
    public static final int IR_FREQUENCY_DEFAULT = 38000;
    public static final int IR_DUTY_DEFAULT = 99;

    private int mFd = -1;
    private final String mDevicePath;
    private final Object mWriteLock = new Object();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    
    public IrDriver(String devicePath) {
        this.mDevicePath = devicePath;
    }

    /**
     * Opens the IR device and sets permissions.
     * @return true if successful.
     */
    public boolean open() {
        if (mDevicePath == null) {
            Log.w(TAG, "No IR device path defined in PortMappings");
            return false;
        }

        if (mFd >= 0) {
            close();
        }

        ExecuteAsRoot.run("chmod 777 " + mDevicePath);
        mFd = Driver.safeOpen(mDevicePath);

        if (mFd < 0) {
            Log.e(TAG, "Failed to open IR device: " + mDevicePath);
            return false;
        }
        return true;
    }

    /**
     * Configures IR parameters.
     */
    public boolean configure(int frequencyHz, int dutyCycle, int mode) {
        if (mFd < 0) return false;

        /* we could cache the configuration to re-apply if needed
        mCurrentFreq = frequencyHz;
        mCurrentDuty = dutyCycle;
        mCurrentMode = mode;*/

        try {
            // Linux LIRC driver (LIRC_SET_SEND_CARRIER) rejects carrier frequency 0 with -EINVAL (-22).
            // For baseband / unmodulated mode (0 Hz), use 1 Hz carrier to prevent carrier modulation
            // while satisfying the kernel driver's positive frequency requirement.
            int carrierFreq = (frequencyHz == 0) ? 1 : frequencyHz;
            int ret = Driver.setIrParameters(mFd, carrierFreq, dutyCycle, mode);
            if (ret == 0) return true;
            Log.w(TAG, "Driver.setIrParameters returned error: " + ret);
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Driver.setIrParameters not found, ensure chimera_drivers 2026.7.10.0+ is loaded", e);
        }
        return false;
    }

    /**
     * Queues an IR signal for sending. This implementation uses a SingleThreadExecutor
     * to ensure that only one write operation happens at a time, addressing concurrency
     * concerns raised during code review.
     * 
     * @param timings The pulse data
     * @param repeats Number of additional times to send the signal
     */
    public void queueWrite(int[] timings, int repeats) {
        if (mExecutor.isShutdown()) return;
        
        mExecutor.execute(() -> {
            // Ensure thread-safe access to the File Descriptor and sequence
            synchronized (mWriteLock) {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);

                for (int i = 0; i <= repeats; i++) {
                    if (write(timings)) {
                        Log.d(TAG, "IR signal sent success (" + (i+1) + ")");
                    }

                    if (i < repeats) {
                        SystemClock.sleep(60);
                    }
                }
                
                // Gap between different button events to prevent driver/receiver overlap
                SystemClock.sleep(60);
            }
        });
    }

    /**
     * Sends IR timings data (Internal, must be called within synchronized block).
     */
    private boolean write(int[] timings) {
        if (mFd < 0) return false;

        // Vortex LIRC driver limit is 67 integers (268 bytes)
        int[] finalTimings = timings;
        if (timings.length > 67) {
            Log.w(TAG, "IR signal too long (" + timings.length + "), truncating to 67");
            finalTimings = new int[67];
            System.arraycopy(timings, 0, finalTimings, 0, 67);
        }

        int[] roundedTimings = roundTo8Multiple(finalTimings);
        byte[] data = intArrayToBytes(roundedTimings);

        try {
            int ret = Driver.write(mFd, data);
            if (ret < 0) {
                Log.e(TAG, "IR write failed: " + ret + " (fd=" + mFd + ")");
                return false;
            }
            return true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Driver.write not implemented", e);
            return false;
        }
    }

    @Override
    public void close() {
        mExecutor.shutdown();
        if (mFd >= 0) {
            Driver.safeClose(mFd);
            mFd = -1;
        }
    }

    public boolean isOpen() {
        return mFd >= 0;
    }

    private static int[] roundTo8Multiple(int[] timings) {
        int[] result = new int[timings.length];
        for (int i = 0; i < timings.length; i++) {
            int value = timings[i];
            int remainder = value % 8;
            if (remainder == 0) {
                result[i] = value;
            } else if (remainder >= 4) {
                result[i] = value + (8 - remainder);
            } else {
                result[i] = value - remainder;
            }
        }
        return result;
    }

    private static byte[] intArrayToBytes(int[] timings) {
        ByteBuffer buffer = ByteBuffer
                .allocate(timings.length * 4)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (int t : timings) {
            buffer.putInt(t);
        }
        return buffer.array();
    }
}
