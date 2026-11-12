package com.jht.chimera.service.hal.lib.salutron;

import android.util.Log;

import com.jht.androidcommonalgorithms.resources.ResourceHelper;

import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class ManifestFile {
    @NonNls private static final String TAG = "ManifestFile";
    private JSONObject manifest;
    private File manifestFile;

    public ManifestFile(File file) throws IOException, JSONException {
        manifestFile = file;
        manifest = readManifest(file);
    }

    private JSONObject readManifest(File file) throws IOException, JSONException {
        return ResourceHelper.loadJSONFromFile(file).getJSONObject("manifest");
    }

    public String getString(String... keys) {
        try {
            JSONObject obj = getParent(keys);
            return obj.getString(keys[keys.length - 1]);
        } catch (JSONException var3) {
            Log.w(TAG, var3);
            return "";
        }
    }

    public int getInt(String... keys) {
        try {
            JSONObject obj = getParent(keys);
            return obj.getInt(keys[keys.length - 1]);
        } catch (JSONException var3) {
            Log.w(TAG, var3);
            return 0;
        }
    }

    public long getLong(String... keys) {
        try {
            JSONObject obj = getParent(keys);
            return obj.getLong(keys[keys.length - 1]);
        } catch (JSONException var3) {
            Log.w(TAG, var3);
            return 0L;
        }
    }

    public double getDouble(String... keys) {
        try {
            JSONObject obj = getParent(keys);
            return obj.getDouble(keys[keys.length - 1]);
        } catch (JSONException var3) {
            Log.w(TAG, var3);
            return 0.0D;
        }
    }

    public int[] getIntArray(String... keys) {
        try {
            JSONObject obj = getParent(keys);
            JSONArray array = obj.getJSONArray(keys[keys.length - 1]);
            int[] list = new int[array.length()];

            for(int i = 0; i < array.length(); ++i) {
                list[i] = array.getInt(i);
            }

            return list;
        } catch (JSONException var6) {
            Log.w(TAG, var6);
            return new int[0];
        }
    }

    public long[] getLongArray(String... keys) {
        try {
            JSONObject obj = getParent(keys);
            JSONArray array = obj.getJSONArray(keys[keys.length - 1]);
            long[] list = new long[array.length()];

            for(int i = 0; i < array.length(); ++i) {
                list[i] = array.getLong(i);
            }

            return list;
        } catch (JSONException var6) {
            Log.w(TAG, var6);
            return new long[0];
        }
    }

    public String[] getStringArray(String... keys) {
        try {
            JSONObject obj = getParent(keys);
            JSONArray array = obj.getJSONArray(keys[keys.length - 1]);
            String[] list = new String[array.length()];

            for(int i = 0; i < array.length(); ++i) {
                list[i] = array.getString(i);
            }

            return list;
        } catch (JSONException var6) {
            Log.w(TAG, var6);
            return new String[0];
        }
    }

    private JSONObject getParent(String... keys) throws JSONException {
        JSONObject obj = manifest;

        for(int i = 0; i < keys.length - 1; ++i) {
            obj = obj.getJSONObject(keys[i]);
        }

        return obj;
    }

    public File getFile() {
        return manifestFile;
    }

    public File getDirectory() {
        return manifestFile.getParentFile();
    }
}
