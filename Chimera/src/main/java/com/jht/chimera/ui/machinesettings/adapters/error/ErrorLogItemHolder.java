package com.jht.chimera.ui.machinesettings.adapters.error;

import android.annotation.SuppressLint;

import com.jht.androiduiwidgets.selector.IOnItemSelected;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;
import com.jht.chimera.ui.machinesettings.pages.error.ErrorLogRow;


/**
 * This class is used to bind an error log item to a view.
 */
class ErrorLogItemHolder extends SelectorItemHolderBase {

    private final ErrorLogRow selectorView;

    /**
     * Cache the views in the holder.
     *
     * @param v The root view.
     */
    ErrorLogItemHolder(ErrorLogRow v) {
        super(v);
        selectorView = v;


    }


    /**
     * The item has changed.  We need to update the views.
     *
     * @param adapter The adapter managing the views.
     * @param item    The item with the view data.
     */
    @SuppressLint("SetTextI18n")
    @Override
    protected void update(final SelectorAdapterBase adapter, final ISelectorItem item, final IOnItemSelected onItemSelected) {

        if (item instanceof ErrorLogItem) {
            selectorView.setBackgroundColor(selectorView.getContext().getResources().getColor(item.getIndex() % 2 == 0 ? R.color.table_even_row_background_color : R.color.table_odd_row_background_color));
            selectorView.link((ErrorLogItem) item);
        }

    }

}
