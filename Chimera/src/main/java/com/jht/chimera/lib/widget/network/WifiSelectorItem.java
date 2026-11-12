package com.jht.chimera.lib.widget.network;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.jht.androiduiwidgets.selector.StandardSelectorItem;
import com.jht.chimera.R;


/**
 * This is used to create a list of networks to connect to over WiFi.
 */
public class WifiSelectorItem extends StandardSelectorItem {

    private SSID ssid;

    private final Drawable[] signalStrengthDrawable;
    private final Drawable wifiSelected;
    private final Context context;


    /**
     * Initialize the item passing the network information.
     *
     * @param ssid      The network information fo the item.
     */
    WifiSelectorItem(Context context, @NonNull SSID ssid) {
        super("", null, null, null);
        update(ssid);

        this.context = context;

        signalStrengthDrawable = new Drawable[4];
        signalStrengthDrawable[0] = ContextCompat.getDrawable(context, R.drawable.icon_wifi_menu_0);
        signalStrengthDrawable[1] = ContextCompat.getDrawable(context, R.drawable.icon_wifi_menu_1);
        signalStrengthDrawable[2] = ContextCompat.getDrawable(context, R.drawable.icon_wifi_menu_2);
        signalStrengthDrawable[3] = ContextCompat.getDrawable(context, R.drawable.icon_wifi_menu_3);
        wifiSelected = ContextCompat.getDrawable(context, R.drawable.icon_wifi_selected);
    }



    @Override
    public Drawable postImage() {
        if (isOther()) {
            return null;
        }
        if (selected()) {
            return wifiSelected;
        } else {
            int level = WifiManager.calculateSignalLevel(ssid.scanResult.level, 4);
            if (level > 3 || level < 0) {
                return signalStrengthDrawable[0];
            }
            return signalStrengthDrawable[level];
        }
    }

    private boolean isOther() {
        return ssid == null || ssid.scanResult == null;
    }

    @Override
    public boolean showPostImage() {
        return !isOther() && !showBusy();
    }

    @Override
    public boolean showBusy() {
        return ssid.state == SSID.SSIDState.CONNECTING;
    }

    @Override
    public boolean animateBusy() {
        return ssid.state == SSID.SSIDState.CONNECTING;
    }

    /**
     * Used to update the network info on subsequent scans.
     *
     * @param ssid      The network info.
     */
    public void update(@NonNull SSID ssid) {
        this.ssid = ssid;
    }

    /**
     *
     * @return  The network info.
     */
    SSID ssid() { return ssid; }


    /**
     *
     */
    void ssid(SSID ssid) { this.ssid = ssid; }


    /**
     * Used to get the item's label.
     * @return      Return the label
     */
    @NonNull
    @Override
    public String toString() {
        if(isOther()) { return context.getString(R.string.other_network_tc); }
        return ssid.scanResult.SSID;
    }


}
