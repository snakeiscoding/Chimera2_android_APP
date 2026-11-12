package com.jht.chimera.lib.widget.indicator;

import static com.jht.chimera.lib.common.datamatrixkeys.HALKeys.STATUS_GYM_KIT_DISCONNECTED;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.mainapp.dialog.GymKitSamsungWatchDialog;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gattserver2.WatchState;

public class TopBarGymKitBadge extends AppCompatImageView {

    DataMatrix mDataMatrix = new DataMatrix();
    private GymKitSamsungWatchDialog gymKitDialog = new GymKitSamsungWatchDialog(getContext(), true);

    public TopBarGymKitBadge(Context context) {
        super(context);
        init();
    }

    public TopBarGymKitBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private JHTTimer jhtTimer = new JHTTimer(10000, () -> {
        if(gymKitDialog != null) {
            gymKitDialog.dismiss();
        }
    });

    public void init() {
        if (ProductConfig.showGymkitUI()) {
            setOnClickListener(v -> {
                gymKitDialog.show();
                jhtTimer.start();
            });
        } else { setVisibility(GONE); }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (!isInEditMode()){
            if (visibility == VISIBLE && ProductConfig.showGymkitUI()) {
                mDataMatrix.finish();
                mDataMatrix.createIntAdapter(HALKeys.DATA_GYM_KIT_STATUS_INT, value -> checkBadgeState(), false);
                mDataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL, value -> checkBadgeState(), false);
                mDataMatrix.createIntAdapter(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, status -> checkBadgeState(), true);

            } else {
                mDataMatrix.finish();
            }
        }
    }

    private void checkBadgeState() {
        setVisibility(
            DataMatrix.getBoolean(SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL) &&
            DataMatrix.getInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT) == WatchState.Disconnected.ordinal() &&
            DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT) == STATUS_GYM_KIT_DISCONNECTED ?
                View.VISIBLE : View.GONE);

    }
}
