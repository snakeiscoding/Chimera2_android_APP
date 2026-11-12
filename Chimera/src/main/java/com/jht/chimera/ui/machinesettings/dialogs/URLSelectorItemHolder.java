package com.jht.chimera.ui.machinesettings.dialogs;

import android.view.View;
import android.widget.ImageButton;

import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.IOnItemSelected;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;

public class URLSelectorItemHolder extends SelectorItemHolderBase {

    private UserTextInput urlEdit;
    private ImageButton deleteItem;

    protected URLSelectorItemHolder(View v) {
        super(v);

        urlEdit = v.findViewById(R.id.network_ip_settings_dialog_url_entry);
        deleteItem = v.findViewById(R.id.url_list_item_delete);
    }

    @Override
    protected void update(final SelectorAdapterBase selectorAdapterBase, ISelectorItem iSelectorItem, IOnItemSelected iOnItemSelected) {
        if (iSelectorItem instanceof URLSelectorItem) {
            final URLSelectorItem item = (URLSelectorItem) iSelectorItem;
            urlEdit.setOnTextChanged(new UserTextInput.IOnTextChanged() {
                @Override
                public void onTextChanged(String text) {
                    item.url = text;
                }
            });
            deleteItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.parent().removeItem(item.getIndex());
                    selectorAdapterBase.notifyItemRemoved(item.getIndex());
                }
            });
            urlEdit.setText(item.url);
            urlEdit.setReadOnly(item.readOnly);
            deleteItem.setVisibility(item.readOnly ? View.GONE : View.VISIBLE);

        }

    }
}
