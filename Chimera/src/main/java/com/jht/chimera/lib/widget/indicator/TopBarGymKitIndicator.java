package com.jht.chimera.lib.widget.indicator;

import static com.jht.chimera.lib.common.datamatrixkeys.HALKeys.STATUS_GYM_KIT_CONNECTED;
import static com.jht.chimera.lib.common.datamatrixkeys.HALKeys.STATUS_GYM_KIT_OUT_OF_RANGE;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.mainapp.dialog.GymKitSamsungWatchDialog;
import com.jht.datamatrixlib.DataMatrix;

public class TopBarGymKitIndicator extends AppCompatImageView {

    DataMatrix mDataMatrix = new DataMatrix();
    private GymKitSamsungWatchDialog gymKitDialog = new GymKitSamsungWatchDialog(getContext(), true);

    public TopBarGymKitIndicator(Context context) {
        super(context);
        init();
    }

    public TopBarGymKitIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private JHTTimer jhtTimer = new JHTTimer(10000, () -> {
        if (gymKitDialog != null) {
            gymKitDialog.dismiss();
        }
    });

    public void init() {
        hideStatusIcon();
    }

    void showGymKitConnected() {
        setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.gymkit_connected));
        setVisibility(VISIBLE);
    }

    void showGymKitOutOfRange() {
        if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL)) {
            setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.gymkit_out_of_range));
            setVisibility(VISIBLE);
        }
    }

    void hideStatusIcon() {
        setVisibility(View.GONE);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            super.onWindowVisibilityChanged(visibility);
            if (visibility == VISIBLE) {
                mDataMatrix.finish();
                mDataMatrix.createIntAdapter(HALKeys.DATA_GYM_KIT_STATUS_INT, this::handleGymKitStatus);

                mDataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL, enabled ->
                        handleGymKitStatus(DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT)), false);

            } else {
                mDataMatrix.finish();
            }
        }
    }

    private void handleGymKitStatus(int status) {
        if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL) && ProductConfig.showGymkitUI()) {
            if (status == STATUS_GYM_KIT_CONNECTED) {
                showGymKitConnected();
            } else if (status == STATUS_GYM_KIT_OUT_OF_RANGE) {
                showGymKitOutOfRange();
            } else {
                hideStatusIcon();
            }
        } else {
            hideStatusIcon();
        }
    }
}
