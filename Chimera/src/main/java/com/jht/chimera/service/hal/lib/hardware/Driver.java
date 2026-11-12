package com.jht.chimera.service.hal.lib.hardware;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Keep;

import com.google.gson.Gson;
import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.androidcommonalgorithms.util.SystemCommands;
import com.jht.androidcommonalgorithms.util.SystemDetection;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.SystemPropertyUtil;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.context.ContextUtil;

import java.io.File;
import java.lang.reflect.Method;

import kotlin.jvm.JvmStatic;

public class Driver {
    protected static final String TAG = "Driver";

    // This will provide hardware abstracted access to common GPIO items
    // Should we expose this directly or create specific methods on Driver that in turn call on this?
    public static final DriverGpio GpioWrapper = DriverGpio.createInstance();

    static {
        try
        {
            Log.i(TAG, "Trying to load driver.so");
            System.loadLibrary("driver");
            Log.i(TAG, "Loaded driver.so");
        }
        catch (UnsatisfiedLinkError ule)
        {
            Log.e(TAG, "WARNING: Could not link driver.so> ", ule);
        }
        catch (Exception ex)
        {
            Log.e(TAG, "WARNING: Could not load driver.so> ", ex);
        }
    }

    public static int safeOpen(String devName) {
        try { return open(devName); }
        catch (UnsatisfiedLinkError ex) { return -1; } // static constructor will log the link error once already
        catch (Exception ex) {
            Log.w(TAG, "WARNING: Driver.open(" + devName + ")> ", ex);
            return -1;
        }
    }
    public static void safeClose(int fd) {
        try { if (fd >=0) close(fd); }
        catch (UnsatisfiedLinkError | Exception ignored) { }
    }

    public native static int open(String devName);

    public native static void close(int fd);

    public native static int write(int fd, byte[] data);

    public native static int read(int fd, byte[] data);

    public native static int readInt(int fd);

    public native static int ioctl(int fd, int cmd, int data);

    public native static int ioctlPtr(int fd, int cmd, int data);

    /**
     * Sets IR parameters (frequency, duty cycle, and mode) in a single call.
     * This avoids multiple JNI calls and keeps low-level ioctl details in native code.
     *
     * @param fd The file descriptor for the IR device (e.g., /dev/lirc0).
     * @param freq The IR carrier frequency in Hz (e.g., 38000).
     * @param duty The IR duty cycle (0-100).
     * @param mode The IR send mode (e.g., LIRC_MODE_PULSE).
     * @return 0 on success, or a negative error code.
     */
    public native static int setIrParameters(int fd, int freq, int duty, int mode);


    //public static native int switchNone();

    //public static native int switchExt();

    //public static native int switchBt();

    //public static native int switchUsbBt();

    //public static native int audioHDMI();

    //public static native int audioCsafe();

    //public static native int audioCardio();

    public static native int cSafePowerOpen();

    public static native int setGpioValue(int gpio, int value);

    public static native int getGpioValue(int gpio); // returns ERR = -1 if cannot read

    public static native int appleCoTest();

    public static native int readI2C(int fd, int i2c_adr, byte[] buf, int length);

    public static native int writeI2C(int fd, int i2c_adr, int sub_adr, byte[] buf, int length);

    public static native int waitForRead(int fd, int timeout_ms); // basically wraps a call to select or poll (return <0 error, >0 has something, =0 timed out)

    public static native String readInputName(int fd); // returns null if it can't figure it out

    public static native String readInputPhysicalLocation(int fd); // returns null if it can't figure it out

    public static native long readInputDeviceProperties(int fd); // return negative value if error

    // THESE ARE BIT POSITIONS, NOT MASKS !
    public static int INPUT_PROP_POINTER = 0x00;
    public static int INPUT_PROP_DIRECT = 0x01;
    public static boolean TEST_BIT(int pos, long mask) { return 0 != (mask & (1 << pos)); }

    public static native int readInputEvent(int fd, struct_input_event event);

    public static native String getFormat();
    public static native String getResolution();
    public static native int getSampleRate();
    public static native int getAudioChannel();

    public static native void initDeinterlacer(int method);


    // "com/jht/chimera/service/hal/lib/hardware/Driver$struct_input_event" // if this class is moved or renamed, update Driver.cpp !
    public static class struct_input_event {
        // event type constants
        public static final int EV_SYN = 0x00;
        public static final int EV_KEY = 0x01;

