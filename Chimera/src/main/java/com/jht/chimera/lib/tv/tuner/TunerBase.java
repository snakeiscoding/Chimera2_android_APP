package com.jht.chimera.lib.tv.tuner;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.util.IOnProgress;
import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.ui.machinesettings.pages.tv.TVChannelSelectorItemNew;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.channel.IChannel;

import java.io.File;
import java.util.List;

public abstract class TunerBase<Channel extends IChannel> implements IChimeraTuner<Channel> {

    private final com.jht.serialport.peripheral.tuner.ITuner<Channel> tuner;

    protected TunerBase(@NonNull com.jht.serialport.peripheral.tuner.ITuner<Channel> tuner) {
        this.tuner = tuner;
    }

    /**
     *
     * @return The tuner.
     */
    @Override
    public ITuner<Channel> tuner() {
        return tuner;
    }

    /** Reboots the tuner, if supported */
    @Override
    public void reboot() {
        throw new UnsupportedOperationException("Cannot reboot tuner because tuner does not implement the function.");
    }

    /**
     *
     * @return Get the default channel index.  This should be the index in the channel list.
     * @throws UnsupportedOperationException  Only used on tuners that have a channel list.
     */
    @Override
    public int getDefaultChannelInt() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot get the default channel as an index because tuner does not have a channel list.");
    }

    /**
     * @return Get the default channel as a UUID string.
     * @throws UnsupportedOperationException if not supported by the tuner.
     */
    @Override
    public String getDefaultChannel() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot get the default channel as a string because the tuner has a channel list.");
    }

    /**
     *
     * @param channel   Set the default channel index.
     * @throws UnsupportedOperationException  Only used on tuners that have a channel list.
     */
    @Override
    public void setDefaultChannel(int channel) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot set the default channel as an index because tuner does not have a channel list.");
    }

    /**
     * Sets the default channel as a UUID string.
     * @param channel   The default channel UUID.
     * @throws UnsupportedOperationException if not supported by the tuner.
     */
    @Override
    public void setDefaultChannel(@NonNull String channel) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot set the default channel as a string because the tuner has a channel list.");
    }
    /**
     * Execute an operation.
     *
     * @param view          The button used to explode the progress dialog from.  If this is automated
     *                      it can be null and there will be no dialog box.
     * @param operation     The operation to perform.
     * @param workingDir    The working directory where you will find files needed by the operation.
     * @param onProgress    Callback for the operation progress.
     * @throws UnsupportedOperationException    Not all tuners support all operations.
     */
    @Override
    public void executeOperation(@Nullable View view, @NonNull TunerOperation operation, @Nullable File workingDir, @Nullable IOnProgress onProgress, @Nullable DialogBase.IOnConfirm onConfirm) throws UnsupportedOperationException  {
        throw new UnsupportedOperationException("Tuner does not implement " + operation);
    }

    /**
     * Shutdown all threads related to the tuner and cleanup.
     */
    @Override
    public void destroy() {
        tuner.destroy();
    }


    /**
     * Create the channel selector items.  This is done by the tuner so we get the full type safety.
     *
     * @param parent            The parent selector item.
     * @param onEditChannel     The callback to handle item actions.
     * @param manageChannel    True if we can edit the channel.
     */
    @Override
    public void createChannelSelectorItems(@NonNull SelectorItemBase parent, @Nullable TVChannelSelectorItemNew.IOnEditChannel onEditChannel, boolean manageChannel, boolean allChannels) {
        for(Channel channel : tuner.getChannelList(allChannels)) {
            parent.addItem(new TVChannelSelectorItemNew<>(this, channel, onEditChannel, manageChannel, tuner().capabilities().canSkipChannel()));
        }

        if(manageChannel && tuner.capabilities().modifyChannelList()) {
            parent.addItem(new TVChannelSelectorItemNew<>(this, null, onEditChannel));
        }
    }

    /**
     * Helper function to create a progress dialog at the beginning of an operation.
     * @param view              The view to explode the dialog from.  If null don't create a dialog box.
     * @param onProgress        The original progress callback.
     * @param title             Title for the progress dialog.
     * @param message           Message for the progress dialog.
     * @param completeMessage   Complete message for the progress dialog.
     * @param allowCancel       True if we can cancel the process.
     * @return                  The on progress callback for the dialog if one was created.
     */
    protected IOnProgress initActionProgress(@Nullable View view, @Nullable IOnProgress onProgress, @NonNull String title, @NonNull String message, @Nullable String completeMessage, @SuppressWarnings("SameParameterValue") boolean allowCancel) {
        if(view == null) {
            return onProgress;
        }
        else {
            return DialogFactory.progressDialog(view, title, message, completeMessage, allowCancel, () -> {
                if(onProgress != null) {
                    onProgress.progress(100, true, null);
                }
            }, () -> tuner().cancelTransaction()).onProgress();
        }
    }

    /**
     * For tuners with a channel list this returns the index of the current channel in the list.
     * @return  The current channel
     */
    @Override
    public int getCurrentChannelIndex() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * For tuners without a channel list this returns the current channel as a number.
     * @return  The current channel
     */
    @Override
    public String getCurrentChannel() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }


    @Override
    public int getChannelSetupModes()  {
        return 1;
    }

    @Override
    public String channelSetupModeString(Context context, int mode)  {
        return context.getString(R.string.channel_mode_tc);
    }

    @Override
    public int getDefaultMode()  {
        return 0;
    }

    @Override
    public SETTINGS_MODE getModeType(int mode)  {
        return SETTINGS_MODE.CHANNEL_LIST;
    }

    /**
     *
     * Note we must make sure the default channel is correctly handled in this case.
     * Basically the expectation is that the default channel is the same after this
     * operation unless you are disabling the current default channel.  Then we
     * just move to the next in the list.  What makes this a little complicated is we
     * have the full list and the active list.  The default channel is an index to the
     * active list which is being modified by this function.  Lastly we are not
     * concerned with the case where this disables the last active channel leaving
     * the channel list empty.  That is not a valid use case.  No one would set up
     * TV and then disable all channels.
     *
     * @param channel   The channel that needs to be updated.
     * @param enable    True if we are enabling he channel.
     */
    @Override
    public void updateChannelEnabledStatus(Channel channel, boolean enable) {
        // TODO: Some tuners like the 750 use channel files to manage channel on-console. As such they override and call tuner().saveSettings to save the channel list. Should this be the case for all tuners?
        try {
            if (!enable) {
                // Compare by UUID so the check is list-order independent and is not
                // affected by whether hidden channels are included in the list
                try {

                    String channelUuid = channel.uuid();
                    String defaultUuid = getDefaultChannel();
                    if (channelUuid.equals(defaultUuid)) {
                        // Default channel is being hidden. Find the nearest visible neighbor:
                        // scan downward first, if nothing below, scan upward
                        List<Channel> visibleChannels = tuner.getChannelList(false);
                        int hiddenIndex = -1;
                        for (int i = 0; i < visibleChannels.size(); i++) {
                            if (visibleChannels.get(i).uuid().equals(channelUuid)) {
                                hiddenIndex = i;
                                break;
                            }
                        }

                        Channel newDefault = null;
                        if (hiddenIndex > 0 && hiddenIndex < visibleChannels.size()) {
                            newDefault = visibleChannels.get(hiddenIndex - 1);
                        } else if (hiddenIndex + 1 < visibleChannels.size()) {
                            newDefault = visibleChannels.get(hiddenIndex + 1);
                        }

                        if (newDefault != null) {
                            setDefaultChannel(newDefault.uuid());
                        } else {
                            // In case no other visible channels remain
                            setDefaultChannel(""); // TODO: CGC-22432 Remove once UUID migration is complete
                            setDefaultChannel(0);
                        }
                    }
                } catch (UnsupportedOperationException ignored) {
                    // Fallback to old flow
                    // TODO: CGC-22432 Remove once UUID migration is complete?
                    int defaultIndex = getDefaultChannelInt();
                    Channel defaultChannel = tuner.getChannelList(true).get(defaultIndex);
                    if (defaultChannel == channel) {
                        setDefaultChannel(0);
                    }
                }
            }
            channel.enabled(enable);
            tuner.sendChannelEnableStatus(channel);
            DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
        }
        catch(UnsupportedOperationException ignored) {}

    }

    @Override
    public void resetSerialPort(boolean update) {
        tuner.resetSerialPort(update);
    }

    @Override
    public void resetTunerVolume(int volume) {
        tuner.setTunerVolume(volume);
    }

}
