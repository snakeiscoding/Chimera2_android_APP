package com.jht.chimera.service.logger;

import static com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.Debug.DATA_WATCHDOG_REBOOT_COUNTER_INT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.network.AddressHelper;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.SystemUtils;
import com.jht.chimera.lib.common.VersionHelper;
import com.jht.chimera.lib.common.analytic.AnalyticsUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.performance.SystemStats;
import com.jht.chimera.lib.workoutapps.AppManager;
import com.jht.chimera.service.workout.WorkoutLaunchData;
import com.jht.crashreportlib.CrashReportingInterface;
import com.jht.crashreportlib.MachineInfo;
import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.liberror.ErrorCodeManager;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;


/**
 * Contains all of the features necessary for monitoring the {@code logcat} output for crashes and
 * all of the logic for handling those crashes.
 * <p></p>
 * <strong>Note:</strong> Access to this class is intentionally restricted to this package.
 */
@SuppressWarnings({"HardCodedStringLiteral"})
class LogMonitor {
    private static final String PERSISTENT_DIR = "/sdcard/log";
    private static final String WORKOUT_MACHINE_STATE = "Workout";
    private static final String IDLE_MACHINE_STATE = "Idle";
    private static final float DEFAULT_CRASH_COLLECTION_TIME_MINS = 5f;
    private static final String TAG = "CRL";
    private final Context mContext;
    private CrashReportingInterface crashReportingInterface;
    private DataMatrix mDataMatrix = new DataMatrix();

    LogMonitor(@NonNull final Context context) {
        Log.i(TAG, "LogMonitor Starting");
        mContext = context;
        File crashLogDirectory = new File(Environment.getExternalStorageDirectory(), "log" + File.separator + "crash");

        // delete any existing crash logs and then recreate the crash directory
        deleteDirectory(crashLogDirectory);
        createDirectory(crashLogDirectory);

        DataMatrix tempDataMatrix = new DataMatrix();

        //Start Log Monitoring
        tempDataMatrix.createBoolAdapter(DAPIKeys.DATA_DAPI_MACHINE_UUID_VALIDATED_BOOL, validMachine -> {
            if(validMachine) {
                startLogMonitoring();
                tempDataMatrix.finish();
            }
        });
    }


