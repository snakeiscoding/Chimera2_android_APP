package com.jht.chimera.service.systemsettings.tv_channel_manager.endpoints

import com.jht.chimera.service.systemsettings.tv_channel_manager.ChannelListTransferEvent
import com.jht.chimera.service.systemsettings.tv_channel_manager.accessors.ChannelListAccessor
import com.jht.data.ChannelListPreview


/**
 * Interface for endpoints that manage the import and export of TV channel lists.
 *
 * This interface serves as a contract for various implementations that handle the transfer of
 * TV channel lists. Each implementation defines the specific mechanisms for how channel
 * lists are imported from and exported to the associated endpoint.
 */
interface ChannelListEndpoint: AutoCloseable {
    /**
     * Imports a channel list from the endpoint to the TV source.
     *
     * @param channelListAccessor The ChannelListAccessor instance for updating the TV source's channel list.
     * @return A [ChannelListTransferEvent] representing the result of the import operation.
     */
    suspend fun importChannelList(channelListAccessor: ChannelListAccessor): ChannelListTransferEvent

    /**
     * Exports the channel list of the TV source to the endpoint.
     *
     * @param channelListAccessor The ChannelListAccessor instance for accessing the TV source's channel list.
     * @return A [ChannelListTransferEvent] representing the result of the export operation.
     */
    suspend fun exportChannelList(channelListAccessor: ChannelListAccessor): ChannelListTransferEvent

    /**
     * Previews the channel list from the endpoint without applying it.
     *
     * @param channelListAccessor The ChannelListAccessor instance for parsing the channel list.
     * @return A [ChannelListPreview] containing the channels and default channel.
     * @throws Exception if the preview operation fails.
     */
    suspend fun previewChannelList(channelListAccessor: ChannelListAccessor): ChannelListPreview
}