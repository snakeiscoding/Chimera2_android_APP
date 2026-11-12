package com.jht.chimera.service.hal.lib.hardware;

import android.util.Log;
import android.util.SparseArray;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameKeyMaps;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.lib.common.process.KillProcessHelper;
import com.jht.chimera.lib.widget.sound.SystemVolume;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.liberror.ErrorCodeManager;
import com.jht.liberror.ErrorCodes;


public class KeypadManager {
    public final String TAG = "KeypadManager";
    private final int MAX_SEARCH_COUNT = 10;
    private KeyMapping mCurrentMapping;

    private JHTTimer keyHoldTimer = new JHTTimer(60*1000, () -> {
        // fixme: This should be simplified.
        ErrorCodeManager.handleErrorCode(ErrorCodes.UcbKeyPressTooLong.getCode());
    });

    public interface KeyPressListener {
        void onKeyPress(int physicalKey, int value);
    }

    public static KeypadManager getInstance() {
        return KeypadManager.SingletonHolder.INSTANCE;
    }
    private static class SingletonHolder {
        private static final KeypadManager INSTANCE = new KeypadManager();
    }

    private KeypadManager() {
        new VisionKeyHandle();
    }

    private KeyPressListener mKeyPressListener;
    private Thread mThread;
    private int mFd = -1;

    public boolean start(KeyPressListener keyPressListener, boolean isKeyFromIO) {
        mKeyPressListener = keyPressListener;

        if (isKeyFromIO) {
            mCurrentMapping = new KeyMapping_io();
            IOCommManager.getInstance().registerKeyPressListener(this::processData);
            return true;
        }

        mFd = findEventSource();
        if (mFd < 0) {
            Log.e(TAG, "cannot find driver");
            return false;
        }

        if (mThread == null) {
            mThread = new Thread(mRunnable, "KeypadManager");
            mThread.start();
        }
        return true;
    }

