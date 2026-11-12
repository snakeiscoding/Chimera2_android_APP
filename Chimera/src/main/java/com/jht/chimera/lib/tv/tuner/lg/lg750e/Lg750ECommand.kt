package com.jht.chimera.lib.tv.tuner.lg.lg750e

import com.jht.chimera.lib.common.datamatrixkeys.TVKeys
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannel
import com.jht.serialport.peripheral.tuner.ITuner
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Every command the console sends to the LG750E tuner, in both encodings: [classic], the original
 * `NN:args\n` strings, and [data], the JSON payload for each of [versions]. A released version's
 * payload never changes; a breaking change adds a version.
 * Protocol: https://jhtnaee.atlassian.net/wiki/spaces/TT/pages/7373586626/STB-750E+Communication
 */
@Serializable
sealed class Lg750ECommand {
    /** Name in the JSON protocol. Several commands can share one. */
    abstract val name: String

    /** Never remove a version that tuners in the field may still need. */
    open val versions: List<Int> get() = listOf(1)

    abstract fun classic(): List<String>

    open fun data(ver: Int): JsonObject = EMPTY

    /** A command whose reply is published to the same DataMatrix keys as its classic reply. */
    @Serializable
    sealed class Query : Lg750ECommand() {
        /** Skips empty values, as the classic router does. */
        abstract fun publishReply(ver: Int, data: JsonObject, publish: (TVKeys, String) -> Unit)
    }

    @Serializable
    data object Reboot : Lg750ECommand() {
        override val name get() = "reboot"
        override fun classic() = listOf("00\n")
    }

    @Serializable
    data class KeyPress(val key: Lg750EKey, val count: Int = 1) : Lg750ECommand() {
        override val name get() = "keyPress"
        override fun classic() = listOf(if (count == 1) "01:${key.code}\n" else "01:${key.code},$count\n")
        override fun data(ver: Int) = buildJsonObject {
            put("key", key.name)
            put("count", count)
        }
    }

    /** [level] is 0 to 100. */
    @Serializable
    data class SetVolume(val level: Int) : Lg750ECommand() {
        override val name get() = "setVolume"
        override fun classic() = listOf("02:$level\n")
        override fun data(ver: Int) = buildJsonObject { put("level", level) }
    }

    @Serializable
    data object GetVersion : Query() {
        override val name get() = "getVersion"
        override fun classic() = listOf("06\n")
        override fun publishReply(ver: Int, data: JsonObject, publish: (TVKeys, String) -> Unit) {
            val appVersion = data.string("appVersion") ?: return
            // The classic reply's format, which the settings screens display.
            publish(TVKeys.DATA_TUNER_VERSION_STRING, "$appVersion|fw:${data.string("firmwareVersion").orEmpty()}")
        }
    }

    @Serializable
    data object GetNetwork : Query() {
        override val name get() = "getNetwork"
        override fun classic() = listOf("07\n")
        override fun publishReply(ver: Int, data: JsonObject, publish: (TVKeys, String) -> Unit) {
            // IP last: callers wait on it and expect the rest to have arrived.
            listOf(
                "dhcp" to TVKeys.EVENT_TUNER_DHCP_STRING,
                "subnet" to TVKeys.EVENT_TUNER_SUBNET_STRING,
                "gateway" to TVKeys.EVENT_TUNER_GATEWAY_STRING,
                "dns1" to TVKeys.EVENT_TUNER_DNS1_STRING,
                "dns2" to TVKeys.EVENT_TUNER_DNS2_STRING,
                "ip" to TVKeys.EVENT_TUNER_IP_ADDRESS_STRING,
            ).forEach { (field, key) -> data.string(field)?.let { publish(key, it) } }
        }
    }

    /** [index] 0 turns captions off. */
    @Serializable
    data class SetClosedCaption(val index: Int) : Lg750ECommand() {
        override val name get() = "setClosedCaption"
        override fun classic() = listOf("08:$index\n")
        override fun data(ver: Int) = buildJsonObject { put("index", index) }
    }

    /** Makes the current channel the tuner's power-on channel. */
    @Serializable
    data object SaveDefaultChannel : Lg750ECommand() {
        override val name get() = "saveDefaultChannel"
        override fun classic() = listOf("11\n")
    }

