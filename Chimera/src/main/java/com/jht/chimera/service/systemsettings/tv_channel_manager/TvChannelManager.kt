package com.jht.chimera.service.systemsettings.tv_channel_manager

import android.content.Context
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVSource
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.service.systemsettings.tv_channel_manager.accessors.ChannelListAccessorFactory
import com.jht.chimera.service.systemsettings.tv_channel_manager.endpoints.ChannelListDestination
import com.jht.chimera.service.systemsettings.tv_channel_manager.endpoints.ChannelListEndpointFactory
import com.jht.datamatrixlib.DataMatrix
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object TvChannelManager {
    private const val TAG = "TvChannelManager"
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var autoImportTask: ScheduledFuture<*>? = null
    private var transferInProgress = false

    fun startDailyChannelListImport(context: Context) {
        autoImportTask?.cancel(false)
        autoImportTask = scheduler.scheduleWithFixedDelay({
            if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_STOP) {
                transferChannelList(
                    context = context,
                    tvSource = TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING)),
                    destination = ChannelListDestination.INTERNET,
                    isImport = true,
                )
            }
        }, 5, 1440, TimeUnit.MINUTES)
    }

    fun stopDailyChannelListImport() {
        autoImportTask?.cancel(false)
    }

    fun transferChannelList(
        context: Context,
        tvSource: TVSource,
        destination: ChannelListDestination,
        isImport: Boolean,
        onTransferFinished: (ChannelListTransferEvent) -> Unit
    ) {
        if (transferInProgress) return
        transferInProgress = true
        scheduler.execute {
            val transferEvent = transferChannelList(context, tvSource, destination, isImport)
            onTransferFinished(transferEvent)
            transferInProgress = false
        }
    }

    private fun transferChannelList(
        context: Context,
        tvSource: TVSource,
        destination: ChannelListDestination,
        isImport: Boolean,
    ) : ChannelListTransferEvent {
        return runBlocking {
            try {
                ChannelListEndpointFactory.create(destination, context).use { endpoint ->
                    val channelListAccessor = ChannelListAccessorFactory.create(tvSource, context)
                    if (isImport) endpoint.importChannelList(channelListAccessor)
                    else endpoint.exportChannelList(channelListAccessor)
                }
            } catch (e : Exception) {
                when (e) {
                    is ChannelListAccessorFactory.UnsupportedTvSourceException -> {
                        ChannelListTransferEvent.TransferFailed(
                            destination = destination,
                            isImport = isImport,
                            error = ChannelListTransferError.UnsupportedTvSource
                        )
                    }
                    else -> {
                        ChannelListTransferEvent.TransferFailed(
                            destination = destination,
                            isImport = isImport,
                            error = ChannelListTransferError.UnknownError
                        )
                    }
                }
            }
        }
    }
}