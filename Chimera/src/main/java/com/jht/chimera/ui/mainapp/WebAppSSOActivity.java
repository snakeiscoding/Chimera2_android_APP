package com.jht.chimera.ui.mainapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.jht.chimera.lib.common.permission.GrantPermission;

// This class behaves the same as the WebAppActivity but stores its cache data in a different location to support single sign on
public class WebAppSSOActivity extends WebAppActivity {
    private static final String TAG = "WebAppSSOActivity";

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected String getWebViewDataDir() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            GrantPermission.grant(this, getPackageName(), Manifest.permission.ACCESS_COARSE_LOCATION, getApplicationInfo().uid);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            GrantPermission.grant(this, getPackageName(), Manifest.permission.ACCESS_FINE_LOCATION, getApplicationInfo().uid);
        }
    }

    @Override
    protected void requestShim() {
        // do nothing
    }
}
