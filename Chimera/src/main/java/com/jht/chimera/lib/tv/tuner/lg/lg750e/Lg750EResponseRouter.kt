package com.jht.chimera.lib.tv.tuner.lg.lg750e

import com.jht.chimera.lib.common.datamatrixkeys.TVKeys

/**
 * Parses classic-API responses from the LG750E tuner and publishes each one to its DataMatrix key.
 * JSON replies are published by [Lg750ECommand.Query] instead.
 *
 * Responses are either one channel-list JSON object per message, ending with a `channellist_end` or
 * `channellist_error` message, or `key=value` lines such as `version=1.2.3`.
 *
 * Thread-safe: responses arrive on the WebSocket thread, while [startChannelImport] is called from a
 * DataMatrix callback.
 *
 * @param publish Publishes a value. Must always notify listeners, even for an unchanged value
 *   (DataMatrix `put`, not `update`), because callers wait for a reply to each request.
 * @param log Diagnostic logging, kept out of this class so it stays testable on the JVM.
 */
internal class Lg750EResponseRouter(
    private val publish: (key: TVKeys, value: String) -> Unit,
    private val log: (String) -> Unit = {},
) {
    private val channelBuffer = mutableListOf<String>()

    /** Discards any partial channel list, ready for the replies to a new command 41. */
    @Synchronized
    fun startChannelImport() {
        channelBuffer.clear()
    }

    @Synchronized
    fun route(response: String) {
        when {
            CHANNEL_ENTRY_MARKER in response -> {
                channelBuffer.add(response)
                log("channel entry ${channelBuffer.size}")
            }
            CHANNEL_END_MARKER in response -> {
                val result = channelBuffer.joinToString(",", "[", "]")
                log("channel list end, delivering ${channelBuffer.size} channels (${result.length} chars)")
                publish(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING, result)
                channelBuffer.clear()
            }
            CHANNEL_ERROR_MARKER in response -> {
                log("channel list error: $response")
                publish(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING, response)
                channelBuffer.clear()
            }
            else -> response.lineSequence().forEach(::publishKeyValue)
        }
    }

    private fun publishKeyValue(line: String) {
        val name = line.substringBefore('=', missingDelimiterValue = "")
        val value = line.substringAfter('=', missingDelimiterValue = "")
        // Skip empty values so a pending request times out instead of completing with nothing.
        if (value.isEmpty()) return
        TV_KEYS_BY_NAME[name]?.let { publish(it, value) }
    }

    private companion object {
        const val CHANNEL_ENTRY_MARKER = "\"channellist_entry\""
        const val CHANNEL_END_MARKER = "\"channellist_end\""
        const val CHANNEL_ERROR_MARKER = "\"channellist_error\""

        /** The key each `name=value` reply is published to. */
        val TV_KEYS_BY_NAME = mapOf(
            "version" to TVKeys.DATA_TUNER_VERSION_STRING,
            "model" to TVKeys.EVENT_TUNER_MODEL_STRING,
            "ip" to TVKeys.EVENT_TUNER_IP_ADDRESS_STRING,
            "mac" to TVKeys.EVENT_TUNER_MAC_ADDRESS_STRING,
            "dhcp" to TVKeys.EVENT_TUNER_DHCP_STRING,
            "subnet" to TVKeys.EVENT_TUNER_SUBNET_STRING,
            "gateway" to TVKeys.EVENT_TUNER_GATEWAY_STRING,
            "dns1" to TVKeys.EVENT_TUNER_DNS1_STRING,
            "dns2" to TVKeys.EVENT_TUNER_DNS2_STRING,
        )
    }
}
