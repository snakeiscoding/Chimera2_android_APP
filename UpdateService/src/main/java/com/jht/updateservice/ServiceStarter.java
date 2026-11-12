package com.jht.updateservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.jetbrains.annotations.NonNls;


public class ServiceStarter extends BroadcastReceiver {

    @NonNls private static final String TAG = "UpdateServiceStarter";

    public static boolean shouldStartUpdateServiceAsForeground() {
        // NOTE: This is duplicated between chimera ServiceStarter and UpdateService ServiceStarter
        // Disabling foreground service because it sometimes throws java.lang.SecurityException: Permission Denial: startForeground from pid=3312, uid=10069 requires android.permission.FOREGROUND_SERVICE
        return BuildConfig.FLAVOR_platform.equals("chimera") || BuildConfig.FLAVOR_platform.equals("lcgui");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Action = " + intent.getAction());
        Intent serviceIntent = new Intent(context, UpdateService.class);
        if (shouldStartUpdateServiceAsForeground()) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

    }
}