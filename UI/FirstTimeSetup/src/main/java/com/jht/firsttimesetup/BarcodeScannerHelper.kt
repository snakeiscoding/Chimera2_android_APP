package com.jht.firsttimesetup

import android.app.Activity
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog

interface ConsoleSerialHandler {
    fun updateSerial(serial: String)
    fun getSerial(): String
}

/**
 * Collects keyboard-like barcode scanner input from Activity.dispatchKeyEvent().
 *
 * Usage:
 *  - override Activity.dispatchKeyEvent() and call scanner.dispatchKeyEvent(event)
 *  - enable/disable via scanner.setEnabled()
 *
 */
class BarcodeScannerHelper(
    private val activity: Activity,
    private val consoleSerialHandler: ConsoleSerialHandler,
) {
    private val buffer = StringBuilder()

    @Volatile
    private var enabled: Boolean = true
    private val handler = Handler(Looper.getMainLooper())

    // Runnable that will process the barcode scanner entry
    private val finalizeRunnable = Runnable {
        val text = buffer.toString()
        buffer.setLength(0)

        // Process any serial with at least 14 characters
        if (text.length >= 14) {
            onSerialDetected(text)
        }
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) {
            handler.removeCallbacks(finalizeRunnable)
            buffer.setLength(0)
        }
    }

    /**
     * Call this from Activity.dispatchKeyEvent(event).
     * Returns true if it consumed the event (we don't consume by default).
     */
    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!enabled) return false

        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return false

        val unicode = event.unicodeChar
        if (unicode == 0) return false

        val rawChar = unicode.toChar()
        if (!rawChar.isLetterOrDigit()) return false

        buffer.append(rawChar.uppercaseChar())

        handler.removeCallbacks(finalizeRunnable)
        handler.postDelayed(finalizeRunnable, 1000)

        // Don't consume
        return false
    }

    private fun onSerialDetected(serial: String) {
        // NOTE: this alert dialog box is for the factory process so no translation needed to speed up flashing the console serial number
        activity.runOnUiThread {
            AlertDialog.Builder(activity)
                .setTitle("Update Console Serial")
                .setMessage(serial)
                .setCancelable(false)
                .setPositiveButton("Save") { _: DialogInterface, _: Int ->
                    // Update via callback
                    consoleSerialHandler.updateSerial(serial)

                    // Show confirmation
                    AlertDialog.Builder(activity)
                        .setTitle("Serial Updated")
                        .setMessage("The console serial was updated to:\n\n${consoleSerialHandler.getSerial()}")
                        .setCancelable(true)
                        .setPositiveButton("OK") { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
                        .show()
                }
                .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                .show()
        }
    }
}
