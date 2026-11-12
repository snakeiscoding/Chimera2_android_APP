package com.jht.chimera.lib.common

import android.util.Log
import java.io.File

/**
 * Once-per-version guard for expensive idempotent boot work (e.g. the root-shell
 * `pm enable/disable` configuration). A stamp is "current" when its stored signature
 * matches the caller's (typically Build.FINGERPRINT + Build.TIME + software version),
 * so an OS or app update — or a changed setting folded into the signature — re-runs
 * the work exactly once. Fail-open: any IO problem reads as "not current" so the
 * guarded work runs rather than being silently skipped.
 *
 * Field override: deleting `<filesDir>/boot_stamps/<name>.stamp` forces a re-run.
 */
class BootStamp(private val stampDir: File) {
    private companion object {
        const val TAG = "BootStamp"
    }

    fun isCurrent(name: String, signature: String): Boolean = runCatching {
        val file = File(stampDir, "$name.stamp")
        file.isFile && file.readText() == signature
    }.getOrElse {
        Log.w(TAG, "isCurrent failed for $name", it)
        false
    }

    fun update(name: String, signature: String) {
        runCatching {
            stampDir.mkdirs()
            File(stampDir, "$name.stamp").writeText(signature)
        }.onFailure { Log.w(TAG, "update failed for $name", it) }
    }
}
