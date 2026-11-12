package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.content.res.AppCompatResources;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys.InternetStatus;
import com.jht.datamatrixlib.DataMatrix;


public class TopBarWifiIndicator extends ImageView {
    private final String TAG = "TopBarWifiIndicator"; // NON-NLS
    DataMatrix mDataMatrix = new DataMatrix();
    InternetStatus status = InternetStatus.NO_CONNECTION;

    public TopBarWifiIndicator(Context context) {
        super(context);
        init();
    }

    public TopBarWifiIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        setWifiStrength(status);
    }

    void setWifiStrength(InternetStatus strength) {
        switch (strength) {
            case NO_CONNECTION:
                setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.topbar_wifi_noconnector));
                break;
            case STRENGTH_25:
                setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.topbar_wifi_25));
                break;
            case STRENGTH_50:
                setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.topbar_wifi_50));
                break;
            case STRENGTH_75:
                setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.topbar_wifi_75));
                break;
            case STRENGTH_100:
                setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.topbar_wifi_100));
                break;
            case ETHERNET:
                setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.topbar_ethernet));
                break;
        }
    }


    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            if (visibility == VISIBLE) {
                mDataMatrix.finish();
                mDataMatrix.createIntAdapter(TopBarKeys.DATA_WIFI_STRENGTH_ENUM_INT, value -> {
                    try {
                        InternetStatus strength = InternetStatus.values()[value];
                        if (status != strength) {
                            status = strength;
                            setWifiStrength(strength);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, e);
                    }
                });

            } else {
                mDataMatrix.finish();
            }

        }
    }

}
