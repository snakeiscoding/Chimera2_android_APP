package com.jht.chimera.ui.machinesettings.adapters.keypress;

import android.view.View;
import android.widget.TextView;

import com.jht.androiduiwidgets.selector.IOnItemSelected;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;


/**
 * This class is used map a keyCode test row to the view.
 */
class KeyPressSelectorItemHolder extends SelectorItemHolderBase {

    private View selectorView;
    private TextView keyCode;
    private TextView keyState;
    private TextView combo;

    /**
     * Cache the views in the holder.
     *
     * @param v The root view.
     */
    KeyPressSelectorItemHolder(View v) {
        super(v);
        selectorView = v;
        keyCode = selectorView.findViewById(R.id.eng_key_test_key);
        keyState = selectorView.findViewById(R.id.eng_key_test_key_code);
        combo = selectorView.findViewById(R.id.eng_key_test_key_combo);
    }


    /**
     * The item has changed.  We need to update the views.
     *
     * @param adapter The adapter managing the views.
     * @param item    The item with the view data.
     */
    @Override
    protected void update(final SelectorAdapterBase adapter, final ISelectorItem item, final IOnItemSelected onItemSelected) {

        if (item instanceof KeyPressSelectorItem) {
            selectorView.setBackgroundColor(selectorView.getContext().getResources().getColor(item.getIndex() % 2 == 0 ? R.color.table_even_row_background_color : R.color.table_odd_row_background_color));
            KeyPressSelectorItem keyItem = (KeyPressSelectorItem) item;
            keyCode.setText("" + keyItem.keyCode());
            keyState.setText("" + keyItem.keyState());
        }

    }

}
