package com.jht.chimera.ui.machinesettings.pages.tv;


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

    public interface IOnEditChannel {
        void onEditChannel(TVChannelSelectorItem item);
    }

    public interface IOnDeleteChannel {
        void onDeleteChannel(TVChannelSelectorItem item);
    }
    private ChannelInfo channelInfo;
    private IOnEditChannel onEditChannel;
    private IOnDeleteChannel onDeleteChannel;
    private boolean canDelete;
    private boolean canEditChannelName;

    /**
     * Default constructor for an item with children.
     *
     */
    public TVChannelSelectorItem(@Nullable ChannelInfo channelInfo, @Nullable List<ISelectorItem> subItems, IOnEditChannel onEditChannel, boolean canDelete, IOnDeleteChannel onDeleteChannel, boolean canEditChannelName) {
        super(subItems);
        this.channelInfo = channelInfo;
        this.onEditChannel = onEditChannel;
        this.canDelete = canDelete;
        this.onDeleteChannel = onDeleteChannel;
        this.canEditChannelName = canEditChannelName;
    }


    @Override
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {

        View v = LayoutInflater.from(context).inflate(R.layout.tv_channel_selector_item_eng, parent,false);
        return new TVChannelSelectorItemHolder(v);
    }

    public ChannelInfo channelInfo() { return channelInfo; }

    boolean calDelete() { return canDelete; }

    void callOnEditChannel() {
        if(onEditChannel != null) {
            onEditChannel.onEditChannel(this);
        }
    }

    void callOnDeleteChannel() {
        if(onDeleteChannel != null) {
            onDeleteChannel.onDeleteChannel(this);
        }
    }

    boolean calEditChannelName() { return canEditChannelName; }
}