    public void close() {
        KillProcessHelper.stopThread(mThread);
        mThread = null;

        Driver.safeClose(mFd);
        mFd = -1;
    }


    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Driver.struct_input_event event = new Driver.struct_input_event();
            int ret = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ret = Driver.waitForRead(mFd, 200);
                    if (ret > 0) {
                        ret = Driver.readInputEvent(mFd, event);
                        if (ret > 0) {
                            processData(event);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reading event> ", e);
                }
            }
        }
    };

    void processData(Driver.struct_input_event event) {
        int key;
        Log.d(TAG, "key type = " + event.type + " code = " + event.code + " value = " + event.value);
        switch (event.type) {
            case Driver.struct_input_event.EV_KEY:
                if (mKeyPressListener != null && mCurrentMapping != null) {
                    key = mCurrentMapping.mapKey(event.code);
                    Log.d(TAG, "code= " + event.code + ", value= " + event.value + ", key= " + key);
                    mKeyPressListener.onKeyPress(key, event.value);

                    if (event.value == 0x00) {
                        keyHoldTimer.stop();
                    }
                    else
                        keyHoldTimer.start();
                }
                break;
        }
    }

    // returns fd of opened driver or -1 if not found
    int findEventSource() {
        for (int i = 0; i < MAX_SEARCH_COUNT; ++i) {
            int fd = -1;
            String deviceName = "/dev/input/event" + i; //NON-NLS
            String name;
            try {
                fd = Driver.safeOpen(deviceName);
                if (fd >= 0) {
                    name = Driver.readInputName(fd);
                    if (name != null) {
                        mCurrentMapping = KeyMapping.getMapping(name);
                        if (mCurrentMapping != null) {
                            Log.d(TAG, "Name for '" + deviceName + "' is '" + name + "'");
                            return fd;
                        }
                    }
                }
            }
            catch (Exception ex) { }
            Driver.safeClose(fd);
        }
        return -1;
    }

    // These are very common key arrangements, so lets provide presets that can be used in frame configs to reduce copy-and-paste.
    public static KeyPress.KeyCode getKeyCode(FrameKeyMaps keyMapEnum, int physicalKey) {
        KeyPress.KeyCode keyCode = KeyPress.KeyCode.KEY_UNKNOWN;
        SparseArray<KeyPress.KeyCode> keyMap = getKeyCodeMapping(keyMapEnum);
        if (keyMap != null) {
            keyCode = keyMap.get(physicalKey, KeyPress.KeyCode.KEY_UNKNOWN);
        }
        return keyCode;
    }
    public static SparseArray<KeyPress.KeyCode> getKeyCodeMapping(FrameKeyMaps keyMap) {
        // use a switch, or do a look up array and rely on enum ordinal?
        switch (keyMap) {
            case Treadmill_6key: return KeyPreset_Treadmill_6key;
            case ResistanceAndIncline: return KeyPreset_ResistanceAndIncline;
            case ResistanceOnly: return KeyPreset_ResistanceOnly;
            case ResistancePauseStop: return KeyPreset_ResistancePauseStop;
            case VisionResistance: return KeyPreset_VisionResistance;
            case VisionSpeed: return KeyPreset_VisionSpeed;
            case Commercial: return KeyPreset_Commercial;
            case Commercial_Treadmill: return KeyPreset_Commercial_Treadmill;
            default: return null;
        }
    }


    private static final SparseArray<KeyPress.KeyCode> KeyPreset_Treadmill_6key = new SparseArray<KeyPress.KeyCode>() {{
        append(250, KeyPress.KeyCode.KEY_GO);  //PHYSICAL_GO_MAP = KeyCode.KEY_GO;
        append(253, KeyPress.KeyCode.KEY_STOP);  //PHYSICAL_STOP_MAP = KeyCode.KEY_STOP;
        append(249, KeyPress.KeyCode.KEY_SPEED_UP);  //PHYSICAL_STOP_UP_MAP = KeyCode.KEY_SPEED_UP;
        append(252, KeyPress.KeyCode.KEY_SPEED_DOWN);  //PHYSICAL_STOP_DOWN_MAP = KeyCode.KEY_SPEED_DOWN;
        append(193, KeyPress.KeyCode.KEY_INCLINE_UP);  //PHYSICAL_GO_UP_MAP = KeyCode.KEY_INCLINE_UP;
        append(194, KeyPress.KeyCode.KEY_INCLINE_DOWN);  //PHYSICAL_GO_DOWN_MAP = KeyCode.KEY_INCLINE_DOWN;
    }};
    private static final SparseArray<KeyPress.KeyCode> KeyPreset_ResistanceAndIncline = new SparseArray<KeyPress.KeyCode>() {{
        append(249, KeyPress.KeyCode.KEY_RESISTANCE_UP);  //PHYSICAL_STOP_UP_MAP = KeyCode.KEY_RESISTANCE_UP;
        append(252, KeyPress.KeyCode.KEY_RESISTANCE_DOWN);    //PHYSICAL_STOP_DOWN_MAP = KeyCode.KEY_RESISTANCE_DOWN;
        append(193, KeyPress.KeyCode.KEY_INCLINE_UP);  //PHYSICAL_GO_UP_MAP = KeyCode.KEY_INCLINE_UP;
        append(194, KeyPress.KeyCode.KEY_INCLINE_DOWN);  //PHYSICAL_GO_DOWN_MAP = KeyCode.KEY_INCLINE_DOWN;
    }};
    private static final SparseArray<KeyPress.KeyCode> KeyPreset_ResistanceOnly = new SparseArray<KeyPress.KeyCode>() {{
        append(249, KeyPress.KeyCode.KEY_RESISTANCE_UP);  //PHYSICAL_STOP_UP_MAP = KeyCode.KEY_RESISTANCE_UP;
        append(252, KeyPress.KeyCode.KEY_RESISTANCE_DOWN);    //PHYSICAL_STOP_DOWN_MAP = KeyCode.KEY_RESISTANCE_DOWN;
    }};
    private static final SparseArray<KeyPress.KeyCode> KeyPreset_ResistancePauseStop = new SparseArray<KeyPress.KeyCode>() {{
        append(249, KeyPress.KeyCode.KEY_RESISTANCE_UP);  //PHYSICAL_STOP_UP_MAP = KeyCode.KEY_RESISTANCE_UP;
        append(252, KeyPress.KeyCode.KEY_RESISTANCE_DOWN);    //PHYSICAL_STOP_DOWN_MAP = KeyCode.KEY_RESISTANCE_DOWN;
        append(193, KeyPress.KeyCode.KEY_PAUSE);  //PHYSICAL_GO_UP_MAP = KeyCode.KEY_PAUSE;
        append(194, KeyPress.KeyCode.KEY_STOP);  //PHYSICAL_GO_DOWN_MAP = KeyCode.KEY_STOP;
    }};

    // Do all vision equipment have same key function mapping? If not, we could add additional presets
    private static final SparseArray<KeyPress.KeyCode> KeyPreset_VisionResistance = new SparseArray<KeyPress.KeyCode>() {{
        append(193, KeyPress.KeyCode.KEY_HOME);
        append(249, KeyPress.KeyCode.KEY_VOLUME_UP);
        append(252, KeyPress.KeyCode.KEY_VOLUME_DOWN);
        append(253, KeyPress.KeyCode.KEY_RESISTANCE_DOWN);
        append(250, KeyPress.KeyCode.KEY_RESISTANCE_UP);
        append(188, KeyPress.KeyCode.KEY_RESISTANCE_UP);
        append(184, KeyPress.KeyCode.KEY_RESISTANCE_DOWN);
        append(191, KeyPress.KeyCode.KEY_INCLINE_UP);
        append(187, KeyPress.KeyCode.KEY_INCLINE_DOWN);

    }};

    private static final SparseArray<KeyPress.KeyCode> KeyPreset_VisionSpeed = new SparseArray<KeyPress.KeyCode>() {{
        append(190, KeyPress.KeyCode.KEY_GO);
        append(191, KeyPress.KeyCode.KEY_STOP);
        append(192, KeyPress.KeyCode.KEY_PAUSE);
        append(254, KeyPress.KeyCode.KEY_HOME);
        append(249, KeyPress.KeyCode.KEY_VOLUME_UP);
        append(189, KeyPress.KeyCode.KEY_VOLUME_DOWN);
        append(253, KeyPress.KeyCode.KEY_SPEED_UP);
        append(193, KeyPress.KeyCode.KEY_SPEED_DOWN);
        append(252, KeyPress.KeyCode.KEY_INCLINE_UP);
        append(250, KeyPress.KeyCode.KEY_INCLINE_DOWN);
    }};

    // Note unsure about the LCGUI change below.
    // Ascent, resistance up, down   elevation up, down and bike (endurance)
    private static final SparseArray<KeyPress.KeyCode> KeyPreset_Commercial = new SparseArray<KeyPress.KeyCode>() {{
        append(ProductConfig.platform.equals(ProductConfig.Platform.LCGUI) ? 192 : 190, KeyPress.KeyCode.KEY_PAUSE);
        append(ProductConfig.platform.equals(ProductConfig.Platform.LCGUI) ? 189 : 192, KeyPress.KeyCode.KEY_STOP);
        append(253, KeyPress.KeyCode.KEY_INCLINE_UP);
        append(250, KeyPress.KeyCode.KEY_INCLINE_DOWN);
        append(252, KeyPress.KeyCode.KEY_RESISTANCE_UP);
        append(249, KeyPress.KeyCode.KEY_RESISTANCE_DOWN);
    }};

    private static final SparseArray<KeyPress.KeyCode> KeyPreset_Commercial_Treadmill = new SparseArray<KeyPress.KeyCode>() {{
        append(189, KeyPress.KeyCode.KEY_GO);
        append(192, KeyPress.KeyCode.KEY_STOP);
        append(252, KeyPress.KeyCode.KEY_SPEED_UP);
        append(249, KeyPress.KeyCode.KEY_SPEED_DOWN);
        append(253, KeyPress.KeyCode.KEY_INCLINE_UP);
        append(250, KeyPress.KeyCode.KEY_INCLINE_DOWN);
    }};
}