        //public struct_timeval time; // we don't use the time
        public int type;
        public int code;
        public int value;
    }

    // "com/jht/chimera/service/hal/lib/hardware/Driver$struct_timeval" // if this class is moved or renamed, update Driver.cpp !
    /*public static class struct_timeval {
        // depending on system these can be 32b or 64b values, java long should be able to store up to 64b
        public long tv_sec;
        public long tv_usec;
    }*/


    /*
    The johnson_gpio driver provides simple read and write of GPIO without setup via setGpioValue and getGpioValue above.
    However if you need more control such as polling, you may need to use file node functionality via export.
     */
    public static native int pollGpio(int gpio, int timeoutMs, int delayUs); // timeout millisecond, delay microsecond
    public enum GpioDirection { in, high, low }
    public enum GpioEdge { none, rising, falling, both }
    public static boolean exportGpio(int gpio) { return exportGpio(gpio, null, null); }
    public static boolean exportGpio(int gpio, GpioDirection direction) { return exportGpio(gpio, direction, null); }
    public static boolean exportGpio(int gpio, GpioDirection direction, GpioEdge edge) {
        // JVM may not have permission to configure GPIOs, so we attempt to do so from a root terminal
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("echo " + gpio + " > /sys/class/gpio/export");
        if (direction != null) executeAsRoot.addCommand("echo " + direction.name() + " > /sys/class/gpio/gpio" + gpio + "/direction");
        if (edge != null) executeAsRoot.addCommand("echo " + edge.name() + " > /sys/class/gpio/gpio" + gpio + "/edge");
        return executeAsRoot.execute();
    }
    public static boolean unexportGpio(int gpio) {
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("echo " + gpio + " > /sys/class/gpio/unexport");
        return executeAsRoot.execute();
    }
    public static boolean isExportedGpio(int gpio) {
        // use java or jni?
        File file = new File("/sys/class/gpio/gpio" + gpio);
        return file.exists();
    }

    /**
     * GPIO wrapper class base
     */
    public static class DriverGpio {
        /**
         * Instance factory
         * @return
         */
        public static DriverGpio createInstance() {
            // Use java side hardware check or do some kind of native checks of hardware?
            if (SystemDetection.isEmulator()) {
                return new EmuGpio();
            }
            if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker)) {
                return new ParkerGpio();
            }

            // TODO: Parker 3 requires its own GPIO class. Keeping Parker 3.0 logic inline with Parker until this task can be completed.
            if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
                return new Parker3Gpio();
            }

            // just use base so we have error returns instead of null ptr exceptions
            return new DriverGpio();
        }

        /**
         * This gpio calculation is fairly common, but derived classes can create their own
         * @param gpio
         * @param port
         * @return
         */
        protected static int GPIO(int gpio, int port) {
            return (gpio * 32 + port);
        }

        /**
         * Get Linux GPIO numbers for common functions, return -1 if not available
         * @return
         */
        public int getGpioLineInDetect() { return -1; }

        // Common hardware functions (return -1 or false if feature unavailable)
        public boolean powerLimitCharging(boolean doLimit) { return false; }
        public boolean powerHdmiBoard(boolean isOn) { return false; }
        public boolean speakerEnable(boolean isOn) { return false; }
        public boolean audioSwitchNone() { return false; }
        public boolean audioSwitchHDMI() { return false; }
        public boolean audioSwitchUSB() { return false; }
        public boolean audioSwitchWiFi() { return false; }
        public boolean audioSwitchEXTLINE() { return false; }
        public boolean audioSwitchVA() { return false; }
        public boolean audioSwitchCSAFE() { return false; }
        public boolean audioSwitchBtSink() { return false; }

        // detect attached screen (null if detection not available on hardware, zero if detection should be available but didn't work)
        public static final int DETECT_SCREEN_ISSUE = 0;
        public Integer getScreen() { return null; } // enum, string, or nullable int inches?
    }


    /****** HDMI native calls *****/
    public static native int openDeviceHDMI();  // returns camFd; camFd < 0 is an error
    public static native void closeDeviceHDMI();
    public static native int[] getFormatHDMI();

    public static native int getFrameRate();

    public static native int[] getFormatNative();

    public static native int isInterlaced();

    public static native int processFrame(
            byte[] src_y,
            byte[] src_u,
            byte[] src_v,
            byte[] dst_y,
            byte[] dst_u,
            byte[] dst_v,
            int src_stride_y,
            int src_stride_u,
            int src_stride_v,
            int width,
            int height
    );
} // END CLASS: Driver


