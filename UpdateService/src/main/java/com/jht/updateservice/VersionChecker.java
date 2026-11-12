package com.jht.updateservice;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import updatepackages.Version;

class VersionChecker {
    @NonNls private static final String TAG = "VersionChecker";

    static Version getOsVersion() {
        return new Version(0, Build.TIME / 1000, 0, 0);
    }

    public static Version getPackageVersion(Context context, @NonNls String packageName) {
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e);
            return null;
        }
        if (pInfo.versionName == null) {
            Log.e(TAG, "Error!!! Null version while getting version!");
            return new Version(0, 0, 0, 0);
        }
        else {
            return Version.parseVersion(pInfo.versionName);
        }
    }
}