    @Serializable
    data object EnableDhcp : Lg750ECommand() {
        override val name get() = SET_NETWORK
        override fun classic() = listOf("26:01\n")
        override fun data(ver: Int) = buildJsonObject { put("dhcp", true) }
    }

    @Serializable
    data class SetStaticNetwork(
        val ip: String,
        val subnet: String,
        val gateway: String,
        val dns1: String,
        val dns2: String,
    ) : Lg750ECommand() {
        override val name get() = SET_NETWORK
        override fun classic() = listOf("21:$ip\n", "22:$subnet\n", "23:$gateway\n", "24:$dns1\n", "25:$dns2\n", "26:00\n")
        override fun data(ver: Int) = buildJsonObject {
            put("dhcp", false)
            put("ip", ip)
            put("subnet", subnet)
            put("gateway", gateway)
            put("dns1", dns1)
            put("dns2", dns2)
        }
    }

    @Serializable
    data object GetMac : Query() {
        override val name get() = "getMac"
        override fun classic() = listOf("27\n")
        override fun publishReply(ver: Int, data: JsonObject, publish: (TVKeys, String) -> Unit) {
            data.string("mac")?.let { publish(TVKeys.EVENT_TUNER_MAC_ADDRESS_STRING, it) }
        }
    }

    /** Reboots the tuner if the resolution changes. [resolution] is a `TunerProperties.OUTPUT_RESOLUTION` name. */
    @Serializable
    data class SetResolution(val resolution: String) : Lg750ECommand() {
        override val name get() = "setResolution"
        override fun classic() = listOf("28:$resolution\n")
        override fun data(ver: Int) = buildJsonObject { put("resolution", resolution.removePrefix("OUTPUT_RESOLUTION_")) }
    }

    @Serializable
    data object GetModel : Query() {
        override val name get() = "getModel"
        override fun classic() = listOf("29\n")
        override fun publishReply(ver: Int, data: JsonObject, publish: (TVKeys, String) -> Unit) {
            data.string("model")?.let { publish(TVKeys.EVENT_TUNER_MODEL_STRING, it) }
        }
    }

    /** [x] and [y] are normalized to 0..1. */
    @Serializable
    data class MoveMouse(val x: Float, val y: Float) : Lg750ECommand() {
        override val name get() = "moveMouse"
        // The tuner requires a period as the decimal separator.
        override fun classic() = listOf(String.format(Locale.US, "30:v2,%.3f,%.3f\n", x, y))
        override fun data(ver: Int) = buildJsonObject {
            put("x", x.toThousandths())
            put("y", y.toThousandths())
        }
    }

    @Serializable
    data object ClickMouse : Lg750ECommand() {
        override val name get() = "clickMouse"
        override fun classic() = listOf("31\n")
    }

    /** [app] is a tuner app keyword, such as `youtube` or `casting`. */
    @Serializable
    data class LaunchApp(val app: String) : Lg750ECommand() {
        override val name get() = "launchApp"
        override fun classic() = listOf("32:$app\n")
        override fun data(ver: Int) = buildJsonObject { put("app", app) }
    }

    /** Both empty turns the soft AP off. The classic form cannot carry a comma in either value. */
    @Serializable
    data class SetSoftAp(val ssid: String, val password: String) : Lg750ECommand() {
        override val name get() = "setSoftAp"
        override fun classic() = listOf("34:$ssid,$password\n")
        override fun toString() = "SetSoftAp(ssid=$ssid, password=${if (password.isEmpty()) "" else "***"})"
        override fun data(ver: Int) = buildJsonObject {
            val enabled = ssid.isNotEmpty() || password.isNotEmpty()
            put("enabled", enabled)
            if (enabled) {
                put("ssid", ssid)
                put("password", password)
            }
        }
    }

    /** Clears the previous user's app logins. */
    @Serializable
    data object ClearUserData : Lg750ECommand() {
        override val name get() = "clearUserData"
        override fun classic() = listOf("35\n")
    }

    /** [locale] is an Android locale tag, such as `ko-KR`. */
    @Serializable
    data class SetLocale(val locale: String) : Lg750ECommand() {
        override val name get() = "setLocale"
        override fun classic() = listOf("36:$locale\n")
        override fun data(ver: Int) = buildJsonObject { put("locale", locale) }
    }

