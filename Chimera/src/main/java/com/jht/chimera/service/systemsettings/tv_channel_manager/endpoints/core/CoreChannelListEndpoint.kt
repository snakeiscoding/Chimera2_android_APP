package com.jht.chimera.service.systemsettings.tv_channel_manager.endpoints.core

import android.content.Context
import android.util.Log
import com.jht.chimera.lib.common.file.FileUtil
import com.jht.chimera.lib.core.documents.CoreDocumentsApi
import com.jht.oauthlib.auth.BksCredentialProvider
import com.jht.chimera.service.systemsettings.tv_channel_manager.ChannelListTransferError
import com.jht.chimera.service.systemsettings.tv_channel_manager.ChannelListTransferEvent
import com.jht.chimera.service.systemsettings.tv_channel_manager.accessors.ChannelListAccessor
import com.jht.chimera.service.systemsettings.tv_channel_manager.endpoints.ChannelListDestination
import com.jht.chimera.service.systemsettings.tv_channel_manager.endpoints.ChannelListEndpoint
import com.jht.firsttimesetup.Channel
import com.jht.data.ChannelListPreview
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import kotlin.time.Duration.Companion.seconds


/**
 * Implementation of [ChannelListEndpoint] for CORE.
 *
 * @property environment The DAPI environment used while communicating with CORE.
 * @property clubId The club ID used for retrieving the channel list.
 * @property documentsApi The instance of [CoreDocumentsApi] used for communicating with the CORE documents endpoint.
 * @property context The application context used for file operations and accessing resources.
 */
