package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.content.res.AppCompatResources;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.datamatrixlib.DataMatrix;

public class TopBarDapiIndicator extends ImageView {

    DataMatrix mDataMatrix = new DataMatrix();

    public TopBarDapiIndicator(Context context) {
        super(context);
        init();
    }

    public TopBarDapiIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.topbar_wifi_noconnector));
        showDapiConnected();
    }

    void showDapiDisconnected() {
        this.setVisibility(View.VISIBLE);
    }

    void showDapiConnected() {
        this.setVisibility(View.GONE);
    }


    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            mDataMatrix.finish();
            mDataMatrix.createBoolAdapter(TopBarKeys.DATA_DAPI_CONNECTED_BOOL, value -> {
                if (value) {
                    showDapiConnected();
                } else {
                    showDapiDisconnected();
                }
            });

        } else {
            mDataMatrix.finish();
        }
    }

}
