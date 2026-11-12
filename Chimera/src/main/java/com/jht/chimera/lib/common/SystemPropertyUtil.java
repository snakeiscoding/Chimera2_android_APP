package com.jht.chimera.lib.common;

import android.annotation.SuppressLint;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.lang.reflect.Method;

// Might need newer gradle to use android.os.SystemProperties directly?

public class SystemPropertyUtil {
    @NonNls
    private static final String TAG = "SystemPropertyUtil";

    @SuppressLint({"PrivateApi", "DiscouragedPrivateApi"})
    public static void set(@NonNls String key, @NonNls String val) {
        try {
            Class<?> classType = Class.forName("android.os.SystemProperties");
            Method method = classType.getDeclaredMethod("set", String.class, String.class);
            method.invoke(classType, key, val);
        } catch (Exception e) {
            Log.e(TAG, "failed to set key=" + key, e);
        }
    }

    @SuppressLint({"PrivateApi", "DiscouragedPrivateApi"})
    public static String get(@NonNls String key, @NonNls String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> classType = Class.forName("android.os.SystemProperties");
            Method method = classType.getDeclaredMethod("get", String.class, String.class);
            value = (String) method.invoke(classType, key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "failed to get key=" + key, e);
        }
        return value;
    }
    public static String get(@NonNls String key) {
        return get(key, "");
    }

}
