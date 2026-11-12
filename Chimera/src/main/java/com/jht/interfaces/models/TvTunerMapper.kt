package com.jht.interfaces.models

import com.jht.firsttimesetup.Channel
import com.jht.serialport.peripheral.tuner.channel.IChannel

object TvTunerMapper {


    /**
     * Maps [com.jht.serialport.peripheral.tuner.channel.IChannel] to [Channel]
     */
    fun mapToChannel(channel: IChannel) : Channel {
        return Channel(
            name = channel.name(),
            number = channel.number(),
            isHidden = !channel.enabled(),
            uuid = channel.uuid(),
        )
    }

    /**
     * Maps list of [com.jht.serialport.peripheral.tuner.channel.IChannel] to list of [Channel]
     */
    fun mapToChannels(channels: List<IChannel>?, defaultChannelIndex: Int) : List<Channel> {
        return channels?.mapIndexed { index, channel ->
            mapToChannel(channel).copy(isDefault = index == defaultChannelIndex)
        } ?: emptyList()
    }
}