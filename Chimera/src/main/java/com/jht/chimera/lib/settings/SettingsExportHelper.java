package com.jht.chimera.lib.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.DataMatrix;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * This class caches the system settings which includes the DM settings keys and the settings files.
 */
public class SettingsExportHelper {
    private static final String TAG = "SettingExportHelper";

    @NonNls
    static final private String KEY = "key";
    @NonNls
    static final private String VALUE = "value";
    @NonNls
    static final private String TYPE = "type";

    private SettingsExportHelper() {
    }

    /**
     * Retrieves data matrix settings as a JSON array.
     *
     * @param all if true, includes all settings; if false, only includes settings with a non-empty export type.
     * @param filter an optional filter to further refine the settings returned (null for no filtering).
     * @return a JSON array of key-value pairs representing the settings.
     */
    public static JSONArray getDataMatrixSettings(boolean all, BiPredicate<String, Object> filter) {
        return getDataMatrixSettings(all, filter, SettingKeys.sDataDefaultList);
    }

    @VisibleForTesting()
    static JSONArray getDataMatrixSettings(boolean all, BiPredicate<String, Object> filter, List<DataDefault<?>> sDataDefaultList) {
        JSONArray settings = new JSONArray();
        for(DataDefault<?> setting : sDataDefaultList) {
            if(all || !setting.exportType().isEmpty()) {
                try {
                    Object settingValue = getDataMatrixValue(setting.name, setting.dataType());
                    if (filter == null || filter.test(setting.name, settingValue)) {
                        settings.put(createKeyValue(setting.name, String.valueOf(settingValue), setting.dataType()));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Unable to include " + setting.name + " in returned settings JSON.", e);
                }
            }
        }
        return settings;
    }


    public static void updateDataMatrixKeys(@NonNull JSONArray settings) {
        try {
            for (int i = 0; i < settings.length(); i++) {
                JSONObject object = settings.optJSONObject(i);
                if (object != null) {
                    if (object.has(KEY) && object.has(TYPE) && object.has(VALUE)) {
                        switch (object.optString(TYPE, "")) {
                            case "integer": // NON-NLS
                                DataMatrix.putInt(object.optString(KEY), Integer.parseInt(object.optString(VALUE, "0")));
                                break;
                            case "string": // NON-NLS
                                DataMatrix.putString(object.optString(KEY), object.optString(VALUE, ""));
                                break;
                            case "boolean": // NON-NLS
                                DataMatrix.putBoolean(object.optString(KEY), Boolean.parseBoolean(object.optString(VALUE, "false")));
                                break;
                            case "double": // NON-NLS
                                DataMatrix.putDouble(object.optString(KEY), Double.parseDouble(object.optString(VALUE, "0.0")));
                                break;
                            case "long": // NON-NLS
                                DataMatrix.putLong(object.optString(KEY), Long.parseLong(object.optString(VALUE, "0")));
                                break;
                            case "float": // NON-NLS
                                DataMatrix.putFloat(object.optString(KEY), Float.parseFloat(object.optString(VALUE, "0.0")));
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to update data matrix keys.", e);
        }
    }

    private static JSONObject createKeyValue(String name, String value, String dataType) {

        JSONObject object = new JSONObject();
        try {
            object.put(KEY, name);
            object.put(VALUE, value);
            object.put(TYPE, dataType);
        } catch (Exception ignored) {
        }
        return object;
    }
    @SuppressLint("SdCardPath")
    public static synchronized void zipSettings(Context context, File destination) {
        try {
            ZipParameters parameters = new ZipParameters();
            ZipFile zipFile = new ZipFile(destination);
            File[] settingsFile = new File(context.getFilesDir(), "settings").listFiles();
            if(settingsFile != null)
                for(File settingFile : settingsFile) {
                    if(settingFile.isFile()) {
                        zipFile.addFile(settingFile, parameters);
                    }
                    else if(settingFile.isDirectory()) {
                        zipFile.addFolder(settingFile, parameters);
                    }
                }
            ExecuteAsRoot.runSyncCommand();

        } catch (ZipException ex) {
            Log.e(TAG, "zip exception. THREAD: " + Thread.currentThread().getName() + ". ", ex);
        }
        //FileUtils.delete(settingsFolder);

    }

    public static void extractSettings(@NonNull Context context, @NonNull File file) {
        try {
            File destinationFile = new File(context.getFilesDir(), "settings");
            FileUtils.delete(destinationFile);
            //noinspection ResultOfMethodCallIgnored
            destinationFile.mkdirs();
            ZipFile zipFile = new ZipFile(file);
            zipFile.extractAll(destinationFile.getAbsolutePath());
        }
        catch(ZipException ignored) {}
        //ExecuteAsRoot.fixFilesInPackage("com.jht.chimera");

    }

    public static void collectSettings(@NonNull Context context, @NonNull File file) {
        try {
            ZipParameters parameters = new ZipParameters();
            ZipFile zipFile = new ZipFile(file);
            File[] settingsFile = new File(context.getFilesDir(), "settings").listFiles();
            if(settingsFile != null)
                for(File settingFile : settingsFile) {
                    if(settingFile.isFile()) {
                        zipFile.addFile(settingFile, parameters);
                    }
                    else if(settingFile.isDirectory()) {
                        zipFile.addFolder(settingFile, parameters);
                    }
                }
            ExecuteAsRoot.runSyncCommand();

        } catch (ZipException ex) {
            Log.e(TAG, "zip exception. THREAD: " + Thread.currentThread().getName() + ". ", ex);
        }
        //FileUtils.delete(settingsFolder);

    }

    /*
     * Helper method to retrieve a data matrix value for a given key and data type.
     */
    private static Object getDataMatrixValue(@NonNull Object key, @NonNull String dataType) {
        switch (dataType) {
            case "integer": return DataMatrix.getInt(key);
            case "string": return DataMatrix.getString(key);
            case "boolean": return DataMatrix.getBoolean(key);
            case "double": return DataMatrix.getDouble(key);
            case "long": return DataMatrix.getLong(key);
        }
        throw new IllegalArgumentException("Unknown data type: " + dataType);
    }
}
