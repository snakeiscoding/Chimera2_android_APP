package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.ObjectUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

public class TopBarHeartRateIndicator extends AppCompatImageButton {

    private final DataMatrix mDataMatrix = new DataMatrix();

    private boolean hasHeartRate;
    private boolean bleConnected;
    private boolean salutronConnected;
    private boolean myZoneConnected;

    public TopBarHeartRateIndicator(Context context) {
        super(context);
    }

    public TopBarHeartRateIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            mDataMatrix.finish();
            if (visibility == VISIBLE) {
                // show the heart rate icon connected if the heart rate > 0
                mDataMatrix.createIntAdapter(HALKeys.DATA_HEART_RATE_INT, hr -> post(() -> {
                    hasHeartRate = hr > 0;
                    updateIndicator();
                }), false);
                mDataMatrix.createBoolAdapter(HALKeys.DATA_SALUTRON_CONNECTED_BOOL, connected -> post(() -> {
                    salutronConnected = connected;
                    updateIndicator();
                }), false);
                mDataMatrix.createStringAdapter(HALKeys.DATA_BLE_HEART_RATE_ADDRESS_STRING, value -> post(() -> {
                    bleConnected = !ObjectUtil.isNullOrEmpty(value);
                    updateIndicator();
                }), false);
                mDataMatrix.createBoolAdapter(HALKeys.DATA_MY_ZONE_CONNECTED_BOOL, connected -> post(() -> {
                    myZoneConnected = connected;
                    updateIndicator();
                }), false);
                hasHeartRate = DataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT) > 0;
                salutronConnected = DataMatrix.getBoolean(HALKeys.DATA_SALUTRON_CONNECTED_BOOL);
                bleConnected = !ObjectUtil.isNullOrEmpty(DataMatrix.getString(HALKeys.DATA_BLE_HEART_RATE_ADDRESS_STRING));
                myZoneConnected = DataMatrix.getBoolean(HALKeys.DATA_MY_ZONE_CONNECTED_BOOL);
                updateIndicator();
            }
        }
    }

    private boolean hasHeartRate() {
        return hasHeartRate || myZoneConnected || bleConnected || salutronConnected;
    }

    private void updateIndicator() {
        setBackground(AppCompatResources.getDrawable(getContext(), myZoneConnected ? R.drawable.logo_myzone : (hasHeartRate() ? R.drawable.ic_heart_rate_monitor_enabled : R.drawable.ic_heart_rate_monitor_disabled)));
    }

}
