package com.jht.chimera.lib.tv.tuner.lg.lg750e

import com.jht.chimera.lib.common.datamatrixkeys.TVKeys
import org.junit.Assert.assertEquals
import org.junit.Test

class Lg750EResponseRouterTest {
    private val published = mutableListOf<Pair<TVKeys, String>>()
    private val router = Lg750EResponseRouter({ key, value -> published += key to value })

    private fun channelEntry(name: String) = """{"type":"channellist_entry","name":"$name","rfClass":"RF1"}"""
    private val channelEnd = """{"type":"channellist_end"}"""
    private val channelError = """{"type":"channellist_error","message":"no signal"}"""

    // region key=value replies

    @Test
    fun `each known key is published to its DataMatrix key`() {
        val expected = listOf(
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
        expected.forEach { (name, _) -> router.route("$name=value-$name\n") }

        assertEquals(expected.map { (name, key) -> key to "value-$name" }, published)
    }

    @Test
    fun `version keeps the firmware suffix the tuner appends`() {
        router.route("version=1.4.2|fw:05.10.20\n")

        assertEquals(listOf(TVKeys.DATA_TUNER_VERSION_STRING to "1.4.2|fw:05.10.20"), published)
    }

    @Test
    fun `a value containing an equals sign is kept whole`() {
        router.route("model=STB=6500\n")

        assertEquals(listOf(TVKeys.EVENT_TUNER_MODEL_STRING to "STB=6500"), published)
    }

    @Test
    fun `a reply without a trailing newline is still published`() {
        router.route("ip=192.168.1.20")

        assertEquals(listOf(TVKeys.EVENT_TUNER_IP_ADDRESS_STRING to "192.168.1.20"), published)
    }

    @Test
    fun `several lines in one message are each published in order`() {
        router.route("dhcp=true\nip=10.0.0.2\n")

        assertEquals(
            listOf(TVKeys.EVENT_TUNER_DHCP_STRING to "true", TVKeys.EVENT_TUNER_IP_ADDRESS_STRING to "10.0.0.2"),
            published,
        )
    }

    @Test
    fun `an unchanged value is published every time`() {
        router.route("mac=AA:BB\n")
        router.route("mac=AA:BB\n")

        assertEquals(2, published.size)
    }

    @Test
    fun `an empty value is not published`() {
        router.route("ip=\n")

        assertEquals(emptyList<Pair<TVKeys, String>>(), published)
    }

    @Test
    fun `an unknown key is not published`() {
        router.route("brightness=5\n")

        assertEquals(emptyList<Pair<TVKeys, String>>(), published)
    }

    @Test
    fun `a line without an equals sign is not published`() {
        router.route("OK\n")
        router.route("")

        assertEquals(emptyList<Pair<TVKeys, String>>(), published)
    }

    // endregion

    // region channel list

    @Test
    fun `an empty channel list delivers an empty array`() {
        router.route(channelEnd)

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING to "[]"), published)
    }

    @Test
    fun `one channel is delivered as a single element array`() {
        router.route(channelEntry("A"))
        router.route(channelEnd)

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING to "[${channelEntry("A")}]"), published)
    }

    @Test
    fun `several channels are delivered together in arrival order`() {
        router.route(channelEntry("A"))
        router.route(channelEntry("B"))
        router.route(channelEntry("C"))

        assertEquals("entries are buffered until the end marker", emptyList<Pair<TVKeys, String>>(), published)

        router.route(channelEnd)

        assertEquals(
            listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING to "[${channelEntry("A")},${channelEntry("B")},${channelEntry("C")}]"),
            published,
        )
    }

    @Test
    fun `a channel entry is not read as a key=value reply`() {
        router.route("""{"type":"channellist_entry","name":"ip=1.2.3.4"}""")

        assertEquals(emptyList<Pair<TVKeys, String>>(), published)
    }

    @Test
    fun `the buffer is cleared after a list is delivered`() {
        router.route(channelEntry("A"))
        router.route(channelEnd)
        published.clear()

        router.route(channelEnd)

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING to "[]"), published)
    }

    @Test
    fun `an error is published and discards the partial list`() {
        router.route(channelEntry("A"))
        router.route(channelError)
        router.route(channelEnd)

        assertEquals(
            listOf(
                TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING to channelError,
                TVKeys.EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING to "[]",
            ),
            published,
        )
    }

    @Test
    fun `starting an import discards a partial list from an earlier one`() {
        router.route(channelEntry("stale"))
        router.startChannelImport()
        router.route(channelEntry("A"))
        router.route(channelEnd)

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING to "[${channelEntry("A")}]"), published)
    }

    // endregion
}
