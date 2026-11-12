package com.jht.chimera.ui.machinesettings.adapters.logincheckbox;


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
public class LoginCheckBoxItem extends SelectorItemBase {


    Login field;


    /**
     * Default constructor
     */
    public LoginCheckBoxItem(@NonNull Login field) {
        this.field = field;
    }


    public Login field() {
        return field;
    }

    @Override
    @NonNull
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {

        View v = LayoutInflater.from(context).inflate(R.layout.login_checkbox, parent, false);
        return new LoginCheckBoxItemHolder(v);
    }
}