// GPIO wrapper for emulator
class EmuGpio extends Driver.DriverGpio {
    private EmuGpioData data;

    public EmuGpio() {
        super();
        data = load();
        if (data == null) data = new EmuGpioData();
    }

    @Override
    public Integer getScreen() {
        if (data != null) return data.screen;
        return null;
    }

    @Keep
    private static class EmuGpioData {
        public Integer screen = null; // if we don't know, defer to the other console type determination methods
    }
    private EmuGpioData load() {
        File file = new File("/sdcard/gpio.json"); //NON-NLS
        if (!file.exists()) return null;
        try {
            String jsonString = FileUtils.readFileToString(file);
            Gson gson = new Gson();
            return gson.fromJson(jsonString, EmuGpioData.class);
        } catch (Exception e) {
            Log.d(Driver.TAG, "EmuGpio load file exception", e);
            return null;
        }
    }
}


// GPIO wrapper for parker hardware
class ParkerGpio extends Driver.DriverGpio {
    private final static int LINE_IN_SW_A = GPIO(RK3399.RK_GPIO1, RK3399.RK_PC6);
    private final static int LINE_IN_SW_B = GPIO(RK3399.RK_GPIO1, RK3399.RK_PC7);
    private final static int AUDIO_SW_A = GPIO(RK3399.RK_GPIO1, RK3399.RK_PA1);
    private final static int AUDIO_SW_B = GPIO(RK3399.RK_GPIO1, RK3399.RK_PA0);

    @Override
    public boolean audioSwitchNone() {
        //Driver.switchNone();
        if (1 != Driver.setGpioValue(LINE_IN_SW_A, 0)) {Log.e(Driver.TAG, "audioSwitchNone: Cannot set pin LINE_IN_SW_A");return false;}
        if (1 != Driver.setGpioValue(LINE_IN_SW_B, 0)) {Log.e(Driver.TAG, "audioSwitchNone: Cannot set pin LINE_IN_SW_B");return false;}
        return true;
    }
    @Override
    public boolean audioSwitchHDMI() {
        //Driver.switchExt();
        if (1 != Driver.setGpioValue(LINE_IN_SW_A, 0)) {Log.e(Driver.TAG, "audioSwitchHDMI: Cannot set pin LINE_IN_SW_A");return false;}
        if (1 != Driver.setGpioValue(LINE_IN_SW_B, 1)) {Log.e(Driver.TAG, "audioSwitchHDMI: Cannot set pin LINE_IN_SW_B");return false;}
        //Driver.audioHDMI();
        if (1 != Driver.setGpioValue(AUDIO_SW_A, 0)) {Log.e(Driver.TAG, "audioSwitchHDMI: Cannot set pin AUDIO_SW_A");return false;}
        if (1 != Driver.setGpioValue(AUDIO_SW_B, 1)) {Log.e(Driver.TAG, "audioSwitchHDMI: Cannot set pin AUDIO_SW_B");return false;}
        return true;
    }
    @Override
    public boolean audioSwitchCSAFE() {
        //Driver.switchExt();
        if (1 != Driver.setGpioValue(LINE_IN_SW_A, 0)) {Log.e(Driver.TAG, "audioSwitchCSAFE: Cannot set pin LINE_IN_SW_A");return false;}
        if (1 != Driver.setGpioValue(LINE_IN_SW_B, 1)) {Log.e(Driver.TAG, "audioSwitchCSAFE: Cannot set pin LINE_IN_SW_B");return false;}
        //Driver.audioCsafe();
        if (1 != Driver.setGpioValue(AUDIO_SW_A, 1)) {Log.e(Driver.TAG, "audioSwitchCSAFE: Cannot set pin AUDIO_SW_A");return false;}
        if (1 != Driver.setGpioValue(AUDIO_SW_B, 1)) {Log.e(Driver.TAG, "audioSwitchCSAFE: Cannot set pin AUDIO_SW_B");return false;}
        return true;
    }
    @Override
    public boolean audioSwitchBtSink() {
        //Driver.switchUsbBt();
        if (1 != Driver.setGpioValue(LINE_IN_SW_A, 1)) {Log.e(Driver.TAG, "audioSwitchBtSink: Cannot set pin LINE_IN_SW_A");return false;}
        if (1 != Driver.setGpioValue(LINE_IN_SW_B, 0)) {Log.e(Driver.TAG, "audioSwitchBtSink: Cannot set pin LINE_IN_SW_B");return false;}
        return true;
    }

