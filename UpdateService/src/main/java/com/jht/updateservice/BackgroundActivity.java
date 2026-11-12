package com.jht.updateservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import org.jetbrains.annotations.NonNls;

/**
 * Created to be a background activity to prevent other apps and services from crashing or starting
 * in the background. Also prevents the starting of sleep mode.
 * <p>
 * Created by chris.foyer on 5/1/2017.
 */

public class BackgroundActivity extends Activity {
    @NonNls public static final String BROADCAST_ACTIVITY_RESUME = "com.jht.broadcast_activity_resume";
    @NonNls public static final String DATA_PACKAGE_NAME = "data_package_name";
    @NonNls public static final String DATA_CLASS_NAME = "data_class_name";
    private BroadcastReceiver closeReceiver;

    @NonNls public static final String CLOSE_ACTION = "close_update_background_activity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.background_layout);

        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(CLOSE_ACTION)) finish();
            }
        };

        IntentFilter filter = new IntentFilter(CLOSE_ACTION);
        registerReceiver(closeReceiver, filter);
    }

    @Override
    public void onDestroy() {
        if (closeReceiver != null) {
            unregisterReceiver(closeReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        // send broadcast with activity info to indicate the current active activity
        Intent intent = new Intent(BROADCAST_ACTIVITY_RESUME);
        intent.putExtra(DATA_PACKAGE_NAME, getClass().getPackage().getName());
        intent.putExtra(DATA_CLASS_NAME, getClass().getSimpleName());
        sendBroadcast(intent);
        super.onResume();
    }
}
