package com.jht.chimera.lib.tv.tuner.lg.lg750e

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Ignores unknown fields, so optional fields can be added without a new version. */
internal val lg750eJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

/**
 * The JSON protocol envelope, the same in both directions.
 *
 * @property tunerApi Tuner apps from before the JSON protocol only act on
 *   [Lg750ELegacyMessage.API_CLASSIC], so they ignore these messages.
 * @property id Counts up from 1 on each connection; echoed on the command's items, reply and error.
 * @property ver The version of [name] that [data] follows.
 * @property seq 0-based position of an item in its stream.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class Lg750EMessage(
    @EncodeDefault val tunerApi: String = API_JSON,
    val type: String,
    val id: Long? = null,
    val name: String,
    @EncodeDefault val ver: Int = 1,
    val seq: Int? = null,
    @EncodeDefault val data: JsonObject = JsonObject(emptyMap()),
) {
    fun encode(): String = lg750eJson.encodeToString(serializer(), this)

    companion object {
        const val API_JSON = "json"

        const val TYPE_HELLO = "hello"
        const val TYPE_COMMAND = "command"
        const val TYPE_REPLY = "reply"
        const val TYPE_ITEM = "item"
        const val TYPE_ERROR = "error"

        const val HELLO_CONSOLE = "console"
        const val HELLO_TUNER = "tuner"

        /** Values of an error's `data.code` after which the console falls back to classic. */
        const val ERROR_UNSUPPORTED_COMMAND = "unsupported_command"
        const val ERROR_UNSUPPORTED_VERSION = "unsupported_version"

        fun decode(text: String): Lg750EMessage? =
            try { lg750eJson.decodeFromString(serializer(), text).takeIf { it.tunerApi == API_JSON } } catch (e: IllegalArgumentException) { null }
    }
}

/** `data` of the console hello. */
@Serializable
internal data class Lg750EConsoleHello(
    val consoleType: String,
    val softwareVersion: String,
    val commands: Map<String, List<Int>>,
)

/**
 * The part of the tuner hello's `data` the console uses. The tuner's details arrive in the classic
 * `tunerInfo`, which every tuner sends.
 */
@Serializable
internal data class Lg750ETunerHello(
    val commands: Map<String, List<Int>> = emptyMap(),
)

/** The pre-JSON WebSocket message, which every console and tuner in the field understands. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class Lg750ELegacyMessage(
    @EncodeDefault val tunerApi: String = API_CLASSIC,
    val command: String? = null,
    val response: String? = null,
    val tunerInfo: TunerInfo? = null,
    val consoleInfo: ConsoleInfo? = null,
) {
    fun encode(): String = lg750eJson.encodeToString(serializer(), this)

    @Serializable
    data class TunerInfo(
        val tunerModel: String? = null,
        val tunerFirmware: String? = null,
        val tunerVersion: String? = null,
        val tunerIp: String? = null,
        val tunerMac: String? = null,
        val tunerMacAp: String? = null,
    )

    @Serializable
    data class ConsoleInfo(val consoleType: String? = null)

    companion object {
        const val API_CLASSIC = "classic"

        fun decode(text: String): Lg750ELegacyMessage? =
            try { lg750eJson.decodeFromString(serializer(), text).takeIf { it.tunerApi == API_CLASSIC } } catch (e: IllegalArgumentException) { null }
    }
}
