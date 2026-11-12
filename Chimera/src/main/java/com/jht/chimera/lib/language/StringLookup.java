package com.jht.chimera.lib.language;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import com.jht.chimera.lib.common.context.ContextUtil;

import java.util.Locale;

public class StringLookup {
    private static final String TAG = "StringLookup";
    static Context sContext = ContextUtil.getApplicationContext();

    public static String get(int stringId) {
        return getString(stringId, sContext);
    }

    public static String getEnglish(int stringId) {
        return getLocalizedString(sContext, Locale.ENGLISH, stringId);
    }

    public static String getString(int stringId, Context context) {
        try {
            return context.getResources().getString(stringId);
        } catch (Exception e) {
            Log.d(TAG, "getString(int,Context)", e);
        }
        // Make invalid String Ids obvious in testing (so they can be fixed) without crashing apps.
        return "^^Bad String^^"; //NON-NLS
    }

    public static String getLocalizedString(Context context, Locale desiredLocale, int stringId) {
        Log.e(TAG, "getLocalizedString. stringID: " + stringId + ", class: " + context.getApplicationInfo().className);
        try {
            Configuration conf = context.getResources().getConfiguration();
            conf = new Configuration(conf);
            conf.setLocale(desiredLocale);
            Context localizedContext = context.createConfigurationContext(conf);
            return localizedContext.getResources().getString(stringId);
        } catch (Exception e) {
            Log.d(TAG, "getLocalizedString", e);
        }
        // Make invalid String Ids obvious in testing (so they can be fixed) without crashing apps.
        return "^^Bad String^^"; //NON-NLS
    }

    public static String getString(String idName, Context context)
    {
        try {
            if (idName != null)
            {
                int id = context.getResources().getIdentifier(idName, "string", context.getPackageName());
                if (id != 0)
                {
                    return context.getResources().getString(id);
                }
                else
                {
                    return "^^Bad String^^"; //NON-NLS
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "getString(String,Context)", e);
        }
        // Make invalid String Ids obvious in testing (so they can be fixed) without crashing apps.
        return "^^Bad String^^"; //NON-NLS
    }
}
