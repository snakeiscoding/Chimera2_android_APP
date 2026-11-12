package com.jht.chimera.service.logger;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.context.ContextUtil;

public class AccessibilityServiceList {
    public static final String TAG = "AccessibilityService...";
    public static final String SECURE_SETTING_LIST = "com.jht.chimera/com.jht.chimera.service.logger.LoggerService"; //NON-NLS

    public static void enableAccessibilityServices() {

        // try to enable the accessibility settings for the service if it was not enabled
        try {
            Context context = ContextUtil.getApplicationContext();

            String accessibilityList = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String accessibilityEnabled = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);

            if (accessibilityList == null
                    ||
                    accessibilityEnabled == null
                    ||
                    accessibilityList.contains(AccessibilityServiceList.SECURE_SETTING_LIST) == false
                    ||
                    accessibilityEnabled.equals("1") == false) {

                AndroidSetting.putString(context.getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, AccessibilityServiceList.SECURE_SETTING_LIST);
                AndroidSetting.putString(context.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_ENABLED, "1");
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }
}
