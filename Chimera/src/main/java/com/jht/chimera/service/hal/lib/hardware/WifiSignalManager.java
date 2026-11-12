package com.jht.chimera.service.hal.lib.hardware;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.common.log.ConsoleEvent;
import com.jht.chimera.lib.common.timer.AbsoluteTimer;
import com.jht.chimera.lib.common.timer.AbsoluteTimerTask;
import com.jht.chimera.lib.widget.network.WifiConfigWrapper;
import com.jht.chimera.lib.widget.network.WifiManagerHelper;
import com.jht.datamatrixlib.DataMatrix;

public class WifiSignalManager {

    final int UPDATE_PERIOD_MS = 1000;
    static volatile AbsoluteTimer mTimer;
    WifiConfigWrapper mWifiConfigWrapper;
    Context mContext;
    private boolean currentInternetStatus;
    private int currentInternetStrength;

    public static WifiSignalManager getInstance() {
        return WifiSignalManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final WifiSignalManager INSTANCE = new WifiSignalManager();
    }

    private WifiSignalManager() {
        mContext = ContextUtil.getApplicationContext();
        mWifiConfigWrapper = new WifiConfigWrapper(mContext);
    }

    public static boolean isEthernetConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_ETHERNET;
    }

    public synchronized void start() {
        stop();
        currentInternetStatus = DataMatrix.getBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL);
        currentInternetStrength = 0;
        mTimer = new AbsoluteTimer("WifiSignalManager");
        mTimer.schedule(new AbsoluteTimerTask() {
            @Override
            public void run() {
                try {
                    TopBarKeys.InternetStatus internetStatus = TopBarKeys.InternetStatus.NO_CONNECTION;
                    if (mWifiConfigWrapper.isWifiConnected()) {
                        int strength = mWifiConfigWrapper.getWifiStrength(4);
                        internetStatus = TopBarKeys.InternetStatus.values()[strength + 1]; // start at index 1
                    }

                    boolean isEthernetConnected = isEthernetConnected(mContext);
                    if (isEthernetConnected) {
                        internetStatus = TopBarKeys.InternetStatus.ETHERNET;
                    }
                    if(currentInternetStrength != internetStatus.ordinal()) {
                        currentInternetStrength = internetStatus.ordinal();
                        DataMatrix.updateInt(TopBarKeys.DATA_WIFI_STRENGTH_ENUM_INT, currentInternetStrength);
                    }
                    boolean internet = !internetStatus.equals(TopBarKeys.InternetStatus.NO_CONNECTION);
                    if (currentInternetStatus != internet) {
                        currentInternetStatus = internet;
                        ConsoleEvent.logEvent("Network " + (internet? "Connected" : "Disconnected") + ", Status: " + internetStatus);

                        //Delay notification of network connection by a couple of seconds to give the OS/internet time to configure.
                        //Without a delay, some API calls were attempting before the network was ready
                        if(internet) {
                            Thread.sleep(2000);
                        }

                        DataMatrix.updateBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL, currentInternetStatus);

                        if (!internet && !DataMatrix.getBoolean(SettingKeys.Experimental.DATA_DISABLE_WIFI_BOOL)) {
                            // The WiFi just went down.  See if we can power it on.
                            new JHTTimer(1000, () -> WifiManagerHelper.TurnWifiOn(mContext)).start();
                        }
                    }
                } catch (Exception e) {
                    Log.e(WifiSignalManager.class.getSimpleName(), "Exception in status timer", e);
                }
            }
        }, 0, UPDATE_PERIOD_MS);
    }

    public synchronized void stop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }
}
