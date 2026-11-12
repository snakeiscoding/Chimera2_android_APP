package com.jht.chimera.lib.widget.indicator;

import static com.jht.chimera.lib.common.datamatrixkeys.HALKeys.STATUS_GYM_KIT_DISCONNECTED;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.mainapp.dialog.GymKitSamsungWatchDialog;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gattserver2.WatchState;

public class TopBarSamsungWatchBadge extends AppCompatImageView {

    DataMatrix mDataMatrix = new DataMatrix();
    private GymKitSamsungWatchDialog watchDialog = new GymKitSamsungWatchDialog(getContext(), false);

    public TopBarSamsungWatchBadge(Context context) {
        super(context);
        init();
    }

    public TopBarSamsungWatchBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private JHTTimer jhtTimer = new JHTTimer(10000, () -> {
        if (watchDialog != null) {
            watchDialog.dismiss();
        }
    });

    public void init() {
        if(!isInEditMode()) {
            if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_SAMSUNG_WATCH_ENABLE_BOOL)) {
                setOnClickListener(v -> {
                    watchDialog.show();
                    jhtTimer.start();
                });
            } else {
                setVisibility(GONE);
            }
        }
    }

    void hideSamsungBadge() {
        setVisibility(View.GONE);
    }

    void showSamsungBadge() {
        setVisibility(View.VISIBLE);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if(!isInEditMode()) {
            if (visibility == VISIBLE && DataMatrix.getBoolean(SettingKeys.Hardware.DATA_SAMSUNG_WATCH_ENABLE_BOOL)) {
                mDataMatrix.finish();
                mDataMatrix.createIntAdapter(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, status -> {
                    checkBadgeVisibility();
                }, false);

                mDataMatrix.createIntAdapter(HALKeys.DATA_GYM_KIT_STATUS_INT, value -> checkBadgeVisibility(), true);
            }
        }
    }

    private void checkBadgeVisibility() {
        int watchStatus = DataMatrix.getInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Disconnected.ordinal());
        if(
            //Check if GymKit is not disconnected. GymKit can be connected or temporarily disconnected
            DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT) != STATUS_GYM_KIT_DISCONNECTED ||
            watchStatus == WatchState.Connected.ordinal() ||
            watchStatus == WatchState.Connecting.ordinal() ||
            watchStatus == WatchState.TemporarilyDisconnected.ordinal()
        ) {
            hideSamsungBadge();
        }
        else {
            showSamsungBadge();
        }
    }
}
