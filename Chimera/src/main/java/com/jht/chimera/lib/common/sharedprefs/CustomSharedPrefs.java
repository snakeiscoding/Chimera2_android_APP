package com.jht.chimera.lib.common.sharedprefs;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.lang.reflect.Constructor;

public class CustomSharedPrefs {
    private static final String TAG = "CustomSharedPrefs";
    private CustomSharedPrefs() {
        // prevent instantiation
    }

    @Nullable
    public static SharedPreferences getSharedPreferences(File file) {
        try {
            Class<?> classSharedPreferencesImpl = Class.forName("android.app.SharedPreferencesImpl");
            Constructor<?> constructorSharedPreferencesImpl = classSharedPreferencesImpl.getDeclaredConstructor(File.class, int.class);
            constructorSharedPreferencesImpl.setAccessible(true);
            SharedPreferences sharedPreferences = (SharedPreferences) constructorSharedPreferencesImpl.newInstance(file, 0);
            return sharedPreferences;
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        return null;
    }
}
