package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;

import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

public class TopBarSmartDeviceIndicator extends AppCompatImageButton {

    private final DataMatrix mDataMatrix = new DataMatrix();
    private boolean sinkConnected = false;

    public TopBarSmartDeviceIndicator(Context context) {
        super(context);
        init();
    }

    public TopBarSmartDeviceIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        updateIndicator(sinkConnected);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            if (!BuildConfig.HAS_BT_SINK) {
                setVisibility(GONE);
                return;
            }

            if (visibility == VISIBLE) {
                mDataMatrix.finish();
                mDataMatrix.createBoolAdapter(HALKeys.DATA_BT_SINK_CONNECTED_BOOL, value -> {
                    if (sinkConnected != value) {
                        sinkConnected = value;
                        updateIndicator(value);
                    }
                });

            } else {
                mDataMatrix.finish();
            }
        }
    }

    private void updateIndicator(boolean sinkConnected) {
        if (BuildConfig.HAS_BT_SINK && BuildConfig.MEDIA_PLAYER_SCANNER) {
            if (sinkConnected) {
                setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.ic_smart_device_enabled));
            } else {
                setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.ic_smart_device_disabled));
            }
        } else {
            setVisibility(View.GONE);
        }
    }

}
