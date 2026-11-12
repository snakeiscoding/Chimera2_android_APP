package com.jht.chimera.lib.common.app;

import android.content.ContentResolver;
import android.provider.Settings;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

public class AndroidSetting {
    @NonNls private static final String TAG = "AndroidSetting";

    private AndroidSetting() {
        // prevent instantiation of utility class.
    }

    public static void putInt(ContentResolver contentResolver, String name, int value) {
        try {
            Settings.Secure.putInt(contentResolver, name, value);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static void putLong(ContentResolver contentResolver, String name, long value) {
        try {
            Settings.Secure.putLong(contentResolver, name, value);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static void putString(ContentResolver contentResolver, String name, String value) {
        try {
            Settings.Secure.putString(contentResolver, name, value);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static String getString(ContentResolver contentResolver, String name) {
        try {
            return Settings.Secure.getString(contentResolver, name);
        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        }
    }

    public static long getLong(ContentResolver contentResolver, String name, long defaultValue) {
        try {
            return Settings.Secure.getLong(contentResolver, name);
        } catch (Exception e) {
            Log.w(TAG, e);
            return defaultValue;
        }
    }
}