// Base class and factory of KeyMapping
class KeyMapping {
    // names can be found running "cat /proc/bus/input/devices" in adb shell and looking for i2c input devices
    protected String[] DRIVER_NAMES;
    public KeyMapping(String... names) { DRIVER_NAMES = names; }

    public boolean supports(String name) {
        for (String n : DRIVER_NAMES) {
            if (name.equalsIgnoreCase(n)) return true;
        }
        return false;
    }

    // This is the one you will most likely override in derived classes
    public int mapKey(int code) {
        return -1;
    }

    public static KeyMapping getMapping(String name) {
        final KeyMapping[] mMappings = {
                new KeyMapping_sn7326(),
                new KeyMapping_tca8418(),
        };
        for (KeyMapping n : mMappings) {
            if (n.supports(name)) return n;
        }
        return null;
    }
}

// Original i2c keypad interface driver
class KeyMapping_sn7326 extends KeyMapping {
    public KeyMapping_sn7326() { super("sn7326"); } //NON-NLS
    @Override
    public int mapKey(int code) {
        // for now just map to the tca key codes going forward
        switch (code) {
            case 249: return 250; //PhysicalKey.Go;
            case 250: return 253; //PhysicalKey.Stop;
            case 251: return 249; //PhysicalKey.StopUp;
            case 252: return 252; //PhysicalKey.StopDown;
            case 253: return 193; //PhysicalKey.GoUp;
            case 254: return 194; //PhysicalKey.GoDown;
        }
        return -1;
    }
}

