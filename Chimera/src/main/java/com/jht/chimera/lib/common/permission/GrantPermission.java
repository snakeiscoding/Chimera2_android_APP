package com.jht.chimera.lib.common.permission;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NonNls;

import java.lang.reflect.Method;

public class GrantPermission {
    @NonNls private static final String TAG = "GrantPermission";

    private GrantPermission() {
        // prevent instantiation
    }

    public static void checkGrantSelfPermission(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            grant(context, context.getPackageName(), permission, context.getApplicationInfo().uid);
        }
    }

    public static void grant(Context context, String packageName, String permission, int uid) {

        try {
            Log.i(TAG, "Try grant: " + packageName + " | " + permission + " | " + uid);
            Method methodGrantRuntimePermission = PackageManager.class.getDeclaredMethod("grantRuntimePermission", String.class, String.class, UserHandle.class);
            PackageManager pm = context.getPackageManager();
            methodGrantRuntimePermission.invoke(pm, packageName, permission, UserHandle.getUserHandleForUid(uid));
        } catch (Exception e) {
            // log error as info which will show in debug build since permission granting shouldn't fail
            Log.i(TAG, "Error grant: " + packageName + " | " + permission + " | " + uid);
        }

    }

    public static void grant(Context context, String packageName, String permission) {

        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
            grant(context, packageName, permission, applicationInfo.uid);
        } catch (Exception e) {
            // log error as info which will show in debug build since permission granting shouldn't fail
            Log.i(TAG, "Error grant: " + packageName + " | " + permission);
        }
    }

}
