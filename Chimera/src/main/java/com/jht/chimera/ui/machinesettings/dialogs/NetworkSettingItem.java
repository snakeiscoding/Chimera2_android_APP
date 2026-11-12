package com.jht.chimera.ui.machinesettings.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;

public class NetworkSettingItem extends SelectorItemBase {

    public String title;
    String value1;
    String value2;

    public NetworkSettingItem(String title, String value1, String value2) {
        super(null);
        this.title = title;
        this.value1 = value1;
        this.value2 = value2;
    }


    /**
     * Used to get the item's label.
     *
     * @return Return the label
     */
    @NonNull
    @Override
    public String toString() {
        return title;
    }


    @Override
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {

        View v = LayoutInflater.from(context).inflate(R.layout.network_setting_item, parent, false);
        return new NetworkSettingItemHolder(v);
    }
}
