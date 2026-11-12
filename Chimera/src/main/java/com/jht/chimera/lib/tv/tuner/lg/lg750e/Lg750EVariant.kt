package com.jht.chimera.lib.tv.tuner.lg.lg750e

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVSource
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannel.ChannelType
import com.jht.chimera.lib.tv.tuner.lg.channel.LgRegion

enum class Lg750EVariant(
    val modelName: String,
    val associatedTvSource: TVSource,
    val settingsDirectory: String,
    val defaultChannelKey: Any,
    val defaultChannelUuidKey: Any,
    val currentChannelKey: Any,
    val supportedChannelTypes: List<ChannelType>,
    val channelRegion: LgRegion
) {
    // Enum to distinguish between the different LG750E variants (US9 / KM7 / BR3 / JP1).
    // Supported channel types per IDCAP documentation.
    NorthAmerica(
        modelName = "STB-750E-UA",
        associatedTvSource = TVSource.US9,
        settingsDirectory = "settings/us9",
        defaultChannelKey = SettingKeys.TV.DATA_TV_US9_DEFAULT_CHANNEL_INT,
        defaultChannelUuidKey = SettingKeys.TV.DATA_TV_US9_DEFAULT_CHANNEL_UUID_STRING,
        currentChannelKey = TVKeys.DATA_US9_CURRENT_CHANNEL_INT,
        supportedChannelTypes = listOf(ChannelType.IP_CLASS_1, ChannelType.IP_CLASS_2, ChannelType.RF_CLASS_1, ChannelType.RF_CLASS_3),
        channelRegion = LgRegion.NORTH_AMERICA
    ),
    Europe(
        modelName = "STB-750E-LA",
        associatedTvSource = TVSource.KM7,
        settingsDirectory = "settings/km7",
        defaultChannelKey = SettingKeys.TV.DATA_TV_KM7_DEFAULT_CHANNEL_INT,
        defaultChannelUuidKey = SettingKeys.TV.DATA_TV_KM7_DEFAULT_CHANNEL_UUID_STRING,
        currentChannelKey = TVKeys.DATA_KM7_CURRENT_CHANNEL_INT,
        supportedChannelTypes = listOf(ChannelType.IP_CLASS_2, ChannelType.RF_CLASS_2, ChannelType.RF_CLASS_4, ChannelType.RF_CLASS_5),
        channelRegion = LgRegion.EUROPE
    ),
    SouthAmerica(
        modelName = "STB-750E-SA",
        associatedTvSource = TVSource.BR3,
        settingsDirectory = "settings/br3",
        defaultChannelKey = SettingKeys.TV.DATA_TV_BR3_DEFAULT_CHANNEL_INT,
        defaultChannelUuidKey = SettingKeys.TV.DATA_TV_BR3_DEFAULT_CHANNEL_UUID_STRING,
        currentChannelKey = TVKeys.DATA_BR3_CURRENT_CHANNEL_INT,
        supportedChannelTypes = listOf(ChannelType.IP_CLASS_2, ChannelType.RF_CLASS_3),
        channelRegion = LgRegion.SOUTH_AMERICA
    ),
    Japan(
        modelName = "STB-750E-JA",
        associatedTvSource = TVSource.JP1,
        settingsDirectory = "settings/jp1",
        defaultChannelKey = SettingKeys.TV.DATA_TV_JP1_DEFAULT_CHANNEL_INT,
        defaultChannelUuidKey = SettingKeys.TV.DATA_TV_JP1_DEFAULT_CHANNEL_UUID_STRING,
        currentChannelKey = TVKeys.DATA_JP1_CURRENT_CHANNEL_INT,
        supportedChannelTypes = listOf(ChannelType.IP_CLASS_2, ChannelType.RF_CLASS_3),
        channelRegion = LgRegion.JAPAN
    );

    companion object {
        /**
         * Looks up the [Lg750EVariant] matching the given tuner model name (case-insensitive).
         *
         * @param modelName The model name reported by the tuner (e.g. "STB-750E-UA").
         * @return The matching [Lg750EVariant], or null if no match is found.
         */
        fun fromModelName(modelName: String): Lg750EVariant? {
            return entries.find { it.modelName.equals(modelName, ignoreCase = true) }
        }
    }
}