    /**
     * Start monitoring the output of {@code logcat} if it is not already running.
     */
    @NonNull
    private synchronized void startLogMonitoring() {
        stopLogMonitoring();

        if (DataMatrix.getBoolean(SettingKeys.Experimental.DATA_CRASH_MONITORING_BOOL)) {
            Log.d(TAG, "starting crash monitor");

            crashReportingInterface = new CrashReportingInterface(mContext, collectMachineInfo(), ExecuteAsRoot.getRootAccessCode(), DataMatrix.getBoolean(SettingKeys.Experimental.DATA_CRASH_REPORTING_DISABLE_ENDPOINT_BOOL));

            setAdapters();

            crashReportingInterface.startMonitoringLogs();
            crashReportingInterface.setInternetConnection(DataMatrix.getBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL));
            crashReportingInterface.setCrashCollectingTime(DEFAULT_CRASH_COLLECTION_TIME_MINS);
            crashReportingInterface.addExtraFiles(updateExtraLogFiles());
            crashReportingInterface.addCrashCallback(i -> {updateExtraLogFiles();});

            checkWatchDogReboot();
        }
    }

    private void checkWatchDogReboot() {
        int currentWatchdogInt = DataMatrix.getInt(DATA_WATCHDOG_REBOOT_COUNTER_INT);
        crashReportingInterface.checkWatchDogReboot(currentWatchdogInt);
    }

    private void setAdapters(){
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, value -> {
            if(value == WorkoutKeys.Constants.RUN_STATE_STOP) {
                Log.i(TAG, "Allow crash to be sent - Workout stop");
                crashReportingInterface.setMachineState(IDLE_MACHINE_STATE);
                crashReportingInterface.sendCurrentCrash(true);
                updateCacheSize();
            }
        },false);

        mDataMatrix.createParcelableAdapter(WorkoutKeys.ACTION_LAUNCH_WORKOUT_PARCELABLE, WorkoutLaunchData.class, event -> {
            Log.i(TAG, "Allow crash to be sent - Workout start");
            crashReportingInterface.setMachineState(WORKOUT_MACHINE_STATE);
            crashReportingInterface.sendCurrentCrash(true);
            updateCacheSize();
        },false);

        mDataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_CRASH_REPORTING_DISABLE_ENDPOINT_BOOL, disableEndpoints -> {
            Log.i(TAG, "Disable Endpoints set to : " + disableEndpoints.toString());
            crashReportingInterface.setDisableEndpoints(disableEndpoints);
        }, true);

        mDataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_CRASH_REPORTING_DISABLE_BLOCKLIST_BOOL, disableBlocklist -> {
            Log.i(TAG, "Disable Blocklist set to : " + disableBlocklist.toString());
            crashReportingInterface.setDisableAWSBlocklist(disableBlocklist);
        }, true);

        mDataMatrix.createIntAdapter(SettingKeys.Experimental.DATA_CRASH_REPORTING_COLLECTION_TIME_MINUTES_INT, collectionTime -> {
            Log.i(TAG, "Collection time set to : " + collectionTime.toString());
            crashReportingInterface.setCrashCollectingTime(collectionTime);
        }, true);

        mDataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_CRASH_REPORTING_LOG_URL_OVERRIDE_BOOL, overrideLogEndpoint -> {
            if(!overrideLogEndpoint){
                Log.i(TAG, "Log override is set to : null");
                crashReportingInterface.setLogEndpointOverride(null);
            }
        }, true);

        mDataMatrix.createStringAdapter(SettingKeys.Experimental.DATA_CRASH_REPORTING_LOG_URL_OVERRIDE_STRING, overriddenLogEndpoint -> {
            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_CRASH_REPORTING_LOG_URL_OVERRIDE_BOOL)) {
                Log.i(TAG, "Log override is set to : " + overriddenLogEndpoint);
                crashReportingInterface.setLogEndpointOverride(overriddenLogEndpoint);
            }
        }, true);

        mDataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_CRASH_REPORTING_INCIDENT_URL_OVERRIDE_BOOL, overrideIncidentEndpoint -> {
            if(overrideIncidentEndpoint == false){
                Log.i(TAG, "Incident override is set to : null");
                crashReportingInterface.setIncidencesEndpointOverride(null);
            }
        }, true);

        mDataMatrix.createStringAdapter(SettingKeys.Experimental.DATA_CRASH_REPORTING_INCIDENT_URL_OVERRIDE_STRING, overriddenIncidentEndpoint -> {
            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_CRASH_REPORTING_LOG_URL_OVERRIDE_BOOL)) {
                Log.i(TAG, "Incident override is set to : " + overriddenIncidentEndpoint);
                crashReportingInterface.setIncidencesEndpointOverride(overriddenIncidentEndpoint);
            }
        },false);

        mDataMatrix.createEventAdapter(LoggerKeys.ACTION_SEND_MANUAL_CRASH, event -> {
            Log.i(TAG, "Sending Manual Crash");
            updateExtraLogFiles();
            crashReportingInterface.sendManualCrash();
            DataMatrix.sendEvent(HALKeys.ACTION_IO_PLAY_BEEP);
        },false);

        mDataMatrix.createEventAdapter(LoggerKeys.ACTION_CLEAR_CRASH_REPORTING_CACHE, event -> {
            Log.i(TAG, "Clearing Cache");
            crashReportingInterface.clearCache();
            updateCacheSize();
            DataMatrix.sendEvent(HALKeys.ACTION_IO_PLAY_BEEP);
        },false);

        mDataMatrix.createEventAdapter(LoggerKeys.ACTION_UPDATE_CRASH_REPORTING_CACHE, event -> {
            Log.i(TAG, "Updating Cache Size");
            updateCacheSize();
            DataMatrix.sendEvent(HALKeys.ACTION_IO_PLAY_BEEP);
        },false);

        mDataMatrix.createBoolAdapter(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL, internetStatus -> {
            Log.i(TAG, "internet Status" + internetStatus);
            crashReportingInterface.setInternetConnection(internetStatus);
        }, true);

        mDataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_CRASH_REPORTING_DEBUG_LOGS_BOOL, debugLogs -> {
            Log.i(TAG, "Crash Reporting Debugging: " + debugLogs);
            crashReportingInterface.setDebugEnabled(debugLogs);
        }, true);

        mDataMatrix.createBoolAdapter(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL, persistentEnabled -> {
            File persistent = new File(PERSISTENT_DIR);
            if(persistentEnabled) {
                Log.i(TAG, "Persistent Logs Enabled");

                crashReportingInterface.addExtraFile(persistent);
            }
            else {
                Log.i(TAG, "Persistent Logs Disabled");
                    crashReportingInterface.removeExtraFile(persistent);
            }
        }, true);

        mDataMatrix.createEventAdapter(LoggerKeys.ACTION_DOWNLOAD_CACHED_LOGS, event -> {
            final File USBDir = SystemUtils.getLogUSBPath(Calendar.getInstance());
            if (USBDir == null) {
                Log.w(TAG, "Aborting cache download callback handler.");
                return;
            }

            updateCacheSize();
            Log.i(TAG, "Downloading cached logs");

            Path sourceDir = Paths.get(crashReportingInterface.getCrashCache().getAbsolutePath());
            Path destinationDir = Paths.get(String.valueOf(USBDir));

            Log.d(TAG, "Copying From " + sourceDir + "/*");
            Log.d(TAG, "Copying To " + destinationDir);

            ExecuteAsRoot root = new ExecuteAsRoot();
            root.addCommand("cp -r " + sourceDir + "/* " + destinationDir);
            root.execute();

            DataMatrix.sendEvent(HALKeys.ACTION_IO_PLAY_BEEP);
            updateCacheSize();
        });
    }

    private synchronized void stopLogMonitoring() {
        if (crashReportingInterface != null) {
            crashReportingInterface.stopMonitoringLogs();
            crashReportingInterface = null;
        }
    }

    /**
     * updates the number of Crashes held in the Cache
     */
    private void updateCacheSize() {
        try (Stream<Path> files = Files.list(Paths.get(crashReportingInterface.getCrashCache().getAbsolutePath()))) {
            Log.d("CRL", "Checking cache size");
            int count = (int) files.count();
            Log.d("CRL", count + " Crashes in Cache");
            DataMatrix.putInt(SettingKeys.Experimental.DATA_CRASH_REPORTING_CACHED_CRASHES_INT, count);
        }
        catch (Exception e){
            Log.e("CRL", "Error updating cache size" + e);
        }
    }

    /** Takes destination folder and generates error code log file. Returns path to log file generated, otherwise null. */
    private @Nullable File updateErrorCodeLog(@NonNull File destination) {
        try {
            ErrorCodeManager.saveErrorCodes(destination);
            return new File(destination, "errors.txt"); // "saveErrorCodes" should maybe return the actual file generated instead of duplicating this
        } catch (Exception e) {
            Log.e(TAG, "Failed to write error codes file", e);
            return null;
        }
    }

    /**
     * Delete the given directory after the given amount of time delay.
     *
     * @param directory the directory to delete.
     */
    private void deleteDirectory(@NonNull final File directory) {
        new ExecuteAsRoot().addCommand("rm -rf " + directory.getAbsolutePath()).execute();
    }

    /**
     * Create the given directory.
     *
     * @param directory the directory to create.
     */
    private void createDirectory(@NonNull final File directory) {
        new ExecuteAsRoot().addCommand("mkdir -p " + directory.getAbsolutePath()).execute();
    }

    /**
     * Collects all machineInfo from the console and returns the data to be used in Crash reporting
     * @return
     */
    @SuppressLint("RestrictedApi")
    private MachineInfo collectMachineInfo() {

        MachineInfo machineInfo = new MachineInfo();
        //Set initial values
        machineInfo.setClubId(String.valueOf(DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT)));
        machineInfo.setMacAddress(AddressHelper.getValidMACAddress(true));
        machineInfo.setWifiStatus(DataMatrix.getBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL));
        machineInfo.setSoftwareVersion(BuildConfig.SoftwareVersion);
        machineInfo.setFrameType(FrameConfigManager.getInstance().getFrameType().name().replace('_', ' '));
        machineInfo.setConsoleType(ProductConfig.getConsoleType().marketingName);
        machineInfo.setPlatform(BuildConfig.UPDATE_EQUIPMENT);
        machineInfo.setConsoleSerial(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING));
        machineInfo.setFrameSerial(DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING));
        machineInfo.setTimezone(TimeZone.getDefault().getID());
        machineInfo.setDapiEnvironment(JHTCommonTypes.DAPIEnvironment.values()[DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)].toString());
        machineInfo.setUuid(DataMatrix.getString(DAPIKeys.DATA_DAPI_MACHINE_UUID_STRING));

        //Set up adapters to listen for updates to any machine info
        mDataMatrix.createIntAdapter(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, club_id -> {
           machineInfo.setClubId(club_id.toString());
        });
        mDataMatrix.createBoolAdapter(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL, machineInfo::setWifiStatus);
        mDataMatrix.createStringAdapter(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, machineInfo::setConsoleSerial);
        mDataMatrix.createStringAdapter(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, machineInfo::setFrameSerial);
        mDataMatrix.createStringAdapter(DAPIKeys.DATA_DAPI_MACHINE_UUID_STRING, machineInfo::setUuid);

        mDataMatrix.createIntAdapter(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, dapi_int -> {
            machineInfo.setDapiEnvironment((JHTCommonTypes.DAPIEnvironment.values()[dapi_int].toString()));
        });

        return machineInfo;
    }

    /**
     * Copies the settings folder to a destination directory
     * @param destinationDir
     */
    public void copyLogFilesToDir(@NonNull Path destinationDir){
        File logFolder= new File("/sdcard/settings");
        Path sourceDir = Paths.get(logFolder.getAbsolutePath());

        Log.d(TAG, "Copying From " + sourceDir + "/*");
        Log.d(TAG, "Copying To " + destinationDir);

        ExecuteAsRoot root = new ExecuteAsRoot();
        root.addCommand("cp -r " + sourceDir + "/* " + destinationDir);
        root.execute();
}

    /**
     * Updates and return the extra log files to be sent on crash
     * @return ArrayList<ExtraLogFiles>
     */
    public ArrayList<File> updateExtraLogFiles(){
        Log.d(TAG, "Updating Extra Files");
        ArrayList<File> extraFiles = new ArrayList<File>();
        @SuppressLint("SdCardPath") File logFolder= new File("/sdcard/settings");
        logFolder.mkdirs();
        File settingsZip = new File(logFolder, "settings.zip");
        File analyticsZip = new File(logFolder, "analytics.zip");
        File systemStats = new File (logFolder, "systemStats.txt");
        File DataMatrixJson = new File(logFolder, "DataMatrix.json");
        File versions = new File(logFolder, "versions.txt");
        File screenshot = new File(logFolder, "screenshot.png");

        try {
            updateLogSettingsZipFile(settingsZip);
            updateAnalyticsFile(analyticsZip);
            updateSystemStatsFile(systemStats);
            updateLogDataMatrixJsonFile(DataMatrixJson);
            updateLogAppVersionsFile(versions);
            updateLogScreenshotFile(screenshot);
            updateCacheSize();
            File errorCodes = updateErrorCodeLog(logFolder);

            extraFiles.add(settingsZip);
            extraFiles.add(analyticsZip);
            extraFiles.add(systemStats);
            extraFiles.add(DataMatrixJson);
            extraFiles.add(versions);
            extraFiles.add(screenshot);
            if (errorCodes != null) extraFiles.add(errorCodes);
        }
        catch (Exception e) {
            Log.e(TAG, "Error Updating Extra Files", e);
        }
        return extraFiles;
    }

    /**
     * Updates the screenshot file to be sent with crashes
     * @param screenshot
     */
    private void updateLogScreenshotFile(@NonNull File screenshot){
        ExecuteAsRoot root = new ExecuteAsRoot();
        root.addCommand("screencap -p " + screenshot.getAbsolutePath());
        root.addCommand("sync");
        root.execute();
    }

    /**
     * Updates the app versions file holding installed apps and their versions
     * @param version
     */
    private void updateLogAppVersionsFile(@NonNull File version) {

        try(FileOutputStream fileOutputStream = new FileOutputStream(version.getAbsolutePath())) {
            StringBuilder versions = new StringBuilder();
            PackageManager pm = mContext.getPackageManager();

            versions.append("Version: " ).append(BuildConfig.SoftwareVersion).append("\r\n");
            versions.append("Firmware Versions:\r\n");

            String firmwareVersions = VersionHelper.getFirmwareVersions("\t", "\r\n");
            versions.append(firmwareVersions);

            versions.append("Third Party App Versions:\r\n");
            List<String> packages = AppManager.getInstance().getApkPackages();
            for (String p : packages) {
                try {
                    PackageInfo info = pm.getPackageInfo(p, 0);
                    versions.append("\t").append(pm.getApplicationLabel(info.applicationInfo)).append(" - ").append(info.versionName).append(" [").append(info.versionCode).append("]").append("\r\n");
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "Package missing", e);
                }
            }
            fileOutputStream.write(versions.toString().getBytes());
        }
        catch(Exception e){
            Log.e(TAG, "Error updating app versions file", e);
        }
    }

    /**
     * Update the analytics file to be sent with all crashes
     * @param analyticsFile
     */
    private void updateAnalyticsFile(@NonNull File analyticsFile) {
        File analyticsFolder = new File(AnalyticsUtil.STATS_FOLDER);

        try {
            ZipParameters parameters = new ZipParameters();

            ZipFile zipFile = new ZipFile(analyticsFile);

            File[] analyticsFiles = analyticsFolder.listFiles();
            if(analyticsFiles != null) {
                for (File file : analyticsFiles) {
                    if (file.isFile()) {
                        zipFile.addFile(file, parameters);
                    } else if (file.isDirectory()) {
                        zipFile.addFolder(file, parameters);
                    }
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error updating analytics file", e);
        }
    }

    /**
     * Update the system stats file to be sent with all crashes
     * @param systemStatsFile
     */
    private void updateSystemStatsFile(@NonNull File systemStatsFile) throws InterruptedException {
        SystemStats stats = new SystemStats(mContext);
        stats.startRecording(100);
        Thread.sleep(10);
        String currentSystemStats = stats.analyzeCurrentData();
        stats.stopRecording();

        try (FileOutputStream outputStream = new FileOutputStream(systemStatsFile)) {
            outputStream.write(currentSystemStats.getBytes());
        }
        catch (Exception e) {
            Log.e(TAG, "Error updating system stats file", e);
        }
    }

    /**
     * Update the settings zip file to be sent with all crashes
     * @param settingsZip
     */
    private void updateLogSettingsZipFile(@NonNull File settingsZip) {

        @SuppressLint("SdCardPath") File settingsFolder = new File("/data/data/com.jht.chimera/files/settings/");
        try {
            ZipParameters parameters = new ZipParameters();

            ZipFile zipFile = new ZipFile(settingsZip);

            File[] settingsFiles = settingsFolder.listFiles();
            if(settingsFiles != null) {
                for (File file : settingsFiles) {
                    if (file.isFile()) {
                        zipFile.addFile(file, parameters);
                    } else if (file.isDirectory()) {
                        zipFile.addFolder(file, parameters);
                    }
                }
            }
        }
        catch (ZipException e) {
            Log.e(TAG, "Error updating settings zip file", e);
        }
    }

    /**
     * Update the DataMatrix.json file holding all datamatrix keys and values
     * @param DataMatrixJson
     */
    private void updateLogDataMatrixJsonFile(@NonNull File DataMatrixJson){
        JSONArray jsonArray = new JSONArray();

        for(DataDefault<?> setting : SettingKeys.sDataDefaultList) {
            switch (setting.dataType()) {
                case "integer": { //NON-NLS
                    int value = DataMatrix.getInt(setting.name);
                    jsonArray.put(createKeyValue(setting.name, String.valueOf(value), setting.dataType()));
                    break;
                }
                case "string": { //NON-NLS
                    String value = DataMatrix.getString(setting.name);
                    jsonArray.put(createKeyValue(setting.name, value, setting.dataType()));
                    break;
                }
                case "boolean": { //NON-NLS
                    boolean value = DataMatrix.getBoolean(setting.name);
                    jsonArray.put(createKeyValue(setting.name, String.valueOf(value), setting.dataType()));
                    break;
                }
                case "double": { //NON-NLS
                    double value = DataMatrix.getDouble(setting.name);
                    jsonArray.put(createKeyValue(setting.name, String.valueOf(value), setting.dataType()));
                    break;
                }
                case "long": { //NON-NLS
                    long value = DataMatrix.getLong(setting.name);
                    jsonArray.put(createKeyValue(setting.name, String.valueOf(value), setting.dataType()));
                    break;
                }
                case "float": { //NON-NLS
                    float value = DataMatrix.getFloat(setting.name);
                    jsonArray.put(createKeyValue(setting.name, String.valueOf(value), setting.dataType()));
                    break;
                }
                default:
                    break;
            }
        }
        try (FileOutputStream outputStream = new FileOutputStream(DataMatrixJson)) {
            outputStream.write(jsonArray.toString().getBytes());
        }
        catch (Exception e) {
            Log.e(TAG, "Error updating datamatrix file", e);
        }
    }


    @NonNls static final private String KEY = "key";
    @NonNls static final private String VALUE = "value";
    @NonNls static final private String TYPE = "type";
    private JSONObject createKeyValue(@NonNull String name, @NonNull String value, @NonNull String dataType) {

        JSONObject object = new JSONObject();
        try {
            object.put(KEY, name);
            object.put(VALUE, value);
            object.put(TYPE, dataType);
        }
        catch (Exception ignored) {
        }
        return object;
    }


    /**
     * Creates a Manual/"Persistent" crash entry
     */
    public void createManualCrashEntry() {
        if (crashReportingInterface != null) crashReportingInterface.sendManualCrash();
    }

}
