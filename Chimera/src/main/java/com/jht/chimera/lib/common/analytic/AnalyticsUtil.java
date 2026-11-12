package com.jht.chimera.lib.common.analytic;

import android.util.Log;

import com.google.gson.Gson;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.workoutapps.AppIcon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("HardCodedStringLiteral")
public class AnalyticsUtil {
    private static final String TAG = "AnalyticsUtil";
    public static final String STATS_FOLDER = "/sdcard/analytics/";
    public static final String STATS_FILE = STATS_FOLDER + "stats.json";
    public static final String WORKOUT_FOLDER = STATS_FOLDER + "workout";

    private AnalyticsUtil() {
        // prevent attempts to instantiate this utility class.
    }

    // FIXME - Unclosed Streams
    public static AnalyticStats loadStats() {
        try {
            File statsFile = new File(STATS_FILE);
            if (statsFile.exists()) {
                Gson gson = new Gson();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(statsFile)));
                AnalyticStats analyticStats = gson.fromJson(bufferedReader, AnalyticStats.class);
                if (analyticStats == null) analyticStats = new AnalyticStats();
                bufferedReader.close();
                return analyticStats;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        // an empty stats if loading failed
        return new AnalyticStats();
    }

    public static String getAppStringId(String windowInfo, Map<String, AppIcon> appList) {
        String appStrindId = windowInfo;

        // skip window info that contains our package
        if (!windowInfo.contains("com.jht")) {
            for (Map.Entry<String, AppIcon> entry : appList.entrySet()) {
                AppIcon appIcon = entry.getValue();

                // return the string id for 3rd party apps
                if (windowInfo.contains(appIcon.getAppPackage())) return appIcon.iconId;

                // return the string id for web apps
                String appIconUrl = appIcon.getUrl();
                if (appIconUrl != null && !appIconUrl.isEmpty() && windowInfo.contains(appIconUrl))
                    return appIcon.iconId;

                // return the string id for our run screen activity apps
                String appIconActivity = appIcon.getAppActivity();
                if (appIconActivity != null && !appIconActivity.isEmpty()) {
                    String simpleClassName = appIconActivity.substring(appIconActivity.lastIndexOf(".") + 1);
                    if (simpleClassName.equalsIgnoreCase(windowInfo)) return appIcon.iconId;
                }

            }
        }

        return appStrindId;
    }

    public static String formatTime(long timeSec) {
        return String.format(Locale.ENGLISH, "%01d:%02d.%02d %s",
                timeSec / 60 / 60,
                timeSec / 60 % 60,
                (int) ((timeSec % 60) / 60f * 100f),
                StringLookup.getEnglish(R.string.hours_lc));
    }

    public static String getDisplayLoginType(String loginType) {
        switch (loginType) {
            case DapiUtil.LOGIN_TYPE_XID:
                return "xID On-Screen Login";
            case DapiUtil.LOGIN_TYPE_RFID:
                return "RFID Login";
            case DapiUtil.LOGIN_TYPE_BARCODE:
                return "Barcode On-Screen Login";
            case DapiUtil.LOGIN_TYPE_EMAIL:
                return "Email On-Screen Login";
            case DapiUtil.LOGIN_TYPE_CUSTOM:
                return "Custom On-Screen Login";
            case DapiUtil.LOGIN_TYPE_PHONE:
                return "Phone On-Screen Login";
            case "guest":
                return "Guest Login";
            case "register_xid":
                return "xID On-Screen Registration";
        }

        return loginType;
    }
}
