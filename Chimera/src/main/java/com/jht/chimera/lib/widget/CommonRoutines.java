package com.jht.chimera.lib.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.events.JHTDataNotification;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;

public class CommonRoutines {
    @NonNls private static final String TAG = "CommonRoutines";
    private static final DataMatrix sDataMatrix = new DataMatrix();
    private static boolean sIsMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);

    static {
        sDataMatrix.createBoolAdapter(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, value -> sIsMetric = value);
    }

    static public JHTCommonTypes.Unit getUnit() {
        if (sIsMetric)
            return JHTCommonTypes.Unit.Metric;
        return JHTCommonTypes.Unit.Imperial;
    }

    static public String safeGetString(Context context, int stringID) {
        if (stringID > 0)
            return context.getString(stringID).toLowerCase();

        return "";
    }

    static public int getStringIdDistance() {
        return sIsMetric ? R.string.km_lc : R.string.mi_lc;
    }

    static public int getStringIdSpeed() {
        return sIsMetric ? R.string.kph_lc : R.string.mph_lc;
    }

    static public float getConvertFactorDistance() {
        return sIsMetric ? 1f : DistanceConvertUtil.convertKilometersToMiles(1f);
    }

    static public float getConvertFactorSpeed() {
        return sIsMetric ? 1f : DistanceConvertUtil.convertKilometersToMiles(1f);
    }

    static public float getConvertFactorWeight() {
        return sIsMetric ? 1f : WeightConvertUtil.convertKilogramsToPounds(1f);
    }

    static public float getConvertFactorHeight() {
        return sIsMetric ? 1f : DistanceConvertUtil.convert(1, DistanceConvertUtil.CENTIMETERS_ID, DistanceConvertUtil.INCHES_ID);
    }

    @SuppressLint("SetTextI18n")
    static public void setButtonText(final Button button, final float value, final int stringID, final boolean isFloat) {
        if (value < 0) return;
        button.post(() -> {
            if (isFloat) {
                button.setText(String.format(Locale.US, "%.1f", value) + " " + safeGetString(button.getContext(), stringID));
            } else {
                button.setText(String.format(Locale.US, "%.0f", value) + " " + safeGetString(button.getContext(), stringID));
            }
        });
    }

    public static int getAge(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        int yearDiff = currentYear - year;
        int monthDiff = currentMonth - month;
        int dayDiff = currentDay - day;

        int age = yearDiff;

        // If their birthday has not yet passed in the year subtract a year
        if (monthDiff < 0) {
            age--;
        } else if (monthDiff == 0 && dayDiff <= 0) {
            age--;
        }

        return age;
    }

    public static int parseInt(String text) {
        int value = 0;
        try {
            value = Integer.parseInt(text);
            if (value < 0) value = 0;
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return value;
    }

    public static float parseFloat(String text) {
        float value = 0;
        try {
            value = Float.parseFloat(text);
            if (value < 0) value = 0;
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return value;
    }

    public static float round(float value, int places) {
        float scale = (float) Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    public static <E extends Enum<?>> int convertStringToOrdinal(E[] values, String value) {
        for(int i = 0; i < values.length; i++) {
            if(values[i].name().equals(value)) {
                return i;
            }
        }
        return 0;
    }

    public static void hideKeyboard(Activity activity) {
        if (activity == null) return;
        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static Uri getAssetUri(String path) {
        @SuppressLint("SdCardPath") File itemFile = new File("/sdcard/jht", path); //NON-NLS
        if (itemFile.exists()) return Uri.fromFile(itemFile);
        return Uri.parse("assets://" + path); //NON-NLS
    }

    public static boolean doesAssetExist(Context context, String directory, String fileName) {
        // Is it a file on sd card?
        @SuppressLint("SdCardPath") File itemFile = new File("/sdcard/jht/" + directory, fileName); //NON-NLS
        if (itemFile.exists()) return true;
        // Is it in the assets?
        try {
            final String[] assets = context.getAssets().list(directory);
            for (String asset : assets) {
                if (asset.equals(fileName)) return true;
            }
        } catch (Exception ignore) {}
        return false;
    }

    // Get resource id by string to avoid compile error if it does not exist
    // example> int resID = getResId("some_view", R.id.class); // or other resource class
    // alternative> getResources().getIdentifier(id, "id", context.getPackageName());
    /* Better to use id.xml resource for ids not used in all flavors
    public static int getResId(String resName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            return Resources.ID_NULL;
        }
    }*/


    /*
    The following code is used as reference for the code below when initializing an activity with a
    webview.  During the inflate of a view that contains a webview Android creates a
    webview_data.lock file and grabs a file lock so that it can only be used by one process.  This
    lock is released by the garbage collector.  However this may not run or can lead to race
    conditions.  We may be adding to this by killing the process.  You can simulate a failure with
    the code in this comment block.  If you look at the log of the code below you will see the first
    attempt fail and the second attempt succeed.  Note that in this test the lock is never released.
    You would do this with a lock.close call.  Our fix is to detect the failure and delete the lock
    file.

        File lockFile = new File(activity.getFilesDir(), "../app_webview_" + dataPath + "/webview_data.lock");
        setWebViewDataDirectory(dataPath);
        RandomAccessFile sLockFile = null;
        FileLock lock = null;
        // Sometimes we can crash if there is a web view and the previous process hasn't completed
        // We try 5 times to give the other process a chance to complete.
        try {
            lockFile.getParentFile().mkdirs();
            sLockFile = new RandomAccessFile(lockFile, "rw");
            lock = sLockFile.getChannel().tryLock();
        } catch (Exception ex) {
            Log.d(TAG, "Error creating random access file " + lockFile.getAbsolutePath());
        }
        for(int i = 0; i < 5; i++) {
            if(i == 1 && sLockFile != null) {
                try {
                    Log.d(TAG, "Should close and delete file.");
                    sLockFile.close();
                    lockFile.delete();

                } catch (IOException ignored) {
                }
                sLockFile = null;
            }
            try {
                activity.setContentView(layoutResID);
                break;
            } catch(Exception ex) {
                Log.d(TAG, "caught exception initializing web activity attempt " + i);
                if(i == 4) {
                    throw ex;
                }
            }
            SystemClock.sleep(1000);
        }
     */

    private interface WebViewInflation {
        void inflate();
    }

    private static void setWebViewDataDirectory(@NonNull Context context, @NonNull String dataPath, @NonNull WebViewInflation inflation) {
        File lockFile = new File(context.getFilesDir(), "../app_webview_" + dataPath + "/webview_data.lock");
        // We used to put this in a static block.  It can fail if the webview was already initialized
        // and we don't care about that failure.
        try {
            WebView.setDataDirectorySuffix(dataPath);
        } catch(IllegalStateException ignored) {}

        for(int i = 0; i < 5; i++) {
            try {
                inflation.inflate();
                return;
            } catch(Exception ex) {
                Log.d(TAG, "caught exception initializing web activity attempt " + i);
                // Delete the lock file as it is held by a dead or dying process.
                //noinspection ResultOfMethodCallIgnored
                lockFile.delete();
                if(i == 4) {
                    throw ex;
                }
            }
            SystemClock.sleep(100);
        }
    }

    public static void initWebActivity(@NonNull Activity activity, @LayoutRes int layoutResID, @NonNull String dataPath) {
        setWebViewDataDirectory(activity, dataPath, () -> activity.setContentView(layoutResID));
    }

    public static void initWebWidget(@NonNull Context context, @NonNull ViewGroup viewGroup, @LayoutRes int layoutResID, @NonNull String dataPath, boolean attachToRoot) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setWebViewDataDirectory(context, dataPath, () -> inflater.inflate(layoutResID, viewGroup, attachToRoot));
    }

    public static View createWebWidget(@NonNull Context context, @LayoutRes int layoutResID, @NonNull String dataPath, @Nullable ViewGroup root) {
        JHTDataNotification<View> viewNotification = new JHTDataNotification<>();
        setWebViewDataDirectory(context, dataPath, () -> viewNotification.setData(LayoutInflater.from(context).inflate(layoutResID, root)));
        return viewNotification.waitForData(700);
    }

}
