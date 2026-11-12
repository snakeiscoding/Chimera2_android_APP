package com.jht.chimera.service.systemsettings.tv_channel_manager.accessors

import android.content.Context
import com.jht.chimera.lib.common.ExecuteAsRoot
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVSource
import com.jht.serialport.peripheral.tuner.channel.IChannel
import java.io.File
import java.io.IOException

/**
 * Implements the [ChannelListAccessor] interface for LG based TV sources (US8, KM6, US9, KM7, BR3, JP1).
 * This class provides the ability to access and/or update LG TvSource channel lists.
 */
internal class LgChannelListAccessor(
    private val context: Context,
    private val associatedTvSource: TVSource,
    private val channelListDirectory: String,
    private val schemaFileName: String,
    private val channelParser: (schemaFile: File, dataFile: File) -> List<IChannel>,
): ChannelListAccessor {

    companion object {
        private const val LG_CHANNEL_DATA_FILE = "channels.xml"
    }

    override fun getAssociatedTvSource() = associatedTvSource

    override fun getChannelList(): File {
        try {
            // Check that channel list directory exists. Attempt to create it if needed.
            val parentDirectory = File(context.filesDir, channelListDirectory)
            if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
                throw IOException("Failed to create directory ${parentDirectory.absolutePath}")
            }
            // Check if channel list already exists. If not, attempt to create it.
            val channelFile = File(parentDirectory, LG_CHANNEL_DATA_FILE)
            if (!channelFile.exists() && !channelFile.createNewFile()) {
                throw IOException("Failed to create file ${channelFile.absolutePath}")
            }
            return channelFile
        } catch (e: Exception) {
            throw ChannelListAccessor.ChannelListAccessException("Failed to get LG6500 channel list", e)
        }
    }

    override fun updateChannelList(channelListFile: File) {
        try {
            // Check that channel list directory exists. Attempt to create it if needed.
            val destinationDirectory = File(context.filesDir, channelListDirectory)
            if (!destinationDirectory.exists() && !destinationDirectory.mkdirs()) {
                throw IOException("Failed to create directory ${destinationDirectory.absolutePath}")
            }
            // Check if channel list already exists. If so, delete it since it will be replaced.
            val destinationFile = File(destinationDirectory, LG_CHANNEL_DATA_FILE)
            if (destinationFile.exists() && !destinationFile.delete()) {
                throw IOException("Failed to delete file ${destinationFile.absolutePath}")
            }
            // Flush downloaded file contents to storage before rename so data survives a power loss
            ExecuteAsRoot.runSyncCommand()
            // Replace old channel list with new channel list
            channelListFile.renameTo(destinationFile)
            // Remove cached channel list if it still exists
            if (channelListFile.exists() && !channelListFile.delete()) {
                throw IOException("Failed to delete file ${channelListFile.absolutePath}")
            }
        } catch (e: Exception) {
            throw ChannelListAccessor.ChannelListAccessException("Failed to update LG6500 channel list", e)
        }
    }

    override fun parseChannelList(channelListFile: File): List<IChannel> {
        try {
            val settingsDir = File(context.filesDir, channelListDirectory)
            val channelListSchema = File(settingsDir, schemaFileName)
            if (!channelListSchema.exists()) {
                throw IllegalStateException("Unable to locate schema for validating the LG channel list")
            }
            return channelParser(channelListSchema, channelListFile)
        } catch (e: Exception) {
            throw ChannelListAccessor.ChannelListAccessException("Failed to parse LG channel list", e)
        }
    }
}