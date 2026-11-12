package com.jht.chimera.lib.tv.tuner.lg.lg750e

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Lg750ECommandTest {
    // region relay round trip

    @Test
    fun `every object command survives a relay round trip`() {
        val objects = listOf(
            Lg750ECommand.Reboot, Lg750ECommand.GetVersion, Lg750ECommand.GetNetwork, Lg750ECommand.SaveDefaultChannel,
            Lg750ECommand.EnableDhcp, Lg750ECommand.GetMac, Lg750ECommand.GetModel, Lg750ECommand.ClickMouse,
            Lg750ECommand.ClearUserData, Lg750ECommand.GetChannelList,
        )

        // Compared as one list, so the failure shows every object that came back wrong or null.
        assertEquals(objects, objects.map { Lg750ECommand.fromRelay(Lg750ECommand.toRelay(it)) })
    }

    @Test
    fun `KeyPress keeps its key and count through a relay round trip`() {
        // Not the default count or the first key, so a value dropped in transit cannot come back by coincidence.
        val command = Lg750ECommand.KeyPress(Lg750EKey.VOLUME_UP, count = 3)

        assertEquals(command, Lg750ECommand.fromRelay(Lg750ECommand.toRelay(command)))
    }

    @Test
    fun `KeyPress with the default count survives a relay round trip`() {
        val command = Lg750ECommand.KeyPress(Lg750EKey.VOLUME_UP)

        assertEquals(command, Lg750ECommand.fromRelay(Lg750ECommand.toRelay(command)))
    }

    @Test
    fun `MoveMouse keeps its coordinates through a relay round trip`() {
        val command = Lg750ECommand.MoveMouse(0.123f, 0.987f)

        assertEquals(command, Lg750ECommand.fromRelay(Lg750ECommand.toRelay(command)))
    }

    @Test
    fun `SetStaticNetwork keeps every field through a relay round trip`() {
        // A different value in every field, so two swapped fields fail.
        val command = Lg750ECommand.SetStaticNetwork("192.168.1.20", "255.255.255.0", "192.168.1.1", "8.8.8.8", "8.8.4.4")

        assertEquals(command, Lg750ECommand.fromRelay(Lg750ECommand.toRelay(command)))
    }

    @Test
    fun `SetSoftAp keeps its password through a relay round trip`() {
        val command = Lg750ECommand.SetSoftAp("GymGuest", "hunter2")

        // Compared by field: toString masks the password, so a failure message would hide it.
        val relayed = Lg750ECommand.fromRelay(Lg750ECommand.toRelay(command)) as? Lg750ECommand.SetSoftAp
        assertEquals("GymGuest", relayed?.ssid)
        assertEquals("hunter2", relayed?.password)
    }

    @Test
    fun `Tune keeps its fields in order through a relay round trip`() {
        val command = Lg750ECommand.Tune(
            "RF2",
            listOf("frequency" to "567000", "programNumber" to "3", "rfBroadcastType" to "ATSC"),
        )

        assertEquals(command, Lg750ECommand.fromRelay(Lg750ECommand.toRelay(command)))
    }

    @Test
    fun `fromRelay returns null for an unknown command name`() {
        assertNull(Lg750ECommand.fromRelay("""{"command":"NotACommand"}"""))
    }

    @Test
    fun `fromRelay returns null for text that is not JSON`() {
        // A classic command string, the likeliest thing to be relayed by mistake.
        assertNull(Lg750ECommand.fromRelay("02:30\n"))
    }

    @Test
    fun `fromRelay returns null when a required field is missing`() {
        // SetVolume with no level.
        val serialName = Lg750ECommand.SetVolume.serializer().descriptor.serialName

        assertNull(Lg750ECommand.fromRelay("""{"command":"$serialName"}"""))
    }

    // endregion
}
