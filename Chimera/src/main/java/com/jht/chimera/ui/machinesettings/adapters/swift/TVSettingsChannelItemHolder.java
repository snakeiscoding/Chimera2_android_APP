package com.jht.chimera.ui.machinesettings.adapters.swift;

import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.IOnItemSelected;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;

public class TVSettingsChannelItemHolder extends SelectorItemHolderBase {
    private static final String TAG = "TVSettingsChannelIte...";
    private View selectorView;
    private UserTextInput channel;
    private UserTextInput name;
    private UserTextInput url;
    private AppCompatImageButton delete;


    /**
     * Cache the views in the holder.
     *
     * @param v The root view.
     */
    TVSettingsChannelItemHolder(View v) {
        super(v);
        selectorView = v;
        channel = selectorView.findViewById(R.id.eng_tv_settings_channel);
        name = selectorView.findViewById(R.id.eng_tv_settings_name);
        delete = selectorView.findViewById(R.id.eng_tv_channel_remove);
        url = selectorView.findViewById(R.id.eng_tv_settings_url);
    }

    /**
     * The item has changed.  We need to update the views.
     *
     * @param adapter The adapter managing the views.
     * @param item    The item with the view data.
     */
    @Override
    protected void update(SelectorAdapterBase adapter, final ISelectorItem item, IOnItemSelected onItemSelected) {
        if (item instanceof TVSettingsChannelItem) {
            TVSettingsChannelItem channelItem = (TVSettingsChannelItem)item;
            selectorView.setBackgroundColor(ContextCompat.getColor(selectorView.getContext(), item.getIndex() % 2 == 0 ? R.color.table_even_row_background_color : R.color.table_odd_row_background_color));
            channel.setText(channelItem.channel().number());
            channel.setOnTextChanged(s -> {
                channelItem.channel().number(s);
                channelItem.setDirty(true);
            });
            channel.setReadOnly(channelItem.channel().numberReadOnly());
            name.setText(channelItem.channel().name());
            name.setOnTextChanged(s -> {
                channelItem.channel().name(s);
                channelItem.setDirty(true);
            });
            if(delete != null) {
                delete.setVisibility(channelItem.canDelete() ? View.VISIBLE : View.GONE);
                delete.setOnClickListener(v -> channelItem.delete());

                if (url != null) {
                    url.setText(channelItem.channel().url());
                    url.setOnTextChanged(s -> {
                        channelItem.channel().url(s);
                        channelItem.setDirty(true);
                    });
                }
            }
        }
    }
}