    /** The JSON reply streams one `item` per RF channel before the final reply. */
    @Serializable
    data object GetChannelList : Lg750ECommand() {
        override val name get() = "getChannelList"
        override fun classic() = listOf("41\n")
    }

    /** Build with [tune]. [fields] are in classic order, under their JSON names. */
    @Serializable
    data class Tune(val channelClass: String, val fields: List<Pair<String, String>>) : Lg750ECommand() {
        override val name get() = "tune"
        override fun classic(): List<String> {
            val command = if (channelClass.startsWith("IP")) "42" else "43"
            return listOf((listOf(command, channelClass) + fields.map { it.second }).joinToString(":", postfix = "\n"))
        }
        override fun data(ver: Int) = buildJsonObject {
            put("channelClass", channelClass)
            fields.forEach { (field, value) ->
                put(field, if (field in NUMERIC_TUNE_FIELDS) value.asJsonNumber() else JsonPrimitive(value.lowercase(Locale.ROOT)))
            }
        }
    }

    companion object {
        private const val SET_NETWORK = "setNetwork"
        private val EMPTY = JsonObject(emptyMap())

        /** The other tune fields are sent lowercase, as the tuner uses them. */
        private val NUMERIC_TUNE_FIELDS = setOf("logicalNumber", "port", "frequency", "programNumber", "majorNumber", "minorNumber", "satelliteId", "plpId")

        private val relayJson = Json { classDiscriminator = "command" }

        /**
         * Advertised in the console hello. Lazy because the command objects cannot be built while
         * this class is still initializing.
         */
        val SUPPORTED: Map<String, List<Int>> by lazy {
            listOf(
                Reboot, KeyPress(Lg750EKey.ENTER), SetVolume(0), GetVersion, GetNetwork, SetClosedCaption(0),
                SaveDefaultChannel, EnableDhcp, GetMac, SetResolution(""), GetModel, MoveMouse(0f, 0f), ClickMouse,
                LaunchApp(""), SetSoftAp("", ""), ClearUserData, SetLocale(""), GetChannelList, Tune("", emptyList()),
            ).associate { it.name to it.versions }
        }

        /** Null for a channel type the LG750E cannot tune. */
        @JvmStatic
        fun tune(channel: LgChannel): Tune? = when (channel) {
            is LgChannel.IpChannel.Class1 -> Tune("IP1", listOf("logicalNumber" to channel.logicalNumber, "ipBroadcastType" to channel.broadcastType.name))
            is LgChannel.IpChannel.Class2 -> Tune("IP2", listOf("ip" to channel.ip, "port" to channel.port, "ipBroadcastType" to channel.broadcastType.name))
            is LgChannel.RfChannel.Class1 -> Tune("RF1", listOf("logicalNumber" to channel.logicalNumber, "rfBroadcastType" to channel.broadcastType.name))
            is LgChannel.RfChannel.Class2 -> Tune("RF2", listOf("frequency" to channel.frequency, "programNumber" to channel.programNumber, "rfBroadcastType" to channel.broadcastType.name))
            is LgChannel.RfChannel.Class3 -> Tune("RF3", listOf("majorNumber" to channel.major, "minorNumber" to channel.minor, "rfBroadcastType" to channel.broadcastType.name))
            is LgChannel.RfChannel.Class4 -> Tune("RF4", listOf(
                "frequency" to channel.frequency, "programNumber" to channel.programNumber, "satelliteId" to channel.satelliteId,
                "polarization" to channel.polarization.name, "rfBroadcastType" to channel.broadcastType.name,
            ))
            is LgChannel.RfChannel.Class5 -> Tune("RF5", listOf(
                "frequency" to channel.frequency, "programNumber" to channel.programNumber, "plpId" to channel.plpId,
                "rfBroadcastType" to channel.broadcastType.name,
            ))
            is LgChannel.RfChannel.Class6 -> Tune("RF6", listOf(
                "majorNumber" to channel.major, "minorNumber" to channel.minor, "plpId" to channel.plpId,
                "rfBroadcastType" to channel.broadcastType.name,
            ))
            else -> null
        }

        fun toRelay(command: Lg750ECommand): String = relayJson.encodeToString(serializer(), command)

        /** Null if [relay] is not a command. */
        fun fromRelay(relay: String): Lg750ECommand? =
            try { relayJson.decodeFromString(serializer(), relay) } catch (e: IllegalArgumentException) { null }

        private fun JsonObject.string(field: String): String? =
            (this[field] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() }

        private fun Float.toThousandths(): Double = (this * 1000).roundToInt() / 1000.0

        /** Left as a string when not numeric, so the tuner rejects it with an error. */
        private fun String.asJsonNumber(): JsonElement =
            toLongOrNull()?.let { JsonPrimitive(it) } ?: toDoubleOrNull()?.let { JsonPrimitive(it) } ?: JsonPrimitive(this)
    }
}

