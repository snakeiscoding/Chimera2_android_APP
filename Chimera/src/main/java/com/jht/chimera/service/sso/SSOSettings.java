package com.jht.chimera.service.sso;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

// this class will contains the version of the app that sso supports and which files to backup
//
public class SSOSettings {
    static private final String TAG = "SSOSettings";

    // list of packages that will use sso
    private Map<String, PackageVersionInfo> mPackageInfoMap;

    // this class will store the package version mapping for different compatible versions
    //
    private class PackageVersionInfo {
        private Map<String, String> mVersionMap;
        private Map<String, String[]> mFilesMap;
        private Map<String, String[]> mCleanUp;

        PackageVersionInfo() {
            mVersionMap = new HashMap<>();
            mFilesMap = new HashMap<>();
            mCleanUp = null;
        }

        public void addVersion(String realVersion, String ssoVersion) {
            mVersionMap.put(realVersion, ssoVersion);
        }

        public String getSsoVersion(String realVersion) {
            return mVersionMap.get(realVersion);
        }

        public void addFileList(String realVersion, String[] fileList) {
            mFilesMap.put(realVersion, fileList);
        }

        public void addCleanUpCommands(String realVersion, String[] commands) {
            if (commands != null && commands.length > 0) {
                if (mCleanUp == null) mCleanUp = new HashMap<>();
                mCleanUp.put(realVersion, commands);
            }
        }

        public String[] getFileList(String realVersion) {
            return mFilesMap.get(realVersion);
        }

        public String[] getCleanUpCommands(String realVersion) {
            return (mCleanUp == null ? null : mCleanUp.get(realVersion));
        }
    }

    // singleton constructor
    //
    private static SSOSettings mInstance = null;

    private SSOSettings() {
    }

    // get the singleton instance
    //
    public static SSOSettings getInstance() {
        if (mInstance == null) {
            mInstance = new SSOSettings();
        }
        return mInstance;
    }

    // parse in the package version json
    //
    public void readSettings(String json) {

        mPackageInfoMap = new HashMap<>();
        try {
            JSONObject root = new JSONObject(json);

            // parse array of packages
            JSONArray packageArray = root.getJSONArray("sso_settings");
            for (int i = 0; i < packageArray.length(); i++) {
                JSONObject packageObject = packageArray.getJSONObject(i);
                if (packageObject.optBoolean("skip", false)) continue; // if we need to temporarily disable an entry
                String packageName = packageObject.getString("packageName");
                PackageVersionInfo packageVersionInfo = new PackageVersionInfo();
                mPackageInfoMap.put(packageName, packageVersionInfo);

                // parse array of version info
                JSONArray versionArray = packageObject.getJSONArray("packageInfo");
                for (int j = 0; j < versionArray.length(); j++) {
                    JSONObject versionObject = versionArray.getJSONObject(j);
                    String realVersion = versionObject.getString("packageVersion");
                    String ssoVersion = versionObject.getString("ssoVersion");
                    packageVersionInfo.addVersion(realVersion, ssoVersion);

                    // parse list of files to backup
                    JSONArray filesArray = versionObject.getJSONArray("files");
                    Log.i(TAG, "files length: " + filesArray.length() + " for " + packageName);

                    if (filesArray.length() > 0) {
                        String[] fileList = new String[filesArray.length()];
                        for (int k = 0; k < filesArray.length(); k++) {
                            fileList[k] = filesArray.getString(k);
                        }
                        packageVersionInfo.addFileList(realVersion, fileList);
                    }

                    // parse list of extra clean up commands
                    JSONArray cleanUpArray = versionObject.optJSONArray("cleanUp");
                    if (cleanUpArray != null) {
                        Log.i(TAG, "cleanUp length: " + cleanUpArray.length() + " for " + packageName);

                        if (cleanUpArray.length() > 0) {
                            String[] commands = new String[cleanUpArray.length()];
                            for (int k = 0; k < cleanUpArray.length(); k++) {
                                commands[k] = cleanUpArray.getString(k);
                            }
                            packageVersionInfo.addCleanUpCommands(realVersion, commands);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "An error occured trying to parse in the json file: " + json, e);
        }

    }

    // return if the current app version is supported by sso
    //
    public boolean isAppSupported(String packageName, String versionName) {
        if (getSsoVersion(packageName, versionName) != null) {

            return true;
        }
        return false;
    }

    // get the sso version
    //
    public String getSsoVersion(String packageName, String versionName) {

        PackageVersionInfo packageVersionInfo = mPackageInfoMap.get(packageName);
        if (packageVersionInfo != null) {
            // get the sso version for the real app version
            String ssoVersion = packageVersionInfo.getSsoVersion(versionName);
            if (ssoVersion != null && !ssoVersion.isEmpty()) {
                return ssoVersion;
            }

            // support * for all version
            String starVersion = packageVersionInfo.getSsoVersion("*");
            if (starVersion != null && !starVersion.isEmpty()) {
                return starVersion;
            }
        }

        return null;
    }

    // get the file list to backup if it exits
    //
    public String[] getFileList(String packageName, String versionName) {

        PackageVersionInfo packageVersionInfo = mPackageInfoMap.get(packageName);
        if (packageVersionInfo != null) {
            // get the file list for the specific version
            String[] fileList = packageVersionInfo.getFileList(versionName);
            if (fileList != null) {
                return fileList;
            }

            // support * for all version
            String[] starFileList = packageVersionInfo.getFileList("*");
            if (starFileList != null) {
                return starFileList;
            }
        }

        return null;
    }

    public String[] getCleanUpCommands(String packageName, String versionName) {

        PackageVersionInfo packageVersionInfo = mPackageInfoMap.get(packageName);
        if (packageVersionInfo != null) {
            // get the file list for the specific version
            String[] fileList = packageVersionInfo.getCleanUpCommands(versionName);
            if (fileList != null) {
                return fileList;
            }

            // support * for all version
            String[] starFileList = packageVersionInfo.getCleanUpCommands("*");
            if (starFileList != null) {
                return starFileList;
            }
        }

        return null;
    }

    // get the list of supported packages
    //
    public String[] getPackageList() {
        String[] packageList = new String[mPackageInfoMap.size()];
        mPackageInfoMap.keySet().toArray(packageList);
        return packageList;
    }
}