// Newer Parker i2c keypad interface driver
class KeyMapping_tca8418 extends KeyMapping {
    public KeyMapping_tca8418() {super("tca8418");} //NON-NLS

    @Override
    public int mapKey(int code) {
        // for now the physical key number will be dictated by the tca numbering until we figure out something more generic but meaningful
        switch (code) {
            // catch pre OS 1.0.0.10 key codes for parker
            case 34: return 250; //PhysicalKey.Go;
            case 38: return 193; //PhysicalKey.GoUp;
            case 37: return 194; //PhysicalKey.GoDown;
            case 48: return 253; //PhysicalKey.Stop;
            case 35: return 249; //PhysicalKey.StopUp;
            case 46: return 252; //PhysicalKey.StopDown;
            // catch pre OS 1.0.0.10 key codes for vision
            // ?
        }
        return code;
    }
}
// Parker 3 keypad interface from io
class KeyMapping_io extends KeyMapping {
    public KeyMapping_io() {
        super("io");
    } //NON-NLS

    @Override
    public int mapKey(int code) {
        // for now the physical key number will be dictated by the tca numbering until we figure out something more generic but meaningful
        switch (code) {
            // catch pre OS 1.0.0.10 key codes for parker
            case 62: return 250; //PhysicalKey.Go;
            case 52: return 193; //PhysicalKey.GoUp;
            case 51: return 194; //PhysicalKey.GoDown;
            case 72: return 253; //PhysicalKey.Stop;
            case 63: return 249; //PhysicalKey.StopUp;
            case 73: return 252; //PhysicalKey.StopDown;
            // catch pre OS 1.0.0.10 key codes for vision
            // ?
        }
        return code;
    }
}

class VisionKeyHandle{
    private static final String TAG = "VisionKeyHandle";
    DataMatrix dataMatrix = new DataMatrix();

    public VisionKeyHandle(){
        dataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, value -> {
            KeyPress.KeyCode keyCode = KeyPress.getKeyCode(value);
            KeyPress.KeyState keyState = KeyPress.getKeyState(value);
            Log.d(TAG, "VisionKeyHandle: physical key  " + keyCode);

            if (keyCode == KeyPress.KeyCode.KEY_VOLUME_UP && keyState == KeyPress.KeyState.KEY_DOWN)
                SystemVolume.getInstance().increaseVolume();

            if (keyCode == KeyPress.KeyCode.KEY_VOLUME_DOWN && keyState == KeyPress.KeyState.KEY_DOWN)
                SystemVolume.getInstance().decreaseVolume();

            if (keyCode == KeyPress.KeyCode.KEY_HOME && keyState == KeyPress.KeyState.KEY_DOWN){
                Log.d(TAG, "VisionKeyHandle: run state1: " + dataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
                if( dataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP) {
                    Log.d(TAG, "VisionKeyHandle: run state2: " + dataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
                    if (DataMatrix.getBoolean(UiServiceKeys.DATA_HOME_BUTTON_ENABLED_BOOL)) {
                        DataMatrix.putBoolean(UiServiceKeys.DATA_HOME_BUTTON_ENABLED_BOOL, false);
                        LaunchAppUtil.launchWorkoutScreen();
                    }
                }
                else
                    if(dataMatrix.getBoolean(HALKeys.DATA_HOMEKEY_ENABLE_BOOL)) {
                        LaunchAppUtil.launchSplashScreen();
                        dataMatrix.putBoolean(HALKeys.DATA_HOMEKEY_ENABLE_BOOL,false);
                    }
            }
        });

        dataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, workoutState -> {
            if(workoutState == WorkoutKeys.Constants.RUN_STATE_STOP){
                dataMatrix.putBoolean(HALKeys.DATA_HOMEKEY_ENABLE_BOOL,false);
            }
        });
    }
}
