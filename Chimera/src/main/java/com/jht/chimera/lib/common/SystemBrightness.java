package com.jht.chimera.lib.common;

import android.content.ContentResolver;
import android.provider.Settings;

import com.jht.androidcommonalgorithms.math.ValueRange;

public class SystemBrightness {

    private static final ValueRange userBrightnessSetting = new ValueRange(0, 100);
    private static final ValueRange systemBrightnessSetting = new ValueRange(80, 255);

    public static void setSystemBrightness(ContentResolver contentResolver, int userBrightness) {
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, (int)userBrightnessSetting.interpolate(userBrightness, systemBrightnessSetting));
    }

    public static int getUserBrightness(ContentResolver contentResolver) {
        try {
            return (int)systemBrightnessSetting.interpolate(Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS), userBrightnessSetting);
        } catch (Settings.SettingNotFoundException ignored) {
        }
        return 100;
    }

    public static void setSystemBrightnessToZero(ContentResolver contentResolver) {
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0);
    }

    public static int getUserBrightness(int systemBrightness) {
        return (int)systemBrightnessSetting.interpolate(systemBrightness, userBrightnessSetting);
    }

    public static int getSystemBrightness(int userBrightness) {
        return (int)userBrightnessSetting.interpolate(userBrightness, systemBrightnessSetting);
    }
}
