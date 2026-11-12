package com.jht.chimera.ui.machinesettings.pages

import com.jht.chimera.ui.machinesettings.pages.UpdateCheckError.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.jhtp.dapiservice.DapiResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UpdateCheckErrorTest {
    @get:Rule val tmp = TemporaryFolder()

    private val expected = "treadmill"
    private val error = 4 // UpdateManagerTypes.CHECK_UPDATE_ERROR

    // updaterInstalledCheck
    @Test fun updaterInstalledCheck_notInstalled_isUpdaterMissing() {
        val packageManager = mockk<PackageManager>()
        every { packageManager.getPackageInfo("com.jht.updateservice", 0) } throws mockk<PackageManager.NameNotFoundException>()
        assertEquals(UPDATER_MISSING, UpdateCheckErrors.updaterInstalledCheck(packageManager)?.error)
    }

    @Test fun updaterInstalledCheck_installed_isOk() {
        val packageManager = mockk<PackageManager>()
        every { packageManager.getPackageInfo("com.jht.updateservice", 0) } returns mockk<PackageInfo>()
        assertNull(UpdateCheckErrors.updaterInstalledCheck(packageManager))
    }

    // networkCheck
    @Test fun networkCheck_notConnected_isNoNetwork() =
        assertEquals(NO_NETWORK, UpdateCheckErrors.networkCheck(false)?.error)

    @Test fun networkCheck_connected_isOk() = assertNull(UpdateCheckErrors.networkCheck(true))

    // usbDriveCheck
    @Test fun usbDriveCheck_noDirs_isNoUsb() =
        assertEquals(NO_USB, UpdateCheckErrors.usbDriveCheck(emptyList())?.error)

    @Test fun usbDriveCheck_missingDirs_isNoUsb() =
        assertEquals(NO_USB, UpdateCheckErrors.usbDriveCheck(listOf(File(tmp.root, "udisk"), File(tmp.root, "udisk2")))?.error)

    @Test fun usbDriveCheck_dirWithoutManifest_isNoManifest() {
        val usb = tmp.newFolder("udisk")
        File(usb, "update").mkdir() // update.xml in a sub folder does not count
        assertEquals(NO_USB_MANIFEST, UpdateCheckErrors.usbDriveCheck(listOf(usb))?.error)
    }

    @Test fun usbDriveCheck_oneDirWithManifest_isOk() {
        val empty = tmp.newFolder("udisk2")
        val usb = tmp.newFolder("udisk")
        File(usb, "update.xml").writeText("<x/>")
        assertNull(UpdateCheckErrors.usbDriveCheck(listOf(empty, usb)))
    }

    // updateServiceCheck
    @Test fun updateServiceCheck_noReply_isServiceNotRunning() =
        assertEquals(SERVICE_NOT_RUNNING, UpdateCheckErrors.updateServiceCheck(false)?.error)

    @Test fun updateServiceCheck_reply_isOk() = assertNull(UpdateCheckErrors.updateServiceCheck(true))

    // manifestCheck
    @Test fun manifestCheck_unreadable_isInvalidBundle() =
        assertEquals(INVALID_BUNDLE, UpdateCheckErrors.manifestCheck(null, expected)?.error)

    @Test fun manifestCheck_otherEquipment_isWrongEquipment() {
        val result = UpdateCheckErrors.manifestCheck("bike", expected)
        assertEquals(WRONG_EQUIPMENT, result?.error)
        assertEquals("Expected: treadmill, found: bike", result?.detail)
    }

    @Test fun manifestCheck_sameEquipment_isOk() = assertNull(UpdateCheckErrors.manifestCheck(expected, expected))

    // serverCheck
    @Test fun serverCheck_noDapiReply_isServerUnreachable() {
        val result = UpdateCheckErrors.serverCheck(null)
        assertEquals(SERVER_UNREACHABLE, result?.error)
        assertEquals("DAPI: no reply", result?.detail)
    }

    @Test fun serverCheck_timeout_isServerUnreachable() =
        assertEquals(SERVER_UNREACHABLE, UpdateCheckErrors.serverCheck(DapiResult.Timeout)?.error)

    @Test fun serverCheck_internalException_isServerUnreachable() =
        assertEquals(SERVER_UNREACHABLE, UpdateCheckErrors.serverCheck(DapiResult.InternalException)?.error)

    @Test fun serverCheck_invalidHttpCode_isServerUnreachable() =
        assertEquals(SERVER_UNREACHABLE, UpdateCheckErrors.serverCheck(DapiResult.InvalidHttpResponseCode)?.error)

    @Test fun serverCheck_success_isOk() = assertNull(UpdateCheckErrors.serverCheck(DapiResult.Success))

    // serialNumberCheck
    @Test fun serialNumberCheck_generalError_isInvalidSerial() =
        assertEquals(INVALID_SERIAL, UpdateCheckErrors.serialNumberCheck(DapiResult.GeneralError)?.error)

    @Test fun serialNumberCheck_success_isOk() = assertNull(UpdateCheckErrors.serialNumberCheck(DapiResult.Success))

    @Test fun serialNumberCheck_noCardioInfoYet_isOk() = assertNull(UpdateCheckErrors.serialNumberCheck(null))

    // usbUpdateErrorChecks
    @Test fun usbUpdateErrorChecks_noReply_isServiceNotRunning() =
        assertEquals(SERVICE_NOT_RUNNING, UpdateCheckErrors.usbUpdateErrorChecks(error, false, null, expected).error)

    @Test fun usbUpdateErrorChecks_replyWrongEquipment_isWrongEquipment() =
        assertEquals(WRONG_EQUIPMENT, UpdateCheckErrors.usbUpdateErrorChecks(error, true, "bike", expected).error)

    @Test fun usbUpdateErrorChecks_allChecksPass_isUnknownWithCode() {
        val result = UpdateCheckErrors.usbUpdateErrorChecks(99, true, expected, expected)
        assertEquals(UNKNOWN, result.error)
        assertEquals("code 99", result.detail)
    }

    // otaUpdateErrorChecks
    @Test fun otaUpdateErrorChecks_replyWrongEquipment_isWrongEquipment() =
        assertEquals(WRONG_EQUIPMENT, UpdateCheckErrors.otaUpdateErrorChecks(error, true, DapiResult.Success, "bike", expected).error)

    @Test fun otaUpdateErrorChecks_replyGoodManifest_isUnknownWithCode() {
        val result = UpdateCheckErrors.otaUpdateErrorChecks(error, true, DapiResult.Success, expected, expected)
        assertEquals(UNKNOWN, result.error)
        assertEquals("code 4", result.detail)
    }

    @Test fun otaUpdateErrorChecks_noReplyTimeout_isServerUnreachable() =
        assertEquals(SERVER_UNREACHABLE, UpdateCheckErrors.otaUpdateErrorChecks(error, false, DapiResult.Timeout, null, expected).error)

    @Test fun otaUpdateErrorChecks_noReplyServerOk_isUnknownWithName() {
        val result = UpdateCheckErrors.otaUpdateErrorChecks(error, false, DapiResult.MachineIdNotFound, null, expected)
        assertEquals(UNKNOWN, result.error)
        assertEquals("code 4, DAPI: MachineIdNotFound", result.detail)
    }
}
