package com.jht.chimera.service.consoleconfig

import android.content.Context
import android.util.Log
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.file.WEB_WELCOME_VIDEOS_SUBFOLDER
import com.jht.chimera.service.ApiWelcomeScreenVideo
import com.jht.chimera.service.FacilityServiceApiClient
import com.jht.chimera.service.resumeDownloadFile
import com.jht.utility.runCatchingCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.lang.AutoCloseable
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "FacilityConfigHelper"
private const val VERBOSE_LOGS = false
// File API get extension does not include the dot
private const val FILE_EXT_PENDING = "PART"
private const val FILE_EXT_MP4 = "mp4"

/** Helper to handle Facility service functions for downloading configuration items like welcome videos */
class FacilityConfigHelper(val context: Context) : AutoCloseable {
    private val scopeIo = CoroutineScope(Dispatchers.IO)
    private val checkRunning = AtomicBoolean(false)
    private val localFolder = File(context.filesDir, WEB_WELCOME_VIDEOS_SUBFOLDER)
    private val downloadJobs = mutableMapOf<String, Job>() // map of video ID to download job
    private val useFullHD = ProductConfig.getConsoleType().heightPx < 2160
    private val apiClient = FacilityServiceApiClient()

    override fun close() {
        scopeIo.cancel()
    }

    fun interface IDigitalProductResponse{
        fun hasSeca(enabled: Boolean)
    }

    /** Sends calls to the facility config endpoints to check for enabled features and assets*/
    fun checkFacilityConfigEndpoints(iDigitalProductResponse: IDigitalProductResponse){
        if (checkRunning.getAndSet(true)) {
            Log.w(TAG, "Check routine already running")
            return
        }
        scopeIo.launch {
            runCatchingCancellable {
                // Should we cache the JSON to local file to resume downloads, or just always operate on a fresh pulled JSON?
                // get web data
                checkDigitalProducts(iDigitalProductResponse)
                checkUpdateWelcomeVideos()
            }.onFailure { Log.e(TAG, "Check routine crashed", it) }
            checkRunning.set(false)
        }
    }

    /** Runs in a coroutine to check web endpoint for digital product offerings */
    suspend fun checkDigitalProducts(iDigitalProductResponse: IDigitalProductResponse) {
        apiClient.apiGetDigitalProducts()?.let { webData ->
            try {
                val digitalProducts = JSONObject(webData).getJSONArray("digitalProducts")
                //Check for the SECA feature
                iDigitalProductResponse.hasSeca(digitalProducts.toString().contains("SECA"))
            } catch (e : JSONException){
                //f no digital products found
                iDigitalProductResponse.hasSeca(false)
                Log.e(TAG, "$webData $e")
            }
        }
    }

    /** Runs a coroutine to check web endpoint for updated video content and adjust the local copies */
    suspend fun checkUpdateWelcomeVideos() {
        apiClient.apiGetWelcomeScreenVideo()?.let { webData ->
            Log.d(TAG, "data> $webData")
            // cancel any unneeded download jobs
            val downloadIterator = downloadJobs.iterator()
            while (downloadIterator.hasNext()) {
                val entry = downloadIterator.next()
                if (webData.find { it.id == entry.key } == null) {
                    Log.d(TAG, "cancel download ${entry.key}")
                    entry.value.cancelAndJoin()
                    downloadIterator.remove()
                }
            }
            // remove items first to make room for new items (be sure to also delete partials)
            val localAllFiles = localFolder.listFiles()?.filter { it.isFile } // any need to protect unrelated files, could do just FILE_EXT_PENDING and FILE_EXT_MP4 ?
            localAllFiles?.forEach { file ->
                // if exist in local but not web delete it
                if (webData.find { it.id == extractIdFromFile(file) } == null) {
                    Log.d(TAG, "deleting $file")
                    runCatchingCancellable { file.delete() }.onFailure { Log.w(TAG,"Cannot delete $file") }
                }
            }
            // download new items (or resume partial downloads)
            localFolder.mkdirs() // folder needs to exist first
            val localFinalFiles = localAllFiles?.filter { it.extension != FILE_EXT_PENDING } // exclude FILE_EXT_PENDING, or explicitly include FILE_EXT_MP4 ?
            webData.filter { it.hasIdAndVideo() }.forEach { item ->
                // if exists in web but not local, queue download (include partial items in case download must be resumed)
                if (localFinalFiles?.find { extractIdFromFile(it) == item.id } == null) {
                    if (!downloadJobs.contains(item.id)) {
                        downloadJobs.put(item.id, createDownloadJob(item))
                    }
                }
            }
        }
    }

    /** extract the video ID from the file */
    private fun extractIdFromFile(file: File): String {
        return file.nameWithoutExtension
    }
    /** construct file from video ID */
    private fun fileFromId(id: String, extension: String): File {
        return File(localFolder, "$id.$extension")
    }

    private fun createDownloadJob(item: ApiWelcomeScreenVideo): Job {
        return scopeIo.launch {
            runCatchingCancellable {
                val url = if (useFullHD) item.fhdUrl else item.uhdUrl
                Log.d(TAG, "Download job ${item.id} FHD=$useFullHD started")
                val localFinalFile = fileFromId(item.id, FILE_EXT_MP4)
                val localPendingFile = fileFromId(item.id, FILE_EXT_PENDING)
                // download file to pending file
                apiClient.createHttpClient().use { client ->
                    client.resumeDownloadFile(url, localPendingFile) { progress ->
                        if (VERBOSE_LOGS) Log.d(TAG, "Downloading ${item.id} progress=$progress")
                    }
                }
                // rename pending file to final file
                localPendingFile.renameTo(localFinalFile)
                Log.d(TAG, "Download job ${item.id} completed")
            }.onFailure { Log.e(TAG, "Download failure in job ${item.id}", it) }
            // always remove the job, if it failed then the next check will try again
            downloadJobs.remove(item.id)
        }
    }

}

