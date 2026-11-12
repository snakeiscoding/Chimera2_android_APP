package com.jht.chimera.ui.machinesettings.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;

public class URLSelectorItem extends SelectorItemBase {

    public String url;
    public boolean readOnly;

    public URLSelectorItem(String url, boolean readOnly) {
        super(null);
        this.url = url;
        this.readOnly = readOnly;
    }


    /**
     * Used to get the item's label.
     *
     * @return Return the label
     */
    @NonNull
    @Override
    public String toString() {
        return url;
    }


    @Override
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {

        View v = LayoutInflater.from(context).inflate(R.layout.url_list_item, parent, false);
        return new URLSelectorItemHolder(v);
    }
}
