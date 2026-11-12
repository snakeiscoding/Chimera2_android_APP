package com.jht.data

import com.jht.firsttimesetup.Channel

/**
 * Represents a preview of a channel list, containing the channels and default channel information.
 *
 * @property channels The list of channels in the preview.
 * @property defaultChannel The default channel, if one is set.
 */
data class ChannelListPreview(
    val channels: List<Channel>,
    val defaultChannel: Channel? = null
) {
    companion object {
        /**
         * Represents a channel list preview of an empty or non-existent channel list
         */
        val NONE = ChannelListPreview(
            channels = emptyList(),
            defaultChannel = null
        )
    }
}