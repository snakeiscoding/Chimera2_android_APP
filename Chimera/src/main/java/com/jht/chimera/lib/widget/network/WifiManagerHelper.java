package com.jht.chimera.lib.widget.network;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.permission.GrantPermission;

public class WifiManagerHelper {

    public static void TurnWifiOn(Context context) {

        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            GrantPermission.grant(context, context.getPackageName(), Manifest.permission.ACCESS_FINE_LOCATION, context.getApplicationInfo().uid);
        }
        if(wm != null) {
            Log.d("WifiManagerHelper", "ENABLING WIFI");
            wm.setWifiEnabled(true);
        }
        if (Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) == Settings.Secure.LOCATION_MODE_OFF) {
            AndroidSetting.putInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
        }
    }
}
