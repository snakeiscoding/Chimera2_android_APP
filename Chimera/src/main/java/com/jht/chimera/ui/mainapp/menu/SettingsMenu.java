package com.jht.chimera.ui.mainapp.menu;

import android.content.Context;
import android.util.AttributeSet;

import com.jht.chimera.lib.widget.menu.RightMenuBaseLayout;

public class SettingsMenu extends RightMenuBaseLayout {
    public SettingsMenu(Context context) {
        super(context);
    }

    public SettingsMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SettingsMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context) {
//        mSettingMenuGroup.setVisibility(VISIBLE);
    }

    @Override
    protected void onCloseHandler() {
        setVisibility(GONE);
    }
}
