@file:UseSerializers(InstantSerializer::class, ApiFacilityMessageTypeSerializer::class)
package com.jht.chimera.service

import android.util.Log
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.utility.runCatchingCancellable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.InternalAPI
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.isActive
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import kotlin.math.min

private const val TAG = "FacilityServiceApiClient"

/** General API client for the 'facility.jht.services' endpoint */
class FacilityServiceApiClient { // should we be AutoClosable? for now we are creating/releasing resources per operation so not yet?
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    fun getFacilityId(): Int {
        return DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT)
    }
    fun getHost(): String {
        val environmentOrdinal = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)
        val environment = JHTCommonTypes.DAPIEnvironment.entries.getOrNull(environmentOrdinal)
        val url = when (environment) {
            JHTCommonTypes.DAPIEnvironment.production -> "facility.jht.services"
            JHTCommonTypes.DAPIEnvironment.staging -> "staging-facility.jhtdev.services"
            JHTCommonTypes.DAPIEnvironment.qa -> "qa-facility.jhtdev.services"
            else -> "dev-facility.jhtdev.services"
        }
        return url
    }
    fun createHttpClient(): HttpClient {
        return HttpClient(Android) {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    /** generic API GET */
    private suspend inline fun <reified T> apiGet(path: String): T? {
        runCatchingCancellable {
            createHttpClient().use { client ->
                val response = client.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = getHost()
                        path(path)
                        headers {
                            append(HttpHeaders.Accept, "application/json")
                        }
                    }
                }
                if (response.status.isSuccess()) return response.body()
                else Log.e(TAG, "GET API failed '${response.status}' for '$path'")
            }
        }.onFailure { Log.e(TAG, "Failed to GET '$path'", it) }
        return null
    }

    suspend fun apiGetDigitalProducts(facilityId: Int  = getFacilityId()): String? {
        return apiGet("console/facility/$facilityId")
    }

    suspend fun apiGetWelcomeScreenVideo(facilityId: Int = getFacilityId()): List<ApiWelcomeScreenVideo>? {
        return apiGet("console/facility/$facilityId/welcome_screen_video")
    }

    suspend fun apiGetMessage(facilityId: Int = getFacilityId()): List<ApiFacilityMessage>? {
        return apiGet("console/facility/$facilityId/message")
    }
}


// https://stackoverflow.com/a/66581165
/** Extension resumes downloading a file based on the assumption that file downloads start to finish and any current file size can be assumed as progress */
@OptIn(InternalAPI::class)
suspend fun HttpClient.resumeDownloadFile(url: String, outFile: File, chunkSize: Int = 1024 * 1024, onProgress: (Float)->Unit = {}) {
    val length = head(urlString = url).headers[HttpHeaders.ContentLength]?.toLong() as Long
    val lastByte = length - 1
    var offset = outFile.length()
    if (offset >= length) {
        // file is complete already, don't touch it
        if (offset > length) Log.w(TAG, "Local file $offset is larger than remote $length")
        return
    }
    FileOutputStream(outFile, true).use { output ->
        while (isActive) {
            onProgress(maxOf(minOf(offset.toFloat() / length, 1f), 0f))
            val end = min(offset + chunkSize - 1, lastByte)
            get(url) {
                header(HttpHeaders.Range, "bytes=${offset}-${end}")
            }.content.copyTo(output)
            if (end >= lastByte) break
            offset += chunkSize
        }
    }
}


/** [ApiFacilityMessage.messageType] as defined in https://jhtnaee.atlassian.net/wiki/spaces/WD/pages/5555683779/Message+Center */
enum class ApiFacilityMessageType {
    WORKOUT_START, WORKOUT_END, RECURRING_MESSAGE, SINGLE_MESSAGE,
    ; // END LIST
    companion object {
        fun fromString(value: String?): ApiFacilityMessageType? = entries.find { it.name == value }
    }
}

/** Serialization for [ApiFacilityMessageType] which will deserialize to null if value is unknown */
private object ApiFacilityMessageTypeSerializer : KSerializer<ApiFacilityMessageType?> { // do we rely on built-in enum serialization for this enum or do it manually for more control?
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ApiFacilityMessageType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ApiFacilityMessageType?) { value?.let { encoder.encodeString(it.name) } }
    override fun deserialize(decoder: Decoder): ApiFacilityMessageType? = ApiFacilityMessageType.fromString(decoder.decodeString())
}

/** Handle serialization of Instant */
private object InstantSerializer : KSerializer<Instant?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant?) {
        value?.let { encoder.encodeString(it.toString()) }
    }
    override fun deserialize(decoder: Decoder): Instant? {
        val valueString = decoder.decodeString()
        // check standard format first
        var value = runCatchingCancellable {
            Instant.parse(valueString)
        }.getOrNull()
        // fallback to non-standard format (can probably remove once finalized)
        if (value == null) {
            value = runCatchingCancellable {
                val formatter = DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter()
                val localDateTime = LocalDateTime.parse(valueString, formatter)
                localDateTime.toInstant(ZoneOffset.UTC)
            }.getOrNull()
        }
        return value
    }
}

@Serializable
data class ApiWelcomeScreenVideo(
    val id: String = "",
    val facilityId: Int = 0,
    // Note: the headers getting video URL will have content type, right now they are assumed to be mp4
    val fhdUrl: String = "",
    val uhdUrl: String = "",
) {
    fun hasValidId() = id.isNotEmpty()
    fun hasVideo() = fhdUrl.isNotEmpty() && uhdUrl.isNotEmpty()
    fun hasIdAndVideo() = hasValidId() && hasVideo()
}


@Serializable
data class ApiFacilityMessage(
    val id: Int = 0,
    val facilityId: Int? = null,
    val groupId: Int? = null,
    val messageType: ApiFacilityMessageType? = null,
    val title: String = "",
    val message: String = "",
    val callToActionURL: String? = null,
    val callToActionText: String? = null,
    @SerialName("duration") val durationSeconds: Int = 0,
    val startAt: Instant? = null,
    val endAt: Instant? = null,
    val recurrence: ApiFacilityMessageRecurrence? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
) {
    fun isAllowed(now: Instant = Instant.now()): Boolean {
        // do we ignore if no start time? end time can be unspecified but start is assumed to be set
        if (startAt != null && startAt <= now) {
            if (endAt == null || endAt >= now) {
                // for now assume if there is no recurrence that there is no recurrence restriction
                if (recurrence == null || recurrence.isAllowed(now)) {
                    return true
                }
            }
        }
        return false
    }
}

@Serializable
data class ApiFacilityMessageRecurrence(
    val sunday: Boolean = false,
    val monday: Boolean = false,
    val tuesday: Boolean = false,
    val wednesday: Boolean = false,
    val thursday: Boolean = false,
    val friday: Boolean = false,
    val saturday: Boolean = false,
) {
    fun isAllowed(now: Instant = Instant.now(), useLocalTime: Boolean = true): Boolean {
        val zone = if (useLocalTime) ZoneId.systemDefault() else ZoneId.of("UTC")
        return when (now.atZone(zone).dayOfWeek) {
            DayOfWeek.SUNDAY -> sunday
            DayOfWeek.MONDAY -> monday
            DayOfWeek.TUESDAY -> tuesday
            DayOfWeek.WEDNESDAY -> wednesday
            DayOfWeek.THURSDAY -> thursday
            DayOfWeek.FRIDAY -> friday
            DayOfWeek.SATURDAY -> saturday
        }
    }
}

