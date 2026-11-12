package com.jht.chimera.service.hal.lib.hardware;

import android.os.SystemClock;
import android.util.Log;

import com.jht.androidcommonalgorithms.util.SystemDetection;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.ProductConfig;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TouchEventManager {

    public final String TAG = "TouchEventManager";
    private final int MAX_SEARCH_COUNT = 10;

    public interface TouchListener {
        void onTouch();
    }

    public static TouchEventManager getInstance() {
        return TouchEventManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final TouchEventManager INSTANCE = new TouchEventManager();
    }

    private TouchEventManager() {
    }

    private String mDeviceName;
    private TouchListener mTouchListener;
    private FileInputStream mFileInputStream;
    private final byte[] mBuffer = new byte[256];
    private Thread mThread;

    public void start(TouchListener touchListener) {
        close(); // protect re-entrance

        mTouchListener = touchListener;

        if (mThread == null) {
            mThread = new Thread(mRunnable);
            mThread.setName("TouchEventManager");
            mThread.start();
        }
    }

    public void close() {
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }

        if (mFileInputStream != null) {
            try {
                mFileInputStream.close();
            } catch (IOException e) {
                Log.w(TAG, e);
            }
            mFileInputStream = null;
        }
    }


    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {


            // tries for one minute in case the input event device is not loaded yet
            int maxTries = 60;
            int numTries = 0;
            do {
                // make sure we have permission (seems to be more of an emulator issue than console)
                ExecuteAsRoot.run("chmod 777 /dev/input/event*");

                mDeviceName = findEventSource();
                File file = new File(mDeviceName);

                numTries++;
                if (file.exists()) {

                    // make sure we have permission (CGC-14310 noted occasional situation where first permission set was not effective)
                    ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
                    executeAsRoot.addCommand("chmod 777 " + mDeviceName);
                    executeAsRoot.execute();

                    try {
                        mFileInputStream = new FileInputStream(mDeviceName);
                    } catch (FileNotFoundException e) {
                        Log.w(TAG, e);
                    }

                    Log.d(TAG, "TouchEventManager successful input stream for " + mDeviceName);
                    break;
                }

                SystemClock.sleep(1000);
            } while (numTries <= maxTries);

            if (mFileInputStream == null) {
                Log.e(TAG, "Could not open input stream for " + mDeviceName);
                return;
            }
            Log.e(TAG, "monitor started successfully");
            int bytesRead = 0;
            long lastTime = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    bytesRead = mFileInputStream.read(mBuffer, 0, mBuffer.length);
                    if (bytesRead > 0) {
                        long currentTime = SystemClock.uptimeMillis();
                        if (lastTime + 1000 < currentTime || lastTime > currentTime) {
                            lastTime = currentTime;
                            if (mTouchListener != null) {
                                // send event
                                mTouchListener.onTouch();
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading in touch device input!!! Stopping touch monitor", e);
                    return;
                }
            }


        }
    };


    // returns the device input for the touch or mouse input
    String findEventSource() {
        @NonNls String devName = null;
        if (SystemDetection.isEmulator()) {
            devName = findEventSourceByName(emulatorInputNames);
            if (devName == null) devName = "/dev/input/event1"; // NON-NLS
        } else if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera) || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2)) {
            devName = "/dev/input/event1"; // NON-NLS
        } else { // parker, UltraThin, vortex, etc
            devName = findEventSourceByName(regularInputNames);
            if (devName == null) devName = findEventSourceByProp(); // fall back if name doesn't have keyword
        }

        return (devName != null ? devName : "/dev/input/not_found"); //NON-NLS
    }

    final private String[] regularInputNames = {"touch", "mouse"}; // we don't have touch screens for all console so adding a way to detect the mouse
    final private String[] emulatorInputNames = {"touch", "qwerty2"}; // so far most emulators have used this name for its fake touch, but we could add more

    String findEventSourceByName(String[] names) {
        // path may vary, so look for input with relevant name
        for (int i = 0; i < MAX_SEARCH_COUNT; ++i) {
            int fd = -1;
            String devicePath = "/dev/input/event" + i; // NON-NLS
            String readName;
            try {
                fd = Driver.safeOpen(devicePath);
                if (fd >= 0) {
                    readName = Driver.readInputName(fd);
                    if (readName != null) {
                        for (String name : names) {
                            if (readName.toLowerCase().contains(name)) {
                                Driver.safeClose(fd);
                                return devicePath;
                            }
                        }
                    }
                }
            }
            catch (Exception ignored) { }
            Driver.safeClose(fd);
        }
        return null;
    }

    String findEventSourceByProp() {
        // input sources of INPUT_PROP_DIRECT are likely to be touchscreens
        for (int i = 0; i < MAX_SEARCH_COUNT; ++i) {
            int fd = -1;
            String devicePath = "/dev/input/event" + i; // NON-NLS
            long prop;
            try {
                fd = Driver.safeOpen(devicePath);
                if (fd >= 0) {
                    prop = Driver.readInputDeviceProperties(fd);
                    if (prop >= 0) {
                        if (Driver.TEST_BIT(Driver.INPUT_PROP_DIRECT, prop)) return devicePath;
                    }
                }
            }
            catch (Exception ignored) { }
            Driver.safeClose(fd);
        }
        return null;
    }

    /*String findEventSourceByPhy() {
        // so far the touchscreen or mouse reports physically on usb
        for (int i = 0; i < MAX_SEARCH_COUNT; ++i) {
            int fd = -1;
            String deviceName = "/dev/input/event" + i;
            String location;
            try {
                fd = Driver.safeOpen(deviceName);
                if (fd >= 0) {
                    location = Driver.readInputPhysicalLocation(fd);
                    if (location != null) {
                        if (location.toLowerCase().contains("usb")) {
                            return deviceName;
                        }
                    }
                }
            }
            catch (Exception ex) { }
            Driver.safeClose(fd);
        }
        return null;
    }*/

}
