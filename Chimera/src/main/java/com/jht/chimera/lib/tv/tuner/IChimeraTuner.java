package com.jht.chimera.lib.tv.tuner;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.util.IOnProgress;
import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.machinesettings.pages.tv.TVChannelSelectorItemNew;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.TunerProperties;
import com.jht.serialport.peripheral.tuner.channel.IChannel;

import java.io.File;

/**
 * This class wraps the actual tuner object provided by the serial port library.  It allows us
 * to provide a generic interface to make the UI easier.  Also the serial port library cannot implement
 * the following functions as they are Chimera specific (or at least flavor specific).
 *      Default channels
 *      Serial port
 *
 * @param <Channel> The channel for the tuner.
 */
public interface IChimeraTuner<Channel extends IChannel> {

    enum DEFAULT_CHANNEL_TYPE {
        CHANNEL_INDEX,      // Default is an index into the channel list.
        CHANNEL_STRING,     // Default is just the channel as a string.  No list.
    }

    /**
     * In general we have several tuner operations.  They may be different per tuner.  Not
     * all tuners provide all operations.
     */
    enum TunerOperation {
        IMPORT_CHANNEL_LIST_FROM_USB(R.string.import_channel_list_from_usb_tc),
        EXPORT_CHANNEL_LIST_TO_USB(R.string.export_channel_list_from_usb_tc),
        EXPORT_CHANNEL_LIST_TO_TUNER(R.string.export_channels_to_tuner),
        IMPORT_CHANNEL_LIST_FROM_TUNER(R.string.import_channels_from_tuner_tc),
        IMPORT_IP_LIST_FROM_USB(R.string.import_ip_list_from_usb_tc),
        EXPORT_IP_LIST_TO_TUNER(R.string.export_ip_list_to_tuner_tc),
        INITIALIZE_TUNER(R.string.initialize_tc),
        ENABLE_LEGACY(R.string.enseo_enable_legacy_tc),
        UPDATE_TUNER_FIRMWARE(R.string.update_tuner_firmware_tc),
        UPLOAD_CHANNEL_XML(R.string.tv_channel_list_upload),
        DOWNLOAD_CHANNEL_XML(R.string.tv_channel_list_download);

        // The string label for the operation to show to the user.
        private final int labelResId;

        /**
         * Enum constructor that sets the label.
         * @param labelResId    The operation label to show the user.
         */
        TunerOperation(int labelResId) {
            this.labelResId = labelResId;
        }

        /**
         *
         * @return  The operation label resource id.
         */
        public int labelResId() {
            return labelResId;
        }

        /**
         * @return True if this operation replaces the channel list with imported data, meaning
         *         the stored default channel index/UUID may no longer be valid after completion.
         */
        public boolean isChannelListImport() {
            return this == IMPORT_CHANNEL_LIST_FROM_USB || this == IMPORT_CHANNEL_LIST_FROM_TUNER;
        }

    }

    ITuner<Channel> tuner();

    /** Reboots the tuner, if supported */
    void reboot() throws UnsupportedOperationException;

    /**
     * @return Get the default channel index.
     * @throws UnsupportedOperationException  Only used on tuners that have a channel list.
     */
    int getDefaultChannelInt() throws UnsupportedOperationException;

    /**
     * @return Get the default channel as a UUID string.
     * @throws UnsupportedOperationException if not supported by the tuner.
     */
    String getDefaultChannel() throws UnsupportedOperationException;

    /**
     * @param channel   Set the default channel index.
     * @throws UnsupportedOperationException if not supported by the tuner.
     */
    void setDefaultChannel(int channel) throws UnsupportedOperationException;

    /**
     * Sets the default channel as a UUID string.
     * @param channel   The default channel UUID.
     * @throws UnsupportedOperationException if not supported by the tuner.
     */
    void setDefaultChannel(@NonNull String channel) throws UnsupportedOperationException;

    /**
     * Upgrade the firmware with the file provided.  This takes care of the progress dialog as well
     * if needed.
     * @param view          The button used to explode the progress dialog from.  If this is automated
     *                      it can be null and there will be no dialog box.
     * @param upgradeFile   The firmware file (or location if the tuner needs a location).
     * @param onProgress    Callback on progress.
     * @throws UnsupportedOperationException  Not all tuners support this operation.
     */
    void upgradeTuner(@Nullable View view, @NonNull File upgradeFile, @Nullable IOnProgress onProgress) throws UnsupportedOperationException;

    /**
     * Execute an operation based on the list above.
     * @param view          The button used to explode the progress dialog from.  If this is automated
     *                      it can be null and there will be no dialog box.
     * @param operation     The operation to perform.
     * @param workingDir    The working directory where you will find files needed by the operation.
     * @param onProgress    Callback for the operation progress.
     * @param onConfirm     Callback for refreshing the page after downloading channel file. (see US8Tuner/TVChannelSetupActivityNew for example)
     *                      This method can be used to do an action after the user clicks an okay on a dialog
     * @throws UnsupportedOperationException    Not all tuners support all operations.
     */
    void executeOperation(@Nullable View view, @NonNull TunerOperation operation, @Nullable File workingDir, @Nullable IOnProgress onProgress, @Nullable DialogBase.IOnConfirm onConfirm) throws UnsupportedOperationException;

