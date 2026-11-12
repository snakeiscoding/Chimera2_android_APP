package com.jht.chimera.ui.machinesettings.pages.tv;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;

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
public class TVChannelSelectorItemHolderNew extends SelectorItemHolderBase {
    private TextView name;
    private AppCompatImageButton edit;
    private AppCompatImageButton deleteChannel;
    private CheckBox enable;
    private Button addChannel;
    private View selectorView;
    private LinearLayout channelInfoContainer;


    /**
     * Cache the views in the holder.
     *
     * @param v     The root view.
     */
    protected TVChannelSelectorItemHolderNew(View v) {
        super(v);
        name = v.findViewById(R.id.tv_channel_list_channel_name);
        edit = v.findViewById(R.id.tv_channel_list_edit_channel);
        addChannel = v.findViewById(R.id.tv_channel_new_button);
        enable = v.findViewById(R.id.tv_channel_enabled);
        deleteChannel = v.findViewById(R.id.tv_channel_list_delete_channel);
        channelInfoContainer = v.findViewById(R.id.tv_channel_list_channel_info_container);
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
        if(item instanceof TVChannelSelectorItemNew<?>) {
            TVChannelSelectorItemNew<?> tvItem = (TVChannelSelectorItemNew<?>)item;
            if(tvItem.channel() == null) {
                channelInfoContainer.setVisibility(View.GONE);
                addChannel.setVisibility(View.VISIBLE);
                addChannel.setOnClickListener(v -> tvItem.callOnEditChannel(TVChannelSelectorItemNew.CHANNEL_ACTIONS.CHANNEL_EDIT));
            }
            else {
                channelInfoContainer.setVisibility(View.VISIBLE);
                addChannel.setVisibility(View.GONE);
                name.setText(tvItem.channel().displayName(null));
                name.setActivated(item.selected());
                selectorView.setActivated(item.selected());
                edit.setImageResource(item.selected() ? R.drawable.ic_edit_yorange : R.drawable.ic_edit_black);
                deleteChannel.setImageResource(item.selected() ? R.drawable.ic_close_circle_yorange : R.drawable.ic_close_circle_black);
                enable.setChecked(tvItem.channel().enabled());

                selectorView.setOnClickListener(v -> {
                    ISelectorItem root = item;
                    while (root.parent() != null) {
                        root = root.parent();
                    }
                    if (parent.getSelection() >= 0) {
                        adapter.notifyItemChanged(parent.getSelection());
                    }
                    parent.setSelection(item.getIndex());
                    item.onSelection();
                    adapter.notifyItemChanged(item.getIndex());
                    if (onItemSelected != null) {
                        ISelectorItem selectedItem = item;
                        while (selectedItem.numChildren() > 0 && selectedItem.showSubItems() && selectedItem.getSelection() >= 0) {
                            selectedItem = selectedItem.getChild(selectedItem.getSelection());
                        }
                        adapter.notifyItemChanged(selectedItem.getIndex());
                        onItemSelected.onItemSelected(selectedItem);
                    }
                });

                edit.setOnClickListener(v -> tvItem.callOnEditChannel(TVChannelSelectorItemNew.CHANNEL_ACTIONS.CHANNEL_EDIT));
                edit.setVisibility(tvItem.manageChannel() && tvItem.channel().canEdit() ? View.VISIBLE : View.GONE);
                deleteChannel.setOnClickListener(v -> tvItem.callOnEditChannel(TVChannelSelectorItemNew.CHANNEL_ACTIONS.CHANNEL_DELETE));
                deleteChannel.setVisibility(tvItem.manageChannel() && tvItem.channel().canDelete() ? View.VISIBLE : View.GONE);
                enable.setOnCheckedChangeListener((v, b) -> {
                    tvItem.callOnEditChannel(b ? TVChannelSelectorItemNew.CHANNEL_ACTIONS.CHANNEL_ENABLE : TVChannelSelectorItemNew.CHANNEL_ACTIONS.CHANNEL_DISABLE );
                });
                // Only display the enable/hide channel control if we are managing channels (in MES), the channel type supports disabling
                enable.setVisibility(tvItem.manageChannel() && tvItem.channel().canDisable() && tvItem.enableChannel()  ? View.VISIBLE : View.GONE);
            }

        }
    }


}
