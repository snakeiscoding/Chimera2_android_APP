package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;

import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gattserver2.WatchState;

public class TopBarSamsungWatchIndicator extends AppCompatImageView {
    private static final String TAG = "TopBarSamsungWatchIn...";
    DataMatrix mDataMatrix = new DataMatrix();

    public TopBarSamsungWatchIndicator(Context context) {
        super(context);
        init();
    }

    public TopBarSamsungWatchIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        hideSamsungIcon();
    }

    void showSamsungConnected() {
        setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.ic_icon_samsung_connected));
        setVisibility(VISIBLE);
    }

    void hideSamsungIcon() {
        setVisibility(GONE);
    }

    void showSamsungConnecting() {
        setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.ic_icon_samsung_connecting));
        setVisibility(VISIBLE);
    }

    void showSamsungTemporarilyDisconnected() {
        setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.ic_icon_samsung_disconnected));
        setVisibility(VISIBLE);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            if (visibility == VISIBLE) {
                if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_SAMSUNG_WATCH_ENABLE_BOOL)) {
                    mDataMatrix.finish();
                    mDataMatrix.createIntAdapter(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, status -> {
                        Log.d(TAG, "samsungState: CHANGED CONNECTION STATE TO: " + status + " in TopBarSamsungWatchIndicator");
                        if (status == WatchState.Connected.ordinal()) {
                            showSamsungConnected();
                        } else if (status == WatchState.Connecting.ordinal()) {
                            showSamsungConnecting();
                        } else if (status == WatchState.TemporarilyDisconnected.ordinal()) {
                            showSamsungTemporarilyDisconnected();
                        } else {
                            hideSamsungIcon();
                        }
                    });
                } else {
                    hideSamsungIcon();
                }

            } else {
                mDataMatrix.finish();
            }
        }
    }

}
