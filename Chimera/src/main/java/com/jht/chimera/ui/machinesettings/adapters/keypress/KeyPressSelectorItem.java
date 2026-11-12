package com.jht.chimera.ui.machinesettings.adapters.keypress;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.keypress.KeyPress;


/**
 * This contains the data needed for a row in the key test table.
 */
public class KeyPressSelectorItem extends SelectorItemBase {

    private KeyPress.KeyCode keyCode;
    private KeyPress.KeyState keyState;


    /**
     * Default constructor
     */
    public KeyPressSelectorItem(KeyPress.KeyCode keyCode, KeyPress.KeyState keyState) {
        this.keyCode = keyCode;
        this.keyState = keyState;
    }


    /**
     * Used to get the item's label.
     *
     * @return Return the label
     */
    @Override
    public String toString() {
        return keyCode.toString();
    }

    public KeyPress.KeyCode keyCode() {
        return keyCode;
    }

    public KeyPress.KeyState keyState() {
        return keyState;
    }

    @Override
    @NonNull
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {

        View v = LayoutInflater.from(context).inflate(R.layout.key_row, parent, false);
        return new KeyPressSelectorItemHolder(v);
    }
}
