package com.jht.chimera.ui.machinesettings.pages.tv;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.tv.tuner.IChimeraTuner;
import com.jht.serialport.peripheral.tuner.channel.IChannel;

/**
 * This handles an item in the channel list in the channel setup activity in machine settings.
 */
public class TVChannelSelectorItemNew<Channel extends IChannel> extends SelectorItemBase {

    /**
     * These are actions that the user can take on each item.  The channel defines which operations
     * to show.
     */
    public enum CHANNEL_ACTIONS {
        CHANNEL_EDIT,
        CHANNEL_DELETE,
        CHANNEL_ENABLE,
        CHANNEL_DISABLE
    }

    /**
     * Callback when the user did an operation on the item.
     */
    public interface IOnEditChannel {
        void onEditChannel(CHANNEL_ACTIONS action, TVChannelSelectorItemNew<?> item);
    }

    // The channel we are managing.
    private final Channel channel;

    // Callback for the channel actions.
    private final IOnEditChannel onEditChannel;

    // Tuner that manages the channels.  This can be null for the IP scan list.
    private final IChimeraTuner<Channel> tuner;

    // This is used to determine if we are managing the channel in the settings app.  False
    // if we are in the TV app and we only want to use this to change channels.
    private final boolean manageChannel;

    // This is used to determine if the tv source of this channel supports enabling channels.
    private final boolean showDisableChannelControl;

    /**
     * Default constructor for an item with children.
     *
     * @param tuner     The tuner that the channel is for.  Set to NULL if the channel is not the main channel type for the tuner.
     * @param channel   The channel we are managing.  Set to Null if this is the special Add Channel item.
     * @param onEditChannel     Callback for user actions on this item.
     */
    public TVChannelSelectorItemNew(@Nullable IChimeraTuner<Channel> tuner, @Nullable Channel channel, @Nullable IOnEditChannel onEditChannel) {
        this(tuner, channel, onEditChannel, true, true);
    }

    /**
     * Default constructor for an item with children.
     *
     * @param tuner     The tuner that the channel is for.  Set to NULL if the channel is not the main channel type for the tuner.
     * @param channel   The channel we are managing.  Set to Null if this is the special Add Channel item.
     * @param onEditChannel     Callback for user actions on this item.
     * @param manageChannel     See above.
     */
    public TVChannelSelectorItemNew(@Nullable IChimeraTuner<Channel> tuner, @Nullable Channel channel, @Nullable IOnEditChannel onEditChannel, boolean manageChannel, boolean showDisableChannelControl) {
        super(null);
        this.channel = channel;
        this.onEditChannel = onEditChannel;
        this.tuner = tuner;
        this.manageChannel = manageChannel;
        this.showDisableChannelControl = showDisableChannelControl;
    }

    @Override
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {
        return new TVChannelSelectorItemHolderNew(LayoutInflater.from(context).inflate(R.layout.tv_channel_selector_item_new, parent,false));
    }

    /**
     *
     * @return  The channel for the item.
     */
    public Channel channel() { return channel; }

    /**
     * If this item is a part of the tuner channel list, change the channel.
     */
    @Override
    public void onSelection() {
        if(tuner != null) {
            tuner.setChannel(channel);
        }
    }

    /**
     * @return True if we are managing the channel settings.
     */
    boolean manageChannel() { return manageChannel; }

    /**
     * @return True if the TV source this channel belongs to supports enabling/hiding channels.
     */
    boolean enableChannel() { return showDisableChannelControl; }

    /**
     * Handle user edit callbacks.  This is called from the view holder.
     *
     * @param action    The action the user is wanting to perform on the item.
     */
    void callOnEditChannel(CHANNEL_ACTIONS action) {
        if(tuner != null) {
            switch (action) {
                case CHANNEL_EDIT:
                    break;
                case CHANNEL_DELETE:
                    tuner.tuner().getChannelList(true).remove(channel);
                    tuner.tuner().saveSettings();
                    break;
                case CHANNEL_ENABLE:
                    tuner.updateChannelEnabledStatus(channel, true);
                    tuner.tuner().saveSettings();
                    break;
                case CHANNEL_DISABLE:
                    tuner.updateChannelEnabledStatus(channel, false);
                    tuner.tuner().saveSettings();
                    break;
            }
        }
        if(onEditChannel != null) {
            onEditChannel.onEditChannel(action, this);
        }
    }


}
