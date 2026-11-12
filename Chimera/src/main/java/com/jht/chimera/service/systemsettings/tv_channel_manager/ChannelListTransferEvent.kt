package com.jht.chimera.service.systemsettings.tv_channel_manager

import android.os.Parcelable
import com.jht.chimera.service.systemsettings.tv_channel_manager.endpoints.ChannelListDestination
import kotlinx.parcelize.Parcelize

/**
 * Represents an event resulting from a channel list transfer operation.
 */
@Parcelize
sealed class ChannelListTransferEvent: Parcelable {
    abstract val destination: ChannelListDestination
    abstract val isImport: Boolean

    /**
     * Indicates that the channel list transfer completed successfully.
     *
     * @property destination The destination for the channel list transfer.
     * @property isImport True if the operation was an import; false if it was an export.
     */
    @Parcelize
    data class TransferSuccessful(
        override val destination: ChannelListDestination,
        override val isImport: Boolean,
    ) : ChannelListTransferEvent()

    /**
     * Indicates that the channel list transfer failed.
     *
     * @property destination The destination for the channel list transfer.
     * @property isImport True if the operation was an import; false if it was an export.
     * @property error The error that occurred during the transfer.
     */
    @Parcelize
    data class TransferFailed(
        override val destination: ChannelListDestination,
        override val isImport: Boolean,
        val error: ChannelListTransferError
    ) : ChannelListTransferEvent()
}

/**
 * Represents an error that occurred during a channel list transfer operation.
 */
@Parcelize
sealed class ChannelListTransferError: Parcelable {
    /**
     * Indicates that the channel list could not be found.
     */
    @Parcelize
    data object ChannelListNotFound : ChannelListTransferError()

    /**
     * Indicates that the channel list is already up to date.
     */
    @Parcelize
    data object ChannelListAlreadyUpToDate : ChannelListTransferError()

    /**
     * Indicates that the TV source is not supported for the channel list transfer.
     */
    @Parcelize
    data object UnsupportedTvSource : ChannelListTransferError()

    /**
     * Indicates that an authentication error occurred during the transfer.
     */
    @Parcelize
    data object AuthenticationError : ChannelListTransferError()

    /**
     * Indicates that a network error occurred during the transfer.
     */
    @Parcelize
    data object NetworkError : ChannelListTransferError()

    /**
     * Indicates that an unknown error occurred during the transfer.
     */
    @Parcelize
    data object UnknownError : ChannelListTransferError()
}