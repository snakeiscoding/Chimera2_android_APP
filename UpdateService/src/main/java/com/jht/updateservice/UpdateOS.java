package com.jht.updateservice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.UpdateEngineCallback;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;

public class UpdateOS {

    final static String TAG = "UpdateOS";
    Context mContext;

    public UpdateOS(Context context) {
        mContext = context;
    }

    @SuppressLint("InvalidWakeLockTag")
    public boolean update(UpdateEngineCallback callback, File otaFile, File propertiesFile) {

        String[] properties = readPropertiesFile(propertiesFile);
        for (int i = 0; i < properties.length; i++) {
            Log.d(TAG, "Received ota request property " + properties[i]);
        }

        try {
            //UpdateEngine updateEngine = new UpdateEngine();
            Object updateEngine = Class.forName("android.os.UpdateEngine").getConstructor().newInstance();
            //PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            //PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "OTA Wakelock");

            //updateEngine.bind(callback);
            Method bind = updateEngine.getClass().getDeclaredMethod("bind", UpdateEngineCallback.class, Handler.class);
            if (callback != null) {
                bind.invoke(updateEngine, callback, null);
            }

            //wakeLock.acquire();
            Log.d(TAG, "Should be calling updateEngine file://" + otaFile.getAbsolutePath());
            //updateEngine.applyPayload("file://" + otaFile.getAbsolutePath(), 0l, 0l, properties);
            Method applyPayload = updateEngine.getClass().getDeclaredMethod("applyPayload", String.class, long.class, long.class, String[].class);
            applyPayload.invoke(updateEngine, "file://" + otaFile.getAbsolutePath(), 0l, 0l, properties);
            //wakeLock.release();

            return true;
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        return false;

    }

    private String[] readPropertiesFile(File properties) {
        StringBuilder propertiesRead = new StringBuilder();
        try (FileInputStream propertiesStream = new FileInputStream(properties)) {
            int bytes;
            byte[] buffer = new byte[4096];
            while ((bytes = propertiesStream.read(buffer)) > 0) {
                propertiesRead.append(new String(buffer, 0, bytes, "UTF-8"));
            }
            return propertiesRead.toString().split("\\r?\\n");
        } catch (Exception ex) {
            Log.d(TAG, "exception", ex);
        }
        return new String[0];
    }

}
