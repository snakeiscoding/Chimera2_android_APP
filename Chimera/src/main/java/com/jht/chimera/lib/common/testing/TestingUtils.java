package com.jht.chimera.lib.common.testing;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Size;

import com.jht.chimera.lib.common.context.ContextUtil;

import java.util.Objects;

/**
 * These are the common testing utility functions. These have been added because we need to have the
 * ability for the toggling into a custom testing mode which at a minimum does the following:
 * <p></p>
 *
 * <ol>
 *     <li>
 *         Disable all activity timers. These need to be disabled because Appium is very slow and
 *         intermittently takes longer than the
 *     </li>
 * </ol>
 */
public final class TestingUtils {
    private TestingUtils() {}

    /**
     * The custom tag to use when logging messages for operations related to this utility class.
     */
    @Size(max = 23)
    public static final String TAG = "ARR_AUTOMATION";

    /**
     * The custom boolean DataMatrix key that maintains the status of whether testing is
     * enabled/disabled.
     */
    public static final String KEY = "DATA_TEST_AUTOMATION_ENABLED_BOOL";

    /**
     * The default status for the testing. This is something that I actually need to allow
     * customizing through a different utility.
     */
    private static final boolean DEFAULT = false;

    @NonNull
    private static final String SHARED_PREF_FILE_NAME = "TestAutomationSettings";

    /**
     * @return true if automation is enabled, else false.
     */
    public synchronized static boolean isAutomationEnabled() {
        return isAutomationEnabled(getContext());
    }

    /**
     * @param context The current context.
     * @return true if automation mode is enabled, else false.
     */
    public synchronized static boolean isAutomationEnabled(@NonNull final Context context) {
        SharedPreferences preferences = getSharedPreferences(context);

        if (!preferences.contains(KEY)) {
            Log.d(TAG, "Missing automation key, will be using the default value.");
        }

        return preferences.getBoolean(KEY, DEFAULT);
    }

    /**
     * @param enabled true if enabling automation, else false.
     */
    public synchronized static void setAutomationEnabled(final boolean enabled) {
        setAutomationEnabled(getContext(), enabled);
    }

    /**
     * Enable/Disable automation mode.
     * @param context the current context.
     * @param enabled true if enabling, else false.
     */
    public synchronized static void setAutomationEnabled(@NonNull final Context context, final boolean enabled) {
        if (isAutomationEnabled(context) == enabled) {
            Log.d(TAG, "Automation Mode already " + enabled + ". Ignoring change request.");
            return;
        }

        Log.d(TAG, "Setting Automation Mode to " + enabled + ". Must reboot for change to be applied.");
        getSharedPreferences(context)
                .edit()
                .putBoolean(KEY, enabled)
                .apply();
    }

    /**
     * @return The current application context.
     */
    @NonNull
    private synchronized static Context getContext() {
        return Objects.requireNonNull(ContextUtil.getApplicationContext());
    }

    private static SharedPreferences mPreferences = null;

    /**
     * Get the shared preferences.
     * @param context The current {@link Context}.
     * @return the {@link SharedPreferences}.
     */
    @NonNull
    private synchronized static SharedPreferences getSharedPreferences(@NonNull final Context context) {
        if (mPreferences == null)
            mPreferences = context.getSharedPreferences(SHARED_PREF_FILE_NAME,
                    Context.MODE_PRIVATE
            );

        return mPreferences;
    }
}
