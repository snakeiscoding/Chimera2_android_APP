package com.jht.chimera.ui.machinesetup.pages;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jht.chimera.R;
import com.jht.chimera.lib.network.NetworkSetupWidget;
import com.jht.chimera.ui.machinesetup.MachineSetupPageData;

public class NetworkSetupPage extends MachineSetupPageBase {

    private NetworkSetupWidget networkSetupWidget;

    public NetworkSetupPage(final MachineSetupPageData pageData) {
        super(pageData);
    }

    @Override
    public boolean onNext() {
        return true;
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(context).inflate(R.layout.ms_network_setup, null);
        container.addView(view);

        networkSetupWidget = view.findViewById(R.id.network_setup_widget);
        return view;
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        pageData().previousButton.setVisibility(View.VISIBLE);
        pageData().nextButton.setVisibility(View.VISIBLE);
        pageData().title.setText(R.string.network_setup_tc);

        networkSetupWidget.start();
    }


    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        networkSetupWidget.finish();
    }
}
