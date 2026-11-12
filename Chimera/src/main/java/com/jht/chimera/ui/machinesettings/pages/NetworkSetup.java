package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.network.NetworkSetupWidget;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;


/**
 * Implements the network setup page gluing the page to the model layer.
 */
public class NetworkSetup extends SettingPageBase {
    private static final String TAG = "NetworkSetup";

    private NetworkSetupWidget networkSetupWidget;
    private final DataMatrix dataMatrix = new DataMatrix();

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(context).inflate(R.layout.network_setup, null);
        container.addView(view);
        networkSetupWidget = view.findViewById(R.id.network_setup_widget);
        return view;
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        networkSetupWidget.start();
        networkSetupWidget.enableButtons(!DataMatrix.getBoolean(SettingKeys.Experimental.DATA_DISABLE_WIFI_BOOL));
    }


    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        networkSetupWidget.finish();
    }

}
