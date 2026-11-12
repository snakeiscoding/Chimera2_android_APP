package com.jht.chimera.service.systemsettings.tv_channel_manager.accessors

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVSource
import com.jht.firsttimesetup.Channel
import com.jht.serialport.peripheral.tuner.channel.IChannel
import java.io.File

/**
 * Interface for accessing and managing a TV source's channel lists.
 */
interface ChannelListAccessor {
    /**
     * Gets the TV source associated with this accessor.
     * @return the TV source as a [TVSource] enum.
     */
    fun getAssociatedTvSource(): TVSource

    /**
     * Retrieves the current channel list of the TV source.
     * @return a [File] containing the current channel list of the TV source.
     * @throws [ChannelListAccessException] if an exception occurs that prevents this operation from completing successfully
     */
    fun getChannelList(): File

    /**
     * Updates the channel list of the TV source with the contents of the provided file.
     * @param channelListFile a file containing the new channel list for the TV source
     * @throws [ChannelListAccessException] if an exception occurs that prevents this operation from completing successfully
     */
    fun updateChannelList(channelListFile: File)

    /**
     * Parses a channel list file and returns the channels without applying them.
     * @param channelListFile a file containing the channel list to parse
     * @return a list of [Channel] objects parsed from the file
     * @throws [ChannelListAccessException] if the file cannot be parsed
     */
    fun parseChannelList(channelListFile: File): List<IChannel>

    /**
     * Exception for handling errors during channel list access operations.
     * @param message explanatory message about the exception.
     * @param cause the cause of the exception.
     */
    class ChannelListAccessException(message: String, cause: Throwable?) : Exception(message, cause)
}