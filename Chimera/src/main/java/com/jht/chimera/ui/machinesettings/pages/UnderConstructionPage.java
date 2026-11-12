package com.jht.chimera.ui.machinesettings.pages;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jht.chimera.R;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;

public class UnderConstructionPage extends SettingPageBase {

    private String message;

    public UnderConstructionPage(String message) {
        this.message = message;
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        View view = LayoutInflater.from(context).inflate(R.layout.under_construction, null);
        container.addView(view);
        ((TextView) view.findViewById(R.id.construction_label)).setText(message);
        return view;
    }


}
