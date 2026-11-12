package com.jht.chimera.ui.mainapp.tv;

import android.view.View;
import android.widget.TextView;

import com.jht.androiduiwidgets.selector.IOnItemSelected;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;

/**
 * This class is used by the recycler view to manage the views.  It caches the views within the selector
 * item.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class SKNETChannelSelectorItemHolder extends SelectorItemHolderBase {
    private TextView number;
    private TextView name;
    private View selectorView;


    /**
     * Cache the views in the holder.
     *
     * @param v     The root view.
     */
    protected SKNETChannelSelectorItemHolder(View v) {
        super(v);
        number = v.findViewById(R.id.tv_channel_list_channel_number);
        name = v.findViewById(R.id.tv_channel_list_channel_name);
        selectorView = v;
    }


    /**
     * The item has changed.  We need to update the views.
     *
     * @param adapter   The adapter managing the views.
     * @param item      The item with the view data.
     */
    protected void update(final SelectorAdapterBase adapter, final ISelectorItem item, final IOnItemSelected onItemSelected) {
        final ISelectorItem parent = item.parent();
        if(item instanceof SKNETChannelSelectorItem) {
            SKNETChannelSelectorItem tvItem = (SKNETChannelSelectorItem)item;
            number.setText(tvItem.sknetChannelInfo().getNumber());
            if(tvItem.sknetChannelInfo().getServiceName() == null || tvItem.sknetChannelInfo().getServiceName().isEmpty()) {
                name.setText(R.string.no_channel_info_tc);
            }
            else {
                name.setText(tvItem.sknetChannelInfo().getServiceName());
            }

            number.setActivated(item.selected());
            name.setActivated(item.selected());
            selectorView.setActivated(item.selected());

            selectorView.setOnClickListener(v -> {
                ISelectorItem root = item;
                while(root.parent() != null) {
                    root = root.parent();
                }
                if(parent.getSelection() >= 0) {
                    adapter.notifyItemChanged(parent.getSelection());
                }
                parent.setSelection(item.getIndex());
                item.onSelection();
                adapter.notifyItemChanged(item.getIndex());
                if(onItemSelected != null) {
                    ISelectorItem selectedItem = item;
                    while (selectedItem.numChildren() > 0 && selectedItem.showSubItems() && selectedItem.getSelection() >= 0) {
                        selectedItem = selectedItem.getChild(selectedItem.getSelection());
                    }
                    adapter.notifyItemChanged(selectedItem.getIndex());
                    onItemSelected.onItemSelected(selectedItem);
                }
            });

        }
    }


}
