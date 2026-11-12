package com.jht.chimera.ui.machinesettings.adapters.update;

import android.view.View;
import android.widget.Button;
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
class UpdateSelectorItemHolder extends SelectorItemHolderBase {


    private TextView version;
    private UpdateSelectorItem.IOnApplyUpdate onApplyUpdate;
    private Button applyUpdate;
    private View selectorView;

    /**
     * Cache the views in the holder.
     *
     * @param v The root view.
     */
    UpdateSelectorItemHolder(View v, final UpdateSelectorItem.IOnApplyUpdate onApplyUpdate) {
        super(v);
        selectorView = v;
        applyUpdate = v.findViewById(R.id.eng_apply_available_update);
        version = v.findViewById(R.id.eng_available_update_version);
        this.onApplyUpdate = onApplyUpdate;
    }


    /**
     * The item has changed.  We need to update the views.
     *
     * @param adapter The adapter managing the views.
     * @param item    The item with the view data.
     */
    @Override
    protected void update(final SelectorAdapterBase adapter, final ISelectorItem item, final IOnItemSelected onItemSelected) {
        applyUpdate.setOnClickListener(v -> {
            if (onApplyUpdate != null) {
                onApplyUpdate.onApplyUdpate(item.getIndex());
            }
        });
        if (item instanceof UpdateSelectorItem) {
            version.setText(((UpdateSelectorItem) item).version());
        }
        applyUpdate.setVisibility(item.selected() ? View.VISIBLE : View.GONE);


        selectorView.setActivated(item.selected());
        version.setActivated(item.selected());

        selectorView.setOnClickListener(v -> {
            ISelectorItem root = item;
            final ISelectorItem parent = item.parent();
            while (root.parent() != null) {
                root = root.parent();
            }
            if (parent.getSelection() >= 0) {
                adapter.notifyItemChanged(parent.getSelection());
            }
            parent.setSelection(item.getIndex());
            item.onSelection();
            if (onItemSelected != null) {
                ISelectorItem selectedItem = item;
                while (selectedItem.numChildren() > 0 && selectedItem.showSubItems() && selectedItem.getSelection() >= 0) {
                    selectedItem = selectedItem.getChild(selectedItem.getSelection());
                }
                adapter.notifyItemChanged(item.getIndex());
                onItemSelected.onItemSelected(selectedItem);
            }
        });

    }
}