    @Override
    public int getGpioLineInDetect() { return GPIO(RK3399.RK_GPIO3, RK3399.RK_PC0); } // pin AIN_DET

    @Override
    public boolean powerLimitCharging(boolean doLimit) {
        // https://jhtnaee.atlassian.net/projects/PRKS/issues/PRKS-40
        int pin_USB_MODE = GPIO(RK3399.RK_GPIO0, RK3399.RK_PA3);
        int pin_USB_MODE1 = GPIO(RK3399.RK_GPIO0, RK3399.RK_PB3);
        /* our gpio driver apparently does not need export?
        // Do we need to create a singular setup method for all of these classes or just have each function check for setup that they need?
        if (!Driver.isExportedGpio(pin_USB_MODE)) {
            if (!Driver.exportGpio(pin_USB_MODE, doLimit ? Driver.GpioDirection.low : Driver.GpioDirection.low)) {
                Log.e(Driver.TAG, "Cannot export pin USB_MODE");
                return false;
            }
        }
        if (!Driver.isExportedGpio(pin_USB_MODE1)) {
            if (!Driver.exportGpio(pin_USB_MODE1, doLimit ? Driver.GpioDirection.high : Driver.GpioDirection.low)) {
                Log.e(Driver.TAG, "Cannot export pin USB_MODE1");
                return false;
            }
        }*/
        // "if we have 14V in,we can use DCP auto mode for charging, otherwise we use SDP mode." However in testing only CDP and SDP provided data!
        // 00 = CDP 1.5A/2.1A, 01 = SDP 0.5A/0.5A, 10 = DCP-Auto 2.1A/2.1A, 00 = Discharge out is low (see PRKS-40 comments)
        // Should we walk it through 11 discharge mode or are the transitions fast enough that we won't cause an intermediate mode
        if (1 != Driver.setGpioValue(pin_USB_MODE, doLimit ? 0 : 0)) {Log.e(Driver.TAG, "powerLimitCharging: Cannot set pin USB_MODE");return false;}
        if (1 != Driver.setGpioValue(pin_USB_MODE1, doLimit ? 1 : 0)) {Log.e(Driver.TAG, "powerLimitCharging: Cannot set pin USB_MODE1");return false;}
        Log.d(Driver.TAG, "powerLimitCharging doLimit=" + doLimit);
        return true;
    }

    @Override
    public boolean powerHdmiBoard(boolean isOn) {
        int pin_HDMI_PWR_EN = GPIO(RK3399.RK_GPIO1, RK3399.RK_PA3);  // HDMI_PWR_EN on HDMI_IN schematic, or HDMI_RST on parker schematic
        /* our gpio driver apparently does not need export?
        if (!Driver.isExportedGpio(pin_HDMI_PWR_EN)) {
            if (!Driver.exportGpio(pin_HDMI_PWR_EN, isOn ? Driver.GpioDirection.high : Driver.GpioDirection.low)) {
                Log.e(Driver.TAG, "Cannot export pin HDMI_PWR_EN");
                return false;
            }
        }*/
        if (1 != Driver.setGpioValue(pin_HDMI_PWR_EN, isOn ? 1 : 0)) {Log.e(Driver.TAG, "powerHdmiBoard: Cannot set pin HDMI_PWR_EN");return false;}
        Log.d(Driver.TAG, "powerHdmiBoard isOn=" + isOn);
        return true;
    }

    @Override
    public boolean speakerEnable(boolean isOn) {
        int pin_PA_CTL = GPIO(RK3399.RK_GPIO4, RK3399.RK_PD0);
        if (1 != Driver.setGpioValue(pin_PA_CTL, isOn ? 1 : 0)) {Log.e(Driver.TAG, "speakerEnable: Cannot set pin PA_CTL");return false;}
        Log.d(Driver.TAG, "speakerEnable isOn=" + isOn);
        return true;
    }

