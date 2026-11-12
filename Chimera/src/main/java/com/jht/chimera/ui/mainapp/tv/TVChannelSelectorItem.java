package com.jht.chimera.ui.mainapp.tv;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.tv.ChannelInfo;

import java.util.List;

/**
 * This is a simple implementation of the ISelectorItem.  This only needs a label for every item.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class TVChannelSelectorItem extends SelectorItemBase {

    private ChannelInfo channelInfo;

    /**
     * Default constructor for an item with children.
     *
     */
    public TVChannelSelectorItem(@NonNull ChannelInfo channelInfo, @Nullable List<ISelectorItem> subItems) {
        super(subItems);
        this.channelInfo = channelInfo;
    }


    @Override
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {

        View v = LayoutInflater.from(context).inflate(R.layout.tv_channel_selector_item, parent,false);
        return new TVChannelSelectorItemHolder(v);
    }

    public ChannelInfo channelInfo() { return channelInfo; }
}