class CoreChannelListEndpoint(
    private val environment: Int,
    private val clubId: Int,
    private val context: Context,
    private val documentsApi: CoreDocumentsApi = CoreDocumentsApi(),
) : ChannelListEndpoint {

    companion object {
        private const val TAG = "CoreChannelList"
    }

    private val coreSecret: String by lazy {
        BksCredentialProvider(context).getString(BksCredentialProvider.ALIAS_CORE_CONSOLE_CONFIG_PASSWORD)
    }

    /**
     * Imports a channel list from CORE to the TV source.
     *
     * @param channelListAccessor The [ChannelListAccessor] instance for updating the TV source's channel list.
     * @return A [ChannelListTransferEvent] representing the result of the import operation.
     */
    override suspend fun importChannelList(channelListAccessor: ChannelListAccessor): ChannelListTransferEvent {
        var downloadFile: File? = null
        try {
            // Obtain the documents available for the given environment and club ID.
            val availableDocuments = documentsApi.getAllDocuments(
                token = documentsApi.authenticate(environment, coreSecret, 10.seconds.inWholeMilliseconds).token,
                environment = environment,
                clubId = clubId,
                timeout = 10.seconds.inWholeMilliseconds
            )

            // Attempt to locate the remote channel list document for the given TV source
            // Return early if the remote channel list document is not found
            val remoteChannelList = availableDocuments.tvChannelDocuments[channelListAccessor.getAssociatedTvSource()]
            remoteChannelList ?: return transferFailed(true, ChannelListTransferError.ChannelListNotFound)

            // Obtain the metadata for the remote channel list document
            val channelListMetadata = documentsApi.getDocumentMetadata(remoteChannelList.url, 10.seconds.inWholeMilliseconds)

            // Check if the local channel list is already up to date
            // Skip this step if the remote channel list document does not provide an ETag for comparison
            channelListMetadata.etag?.let { remoteHash ->
                val localChannelList = channelListAccessor.getChannelList()
                val localHash = FileUtil.calculateMD5(localChannelList)
                if (localHash == remoteHash) {
                    return transferFailed(true, ChannelListTransferError.ChannelListAlreadyUpToDate)
                }
            }

            // Download channel list to a temporary file
            // We do this so that the local channel list can be preserved in case of an error
            downloadFile = File(context.cacheDir, "channelList.tmp")
            documentsApi.downloadDocument(remoteChannelList.url, downloadFile, 60.seconds.inWholeMilliseconds)

            // Update the local channel list
            channelListAccessor.updateChannelList(downloadFile)

            // Notify the caller that the channel list was successfully imported
            return transferSuccessful(true)
        }
        catch (e : Exception) {
            return handleException(true, e)
        }
        finally {
            // Clean up temporary download file if it exists
            downloadFile?.also { if (it.exists()) { runCatching { it.delete() } } }
        }
    }

    /**
     * Exports the channel list of the TV source to CORE.
     *
     * @param channelListAccessor The [ChannelListAccessor] instance for accessing the TV source's channel list.
     * @return A [ChannelListTransferEvent] representing the result of the import operation.
     */
    override suspend fun exportChannelList(channelListAccessor: ChannelListAccessor): ChannelListTransferEvent {
        var cachedChannelList: File? = null
        try {
            // Cache the local channel list
            // We do this to ensure that modifications to the local channel list do not affect the export process
            val sourceChannelList = channelListAccessor.getChannelList()
            cachedChannelList = File(context.cacheDir, sourceChannelList.name)
            copyFileSafely(sourceChannelList, cachedChannelList)

            // Obtain the documents available for the given environment and club ID.
            val availableDocuments = documentsApi.getAllDocuments(
                token = documentsApi.authenticate(environment, coreSecret, 10.seconds.inWholeMilliseconds).token,
                environment = environment,
                clubId = clubId,
                timeout = 10.seconds.inWholeMilliseconds
            )

            // Attempt to locate the remote channel list and its metadata for the given TV source
            // Return early if we verify that the remote channel list is already up to date
            val remoteChannelList = availableDocuments.tvChannelDocuments[channelListAccessor.getAssociatedTvSource()]
            val channelListMetadata = remoteChannelList?.url?.let { documentsApi.getDocumentMetadata(it, 10.seconds.inWholeMilliseconds) }
            channelListMetadata?.etag?.let { remoteHash ->
                val localHash = FileUtil.calculateMD5(cachedChannelList)
                if (localHash == remoteHash) {
                    return transferFailed(false, ChannelListTransferError.ChannelListAlreadyUpToDate)
                }
            }

            // Upload the channel list to CORE
            val subType = channelListAccessor.getAssociatedTvSource().name.lowercase()
            Log.d(TAG, "Uploading channel list: file=${cachedChannelList.name} size=${cachedChannelList.length()}B subType=$subType clubId=$clubId env=$environment")
            Log.d(TAG, "Channel list content:\n${cachedChannelList.readText()}")
            documentsApi.uploadDocument(
                token = documentsApi.authenticate(environment, coreSecret, 10.seconds.inWholeMilliseconds).token,
                environment = environment,
                clubId = clubId,
                documentType = "tv_channels",
                documentSubType = subType,
                document = cachedChannelList,
                timeout = 60.seconds.inWholeMilliseconds
            )

            // Notify the caller that the channel list was successfully exported
            Log.d(TAG, "Upload succeeded")
            return transferSuccessful(false)
        }
        catch (e: Exception) {
            Log.e(TAG, "Export failed: ${e.message}", e)
            Log.e(TAG, "Cause: ${e.cause?.message}")
            return handleException(false, e)
        }
        finally {
            // Clean up cached channel list if it exists
            cachedChannelList?.also { if (it.exists()) runCatching { it.delete() } }
        }
    }

    /**
     * Previews the channel list from CORE without applying it to the TV source.
     * Downloads and parses the channel list, returning the channels for display.
     *
     * @param channelListAccessor The accessor for the TV source to preview channels for.
     * @return A [ChannelListPreview] containing the channels and default channel.
     * @throws ChannelListNotFoundException if no channel list exists on CORE for the TV source.
     * @throws CoreDocumentsApi.CoreDocumentsApiException if a CORE API error occurs.
     * @throws ChannelListAccessor.ChannelListAccessException if parsing fails.
     */
    override suspend fun previewChannelList(channelListAccessor: ChannelListAccessor): ChannelListPreview {
        var downloadFile: File? = null
        try {
            // Authenticate and get available documents
            val availableDocuments = documentsApi.getAllDocuments(
                token = documentsApi.authenticate(environment, coreSecret, 10.seconds.inWholeMilliseconds).token,
                environment = environment,
                clubId = clubId,
                timeout = 10.seconds.inWholeMilliseconds
            )

            // Find the CORE channel list for the requested TV source
            val tvSource = channelListAccessor.getAssociatedTvSource()
            val remoteChannelList = availableDocuments.tvChannelDocuments[tvSource]
                ?: throw ChannelListNotFoundException("No channel list found for TV source: $tvSource")

            // Download to temp file
            downloadFile = File(context.cacheDir, "preview_channels.tmp")
            documentsApi.downloadDocument(
                documentUrl = remoteChannelList.url,
                outputDocument = downloadFile,
                timeout = 60.seconds.inWholeMilliseconds
            )

            // Use the accessor to parse the channel list (format-specific parsing)
            val parsedChannels = channelListAccessor.parseChannelList(downloadFile)

            // Map to Channel objects and find default
            val channels = parsedChannels.map { iChannel ->
                Channel(
                    name = iChannel.name(),
                    number = iChannel.number(),
                    isHidden = !iChannel.enabled(),
                    isDefault = false
                )
            }
            val defaultChannel = channels.firstOrNull { !it.isHidden }

            return ChannelListPreview(
                channels = channels,
                defaultChannel = defaultChannel
            )
        } finally {
            downloadFile?.let { if (it.exists()) runCatching { it.delete() } }
        }
    }

    /**
     * Exception thrown when a channel list is not found on CORE.
     */
    class ChannelListNotFoundException(message: String) : Exception(message)

    /**
     * Closes any underlying resources utilized by the CoreChannelListEndpoint.
     */
    override fun close() {
        documentsApi.close()
    }

    /**
     * Centralizes exception handling for channel list transfer operations.
     *
     * @param isImport True if the operation encountering the exception was an import; false if it was an export.
     * @param e The exception thrown during the channel list transfer operation.
     * @return A [ChannelListTransferEvent] representing a failed transfer with the appropriate error type.
     */
    private fun handleException(isImport: Boolean, e: Exception): ChannelListTransferEvent {
        return when(e) {
            // Handle exceptions related to CoreDocumentsApi
            is CoreDocumentsApi.CoreDocumentsApiException -> {
                when (val cause = e.cause) {
                    // Client-side exceptions (400 Bad Request, 401 Unauthorized, etc)
                    is ClientRequestException -> {
                        val statusCode = cause.response.status
                        Log.e(TAG, "HTTP ${statusCode.value} ${statusCode.description}: ${cause.message}")
                        if (statusCode == HttpStatusCode.Unauthorized || statusCode == HttpStatusCode.Forbidden) {
                            transferFailed(isImport, ChannelListTransferError.AuthenticationError)
                        } else {
                            transferFailed(isImport)
                        }
                    }
                    // Network timeout exception
                    is HttpRequestTimeoutException -> {
                        Log.e(TAG, "Request timed out: ${cause.message}")
                        transferFailed(isImport, ChannelListTransferError.NetworkError)
                    }
                    // Unknown exceptions
                    else -> {
                        Log.e(TAG, "Unknown cause: ${cause?.message}")
                        transferFailed(isImport)
                    }
                }
            }

            // Handle unknown exceptions
            else -> transferFailed(isImport)
        }
    }

    /**
     * Creates a TransferFailed event indicating that a channel list transfer has failed.
     *
     * @param isImport True if the failed transfer was an import operation; false for export.
     * @param error The error encountered during the transfer; defaults to UnknownError.
     * @return A [ChannelListTransferEvent.TransferFailed] event with the specified parameters.
     */
    private fun transferFailed(
        isImport: Boolean,
        error: ChannelListTransferError = ChannelListTransferError.UnknownError
    ): ChannelListTransferEvent {
        return ChannelListTransferEvent.TransferFailed(
            destination = ChannelListDestination.INTERNET,
            isImport = isImport,
            error = error
        )
    }

    /**
     * Creates a TransferSuccessful event indicating that a channel list transfer has succeeded.
     *
     * @param isImport True if the successful transfer was an import operation; false for export.
     * @return A [ChannelListTransferEvent.TransferSuccessful] event with the specified parameters.
     */
    private fun transferSuccessful(
        isImport: Boolean,
    ): ChannelListTransferEvent {
        return ChannelListTransferEvent.TransferSuccessful(
            destination = ChannelListDestination.INTERNET,
            isImport = isImport
        )
    }

    /**
     * Copies [sourceFile] to [destinationFile] safely by acquiring a shared lock on the source file.
     *
     * @param sourceFile The file to copy from.
     * @param destinationFile The file to copy to. If it exists, it will be overwritten.
     */
    private suspend fun copyFileSafely(sourceFile: File, destinationFile: File) = withContext(Dispatchers.IO) {
        RandomAccessFile(sourceFile, "r").channel.use { channel ->
            channel.lock(0L, Long.MAX_VALUE, true).use {
                sourceFile.copyTo(destinationFile, overwrite = true)
            }
        }
    }
}