    @Override
    public Integer getScreen() {
        // If parker OS is real old or OS is another variant of rk3399, fall back to old methods
        if (!"c49f4442-e31b-4efe-9b7a-c0d99e4e7c08".equals(Build.MODEL)) { //NON-NLS
            Log.w(Driver.TAG, "getScreen: PARKER OS is either old or a different variation");
            return null;
        }
        int pin_ID0 = GPIO(RK3399.RK_GPIO0, RK3399.RK_PB0);
        int pin_ID1 = GPIO(RK3399.RK_GPIO0, RK3399.RK_PB4);
        int ID0 = Driver.getGpioValue(pin_ID0);
        int ID1 = Driver.getGpioValue(pin_ID1);
        if (ID0 < 0) {Log.e(Driver.TAG, "getScreen: Cannot get pin ID0");return DETECT_SCREEN_ISSUE;}
        if (ID1 < 0) {Log.e(Driver.TAG, "getScreen: Cannot get pin ID1");return DETECT_SCREEN_ISSUE;}
        if (ID0 == 0 && ID1 == 0) {
            return 22;  // LVDS 1920x1080
        } else if (ID0 != 0 && ID1 == 0) {
            return 16; // EDP 1366x768 (later 1920x1080)
        } else if (ID0 == 0 && ID1 != 0) {
            return 10; // MIPI
        } else if (ID0 != 0 && ID1 != 0) {
            return 10; // LVDS 1024x600 (assuming some version of 10 that was on the LVDS interface?)
        }
        Log.e(Driver.TAG, "getScreen: Cannot determine screen from ID0=" + ID0 + " ID1=" + ID1);
        return DETECT_SCREEN_ISSUE;
    }

} // END CLASS: ParkerGpio

class Parker3Gpio extends Driver.DriverGpio {

    private static String TAG = "Parker3Gpio";
    private Context context = ContextUtil.getApplicationContext();
    @SuppressLint("WrongConstant")
    private Object audioSysLibService = context.getSystemService("jht_audiosyslib");

    enum AudioSourceConstants {
        DEFAULT(0),
        SOC_COMMON(1),
        SOC_VA(2),
        SOC_WIFI(3),
        BTSINK1(4),
        BTSINK2(5),
        EXT_LINE(6),
        REMOTE_TV(7),
        HDMI_RESERVED(8),
        HDMI_EXT(9),
        HDMI_TUNER_STB(10),
        CARDIO(11),
        HDMI_1PORT(12);


        private int mValue;
        private AudioSourceConstants(int value) {
            mValue = value;
        }
        public int value() {
            return mValue;
        }

    }

    @Override
    public boolean audioSwitchNone() {
        return switchAudioSource(AudioSourceConstants.SOC_COMMON);
    }

    @Override
    public boolean audioSwitchEXTLINE() {
        return switchAudioSource(AudioSourceConstants.EXT_LINE);
    }

    @Override
    public boolean audioSwitchVA() {
        return switchAudioSource(AudioSourceConstants.SOC_VA);
    }

    @Override
    public boolean audioSwitchWiFi() {
        return switchAudioSource(AudioSourceConstants.SOC_WIFI);
    }

    @Override
    public boolean audioSwitchUSB() {
        return switchAudioSource(AudioSourceConstants.SOC_COMMON);
    }

    @Override
    public boolean audioSwitchHDMI() {
        return switchAudioSource(AudioSourceConstants.HDMI_1PORT);
    }

    @Override
    public boolean audioSwitchCSAFE() {
        return switchAudioSource(AudioSourceConstants.REMOTE_TV);
    }

    @Override
    public boolean audioSwitchBtSink() {
        return switchAudioSource(AudioSourceConstants.BTSINK1);
    }

    @Override
    public int getGpioLineInDetect() { return GPIO(RK3399.RK_GPIO3, RK3399.RK_PA6); }

