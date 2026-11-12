package com.jht.chimera.service.systemsettings.tv_channel_manager.endpoints

import android.content.Context
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.service.systemsettings.tv_channel_manager.endpoints.core.CoreChannelListEndpoint
import com.jht.datamatrixlib.DataMatrix

/**
 * Enum of possible destinations for channel list transfers.
 */
enum class ChannelListDestination {
    INTERNET
}

/**
 * Factory object for creating instances of [ChannelListEndpoint].
 */
object ChannelListEndpointFactory {
    /**
     * Creates an instance of [ChannelListEndpoint] for the provided [destination].
     *
     * @param destination The destination for which a [ChannelListEndpoint] is needed.
     * @param context The Android context necessary for certain [ChannelListEndpoint] instances.
     * @return A [ChannelListEndpoint] for the provided destination.
     */
    fun create(
        destination: ChannelListDestination,
        context: Context
    ): ChannelListEndpoint = when (destination) {
        ChannelListDestination.INTERNET -> {
            val dapiEnvironment = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)
            val clubId = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT)
            CoreChannelListEndpoint(dapiEnvironment, clubId, context)
        }
    }
}