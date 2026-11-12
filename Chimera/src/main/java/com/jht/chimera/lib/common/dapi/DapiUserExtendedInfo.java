package com.jht.chimera.lib.common.dapi;

import android.util.Log;

import androidx.annotation.Keep;

import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Keep
public class DapiUserExtendedInfo {
    @NonNls private static final String TAG = "DapiUserExtendedInfo";
    public String bluetoothHeadphones = "";
    public String bluetoothHeartRate = "";
    public String bluetoothMediaPlayer = "";
    public Map<String, String> customWorkouts;
    public String progressionData = "";

    public DapiUserExtendedInfo() { customWorkouts = new HashMap<>(); }
    public DapiUserExtendedInfo(DapiUserExtendedInfo other) {
        bluetoothHeadphones = other.bluetoothHeadphones;
        bluetoothHeartRate = other.bluetoothHeartRate;
        bluetoothMediaPlayer = other.bluetoothMediaPlayer;
        customWorkouts = new HashMap<>(other.customWorkouts);
        progressionData = other.progressionData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DapiUserExtendedInfo)) return false;
        DapiUserExtendedInfo other = (DapiUserExtendedInfo)o;
        if (!bluetoothHeadphones.equals(other.bluetoothHeadphones)) return false;
        if (!bluetoothHeartRate.equals(other.bluetoothHeartRate)) return false;
        if (!bluetoothMediaPlayer.equals(other.bluetoothMediaPlayer)) return false;
        if (!customWorkouts.equals(other.customWorkouts)) return false;
        if (!progressionData.equals(other.progressionData)) return false;
        return true;
    }
    @Override
    public int hashCode() {
        return Objects.hash(bluetoothHeadphones, bluetoothHeartRate, bluetoothMediaPlayer, customWorkouts);
    }

    public static DapiUserExtendedInfo parseJson(String text) {
        DapiUserExtendedInfo userExtendedInfo = new DapiUserExtendedInfo();
        if (text == null || text.isEmpty()) return userExtendedInfo;

        try {
            JSONObject object = new JSONObject(text);
            userExtendedInfo.bluetoothHeadphones = object.getString("bluetoothHeadphones");
            userExtendedInfo.bluetoothHeartRate = object.getString("bluetoothHeartRate");
            userExtendedInfo.bluetoothMediaPlayer = object.getString("bluetoothMediaPlayer");
            if (object.has("progressionData")) {
                userExtendedInfo.progressionData = object.getString("progressionData");
            }

            JSONArray array = object.getJSONArray("customWorkouts");
            for (int i = 0; i < array.length(); i++) {
                JSONObject arrayItemObject = array.getJSONObject(i);
                String key = arrayItemObject.getString("key");
                String value = arrayItemObject.getString("value");
                userExtendedInfo.customWorkouts.put(key, value);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        return userExtendedInfo;
    }

    public static String toJsonString(DapiUserExtendedInfo userExtendedInfo) {

        try {
            JSONObject object = new JSONObject();
            object.put("bluetoothHeadphones", userExtendedInfo.bluetoothHeadphones);
            object.put("bluetoothHeartRate", userExtendedInfo.bluetoothHeartRate);
            object.put("bluetoothMediaPlayer", userExtendedInfo.bluetoothMediaPlayer);
            object.put("progressionData", userExtendedInfo.progressionData);

            JSONArray array = new JSONArray();
            for (Map.Entry<String, String> entry : userExtendedInfo.customWorkouts.entrySet()) {
                JSONObject entryObject = new JSONObject();
                entryObject.put("key", entry.getKey());
                entryObject.put("value", entry.getValue());
                array.put(entryObject);
            }
            object.put("customWorkouts", array);


            return object.toString();
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        return "";
    }
}
