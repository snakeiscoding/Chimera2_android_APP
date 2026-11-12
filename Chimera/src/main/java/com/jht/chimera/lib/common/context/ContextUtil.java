package com.jht.chimera.lib.common.context;

import android.app.Application;
import android.content.Context;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.jht.chimera.BuildConfig;

import java.lang.reflect.Method;

public final class ContextUtil {
    @NonNull
    private static final String TAG = "ContextUtil";

    /** Prevent attempts to instantiate this utility class. */
    private ContextUtil() {}

    @Nullable
    public static Context getApplicationContext() {
        Context context = null;

        try {
            final Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            final Method method = activityThreadClass.getMethod("currentApplication");
            Application application = (Application) method.invoke(null, (Object[]) null);
            context = application.getApplicationContext();
        } catch (Exception e) {
            // Removing this so we can use it in a unit test.
            //Log.e(TAG, "This exception prevented acquiring the current application context.", e);
        }

        return context;
    }

    /**
     * Returns a localized string from the application's package's default string table.
     *
     * @param resId Resource id for the string.
     * @return The string data associated with the resource, stripped of styled text information. <strong>Note,</strong>
     * the empty string is returned if there is an issue getting the current context.
     */
    @NonNull
    public static String getString(@StringRes final int resId) {
        final Context context = getApplicationContext();
        return context == null ? "" : context.getString(resId);
    }

    /**
     * Returns a ContextThemeWrapper based on the current build variant
     *
     * @param context Context from panel requesting the theme
     * @return The ContextThemeWrapper which encapsulates the application theme for the current build variant.
     * This is necessary for views that are common across multiple build variants, but whose styling is different.
     */

    @NonNull
    public static ContextThemeWrapper getThemeContext(Context context) {
        int style = context.getResources().getIdentifier(BuildConfig.APP_THEME, "style", context.getPackageName());
        return new ContextThemeWrapper(context, style);
    }
}
