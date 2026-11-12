package com.jht.chimera.service;

import static com.jht.chimera.lib.common.string.StringUtil.quote;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.jht.chimera.lib.common.ObjectUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.datamatrixlib.DataMatrix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class IPGeolocationService extends Service
{
    static final String TAG = "IPGeolocationService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Keep
    static class LocationInfo {
        @SerializedName("query") String ipAddress; //NON-NLS
        @SerializedName("countryCode") String countryCode; //NON-NLS

        @Override
        public String toString() {
            return "LocationInfo { ipAddress: "+quote(ipAddress)+", countryCode: "+quote(countryCode)+" }";
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof LocationInfo)) return false;
            LocationInfo other = (LocationInfo) obj;
            return ObjectUtil.equals(this.ipAddress, other.ipAddress) && ObjectUtil.equals(this.countryCode, other.countryCode);
        }

        @Override
        public int hashCode() {
            return 1337 + ObjectUtil.hash(this.ipAddress, this.countryCode);
        }
    }

    DataMatrix mDataMatrix = new DataMatrix();

    URL getCheckUrl() throws MalformedURLException {
        String ipOverride = DataMatrix.getString(SettingKeys.DATA_IP_LOCATION_IP_OVERRIDE_STRING, "");
        return new URL("https://location-ip.jfit.co/"+(ipOverride.isEmpty() ? "" : "?ip="+ipOverride)); //NON-NLS
    }

    static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream into = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int z;
        while ((z = is.read(buffer, 0, buffer.length)) > 0) {
            into.write(buffer, 0, z);
        }
        return into.toByteArray();
    }

    public static LocationInfo parseLocationInfo(String locationServiceResponse) {
        return new GsonBuilder().setLenient().create().fromJson(locationServiceResponse, LocationInfo.class);
    }

    class CheckerThread extends Thread {
        boolean updatesPending = false;
        boolean lastUpdateSuccessful = false;

        /** Milliseconds between retries after unsuccessful update attempts */
        static final long RETRY_INTERVAL = 30*1000;

        void update() throws IOException {
            ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null || cm.getActiveNetwork() == null) {
                // Update is 'successful' in the sense that there's no point retrying
                // until conditions change.
                Log.d(TAG, "No network; won't try to update IP-based geolocation");
                lastUpdateSuccessful = true;
                return;
            }

            URL checkUrl = getCheckUrl();

            lastUpdateSuccessful = false;
            Log.d(TAG, "Checking with "+checkUrl+"...");
            HttpURLConnection conn = (HttpURLConnection)checkUrl.openConnection();
            try {
                conn.connect();
                byte[] responseData = readFully(conn.getInputStream());
                String responseString = new String(responseData, "utf-8");
                Log.d(TAG, "Read "+responseData.length+" bytes: "+responseString);
                LocationInfo li = parseLocationInfo(responseString);
                Log.d(TAG, "Location info: "+li);
                DataMatrix.putString(SettingKeys.DATA_IP_LOCATION_COUNTRY_CODE_STRING, li.countryCode);
                lastUpdateSuccessful = li.countryCode != null && !li.countryCode.isEmpty();
            } catch (Exception e) {
                Log.e(TAG, "Update exception ", e);
            } finally {
                conn.disconnect();
            }
        }

        @Override public void run() {
            try {
                while (!isInterrupted()) {
                    synchronized (this) {
                        if (!updatesPending) {
                            // To wait(n), zero means infinity.  >_>
                            wait(lastUpdateSuccessful ? 0 : RETRY_INTERVAL);
                            // Assuming for now that updatesPending=true if wait quit early
                            // to avoid unnecessary loops and timing arithmetic
                        }
                        updatesPending = false;
                    }

                    try {
                        update();
                    } catch (IOException e) {
                        Log.e(TAG, "IP geolocation update error", e);
                    }
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Checker thread interrupted; quitting");
                return;
            }
        }

        public synchronized void requestUpdate() {
            updatesPending = true;
            notifyAll();
        }

        /*
        public synchronized void requestUpdateIfLastUpdateUnsuccessful() {
            if (lastUpdateSuccessful) return;
            requestUpdate();
        }
         */
    }

    CheckerThread checkerThread = new CheckerThread();

    BroadcastReceiver connectivityChangeListener =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            checkerThread.requestUpdate();
        }
    };

    Handler handler = new Handler(Looper.myLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()...");
        checkerThread.start();

        mDataMatrix.createStringAdapter(SettingKeys.DATA_IP_LOCATION_IP_OVERRIDE_STRING, ip -> {
            Log.d(TAG, SettingKeys.DATA_IP_LOCATION_IP_OVERRIDE_STRING +" updated to "+ StringUtil.quote(ip));
            checkerThread.requestUpdate();
        }, true);

        IntentFilter connectivityFilter = new IntentFilter();
        connectivityFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityChangeListener, connectivityFilter, null, handler);

        checkerThread.requestUpdate();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(connectivityChangeListener);
        mDataMatrix.finish();
        checkerThread.interrupt();
        super.onDestroy();
    }
}
