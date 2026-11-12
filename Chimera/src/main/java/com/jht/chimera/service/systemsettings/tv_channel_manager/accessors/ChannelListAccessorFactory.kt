package com.jht.chimera.service.systemsettings.tv_channel_manager.accessors

import android.content.Context
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVSource
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannel
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannelLoader
import com.jht.chimera.lib.tv.tuner.lg.lg6500.LG6500Variant
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750EVariant
import com.jht.serialport.peripheral.tuner.channel.IChannel
import com.jht.serialport.peripheral.tuner.enseo.EnseoChannel
import com.jht.serialport.peripheral.tuner.enseo.EnseoChannelLoader
import com.jht.chimera.service.systemsettings.tv_channel_manager.accessors.ChannelListAccessorFactory.UnsupportedTvSourceException
import java.io.File

/**
 * A factory class for creating instances of [ChannelListAccessor] based on the TV source.
 *
 * @throws UnsupportedTvSourceException if the TV source provided does not have a corresponding accessor implementation.
 */
object ChannelListAccessorFactory {
    private const val SCHEMA_IPTV = "ip_list.xsd"
    private const val SCHEMA_LG = "lg_channel_list.xsd"

    /**
     * Creates an instance of [ChannelListAccessor] corresponding to the provided [tvSource].
     *
     * @param tvSource The TV source for which the channel list accessor is to be created.
     * @param context The Android context, necessary for some accessors that may interact with Android-specific APIs.
     * @return an instance of [ChannelListAccessor] that is capable of handling channel list operations for the specified TV source.
     * @throws UnsupportedTvSourceException if there is no accessor available for the given TV source.
     */
    fun create(
        tvSource: TVSource,
        context: Context
    ) : ChannelListAccessor = when (tvSource) {
        TVSource.US8 -> LgChannelListAccessor(context, TVSource.US8, LG6500Variant.US8.settingsDirectory, SCHEMA_IPTV, ::parseIptvChannels)
        TVSource.KM6 -> LgChannelListAccessor(context, TVSource.KM6, LG6500Variant.KM6.settingsDirectory, SCHEMA_IPTV, ::parseIptvChannels)
        TVSource.US9 -> LgChannelListAccessor(context, TVSource.US9, Lg750EVariant.NorthAmerica.settingsDirectory, SCHEMA_LG, ::parseLgChannels)
        TVSource.KM7 -> LgChannelListAccessor(context, TVSource.KM7, Lg750EVariant.Europe.settingsDirectory, SCHEMA_LG, ::parseLgChannels)
        TVSource.BR3 -> LgChannelListAccessor(context, TVSource.BR3, Lg750EVariant.SouthAmerica.settingsDirectory, SCHEMA_LG, ::parseLgChannels)
        TVSource.JP1 -> LgChannelListAccessor(context, TVSource.JP1, Lg750EVariant.Japan.settingsDirectory, SCHEMA_LG, ::parseLgChannels)
        else -> throw UnsupportedTvSourceException()
    }

    private fun parseIptvChannels(schema: File, file: File): List<IChannel> {
        val channels = mutableListOf<EnseoChannel>()
        EnseoChannelLoader().loadList(schema, file, channels)
        return channels
    }

    private fun parseLgChannels(schema: File, file: File): List<IChannel> {
        val channels = mutableListOf<LgChannel>()
        LgChannelLoader().loadList(schema, file, channels)
        return channels
    }

    /**
     * Exception thrown when a TV source is provided for which there is no corresponding [ChannelListAccessor] implementation.
     */
    class UnsupportedTvSourceException : Exception()
}