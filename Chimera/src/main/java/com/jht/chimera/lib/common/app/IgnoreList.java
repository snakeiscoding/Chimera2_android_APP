package com.jht.chimera.lib.common.app;

import org.jetbrains.annotations.NonNls;

public class IgnoreList {
    static String separator = "|";
    @NonNls public static String sIgnoreIntentList = "" +
            "android.settings." + separator +
            "android.intent.action.CHOOSER" + separator +
            "android.intent.action.SEND" + separator +
            "android.intent.action.EDIT" + separator +
            "android.intent.action.INSERT_OR_EDIT" + separator +
            "android.intent.action.PICK" + separator +
            "android.intent.action.DIAL" + separator +
            "android.intent.action.GET_CONTENT" + separator +
            "android.intent.action.CALL" + separator +
            "android.intent.action.INSERT" + separator +
            "android.intent.action.SEARCH" + separator +
            "android.intent.action.SYSTEM_TUTORIAL" + separator +
            "android.intent.action.WEB_SEARCH" + separator +
            "android.intent.action.ALL_APPS" + separator +
            "android.intent.action.SET_WALLPAPER" + separator +
            "android.intent.action.BUG_REPORT" + separator +
            "android.intent.action.VOICE_COMMAND" + separator +
            "android.intent.action.POWER_USAGE_SUMMARY" + separator +
            "android.intent.action.UPGRADE_SETUP" + separator +
            "android.bluetooth.device.action.PAIRING_REQUEST" + separator +
            "android.intent.action.MANAGE_NETWORK_USAGE" /*+ separator  +
            "android.intent.action.VIEW:https"*/;


    @NonNls public static String sIgnoreActivityList = "" +
            "com.google.android.apps.inputmethod.pinyin.preference.SettingsActivity" + separator +
            "com.android.inputmethod.latin.settings.SettingsActivity" /*+ separator +
            "com.google.android.gms.ads.AdActivity"*/;
}