    @Override
    public boolean powerLimitCharging(boolean doLimit) {
        int pin_USB_MODE = GPIO (RK3399.RK_GPIO1, RK3399.RK_PC6);
        int pin_USB_MODE1 = GPIO(RK3399.RK_GPIO1, RK3399.RK_PC7);
        Log.d(Driver.TAG, "powerLimitCharging doLimit=" + doLimit);
        // "if we have 14V in,we can use DCP auto mode for charging, otherwise we use SDP mode." However in testing only CDP and SDP provided data!
        // 00 = CDP 1.5A/2.1A, 01 = SDP 0.5A/0.5A, 10 = DCP-Auto 2.1A/2.1A, 11 = Discharge out is low (see PRKS-40 comments)
        // Should we walk it through 11 discharge mode or are the transitions fast enough that we won't cause an intermediate mode
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("echo 0" + " > /sys/class/gpio/gpio" + pin_USB_MODE + "/value");
        executeAsRoot.addCommand("echo " + (doLimit ? "1" : "0") + " > /sys/class/gpio/gpio" + pin_USB_MODE1 + "/value");
        return executeAsRoot.execute();
    }

    @Override
    public boolean powerHdmiBoard(boolean isOn) {
        int pin_HDMI_PWR_EN = GPIO(RK3399.RK_GPIO1, RK3399.RK_PA1);  // HDMI_PWR_EN on HDMI_IN schematic, or HDMI_RST on parker schematic
        Log.d(Driver.TAG, "powerHdmiBoard isOn=" + isOn);
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("echo " + (isOn ? "1" : "0") + " > /sys/class/gpio/gpio" + pin_HDMI_PWR_EN + "/value");
        return executeAsRoot.execute();
    }

    @Override
    public boolean speakerEnable(boolean isOn) {
        int pin_PA_CTL = GPIO(RK3399.RK_GPIO4, RK3399.RK_PD5);
        Log.d(Driver.TAG, "powerHdmiBoard isOn=" + isOn);
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("echo " + (isOn ? "0" : "1") + " > /sys/class/gpio/gpio" + pin_PA_CTL + "/value");
        return executeAsRoot.execute();
    }

    @Override
    public Integer getScreen() {
        String screenString = SystemCommands.getSystemProperty("vendor.jht.display.size"); // should return 22, 16, or 10
        try {
            return Integer.parseInt(screenString);
        } catch (Exception e) {
            Log.e(Driver.TAG, "getScreen: cannot parse vendor.jht.display.size=" + screenString, e);
            return DETECT_SCREEN_ISSUE;
        }
    }

    private boolean switchAudioSource(AudioSourceConstants source) {
        Object result;
        if (audioSysLibService == null) return false;

        try {
            Method m = audioSysLibService.getClass().getDeclaredMethod("selectInputSource", int.class);
            m.setAccessible(true);
            result = m.invoke(audioSysLibService, source.ordinal());
        } catch (Exception e) {
            Log.w(TAG, e);
            return false;
        }
        Log.d(TAG, "switch " + source + " " + result);
        return (int) result == 0;
    }


}

// GPIO constants for rockchip 3399 which is used on multiple consoles
final class RK3399 {
    public static final int RK_GPIO0 = 0;
    public static final int RK_GPIO1 = 1;
    public static final int RK_GPIO2 = 2;
    public static final int RK_GPIO3 = 3;
    public static final int RK_GPIO4 = 4;
    public static final int RK_GPIO6 = 6;

    public static final int RK_PA0 = 0;
    public static final int RK_PA1 = 1;
    public static final int RK_PA2 = 2;
    public static final int RK_PA3 = 3;
    public static final int RK_PA4 = 4;
    public static final int RK_PA5 = 5;
    public static final int RK_PA6 = 6;
    public static final int RK_PA7 = 7;
    public static final int RK_PB0 = 8;
    public static final int RK_PB1 = 9;
    public static final int RK_PB2 = 10;
    public static final int RK_PB3 = 11;
    public static final int RK_PB4 = 12;
    public static final int RK_PB5 = 13;
    public static final int RK_PB6 = 14;
    public static final int RK_PB7 = 15;
    public static final int RK_PC0 = 16;
    public static final int RK_PC1 = 17;
    public static final int RK_PC2 = 18;
    public static final int RK_PC3 = 19;
    public static final int RK_PC4 = 20;
    public static final int RK_PC5 = 21;
    public static final int RK_PC6 = 22;
    public static final int RK_PC7 = 23;
    public static final int RK_PD0 = 24;
    public static final int RK_PD1 = 25;
    public static final int RK_PD2 = 26;
    public static final int RK_PD3 = 27;
    public static final int RK_PD4 = 28;
    public static final int RK_PD5 = 29;
    public static final int RK_PD6 = 30;
    public static final int RK_PD7 = 31;
}
