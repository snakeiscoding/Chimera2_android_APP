package com.jht.chimera.ui.machinesettings.adapters.update;

import android.view.View;
import android.widget.TextView;

import com.jht.androiduiwidgets.selector.IOnItemSelected;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;


/**
 * This class is used map a key test row to the view.
 */
class UpdatePackageSelectorItemHolder extends SelectorItemHolderBase {

    private TextView text;
    private View selectorView;

    /**
     * Cache the views in the holder.
     *
     * @param v The root view.
     */
    UpdatePackageSelectorItemHolder(View v) {
        super(v);
        selectorView = v;
        text = v.findViewById(R.id.eng_update_package);
    }


    /**
     * The item has changed.  We need to update the views.
     *
     * @param adapter The adapter managing the views.
     * @param item    The item with the view data.
     */
    @Override
    protected void update(final SelectorAdapterBase adapter, final ISelectorItem item, final IOnItemSelected onItemSelected) {

        text.setText(item.toString());
        selectorView.setBackgroundColor(selectorView.getContext().getResources().getColor(item.getIndex() % 2 == 0 ? R.color.table_even_row_background_color : R.color.table_odd_row_background_color));


    }

}
