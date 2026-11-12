package com.jht.chimera.ui.machinesettings.adapters.update;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;


/**
 * This contains the data needed for a row in the key test table.
 */
public class UpdatePackageSelectorItem extends SelectorItemBase {

    private String text;

    /**
     * Default constructor
     */
    public UpdatePackageSelectorItem(String text) {
        this.text = text;
    }


    /**
     * Used to get the item's label.
     *
     * @return Return the label
     */
    @Override
    public String toString() {
        return text.toString();
    }


    @Override
    @NonNull
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {

        View v = LayoutInflater.from(context).inflate(R.layout.update_package_row, parent, false);
        return new UpdatePackageSelectorItemHolder(v);
    }
}
