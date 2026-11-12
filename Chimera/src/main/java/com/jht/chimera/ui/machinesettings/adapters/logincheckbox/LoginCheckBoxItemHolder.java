package com.jht.chimera.ui.machinesettings.adapters.logincheckbox;

import android.view.View;
import android.widget.CheckBox;

import com.jht.androiduiwidgets.selector.IOnItemSelected;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;


/**
 * This class is used map a key test row to the view.
 */
class LoginCheckBoxItemHolder extends SelectorItemHolderBase {

    private View selectorView;
    private CheckBox checkBox;

    /**
     * Cache the views in the holder.
     *
     * @param v The root view.
     */
    LoginCheckBoxItemHolder(View v) {
        super(v);
        selectorView = v;
        checkBox = selectorView.findViewById(R.id.cell_checkbox);
    }


    /**
     * The item has changed.  We need to update the views.
     *
     * @param adapter The adapter managing the views.
     * @param item    The item with the view data.
     */
    @Override
    protected void update(final SelectorAdapterBase adapter, final ISelectorItem item, final IOnItemSelected onItemSelected) {

        /* TODO
        if (item instanceof LoginCheckBoxItem) {
            LoginCheckBoxItem loginItem = (LoginCheckBoxItem) item;
            UserInputManager.manageCheckbox(checkBox, loginItem.field().enabled, null);
            checkBox.setText(LanguageHelper.loadResource(selectorView.getContext(), loginItem.field.loginName()));
        }
        */

    }

}
