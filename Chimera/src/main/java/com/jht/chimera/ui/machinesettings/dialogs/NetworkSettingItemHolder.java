package com.jht.chimera.ui.machinesettings.dialogs;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatTextView;

import com.jht.androiduiwidgets.selector.IOnItemSelected;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;

public class NetworkSettingItemHolder extends SelectorItemHolderBase {

    private AppCompatTextView title;
    private AppCompatTextView value1;
    private AppCompatTextView value2;
    private LinearLayout container;

    NetworkSettingItemHolder(View v) {
        super(v);

        container = v.findViewById(R.id.network_setting_item_container);
        title = v.findViewById(R.id.network_setting_item_title);
        value1 = v.findViewById(R.id.network_setting_item_value1);
        value2 = v.findViewById(R.id.network_setting_item_value2);
    }

    @Override
    protected void update(final SelectorAdapterBase selectorAdapterBase, ISelectorItem iSelectorItem, IOnItemSelected iOnItemSelected) {
        if (iSelectorItem instanceof NetworkSettingItem) {
            final NetworkSettingItem item = (NetworkSettingItem) iSelectorItem;
            title.setText(item.title);
            value1.setText(item.value1);
            value2.setText(item.value2);
            //noinspection HardCodedStringLiteral,HardCodedStringLiteral
            container.setBackgroundColor(Color.parseColor(item.getIndex() % 2 == 0 ? "#FF1C2122" : "#FF3D454D"));
            if (item.getIndex() == 0) {
                value1.setTypeface(value1.getTypeface(), Typeface.BOLD);
                value2.setTypeface(value1.getTypeface(), Typeface.BOLD);

            }
        }

    }
}
