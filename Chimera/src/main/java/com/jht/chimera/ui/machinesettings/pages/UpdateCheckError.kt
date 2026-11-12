package com.jht.chimera.ui.machinesettings.pages

import android.content.pm.PackageManager
import com.jht.chimera.R
import com.jhtp.dapiservice.DapiResult
import java.io.File

/** Why an update check failed. [message] is the short text shown to the user. */
enum class UpdateCheckError(val message: Int) {
    NO_NETWORK(R.string.no_network_pg),
    NO_USB(R.string.update_error_no_usb_pg),
    NO_USB_MANIFEST(R.string.update_error_no_usb_manifest_pg),
    INVALID_BUNDLE(R.string.update_error_invalid_bundle_pg),
    WRONG_EQUIPMENT(R.string.update_error_wrong_equipment_pg),
    INVALID_SERIAL(R.string.update_error_invalid_serial_pg),
    SERVER_UNREACHABLE(R.string.update_error_server_unreachable_pg),
    UPDATER_MISSING(R.string.update_error_updater_missing_pg),
    SERVICE_NOT_RUNNING(R.string.update_error_service_not_running_pg),
    UNKNOWN(R.string.error_occurred_tc),
}

/** An error plus an English [detail] for the log. The dialog shows only the error text. */
data class UpdateCheckResult @JvmOverloads constructor(val error: UpdateCheckError, val detail: String = "")

/**
 * Finds the reason for a failed update check. The updater library returns the same code
 * (CHECK_UPDATE_ERROR) for many problems, so we use other values the app has to tell them apart.
 *
 * Each `*Check` function checks one thing and returns null when that check passes.
 */
object UpdateCheckErrors {
    private const val MANIFEST = "update.xml"
    private const val UPDATER_PACKAGE = "com.jht.updateservice"

    @JvmStatic
    fun updaterInstalledCheck(packageManager: PackageManager): UpdateCheckResult? {
        try {
            packageManager.getPackageInfo(UPDATER_PACKAGE, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return UpdateCheckResult(UpdateCheckError.UPDATER_MISSING)
        }
        return null
    }

    @JvmStatic
    fun networkCheck(isConnected: Boolean): UpdateCheckResult? {
        if (!isConnected) {
            return UpdateCheckResult(UpdateCheckError.NO_NETWORK)
        }
        return null
    }

    /** Checks for a USB drive with an update.xml. A missing drive has no folder. */
    @JvmStatic
    fun usbDriveCheck(dirs: List<File>): UpdateCheckResult? {
        val drives = dirs.filter { it.isDirectory }
        if (drives.isEmpty()) {
            return UpdateCheckResult(UpdateCheckError.NO_USB)
        }
        if (drives.none { File(it, MANIFEST).isFile }) {
            return UpdateCheckResult(UpdateCheckError.NO_USB_MANIFEST)
        }
        return null
    }

    /** No reply means the check timed out, so the Update Service is not running. */
    @JvmStatic
    fun updateServiceCheck(hasReply: Boolean): UpdateCheckResult? {
        if (!hasReply) {
            return UpdateCheckResult(UpdateCheckError.SERVICE_NOT_RUNNING)
        }
        return null
    }

    /** [equipment] is null when the manifest cannot be read. */
    @JvmStatic
    fun manifestCheck(equipment: String?, expected: String): UpdateCheckResult? {
        if (equipment == null) {
            return UpdateCheckResult(UpdateCheckError.INVALID_BUNDLE)
        }
        if (equipment != expected) {
            return UpdateCheckResult(UpdateCheckError.WRONG_EQUIPMENT, "Expected: $expected, found: $equipment")
        }
        return null
    }

    /** InternalException is a failed HTTP call (DNS, SSL, refused), so it is the same as a timeout for the user. */
    @JvmStatic
    fun serverCheck(softwareDapi: DapiResult?): UpdateCheckResult? {
        val serverFailed = softwareDapi == null
                || softwareDapi == DapiResult.Timeout
                || softwareDapi == DapiResult.InternalException
                || softwareDapi == DapiResult.InvalidHttpResponseCode
        if (serverFailed) {
            return UpdateCheckResult(UpdateCheckError.SERVER_UNREACHABLE, "DAPI: ${softwareDapi ?: "no reply"}")
        }
        return null
    }

    /** cardioInfo fails with GeneralError when the serial numbers are not valid. */
    @JvmStatic
    fun serialNumberCheck(cardioInfoDapi: DapiResult?): UpdateCheckResult? {
        if (cardioInfoDapi == DapiResult.GeneralError) {
            return UpdateCheckResult(UpdateCheckError.INVALID_SERIAL)
        }
        return null
    }

    /** Finds why a USB update check failed. */
    @JvmStatic
    fun usbUpdateErrorChecks(code: Int, hasReply: Boolean, equipment: String?, expected: String): UpdateCheckResult {
        val serviceError = updateServiceCheck(hasReply)
        if (serviceError != null) {
            return serviceError
        }
        val manifestError = manifestCheck(equipment, expected)
        if (manifestError != null) {
            return manifestError
        }
        return UpdateCheckResult(UpdateCheckError.UNKNOWN, "code $code")
    }

    /**
     * Finds why an OTA update check failed. We cannot use [updateServiceCheck] here, because the
     * library also sends no reply when its DAPI call or update.xml download fails.
     */
    @JvmStatic
    fun otaUpdateErrorChecks(
        code: Int,
        hasReply: Boolean,
        softwareDapi: DapiResult?,
        equipment: String?,
        expected: String,
    ): UpdateCheckResult {
        if (hasReply) {
            // The updater read the downloaded manifest
            val manifestError = manifestCheck(equipment, expected)
            if (manifestError != null) {
                return manifestError
            }
            return UpdateCheckResult(UpdateCheckError.UNKNOWN, "code $code")
        }
        val serverError = serverCheck(softwareDapi)
        if (serverError != null) {
            return serverError
        }
        return UpdateCheckResult(UpdateCheckError.UNKNOWN, "code $code, DAPI: $softwareDapi")
    }
}
