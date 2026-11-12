package com.jht.chimera.ui.mainapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.widget.menu.RightMenuBaseLayout;

public class WelcomeScreenSettingsMenu extends RightMenuBaseLayout {
    public WelcomeScreenSettingsMenu(Context context) {
        super(context);
    }

    public WelcomeScreenSettingsMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WelcomeScreenSettingsMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context) {
        findViewById(R.id.wifi_button).setVisibility(VISIBLE);
        findViewById(R.id.wifi_button_icon).setVisibility(VISIBLE);
        findViewById(R.id.audioSlider).setVisibility(INVISIBLE);
        findViewById(R.id.btnMute).setVisibility(INVISIBLE);
        findViewById(R.id.bt_pairing_button).setVisibility(GONE);
        findViewById(R.id.bt_icon).setVisibility(GONE);

        findViewById(R.id.wifi_button).setOnClickListener(v -> LaunchAppUtil.launchWifiSetup());

    }

    @Override
    protected void onCloseHandler() {
        setVisibility(GONE);
    }
}
