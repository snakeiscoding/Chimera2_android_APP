package com.jht.chimera.service.sso;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.androidcommonalgorithms.resources.ResourceHelper;
import com.jht.androidcommonalgorithms.singlesignon.SingleSignOnInterface;
import com.jht.androidcommonalgorithms.singlesignon.SingleSignOnItem;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.dapi.DapiCreateUser;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SSOKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.process.KillProcessHelper;
import com.jht.chimera.lib.workoutapps.AppInfo;
import com.jht.chimera.lib.workoutapps.AppManager;
import com.jht.chimera.service.dapi.UserProfileManager;
import com.jht.chimera.service.workout.enums.WorkoutRunState;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.oauthlib.auth.BksCredentialProvider;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import org.jetbrains.annotations.NonNls;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Manages Single Sign On (SSO) capabilities for apps that support it.
 */
@SuppressWarnings("SDCardPath")
public class SingleSignOnService extends Service implements SingleSignOnInterface.IOnGetData, SingleSignOnInterface.IOnPutData {
    static final String TAG = "SingleSignOnService";

    DataMatrix dataMatrix = new DataMatrix();
    SingleSignOnInterface singleSignOnInterface;
    @NonNls final String SSO_DOWNLOAD_PATH = "/data/data/com.jht.chimera/files/sso/sso_d";
    @NonNls final String SSO_UPLOAD_PATH = "/data/data/com.jht.chimera/files/sso/sso_u";
    @NonNls final String CHANGE_WORKOUT_PACKAGE = "com.jht.changeuserinfo"; // The change activities were removed, but this is used as a key for SSO weight
    @NonNls final String CHANGE_WORKOUT_VERSION = "1.0";
    @NonNls final String USER_EXT_INFO_PACKAGE = "user_extended_info";

