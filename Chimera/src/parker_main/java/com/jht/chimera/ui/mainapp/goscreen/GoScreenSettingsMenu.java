package com.jht.chimera.ui.mainapp.goscreen;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.widget.menu.RightMenuBaseLayout;

public class GoScreenSettingsMenu extends RightMenuBaseLayout {
    public GoScreenSettingsMenu(Context context) {
        super(context);
    }

    public GoScreenSettingsMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GoScreenSettingsMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context) {
        findViewById(R.id.wifi_button).setVisibility(VISIBLE);
        findViewById(R.id.wifi_button_icon).setVisibility(VISIBLE);
        findViewById(R.id.machine_setting_button).setVisibility(VISIBLE);
        findViewById(R.id.machine_setting_button_icon).setVisibility(VISIBLE);

        findViewById(R.id.wifi_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LaunchAppUtil.launchWifiSetup();
            }
        });
        findViewById(R.id.machine_setting_button).setOnClickListener(v -> LaunchAppUtil.launchSystemSetup());
    }

    @Override
    protected void onCloseHandler() {
        setVisibility(GONE);
    }
}
