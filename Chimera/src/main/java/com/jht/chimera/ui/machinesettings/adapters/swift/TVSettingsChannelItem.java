package com.jht.chimera.ui.machinesettings.adapters.swift;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.lib.tv.ChannelInfo;

/**
 * This is contains data needed to create a row in the channel table for swiftInfo tuner.
 */
public class TVSettingsChannelItem extends SelectorItemBase {
    public interface IOnChannelItemUpdate {
        void onDirty(TVSettingsChannelItem item);
        void onDelete(TVSettingsChannelItem item);
    }

    private ChannelInfo channel;
    private boolean dirty = false;
    private IOnChannelItemUpdate onChannelItemUpdate;
    private boolean canDelete;
    private int layoutId;


    /**
     * Default constructor for an item with children.
     */
    public TVSettingsChannelItem(ChannelInfo channel, IOnChannelItemUpdate onChannelItemUpdate, boolean canDelete, int layoutId) {
        this.channel = channel;
        this.onChannelItemUpdate = onChannelItemUpdate;
        this.canDelete = canDelete;
        this.layoutId = layoutId;
    }

    // Getters for the data
    public ChannelInfo channel() {
        return channel;
    }

    public boolean dirty() { return dirty; }

    public boolean canDelete() { return canDelete; }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if(onChannelItemUpdate != null) {
            onChannelItemUpdate.onDirty(this);
        }
    }

    public void delete() {
        if(onChannelItemUpdate != null) {
            onChannelItemUpdate.onDelete(this);
        }
    }

    @NonNull
    @Override
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new TVSettingsChannelItemHolder(v);
    }
}