    SSOSettings mSSOSettings = SSOSettings.getInstance();
    volatile boolean stopDownload = false;
    Context mContext = ContextUtil.getApplicationContext(); // context has not been available at construction for some reason, so we have been using ContextUtil
    List<String> tpaList = AppManager.getInstance().getTpaPackages(); // app list is only dependent on console & frame, so safe to grab once (icon list is dynamic)
    Set<String> downloadedAppPackageList = new HashSet<>();
    String currentDowloadUserId = "";
    private JHTNotification uploadUserDataComplete = new JHTNotification();
    private static final String[] webProcessNames = new String[] { "com.jht.webapp", "com.jht.webappsso", "com.jht.webBrowserCompose" };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d("ServiceStarter", "Starting " + this);
        super.onCreate();
        init();
        Log.d("ServiceStarter", "Ending " + this);
    }

    private String getZipPassword() {
        BksCredentialProvider credentialProvider = new BksCredentialProvider(mContext);
        return credentialProvider.getString("SSO_ZIP_PASSWORD");
    }

    /**
     * @param packageName app package to check
     * @return true if TPA, false if not
     */
    private boolean isTpaPackage(String packageName) {
        for (String tpa : tpaList) {
            if (packageName.equals(tpa)) return true;
        }
        return false;
    }

    private Thread killApps() {
        return killApps(true, false);
    }

    /**
     * @param clearData true to clear data, false to retain data
     * @param forceAllApps true to force, false to not force
     * @return the thread
     */
    private Thread killApps(boolean clearData, boolean forceAllApps) {
        // if there are no side effects to calling with uninstalled package, this list could be comprehensive across flavors (otherwise could check if installed first)
        Thread thd = new Thread(() -> {
            killAppsSync(clearData, forceAllApps);
        });
        thd.start();
        return thd;
    }

    /**
     * @param clearData true to clear saved data
     * @param forceAllApps false to not force
     */
    private void killAppsSync(boolean clearData, boolean forceAllApps) {
        KillProcessHelper killProcessHelper = new KillProcessHelper();
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();

        for (String processName : webProcessNames) killProcessHelper.addPackageToKill(processName, false);
        killProcessHelper.addPackageToKill("com.jht.mymedia", false);
        killProcessHelper.addPackageToKill("com.jht.webbrowserapp", false);

        String overrideAppId = DataMatrix.getString(SettingKeys.Experimental.DATA_OVERRIDE_MAIN_APP_ID_STRING);

        for (String pack : tpaList) {
            AppInfo app = AppManager.getInstance().getAppFromPackage(pack);

            if(app != null){
                if (Objects.equals(app.uuid, overrideAppId)) continue;
                if (!forceAllApps && app.requiresPrelaunchKill) continue;
            }
            // add third party app to disable list
            if (AppManager.shouldPackageBeToggled(pack)) killProcessHelper.addPackageToDisable(pack, false);

            if (clearData) {
                killProcessHelper.addPackagesToClear(pack, false);
                addPackageCleanUpCommands(executeAsRoot, pack);
            }
        }
        killProcessHelper.kill();

        if (clearData) {
            // Could these move to killUserWebData() ?
            executeAsRoot.addCommand("rm -rf /sdcard/Android/data/com.facebook.*");
            executeAsRoot.addCommand("rm -rf /sdcard/Android/obb/com.facebook.*");
            executeAsRoot.addCommand("rm -rf /data/misc/profiles/cur/0/com.facebook.*");

            executeAsRoot.addCommand("rm -rf /data/system_ce/0/accounts*");
            executeAsRoot.addCommand("rm -rf /data/system_de/0/accounts*");
            executeAsRoot.addCommand("rm -rf /data/system/sync/accounts.xml");

            // remove web caches - see killUserWebData
            // Note from BDC. This is moved to the splash screen.  If we do it here there is a chance
            // that we hit this at the exact time that the workout summary loads.  The workout summary
            // has a webview for end of workout surveys and if our timing is just right we will
            // crash the main app.
            // executeAsRoot.addCommand("rm -rf /data/data/com.jht.chimera/app_web*");
            // remove widevine caches
            //executeAsRoot.addCommand("rm -rf /data/vendor/mediadrm/IDM1013/L3/*"); // now runs as part of netflix package clean up

            //currentDowloadUserId = ""; // should we make sure this is reset whenever we clear the data?
        }
        executeAsRoot.execute(false);


        // NOTE: pm clear will revoke permission so we need to declare it before the app starts
        for (String pack : tpaList) {
            AppInfo app = AppManager.getInstance().getAppFromPackage(pack);
            AppManager.grantPermissions(app);
        }
    }

    public static void killUserWebData() {
        // This used to go with the killing of apps in the single sign on service.  We place it here
        // to avoid the race condition where we pull the app_web* directory out from under the workout summary.
        // Update: Having this in splash creates a race with parker local profiles SSO where data was deleted after being extracted. So we do it when the user changes now.
        // Update: Still need to do this between guest workouts, so put it back on splash for guests
        // Should we make extra sure the web processes are dead before we attempt to delete the data folders?
        KillProcessHelper killProcessHelper = new KillProcessHelper();
        for (String processName : webProcessNames) killProcessHelper.addPackageToKill(processName, false);
        killProcessHelper.kill();
        //Log.d(TAG, "User purge web data");
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("rm -rf /data/data/com.jht.chimera/app_web*"); // could we just use WebStorage.getInstance().deleteAllData() + cookie manager removeAllCookies?
        // delete downloaded files of WeChat
        //executeAsRoot.addCommand("rm -rf /mnt/sdcard/tencent/MicroMsg/Download/*"); // should this be a part of a general downloads purge?
        executeAsRoot.execute();
    }

    private void killDownloads() {
        if (!ProductConfig.isProjectOne()) {
            ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
            executeAsRoot.addCommand("rm -rf /mnt/sdcard/tencent/MicroMsg/Download/*"); // delete downloaded files of WeChat
            executeAsRoot.execute();
        }
    }

    /**
     * Should we save/load user weight using this SSO mechanism?
     *
     * Dan S, regarding PRKS-1494, 2020-09:
     * Oh no, this user weight stored in userinfo.zip (or is it userData.zip?) is
     * (A) duplicated information, (B) sometimes wrong, and
     * (C) clobbering the correct weight value when I switch users!
     *
     * Why do we need to save weight with SSO info at all, since it's already stored with other user info?
     *
     * Brenden C:
     * I didn't want to save the weight here, but had no other real option.
     * I know there is a sync user operation for retail on DAPI.
     * We cannot use that on commercial.  Not everyone is tied to an xID.
     * Web team refused to manage the user info in their database so we had to do it outside of them.
     * Sucks because now your weight won't be the same across GUI and LED.
     * Unfortunately this was the only option I had and we had to have a change weight app per Kolman.
     * Also, I think the user settings is a CBOR implementation and we did not have CBOR on Generations when this was added there.
     *
     * We also don't have a switch user on commercial so you have a use case we don't have.
     * Not sure the best solution for this.
     *
     * Kong Y:
     * Might be easier to just ignore it on Parker then using it.
     * Just use whatever dapi user weight is
     * Well that's my opinion lol
     *
     * Dan:
     * This is all very confusing, and probably has deeper problems and could use refactoring,
     * but for purposes of fixing this bug I will just 'not do the SSO weight syncing on Parker'
     *
     * Doug K (8 Dec 22):
     * changed DATA_USE_SSO_FOR_WEIGHT_BOOL default to false, and update key based on whether or not
     * the dapi user has a weight associated with it.
     * basically use dapi weight if have it, then use sso if no dapi weight (to catch those 3rd party logins
     * that don't have a dapi weight as mentioned above)
     */
    protected boolean shouldSyncUserWeight() {
        return !ProductConfig.isRetail() && DataMatrix.getBoolean(SSOKeys.DATA_USE_SSO_FOR_WEIGHT_BOOL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dataMatrix.finish();
    }

    void init() {
        BksCredentialProvider credentialProvider = new BksCredentialProvider(getApplicationContext());
        String totpSecret = credentialProvider.getString("SSO_TOTP_SECRET");
        singleSignOnInterface = new SingleSignOnInterface(JHTCommonTypes.DAPIEnvironment.values()[DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)], totpSecret);
        mSSOSettings.readSettings(getSettingsJson());
        DataMatrix.putInt(SSOKeys.DATA_SSO_SERVICE_STATE_INT, SSOKeys.States.IDLE); // or do we set idle as default, or do we set unknown as default and set idle when init is done?

        dataMatrix.createIntAdapter(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, integer -> {
            singleSignOnInterface.switchDapiEnvironment(JHTCommonTypes.DAPIEnvironment.values()[integer]);
        });
        dataMatrix.createStringAdapter(DAPIKeys.DATA_USER_DAPI_UUID_STRING, integer -> downloadUserData(), false);
        dataMatrix.createIntAdapter(DapiCreateUser.Keys.EVENT_REGISTER_RESPONSE_INT, integer -> {
            if (!ProductConfig.hasUserProfiles())
                downloadUserData(); // wouldn't register ultimately trigger a UUID set making this redundant?
        }, false);
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, runState -> {
            if (runState == WorkoutRunState.STOP.ordinal()) {
                uploadBetweenWorkouts();
            }
        }, false);
        dataMatrix.createStringAdapter(SSOKeys.ACTION_SSO_UPDATE_APP_STRING, appUuid -> {
            // would be nice to be able to check if this app has been uploaded yet and just do this app?
            uploadBetweenWorkouts();
        }, false);

        dataMatrix.createEventAdapter(SSOKeys.ACTION_SSO_SAVE_USER_CUSTOM_WORKOUT, s -> {
            if (DapiUser.isGuestUser()) return;
            String userUuid = DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING);
            uploadUserExtendedInfo(userUuid);
        });

        // delete any leftover files on boot
        new Thread(() -> {
            FileUtils.delete(new File(SSO_DOWNLOAD_PATH));
            FileUtils.delete(new File(SSO_UPLOAD_PATH));
        }).start();

        // ACTION_LOGOUT might have been called before the service starts so do some cleanup on init
        if (ProductConfig.hasUserProfiles()) {
            killApps(false, true); // don't clear for user profiles
        } else {
            killApps();
        }
    }

    void uploadBetweenWorkouts() {
        /*if (!ProductConfig.supportsFullSso()) {
            //We still want to stop the apps, will remove Facebook and accounts data if Guest
            killApps(DapiUser.isGuestUser(), false);
            return;
        }*/

        uploadUserData();
        // if using local profiles, skip purge/re-download between workouts
        // note if download fires before upload finishes, this thread would clear record of the that download possibly causing sync-in-progress dialogs
        if (!ProductConfig.hasUserProfiles()) {
            new Thread(() -> {
                uploadUserDataComplete.waitForNotification();

                //We still want to stop the apps, will remove Facebook and accounts data if Guest
                killApps(DapiUser.isGuestUser(), false);
                Log.d(TAG, "workout end, upload complete, clearing downloadedAppPackageList");
                currentDowloadUserId = "";
                synchronized(downloadedAppPackageList) {
                    downloadedAppPackageList.clear();
                    DataMatrix.putStringList(SSOKeys.DATA_SSO_DOWNLOADED_APP_PACKAGE_STRING_LIST, new LinkedList<>(downloadedAppPackageList));
                }
                // killUserWebData will be called at splash if user is guest
                killDownloads(); // purge download folders between workouts to conserve space
            }).start();
        } else {
            // for profiles, would it make more sense to stop apps first, then upload since we aren't clearing them?
            new Thread(() -> {
                uploadUserDataComplete.waitForNotification();
                killApps(DapiUser.isGuestUser(), false); // we still want to stop the apps, just don't clear the data (unless it is a guest)
                Log.d(TAG, "workout end, upload complete, stopped apps without clearing");
                // killUserWebData will be called at splash if user is guest
                killDownloads(); // purge download folders between workouts to conserve space
            }).start();
        }
    }

    void downloadUserData() {
        String userId = DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING);
        if (userId.equalsIgnoreCase(currentDowloadUserId)) { // current id starts empty, so this should still allow purging guest at startup
            return;
        }
        if (DapiUser.isGuestUser()) {
            // if we have switched to a guest, always clear since console may have powered down
            // without clearing previous guest info
            Log.d(TAG, "clearing out sso data (guest)");

            killApps(true, true);
            currentDowloadUserId = "";
            downloadedAppPackageList.clear();
        }
        currentDowloadUserId = userId;
        //if (!ProductConfig.supportsFullSso()) return;
        downloadData(userId);
    }

    void uploadUserData() {
        // TODO - should we check that a user has been downloaded/selected yet?

        stopDownload = true;    // should this be AFTER the guest check?

        /*
        String packageList[] = mSSOSettings.getPackageList();
        for (String packageName : packageList) {
            List<String> list = dataMatrix.getStringList(MainAppKeys.LAUNCHED_SSO_APP_PACKAGE_STRING_LIST);
            if (!list.contains(packageName)) {
                if (packageName.equalsIgnoreCase(getPackageName())) {
                    if (!list.contains(WebAppSSOActivity.class.getCanonicalName()))
                        continue;
                } else {
                    continue;
                }
            }
        }
        */
        if (DapiUser.isGuestUser()) {
            uploadUserDataComplete.setNotification();
            return;
        }
        String userUuid = DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING);
        uploadData(userUuid);
    }

    private String getSettingsJson() {
        String jsonSettings = null;
        try {
            InputStream is = getAssets().open("sso_settings.json"); //NON-NLS
            int size = is.available();
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();
            //noinspection CharsetObjectCanBeUsed
            jsonSettings = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.w(TAG, ex);
        }
        return jsonSettings;
    }


    private void downloadData(final String userId) {
        new Thread(() -> {
            try {
                int state = DataMatrix.getInt(SSOKeys.DATA_SSO_SERVICE_STATE_INT);

                if (state != SSOKeys.States.IDLE) {
                    Log.e(TAG, "downloadData started but SSO is not idle! " + state);
                }

                DataMatrix.putInt(SSOKeys.DATA_SSO_SERVICE_STATE_INT, SSOKeys.States.DOWNLOADING);
                stopDownload = false;

                // commercial always transitions to guest between users, so they would already
                // purge, but with profiles, direct user changes are possible
                if (ProductConfig.hasUserProfiles()) {
                    // should NOT assume that incoming zip will overwrite everything, be sure to
                    // clear out data BEFORE zip extraction
                    Log.d(TAG, "clearing out sso data (user change)");
                    killAppsSync(true, true);

                    // this was removed from killAppsSync
                    killUserWebData();
                }

                FileUtils.delete(new File(SSO_DOWNLOAD_PATH));
                //noinspection ResultOfMethodCallIgnored
                new File(SSO_DOWNLOAD_PATH).mkdirs();

                // download user data
                downloadUserExtendedInfo(userId);
                if (shouldSyncUserWeight()) {
                    downloadUserWeight(userId);
                }

                // download sso apk data
                String[] packageList = mSSOSettings.getPackageList();
                if (packageList == null) { // if there is a setting, but app has SSO disabled, skip it
                    Log.w(TAG, "download packageList is null");
                } else if (packageList.length == 0) {
                    Log.w(TAG, "download packageList is empty");
                } else {
                    for (String packageName : packageList) {
                        String versionName = getVersionName(mContext, packageName);
                        Log.d(TAG, "checking package " + packageName + " " + versionName);
                        if (mSSOSettings.isAppSupported(packageName, versionName)) {
                            AppInfo app = AppManager.getInstance().getAppFromPackage(packageName);

                            if (app != null && !app.supportsSSO) {
                                // if there is a setting, but app has SSO disabled, skip it
                                Log.d(TAG, "skipping due to SSO support disabled " + packageName);
                                continue;
                            }
                            String ssoVersion = mSSOSettings.getSsoVersion(packageName, versionName);
                            Log.d(TAG, "ssoVersion " + ssoVersion);
                            if (stopDownload) {
                                return;
                            }
                            Log.d(TAG, "getting data for " + packageName);
                            if (ProductConfig.hasUserProfiles()) {
                                SingleSignOnItem item = new SingleSignOnItem(packageName, userId, ssoVersion);
                                // if user is xid, attempt to get from dapi, otherwise it is local copy or bust
                                if (!DapiUser.isLocalUser(userId)) {
                                    // try for dapi update first
                                    // TODO should we use filename suffix to distinguish inbound
                                    //  xid zip vs current local zip? copy xid zip over local after we have it?

                                    // TODO should we use common download folder as temp and then
                                    //  move to local profile folder? or create temp subfolder for
                                    //  each user?

                                    // TODO should we just allow direct downloading to keep
                                    //  simple? Download already goes to temp file until done then
                                    //  decrypts into final zip?

                                    singleSignOnInterface.getData(item, SingleSignOnService.this, new File(SSO_DOWNLOAD_PATH, packageName + ".zip"));
                                } else {
                                    // try applying local copy
                                    // TODO ? need to manually call onData or create similar function ?
                                    applyUserData(item, true, "", "");
                                }
                            } else {
                                // No local profiles, only cloud copy
                                singleSignOnInterface.getData(new SingleSignOnItem(packageName, userId, ssoVersion), SingleSignOnService.this, new File(SSO_DOWNLOAD_PATH, packageName + ".zip"));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "downloadData THREAD CRASH", e);
            } finally {
                DataMatrix.putInt(SSOKeys.DATA_SSO_SERVICE_STATE_INT, SSOKeys.States.IDLE);
            }
        }).start();
    }

    private void uploadData(final String userId) {
        Log.d(TAG, "uploadData userId=" + userId);
        List<String> appList = DataMatrix.getStringList(MainAppKeys.DATA_LAUNCHED_SSO_APP_PACKAGE_STRING_LIST); // be sure to grab this before it gets cleared out?

        new Thread(() -> {
            try {
                Log.d(TAG, "uploadData THREAD RUN userId=" + userId);
                DataMatrix.putInt(SSOKeys.DATA_SSO_SERVICE_STATE_INT, SSOKeys.States.UPLOADING);

                // make sure all data is written to disk
                ExecuteAsRoot exec = new ExecuteAsRoot();
                exec.addCommand("sync");
                exec.execute();

                FileUtils.delete(new File(SSO_UPLOAD_PATH));
                //noinspection ResultOfMethodCallIgnored
                new File(SSO_UPLOAD_PATH).mkdirs();

                // upload user data
                if (shouldSyncUserWeight()) {
                    updateUserWeight(userId);
                }

                // upload APK app data
                String[] packageList = mSSOSettings.getPackageList();
                if (packageList == null) {
                    Log.w(TAG, "upload packageList is null");
                } else if (packageList.length == 0) {
                    Log.w(TAG, "upload packageList is empty");
                } else {
                    if (appList == null) {
                        return; // in theory, finally should still be called
                    }
                    for (String packageName : packageList) {
                        // check if app was launched
                        if (!appList.contains(packageName)) {
                            continue;
                        }

                        String versionName = getVersionName(mContext, packageName);
                        if (mSSOSettings.isAppSupported(packageName, versionName)) {
                            String ssoVersion = mSSOSettings.getSsoVersion(packageName, versionName);

                            File zipTarget = new File(SSO_UPLOAD_PATH, packageName + ".zip");
                            File zipSourceFiles = new File(SSO_UPLOAD_PATH, packageName);
                            File packageRoot = new File("/data/data/" + packageName);

                            //noinspection ResultOfMethodCallIgnored
                            zipSourceFiles.mkdirs();

                            ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
                            String[] backupFileList = mSSOSettings.getFileList(packageName, versionName);
                            if (backupFileList == null) {
                                continue;
                            }

                            @NonNls String shellCommand="";
                            for (String filepath : backupFileList) {
                                //noinspection ResultOfMethodCallIgnored
                                new File(zipSourceFiles, filepath).getParentFile().mkdirs();
                                if (filepath.startsWith("/")) { // assume this is absolute path
                                    shellCommand = "cp -rf " + filepath + " " + zipSourceFiles.getAbsolutePath() + filepath;
                                    executeAsRoot.addCommand(shellCommand);
                                } else  {
                                    shellCommand = "cp -rf " + packageRoot.getAbsolutePath() + "/" + filepath + " " + zipSourceFiles.getAbsolutePath() + "/" + filepath;
                                    executeAsRoot.addCommand(shellCommand);
                                }
                                Log.d(TAG, "add file: " + shellCommand);
                            }
                            executeAsRoot.addCommand("chmod -R 777 " + SSO_UPLOAD_PATH);
                            executeAsRoot.execute();
                            if (createZipPackage(zipTarget, zipSourceFiles)) { // only store zip if it was created without issues
                                if (ProductConfig.hasUserProfiles()) {
                                    // copy to local first
                                    File localZip = new File(UserProfileManager.getInstance().getUserFolder(), packageName + ".zip");
                                    ExecuteAsRoot.copyFilePreserve(zipTarget.getAbsolutePath(), localZip.getAbsolutePath());
                                    writeLocalZipVersion(localZip, ssoVersion);
                                    if (!DapiUser.isLocalUser(userId)) {
                                        // upload zip if user has xid
                                        singleSignOnInterface.putFile(new SingleSignOnItem(packageName, userId, ssoVersion), zipTarget, SingleSignOnService.this);
                                    }
                                } else {
                                    singleSignOnInterface.putFile(new SingleSignOnItem(packageName, userId, ssoVersion), zipTarget, SingleSignOnService.this);
                                }
                            }
                            FileUtils.delete(zipSourceFiles);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "uploadData THREAD CRASH", e);
            } finally {
                DataMatrix.putInt(SSOKeys.DATA_SSO_SERVICE_STATE_INT, SSOKeys.States.IDLE);
                uploadUserDataComplete.setNotification();
            }

        }).start();
    }

    /**
     * Returns true on success; false on error or zipping an empty folder (logs reason as warning)
     */
    private boolean createZipPackage(File zipFile, File source) {
        // create zip
        ZipParameters parameters = new ZipParameters();
        parameters.setPassword(getZipPassword());
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
        parameters.setEncryptFiles(true);
        ZipFile zip;
        try {
            zip = new ZipFile(zipFile);

            File[] children = source.listFiles();
            if (children != null) {
                for (File file : children) {
                    if (file.isFile()) {
                        Log.d(TAG, "add to zip = " + file.getName());
                        try {
                            zip.addFile(file, parameters);
                        } catch (ZipException e) {
                            Log.e(TAG, "ZIP FILE EXCEPTION", e);
                        }
                    } else if (file.isDirectory()) {
                        Log.d(TAG, "add to zip = " + file.getName());
                        try {
                            zip.addFolder(file, parameters);
                        } catch (ZipException e) {
                            Log.e(TAG, "ZIP DIRECTORY EXCEPTION", e);
                        }
                    }
                }
            } else {
                Log.w(TAG, "Nothing to zip, source= " + source);
                return false; // Should we return false if there is nothing to zip? If there is nothing to zip, should we not upload it?
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to create zip " + zipFile, e);
            return false;
        }
    }


    @Override
    public void onData(SingleSignOnItem singleSignOnItem, boolean success, String data, String md5) {
        Log.d(TAG, "onData success=" + success + ", id=" + singleSignOnItem.appId);
        if (ProductConfig.hasUserProfiles() && success) {
            // copy new zip over old zip ( actually probably better to delete old and rename new )
            // remote copy always wins vs local ?
            File userFolder = UserProfileManager.getInstance().getUserFolder();
            File downloadZip = new File(SSO_DOWNLOAD_PATH, singleSignOnItem.appId + ".zip");
            File localZip = new File(userFolder, singleSignOnItem.appId + ".zip");
            if (downloadZip.exists()) {
                ExecuteAsRoot.copyFilePreserve(downloadZip.getAbsolutePath(), localZip.getAbsolutePath());
                writeLocalZipVersion(localZip, singleSignOnItem.version);
            }
        }
        applyUserData(singleSignOnItem, success, data, md5);
    }

    private void applyUserData(SingleSignOnItem singleSignOnItem, boolean success, String data, String md5) {
        if (stopDownload) {
            Log.d(TAG, "skip applyUserData for stopDownload " + singleSignOnItem.appId);
            return;
        }

        // if app was launched, don't override the data
        List<String> list = DataMatrix.getStringList(MainAppKeys.DATA_LAUNCHED_SSO_APP_PACKAGE_STRING_LIST);
        if (list != null && list.contains(singleSignOnItem.appId)) {
            Log.d(TAG, "skip applyUserData for launched app " + singleSignOnItem.appId);
            return;
        }

        synchronized(downloadedAppPackageList) {
            downloadedAppPackageList.add(singleSignOnItem.appId);
            DataMatrix.putStringList(SSOKeys.DATA_SSO_DOWNLOADED_APP_PACKAGE_STRING_LIST, new LinkedList<>(downloadedAppPackageList));
        }

        File zipTarget;
        if (ProductConfig.hasUserProfiles()) { // if local copy exists and is valid, use it whether or not remote download worked
            zipTarget = new File(UserProfileManager.getInstance().getUserFolder(), singleSignOnItem.appId + ".zip");
            if (zipTarget.exists()) {
                if (checkLocalZipVersion(zipTarget, singleSignOnItem.version)) {
                    extractZip(zipTarget, singleSignOnItem.appId, false);
                    return; // local copy works, bail out
                }
            }
        }
        if (success) {
            zipTarget = new File(SSO_DOWNLOAD_PATH, singleSignOnItem.appId + ".zip");
            if (zipTarget.exists()) {
                extractZip(zipTarget, singleSignOnItem.appId, true);
            } else {
                Log.w(TAG, "extract sso data, cannot find " + zipTarget.getName());
            }
        }
    }

    private boolean checkLocalZipVersion(File zipFile, String expectedSsoVersion) {
        return true; // bypass until we need it as it would force reset all local profile SSO if meta data does not exist
        /*try {
            File ssoVersionFile = new File(zipFile.getAbsolutePath() + ".json");
            UserProfileManager.LocalSsoData data = UserProfileManager.LocalSsoData.read(ssoVersionFile);
            if (data != null) {
                return data.isValid(zipFile, expectedSsoVersion);
            }
        } catch (Exception e) {
            Log.e(TAG, "local sso read", e);
        }
        return false;*/
    }
    private boolean writeLocalZipVersion(File zipFile, String ssoVersion) {
        try {
            String md5 = FileUtils.computeMd5(zipFile);
            Log.d(TAG, "writeLocalZipVersion " + zipFile.getAbsolutePath() + ", " + ssoVersion + ", " + md5);
            UserProfileManager.LocalSsoData data = new UserProfileManager.LocalSsoData();
            data.ssoVersion = ssoVersion;
            data.md5 = md5;
            File ssoVersionFile = new File(zipFile.getAbsolutePath() + ".json");
            return UserProfileManager.LocalSsoData.write(data, ssoVersionFile);
        } catch (Exception e) {
            Log.e(TAG, "local sso write", e);
            return false;
        }
    }

    /*private Thread clearSinglePackage(String packageName) {
        Thread thd = new Thread(() -> {
            KillProcessHelper killProcessHelper = new KillProcessHelper();
            killProcessHelper.addPackagesToClear(packageName, false);
            killProcessHelper.kill();
            ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
            addPackageCleanUpCommands(executeAsRoot, packageName);
            executeAsRoot.execute(false);
        });
        thd.start();
        return thd;
    }*/

    private void addPackageCleanUpCommands(ExecuteAsRoot executeAsRoot, String packageName) {
        String versionName = getVersionName(mContext, packageName);
        if (mSSOSettings.isAppSupported(packageName, versionName)) {
            String[] commands = mSSOSettings.getCleanUpCommands(packageName, versionName);
            if (commands != null) {
                Log.d(TAG, "package=" + packageName + ", cleanUp=" + TextUtils.join(", ", commands));
                executeAsRoot.addCommand(commands);
            }
        }
    }

    @Override
    public void onPutData(SingleSignOnItem singleSignOnItem, boolean success) {
        Log.d(TAG, "onPutData success=" + success + ", id=" + singleSignOnItem.appId);
        //noinspection ResultOfMethodCallIgnored
        new File(SSO_UPLOAD_PATH, singleSignOnItem.appId + ".zip").delete();
    }

    //private void extractZip(final File zipFile, String packageName) { extractZip(zipFile, packageName, true); }
    private void extractZip(final File zipFile, String packageName, boolean deleteZipSrc) {
        Log.d(TAG, "extractZip " + zipFile.getAbsolutePath() + " pack=" + packageName);
        if (!zipFile.canRead()) {
            // Not sure why xid user zips are not accessible on ultra thin? This is silly workaround for now.
            Log.w(TAG, "Adjust zip access " + zipFile.getAbsolutePath());
            ExecuteAsRoot.chmod("666", zipFile.getAbsolutePath());
        }
        final File zipSourceFiles = new File(SSO_UPLOAD_PATH, packageName);
        //noinspection ResultOfMethodCallIgnored
        zipSourceFiles.delete();
        //noinspection ResultOfMethodCallIgnored
        zipSourceFiles.mkdirs();
        File packageRoot = new File("/data/data/" + packageName);

        // extract the files
        try {
            ZipFile zip = new ZipFile(zipFile);
            zip.setPassword(getZipPassword());
            zip.extractAll(zipSourceFiles.getPath());
            if (stopDownload) {
                FileUtils.delete(zipSourceFiles);
                if (deleteZipSrc) {
                    FileUtils.delete(zipFile);
                }
                Log.d(TAG, "extractZip skip for stopDownload " + zipFile);
                return;
            }

            ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
            executeAsRoot.addCommand("id");
            executeAsRoot.addCommand("ls " + zipSourceFiles.getAbsolutePath());
            executeAsRoot.addCommand("cp -vrf " + zipSourceFiles.getAbsolutePath() + "/* " + packageRoot.getAbsolutePath());
            executeAsRoot.addCommand("find /data/data/" + packageName + " -not -name \"lib\" -print0 | xargs -0 chown `ls -l /data/data | grep " + packageName + " | tr -s ' ' | cut -d' ' -f3`");
            executeAsRoot.addCommand("find /data/data/" + packageName + " -not -name \"lib\" -print0 | xargs -0 chgrp `ls -l /data/data | grep " + packageName + " | tr -s ' ' | cut -d' ' -f4`");
            executeAsRoot.addCommand("find /data/data/" + packageName + " -not -name \"lib\" -print0 | xargs -0 restorecon ");
            executeAsRoot.execute();

            String versionName = getVersionName(mContext, packageName);
            String[] backupFileList = mSSOSettings.getFileList(packageName, versionName);
            if (backupFileList != null) {
                for (String filepath : backupFileList) {
                    if (filepath.startsWith("/")) { // assume this is absolute path
                        boolean isFile = !filepath.endsWith("/");
                        String folderPath = isFile ? new File(filepath).getParent() : filepath;
                        executeAsRoot = new ExecuteAsRoot();
                        executeAsRoot.addCommand("mkdir -p " + folderPath);
                        executeAsRoot.addCommand("cp -rf /data/data/" + packageName + filepath + (isFile ? " " : "* ") + filepath);
                        executeAsRoot.addCommand("chmod -R 777 " + filepath);
                        executeAsRoot.addCommand("rm -rf /data/data/" + packageName + filepath + (isFile ? " " : "* "));
                        executeAsRoot.execute();
                    }
                }
            }

            FileUtils.delete(zipSourceFiles);
            if (deleteZipSrc) {
                FileUtils.delete(zipFile);
            }
            FileUtils.delete(new File(zipFile.getAbsoluteFile() + ".tmp"));


        } catch (Exception e) {
            Log.e(TAG, "Unzip exception", e);
        }
    }

    /**
     * Get the package real version
     */
    private String getVersionName(Context context, String packageName) {
        String versionName;
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }

        versionName = packageInfo.versionName;
        if (versionName == null || versionName.isEmpty()) {
            Log.e(TAG, "An error occurred. The versionName is empty for " + packageName);
            return "";
        }
        return versionName;
    }

    private void downloadUserExtendedInfo(final String userId) {
        if (ProductConfig.hasUserProfiles() && DapiUser.isLocalUser(userId)) {
            //This info should be set when profile is selected
            return;
        }

        singleSignOnInterface.getData(new SingleSignOnItem(USER_EXT_INFO_PACKAGE, userId, "1.0"), (item, success, data, md5) -> {
            if (success) {
                DataMatrix.putString(DAPIKeys.ACTION_UPDATE_EXTENDED_USER_INFO_JSON_STRING, data);
            } else {
                // if system has local profiles, just run with the last data; otherwise clear it
                if (!ProductConfig.hasUserProfiles()) {
                    DataMatrix.putString(DAPIKeys.ACTION_UPDATE_EXTENDED_USER_INFO_JSON_STRING, "");
                }
            }
        });
    }

    private void uploadUserExtendedInfo(final String userId) {
        if (ProductConfig.hasUserProfiles() && DapiUser.isLocalUser(userId)) {
            return; // this info should be set when DapiUserManagement changes it
        }

        singleSignOnInterface.putData(new SingleSignOnItem(USER_EXT_INFO_PACKAGE, userId, "1.0"),
                DataMatrix.getString(DAPIKeys.DATA_USER_EXTENDED_DATA_JSON_STRING), (singleSignOnItem, success) -> {
                    //Log.d(TAG, "uploadUserExtendedInfo dapi success=" + success);
                });
    }

    private void downloadUserWeight(final String userId) {
        Log.d(TAG, "downloadUserWeight");
        if (ProductConfig.hasUserProfiles() && DapiUser.isLocalUser(userId)) {
            return; // this info should be set when profile is selected
        }

        final File extendedUserWeightZip = new File(SSO_DOWNLOAD_PATH, "userinfo.zip");
        final File extendedUserExtractZip = new File(SSO_DOWNLOAD_PATH, "userinfo");

        singleSignOnInterface.getData(new SingleSignOnItem(CHANGE_WORKOUT_PACKAGE, userId, CHANGE_WORKOUT_VERSION), (singleSignOnItem, success, data, md5) -> {

            if (stopDownload) {
                return;
            }

            if (success) {
                //noinspection ResultOfMethodCallIgnored
                extendedUserExtractZip.mkdirs();
                ZipFile zip;
                try {
                    zip = new ZipFile(extendedUserWeightZip);
                    zip.setPassword(getZipPassword());
                    zip.extractAll(extendedUserExtractZip.getAbsolutePath());
                    JSONObject jsonObject = (ResourceHelper.loadJSONFromFile(new File(extendedUserExtractZip, "/files/userData.json")));
                    int weightLbs_x10 = jsonObject.getInt("weight");
                    float weightKg = WeightConvertUtil.convertLbsToKg(weightLbs_x10 / 10f);

                    // Don't let an invalid SSO weight overwrite the user's current weight
                    if (weightLbs_x10 > 0) {
                        DataMatrix.putFloat(DAPIKeys.ACTION_UPDATE_USER_WEIGHT_KG_FLOAT, weightKg);
                    }

                } catch (Exception e) {
                    Log.w(TAG, e);
                }
                //noinspection ResultOfMethodCallIgnored
                extendedUserWeightZip.delete();
                FileUtils.delete(extendedUserExtractZip);
                FileUtils.delete(new File(extendedUserWeightZip.getAbsoluteFile() + ".tmp"));
            }
        }, extendedUserWeightZip);
    }

    private void updateUserWeight(final String userId) {
        Log.d(TAG, "updateUserWeight");
        if (ProductConfig.hasUserProfiles() && DapiUser.isLocalUser(userId)) {
            return; // this info should be set when DapiUserManagement changes it
        }

        // Send the weight.
        float weightKg = DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT);
        int weightLbs_x10 = (int) (WeightConvertUtil.convertKilogramsToPounds(weightKg) * 10.0 + 0.5);
        @NonNls String weightJSON = "{\n" +
                "\t\"externalId\": \"" + userId + "\",\n" + //NON-NLS
                "\t\"weight\": " + weightLbs_x10 + "\n" + //NON-NLS
                "}";

        final File f = new File(SSO_UPLOAD_PATH, "files/userData.json");
        final File folder = new File(SSO_UPLOAD_PATH, "files");
        final File fZip = new File(SSO_UPLOAD_PATH, "userData.zip");
        //noinspection ResultOfMethodCallIgnored
        folder.mkdirs();
        try (FileOutputStream fout = new FileOutputStream(f)) {
            fout.write(weightJSON.getBytes());
            ZipParameters parameters = new ZipParameters();
            parameters.setPassword(getZipPassword());
            parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
            parameters.setEncryptFiles(true);
            ZipFile zip;
            zip = new ZipFile(fZip);
            zip.addFolder(folder, parameters);
        } catch (Exception e) {
            Log.w(TAG, e);
            return;
        }

        singleSignOnInterface.putFile(new SingleSignOnItem(CHANGE_WORKOUT_PACKAGE, userId,
                CHANGE_WORKOUT_VERSION), fZip, (singleSignOnItem, success) -> {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
            //noinspection ResultOfMethodCallIgnored
            fZip.delete();
        });
    }

    public static void setupIdFiles() {
        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker)
                || ProductConfig.hardware.isVortexBase()
                || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)                || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)
                || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.UltraThin)) {
            // Vision only uses SSO for data purging. However since this is an OS mod, we'll just do it for anything on parker OS just in case.

            // Netflix requires a common device SN
            ExecuteAsRoot.copyFileIfDifferent("/sdcard/jht/configs/serial.txt", "/data/system/serial.txt");
            ExecuteAsRoot.chmod("777", "/data/system/serial.txt");

            // Specify ANDROID_ID for SSO
            ExecuteAsRoot.copyFileIfDifferent("/sdcard/jht/configs/sso_aid.txt", "/data/system/sso_aid.txt");
            ExecuteAsRoot.chmod("777", "/data/system/sso_aid.txt");

            // Specify list of APKs to not allow to set the FLAG_LAYOUT_INSET_DECOR flag which seems to cause issues with the workout panels
            ExecuteAsRoot.copyFileIfDifferent("/sdcard/jht/configs/inset_decor_list.txt", "/data/system/inset_decor_list.txt");
            ExecuteAsRoot.chmod("777", "/data/system/inset_decor_list.txt");
        }
    }
}
