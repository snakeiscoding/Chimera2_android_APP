package com.jht.chimera.lib.core.documents

import com.jht.chimera.lib.common.dapi.DapiUtil
import com.jht.chimera.lib.common.inputstream.CountingInputStream
import com.jht.chimera.lib.core.documents.serialization.AuthRequest
import com.jht.chimera.lib.core.documents.serialization.AuthResponse
import com.jht.chimera.lib.core.documents.serialization.CoreDocumentMetadata
import com.jht.chimera.lib.core.documents.serialization.CoreDocuments
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.streams.asInput
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLConnection
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * API client for interacting with the CORE Documents Endpoint.
 */
class CoreDocumentsApi(engine: HttpClientEngine = Android.create()): AutoCloseable {
    // --- Instance variables ---
    private val httpClient: HttpClient by lazy {
        HttpClient(engine) {
            expectSuccess = true
            install(HttpTimeout)
            install(ContentNegotiation) {
                json(Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    /**
     * Authenticates with the CORE Documents Endpoint.
     *
     * @param environment The DAPI environment identifier.
     * @param secret The CORE console config password (from BKS keystore).
     * @param timeout The timeout duration for the request in milliseconds.
     * @return [AuthResponse] on successful authentication.
     * @throws CoreDocumentsApiException if the authentication request fails (e.g., due to network errors, timeouts, or serialization issues).
     */
    @Throws(CoreDocumentsApiException::class)
    suspend fun authenticate(
        environment: Int,
        secret: String,
        timeout: Long = 60.seconds.inWholeMilliseconds
    ): AuthResponse {
        return try {
            httpClient.post("${baseURL(environment)}machine-auth") {
                timeout { requestTimeoutMillis = timeout }
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(secret))
            }.body()
        } catch (e: Exception) {
            throw CoreDocumentsApiException("Failed to authenticate with CORE documents endpoint", e)
        }
    }

    /**
     * Obtains the collection of documents for a given environment and club ID.
     *
     * @param token Authorization token.
     * @param environment The DAPI environment identifier.
     * @param clubId The identifier for the club.
     * @param timeout The timeout duration for the request in milliseconds.
     * @return [CoreDocuments] containing the documents for the specified CORE environment.
     * @throws CoreDocumentsApiException if the retrieval fails (e.g., due to network errors, timeouts, or serialization issues).
     */
    @Throws(CoreDocumentsApiException::class)
    suspend fun getAllDocuments(
        token: String,
        environment: Int,
        clubId: Int,
        timeout: Long = 60.seconds.inWholeMilliseconds
    ): CoreDocuments {
        return try {
            httpClient.get("${baseURL(environment)}document/$clubId") {
                timeout { requestTimeoutMillis = timeout }
                accept(ContentType.Application.Json)
                headers { append("Authorization", token) }
            }.body()
        } catch (e: Exception) {
            throw CoreDocumentsApiException("Failed to retrieve CORE documents for environment $environment and club ID $clubId", e)
        }
    }

    /**
     * Retrieves metadata for a specific document.
     *
     * @param documentUrl The URL of the document.
     * @param timeout The timeout duration for the request in milliseconds.
     * @return [CoreDocumentMetadata] containing the document's metadata.
     * @throws CoreDocumentsApiException if the metadata retrieval fails.
     */
    @Throws(CoreDocumentsApiException::class)
    suspend fun getDocumentMetadata(
        documentUrl: String,
        timeout: Long = 60.seconds.inWholeMilliseconds
    ): CoreDocumentMetadata {
        return try {
            httpClient.head(documentUrl) {
                timeout { requestTimeoutMillis = timeout }
            }.let { response ->
                CoreDocumentMetadata(
                    url = documentUrl,
                    contentType = response.headers["Content-Type"],
                    contentLength = response.headers["Content-Length"]?.toLongOrNull(),
                    lastModified = response.headers["Last-Modified"],
                    etag = response.headers["ETag"].let { it
                        ?.removePrefix("W/")
                        ?.removePrefix("/")
                        ?.replace("\"", "")
                    }
                )
            }
        } catch (e: Exception) {
            throw CoreDocumentsApiException("Failed to retrieve metadata for document $documentUrl", e)
        }
    }

    /**
     * Downloads a document from the specified URL.
     *
     * @param documentUrl The URL of the document.
     * @param outputDocument The file where the document will be saved.
     * @param timeout The timeout duration for the request in milliseconds.
     * @param onProgress A callback to report download progress.
     * @return The number of bytes downloaded to [outputDocument].
     * @throws CoreDocumentsApiException if the download fails (e.g., due to network errors, timeouts, or I/O issues).
     */
    @Throws(CoreDocumentsApiException::class)
    suspend fun downloadDocument(
        documentUrl: String,
        outputDocument: File,
        timeout: Long = 5.minutes.inWholeMilliseconds,
        onProgress: (Float) -> Unit = {}
    ): Long {
        return try {
            httpClient.prepareGet {
                url(documentUrl)
                timeout { requestTimeoutMillis = timeout }
                onDownload { bytesSentTotal, contentLength ->
                    val progress = (bytesSentTotal.toFloat() / contentLength.toFloat())
                    if (progress.isFinite() && progress >= 0F) {
                        onProgress(progress)
                    }
                }
            }.execute { response ->
                runCatching { outputDocument.parentFile?.mkdirs() }
                outputDocument.outputStream().use { response.bodyAsChannel().copyTo(it) }
            }
        } catch (e: Exception) {
            throw CoreDocumentsApiException("Failed to download document $documentUrl", e)
        }
    }

    /**
     * Uploads a document to the CORE Documents Endpoint.
     *
     * @param token Authorization token.
     * @param environment The DAPI environment identifier.
     * @param clubId The club identifier.
     * @param documentType The type of the document.
     * @param documentSubType The subtype of the document.
     * @param document The document file to upload.
     * @param timeout The timeout duration for the request in milliseconds.
     * @param onProgress A callback to report upload progress.
     * @return The number of bytes uploaded to the CORE Documents Endpoint.
     * @throws CoreDocumentsApiException if the upload fails (e.g., due to network errors, timeouts, or I/O issues).
     */
    @Throws(CoreDocumentsApiException::class)
    suspend fun uploadDocument(
        token: String,
        environment: Int,
        clubId: Int,
        documentType: String,
        documentSubType: String,
        document: File,
        timeout: Long = 5.minutes.inWholeMilliseconds,
        onProgress: (Float) -> Unit = {}
    ): Long {
        return try {
            val countingInputStream = CountingInputStream(document.inputStream())
            httpClient.submitFormWithBinaryData(
                url = "${baseURL(environment)}document/console-upload",
                formData = formData {
                    append("club_id", clubId)
                    append("document_type", documentType)
                    append("document_subtype", documentSubType)
                    append(
                        key = "document",
                        value = InputProvider(document.length()) { countingInputStream.asInput() },
                        headers = headers {
                            append(HttpHeaders.ContentType, URLConnection.guessContentTypeFromName(document.name) ?: "application/octet-stream")
                            append(HttpHeaders.ContentDisposition, "filename=\"${document.name}\"")
                        }
                    )
                }
            ) {
                headers { append("Authorization", token) }
                timeout { requestTimeoutMillis = timeout }
                onUpload { bytesSentTotal, contentLength ->
                    val progress = (bytesSentTotal.toFloat() / contentLength.toFloat())
                    if (progress.isFinite() && progress >= 0F) {
                        onProgress(progress)
                    }
                }
            }
            countingInputStream.bytesRead
        } catch (e: Exception) {
            throw CoreDocumentsApiException("Failed to upload document", e)
        }
    }

    /**
     * Closes the API client to ensure resources properly released.
     */
    override fun close() {
        httpClient.close()
    }

    /*
     * Builds the base URL for the CORE Documents Endpoint.
     */
    private fun baseURL(environment: Int): String = DapiUtil.getDapiConfigUrlPOST(environment)

    /**
     * Exception class for wrapping exceptions related to the CORE Documents API.
     *
     * This exception is thrown when any request to the CORE Documents Endpoint fails.
     */
    class CoreDocumentsApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
}