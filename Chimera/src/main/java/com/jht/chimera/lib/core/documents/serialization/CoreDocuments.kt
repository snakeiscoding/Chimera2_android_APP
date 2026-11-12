package com.jht.chimera.lib.core.documents.serialization

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents all documents available on CORE for a given environment and club ID.
 *
 * @property tvChannelDocuments A map of TV source settings to their corresponding TV channel documents.
 */
@Serializable
data class CoreDocuments(
    @SerialName("tv_channels")
    @Serializable(with = TvChannelDocumentsSerializer::class)
    val tvChannelDocuments: Map<SettingKeys.TVSource, CoreDocument> = emptyMap()
)

/**
 * Represents a specific document on CORE with its associated URL.
 *
 * @property url The URL of the TV channel document.
 */
@Serializable
data class CoreDocument(
    val url: String
)

/**
 * Represents metadata for a specific document.
 *
 * @property url The URL where the document is hosted.
 * @property contentType The MIME type of the document, if available.
 * @property contentLength The size of the document in bytes, if available.
 * @property lastModified The last modified timestamp of the document, if available.
 * @property etag The entity tag (ETag) for cache validation, if available.
 */
@Serializable
data class CoreDocumentMetadata(
    val url: String,
    val contentType: String? = null,
    val contentLength: Long? = null,
    val lastModified: String? = null,
    val etag: String? = null
)