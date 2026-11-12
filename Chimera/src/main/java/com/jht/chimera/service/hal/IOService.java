package com.jht.chimera.service.hal;

import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.chimera.lib.common.SystemBrightness;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.service.hal.lib.hardware.IOCommManager;
import com.jht.chimera.service.hal.lib.hardware.KeypadManager;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.services.ChildConsoleService;
import com.jht.services.ConsoleServiceState;

public class IOService extends ChildConsoleService {
    private final String TAG = "IOService";
    private Handler mHandler = new Handler();
    private boolean isInSleepMode = false;
    private final ContentObserver brightnessObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            try {
                isInSleepMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) <= 0;
            } catch (Settings.SettingNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // only start some hardware manager if this is the chimera or parker board
        if (ProductConfig.hasIO()) {
            Log.i(TAG, "Starting IOCommManager");
            IOCommManager.getInstance().start();

            // This was specifically added for Parker 3, the IO is a bit weird - James, 7/17
            if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
                KeypadManager.getInstance().start((physicalKey, value) -> {
                    if (isInSleepMode) {
                        LaunchAppUtil.launchSplashScreen();
                        // Delay to prevent see the screen before splash screen
                        mHandler.postDelayed(() -> {
                            SystemBrightness.setSystemBrightness(getContentResolver(),
                                DataMatrix.getInt(SettingKeys.Hardware.DATA_BACKLIGHT_PROGRESS_INT));
                            DataMatrix.putBoolean(UiServiceKeys.ACTION_SHOW_OVERLAY_PANEL_BOOL,
                                    false);
                        }, 300);
                    } else {
                        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
                        KeyPress.KeyCode keyCode = KeypadManager.getKeyCode(frameConfig.getKeyMap(),
                                physicalKey);
                        KeyPress.KeyState keyState = (value == 0 ? KeyPress.KeyState.KEY_UP
                                : KeyPress.KeyState.KEY_DOWN);
                        int bitmapKey = KeyPress.createBitmapKey(keyCode, keyState);
                        DataMatrix.putInt(HALKeys.EVENT_KEYPRESS_BITMAP_INT, bitmapKey);
                    }
                }, true);
                getContentResolver().registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SCREEN_BRIGHTNESS), false, brightnessObserver);
            }
        } else {
            Log.i(TAG, "Skipping IOCommManager");
        }
        updateMyServiceState(ConsoleServiceState.READY);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        IOCommManager.getInstance().stop();
    }

}