    /**
     * @param mode  Some tuners have different modes. For example the US7 tuner has two modes.  One
     *              for IP scan list and one for the channel list.
     *
     * @return      A list of operations supported by the tuner.
     */
    TunerOperation[] getSupportedOperations(int mode);

    /**
     * Shutdown all threads related to the tuner and cleanup.
     */
    void destroy();

    /**
     * Create the channel selector items.  This is done by the tuner so we get the full type safety.
     *
     * @param parent            The parent selector item.
     * @param onEditChannel     The callback to handle item actions.
     * @param manageChannel    True if we can edit the channel.
     */
    void createChannelSelectorItems(@NonNull SelectorItemBase parent, @Nullable TVChannelSelectorItemNew.IOnEditChannel onEditChannel, boolean manageChannel, boolean allChannels);

    /**
     * Reset the tuner changing the channel to the default channel and setting the tuner volume.
     */
    void reset();

    /**
     * Change the channel to the default channel.
     */
    void setChannelToDefault();

    /**
     * Execute the channel up operation.  Use this so we can keep track of the previous and current channels.
     */
    void channelUp();

    /**
     * Execute the channel down operation.  Use this so we can keep track of the previous and current channels.
     */
    void channelDown();

    /**
     * Set the current channel.  Call this instead of the tuner function so that we can keep track of the current and previous channels.
     * @param channel   The channel to set.
     */
    void setChannel(Channel channel);

    /**
     * For tuners with a channel list this returns the index of the current channel in the list.
     * @return  The current channel
     */
    int getCurrentChannelIndex() throws UnsupportedOperationException;

    /**
     * For tuners without a channel list this returns the current channel as a number.
     * @return  The current channel
     */
    String getCurrentChannel() throws UnsupportedOperationException;

    /**
     * Execute the previous channel operation.  Do this instead of calling directly into the tuner
     * so we can keep the previous and current channels up to date.
     */
    void previousChannel();

    enum SETTINGS_MODE {
        IP_SCAN_LIST,
        CHANNEL_LIST,
        NO_LIST

    }

    /**
     *
     * @return How the default channel is handled.  Note for CAB this is special because it may be
     * either depending on whether there is a channel list.
     */
    DEFAULT_CHANNEL_TYPE getDefaultChannelType();

    /**
     *
     * @return  Number of channel setup modes for the channel setup activity.
     */
    int getChannelSetupModes();

    /**
     *
     * @param context   Context used to get the string resource.
     * @param mode      The channel setup mode.
     * @return          The string for the given channel mode.
     */
    String channelSetupModeString(Context context, int mode);

    /**
     *
     * @return   The default channel mode.  This may change based on previous setup.
     */
    int getDefaultMode();

    /**
     *
     * @param mode  The mode to check.
     * @return      The mode type.  Useful for figuring out what UI elements to display.
     */
    SETTINGS_MODE getModeType(int mode);

    /**
     * Sends the channel status to the tuner and updates the default channel if necessary.
     * The enabled status is assumed to have already been updated in the channel object passed.
     *
     * @param channel   The channel that needs to be updated.
     */
    void updateChannelEnabledStatus(Channel channel, boolean enable);

    /**
     * Attempt to detect the tuner hardware and update the tuner setting if the tuner detected doesn't
     * match with the current tuner setting.  This generally applies only to tuners that follow the general
     * tuner protocol.  If the setting is CAB, STB, UIPTV, or US4 then leave it alone.  Still detect the
     * tuner so we can filter the selection.
     *
     * @param source    The current source.
     * @param force     True if we should assume KM4.  KM4 doesn't follow the protocol
     *                  exactly and we want to avoid false positives for the KM4.
     * @return          True if the detection was complete.
     */
    boolean ensureTunerMatchesDetectedHardware(SettingKeys.TVSource source, boolean force);

    /**
     * Reset the serial port for the tuner.
     *
     * @param update    True if we are opening the serial port for the update configuration.  For some
     *                  tuners the baudrate is different when updating firmware.
     */
    void resetSerialPort(boolean update);

    /**
     * Reset the tuner volume to the system setting.
     *
     * @param volume     The volume to set.
     */
    void resetTunerVolume(int volume);

    /**
     * Attempts to change the tuner's video output resolution. Note that this may cause the STB
     * to reboot (unless the specified resolution is the current resolution).
     * ( At this time it is used primarily with LG tuners. )
     *
     * @param resolution The output resolution the STB will be set to
     */
    void setTunerOutputResolution(@NonNull TunerProperties.OUTPUT_RESOLUTION resolution);

    /** Initiates version retrieval, if implemented for the given tuner */
    void getVersion();

    /** Set locale based on current android locale, if supported */
    void setLocale(String androidLocaleTag);
}