/** [name] is the tuner's IDCAP key name, used in JSON; [code] is the HCAP code classic sends. */
enum class Lg750EKey(val code: Int) {
    ZERO(48), ONE(49), TWO(50), THREE(51), FOUR(52), FIVE(53), SIX(54), SEVEN(55), EIGHT(56), NINE(57),
    CHANNEL_UP(427), CHANNEL_DOWN(428), LAST_CHANNEL(711),
    VOLUME_UP(447), VOLUME_DOWN(448), MUTE(449), TOGGLE_CC(1008),
    UP(38), DOWN(40), LEFT(37), RIGHT(39), ENTER(13),
    MENU(18), POWER(409), EXIT(1001), INFO(457),
    RED(403), GREEN(404), YELLOW(405), BLUE(406),
    SAP(723), BACK(461), GUIDE(458);

    companion object {
        /** Null for buttons the LG750E lacks, such as `DASH`. */
        @JvmStatic
        fun forRemoteButton(button: ITuner.REMOTE_BUTTON): Lg750EKey? = when (button) {
            ITuner.REMOTE_BUTTON.ZERO -> ZERO
            ITuner.REMOTE_BUTTON.ONE -> ONE
            ITuner.REMOTE_BUTTON.TWO -> TWO
            ITuner.REMOTE_BUTTON.THREE -> THREE
            ITuner.REMOTE_BUTTON.FOUR -> FOUR
            ITuner.REMOTE_BUTTON.FIVE -> FIVE
            ITuner.REMOTE_BUTTON.SIX -> SIX
            ITuner.REMOTE_BUTTON.SEVEN -> SEVEN
            ITuner.REMOTE_BUTTON.EIGHT -> EIGHT
            ITuner.REMOTE_BUTTON.NINE -> NINE
            ITuner.REMOTE_BUTTON.CHANNEL_UP -> CHANNEL_UP
            ITuner.REMOTE_BUTTON.CHANNEL_DOWN -> CHANNEL_DOWN
            ITuner.REMOTE_BUTTON.LAST_CHANNEL -> LAST_CHANNEL
            ITuner.REMOTE_BUTTON.VOLUME_UP -> VOLUME_UP
            ITuner.REMOTE_BUTTON.VOLUME_DOWN -> VOLUME_DOWN
            ITuner.REMOTE_BUTTON.MUTE -> MUTE
            ITuner.REMOTE_BUTTON.TOGGLE_CC -> TOGGLE_CC
            ITuner.REMOTE_BUTTON.UP -> UP
            ITuner.REMOTE_BUTTON.DOWN -> DOWN
            ITuner.REMOTE_BUTTON.LEFT -> LEFT
            ITuner.REMOTE_BUTTON.RIGHT -> RIGHT
            ITuner.REMOTE_BUTTON.ENTER, ITuner.REMOTE_BUTTON.SELECT -> ENTER
            ITuner.REMOTE_BUTTON.MENU -> MENU
            ITuner.REMOTE_BUTTON.POWER -> POWER
            ITuner.REMOTE_BUTTON.EXIT -> EXIT
            ITuner.REMOTE_BUTTON.INFO -> INFO
            ITuner.REMOTE_BUTTON.RED -> RED
            ITuner.REMOTE_BUTTON.GREEN -> GREEN
            ITuner.REMOTE_BUTTON.YELLOW -> YELLOW
            ITuner.REMOTE_BUTTON.BLUE -> BLUE
            ITuner.REMOTE_BUTTON.SAP -> SAP
            ITuner.REMOTE_BUTTON.BACK -> BACK
            ITuner.REMOTE_BUTTON.GUIDE -> GUIDE
            else -> null
        }
    }
}
